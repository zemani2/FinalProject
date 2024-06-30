package com.example.anxietyByHeartRate;

public class HeartRateData {
    private int heartRate;
    private long timestamp;

    public HeartRateData(int heartRate, long timestamp) {
        this.heartRate = heartRate;
        this.timestamp = timestamp;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
