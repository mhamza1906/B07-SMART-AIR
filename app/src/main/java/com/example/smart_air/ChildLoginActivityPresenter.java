package com.example.smart_air;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

public class ChildLoginActivityPresenter {

    private final ChildLoginActivityView view;
    private final ChildLoginActivityModel model;

    public ChildLoginActivityPresenter(ChildLoginActivityView view, ChildLoginActivityModel model) {
        this.view = view;
        this.model = model;
        this.model.setPresenter(this);
        this.view.setPresenter(this);
    }

    // Presenter calls Model to login a child
    public void loginUser(String email, String password) {
        model.signUserIn(email, password);
    }

    // calls Model after login
    public void onSignInComplete(Task<AuthResult> task) {
        if (task.isSuccessful()) {
            FirebaseUser app_user = model.getFirebaseAuth().getCurrentUser();
            if (app_user != null) {
                view.toastMessage("Login successful!");
                model.getUserFromRTDB(app_user.getUid());
            }
        } else {
            handleLoginError(task.getException());
        }
    }

    // calls Model to get child user information
    public void onUserFetchComplete(Task<DataSnapshot> task) {
        if (task.isSuccessful() && task.getResult().exists()) {
            DataSnapshot snapshot = task.getResult();
            String accountType = snapshot.child("accountType").getValue(String.class);
            String userId = snapshot.getKey();

            if (accountType != null) {
                if (accountType.equals("Child")) {
                    view.switchToChildDashboard(userId);
                } else {
                    view.toastMessage("Unknown account type");
                }
            } else {
                view.toastMessage("Account type cannot be found");
            }
        } else {
            view.toastMessage("Failed to retrieve child info");
        }
    }

    // deal with login failed exceptions
    private void handleLoginError(Exception exception) {
        String errorMsg = "Login failed.";
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            switch (errorCode) {
                case "ERROR_USER_NOT_FOUND":
                    errorMsg = "You DON'T have an account, please tell your parent to register you first";
                    break;
                case "ERROR_INVALID_CREDENTIAL":
                    errorMsg = "Incorrect password. Please try again.";
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
