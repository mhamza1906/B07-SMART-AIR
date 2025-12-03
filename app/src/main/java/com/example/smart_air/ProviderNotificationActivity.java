package com.example.smart_air;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.*;

import java.util.*;

public class ProviderNotificationActivity extends AppCompatActivity {

    private String providerUsername;

    private ProviderInviteAdapter adapter;
    private final List<ProviderInviteItem> list = new ArrayList<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.provider_notification);

        providerUsername = getIntent().getStringExtra("providerUsername");

        db = FirebaseFirestore.getInstance();

        RecyclerView recycler = findViewById(R.id.recyclerCodesLinks);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProviderInviteAdapter(list, this::onLinkClick);
        recycler.setAdapter(adapter);

        Button btnCheckCode = findViewById(R.id.btnCheckACode);
        btnCheckCode.setOnClickListener(v -> showCodeCheckSheet());

        loadInvites();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadInvites() {
        db.collection("provider-child-index")
                .document(providerUsername)
                .collection("children")
                .whereEqualTo("availability", true)
                .addSnapshotListener((snap, e) -> {
                    list.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap) {

                            Boolean used = doc.getBoolean("used");
                            if (Boolean.TRUE.equals(used)) continue;

                            String childId = doc.getString("childId");
                            String code = doc.getString("code");
                            String link = doc.getString("link");

                            list.add(new ProviderInviteItem(childId, code, link));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }


    private void showCodeCheckSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(
                R.layout.bottomsheet_check_code,
                findViewById(android.R.id.content),
                false
        );


        EditText edt = v.findViewById(R.id.edtCodeInput);
        Button btn = v.findViewById(R.id.btnConfirmCode);

        btn.setOnClickListener(view -> {
            String input = edt.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                edt.setError("Enter code");
                return;
            }
            validateCode(input);
            dialog.dismiss();
        });

        dialog.setContentView(v);
        dialog.show();
    }

    private void validateCode(String input) {
        db.collection("provider-child-index")
                .document(providerUsername)
                .collection("children")
                .whereEqualTo("code", input)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "Incorrect code.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String childId = doc.getString("childId");

                    updateUsedAndChecked(childId);
                });
    }

    private void updateUsedAndChecked(String childId) {

        DocumentReference ref = db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .document(providerUsername);

        Map<String,Object> m = new HashMap<>();
        m.put("used", true);
        m.put("code_checked", true);

        ref.update(m).addOnSuccessListener(r -> {

            db.collection("provider-child-index")
                    .document(providerUsername)
                    .collection("children")
                    .document(childId)
                    .update("used", true, "code_checked", true);

            Toast.makeText(this,
                    "Code verified! It will disappear.",
                    Toast.LENGTH_LONG).show();
        });
    }


    private void onLinkClick(String linkValue, String childId) {

        updateUsedAndChecked(childId);

        db.collection("child-provider-share")
                .document(childId)
                .collection("providers")
                .document(providerUsername)
                .get()
                .addOnSuccessListener(doc -> {
                    Intent i = new Intent(this, ProviderViewChildSummaryActivity.class);
                    i.putExtra("childID", childId);
                    i.putExtra("providerUsername", providerUsername);

                    Map<String, Boolean> vis = (Map<String, Boolean>) doc.get("summary_visibility");
                    if (vis != null) {
                        for (Map.Entry<String, Boolean> entry : vis.entrySet()) {
                            i.putExtra("summary_" + entry.getKey(), entry.getValue());
                        }
                    }

                    startActivity(i);
                });
    }

}
