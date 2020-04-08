package com.zebra.jamesswinton.printconnectfileobserverinterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    // Debugging
    private static final String TAG = "MainActivity";

    // Constants
    private static final int PERMISSIONS_REQUEST = 0;
    private static final String[] PERMISSIONS = {
            WRITE_EXTERNAL_STORAGE
    };

    // Private Variables


    // Public Variables


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkStandardPermissions()) {
            // Start Service
            startService(new Intent(this, FileObserverService.class));
            // Finish
            finish();
        } else {
            // Request Permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_REQUEST);
        }
    }

    public boolean checkStandardPermissions() {
        boolean permissionsGranted = true;
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                permissionsGranted = false;
                break;
            }
        }

        return permissionsGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        // Handle Permissions Request
        if (requestCode == PERMISSIONS_REQUEST) {
            Log.i(TAG, "Permissions Request Complete - checking permissions granted...");

            // Validate Permissions State
            boolean permissionsGranted = true;
            if (results.length > 0) {
                for (int result : results) {
                    if (result != PERMISSION_GRANTED) {
                        permissionsGranted = false;
                    }
                }
            } else {
                permissionsGranted = false;
            }

            // Check Permissions were granted & Load slide images or exit
            if (permissionsGranted) {
                Log.i(TAG, "Permissions Granted, loading slide images...");

                // Start Service
                startService(new Intent(this, FileObserverService.class));
                // Finish
                finish();
            } else {
                Log.e(TAG, "Permissions Denied - Exiting App");

                // Explain reason
                Toast.makeText(this, "Please enable all permissions to run this app",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
