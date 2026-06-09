package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

public final class AppPreferences {
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    public static final String KEYBOARD_THEME_FOLLOW_APP = "follow_app";
    public static final String KEYBOARD_THEME_LIGHT = "light";
    public static final String KEYBOARD_THEME_DARK = "dark";
    public static final String KEYBOARD_THEME_OLED = "oled";

    private static final String FILE_NAME = "appearance_preferences";
    private static final String APP_THEME = "app_theme";
    private static final String KEYBOARD_THEME = "keyboard_theme";
    private static final String KEY_HEIGHT = "key_height";
    private static final String NUMBER_ROW = "number_row";
    private static final String SUGGESTION_BAR = "suggestion_bar";
    private static final String HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String SOUND_FEEDBACK = "sound_feedback";
    private static final String ACCESSIBILITY_DISCLOSURE_ACCEPTED =
            "accessibility_disclosure_accepted";

    private AppPreferences() {
    }

    public static void applyTheme(Activity activity) {
        activity.setTheme(isAppDark(activity) ? R.style.AppThemeDark : R.style.AppThemeLight);
    }

    public static void applySystemBarAppearance(Activity activity) {
        int flags = activity.getWindow().getDecorView().getSystemUiVisibility();
        if (isAppDark(activity)) {
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

    public static String getAppTheme(Context context) {
        return preferences(context).getString(APP_THEME, THEME_SYSTEM);
    }

    public static void setAppTheme(Context context, String value) {
        preferences(context).edit().putString(APP_THEME, value).apply();
    }

    public static boolean isAppDark(Context context) {
        String theme = getAppTheme(context);
        if (THEME_DARK.equals(theme)) {
            return true;
        }
        if (THEME_LIGHT.equals(theme)) {
            return false;
        }
        return isSystemDark(context);
    }

    public static String getKeyboardTheme(Context context) {
        return preferences(context).getString(
                KEYBOARD_THEME,
                KEYBOARD_THEME_FOLLOW_APP
        );
    }

    public static void setKeyboardTheme(Context context, String value) {
        preferences(context).edit().putString(KEYBOARD_THEME, value).apply();
    }

    public static boolean isKeyboardDark(Context context) {
        String theme = getKeyboardTheme(context);
        if (KEYBOARD_THEME_DARK.equals(theme) || KEYBOARD_THEME_OLED.equals(theme)) {
            return true;
        }
        if (KEYBOARD_THEME_LIGHT.equals(theme)) {
            return false;
        }
        return isAppDark(context);
    }

    public static boolean isKeyboardOled(Context context) {
        return KEYBOARD_THEME_OLED.equals(getKeyboardTheme(context));
    }

    public static int getKeyHeight(Context context) {
        return preferences(context).getInt(KEY_HEIGHT, 50);
    }

    public static void setKeyHeight(Context context, int value) {
        preferences(context).edit().putInt(KEY_HEIGHT, value).apply();
    }

    public static boolean isNumberRowEnabled(Context context) {
        return preferences(context).getBoolean(NUMBER_ROW, true);
    }

    public static void setNumberRowEnabled(Context context, boolean value) {
        preferences(context).edit().putBoolean(NUMBER_ROW, value).apply();
    }

    public static boolean isSuggestionBarEnabled(Context context) {
        return preferences(context).getBoolean(SUGGESTION_BAR, true);
    }

    public static void setSuggestionBarEnabled(Context context, boolean value) {
        preferences(context).edit().putBoolean(SUGGESTION_BAR, value).apply();
    }

    public static boolean isHapticFeedbackEnabled(Context context) {
        return preferences(context).getBoolean(HAPTIC_FEEDBACK, true);
    }

    public static void setHapticFeedbackEnabled(Context context, boolean value) {
        preferences(context).edit().putBoolean(HAPTIC_FEEDBACK, value).apply();
    }

    public static boolean isSoundFeedbackEnabled(Context context) {
        return preferences(context).getBoolean(SOUND_FEEDBACK, false);
    }

    public static void setSoundFeedbackEnabled(Context context, boolean value) {
        preferences(context).edit().putBoolean(SOUND_FEEDBACK, value).apply();
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

    public static String appThemeLabel(Context context) {
        String value = getAppTheme(context);
        if (THEME_LIGHT.equals(value)) {
            return context.getString(R.string.theme_light);
        }
        if (THEME_DARK.equals(value)) {
            return context.getString(R.string.theme_dark);
        }
        return context.getString(R.string.theme_system);
    }

    public static String keyboardThemeLabel(Context context) {
        String value = getKeyboardTheme(context);
        if (KEYBOARD_THEME_LIGHT.equals(value)) {
            return context.getString(R.string.theme_light);
        }
        if (KEYBOARD_THEME_DARK.equals(value)) {
            return context.getString(R.string.theme_dark);
        }
        if (KEYBOARD_THEME_OLED.equals(value)) {
            return context.getString(R.string.theme_oled);
        }
        return context.getString(R.string.theme_follow_app);
    }

    public static String keyHeightLabel(Context context) {
        int value = getKeyHeight(context);
        if (value <= 44) {
            return context.getString(R.string.keyboard_height_compact);
        }
        if (value >= 56) {
            return context.getString(R.string.keyboard_height_tall);
        }
        return context.getString(R.string.keyboard_height_standard);
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
