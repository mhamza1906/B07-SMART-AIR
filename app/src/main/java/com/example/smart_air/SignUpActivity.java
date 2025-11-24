package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText fNameEditText, lNameEditText, usernameEditText, emailEditText, passwordEditText;
    private String selectedAccountType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        Spinner accountTypeSpinner = findViewById(R.id.accountTypeSpinner); // Corrected ID
        fNameEditText = findViewById(R.id.fNameEditText);
        lNameEditText = findViewById(R.id.lNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button registerButton = findViewById(R.id.registerButton);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.accountType, // Corrected array name
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        accountTypeSpinner.setAdapter(adapter);

        // This listener has two methods, so it CANNOT be converted to a lambda.
        accountTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedAccountType = parent.getItemAtPosition(position).toString();
                } else {
                    selectedAccountType = "";
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedAccountType = "";
            }
        });

        // LAMBDA in use here
        registerButton.setOnClickListener(v -> {
            String fName = fNameEditText.getText().toString().trim();
            String lName = lNameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (fName.isEmpty() || lName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || selectedAccountType.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill all fields and select an account type", Toast.LENGTH_LONG).show();
                return;
            }
            checkUsernameUniqueness(username, fName, lName, email, password, selectedAccountType);
        });
    }

    private void checkUsernameUniqueness(String username, String fName, String lName, String email, String password, String accountType) {
        mDatabase.child("users")
                .orderByChild("username")
                .equalTo(username)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        String errorMessage = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(SignUpActivity.this, "Error checking username: " + errorMessage, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (task.getResult().exists()) {
                        // username already exists
                        Toast.makeText(SignUpActivity.this, "Username already exists, please make some changes", Toast.LENGTH_SHORT).show();
                    } else {
                        // username DNE
                        registerNewUser(fName, lName, username, email, password, accountType);
                    }
                });
    }

    private void switchToTutorialSession(String userId) {
        mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists() && task.getResult() != null) {
                String accountType = task.getResult().child("accountType").getValue(String.class);

                if (accountType != null) {
                    switch (accountType) {
                        case "Parent":
                            startActivity(new Intent(SignUpActivity.this, ParentTutorialActivity.class));
                            break;
                        case "Healthcare Provider":
                            startActivity(new Intent(SignUpActivity.this, HealthcareProviderTutorialActivity.class));
                            break;
                        default:
                            Toast.makeText(SignUpActivity.this, "Unknown account type", Toast.LENGTH_SHORT).show();
                            return;
                    }
                    finish(); // turn off SignUpActivity
                } else {
                    Toast.makeText(SignUpActivity.this, "Account type cannot found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SignUpActivity.this, "Failed to retrieve user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerNewUser(final String fName, final String lName, final String username, final String email, final String password, final String accountType) {
        // LAMBDA in use here
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            User newUser = new User(fName, lName, email, username, accountType);

                            // LAMBDA in use here
                            mDatabase.child("users").child(userId).setValue(newUser)
                                    .addOnCompleteListener(databaseTask -> {
                                        if (databaseTask.isSuccessful()) {
                                            Toast.makeText(SignUpActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            switchToTutorialSession(userId);
                                        } else {
                                            String dbError = databaseTask.getException() != null ? databaseTask.getException().getMessage() : "DB error";
                                            Toast.makeText(SignUpActivity.this, "Profile save failed: " + dbError, Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        Exception authErrorInstance = task.getException();
                        String errorCode = authErrorInstance instanceof FirebaseAuthException ? ((FirebaseAuthException) authErrorInstance).getErrorCode() : "Unknown authentication error";
                        String authError = authErrorInstance != null ? authErrorInstance.getMessage() : "Unknown authentication error";
                        if (authErrorInstance instanceof FirebaseNetworkException) {
                            Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        switch (errorCode) {
                            case "ERROR_INVALID_EMAIL":
                                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
                                break;

                            case "ERROR_EMAIL_ALREADY_IN_USE":
                                Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
                                break;

                            case "ERROR_WEAK_PASSWORD":
                                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                                break;

                            default:
                                Toast.makeText(SignUpActivity.this, "Registration failed: " + authError, Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                });
    }
}
