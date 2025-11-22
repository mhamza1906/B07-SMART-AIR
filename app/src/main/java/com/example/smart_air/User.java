package com.example.smart_air;

public class User {
    public String fName;
    public String lName;
    public String email;
    public String username;
    public String accountType;
    public User() {
    }

    public User(String fName, String lName, String email, String username, String accountType) {
        this.fName = fName;
        this.lName = lName;
        this.email = email;
        this.username = username;
        this.accountType = accountType;
    }

    //child user constructor
    public User(String fName, String lName, String username) {
        this.fName = fName;
        this.lName = lName;
        this.username = username;
        this.email = null;
        this.accountType = "Child";
    }
}
