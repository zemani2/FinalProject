package com.example.anxietyByHeartRate;

/**
 * The Kid class represents a child associated with a parent user in the system.
 * It holds the child's basic information such as first name, last name, and email.
 */
public class Kid {

    // Child's first name
    private String firstName;

    // Child's last name
    private String lastName;

    // Child's email address
    private String email;

    /**
     * Default constructor required for calls to DataSnapshot.getValue(Kid.class).
     * This constructor is necessary for Firebase deserialization.
     */
    public Kid() {
        // Default constructor
    }

    /**
     * Parameterized constructor to initialize a Kid object with specific details.
     *
     * @param email     The email address of the kid.
     * @param firstName The first name of the kid.
     * @param lastName  The last name of the kid.
     */
    public Kid(String email, String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Getter and setter methods

    /**
     * Gets the first name of the kid.
     *
     * @return The first name of the kid.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name of the kid.
     *
     * @param firstName The first name to set.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the last name of the kid.
     *
     * @return The last name of the kid.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name of the kid.
     *
     * @param lastName The last name to set.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the email address of the kid.
     *
     * @return The email address of the kid.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the kid.
     *
     * @param email The email address to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
