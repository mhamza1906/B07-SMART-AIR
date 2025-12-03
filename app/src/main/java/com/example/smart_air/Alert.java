package com.example.smart_air;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Alert {
    private String message;
    private @ServerTimestamp Date timestamp;

    @SuppressWarnings("unused")
    public Alert() {}

    @SuppressWarnings("unused")
    public Alert(String message, Date timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
