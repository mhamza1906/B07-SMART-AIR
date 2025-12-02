package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class InventoryLogActivity extends AppCompatActivity {
    private String childId;
    private FirebaseFirestore db;

    private TextView controllerPurchaseDate, controllerAmountLeft, controllerExpiryDate, controllerReplacementReminder;
    private TextView rescuePurchaseDate, rescueAmountLeft, rescueExpiryDate, rescueReplacementReminder;
    private Button refillButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_log);

        childId = getIntent().getStringExtra("childID");
        if (childId == null || childId.isEmpty()) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initializeViews();

        refillButton.setOnClickListener(v -> {
            Intent intent = new Intent(InventoryLogActivity.this, RefillActivity.class);
            intent.putExtra("childID", childId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchInventoryData();
    }

    private void initializeViews() {
        controllerPurchaseDate = findViewById(R.id.controller_purchase_date);
        controllerAmountLeft = findViewById(R.id.controller_amount_left);
        controllerExpiryDate = findViewById(R.id.controller_expiry_date);
        controllerReplacementReminder = findViewById(R.id.controller_replacement_reminder);

        rescuePurchaseDate = findViewById(R.id.rescue_purchase_date);
        rescueAmountLeft = findViewById(R.id.rescue_amount_left);
        rescueExpiryDate = findViewById(R.id.rescue_expiry_date);
        rescueReplacementReminder = findViewById(R.id.rescue_replacement_reminder);

        refillButton = findViewById(R.id.refill_button);
    }

    private void fetchInventoryData() {
        DocumentReference inventoryDoc = db.collection("inventory").document(childId);

        inventoryDoc.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Map<String, Object> controllerData = (Map<String, Object>) document.get("controller");
                    Map<String, Object> rescueData = (Map<String, Object>) document.get("rescue");

                    populateView(controllerData, rescueData);
                } else {
                    populateView(null, null);
                }
            } else {
                Toast.makeText(InventoryLogActivity.this, "Error fetching inventory data.", Toast.LENGTH_SHORT).show();
                populateView(null, null);
            }
        });
    }

    private void populateView(Map<String, Object> controllerData, Map<String, Object> rescueData) {
        if (controllerData != null) {
            controllerPurchaseDate.setText(String.valueOf(controllerData.getOrDefault("purchaseDate", "")));
            controllerAmountLeft.setText(String.valueOf(controllerData.getOrDefault("amountLeft", "")));
            controllerExpiryDate.setText(String.valueOf(controllerData.getOrDefault("expiry", "")));
            controllerReplacementReminder.setText(String.valueOf(controllerData.getOrDefault("replacementReminder", "")));
        } else {
            controllerPurchaseDate.setText("");
            controllerAmountLeft.setText("");
            controllerExpiryDate.setText("");
            controllerReplacementReminder.setText("");
        }

        if (rescueData != null) {
            rescuePurchaseDate.setText(String.valueOf(rescueData.getOrDefault("purchaseDate", "")));
            rescueAmountLeft.setText(String.valueOf(rescueData.getOrDefault("amountLeft", "")));
            rescueExpiryDate.setText(String.valueOf(rescueData.getOrDefault("expiry", "")));
            rescueReplacementReminder.setText(String.valueOf(rescueData.getOrDefault("replacementReminder", "")));
        } else {
            rescuePurchaseDate.setText("");
            rescueAmountLeft.setText("");
            rescueExpiryDate.setText("");
            rescueReplacementReminder.setText("");
        }
    }
}
