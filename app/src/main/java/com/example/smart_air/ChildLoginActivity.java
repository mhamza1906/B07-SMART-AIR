package com.example.smart_air;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class ChildLoginActivity extends AppCompatActivity {

    private EditText childUserNameEditText;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_login_with_username);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        childUserNameEditText = findViewById(R.id.childUserNameEditText);
        Button loginBtn = findViewById(R.id.childLogin_btn);
        loginBtn.setOnClickListener(v -> attemptChildLogin());
    }


    private void attemptChildLogin() {
        String username = childUserNameEditText.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(ChildLoginActivity.this, "Please type your User Name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find Child User ID
        CollectionReference usersRef = db.collection("users");
        usersRef.whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(query -> {

                    // fail to find Child User ID
                    if (query.isEmpty()) {
                        Toast.makeText(this, "Your account does NOT exist, please tell your parent to register you first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // successfully find Child User ID
                    DocumentSnapshot childDoc = query.getDocuments().get(0);
                    String childId = childDoc.getId();
                    String parentId = childDoc.getString("parentId");

                    // check Parent-child pair
                    if (parentId == null || parentId.trim().isEmpty()) {
                        Toast.makeText(this, "Your parent's account is invalid", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // check Parent-child pair continued
                    checkParentChildLink(parentId, childId, childDoc);

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "fail to search Parent-child pair：" + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // check Parent-child pair continued
    private void checkParentChildLink(String parentId, String childId, DocumentSnapshot childDoc) {

        DocumentReference parentDocRef = db.collection("Parent-child").document(parentId);

        parentDocRef.get().addOnSuccessListener(parentSnapshot -> {

            if (!parentSnapshot.exists()) {
                Toast.makeText(this, "Your parent does NOT have an account yet", Toast.LENGTH_LONG).show();
                return;
            }

            // 判断父文档中是否包含该 childId
            if (!parentSnapshot.contains(childId)) {
                Toast.makeText(this, "Your parent has NOT linked you, please tell your parent to link you first", Toast.LENGTH_LONG).show();
                return;
            }

            // Parent-child successfully checked!
            loginChild(childDoc);

        }).addOnFailureListener(e ->
                Toast.makeText(this, "fail to read data from the Parent account：" + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    // Firebase Authentication and Login
    private void loginChild(DocumentSnapshot childDoc) {
        String childEmail = childDoc.getString("email");
        String childPassword = childDoc.getString("password");

        if (childEmail == null || childPassword == null) {
            Toast.makeText(this, "the Child account is incompleted", Toast.LENGTH_LONG).show();
            return;
        }

        auth.signInWithEmailAndPassword(childEmail, childPassword)
                .addOnSuccessListener(authResult -> {
                    Toast.makeText(this, "Login successful！", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ChildLoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Login failed:" + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
