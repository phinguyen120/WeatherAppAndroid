package com.example.weatherapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.os.Looper;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

public class LocationHelper {
        private final Context context;
        private final FusedLocationProviderClient fusedLocationProviderClient;

        public LocationHelper(Context context) {
            this.context = context;
            this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        }

        @SuppressLint("MissingPermission")
        public void getCurrentLocation(LocationCallback locationCallback) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(10000);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            SettingsClient settingsClient = LocationServices.getSettingsClient(context);
            settingsClient.checkLocationSettings(builder.build())
                    .addOnSuccessListener(locationSettingsResponse -> {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof ResolvableApiException) {
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult((Activity) context, 1001);
                            } catch (IntentSender.SendIntentException sendEx) {
                                sendEx.printStackTrace();
                            }
                        }
                    });
        }
}
