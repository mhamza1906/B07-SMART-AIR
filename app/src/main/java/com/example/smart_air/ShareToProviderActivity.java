package com.example.smart_air;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.*;
import java.util.*;

public class ShareToProviderActivity extends AppCompatActivity {

    private String childId;
    private FirebaseFirestore db;

    private ProviderShareAdapter adapter;
    private final List<ProviderShareItem> providerList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_to_provider);

        childId = getIntent().getStringExtra("childID");
        if (childId == null) {
            Toast.makeText(this, "Child ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        RecyclerView providerRecycler = findViewById(R.id.recyclerProviders);
        Button btnAddProvider = findViewById(R.id.btnLinkNewProvider);

        providerRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProviderShareAdapter(providerList, this::revokeSharing);
        providerRecycler.setAdapter(adapter);

        btnAddProvider.setOnClickListener(v -> showProviderInputBottomSheet());

        loadSharedProviders();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadSharedProviders() {
        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .get()
                .addOnSuccessListener(snap -> {
                    providerList.clear();
                    for (DocumentSnapshot doc : snap) {

                        Boolean available = doc.getBoolean("availability");
                        if (available != null && !available) {
                            continue;
                        }

                        String username = doc.getString("provider_username");

                        providerList.add(new ProviderShareItem(username));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showProviderInputBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(
                R.layout.bottomsheet_provider_username,
                findViewById(android.R.id.content),
                false
        );


        EditText edtUsername = view.findViewById(R.id.edtProviderUsername);
        Button btnNext = view.findViewById(R.id.btnFindProvider);

        btnNext.setOnClickListener(v -> {
            String username = edtUsername.getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                edtUsername.setError("Please enter a provider username");
                return;
            }
            dialog.dismiss();
            findProviderAccount(username);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void findProviderAccount(String username) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");

        ref.orderByChild("username").equalTo(username)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        boolean found = false;

                        for (DataSnapshot child : snapshot.getChildren()) {

                            String type = child.child("accountType").getValue(String.class);
                            if (type != null && type.equals("Healthcare Provider")) {

                                found = true;

                                String providerId = child.getKey(); // this is the providerID
                                String providerUsername =
                                        child.child("username").getValue(String.class);

                                showShareChartsBottomSheet(providerUsername, providerId);
                                break;
                            }
                        }

                        if (!found) {
                            Toast.makeText(ShareToProviderActivity.this,
                                    "Provider not found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ShareToProviderActivity.this,
                                "Database error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void showShareChartsBottomSheet(String providerUsername, String providerId) {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(
                R.layout.bottomsheet_share_charts,
                findViewById(android.R.id.content),
                false
        );


        CheckBox chkSymptoms = view.findViewById(R.id.chkSymptoms);
        CheckBox chkTriggers = view.findViewById(R.id.chkTriggers);
        CheckBox chkRescue = view.findViewById(R.id.chkRescue);
        CheckBox chkController = view.findViewById(R.id.chkController);
        CheckBox chkPEF = view.findViewById(R.id.chkPEF);
        CheckBox chkTriage = view.findViewById(R.id.chkTriage);

        Button btnConfirm = view.findViewById(R.id.btnConfirmCharts);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();

            Map<String, Boolean> summaryMap = new HashMap<>();
            summaryMap.put("symptoms", chkSymptoms.isChecked());
            summaryMap.put("triggers", chkTriggers.isChecked());
            summaryMap.put("rescue", chkRescue.isChecked());
            summaryMap.put("controller", chkController.isChecked());
            summaryMap.put("pef", chkPEF.isChecked());
            summaryMap.put("triage", chkTriage.isChecked());

            saveChildProviderLink(providerUsername, providerId, summaryMap);
        });

        dialog.setContentView(view);
        dialog.show();
    }


    private void saveChildProviderLink(String providerUsername,
                                       String providerId,
                                       Map<String, Boolean> summaryMap) {

        DocumentReference ref =
                db.collection("child-provider-share")
                        .document(childId)
                        .collection("providers")
                        .document(providerUsername);

        Map<String, Object> data = new HashMap<>();
        data.put("provider_username", providerUsername);
        data.put("provider_id", providerId);
        data.put("availability", true);
        data.put("generated_at", Timestamp.now());
        data.put("summary_visibility", summaryMap);

        ref.set(data, SetOptions.merge()).addOnSuccessListener(r -> {

            // another provider-child-index: provider-child-index -> providerUsername -> childIDs
            Map<String, Object> reverse = new HashMap<>();
            reverse.put("childId", childId);
            reverse.put("provider_id", providerId);
            reverse.put("availability", true);
            reverse.put("generated_at", Timestamp.now());
            reverse.put("code", null);
            reverse.put("link", null);

            db.collection("provider-child-index")
                    .document(providerUsername)
                    .collection("children")
                    .document(childId)
                    .set(reverse, SetOptions.merge());


            showCodeOrLinkBottomSheet(providerUsername);
        });
    }


    private void showCodeOrLinkBottomSheet(String providerUsername) {

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(
                R.layout.bottomsheet_invite_type,
                findViewById(android.R.id.content),
                false
        );


        Button btnCode = view.findViewById(R.id.btnGenerateCode);
        Button btnLink = view.findViewById(R.id.btnGenerateLink);

        btnCode.setOnClickListener(v -> {
            dialog.dismiss();
            generateSharingCode(providerUsername);
        });

        btnLink.setOnClickListener(v -> {
            dialog.dismiss();
            generateSharingLink(providerUsername);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    private void generateSharingCode(String providerUsername) {

        String code = generateRandomCode();

        DocumentReference ref =
                db.collection("child-provider-share")
                        .document(childId)
                        .collection("providers")
                        .document(providerUsername);

        Map<String, Object> update = new HashMap<>();
        update.put("code", code);
        update.put("link", null);
        update.put("generated_at", Timestamp.now());

        ref.update(update).addOnSuccessListener(r -> {

            Map<String, Object> reverseUpdate = new HashMap<>();
            reverseUpdate.put("code", code);
            reverseUpdate.put("link", null);
            reverseUpdate.put("generated_at", Timestamp.now());

            db.collection("provider-child-index")
                    .document(providerUsername)
                    .collection("children")
                    .document(childId)
                    .update(reverseUpdate);

            Toast.makeText(this, "Code generated: " + code, Toast.LENGTH_LONG).show();
            loadSharedProviders();
        });
    }


    private void generateSharingLink(String providerUsername) {

        String code = generateRandomCode();
        String link = "smart-air://share?code=" + code;

        DocumentReference ref =
                db.collection("child-provider-share")
                        .document(childId)
                        .collection("providers")
                        .document(providerUsername);

        Map<String, Object> update = new HashMap<>();
        update.put("code", code);
        update.put("link", link);
        update.put("generated_at", Timestamp.now());

        ref.update(update).addOnSuccessListener(r -> {

            Map<String, Object> reverseUpdate = new HashMap<>();
            reverseUpdate.put("code", code);
            reverseUpdate.put("link", link);
            reverseUpdate.put("generated_at", Timestamp.now());

            db.collection("provider-child-index")
                    .document(providerUsername)
                    .collection("children")
                    .document(childId)
                    .update(reverseUpdate);

            Toast.makeText(this, "Link generated", Toast.LENGTH_LONG).show();
            loadSharedProviders();
        });
    }


    private void revokeSharing(String providerUsername) {

        DocumentReference ref =
                db.collection("child-provider-share")
                        .document(childId)
                        .collection("providers")
                        .document(providerUsername);

        Map<String, Object> update = new HashMap<>();
        update.put("availability", false);

        ref.update(update).addOnSuccessListener(r -> {

            db.collection("provider-child-index")
                    .document(providerUsername)
                    .collection("children")
                    .document(childId)
                    .update("availability", false);

            Toast.makeText(this, "Sharing revoked", Toast.LENGTH_SHORT).show();
            loadSharedProviders();
        });
    }

}
