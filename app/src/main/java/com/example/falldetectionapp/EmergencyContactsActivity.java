package com.example.falldetectionapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyContactsActivity extends AppCompatActivity {

    private EditText contactInput;
    private Button saveContact, testLocationBtn, backButton;
    private Switch smsEnabledSwitch;
    private SMSHelper smsHelper;
    private LocationHelper locationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts); // Make sure this XML file exists in res/layout

        // Initialize helpers
        smsHelper = new SMSHelper(this);
        locationHelper = new LocationHelper(this);

        // Find views
        contactInput = findViewById(R.id.contactInput);
        saveContact = findViewById(R.id.saveContact);
        testLocationBtn = findViewById(R.id.testLocationBtn);
        backButton = findViewById(R.id.backButton);
        smsEnabledSwitch = findViewById(R.id.smsEnabledSwitch);

        // Load saved SMS switch value
        smsEnabledSwitch.setChecked(smsHelper.isSMSEnabled());

        // Save contact button logic
        saveContact.setOnClickListener(v -> {
            String phone = contactInput.getText().toString().trim();
            if (!phone.isEmpty() && phone.matches("^[+]?[0-9]{10,15}$")) {
                smsHelper.saveContact(phone);
                Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
                contactInput.setText("");
            } else {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
            }
        });

        // SMS switch logic
        smsEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            smsHelper.setSMSEnabled(isChecked);
            Toast.makeText(this, isChecked ? "SMS alerts enabled" : "SMS alerts disabled", Toast.LENGTH_SHORT).show();
        });

        // Location test button logic
        testLocationBtn.setOnClickListener(v -> {
            locationHelper.getCurrentLocation(new LocationHelper.LocationCallback() {
                @Override
                public void onLocationReceived(double latitude, double longitude, String address) {
                    runOnUiThread(() -> Toast.makeText(EmergencyContactsActivity.this,
                            "Location: " + address, Toast.LENGTH_LONG).show());
                }

                @Override
                public void onLocationError(String error) {
                    runOnUiThread(() -> Toast.makeText(EmergencyContactsActivity.this,
                            "Error: " + error, Toast.LENGTH_LONG).show());
                }
            });
        });

        // Back button
        backButton.setOnClickListener(v -> finish());
    }
}
