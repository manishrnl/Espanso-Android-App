package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

public final class AppPreferences {
    private static final String FILE_NAME = "appearance_preferences";
    private static final String ACCESSIBILITY_DISCLOSURE_ACCEPTED =
            "accessibility_disclosure_accepted";

    private AppPreferences() {
    }

    public static void applyTheme(Activity activity) {
        activity.setTheme(
                isSystemDark(activity) ? R.style.AppThemeDark : R.style.AppThemeLight
        );
    }

    public static void applySystemBarAppearance(Activity activity) {
        int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
        if (isSystemDark(activity)) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        activity.getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public static boolean isAppDark(Context context) {
        return isSystemDark(context);
    }

    public static boolean isAccessibilityDisclosureAccepted(Context context) {
        return preferences(context).getBoolean(
                ACCESSIBILITY_DISCLOSURE_ACCEPTED,
                false
        );
    }

    public static void setAccessibilityDisclosureAccepted(
            Context context,
            boolean value
    ) {
        preferences(context)
                .edit()
                .putBoolean(ACCESSIBILITY_DISCLOSURE_ACCEPTED, value)
                .apply();
    }

    private static boolean isSystemDark(Context context) {
        int mode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
