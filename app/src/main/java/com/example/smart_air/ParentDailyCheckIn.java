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

    private RadioGroup groupNightWaking;
    private RadioGroup groupCoughWheeze;
    private RadioGroup groupActivityLimit;

    private ViewGroup listNightWakingTriggers;
    private ViewGroup listCoughWheezeTriggers;
    private ViewGroup listActivityLimitTriggers;

    private View containerNightWakingTriggers;
    private View containerCoughWheezeTriggers;
    private View containerActivityLimitTriggers;

    private View txtNightWakingScrollHint;
    private View txtCoughWheezeScrollHint;
    private View txtActivityLimitScrollHint;

    private View txtNightWakingTriggersLabel;
    private View txtCoughWheezeTriggersLabel;
    private View txtActivityLimitTriggersLabel;

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


        groupNightWaking = findViewById(R.id.group_night_waking);
        groupCoughWheeze = findViewById(R.id.group_cough_wheeze);
        groupActivityLimit = findViewById(R.id.group_activity_limit);

        listNightWakingTriggers = findViewById(R.id.list_night_waking_triggers);
        listCoughWheezeTriggers = findViewById(R.id.list_cough_wheeze_triggers);
        listActivityLimitTriggers = findViewById(R.id.list_activity_limit_triggers);

        containerNightWakingTriggers = findViewById(R.id.container_night_waking_triggers);
        containerCoughWheezeTriggers = findViewById(R.id.container_cough_wheeze_triggers);
        containerActivityLimitTriggers = findViewById(R.id.container_activity_limit_triggers);

        txtNightWakingScrollHint = findViewById(R.id.txt_night_waking_scroll_hint);
        txtCoughWheezeScrollHint = findViewById(R.id.txt_cough_wheeze_scroll_hint);
        txtActivityLimitScrollHint = findViewById(R.id.txt_activity_limit_scroll_hint);

        txtNightWakingTriggersLabel = findViewById(R.id.txt_nw_triggers_label);
        txtCoughWheezeTriggersLabel = findViewById(R.id.txt_cw_triggers_label);
        txtActivityLimitTriggersLabel = findViewById(R.id.txt_al_triggers_label);

        btnSubmit = findViewById(R.id.btn_submit_check_in);
        btnEdit = findViewById(R.id.btn_edit_check_in);

        setupTriggerListener(
                groupNightWaking,
                containerNightWakingTriggers,
                txtNightWakingScrollHint,
                txtNightWakingTriggersLabel,
                R.id.radio_night_waking_no
        );

        setupTriggerListener(
                groupCoughWheeze,
                containerCoughWheezeTriggers,
                txtCoughWheezeScrollHint,
                txtCoughWheezeTriggersLabel,
                R.id.radio_cough_wheeze_none
        );

        setupTriggerListener(
                groupActivityLimit,
                containerActivityLimitTriggers,
                txtActivityLimitScrollHint,
                txtActivityLimitTriggersLabel,
                R.id.radio_activity_none
        );

        loadCheckInData();

        btnEdit.setOnClickListener(v -> editData());
        btnSubmit.setOnClickListener(v -> saveCheckInData());
    }

    private void loadCheckInData() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DocumentReference docRef = db.collection("DailyCheckIns")
                .document(childId).collection("log").document(todayDate);

        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {

                selectRadioButtonByText(groupNightWaking, doc.getString("NightWaking"));
                selectRadioButtonByText(groupCoughWheeze, doc.getString("CoughWheeze"));
                selectRadioButtonByText(groupActivityLimit, doc.getString("ActivityLimit"));

                selectCheckboxesByText(listNightWakingTriggers, getStringList(doc, "NWTrigger"));
                selectCheckboxesByText(listCoughWheezeTriggers, getStringList(doc, "CWTrigger"));
                selectCheckboxesByText(listActivityLimitTriggers, getStringList(doc, "ALTrigger"));

                btnSubmit.setVisibility(View.GONE);
                btnEdit.setVisibility(View.VISIBLE);

                enableAllInputs(false);


                showLoadedTriggers(groupNightWaking, containerNightWakingTriggers, txtNightWakingScrollHint, txtNightWakingTriggersLabel, R.id.radio_night_waking_no);
                showLoadedTriggers(groupCoughWheeze, containerCoughWheezeTriggers, txtCoughWheezeScrollHint, txtCoughWheezeTriggersLabel, R.id.radio_cough_wheeze_none);
                showLoadedTriggers(groupActivityLimit, containerActivityLimitTriggers, txtActivityLimitScrollHint, txtActivityLimitTriggersLabel, R.id.radio_activity_none);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load existing data.", Toast.LENGTH_SHORT).show()
        );
    }

    private void showLoadedTriggers(RadioGroup group, View container, View hint, View label, int noneOptionId) {
        int id = group.getCheckedRadioButtonId();
        if (id != noneOptionId && id != -1) {
            container.setVisibility(View.VISIBLE);
            hint.setVisibility(View.VISIBLE);
            label.setVisibility(View.VISIBLE);
        }
    }

    private void editData() {
        enableAllInputs(true);
        btnSubmit.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);
    }

    private void enableAllInputs(boolean enabled) {
        enableRadioGroup(groupNightWaking, enabled);
        enableRadioGroup(groupCoughWheeze, enabled);
        enableRadioGroup(groupActivityLimit, enabled);

        enableCheckboxGroup(listNightWakingTriggers, enabled);
        enableCheckboxGroup(listCoughWheezeTriggers, enabled);
        enableCheckboxGroup(listActivityLimitTriggers, enabled);
    }

    private void enableRadioGroup(RadioGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
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
        data.put("Author", "Parent");

        data.put("NightWaking", getSelectedText(groupNightWaking));
        data.put("CoughWheeze", getSelectedText(groupCoughWheeze));
        data.put("ActivityLimit", getSelectedText(groupActivityLimit));

        if (groupNightWaking.getCheckedRadioButtonId() == R.id.radio_night_waking_no)
            data.put("NWTrigger", new ArrayList<>());
        else
            data.put("NWTrigger", getSelectedTriggers(listNightWakingTriggers));

        if (groupCoughWheeze.getCheckedRadioButtonId() == R.id.radio_cough_wheeze_none)
            data.put("CWTrigger", new ArrayList<>());
        else
            data.put("CWTrigger", getSelectedTriggers(listCoughWheezeTriggers));

        if (groupActivityLimit.getCheckedRadioButtonId() == R.id.radio_activity_none)
            data.put("ALTrigger", new ArrayList<>());
        else
            data.put("ALTrigger", getSelectedTriggers(listActivityLimitTriggers));

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference docRef = db.collection("DailyCheckIns")
                .document(childId).collection("log").document(todayDate);

        docRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ParentDailyCheckIn.this, "Check-in saved!", Toast.LENGTH_SHORT).show();
                    enableAllInputs(false);
                    btnSubmit.setVisibility(View.GONE);
                    btnEdit.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ParentDailyCheckIn.this, "Failed to save data.", Toast.LENGTH_SHORT).show()
                );
    }

    private String getSelectedText(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return "";
        RadioButton rb = findViewById(id);
        return rb.getText().toString();
    }

    private List<String> getSelectedTriggers(ViewGroup container) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox) v).isChecked())
                list.add(((CheckBox) v).getText().toString());
        }
        return list;
    }

    private void setupTriggerListener(
            RadioGroup group,
            View triggersContainer,
            View scrollHint,
            View triggersLabel,
            int noneOptionId
    ) {
        group.setOnCheckedChangeListener((g, checkedId) -> {
            if (checkedId == noneOptionId || checkedId == -1) {
                triggersContainer.setVisibility(View.GONE);
                scrollHint.setVisibility(View.GONE);
                triggersLabel.setVisibility(View.GONE);
            } else {
                triggersContainer.setVisibility(View.VISIBLE);
                scrollHint.setVisibility(View.VISIBLE);
                triggersLabel.setVisibility(View.VISIBLE);
            }
        });
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
