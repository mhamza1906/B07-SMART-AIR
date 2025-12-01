package com.example.smart_air;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class RefillActivity extends AppCompatActivity {

    private String childId;
    private FirebaseFirestore db;

    // Controller EditTexts
    private EditText controllerPurchaseDateInput, controllerAmountLeftInput, controllerExpiryDateInput, controllerReplacementReminderInput;

    // Rescue EditTexts
    private EditText rescuePurchaseDateInput, rescueAmountLeftInput, rescueExpiryDateInput, rescueReplacementReminderInput;

    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refill);

        childId = getIntent().getStringExtra("childID");
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initializeViews();
        loadInventoryData();

        saveButton.setOnClickListener(v -> saveInventoryData());
    }

    private void initializeViews() {
        // Controller Inputs
        controllerPurchaseDateInput = findViewById(R.id.controller_purchase_date_input);
        controllerAmountLeftInput = findViewById(R.id.controller_amount_left_input);
        controllerExpiryDateInput = findViewById(R.id.controller_expiry_date_input);
        controllerReplacementReminderInput = findViewById(R.id.controller_replacement_reminder_input);

        // Rescue Inputs
        rescuePurchaseDateInput = findViewById(R.id.rescue_purchase_date_input);
        rescueAmountLeftInput = findViewById(R.id.rescue_amount_left_input);
        rescueExpiryDateInput = findViewById(R.id.rescue_expiry_date_input);
        rescueReplacementReminderInput = findViewById(R.id.rescue_replacement_reminder_input);

        // Save Button
        saveButton = findViewById(R.id.save_button);
    }

    private void loadInventoryData() {
        DocumentReference inventoryDoc = db.collection("inventory").document(childId);
        inventoryDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> controllerData = (Map<String, Object>) documentSnapshot.get("controller");
                Map<String, Object> rescueData = (Map<String, Object>) documentSnapshot.get("rescue");

                if (controllerData != null) {
                    controllerPurchaseDateInput.setText(String.valueOf(controllerData.getOrDefault("purchaseDate", "")));
                    controllerAmountLeftInput.setText(String.valueOf(controllerData.getOrDefault("amountLeft", "")));
                    controllerExpiryDateInput.setText(String.valueOf(controllerData.getOrDefault("expiry", "")));
                    controllerReplacementReminderInput.setText(String.valueOf(controllerData.getOrDefault("replacementReminder", "")));
                }

                if (rescueData != null) {
                    rescuePurchaseDateInput.setText(String.valueOf(rescueData.getOrDefault("purchaseDate", "")));
                    rescueAmountLeftInput.setText(String.valueOf(rescueData.getOrDefault("amountLeft", "")));
                    rescueExpiryDateInput.setText(String.valueOf(rescueData.getOrDefault("expiry", "")));
                    rescueReplacementReminderInput.setText(String.valueOf(rescueData.getOrDefault("replacementReminder", "")));
                }
            }
        });
    }

    private void saveInventoryData() {
        // Controller Data
        Map<String, Object> controllerMap = new HashMap<>();
        controllerMap.put("purchaseDate", controllerPurchaseDateInput.getText().toString());
        controllerMap.put("amountLeft", Long.parseLong(controllerAmountLeftInput.getText().toString()));
        controllerMap.put("expiry", controllerExpiryDateInput.getText().toString());
        controllerMap.put("replacementReminder", controllerReplacementReminderInput.getText().toString());

        // Rescue Data
        Map<String, Object> rescueMap = new HashMap<>();
        rescueMap.put("purchaseDate", rescuePurchaseDateInput.getText().toString());
        rescueMap.put("amountLeft", Long.parseLong(rescueAmountLeftInput.getText().toString()));
        rescueMap.put("expiry", rescueExpiryDateInput.getText().toString());
        rescueMap.put("replacementReminder", rescueReplacementReminderInput.getText().toString());

        Map<String, Object> inventoryData = new HashMap<>();
        inventoryData.put("controller", controllerMap);
        inventoryData.put("rescue", rescueMap);

        DocumentReference inventoryDoc = db.collection("inventory").document(childId);
        inventoryDoc.set(inventoryData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RefillActivity.this, "Inventory updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the previous activity
                })
                .addOnFailureListener(e -> Toast.makeText(RefillActivity.this, "Error updating inventory.", Toast.LENGTH_SHORT).show());
    }
}
