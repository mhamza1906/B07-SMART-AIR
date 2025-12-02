package com.example.smart_air;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HistoryBrowserActivity extends AppCompatActivity {

    private static final String TAG = "HistoryBrowserActivity";
    private FirebaseFirestore db;
    private String childId;

    // Main UI
    private Button btnShowCheckinHistory, btnShowPefHistory;
    private CardView cardCheckinHistory, cardPefHistory;

    // PEF History
    private RecyclerView recyclerPefHistory;
    private PefHistoryAdapter pefHistoryAdapter;
    private List<PefHistoryItem> pefHistoryList;

    // Check-in History
    private RecyclerView recyclerCheckinHistory;
    private DailyCheckinHistoryAdapter checkinHistoryAdapter;
    private List<DailyCheckinHistoryItem> checkinHistoryList;
    private Button btnToggleSymptoms, btnToggleTriggers, btnToggleDate, btnApplyFilters, btnClearFilters, btnExportPdf;
    private LinearLayout containerSymptomFilters, containerTriggerFilters, containerDateRangeFilter;
    private Button btnStartDate, btnEndDate;
    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_browser);

        childId = getIntent().getStringExtra("childID");
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "Child ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupPefRecycler();
        setupCheckinRecycler();
        setupClickListeners();
    }

    private void bindViews() {
        // Main toggle buttons
        btnShowCheckinHistory = findViewById(R.id.btn_show_checkin_history);
        btnShowPefHistory = findViewById(R.id.btn_show_pef_history);

        // History section cards
        cardCheckinHistory = findViewById(R.id.card_checkin_history);
        cardPefHistory = findViewById(R.id.card_pef_history);

        // PEF History
        recyclerPefHistory = findViewById(R.id.recycler_pef_history);

        // Check-in History
        recyclerCheckinHistory = findViewById(R.id.recycler_checkin_history);
        btnToggleSymptoms = findViewById(R.id.btn_toggle_symptoms_filter);
        btnToggleTriggers = findViewById(R.id.btn_toggle_triggers_filter);
        btnToggleDate = findViewById(R.id.btn_toggle_date_filter);
        btnApplyFilters = findViewById(R.id.btn_apply_filters);
        btnClearFilters = findViewById(R.id.btn_clear_filters);
        btnExportPdf = findViewById(R.id.btn_export_pdf);
        containerSymptomFilters = findViewById(R.id.container_symptom_filters);
        containerTriggerFilters = findViewById(R.id.container_trigger_filters);
        containerDateRangeFilter = findViewById(R.id.container_date_range_filter);
        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
    }

    private void setupPefRecycler() {
        pefHistoryList = new ArrayList<>();
        pefHistoryAdapter = new PefHistoryAdapter(pefHistoryList);
        recyclerPefHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerPefHistory.setAdapter(pefHistoryAdapter);
    }

    private void setupCheckinRecycler() {
        checkinHistoryList = new ArrayList<>();
        checkinHistoryAdapter = new DailyCheckinHistoryAdapter(checkinHistoryList);
        recyclerCheckinHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerCheckinHistory.setAdapter(checkinHistoryAdapter);
    }

    private void setupClickListeners() {
        btnShowPefHistory.setOnClickListener(v -> {
            cardPefHistory.setVisibility(View.VISIBLE);
            cardCheckinHistory.setVisibility(View.GONE);
            loadPefHistory();
        });

        btnShowCheckinHistory.setOnClickListener(v -> {
            cardCheckinHistory.setVisibility(View.VISIBLE);
            cardPefHistory.setVisibility(View.GONE);
            loadCheckinHistory(false); //Initial load without filters
        });

        //Filter toggles
        setupFilterToggleListener(btnToggleSymptoms, containerSymptomFilters, containerTriggerFilters, containerDateRangeFilter);
        setupFilterToggleListener(btnToggleTriggers, containerTriggerFilters, containerSymptomFilters, containerDateRangeFilter);
        setupFilterToggleListener(btnToggleDate, containerDateRangeFilter, containerSymptomFilters, containerTriggerFilters);

        //Date pickers
        btnStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        btnEndDate.setOnClickListener(v -> showDatePickerDialog(false));

        //Action buttons
        btnApplyFilters.setOnClickListener(v -> {
            loadCheckinHistory(true);
            containerSymptomFilters.setVisibility(View.GONE);
            containerTriggerFilters.setVisibility(View.GONE);
            containerDateRangeFilter.setVisibility(View.GONE);
        });
        btnClearFilters.setOnClickListener(v -> {
            clearFilters();
            loadCheckinHistory(false);
        });
        btnExportPdf.setOnClickListener(v -> exportCheckinHistoryToPdf());
    }

    private void exportCheckinHistoryToPdf() {
        if (checkinHistoryList == null || checkinHistoryList.isEmpty()) {
            Toast.makeText(this, "No history data to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().getTime());
            String fileName = "Check-in-History-" + timeStamp + ".pdf";
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);


            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.add(new Paragraph("Daily Check-in History").setBold().setFontSize(18));


            Table table = new Table(4);
            table.addHeaderCell("Date");
            table.addHeaderCell("Author");
            table.addHeaderCell("Symptoms");
            table.addHeaderCell("Triggers");

            for (DailyCheckinHistoryItem item : checkinHistoryList) {
                table.addCell(item.getDate());
                table.addCell(item.getAuthor());
                table.addCell(String.join(", ", item.getSymptoms()));
                table.addCell(String.join(", ", item.getTriggers()));
            }

            document.add(table);
            document.close();

            Toast.makeText(this, "PDF saved to Downloads folder: " + fileName, Toast.LENGTH_LONG).show();

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error creating PDF", e);
            Toast.makeText(this, "Failed to create PDF. Check permissions and storage.", Toast.LENGTH_LONG).show();
        }
    }

    private void clearFilters() {
        // Clear symptom checkboxes
        ((CheckBox) findViewById(R.id.check_symptom_night_waking)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_symptom_cough_wheeze)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_symptom_activity_limit)).setChecked(false);

        // Clear trigger checkboxes
        ((CheckBox) findViewById(R.id.check_trigger_dust)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_trigger_pets)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_trigger_smoke)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_trigger_odors)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_trigger_cold_air)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_trigger_illness)).setChecked(false);
        ((CheckBox) findViewById(R.id.check_trigger_exercise)).setChecked(false);

        // Clear dates
        startDate = null;
        endDate = null;
        btnStartDate.setText("Start Date");
        btnEndDate.setText("End Date");
    }

    private void setupFilterToggleListener(Button button, final View targetContainer, final View otherContainer1, final View otherContainer2) {
        button.setOnClickListener(v -> {
            if (targetContainer.getVisibility() == View.VISIBLE) {
                targetContainer.setVisibility(View.GONE);
            } else {
                targetContainer.setVisibility(View.VISIBLE);
                otherContainer1.setVisibility(View.GONE);
                otherContainer2.setVisibility(View.GONE);
            }
        });
    }

    private void showDatePickerDialog(final boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    if (isStartDate) {
                        startDate = selectedDate;
                        btnStartDate.setText(dateFormat.format(startDate.getTime()));
                    }
                    else {
                        endDate = selectedDate;
                        btnEndDate.setText(dateFormat.format(endDate.getTime()));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void loadPefHistory() {
        db.collection("PEF").document(childId).collection("zone_history")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        Toast.makeText(this, "No PEF history found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pefHistoryList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        pefHistoryList.add(new PefHistoryItem(doc.getDate("timestamp"), doc.getLong("percent").intValue(), doc.getString("zone")));
                    }
                    pefHistoryAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading PEF history", e);
                    Toast.makeText(this, "Failed to load PEF history. Check Logcat for details.", Toast.LENGTH_LONG).show();
                });
    }

    private void loadCheckinHistory(boolean applyFilters) {
        Query query = db.collection("DailyCheckIns").document(childId).collection("log");
        
        final List<String> selectedSymptoms = getSelectedSymptoms();
        final List<String> selectedTriggers = getSelectedTriggers();
        boolean isDateFilterSet = startDate != null && endDate != null;
        boolean hasSymptomOrTriggerFilter = !selectedSymptoms.isEmpty() || !selectedTriggers.isEmpty();

        if (applyFilters && (isDateFilterSet || hasSymptomOrTriggerFilter)) {
            if (isDateFilterSet) {
                if (startDate.after(endDate)) {
                    Toast.makeText(this, "Start date must be before end date.", Toast.LENGTH_SHORT).show();
                    return;
                }

                long diffInMillis = Math.abs(endDate.getTimeInMillis() - startDate.getTimeInMillis());
                long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

                if (diffInDays < 90 || diffInDays > 180) {
                    Toast.makeText(this, "Date range must be between 3 and 6 months.", Toast.LENGTH_SHORT).show();
                    return;
                }

                query = query.whereGreaterThanOrEqualTo(FieldPath.documentId(), dateFormat.format(startDate.getTime())).whereLessThanOrEqualTo(FieldPath.documentId(), dateFormat.format(endDate.getTime()));
            }
            else if (startDate != null || endDate != null) {
                Toast.makeText(this, "Please select both a start and an end date.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        else {
            // --- Default Query Logic ---
            query = query.orderBy(FieldPath.documentId(), Query.Direction.DESCENDING).limit(50);
        }

        query.get().addOnSuccessListener(snapshots -> {
            if (snapshots.isEmpty()) {
                checkinHistoryList.clear();
                checkinHistoryAdapter.notifyDataSetChanged();
                Toast.makeText(this, "No matching check-in history found.", Toast.LENGTH_SHORT).show();
                return;
            }
            checkinHistoryList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                if (applyFilters && (isDateFilterSet || hasSymptomOrTriggerFilter)) {
                    boolean symptomsMatch = true;
                    boolean triggersMatch = true;

                    if (!selectedSymptoms.isEmpty()) {
                        symptomsMatch = checkSymptomsMatch(doc, selectedSymptoms);
                    }

                    if (!selectedTriggers.isEmpty()) {
                        triggersMatch = checkTriggersMatch(doc, selectedTriggers);
                    }
                    if (!symptomsMatch || !triggersMatch) {
                        continue; // Skip this document if it doesn't match filters
                    }
                }

                List<String> symptoms = new ArrayList<>();
                if ("Yes".equals(doc.getString("NightWaking")))
                    symptoms.add("Night Waking");
                if ("Yes".equals(doc.getString("CoughWheeze")))
                    symptoms.add("Cough/Wheeze");
                if ("Yes".equals(doc.getString("ActivityLimit")))
                    symptoms.add("Limited Activity");

                if (symptoms.isEmpty()){
                    symptoms.add("None");
                }

                List<String> triggers = getStringList(doc, "Triggers");
                if (triggers.isEmpty()) {
                    triggers.add("None");
                }



                checkinHistoryList.add(new DailyCheckinHistoryItem(doc.getId(), doc.getString("Author"), symptoms, triggers));
            }
            if(checkinHistoryList.isEmpty()){
                Toast.makeText(this, "No entries match the selected filters.", Toast.LENGTH_SHORT).show();
            }
            checkinHistoryAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading check-in history", e);
            Toast.makeText(this, "Failed to load check-in history. Check Logcat for details.", Toast.LENGTH_LONG).show();
        });
    }

    private List<String> getSelectedSymptoms() {
        List<String> list = new ArrayList<>();
        if (((CheckBox) findViewById(R.id.check_symptom_night_waking)).isChecked()) {
            list.add("NightWaking");
        }
        if (((CheckBox) findViewById(R.id.check_symptom_cough_wheeze)).isChecked()) {
            list.add("CoughWheeze");
        }
        if (((CheckBox) findViewById(R.id.check_symptom_activity_limit)).isChecked()) {
            list.add("ActivityLimit");
        }
        return list;
    }

    private boolean checkSymptomsMatch(DocumentSnapshot doc, List<String> selected) {
        for (String symptomField : selected) {
            if (!"Yes".equals(doc.getString(symptomField))) {
                return false;
            }
        }
        return true;
    }

    private List<String> getSelectedTriggers() {
        List<String> list = new ArrayList<>();
        if (((CheckBox) findViewById(R.id.check_trigger_dust)).isChecked()) {
            list.add("Dust");
        }
        if (((CheckBox) findViewById(R.id.check_trigger_pets)).isChecked()) {
            list.add("Pets");
        }
        if (((CheckBox) findViewById(R.id.check_trigger_smoke)).isChecked()) {
            list.add("Smoke");
        }
        if (((CheckBox) findViewById(R.id.check_trigger_odors)).isChecked()) {
            list.add("Strong odor");
        }
        if (((CheckBox) findViewById(R.id.check_trigger_cold_air)).isChecked()) {
            list.add("Cold air");
        }
        if (((CheckBox) findViewById(R.id.check_trigger_illness)).isChecked()) {
            list.add("Illness");
        }
        if (((CheckBox) findViewById(R.id.check_trigger_exercise)).isChecked()) {
            list.add("Exercise");
        }
        return list;
    }

    private boolean checkTriggersMatch(DocumentSnapshot doc, List<String> selected) {
        List<String> docTriggers = getStringList(doc, "Triggers");
        return docTriggers.containsAll(selected);
    }

    private List<String> getStringList(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        if (value instanceof List<?>) {
            List<String> result = new ArrayList<>();
            for (Object o : (List<?>) value) {
                if (o instanceof String) {
                    result.add((String) o);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
}
