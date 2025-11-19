package com.example.smart_air;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
public class LoginActivity extends AppCompatActivity{
    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    Button login_btn, sign_up_btn;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        login_btn = findViewById(R.id.login_btn);
        sign_up_btn = findViewById(R.id.sign_up_btn);

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                // gain input user email and password

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Email or password cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // empty check

                loginUser(email, password);
                // login user through firebase
            }
        });

        sign_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Jump to SignUpActivity
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser(String email, String password) {

        mAuth.signInWithEmailAndPassword(email, password)
                // login with firebase

                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            // if login successful

                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();  // prevent to go back to login page

                        } else {
                            // fail to login, analyze the reason
                            String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();

                            switch (errorCode) {

                                case "ERROR_USER_NOT_FOUND":
                                    Toast.makeText(LoginActivity.this, "You don't have an account yet. Redirecting to Sign Up...", Toast.LENGTH_LONG).show();

                                    // automatically jump to the sign up page
                                    startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
                                    break;

                                case "ERROR_WRONG_PASSWORD":
                                    Toast.makeText(LoginActivity.this, "Incorrect password. Please try again.", Toast.LENGTH_LONG).show();
                                    break;

                                case "ERROR_INVALID_EMAIL":
                                    Toast.makeText(LoginActivity.this, "Invalid email format.", Toast.LENGTH_LONG).show();
                                    break;

                                default:
                                    Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    break;
                            }
                        }
                    }
                });
    }
}


