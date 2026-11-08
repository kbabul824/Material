package com.example.datatimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class DataMonitorService extends Service {
    private static final String CHANNEL_ID = "DataMonitorChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long TIMEOUT_MILLIS = 10 * 60 * 1000;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private PowerManager.WakeLock wakeLock;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = buildNotification("Monitoring mobile data status...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DataTimer::CpuWakeLock");
        }

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                startTenMinuteTimer();
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                cancelTimer();
            }
        };

        if (connectivityManager != null) {
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
    }

    private synchronized void startTenMinuteTimer() {
        if (isTimerRunning) return;
        isTimerRunning = true;

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(11 * 60 * 1000L);
        }

        countDownTimer = new CountDownTimer(TIMEOUT_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                updateNotification(String.format("Mobile data ON. Turning off in %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                updateNotification("10-minute limit reached. Disabling mobile data...");
                triggerDataDisable();
                releaseWakeLock();
            }
        }.start();
    }

    private synchronized void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        isTimerRunning = false;
        updateNotification("Mobile data is OFF. Standing by...");
        releaseWakeLock();
    }

    private void triggerDataDisable() {
        Intent intent = new Intent(DataToggleAccessibilityService.ACTION_TRIGGER_DISABLE);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mobile Data Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Data Timeout Controller")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTimer();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        releaseWakeLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}