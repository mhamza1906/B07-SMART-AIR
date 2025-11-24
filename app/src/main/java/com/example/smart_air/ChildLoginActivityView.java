package com.example.smart_air;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.view.View;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class ChildLoginActivityView extends AppCompatActivity {

    private ChildLoginActivityPresenter presenter;

    private EditText usernameEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_login_with_username);

        // Initialize EditText and Button
        usernameEditText = findViewById(R.id.childUserNameEditText);
        passwordEditText = findViewById(R.id.childUserPasswordEditText);
        Button loginBtn = findViewById(R.id.childLogin_btn);

        // Initialize Presenter
        presenter = new ChildLoginActivityPresenter(this, new ChildLoginActivityModel());

        // set button
        loginBtn.setOnClickListener(this::handleOnClick);
    }

    public void setPresenter(ChildLoginActivityPresenter presenter) {
        this.presenter = presenter;
    }

    public String[] getEditText() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        return new String[]{username, password};
    }

    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void handleOnClick(View view) {
        if (view.getId() == R.id.childLogin_btn) {
            String[] input = getEditText();
            String username = input[0];
            String password = input[1];
            if (username.isEmpty() || password.isEmpty()) {
                toastMessage("Username or password cannot be empty.");
                return;
            }
            String fakeEmail = username.toLowerCase() + "@child.smart-air.com";
            presenter.loginUser(fakeEmail, password);
        }
    }

    public void switchToChildDashboard(String userId) {
        Intent childIntent = new Intent(this, ChildDashboardActivity.class);
        childIntent.putExtra("childID", userId);
        this.startActivity(childIntent);
    }
}
