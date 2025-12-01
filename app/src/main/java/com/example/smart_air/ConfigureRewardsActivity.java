package com.example.smart_air;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ConfigureRewardsActivity extends AppCompatActivity {

    private String childID;

    private EditText inputBudding, inputShining, inputLucky;
    private ImageView imgBudding, imgShining, imgLucky;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure_rewards_page);

        childID = getIntent().getStringExtra("childID");

        if (childID == null) {
            Toast.makeText(this, "Missing child ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        setupViews();
        loadAnimations();
        loadExistingThresholds();
        setupSaveListeners();
    }


    private void setupViews() {
        inputBudding = findViewById(R.id.input_budding);
        inputShining = findViewById(R.id.input_shining);
        inputLucky = findViewById(R.id.input_lucky);

        imgBudding = findViewById(R.id.img_budding);
        imgShining = findViewById(R.id.img_shining);
        imgLucky = findViewById(R.id.img_lucky);
    }


    private void loadAnimations() {
        Animation buddingAnim = AnimationUtils.loadAnimation(this, R.anim.budding_star_glow);
        Animation shiningAnim = AnimationUtils.loadAnimation(this, R.anim.shining_star_twinkle);
        Animation luckyAnim = AnimationUtils.loadAnimation(this, R.anim.lucky_star_sparkle);

        imgBudding.startAnimation(buddingAnim);
        imgShining.startAnimation(shiningAnim);
        imgLucky.startAnimation(luckyAnim);
    }


    private void loadExistingThresholds() {
        db.collection("badges")
                .document(childID)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Long b = snapshot.getLong("budding_star");
                        Long s = snapshot.getLong("shining_star");
                        Long l = snapshot.getLong("lucky_star");

                        if (b != null) inputBudding.setText(String.valueOf(b));
                        if (s != null) inputShining.setText(String.valueOf(s));
                        if (l != null) inputLucky.setText(String.valueOf(l));
                    }
                });
    }


    private void setupSaveListeners() {
        inputBudding.addTextChangedListener(thresholdWatcher("budding_star"));
        inputShining.addTextChangedListener(thresholdWatcher("shining_star"));
        inputLucky.addTextChangedListener(thresholdWatcher("lucky_star"));
    }


    private TextWatcher thresholdWatcher(String key) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();

                if (text.isEmpty()) return;

                try {
                    int n = Integer.parseInt(text);
                    if (n < 1) return;

                    Map<String, Object> data = new HashMap<>();
                    data.put(key, n);

                    db.collection("badges")
                            .document(childID)
                            .set(data, SetOptions.merge());

                } catch (NumberFormatException ignored) {}
            }
        };
    }
}
