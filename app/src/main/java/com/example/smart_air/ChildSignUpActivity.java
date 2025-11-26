package com.example.smart_air;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ChildSignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore firestore;

    private String parentID;

    private EditText birthdayEditText;
    private String birthdayString = ""; // yyyy-MM-dd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        parentID = getIntent().getStringExtra("parentID");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_signup);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();

        EditText fNameEditText = findViewById(R.id.fNameEditText);
        EditText lNameEditText = findViewById(R.id.lNameEditText);
        EditText usernameEditText = findViewById(R.id.usernameEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        birthdayEditText = findViewById(R.id.birthdayEditText);
        Button registerButton = findViewById(R.id.registerButton);
        // set birthday selector
        birthdayEditText.setFocusable(false);
        birthdayEditText.setOnClickListener(v -> openBirthDatePicker());

        registerButton.setOnClickListener(v -> {
            String fName = fNameEditText.getText().toString().trim();
            String lName = lNameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (fName.isEmpty() || lName.isEmpty() || username.isEmpty()
                    || password.isEmpty() || birthdayString.isEmpty()) {

                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_LONG).show();
                return;
            }

            // username rule
            if (!username.matches("^[a-zA-Z0-9+_.-]+$")) {
                Toast.makeText(this, "Username can only contain letters, numbers and + - _ .", Toast.LENGTH_LONG).show();
                return;
            }

            // password strength
            if (!checkPasswordStrength(password)) {
                passwordEditText.setError("Password must contain upper, lower, digit, !@#$%^&*(),.?\":{}|<>, and ≥ 8 chars");
                return;
            }

            // generate fake email
            String fakeEmailForAuth = username + "@child.smart-air.com";

            checkUsernameUniqueness(username, fName, lName, fakeEmailForAuth, password);
        });
    }


    // select birthday
    private void openBirthDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    month += 1;
                    birthdayString = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayOfMonth);
                    birthdayEditText.setText(birthdayString);
                },
                y, m, d
        );

        dialog.show();
    }


    // prevent weak password
    private boolean checkPasswordStrength(String pwd) {
        if (pwd.length() < 8) return false;
        boolean hasUpper = pwd.matches(".*[A-Z].*");
        boolean hasLower = pwd.matches(".*[a-z].*");
        boolean hasDigit = pwd.matches(".*[0-9].*");
        boolean hasSpecial = pwd.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }


    // check if user name is unique as child can login with username
    private void checkUsernameUniqueness(String username, String fName, String lName, String email, String password) {
        mDatabase.child("users")
                .orderByChild("username")
                .equalTo(username)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (task.getResult().exists()) {
                        Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        registerNewUser(fName, lName, username, email, password);
                    }
                });
    }

    private void registerNewUser(final String fName, final String lName, final String username,
                                 final String email, final String password) {


        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Registration failed: " +
                                        (task.getException() != null ? task.getException().getMessage() : ""),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        Toast.makeText(this, "Registration failed: user is null", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String childID = firebaseUser.getUid();

                    // save child in Realtime Database
                    User newChildUser = new User(fName, lName, username);
                    mDatabase.child("users").child(childID).setValue(newChildUser)
                            .addOnCompleteListener(dbTask -> {
                                if (!dbTask.isSuccessful()) {
                                    Toast.makeText(this, "Failed creating the child profile", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // save parent-child pair to Firestore
                                saveChildToFirestore(childID, fName, lName, username);
                            });
                });
    }

    // Write the parent-child pair into Firestore
    private void saveChildToFirestore(String childID, String fName, String lName, String username) {

        Map<String, Object> data = new HashMap<>();
        data.put("fName", fName);
        data.put("lName", lName);
        data.put("username", username);
        data.put("birthday", birthdayString);
        data.put("accountType", "child");
        data.put("childID", childID);

        firestore.collection("parent-child")
                .document(parentID)
                .collection("child")
                .document(childID)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Your child's profile saved!", Toast.LENGTH_SHORT).show();

                    // add a delay to guarantee firestore callback
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        startActivity(new Intent(this, ChildTutorialActivity.class));
                        finish();
                    }, 500); // delay 0.5s

                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed saving your child's profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }


}
