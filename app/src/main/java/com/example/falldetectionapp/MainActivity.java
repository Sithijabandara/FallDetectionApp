package com.example.falldetectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";

    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;

    private TextView accelDataX, accelDataY, accelDataZ;

    private TextView gyroDataX, gyroDataY, gyroDataZ;
    private TextView statusText;

    private Button settingsBtn, contactsBtn, testBtn;

    private FallDetector fallDetector;
    private LocationHelper locationHelper;
    private SMSHelper smsHelper;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initSensors();

        fallDetector = new FallDetector();
        locationHelper = new LocationHelper(this);
        smsHelper = new SMSHelper(this);

        if (!hasAllPermissions()) {
            requestPermissions();
        }

        setupButtons();

        Log.d(TAG, "MainActivity created");
    }

    private void initViews() {
        accelDataX = findViewById(R.id.accelDataX);
        accelDataY = findViewById(R.id.accelDataY);
        accelDataZ = findViewById(R.id.accelDataZ);

        gyroDataX = findViewById(R.id.gyroDataX);
        gyroDataY = findViewById(R.id.gyroDataY);
        gyroDataZ = findViewById(R.id.gyroDataZ);

        statusText = findViewById(R.id.statusText);

        settingsBtn = findViewById(R.id.settingsBtn);
        contactsBtn = findViewById(R.id.contactsBtn);

    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Missing permission: " + permission);
                return false;
            }
        }
        Log.d(TAG, "All permissions granted");
        return true;
    }

    private void requestPermissions() {
        Log.d(TAG, "Requesting permissions");
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    private void setupButtons() {
        settingsBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Settings screen not available", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Settings activity not found", e);
            }
        });

        contactsBtn.setOnClickListener(v -> {
            try {
                startActivity(new Intent(MainActivity.this, EmergencyContactsActivity.class));
            } catch (Exception e) {
                Toast.makeText(this, "Contacts screen not available", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Contacts activity not found", e);
            }
        });

        // Test SMS button (optional)
        if (testBtn != null) {
            testBtn.setOnClickListener(v -> testSMS());
        }
    }

    // Test SMS functionality
    private void testSMS() {
        Log.d(TAG, "Testing SMS manually");

        // Check if emergency contact is set
        if (!smsHelper.isContactSet()) {
            Toast.makeText(this, "Please set emergency contact first", Toast.LENGTH_LONG).show();
            return;
        }

        // Check permissions
        if (!hasAllPermissions()) {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_LONG).show();
            requestPermissions();
            return;
        }

        smsHelper.testSMS();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            updateAccelDisplay(event.values);
            if (hasAllPermissions() && fallDetector.detectFall(event.values, null)) {
                handleFallDetected();
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            updateGyroDisplay(event.values);
        }
    }

    private void updateAccelDisplay(float[] values) {
        accelDataX.setText(String.format(Locale.getDefault(), "X-axis: %.2f", values[0]));
        accelDataY.setText(String.format(Locale.getDefault(), "Y-axis: %.2f", values[1]));
        accelDataZ.setText(String.format(Locale.getDefault(), "Z-axis: %.2f", values[2]));
    }

    private void updateGyroDisplay(float[] values) {
        gyroDataX.setText(String.format(Locale.getDefault(), "X-rotation: %.2f", values[0]));
        gyroDataY.setText(String.format(Locale.getDefault(), "Y-rotation: %.2f", values[1]));
        gyroDataZ.setText(String.format(Locale.getDefault(), "Z-rotation: %.2f", values[2]));
    }

    private void handleFallDetected() {
        Log.d(TAG, "Fall detected - starting alert process");

        runOnUiThread(() -> statusText.setText("Fall detected! Sending alert..."));

        // Get user settings from SharedProfiles
        SharedPreferences prefs = getSharedPreferences("FallDetectionPrefs", MODE_PRIVATE);
        boolean soundEnabled = prefs.getBoolean("sound_enabled", true);
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);

        // Check if emergency contact is set
        String contact = smsHelper.getContact();
        Log.d(TAG, "Emergency contact: " + (contact.isEmpty() ? "NOT SET" : "SET"));

        // Check if SMS is enabled
        boolean smsEnabled = smsHelper.isSMSEnabled();
        Log.d(TAG, "SMS enabled: " + smsEnabled);

        // Sound alert
        if (soundEnabled) playAlertSound();

        // Vibration alert
        if (vibrationEnabled) triggerVibration();

        // Try location
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
            @Override
            public void onLocationReceived(double latitude, double longitude, String address) {
                Log.d(TAG, "Location received, sending SMS with location");
                smsHelper.sendAlert(latitude, longitude, address);
                runOnUiThread(() -> statusText.setText("Fall alert sent with location."));
            }

            @Override
            public void onLocationError(String error) {
                Log.d(TAG, "Location error: " + error + " - sending SMS without location");
                smsHelper.sendAlert(); // fallback
                runOnUiThread(() -> statusText.setText("Fall alert sent (no location)."));
            }
        });
    }

    private void playAlertSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                Log.d(TAG, "Alert sound played");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to play alert sound", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error playing alert sound", e);
        }
    }

    private void triggerVibration() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(1000);
                }
                Log.d(TAG, "Vibration triggered");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to trigger vibration", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error triggering vibration", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
        Log.d(TAG, "Sensor listeners registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        Log.d(TAG, "Sensor listeners unregistered");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (smsHelper != null) {
            smsHelper.unregisterReceivers();
        }
        Log.d(TAG, "MainActivity destroyed");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission denied: " + permissions[i], Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Permission denied: " + permissions[i]);
                    allGranted = false;
                }
            }

            if (allGranted) {
                Log.d(TAG, "All permissions granted");
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Some permissions denied - app functionality may be limited");
                Toast.makeText(this, "Some permissions denied - app functionality may be limited", Toast.LENGTH_LONG).show();
            }
        }
    }
}