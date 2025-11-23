package com.example.smart_air;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

public class LoginActivityPresenter {

    private final LoginActivityView view;
    private final LoginActivityModel model;

    public LoginActivityPresenter(LoginActivityView view, LoginActivityModel model) {
        this.view = view;
        this.model = model;
        this.model.setPresenter(this);
    }

    // Presenter calls Model to login
    public void loginUser(String email, String password) {
        model.signUserIn(email, password);
    }

    // Presenter calls Model send password reset email
    public void handleCredentialRecoveryClick(String email) {
        if (view.isRecoveryCooling()) {
            view.showTooFrequentDialog();
            return;
        }
        model.sendResetEmail(email);
        view.startRecoveryCooldown();
    }

    // calls Model after Login
    public void onSignInComplete(Task<AuthResult> task) {
        if (task.isSuccessful()) {
            FirebaseUser app_user = model.getSuccessfulLoginUser();
            if (app_user != null) {
                view.toastMessage("Login successful!");
                model.getUserFromRTDB(app_user.getUid());
            }
        } else {
            handleLoginError(task.getException());
        }
    }

    // calls Model to query user information
    public void onUserFetchComplete(Task<DataSnapshot> task) {
        if (task.isSuccessful() && task.getResult().exists()) {
            DataSnapshot snapshot = task.getResult();
            String accountType = snapshot.child("accountType").getValue(String.class);
            String userId = snapshot.getKey();
            if (accountType != null) {
                switch (accountType) {
                    case "Parent":
                        view.navigateToParentDashboard(userId);
                        break;
                    case "Healthcare Provider":
                        view.navigateToProviderDashboard(userId);
                        break;
                    default:
                        view.toastMessage("Unknown account type");
                        break;
                }
                view.finishActivity();
            } else {
                view.toastMessage("Account type cannot be found");
            }
        } else {
            view.toastMessage("Failed to retrieve user info");
        }
    }

    // calls Model to sent reset email
    public void onResetEmailComplete(Task<Void> task) {
        if (task.isSuccessful()) {
            view.showResetEmailSentDialog();
            view.startRecoveryCooldown();
        } else {
            view.toastMessage("Failed to send reset email.");
        }
    }


    // deal with login failed exceptions
    private void handleLoginError(Exception exception) {
        String errorMsg = "Login failed.";
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();

            switch (errorCode) {
                case "ERROR_USER_NOT_FOUND":
                    view.toastMessage("You don't have an account yet. Redirecting to Sign Up...");
                    view.redirectToSignUp();
                    return;
                case "ERROR_INVALID_CREDENTIAL":
                    errorMsg = "Incorrect password. Please try again.";
                    view.showForgotPasswordLink();
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
        view.toastMessage(errorMsg);
    }
}
