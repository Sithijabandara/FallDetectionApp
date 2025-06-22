package com.example.falldetectionapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar sensitivitySeekBar;
    private Switch smsEnabledSwitch;        // Sound Alerts switch (sms_enabled)
    private Switch vibrationEnabledSwitch;  // Vibration Alerts switch
    private TextView sensitivityText;
    private Button backButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("FallDetectionPrefs", MODE_PRIVATE);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        sensitivitySeekBar = findViewById(R.id.sensitivitySeekBar);
        sensitivityText = findViewById(R.id.sensitivityText);
        smsEnabledSwitch = findViewById(R.id.soundEnabledSwitch);
        vibrationEnabledSwitch = findViewById(R.id.vibrationEnabledSwitch);
        backButton = findViewById(R.id.backButton);
    }

    private void loadSettings() {
        int sensitivity = prefs.getInt("sensitivity", 50);
        boolean smsEnabled = prefs.getBoolean("sms_enabled", true);
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);

        sensitivitySeekBar.setProgress(sensitivity);
        smsEnabledSwitch.setChecked(smsEnabled);
        vibrationEnabledSwitch.setChecked(vibrationEnabled);

        updateSensitivityText(sensitivity);
    }

    private void setupListeners() {
        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSensitivityText(progress);
                saveSensitivity(progress);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        smsEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sms_enabled", isChecked).apply();
        });

        vibrationEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply();
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void updateSensitivityText(int progress) {
        String level;
        if (progress <= 30) {
            level = "Low";
        } else if (progress <= 70) {
            level = "Medium";
        } else {
            level = "High";
        }

        sensitivityText.setText("Sensitivity: " + level + " (" + progress + "%)");
    }

    private void saveSensitivity(int sensitivity) {
        prefs.edit().putInt("sensitivity", sensitivity).apply();
    }
}
