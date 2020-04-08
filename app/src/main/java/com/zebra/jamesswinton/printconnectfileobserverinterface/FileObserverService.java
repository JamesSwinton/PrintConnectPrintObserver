package com.zebra.jamesswinton.printconnectfileobserverinterface;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.FileUtils.readFileToString;

public class FileObserverService extends Service {

    // Debugging
    private static final String TAG = "FileObserverService";
    private static final String ON_EVENT_TAG = "FOS - onEvent";

    // Constants
    private static final int PRINT_SUCCESS = 0;
    private static final int BACKGROUND_SERVICE_NOTIFICATION = 1;

    // Intents
    private static final String ACTION_STOP_SERVICE = "com.zebra.jamesswinton.fileobserver.STOP_SERVICE";

    private static final String PRINT_CONNECT_ERROR_INTENT = "com.zebra.printconnect.PrintService.ERROR_MESSAGE";

    // FileObservation
    private static FileObserver mFileObserver = null;
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();

        // Start Service
        startForeground(BACKGROUND_SERVICE_NOTIFICATION, createServiceNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service Started");

        // Handle Intent
        if (intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_STOP_SERVICE)) {
                // Stop Service
                stopSelf();

                // Stop
                return START_NOT_STICKY;
            }
        }

        //Start Observation
        startObservingDirectory();

        // Start Sticky
        return START_STICKY;
    }

    private Notification createServiceNotification() {
        // Create Variables
        String channelId = "com.zebra.fileobserverinterface";
        String channelName = "Custom Background Notification Channel";

        // Create Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Set Channel
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        // Build Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,
                channelId);

        // Build StopService action
        Intent stopIntent = new Intent(this, FileObserverService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(this,
                0, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action stopServiceAction = new NotificationCompat.Action(
                R.drawable.ic_file,
                "Stop Service",
                stopPendingIntent
        );

        // Return Build Notification object
        return notificationBuilder
                .setContentTitle("File Observer Active")
                .setContentInfo("Observing path: " + " for new ZPL files")
                .setSmallIcon(R.drawable.ic_file)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .addAction(stopServiceAction)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void startObservingDirectory() {
        // Init File Observer
        String basePath = Environment.getExternalStorageDirectory().getPath() + "/download/";
        mFileObserver = new FileObserver(basePath) {
            @Override
            public void onEvent(int event, String filePath) {
                // Validate Path
                if (filePath == null) {
                    Log.w(TAG, "No path found, ignoring");
                    return;
                }

                // Monitor Creation Events
                if (event == FileObserver.CREATE) {
                    Log.i(ON_EVENT_TAG, "Event: " + event + " | New file detected: " + filePath);

                    // Verify ZPL
                    if (filePath.endsWith(".zpl")) {
                        Log.i(ON_EVENT_TAG, "Event: " + event + " | ZPL File detected: " + filePath);

                        // Get Bytes from File
                        try {
                            // Convert to Byte[] (Apache Commons IO)
                            File zplFile = new File(basePath + filePath);
                            String zplString = readFileToString(zplFile);
                            byte[] zplBytes = zplString.getBytes(StandardCharsets.UTF_8);
                            Log.i(ON_EVENT_TAG, "Event: " + event + " | ZPL Converted to byte[] - sending: " + zplFile.getAbsolutePath() + " to printer");

                            // Send to PrintConnect
                            PrintHandler.sendPrintJobWithContent(FileObserverService.this,
                                    zplBytes, new HashMap<>(), (new ResultReceiver(null) {
                                    @Override
                                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                                        Log.i(ON_EVENT_TAG, "Event: " + event + " | Print Result: " + resultCode);

                                        if (resultCode == PRINT_SUCCESS) {
                                            Log.i(ON_EVENT_TAG, "File Printed, deleting file");

                                            // Delete file
                                            boolean deleted = zplFile.delete();
                                            Log.i(ON_EVENT_TAG, "File deleted: " + deleted);

                                            // Handle successful print
                                            String toastMessage = "File Printed: " + filePath
                                                    + "\n\n File Deleted: " + deleted;
                                            mHandler.post(() -> Toast.makeText(
                                                    FileObserverService.this,
                                                    toastMessage, Toast.LENGTH_LONG).show());
                                        } else {
                                            // Handle unsuccessful print
                                            String errorMessage = resultData.getString(PRINT_CONNECT_ERROR_INTENT);

                                            // Handle unsuccessful print
                                            Log.e(ON_EVENT_TAG, "Event: " + event + " | Print Error: " + errorMessage + " | " + resultCode + " | " + resultData);
                                            mHandler.post(() -> Toast.makeText(FileObserverService.this, "Error Printing File: " + errorMessage, Toast.LENGTH_LONG).show());
                                        }
                                    }
                                }));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        // Start Observer
        mFileObserver.startWatching();
    }
}
