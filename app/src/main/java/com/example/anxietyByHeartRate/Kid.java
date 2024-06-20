package com.example.anxietyByHeartRate;

public class Kid {
    private String firstName;
    private String lastName;
    private String email;

    public Kid() {
        // Default constructor required for calls to DataSnapshot.getValue(Kid.class)
    }

    public Kid(String email, String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Getters and setters

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
