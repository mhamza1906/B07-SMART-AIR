package com.example.smart_air;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.gridlayout.widget.GridLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentDailyCheckIn extends AppCompatActivity {


    private RadioGroup groupNightWaking;
    private RadioGroup groupCoughWheeze;
    private RadioGroup groupActivityLimit;

    private GridLayout triggersgrid;

    private Button btnSubmit;
    private Button btnEdit;

    private String childId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.daily_check_in);

        db = FirebaseFirestore.getInstance();

        childId = getIntent().getStringExtra("childID");
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "User ID not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        groupNightWaking = findViewById(R.id.night_waking_group);
        groupCoughWheeze = findViewById(R.id.cough_wheeze_group);
        groupActivityLimit = findViewById(R.id.activity_limit_group);

        triggersgrid = findViewById(R.id.grid_global_triggers);

        btnSubmit = findViewById(R.id.submit_check_in_button);
        btnEdit = findViewById(R.id.editbtn);

        loadCheckInData();

        btnEdit.setOnClickListener(v -> editData());
        btnSubmit.setOnClickListener(v -> saveCheckInData());
    }

    private void loadCheckInData() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference docRef = db.collection("DailyCheckIns")
                .document(childId)
                .collection("log")
                .document(todayDate);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                selectRadioButtonByText(groupNightWaking, documentSnapshot.getString("NightWaking"));
                selectRadioButtonByText(groupCoughWheeze, documentSnapshot.getString("CoughWheeze"));
                selectRadioButtonByText(groupActivityLimit, documentSnapshot.getString("ActivityLimit"));


                selectCheckboxesByText(triggersgrid, getStringList(documentSnapshot, "Triggers"));

                btnSubmit.setVisibility(View.GONE);
                btnEdit.setVisibility(View.VISIBLE);
                EnableAllInputs(false);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load existing data.", Toast.LENGTH_SHORT).show()
        );
    }

    private void editData() {
        EnableAllInputs(true);
        btnSubmit.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);
    }

    private void EnableAllInputs(boolean enabled) {
        enableRadioGroup(groupNightWaking, enabled);
        enableRadioGroup(groupCoughWheeze, enabled);
        enableRadioGroup(groupActivityLimit, enabled);
        enableCheckboxGroup(triggersgrid, enabled);
    }

    private void enableRadioGroup(RadioGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    private void enableCheckboxGroup(ViewGroup container, boolean enabled) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof CheckBox)
                child.setEnabled(enabled);
        }
    }

    private void saveCheckInData() {
        if (groupNightWaking.getCheckedRadioButtonId() == -1 ||
                groupCoughWheeze.getCheckedRadioButtonId() == -1 ||
                groupActivityLimit.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please fill out all required symptom fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("Author", "parent");

        data.put("NightWaking", getSelectedText(groupNightWaking));
        data.put("CoughWheeze", getSelectedText(groupCoughWheeze));
        data.put("ActivityLimit", getSelectedText(groupActivityLimit));

        // Simplified trigger saving
        data.put("Triggers", getSelectedTriggers(triggersgrid));

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference docRef = db.collection("DailyCheckIns")
                .document(childId)
                .collection("log")
                .document(todayDate);

        docRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ParentDailyCheckIn.this, "Check-in saved!", Toast.LENGTH_SHORT).show();
                    EnableAllInputs(false);
                    btnSubmit.setVisibility(View.GONE);
                    btnEdit.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ParentDailyCheckIn.this, "Failed to save data.", Toast.LENGTH_SHORT).show()
                );
    }

    private String getSelectedText(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton selected = findViewById(selectedId);
        return selected.getText().toString();
    }

    private List<String> getSelectedTriggers(ViewGroup container) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                list.add(((CheckBox) v).getText().toString());
            }
        }
        return list;
    }

    private void selectRadioButtonByText(RadioGroup group, String text) {
        if (text == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (v instanceof RadioButton) {
                RadioButton rb = (RadioButton) v;
                if (rb.getText().toString().equals(text)) {
                    rb.setChecked(true);
                    return;
                }
            }
        }
    }

    private void selectCheckboxesByText(ViewGroup container, List<String> values) {
        if (values == null) return;
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            if (v instanceof CheckBox) {
                CheckBox cb = (CheckBox) v;
                cb.setChecked(values.contains(cb.getText().toString()));
            }
        }
    }

    private List<String> getStringList(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        List<String> list = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object o : (List<?>) value) {
                if (o instanceof String)
                    list.add((String) o);
            }
        }
        return list;
    }
}
