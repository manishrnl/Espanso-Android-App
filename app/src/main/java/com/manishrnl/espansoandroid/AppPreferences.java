package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class AppPreferences {
    private static final String FILE_NAME = "appearance_preferences";
    private static final String ACCESSIBILITY_DISCLOSURE_ACCEPTED =
            "accessibility_disclosure_accepted";
    private static final String THEME_MODE = "theme_mode";
    private static final String PASSWORD_EXPANSION_ENABLED =
            "password_expansion_enabled";
    private static final String AUTO_PAUSE_PAYMENT_ENABLED =
            "auto_pause_payment_enabled";
    private static final String EXCLUDED_PAYMENT_APPS =
            "excluded_payment_apps";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private static final String[] DEFAULT_EXCLUDED_PACKAGES = {
            "com.naviapp",
            "net.one97.paytm",
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user",
            "in.org.npci.upiapp",
            "com.dreamplug.android.cred",
            "money.jupiter",
            "com.drivetrackplusrefuel",
            "com.fi.money",
            "com.goniyo.pay",
            "indwin.c3.shareapp",
            "com.super.money",
            "com.getsimpl.app",
            "com.lazypay.android",
            "com.fampay.in",
            "com.mobikwik_new",
            "com.freecharge.android",
            "com.bharatpe.app",
            "com.postpe.app",
            "com.bajajfinserv",
            "com.snapwork.hdfc",
            "com.paypal.android.p2pmobile",
            "com.zerodha.kite3",
            "com.nextbillion.groww",
            "com.upstox.pro",
            "com.msf.angelmobile",
            "com.onecard",
            "com.csam.icici.bank.imobile",
            "com.sbi.yono",
            "com.sbi.yonolite",
            "com.axis.mobile",
            "com.msf.k8800",
            "com.indusind.mobile",
            "com.pnb.pnbone",
            "com.bankofbaroda.mconnect",
            "com.canarabank.ai1",
            "com.unionbank.online",
            "com.idfcfirstbank.banking",
            "com.rblbank.mobank",
            "com.aubank.mobile"
    };

    private AppPreferences() {
    }

    public static void applyTheme(Activity activity) {
        activity.setTheme(
                isAppDark(activity) ? R.style.AppThemeDark : R.style.AppThemeLight
        );
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

    public static boolean isAppDark(Context context) {
        String mode = getThemeMode(context);
        if (THEME_DARK.equals(mode)) {
            return true;
        }
        if (THEME_LIGHT.equals(mode)) {
            return false;
        }
        return isSystemDark(context);
    }

    public static String getThemeMode(Context context) {
        return preferences(context).getString(THEME_MODE, THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, String mode) {
        if (!THEME_LIGHT.equals(mode) && !THEME_DARK.equals(mode)) {
            mode = THEME_SYSTEM;
        }
        preferences(context).edit().putString(THEME_MODE, mode).apply();
    }

    public static boolean isPasswordExpansionEnabled(Context context) {
        return preferences(context).getBoolean(PASSWORD_EXPANSION_ENABLED, false);
    }

    public static void setPasswordExpansionEnabled(Context context, boolean enabled) {
        preferences(context)
                .edit()
                .putBoolean(PASSWORD_EXPANSION_ENABLED, enabled)
                .apply();
    }

    public static boolean isAutoPauseForPaymentAppsEnabled(Context context) {
        return preferences(context).getBoolean(AUTO_PAUSE_PAYMENT_ENABLED, true);
    }

    public static void setAutoPauseForPaymentAppsEnabled(Context context, boolean enabled) {
        preferences(context)
                .edit()
                .putBoolean(AUTO_PAUSE_PAYMENT_ENABLED, enabled)
                .apply();
    }

    public static final String AUTO_PAUSE_MODE_SMART = "smart_pause";
    public static final String AUTO_PAUSE_MODE_SYSTEM = "system_disable";
    private static final String AUTO_PAUSE_MODE = "auto_pause_mode";

    public static String getAutoPauseMode(Context context) {
        return preferences(context).getString(AUTO_PAUSE_MODE, AUTO_PAUSE_MODE_SMART);
    }

    public static void setAutoPauseMode(Context context, String mode) {
        if (!AUTO_PAUSE_MODE_SYSTEM.equals(mode)) {
            mode = AUTO_PAUSE_MODE_SMART;
        }
        preferences(context).edit().putString(AUTO_PAUSE_MODE, mode).apply();
    }

    public static String[] getDefaultExcludedPackages() {
        return DEFAULT_EXCLUDED_PACKAGES.clone();
    }

    public static Set<String> getExcludedPaymentApps(Context context) {
        Set<String> defaultSet = new HashSet<>(Arrays.asList(DEFAULT_EXCLUDED_PACKAGES));
        return preferences(context).getStringSet(EXCLUDED_PAYMENT_APPS, defaultSet);
    }

    public static void setExcludedPaymentApps(Context context, Set<String> apps) {
        preferences(context)
                .edit()
                .putStringSet(EXCLUDED_PAYMENT_APPS, new HashSet<>(apps))
                .apply();
    }

    public static void addExcludedApp(Context context, String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return;
        }
        Set<String> set = new HashSet<>(getExcludedPaymentApps(context));
        set.add(packageName.trim().toLowerCase(Locale.US));
        setExcludedPaymentApps(context, set);
    }

    public static void removeExcludedApp(Context context, String packageName) {
        if (packageName == null) {
            return;
        }
        Set<String> set = new HashSet<>(getExcludedPaymentApps(context));
        set.remove(packageName.trim().toLowerCase(Locale.US));
        setExcludedPaymentApps(context, set);
    }

    public static void resetExcludedAppsToDefault(Context context) {
        setExcludedPaymentApps(context, new HashSet<>(Arrays.asList(DEFAULT_EXCLUDED_PACKAGES)));
    }

    public static void clearAllExcludedApps(Context context) {
        setExcludedPaymentApps(context, new HashSet<>());
    }

    public static boolean isPackageExcluded(Context context, CharSequence packageName) {
        if (packageName == null) {
            return false;
        }
        String pkg = packageName.toString().trim().toLowerCase(Locale.US);

        // Navi app package variants always match
        if (pkg.contains("navi") || pkg.equals("com.naviapp") || pkg.equals("com.navi.passport")) {
            return isAutoPauseForPaymentAppsEnabled(context);
        }

        if (!isAutoPauseForPaymentAppsEnabled(context)) {
            return false;
        }

        Set<String> excluded = getExcludedPaymentApps(context);
        if (excluded.contains(pkg)) {
            return true;
        }
        for (String item : excluded) {
            if (pkg.equals(item) || pkg.startsWith(item + ".")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPackageExcludedForPaymentSafety(Context context, CharSequence packageName) {
        return isPackageExcluded(context, packageName);
    }

    public static boolean hasWriteSecureSettings(Context context) {
        return context.checkCallingOrSelfPermission(
                "android.permission.WRITE_SECURE_SETTINGS"
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasUsageStatsPermission(Context context) {
        try {
            android.app.AppOpsManager appOps =
                    (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) {
                return false;
            }
            int mode = appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.getPackageName()
            );
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean setAccessibilityServiceEnabled(Context context, boolean enabled) {
        String serviceComponent = context.getPackageName() + "/"
                + TextExpansionAccessibilityService.class.getName();
        try {
            String currentServices = android.provider.Settings.Secure.getString(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            Set<String> set = new HashSet<>();
            if (currentServices != null && !currentServices.isEmpty()) {
                String[] parts = currentServices.split(":");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        set.add(trimmed);
                    }
                }
            }
            if (enabled) {
                set.add(serviceComponent);
            } else {
                set.remove(serviceComponent);
            }
            StringBuilder sb = new StringBuilder();
            for (String item : set) {
                if (sb.length() > 0) {
                    sb.append(":");
                }
                sb.append(item);
            }
            android.provider.Settings.Secure.putString(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    sb.toString()
            );
            android.provider.Settings.Secure.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                    set.isEmpty() ? 0 : 1
            );
            return true;
        } catch (SecurityException ignored) {
            return false;
        }
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
