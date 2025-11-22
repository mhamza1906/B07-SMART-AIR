package com.example.smart_air;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChildSignUp extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_signup);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        EditText fNameEditText = findViewById(R.id.fNameEditText);
        EditText lNameEditText = findViewById(R.id.lNameEditText);
        EditText usernameEditText = findViewById(R.id.usernameEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        Button registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String fName = fNameEditText.getText().toString().trim();
            String lName = lNameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (fName.isEmpty() || lName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(ChildSignUp.this, "Please fill all fields", Toast.LENGTH_LONG).show();
                return;
            }

            // Since children don't have emails, we can generate a "fake" one for Firebase Auth
            // This email is ONLY for the authentication system and won't be stored in the database
            String fakeEmailForAuth = username.toLowerCase() + "@child.smart_air.com";

            registerNewChild(fName, lName, username, fakeEmailForAuth, password);
        });
    }

    private void registerNewChild(final String fName, final String lName, final String username, final String email, final String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Use the new constructor from User.java
                            User newChildUser = new User(fName, lName, username);

                            mDatabase.child("users").child(userId).setValue(newChildUser)
                                    .addOnCompleteListener(databaseTask -> {
                                        if (databaseTask.isSuccessful()) {
                                            Toast.makeText(ChildSignUp.this, "Child account created!", Toast.LENGTH_SHORT).show();
                                            // You can navigate to the child's dashboard or tutorial here
                                        } else {
                                            String dbError = databaseTask.getException() != null ? databaseTask.getException().getMessage() : "DB error";
                                            Toast.makeText(ChildSignUp.this, "Profile save failed: " + dbError, Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        String authError = task.getException() != null ? task.getException().getMessage() : "Auth error";
                        Toast.makeText(ChildSignUp.this, "Registration failed: " + authError, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
