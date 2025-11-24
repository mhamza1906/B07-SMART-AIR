package com.example.smart_air;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChildLoginActivityModel {

    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private ChildLoginActivityPresenter presenter;

    public ChildLoginActivityModel() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void setPresenter(ChildLoginActivityPresenter presenter) {
        this.presenter = presenter;
    }

    public void signUserIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> presenter.onSignInComplete(task));
    }

    public void getUserFromRTDB(String userId) {
        mDatabase.child("users").child(userId).get()
                .addOnCompleteListener(task -> presenter.onUserFetchComplete(task));
    }

    public FirebaseAuth getFirebaseAuth() {
        return mAuth;
    }
}
