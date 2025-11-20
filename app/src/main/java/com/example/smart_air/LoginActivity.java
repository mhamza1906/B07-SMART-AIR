package com.example.smart_air;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView forgotPasswordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        forgotPasswordTextView = findViewById(R.id.Credential_Recovery);

        forgotPasswordTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
                startActivity(intent);
            }
        });

        mAuth = FirebaseAuth.getInstance();

        // local variables: user inputs and buttons
        EditText emailEditText = findViewById(R.id.emailEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        Button login_btn = findViewById(R.id.login_btn);
        Button sign_up_btn = findViewById(R.id.sign_up_btn);
        Button childLoginBtn = findViewById(R.id.childLogin_btn);

        login_btn.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email or password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });

        childLoginBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, ChildLoginActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e("LoginActivity", "Failed to open ChildLoginActivity", e);
                Toast.makeText(this, "Unable to open Child Login page.", Toast.LENGTH_SHORT).show();
            }
        });

        sign_up_btn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, SignUpActivity.class));
            } catch (Exception e) {
                Log.e("LoginActivity", "Failed to open SignUpActivity", e);
                Toast.makeText(this, "Unable to open Sign Up page.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Exception exception = task.getException();
                        String errorMsg = "Login failed.";

                        if (exception instanceof FirebaseAuthException) {
                            String errorCode = ((FirebaseAuthException) exception).getErrorCode();

                            switch (errorCode) {
                                case "ERROR_USER_NOT_FOUND":
                                    Toast.makeText(this, "You don't have an account yet. Redirecting to Sign Up...", Toast.LENGTH_LONG).show();
                                    try {
                                        startActivity(new Intent(this, SignUpActivity.class));
                                    } catch (Exception e) {
                                        Log.e("LoginActivity", "Failed to open SignUpActivity", e);
                                        Toast.makeText(this, "Unable to open Sign Up page.", Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                case "ERROR_WRONG_PASSWORD":
                                    errorMsg = "Incorrect password. Please try again.";
                                    break;
                                case "ERROR_INVALID_EMAIL":
                                    errorMsg = "Invalid email format.";
                                    break;
                                default:
                                    errorMsg = "Login failed: " + exception.getMessage();
                                    break;
                            }
                        } else if (exception != null) {
                            errorMsg = "Login failed: " + exception.getMessage();
                        }

                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
