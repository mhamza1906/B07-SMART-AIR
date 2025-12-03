package com.example.smart_air;

import static com.itextpdf.layout.property.HorizontalAlignment.CENTER;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.type.DateTime;
import com.itextpdf.io.IOException;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import org.checkerframework.checker.units.qual.A;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class ExportChildHistoryActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private String childID;
    private String parentID;
    private Calendar calendar;
    int monthRange;
    PieChart zoneChart;
    Map<String,Integer> zoneMap = new HashMap<>();
    private int greenCount = 0;
    private int yellowCount = 0;
    private int redCount = 0;

    private int rescueNum = 0;
    private int controllerAdherence = 0;
    private int symptoms = 0;
    private int activityLimit = 0;
    private int coughWheeze = 0;
    private int nightWaking = 0;
    private int triage = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_child_history);

        childID = getIntent().getStringExtra("childID");

        if (childID == null || childID.isEmpty()) {
            Toast.makeText(this, "Child ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DatabaseReference childUserRef = FirebaseDatabase.getInstance().getReference("users").child(childID);

        childUserRef.child("parentID").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    parentID = snapshot.getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ExportChildHistoryActivity","Database error when finding ParentID"+error.getMessage());
            }
        });

        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        firestore = FirebaseFirestore.getInstance();
        zoneMap.put("Green",greenCount);
        zoneMap.put("Yellow",yellowCount);
        zoneMap.put("Red",redCount);
        ArrayList<Integer> zoneColors = new ArrayList<>();
        zoneColors.add(Color.parseColor("#00ff00"));
        zoneColors.add(Color.parseColor("#ff0000"));
        zoneColors.add(Color.parseColor("#ffff00"));

        TextView months = findViewById(R.id.TxtMonth);
        monthRange = Integer.parseInt((String) months.getText());
        Button increment = findViewById(R.id.incrementButton2);
        Button decrement = findViewById(R.id.decrementButton2);
        Button generatePDF = findViewById(R.id.PDFButton);
        zoneChart = findViewById(R.id.zonePie);

        increment.setOnClickListener(v -> {
            int n = Integer.parseInt(months.getText().toString());
            if (n < 6) {
                months.setText(String.valueOf(n + 1));
                updateMonths(n+1);
            }
            else Toast.makeText(this, "Maximum 6 months", Toast.LENGTH_SHORT).show();
        });

        decrement.setOnClickListener(v -> {
            int n = Integer.parseInt(months.getText().toString());
            if (n > 3) {
                months.setText(String.valueOf(n - 1));
                updateMonths(n-1);
            }
            else Toast.makeText(this, "Minimum 3 months", Toast.LENGTH_SHORT).show();
        });

        // Calculate zone here
        firestore.collection("PEF").document(childID).collection("log").orderBy("zone").get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String date = document.getId();
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                            Date compareDate;
                            try {
                                compareDate = format.parse(date);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }
                            Calendar compareCalendar = Calendar.getInstance();
                            compareCalendar.setTime(compareDate);
                            compareCalendar.set(Calendar.HOUR_OF_DAY, 0);
                            compareCalendar.set(Calendar.MINUTE, 0);
                            compareCalendar.set(Calendar.SECOND, 0);
                            compareCalendar.set(Calendar.MILLISECOND, 0);
                            int difference = (int) Duration.between(calendar.toInstant(),compareCalendar.toInstant()).toDays();
                            if (difference <= 30 * monthRange) {
                                String zone = (String) document.get("zone");

                                if ("green".equalsIgnoreCase(zone)) {
                                    greenCount++;
                                    zoneMap.put("Green", greenCount);
                                } else if ("yellow".equalsIgnoreCase(zone)) {
                                    yellowCount++;
                                    zoneMap.put("Yellow", yellowCount);
                                } else if ("red".equalsIgnoreCase(zone)) {
                                    redCount++;
                                    zoneMap.put("Red", redCount);
                                }
                            }
                        }
                    }
                });

        setUpPieChart();
        showPieChart("Zone distribution",zoneMap,zoneColors);

        generatePDF.setOnClickListener(v -> {
            /* Needed values:
             *  Rescue frequency in last 3/4/5/6 months: x rescue uses - how to get? check document ids within month range, check if rescue exists
             *  - if it does, count number of doses
             *  Controller adherence: x% - calculated from planned-schedule, check document ids similarly to rescue frequency,
             *  - increment 2 separate variables, one for each log, one for each log that is taken, calculate accordingly
             *  Symptom burden: x problem days - Use DailyCheckIns collection, increment if activitylimit/coughwheeze/nightwaking is yes
             *  Notable triage incidents: x incidents - use triage_incidents, increment if any red flag is present*/

            // Display controller adherence over time as a time-series line chart
            // Display zone distribution as a pie chart
//            int rescueNum = 0;
//            int controllerAdherence = 0;
//            int symptoms = 0;
//            int activityLimit = 0,coughWheeze = 0,nightWaking = 0;
//            int triage = 0;

            firestore.collection("medlog").document(childID).collection("log").orderBy("rescue").get()
                    .addOnCompleteListener(task -> {
                       if(task.isSuccessful()) {
                           for (QueryDocumentSnapshot document : task.getResult()) {
                               String date = document.getId();
                               SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                               Date compareDate;
                               try {
                                   compareDate = format.parse(date);
                               } catch (ParseException e) {
                                   throw new RuntimeException(e);
                               }
                               Calendar compareCalendar = Calendar.getInstance();
                               compareCalendar.setTime(compareDate);
                               compareCalendar.set(Calendar.HOUR_OF_DAY, 0);
                               compareCalendar.set(Calendar.MINUTE, 0);
                               compareCalendar.set(Calendar.SECOND, 0);
                               compareCalendar.set(Calendar.MILLISECOND, 0);
                               int difference = (int) Duration.between(calendar.toInstant(),compareCalendar.toInstant()).toDays();
                               if(difference<=30*monthRange) {
                                   Map rescues = (Map) document.get("rescue");
                                   rescueNum += rescues.size();
                               }
                           }
                       }
                    });

            firestore.collection("planned-schedule").document(childID).collection("Schedules").orderBy("taken").get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            int total = 0;
                            int adhered = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String date = document.getId();
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                                Date compareDate;
                                try {
                                    compareDate = format.parse(date);
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                                Calendar compareCalendar = Calendar.getInstance();
                                compareCalendar.setTime(compareDate);
                                compareCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                compareCalendar.set(Calendar.MINUTE, 0);
                                compareCalendar.set(Calendar.SECOND, 0);
                                compareCalendar.set(Calendar.MILLISECOND, 0);
                                int difference = (int) Duration.between(calendar.toInstant(),compareCalendar.toInstant()).toDays();
                                if(difference<=30*monthRange) {
                                    total += 1;
                                    if((boolean) document.get("taken")) adhered += 1;
                                }
                            }
                            int adheredPercent = adhered/total * 100;
//                            updateCount(controllerAdherence,adheredPercent);
                            controllerAdherence+= adheredPercent;
                        }
                    });

            firestore.collection("DailyCheckIns").document(childID).collection("log").orderBy("ActivityLimit").get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String date = document.getId();
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                                Date compareDate;
                                try {
                                    compareDate = format.parse(date);
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                                Calendar compareCalendar = Calendar.getInstance();
                                compareCalendar.setTime(compareDate);
                                compareCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                compareCalendar.set(Calendar.MINUTE, 0);
                                compareCalendar.set(Calendar.SECOND, 0);
                                compareCalendar.set(Calendar.MILLISECOND, 0);
                                int difference = (int) Duration.between(calendar.toInstant(),compareCalendar.toInstant()).toDays();
                                if(difference<=30*monthRange) {
                                    Boolean problem = false;
                                    String activity = (String) document.get("ActivityLimit");
                                    String cough = (String) document.get("CoughWheeze");
                                    String nightWake = (String) document.get("NightWaking");
                                    if(activity=="Yes") {
                                        problem = true;
//                                        updateCount(activityLimit,1);
                                        activityLimit++;
                                    }
                                    if(cough=="Yes") {
                                        problem = true;
//                                        updateCount(coughWheeze,1);
                                        coughWheeze++;
                                    }
                                    if(nightWake=="Yes") {
                                        problem = true;
//                                        updateCount(nightWaking,1);
                                        nightWaking++;
                                    }
                                    if(problem){
                                        symptoms++;
                                    }
                                }
                            }
                        }
                    });

            firestore.collection("triage_incidents").document(childID).collection("incident_log").orderBy("redFlagsPresent").get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Timestamp compareTime = (Timestamp) document.get("timestamp");
                                Calendar compareCalendar = Calendar.getInstance();
                                compareCalendar.setTime(compareTime.toDate());
                                compareCalendar.set(Calendar.HOUR_OF_DAY, 0);
                                compareCalendar.set(Calendar.MINUTE, 0);
                                compareCalendar.set(Calendar.SECOND, 0);
                                compareCalendar.set(Calendar.MILLISECOND, 0);
                                int difference = (int) Duration.between(calendar.toInstant(),compareCalendar.toInstant()).toDays();
                                if(difference<=30*monthRange) {
                                    Map redFlags = (Map) document.get("redFlagsPresent");
                                    for (Object key : redFlags.keySet()) {
                                        if((Boolean) redFlags.get(key)) {
//                                            updateCount(triage++,1);
                                            triage++;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    });

            try {
                createPDF(rescueNum,controllerAdherence,symptoms,triage);
            } catch (FileNotFoundException e) {
                Toast.makeText(this,"File not found",Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });
    }

    public void updateMonths(int newMonth) {
        monthRange = newMonth;
    }

//    public void updateCount(int count, int update) {
//        count = count + update;
//    }

    public int updateCount(int count, int update) {
        return count + update;
    }

    public void setUpPieChart() {
        zoneChart.setUsePercentValues(true);
        zoneChart.getDescription().setEnabled(false);
        zoneChart.setHoleRadius(0f);
        zoneChart.setTransparentCircleRadius(0f);
    }

    public void showPieChart(String label, Map<String,Integer> zoneMap, ArrayList<Integer> colors) {
        ArrayList<PieEntry> zoneEntries = new ArrayList<>();
        for(String zone: zoneMap.keySet()) {
            zoneEntries.add(new PieEntry(zoneMap.get(zone).floatValue(),zone));
        }
        PieDataSet zoneDataSet = new PieDataSet(zoneEntries,label);
        zoneDataSet.setValueTextSize(12f);
        zoneDataSet.setColors(colors);
        PieData zoneData = new PieData(zoneDataSet);
        zoneData.setValueFormatter(new PercentFormatter());
        zoneData.setDrawValues(true);

        zoneChart.setData(zoneData);
        zoneChart.invalidate();
    }

    public void createPDF(int rescueFreq, int controllerAdherence, int symptomBurden, int triageIncidents) throws FileNotFoundException {
        FirebaseDatabase.getInstance().getReference("users").child(childID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String fName = snapshot.child("fName").getValue(String.class);
                        String lName = snapshot.child("lName").getValue(String.class);
                        String fullName = "";
                        if (fName != null && !fName.isEmpty()) {
                            fullName = fullName + fName;
                        }
                        if (lName != null && !lName.isEmpty()) {
                            fullName = fullName + lName;
                        }
                        try {
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().getTime());
                            String fileName = "Provider-Report-" + fullName.trim().replace(" ", "_") + "-" + timeStamp + ".pdf";
                            String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                            File file = new File(pdfPath,fileName);

                            PdfWriter writer = new PdfWriter(file);
                            PdfDocument pdfDocument = new PdfDocument(writer);
                            Document document = new Document(pdfDocument);

                            document.add(new Paragraph("Provider Report").setBold().setFontSize(20));
                            document.add(new Paragraph("Report for: "+fullName.trim()).setFontSize(12).setMarginBottom(20));
                            document.add(new Paragraph("Report of last "+Integer.toString(monthRange)+" months").setBold().setFontSize(20));

                            Table reportTable = new Table(4);
                            reportTable.addHeaderCell("Rescue Frequency");
                            reportTable.addHeaderCell("Controller Adherence Percentage");
                            reportTable.addHeaderCell("Symptom Burden");
                            reportTable.addHeaderCell("Notable Triage Incidents");

                            reportTable.addCell(Integer.toString(rescueFreq)+" Days of Rescue Use");
                            reportTable.addCell(Integer.toString(controllerAdherence)+"%");
                            reportTable.addCell(Integer.toString(symptomBurden)+" Problem Days");
                            reportTable.addCell(Integer.toString(triageIncidents) +" Notable Triage Incidents");

                            document.add(reportTable);

                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            Bitmap zoneBitmap = zoneChart.getChartBitmap();
                            zoneBitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
                            byte[] zoneBitmapData = stream.toByteArray();

                            ImageData imageData = ImageDataFactory.create(zoneBitmapData);
                            Image zoneImage = new Image(imageData);
                            zoneImage.setHeight(100);
                            zoneImage.setWidth(100);
                            zoneImage.setHorizontalAlignment(CENTER);

                            document.close();

                            Toast.makeText(ExportChildHistoryActivity.this, "PDF saved to Downloads folder: " + fileName, Toast.LENGTH_LONG).show();

                        } catch(FileNotFoundException e) {
                            Log.e("ExportChildHistoryActivity", "Error creating PDF", e);
                            Toast.makeText(ExportChildHistoryActivity.this, "Failed to create PDF. Check permissions and storage.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("ExportChildHistoryActivity", "Failed to read user name for PDF.", error.toException());
                        Toast.makeText(ExportChildHistoryActivity.this, "Could not fetch child's name for PDF.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
