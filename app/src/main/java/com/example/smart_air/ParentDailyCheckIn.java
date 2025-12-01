package com.example.smart_air;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

    private RadioGroup nightWakingGroup;
    private RadioGroup coughWheezeGroup;
    private RadioGroup activityLimitGroup;

    private ViewGroup nightWakingTriggersContainer;
    private ViewGroup coughWheezeTriggersContainer;
    private ViewGroup activityLimitTriggersContainer;

    private Button submitButton;
    private Button editBtn;

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


        nightWakingGroup = findViewById(R.id.night_waking_group);
        coughWheezeGroup = findViewById(R.id.cough_wheeze_group);
        activityLimitGroup = findViewById(R.id.activity_limit_group);

        nightWakingTriggersContainer = findViewById(R.id.night_waking_triggers_list);
        coughWheezeTriggersContainer = findViewById(R.id.cough_wheeze_triggers_list);
        activityLimitTriggersContainer = findViewById(R.id.activity_limit_triggers_list);

        submitButton = findViewById(R.id.submit_check_in_button);
        editBtn = findViewById(R.id.editbtn);


        setupTriggerListener(
                nightWakingGroup,
                findViewById(R.id.night_waking_triggers_container),
                R.id.night_waking_no
        );

        setupTriggerListener(
                coughWheezeGroup,
                findViewById(R.id.cough_wheeze_triggers_container),
                R.id.cough_wheeze_none
        );

        setupTriggerListener(
                activityLimitGroup,
                findViewById(R.id.activity_limit_triggers_container),
                R.id.activity_limit_none
        );


        loadCheckInData();

        editBtn.setOnClickListener(v -> editData());
        submitButton.setOnClickListener(v -> saveCheckInData());
    }

    private void editData() {
        enableAllInputs(true);
        submitButton.setVisibility(View.VISIBLE);
        editBtn.setVisibility(View.GONE);
    }

    private void loadCheckInData() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference docRef = db.collection("DailyCheckIns")
                .document(childId)
                .collection("log")
                .document(todayDate);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {

                selectRadioButtonByText(nightWakingGroup, documentSnapshot.getString("NightWaking"));
                selectRadioButtonByText(coughWheezeGroup, documentSnapshot.getString("CoughWheeze"));
                selectRadioButtonByText(activityLimitGroup, documentSnapshot.getString("ActivityLimit"));


                selectCheckboxesByText(
                        nightWakingTriggersContainer,
                        getStringList(documentSnapshot, "NWTrigger")
                );
                selectCheckboxesByText(
                        coughWheezeTriggersContainer,
                        getStringList(documentSnapshot, "CWTrigger")
                );
                selectCheckboxesByText(
                        activityLimitTriggersContainer,
                        getStringList(documentSnapshot, "ALTrigger")
                );

                submitButton.setVisibility(View.GONE);
                editBtn.setVisibility(View.VISIBLE);
                enableAllInputs(false);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load existing data.", Toast.LENGTH_SHORT).show()
        );
    }

    private void enableAllInputs(boolean enabled) {
        enableRadioGroup(nightWakingGroup, enabled);
        enableRadioGroup(coughWheezeGroup, enabled);
        enableRadioGroup(activityLimitGroup, enabled);

        enableCheckboxGroup(nightWakingTriggersContainer, enabled);
        enableCheckboxGroup(coughWheezeTriggersContainer, enabled);
        enableCheckboxGroup(activityLimitTriggersContainer, enabled);
    }

    private void enableRadioGroup(RadioGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof RadioButton) {
                child.setEnabled(enabled);
            }
        }
    }

    private void enableCheckboxGroup(ViewGroup container, boolean enabled) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof CheckBox) {
                child.setEnabled(enabled);
            }
        }
    }

    private void saveCheckInData() {
        if (nightWakingGroup.getCheckedRadioButtonId() == -1 ||
                coughWheezeGroup.getCheckedRadioButtonId() == -1 ||
                activityLimitGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please fill out all required symptom fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("Author", "Parent");

        data.put("NightWaking", getSelectedText(nightWakingGroup));
        data.put("CoughWheeze", getSelectedText(coughWheezeGroup));
        data.put("ActivityLimit", getSelectedText(activityLimitGroup));

        if (nightWakingGroup.getCheckedRadioButtonId() == R.id.night_waking_no) {
            data.put("NWTrigger", new ArrayList<>());
        } else {
            data.put("NWTrigger", getSelectedTriggers(nightWakingTriggersContainer));
        }

        if (coughWheezeGroup.getCheckedRadioButtonId() == R.id.cough_wheeze_none) {
            data.put("CWTrigger", new ArrayList<>());
        } else {
            data.put("CWTrigger", getSelectedTriggers(coughWheezeTriggersContainer));
        }

        if (activityLimitGroup.getCheckedRadioButtonId() == R.id.activity_limit_none) {
            data.put("ALTrigger", new ArrayList<>());
        } else {
            data.put("ALTrigger", getSelectedTriggers(activityLimitTriggersContainer));
        }

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference docRef = db.collection("DailyCheckIns")
                .document(childId)
                .collection("log")
                .document(todayDate);

        docRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ParentDailyCheckIn.this, "Check-in saved!", Toast.LENGTH_SHORT).show();
                    enableAllInputs(false);
                    submitButton.setVisibility(View.GONE);
                    editBtn.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ParentDailyCheckIn.this, "Failed to save data.", Toast.LENGTH_SHORT).show()
                );
    }

    private String getSelectedText(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        if (selectedId == -1) return "";
        RadioButton selectedButton = findViewById(selectedId);
        return selectedButton.getText().toString();
    }

    private List<String> getSelectedTriggers(ViewGroup container) {
        List<String> triggers = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof CheckBox && ((CheckBox) child).isChecked()) {
                triggers.add(((CheckBox) child).getText().toString());
            }
        }
        return triggers;
    }

    private void setupTriggerListener(RadioGroup group, final View triggersContainer, final int noneOptionId) {
        group.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            if (checkedId == noneOptionId || checkedId == -1) {
                triggersContainer.setVisibility(View.GONE);
            } else {
                triggersContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void selectRadioButtonByText(RadioGroup group, String text) {
        if (text == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton button = (RadioButton) child;
                if (button.getText().toString().equals(text)) {
                    button.setChecked(true);
                    return;
                }
            }
        }
    }

    private void selectCheckboxesByText(ViewGroup container, List<String> texts) {
        if (texts == null) return;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) child;
                checkBox.setChecked(texts.contains(checkBox.getText().toString()));
            }
        }
    }


    private List<String> getStringList(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        List<String> result = new ArrayList<>();
        if (value instanceof List<?>) {
            for (Object o : (List<?>) value) {
                if (o instanceof String) {
                    result.add((String) o);
                }
            }
        }
        return result;
    }
}
