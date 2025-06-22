package com.example.falldetectionapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {

    private Context context;
    private LocationManager locationManager;
    private Geocoder geocoder;

    private static final int LOCATION_TIMEOUT = 30000;       // 30 seconds timeout
    private static final int LOCATION_CHECK_INTERVAL = 5000; // check every 5 seconds

    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude, String address);
        void onLocationError(String error);
    }

    private Location lastKnownLocation = null;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable checkLocationRunnable;
    private LocationListener locationListener;

    public LocationHelper(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.geocoder = new Geocoder(context, Locale.getDefault());
    }

    public void getCurrentLocation(LocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission not granted");
            return;
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services disabled");
            return;
        }

        lastKnownLocation = null; // reset previous location

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastKnownLocation = location;
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        try {
            // Request location updates from GPS if enabled, else from Network provider
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            } else {
                callback.onLocationError("No location providers available");
                return;
            }
        } catch (SecurityException e) {
            callback.onLocationError("Security exception: " + e.getMessage());
            return;
        }

        final long startTime = System.currentTimeMillis();

        // Runnable to check location every 5 seconds
        checkLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (lastKnownLocation != null) {
                    // Got location! Stop updates and callback
                    locationManager.removeUpdates(locationListener);

                    double lat = lastKnownLocation.getLatitude();
                    double lon = lastKnownLocation.getLongitude();
                    String address = getAddressFromLocation(lat, lon);

                    callback.onLocationReceived(lat, lon, address);
                } else {
                    // No location yet, check timeout
                    if (System.currentTimeMillis() - startTime >= LOCATION_TIMEOUT) {
                        // Timeout reached, stop updates and callback error
                        locationManager.removeUpdates(locationListener);
                        callback.onLocationError("Location request timed out");
                    } else {
                        // Not timed out, schedule next check after 5 seconds
                        handler.postDelayed(this, LOCATION_CHECK_INTERVAL);
                    }
                }
            }
        };

        // Start checking immediately
        handler.post(checkLocationRunnable);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                if (address.getFeatureName() != null) sb.append(address.getFeatureName()).append(", ");
                if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
                if (address.getLocality() != null) sb.append(address.getLocality()).append(", ");
                if (address.getAdminArea() != null) sb.append(address.getAdminArea()).append(", ");
                if (address.getCountryName() != null) sb.append(address.getCountryName());

                String result = sb.toString();
                return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
            }
        } catch (IOException e) {
            // Geocoding failed; ignore and fallback below
        }

        // Fallback: just return coordinates
        return String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude);
    }
}
