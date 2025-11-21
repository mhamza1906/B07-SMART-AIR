package com.example.smart_air;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button resetPasswordButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = newPasswordEditText.getText().toString();
                String confirmPassword = confirmPasswordEditText.getText().toString();

                if (newPassword.equals(confirmPassword)) {
                    // TODO: Implement logic to reset password
                    Toast.makeText(ResetPasswordActivity.this, "Password reset successfully!", Toast.LENGTH_SHORT).show();
                    // TODO: Navigate back to login screen
                } else {
                    confirmPasswordEditText.setError("Passwords do not match");
                }
            }
        });
    }
}