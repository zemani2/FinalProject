package com.example.anxietyByHeartRate;
public class Request {
    private String kidEmail;
    private String status;
    private boolean accepted; // New field
    private String parentEmail;

    public Request() {
        // Default constructor required for calls to DataSnapshot.getValue(Request.class)
    }

    public Request(String kidEmail, String status, boolean accepted, String parentEmail) {
        this.kidEmail = kidEmail;
        this.status = status;
        this.accepted = accepted;
        this.parentEmail = parentEmail;
    }

    public String getKidEmail() {
        return kidEmail;
    }

    public void setKidEmail(String kidEmail) {
        this.kidEmail = kidEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getParentEmail() {
        return parentEmail;
    }

    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }
}
