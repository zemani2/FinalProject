package com.example.anxietyByHeartRate;

/**
 * Represents a request in the system involving a kid and a parent.
 */
public class Request {
    private String kidEmail;
    private String status;
    private boolean accepted; // Indicates whether the request has been accepted
    private String parentEmail;

    /**
     * Default constructor required for Firebase data deserialization.
     */
    public Request() {
        // Default constructor
    }

    /**
     * Constructs a Request with specified parameters.
     *
     * @param kidEmail   The email of the kid associated with the request.
     * @param status     The current status of the request (e.g., pending, approved).
     * @param accepted   Indicates whether the request has been accepted.
     * @param parentEmail The email of the parent associated with the request.
     */
    public Request(String kidEmail, String status, boolean accepted, String parentEmail) {
        this.kidEmail = kidEmail;
        this.status = status;
        this.accepted = accepted;
        this.parentEmail = parentEmail;
    }

    /**
     * Gets the email of the kid associated with the request.
     *
     * @return The kid's email.
     */
    public String getKidEmail() {
        return kidEmail;
    }

    /**
     * Sets the email of the kid associated with the request.
     *
     * @param kidEmail The kid's email.
     */
    public void setKidEmail(String kidEmail) {
        this.kidEmail = kidEmail;
    }

    /**
     * Gets the current status of the request.
     *
     * @return The request status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the current status of the request.
     *
     * @param status The request status.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Checks if the request has been accepted.
     *
     * @return True if the request has been accepted, false otherwise.
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Sets whether the request has been accepted.
     *
     * @param accepted True if the request has been accepted, false otherwise.
     */
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    /**
     * Gets the email of the parent associated with the request.
     *
     * @return The parent's email.
     */
    public String getParentEmail() {
        return parentEmail;
    }

    /**
     * Sets the email of the parent associated with the request.
     *
     * @param parentEmail The parent's email.
     */
    public void setParentEmail(String parentEmail) {
        this.parentEmail = parentEmail;
    }
}
