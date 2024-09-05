package com.example.anxietyByHeartRate;

/**
 * OnDateChangedListener is an interface to be implemented by components that
 * need to respond to date changes.
 *
 * When the date is changed in the UI, the implementing class should update its
 * data or UI based on the new selected date.
 */
public interface OnDateChangedListener {

    /**
     * Called when the selected date has changed.
     *
     * @param selectedDate The newly selected date in a specific format (e.g., "YYYY-MM-DD").
     */
    void onDateChanged(String selectedDate);
}
