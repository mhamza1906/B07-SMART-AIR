package com.example.smart_air;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivityModel {

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private LoginActivityPresenter presenter;

    public LoginActivityModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public DatabaseReference getUsersReference() {
        return FirebaseDatabase.getInstance().getReference("users");
    }

    public void setPresenter(LoginActivityPresenter presenter) {
        this.presenter = presenter;
    }

    public FirebaseUser getSuccessfulLoginUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void getUserEmail(String email) {
        getUsersReference()
                .orderByChild("email")
                .equalTo(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (presenter != null) {
                        presenter.onCheckEmailComplete(task, email);
                    }
                });
    }

    public void signUserIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (presenter != null) {
                        presenter.onSignInComplete(task);
                    }
                });
    }

    public void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (presenter != null) {
                        presenter.onResetEmailComplete(task);
                    }
                });
    }

    public void getUserFromRTDB(String userId) {
        mDatabase.child("users").child(userId).get()
                .addOnCompleteListener(task -> {
                    if (presenter != null) {
                        presenter.onUserFetchComplete(task);
                    }
                });
    }
}
