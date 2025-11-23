package com.example.smart_air;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivityView extends AppCompatActivity {

    private boolean recoveryCooling = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Presenter
        presenter = new LoginActivityPresenter(this, new LoginActivityModel());

        // set buttons and link
        findViewById(R.id.login_btn).setOnClickListener(this::handleOnClick);
        findViewById(R.id.sign_up_btn).setOnClickListener(this::handleOnClick);
        findViewById(R.id.childLogin_btn).setOnClickListener(this::handleOnClick);
        findViewById(R.id.Credential_Recovery).setOnClickListener(this::handleOnClick);
    }


    private LoginActivityPresenter presenter;

    @Override
    protected void onStart() {
        super.onStart();
        presenter = new LoginActivityPresenter(this, new LoginActivityModel());
    }

    public String getEditText(int id) {
        EditText editText = findViewById(id);
        return editText.getText().toString().trim();
    }

    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    public void showTooFrequentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("ATTENTION:\n")
                .setMessage("You are clicking too frequently, please be patient and wait for 1-2 minutes." +
                        "\nIf you still haven't received the email after 2 minutes, you can click again.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void showResetEmailSentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("RESET EMAIL SENT:\n")
                .setMessage("The password reset email has been sent to the email address you entered, which may take 1-2 minutes." +
                        "\nPlease check your INBOX and SPAM emails.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public void redirectToSignUp() {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    public void showForgotPasswordLink() {
        findViewById(R.id.Credential_Recovery).setVisibility(View.VISIBLE);
    }

    public boolean isRecoveryCooling() {
        return recoveryCooling;
    }

    public void startRecoveryCooldown() {
        recoveryCooling = true;
        new android.os.Handler().postDelayed(() -> recoveryCooling = false, 2 * 60 * 1000);
    }


    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    public void finishActivity() {
        super.finish();
    }

    public void navigateToParentDashboard(String parentID) {
        Intent intent = new Intent(this, ParentDashboardActivity.class);
        intent.putExtra("parentID", parentID);
        startActivity(intent);
        finish();
    }


    public void navigateToProviderDashboard(String providerID) {
        Intent intent = new Intent(this, ProviderDashboardActivity.class);
        intent.putExtra("providerID", providerID);
        startActivity(intent);
        finish();
    }


    public void handleOnClick(View view) {
        int id = view.getId();
        String email = getEditText(R.id.emailEditText);
        String password = getEditText(R.id.passwordEditText);

        if (id == R.id.login_btn) {
            if (email.isEmpty() || password.isEmpty()) {
                toastMessage("Email or password cannot be empty.");
                return;
            }
            presenter.loginUser(email, password);

        } else if (id == R.id.sign_up_btn) {
            startActivity(new android.content.Intent(this, SignUpActivity.class));

        } else if (id == R.id.childLogin_btn) {
            startActivity(new android.content.Intent(this, ChildLoginActivityView.class));

        } else if (id == R.id.Credential_Recovery) {
            if (email.isEmpty()) {
                toastMessage("Please enter your email id");
                return;
            }
            presenter.handleCredentialRecoveryClick(email);
        }
    }

}
