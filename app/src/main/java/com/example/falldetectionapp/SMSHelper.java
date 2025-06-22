package com.example.falldetectionapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SMSHelper {

    private final Context context;
    private final SharedPreferences prefs;
    private static final String TAG = "SMSHelper";

    private static final String PREF_NAME = "FallDetectionPrefs";
    private static final String CONTACT_KEY = "emergency_contact";
    private static final String SMS_ENABLED_KEY = "sms_enabled";

    // Define your SMS action strings here for PendingIntent
    private static final String SMS_SENT_ACTION = "SMS_SENT";
    private static final String SMS_DELIVERED_ACTION = "SMS_DELIVERED";

    private boolean receiversRegistered = false;
    private boolean hasTriedSimpleMessage = false; // Track if we've already tried simple message

    public SMSHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        registerReceivers();
    }

    // Save emergency contact phone number
    public void saveContact(String phoneNumber) {
        prefs.edit().putString(CONTACT_KEY, phoneNumber).apply();
        Log.d(TAG, "Emergency contact saved");
    }

    // Retrieve saved contact phone number
    public String getContact() {
        return prefs.getString(CONTACT_KEY, "");
    }

    // Check if contact has been set
    public boolean isContactSet() {
        return !getContact().isEmpty();
    }

    // Check if SMS alerts are enabled
    public boolean isSMSEnabled() {
        return prefs.getBoolean(SMS_ENABLED_KEY, true);
    }

    // Enable or disable SMS alerts
    public void setSMSEnabled(boolean enabled) {
        prefs.edit().putBoolean(SMS_ENABLED_KEY, enabled).apply();
    }

    // Check if SMS permission is granted
    private boolean hasSMSPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Send fallback SMS (no location available)
    public void sendAlert() {
        sendAlert(0, 0, "Location unavailable");
    }

    // Send SMS with location if available
    public void sendAlert(double latitude, double longitude, String address) {
        Log.d(TAG, "sendAlert called");
        hasTriedSimpleMessage = false; // Reset the flag for new alert

        String contact = getContact();
        Log.d(TAG, "Emergency contact: " + (contact.isEmpty() ? "NOT SET" : "SET - " + maskPhoneNumber(contact)));

        if (contact.isEmpty()) {
            String message = "No emergency contact set";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, message);
            return;
        }

        if (!isSMSEnabled()) {
            String message = "SMS alerts are disabled";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            Log.w(TAG, message);
            return;
        }

        if (!hasSMSPermission()) {
            String message = "SMS permission not granted";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            Log.e(TAG, message);
            return;
        }

        sendSMSMessage(latitude, longitude, address, false);
    }

    // Internal method to send SMS with retry logic
    private void sendSMSMessage(double latitude, double longitude, String address, boolean useSimpleMessage) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message;

            if (useSimpleMessage) {
                message = buildSimpleEmergencyMessage();
                Log.d(TAG, "Sending simple emergency message");
            } else {
                message = buildEmergencyMessage(latitude, longitude, address);
                Log.d(TAG, "Sending detailed emergency message");
            }

            String contact = getContact();
            Log.d(TAG, "Attempting to send SMS to: " + maskPhoneNumber(contact));
            Log.d(TAG, "Message length: " + message.length());
            Log.d(TAG, "Message content: " + message);

            // Prepare PendingIntents for sent and delivered status
            PendingIntent sentPI = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent(SMS_SENT_ACTION)
                            .putExtra("latitude", latitude)
                            .putExtra("longitude", longitude)
                            .putExtra("address", address)
                            .putExtra("isSimpleMessage", useSimpleMessage),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            PendingIntent deliveredPI = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent(SMS_DELIVERED_ACTION),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            // If message is long, split and send multipart text message
            if (message.length() > 160) {
                Log.d(TAG, "Sending multipart SMS");
                ArrayList<String> messageParts = smsManager.divideMessage(message);
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

                // Add the same PendingIntent for each part
                for (int i = 0; i < messageParts.size(); i++) {
                    sentIntents.add(sentPI);
                    deliveredIntents.add(deliveredPI);
                }

                smsManager.sendMultipartTextMessage(contact, null, messageParts, sentIntents, deliveredIntents);
            } else {
                Log.d(TAG, "Sending single SMS");
                // Send single part message
                smsManager.sendTextMessage(contact, null, message, sentPI, deliveredPI);
            }

            String successMessage = "Emergency SMS sent to " + maskPhoneNumber(contact);
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show();
            Log.d(TAG, successMessage);

        } catch (SecurityException e) {
            String errorMessage = "SMS permission denied: " + e.getMessage();
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMessage, e);
        } catch (Exception e) {
            String errorMessage = "Failed to send SMS: " + e.getMessage();
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMessage, e);
        }
    }

    // Compose detailed emergency message
    private String buildEmergencyMessage(double latitude, double longitude, String address) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        StringBuilder message = new StringBuilder();
        message.append("EMERGENCY: Fall detected!\n");
        message.append("\nTime: ").append(timestamp).append("\n");

        if (latitude != 0 && longitude != 0 && !"Location unavailable".equalsIgnoreCase(address)) {
            message.append("Location: ")
                    .append(String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude))
                    .append("\nAddress: ").append(address)
                    .append("\nGoogle Maps: https://maps.google.com/?q=")
                    .append(latitude).append(",").append(longitude);
        } else {
            message.append("Location: Unable to determine location");
        }

        message.append("\n\nPlease check on me immediately!");

        return message.toString();
    }

    // Compose simple emergency message (fallback)
    private String buildSimpleEmergencyMessage() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        return "EMERGENCY: Fall detected at " + timestamp + ". Please check on me immediately!";
    }

    // Mask phone number for display in Toasts
    private String maskPhoneNumber(String phone) {
        if (phone.length() > 4) {
            return "*****" + phone.substring(phone.length() - 4);
        }
        return phone;
    }

    // Broadcast receiver for SMS sent status
    private final BroadcastReceiver smsSentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            String address = intent.getStringExtra("address");
            boolean isSimpleMessage = intent.getBooleanExtra("isSimpleMessage", false);

            switch (getResultCode()) {
                case android.app.Activity.RESULT_OK:
                    String successMsg = "SMS sent successfully";
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, successMsg);
                    hasTriedSimpleMessage = false; // Reset flag on success
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    String genericError = "SMS failed: Generic failure";
                    Log.e(TAG, genericError);

                    // If this was a detailed message and we haven't tried simple message yet, try simple message
                    if (!isSimpleMessage && !hasTriedSimpleMessage) {
                        hasTriedSimpleMessage = true;
                        Log.d(TAG, "Retrying with simple message");
                        Toast.makeText(context, "Retrying with simple message...", Toast.LENGTH_SHORT).show();
                        sendSMSMessage(latitude, longitude, address, true);
                    } else {
                        Toast.makeText(context, genericError, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    String noServiceError = "SMS failed: No service";
                    Toast.makeText(context, noServiceError, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, noServiceError);
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    String nullPduError = "SMS failed: Null PDU";
                    Toast.makeText(context, nullPduError, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, nullPduError);
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    String radioOffError = "SMS failed: Radio off";
                    Toast.makeText(context, radioOffError, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, radioOffError);
                    break;
                default:
                    String unknownError = "SMS failed: Unknown error (" + getResultCode() + ")";
                    Toast.makeText(context, unknownError, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, unknownError);
                    break;
            }
        }
    };

    // Broadcast receiver for SMS delivered status
    private final BroadcastReceiver smsDeliveredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case android.app.Activity.RESULT_OK:
                    String deliveredMsg = "SMS delivered";
                    Toast.makeText(context, deliveredMsg, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, deliveredMsg);
                    break;
                case android.app.Activity.RESULT_CANCELED:
                    String notDeliveredMsg = "SMS not delivered";
                    Toast.makeText(context, notDeliveredMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, notDeliveredMsg);
                    break;
                default:
                    String unknownDeliveryMsg = "SMS delivery status unknown (" + getResultCode() + ")";
                    Log.w(TAG, unknownDeliveryMsg);
                    break;
            }
        }
    };

    // Register broadcast receivers
    private void registerReceivers() {
        if (!receiversRegistered) {
            try {
                IntentFilter sentFilter = new IntentFilter(SMS_SENT_ACTION);
                IntentFilter deliveredFilter = new IntentFilter(SMS_DELIVERED_ACTION);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ (API 33+) requires RECEIVER_EXPORTED flag
                    context.registerReceiver(smsSentReceiver, sentFilter, Context.RECEIVER_EXPORTED);
                    context.registerReceiver(smsDeliveredReceiver, deliveredFilter, Context.RECEIVER_EXPORTED);
                } else {
                    // Pre-Android 13 - use standard registration
                    context.registerReceiver(smsSentReceiver, sentFilter);
                    context.registerReceiver(smsDeliveredReceiver, deliveredFilter);
                }

                receiversRegistered = true;
                Log.d(TAG, "SMS receivers registered");
            } catch (Exception e) {
                Log.e(TAG, "Error registering SMS receivers", e);
            }
        }
    }

    // Unregister broadcast receivers
    public void unregisterReceivers() {
        if (receiversRegistered) {
            try {
                context.unregisterReceiver(smsSentReceiver);
                context.unregisterReceiver(smsDeliveredReceiver);
                receiversRegistered = false;
                Log.d(TAG, "SMS receivers unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering SMS receivers", e);
            }
        }
    }

    // Test method to manually send SMS
    public void testSMS() {
        Log.d(TAG, "Testing SMS manually");
        Toast.makeText(context, "Testing SMS...", Toast.LENGTH_SHORT).show();
        sendAlert(0, 0, "Test location");
    }
}