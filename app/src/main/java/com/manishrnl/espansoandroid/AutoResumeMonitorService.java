package com.manishrnl.espansoandroid;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;

import java.util.List;

public final class AutoResumeMonitorService extends Service {
    public static final String EXTRA_EXCLUDED_PACKAGE = "extra_excluded_package";
    private static final long CHECK_INTERVAL_MS = 800;
    private static final long MAX_MONITOR_MS = 15 * 60 * 1000; // 15 mins max

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String monitoredPackage = "";
    private long startTime;

    public static void startMonitoring(Context context, String packageName) {
        Intent intent = new Intent(context, AutoResumeMonitorService.class);
        intent.putExtra(EXTRA_EXCLUDED_PACKAGE, packageName);
        try {
            context.startService(intent);
        } catch (Exception ignored) {
            // Background start restrictions on Android 8+
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_EXCLUDED_PACKAGE)) {
            monitoredPackage = intent.getStringExtra(EXTRA_EXCLUDED_PACKAGE);
        }
        startTime = System.currentTimeMillis();
        handler.removeCallbacks(checkRunnable);
        handler.postDelayed(checkRunnable, CHECK_INTERVAL_MS);
        return START_NOT_STICKY;
    }

    private final Runnable checkRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - startTime > MAX_MONITOR_MS) {
                stopSelf();
                return;
            }

            if (hasExitedMonitoredApp()) {
                onMonitoredAppExited();
                stopSelf();
                return;
            }

            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    private boolean hasExitedMonitoredApp() {
        if (monitoredPackage == null || monitoredPackage.isEmpty()) {
            return true;
        }

        // 1. Check UsageStatsManager if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long now = System.currentTimeMillis();
                UsageEvents events = usm.queryEvents(now - 3000, now);
                UsageEvents.Event event = new UsageEvents.Event();
                String lastForeground = null;
                while (events != null && events.hasNextEvent()) {
                    events.getNextEvent(event);
                    if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                        lastForeground = event.getPackageName();
                    }
                }
                if (lastForeground != null) {
                    return !monitoredPackage.equals(lastForeground);
                }
            }
        }

        // 2. Check running processes
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                boolean foundInForeground = false;
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        if (process.pkgList != null) {
                            for (String pkg : process.pkgList) {
                                if (monitoredPackage.equals(pkg)) {
                                    foundInForeground = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!foundInForeground) {
                    return true;
                }
            }
        }

        return false;
    }

    private void onMonitoredAppExited() {
        if (AppPreferences.hasWriteSecureSettings(this)) {
            AppPreferences.setAccessibilityServiceEnabled(this, true);
        } else {
            showResumeNotification();
        }
    }

    private void showResumeNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        String channelId = "resume_safety_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }
        builder.setSmallIcon(R.mipmap.rnl_espanso_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.auto_resume_notification_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            nm.notify(1002, builder.build());
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(checkRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
