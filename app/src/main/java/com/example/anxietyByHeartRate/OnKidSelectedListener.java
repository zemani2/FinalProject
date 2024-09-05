package com.example.anxietyByHeartRate;

/**
 * OnKidSelectedListener is an interface that should be implemented by components
 * that need to respond to a kid selection event.
 *
 * When a user selects a kid from a list, the implementing class will be notified,
 * and it can take appropriate actions based on the selected kid's email.
 */
public interface OnKidSelectedListener {

    /**
     * Called when a kid is selected.
     *
     * @param selectedKidEmail The email of the kid that was selected.
     */
    void onKidSelected(String selectedKidEmail);
}
