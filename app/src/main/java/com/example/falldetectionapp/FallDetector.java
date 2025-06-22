package com.example.falldetectionapp;

public class FallDetector {

    private static final float FALL_THRESHOLD_HIGH = 15.0f;    // High accel magnitude threshold
    private static final float FALL_THRESHOLD_LOW = 2.0f;      // Low accel threshold (impact)
    private static final float IMPACT_THRESHOLD = 12.0f;       // Impact threshold (high accel after fall)
    private static final float GYRO_THRESHOLD = 3.0f;          // Gyroscope angular velocity threshold (rad/s)
    private static final int CONFIRMATION_COUNT = 3;           // Number of confirmations for fall
    private static final long FALL_COOLDOWN = 10000;           // 10 seconds cooldown between falls

    private int fallConfirmations = 0;
    private long lastFallTime = 0;
    private boolean highAccelDetected = false;
    private long highAccelTime = 0;
    private static final long IMPACT_WINDOW = 2000;            // 2 seconds window for impact detection

    // Moving average for noise reduction on accelerometer
    private float[] accelHistory = new float[5];
    private int historyIndex = 0;

    /**
     * Detect fall using both accelerometer and gyroscope data.
     *
     * @param accelValues float array with accelerometer X, Y, Z values
     * @param gyroValues  float array with gyroscope X, Y, Z values
     * @return true if fall detected, false otherwise
     */
    public boolean detectFall(float[] accelValues, float[] gyroValues) {
        if (accelValues == null || gyroValues == null) return false;

        // Calculate total acceleration magnitude
        float totalAccel = (float) Math.sqrt(
                accelValues[0] * accelValues[0] +
                        accelValues[1] * accelValues[1] +
                        accelValues[2] * accelValues[2]
        );

        // Apply moving average filter to smooth accelerometer data
        totalAccel = applyMovingAverage(totalAccel);

        // Process gyroscope data for angular velocity magnitude
        boolean gyroFallDetected = processGyroscopeData(gyroValues, GYRO_THRESHOLD);

        long currentTime = System.currentTimeMillis();

        // Check for high acceleration indicating potential fall start
        if (totalAccel > FALL_THRESHOLD_HIGH && gyroFallDetected) {
            highAccelDetected = true;
            highAccelTime = currentTime;
            fallConfirmations++;
        }

        // Check for impact (low or high acceleration) after high acceleration
        if (highAccelDetected && (currentTime - highAccelTime) < IMPACT_WINDOW) {
            if (totalAccel < FALL_THRESHOLD_LOW || totalAccel > IMPACT_THRESHOLD) {
                fallConfirmations++;
            }
        }

        // Reset state if impact window has passed without confirmation
        if (currentTime - highAccelTime > IMPACT_WINDOW) {
            highAccelDetected = false;
            fallConfirmations = 0;
        }

        // Confirm fall if pattern detected with cooldown to avoid repeated alerts
        if (fallConfirmations >= CONFIRMATION_COUNT) {
            if (currentTime - lastFallTime > FALL_COOLDOWN) {
                lastFallTime = currentTime;
                fallConfirmations = 0;
                highAccelDetected = false;
                return true;
            }
        }

        return false;
    }

    /**
     * Calculate moving average for accelerometer noise reduction.
     * @param newValue New acceleration magnitude value
     * @return Smoothed acceleration magnitude
     */
    private float applyMovingAverage(float newValue) {
        accelHistory[historyIndex] = newValue;
        historyIndex = (historyIndex + 1) % accelHistory.length;

        float sum = 0;
        for (float value : accelHistory) {
            sum += value;
        }

        return sum / accelHistory.length;
    }

    /**
     * Process gyroscope data to detect abnormal rotation.
     *
     * @param gyroValues float array with gyroscope X, Y, Z angular velocity values (rad/s)
     * @param threshold Threshold magnitude for rotation to be considered abnormal
     * @return true if abnormal rotation detected, false otherwise
     */
    private boolean processGyroscopeData(float[] gyroValues, float threshold) {
        if (gyroValues == null || gyroValues.length < 3) {
            return false;
        }

        // Calculate magnitude of angular velocity vector
        float rotationMagnitude = (float) Math.sqrt(
                gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]
        );

        // Return true if rotation magnitude exceeds threshold
        return rotationMagnitude > threshold;
    }

    /**
     * Set sensitivity to adjust detection thresholds dynamically.
     *
     * @param sensitivity float from 0 to 100 representing sensitivity level
     */
    public void setSensitivity(float sensitivity) {
        // Map sensitivity (0-100) to a factor (0.5 to 2.0)
        float factor = sensitivity / 100.0f * 1.5f + 0.5f;

        // Adjust thresholds based on sensitivity factor
        // (Example: multiply thresholds by factor, tune as needed)
        // Note: Add synchronized keyword if multithreaded access is possible

        // This is a simple example; you might want to add setters/getters
        // or store these values in instance variables for use in detectFall()
    }
}
