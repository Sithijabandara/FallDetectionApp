
# FallDetectionApp

An Android application that detects falls using the device’s **accelerometer** and **gyroscope** sensors and sends an SMS alert to a predefined emergency contact. This app is designed to support elderly individuals or those with medical conditions by providing real-time fall detection and emergency communication.

---

## 📱 Features

- Real-time monitoring using accelerometer and gyroscope
- Smart fall detection algorithm with threshold, impact, and cooldown logic
- Sends SMS alert with GPS location to emergency contact
- Fallback message when location is unavailable
- Adjustable sensitivity through the settings
- Enable/disable SMS alerts
- Sound and vibration notification options
- Manual testing option for SMS and location
- Lightweight and easy-to-use interface

---

## 🛠️ Environment Setup

- **IDE**: Android Studio Hedgehog 
- **Programming Language**: Java
- **Minimum SDK**: 21
- **Target SDK**: 33 (Android 13)
- **Gradle Plugin Version**: Compatible with API 33

---

## ⚙️ Permissions Required

Add these permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>
```

> ⚠️ Note: Runtime permissions are required for SMS and Location access on Android 6.0+ (API 23+).

---

## 🚀 How to Run the App

1. Clone or download this repository.
2. Open it in Android Studio.
3. Connect a physical Android device (SMS cannot be tested on emulator).
4. Build and run the app.
5. Grant all required permissions.
6. Add an emergency contact number via the UI.
7. Walk, shake, or simulate a fall to test the functionality.

---

## 📂 Project Structure

```
FallDetectionApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/falldetectionapp/
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── SMSHelper.java
│   │   │   │   ├── LocationHelper.java
│   │   │   │   ├── FallDetector.java
│   │   │   │   ├── SettingsActivity.java
│   │   │   │   └── EmergencyContactActivity.java
│   │   │   ├── res/layout/
│   │   │   └── AndroidManifest.xml
```

---

## 🔍 Fall Detection Algorithm

- Calculates total acceleration using accelerometer data.
- Uses thresholds to identify fall-like motion:
  - Sudden acceleration spike > `15.0 m/s²`
  - Followed by impact < `2.0 m/s²` or another spike > `12.0 m/s²`
  - Gyroscope detects angular velocity > `3.0 rad/s`
- Confirms fall after `3 detections` within `2 seconds`
- Prevents duplicate alerts using `10-second cooldown`
- Applies a moving average filter to smooth out sensor noise

---

## 📡 SMS Alert Functionality

- Sends SMS using `SmsManager` when a fall is confirmed.
- If GPS is available, it includes a Google Maps link.
- If location is unavailable, it sends a simplified emergency message.
- Registered `BroadcastReceiver` for both `SMS_SENT` and `SMS_DELIVERED` status.
- SMS feature can be toggled in the emergency contact screen.

---

## 🎨 User Interface Overview

### 5.1 Main Screen
- Real-time accelerometer and gyroscope readings
- Fall detection status: **Monitoring...** or **Fall Detected**
- Navigation buttons:
  - Emergency Contact
  - Settings

### 5.2 Settings Screen
- Adjust fall detection sensitivity using SeekBar
- Enable/Disable:
  - Sound alerts
  - Vibration alerts
- View app version in About section
- Back button for navigation

### 5.3 Emergency Contact Screen
- Enter and save emergency contact number
- Toggle to enable/disable SMS alerts
- Button to test location and SMS sending
- Back button for navigation

---

## 🧪 Testing & Scenarios

| Scenario               | Result                             |
|------------------------|------------------------------------|
| Light Movement         | No false alerts                    |
| Simulated Fall         | SMS sent with location (if available) |
| No GPS Signal          | Simple SMS message sent            |
| No Mobile Network      | Failure logged; user notified via Toast |

---

## 🧰 Troubleshooting

- Make sure to grant all required runtime permissions.
- SMS won't work on emulator – use a real Android device.
- Ensure SIM card and messaging service are active.

---

## 📄 Documentation

### Project Report
The project documentation includes detailed sections on environment setup, sensor access, fall detection logic, SMS implementation, UI structure, test scenarios, and troubleshooting techniques.

### Code Comments
All classes and methods are documented with in-line comments to help with maintainability and future development.

---

## 📖 License

This project is developed for educational use. You are free to use, modify, and distribute it with proper attribution.

---

## 👤 Author

**Sithija Bandara**  
Email: 
GitHub: 

---

## 📽️ Optional Demo

- GitHub Repo: 
- YouTube Demo: 
