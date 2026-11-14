package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.Settings;
import android.view.Window;
import android.widget.Button;

public final class PermissionsDialogHelper {
    public static final int REQUEST_CODE_NOTIFICATIONS = 9001;

    private PermissionsDialogHelper() {
    }

    public static void show(Activity activity) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_permissions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        Button notifBtn = dialog.findViewById(R.id.permissionNotificationButton);
        Button usageBtn = dialog.findViewById(R.id.permissionUsageButton);
        Button doneBtn = dialog.findViewById(R.id.permissionDoneButton);

        updateButtonStates(activity, notifBtn, usageBtn);

        notifBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.requestPermissions(
                        new String[]{"android.permission.POST_NOTIFICATIONS"},
                        REQUEST_CODE_NOTIFICATIONS
                );
            }
            dialog.dismiss();
        });

        usageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            dialog.dismiss();
        });

        doneBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkCallingOrSelfPermission("android.permission.POST_NOTIFICATIONS")
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void checkAndPromptInitial(Activity activity) {
        // If notifications not granted on Android 13+, or usage access needed, prompt user
        if (!hasNotificationPermission(activity)) {
            show(activity);
        }
    }

    private static void updateButtonStates(Context context, Button notifBtn, Button usageBtn) {
        boolean hasNotif = hasNotificationPermission(context);
        if (hasNotif) {
            notifBtn.setText(R.string.permission_granted_badge);
            notifBtn.setEnabled(false);
            notifBtn.setAlpha(0.6f);
        } else {
            notifBtn.setText(R.string.permission_grant_button);
            notifBtn.setEnabled(true);
            notifBtn.setAlpha(1.0f);
        }

        boolean hasUsage = AppPreferences.hasUsageStatsPermission(context);
        if (hasUsage) {
            usageBtn.setText(R.string.permission_granted_badge);
            usageBtn.setEnabled(false);
            usageBtn.setAlpha(0.6f);
        } else {
            usageBtn.setText(R.string.permission_grant_button);
            usageBtn.setEnabled(true);
            usageBtn.setAlpha(1.0f);
        }
    }
}
