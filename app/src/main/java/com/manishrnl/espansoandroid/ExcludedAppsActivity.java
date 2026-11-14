package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExcludedAppsActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ExcludedAppsAdapter adapter;
    private Switch masterSwitch;
    private RadioGroup modeRadioGroup;
    private RadioButton radioModeSmart;
    private RadioButton radioModeSystem;
    private View writeSecureSettingsCard;
    private TextView writeSecureStatusText;
    private TextView countSummaryText;
    private TextView emptyView;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_excluded_apps);

        ListView listView = findViewById(R.id.excludedAppsList);
        adapter = new ExcludedAppsAdapter(this);

        View header = LayoutInflater.from(this).inflate(
                R.layout.header_excluded_apps,
                listView,
                false
        );

        header.findViewById(R.id.excludedBackButton).setOnClickListener(v -> finish());

        masterSwitch = header.findViewById(R.id.excludedMasterSwitch);
        modeRadioGroup = header.findViewById(R.id.excludedModeRadioGroup);
        radioModeSmart = header.findViewById(R.id.radioModeSmart);
        radioModeSystem = header.findViewById(R.id.radioModeSystem);
        writeSecureSettingsCard = header.findViewById(R.id.writeSecureSettingsCard);
        writeSecureStatusText = header.findViewById(R.id.writeSecureStatusText);
        countSummaryText = header.findViewById(R.id.excludedCountSummary);
        emptyView = header.findViewById(R.id.emptyExcludedView);
        searchInput = header.findViewById(R.id.excludedSearchInput);

        setupMasterSwitch();
        setupModeControls(header);
        setupActionButtons(header);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        adapter.setOnDeleteClickListener(item -> {
            AppPreferences.removeExcludedApp(this, item.packageName);
            Toast.makeText(this, R.string.package_removed_toast, Toast.LENGTH_SHORT).show();
            reloadList();
        });

        listView.addHeaderView(header, null, false);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWriteSecureStatus();
        reloadList();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }

    private void setupMasterSwitch() {
        masterSwitch.setChecked(AppPreferences.isAutoPauseForPaymentAppsEnabled(this));
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPreferences.setAutoPauseForPaymentAppsEnabled(this, isChecked);
            updateModeVisibility(isChecked);
        });
        updateModeVisibility(masterSwitch.isChecked());
    }

    private void updateModeVisibility(boolean enabled) {
        modeRadioGroup.setEnabled(enabled);
        radioModeSmart.setEnabled(enabled);
        radioModeSystem.setEnabled(enabled);
        modeRadioGroup.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void setupModeControls(View header) {
        String currentMode = AppPreferences.getAutoPauseMode(this);
        if (AppPreferences.AUTO_PAUSE_MODE_SYSTEM.equals(currentMode)) {
            radioModeSystem.setChecked(true);
            writeSecureSettingsCard.setVisibility(View.VISIBLE);
        } else {
            radioModeSmart.setChecked(true);
            writeSecureSettingsCard.setVisibility(View.GONE);
        }

        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioModeSystem) {
                AppPreferences.setAutoPauseMode(this, AppPreferences.AUTO_PAUSE_MODE_SYSTEM);
                writeSecureSettingsCard.setVisibility(View.VISIBLE);
                updateWriteSecureStatus();
            } else {
                AppPreferences.setAutoPauseMode(this, AppPreferences.AUTO_PAUSE_MODE_SMART);
                writeSecureSettingsCard.setVisibility(View.GONE);
            }
        });

        header.findViewById(R.id.copyAdbButton).setOnClickListener(v -> copyAdbCommand());
    }

    private void updateWriteSecureStatus() {
        boolean granted = AppPreferences.hasWriteSecureSettings(this);
        if (granted) {
            writeSecureStatusText.setText(R.string.write_secure_settings_status_granted);
            writeSecureStatusText.setTextColor(getColor(R.color.success));
        } else {
            writeSecureStatusText.setText(R.string.write_secure_settings_status_not_granted);
            writeSecureStatusText.setTextColor(getColor(R.color.warning));
        }
    }

    private void copyAdbCommand() {
        String cmd = getString(R.string.write_secure_settings_adb_cmd);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("ADB Command", cmd);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.adb_command_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupActionButtons(View header) {
        header.findViewById(R.id.addAppButton).setOnClickListener(v -> showAppPickerDialog());
        header.findViewById(R.id.addCustomPackageButton).setOnClickListener(v -> showAddCustomPackageDialog());

        header.findViewById(R.id.excludedResetButton).setOnClickListener(v -> {
            int defaultCount = AppPreferences.getDefaultExcludedPackages().length;
            new AlertDialog.Builder(this)
                    .setTitle(R.string.reset_defaults_button)
                    .setMessage(getString(R.string.reset_defaults_confirm, defaultCount))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.reset_defaults_button, (dialog, which) -> {
                        AppPreferences.resetExcludedAppsToDefault(this);
                        Toast.makeText(this, R.string.defaults_restored, Toast.LENGTH_SHORT).show();
                        reloadList();
                    })
                    .show();
        });

        header.findViewById(R.id.clearAllButton).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.clear_all_apps)
                    .setMessage(R.string.clear_all_confirm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.clear_all_apps, (dialog, which) -> {
                        AppPreferences.clearAllExcludedApps(this);
                        Toast.makeText(this, R.string.all_apps_cleared, Toast.LENGTH_SHORT).show();
                        reloadList();
                    })
                    .show();
        });
    }

    private void reloadList() {
        executor.execute(() -> {
            Set<String> packageSet = AppPreferences.getExcludedPaymentApps(this);
            Set<String> defaultSet = new HashSet<>(Arrays.asList(AppPreferences.getDefaultExcludedPackages()));
            PackageManager pm = getPackageManager();

            List<ExcludedAppsAdapter.ExcludedAppItem> items = new ArrayList<>();
            for (String pkg : packageSet) {
                String appName;
                Drawable icon = null;
                boolean isInstalled = false;
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    appName = pm.getApplicationLabel(info).toString();
                    icon = pm.getApplicationIcon(info);
                    isInstalled = true;
                } catch (Exception ignored) {
                    appName = formatPackageFriendlyName(pkg);
                }

                boolean isDefault = defaultSet.contains(pkg);
                items.add(new ExcludedAppsAdapter.ExcludedAppItem(
                        pkg,
                        appName,
                        icon,
                        isInstalled,
                        isDefault
                ));
            }

            // Sort: installed first, then alphabetically
            items.sort((a, b) -> {
                if (a.isInstalled != b.isInstalled) {
                    return a.isInstalled ? -1 : 1;
                }
                return a.appName.compareToIgnoreCase(b.appName);
            });

            mainHandler.post(() -> {
                adapter.setData(items);
                if (searchInput.getText() != null && !searchInput.getText().toString().isEmpty()) {
                    adapter.filter(searchInput.getText().toString());
                }
                countSummaryText.setText(getString(R.string.excluded_apps_count, items.size()));
                emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private String formatPackageFriendlyName(String pkg) {
        if (pkg.contains("navi")) return "Navi";
        if (pkg.contains("paytm")) return "Paytm";
        if (pkg.contains("phonepe")) return "PhonePe";
        if (pkg.contains("paisa") || pkg.contains("gpay")) return "Google Pay";
        if (pkg.contains("upiapp") || pkg.contains("bhim")) return "BHIM UPI";
        if (pkg.contains("cred")) return "CRED";
        if (pkg.contains("jupiter")) return "Jupiter";
        if (pkg.contains("fi.money")) return "Fi Money";
        if (pkg.contains("niyo")) return "Niyo";
        if (pkg.contains("slice")) return "Slice";
        if (pkg.contains("super.money")) return "super.money";
        if (pkg.contains("simpl")) return "Simpl";
        if (pkg.contains("lazypay")) return "LazyPay";
        if (pkg.contains("fampay")) return "FamPay";
        if (pkg.contains("mobikwik")) return "MobiKwik";
        if (pkg.contains("freecharge")) return "Freecharge";
        if (pkg.contains("bharatpe")) return "BharatPe";
        if (pkg.contains("postpe")) return "PostPe";
        if (pkg.contains("bajaj")) return "Bajaj Finserv";
        if (pkg.contains("hdfc")) return "HDFC Bank";
        if (pkg.contains("paypal")) return "PayPal";
        if (pkg.contains("zerodha") || pkg.contains("kite")) return "Zerodha Kite";
        if (pkg.contains("groww")) return "Groww";
        if (pkg.contains("upstox")) return "Upstox";
        if (pkg.contains("angel")) return "Angel One";
        if (pkg.contains("onecard")) return "OneCard";
        if (pkg.contains("icici")) return "iMobile ICICI";
        if (pkg.contains("yono")) return "YONO SBI";
        if (pkg.contains("axis")) return "Axis Mobile";
        if (pkg.contains("indusind")) return "IndusInd Mobile";
        if (pkg.contains("pnb")) return "PNB ONE";
        if (pkg.contains("bankofbaroda")) return "bob World";
        if (pkg.contains("canarabank")) return "Canara ai1";
        if (pkg.contains("unionbank")) return "Union Bank";
        if (pkg.contains("idfc")) return "IDFC FIRST";
        if (pkg.contains("rbl")) return "RBL MoBank";
        if (pkg.contains("aubank")) return "AU 0101";

        int lastDot = pkg.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < pkg.length() - 1) {
            String tail = pkg.substring(lastDot + 1);
            if (tail.length() > 2) {
                return tail.substring(0, 1).toUpperCase(Locale.ROOT) + tail.substring(1);
            }
        }
        return pkg;
    }

    private void showAppPickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ListView pickerList = dialog.findViewById(R.id.pickerAppList);
        ProgressBar loadingProgress = dialog.findViewById(R.id.pickerLoadingProgress);
        TextView emptyPickerText = dialog.findViewById(R.id.pickerEmptyText);
        EditText searchPicker = dialog.findViewById(R.id.pickerSearchInput);
        Button closeButton = dialog.findViewById(R.id.pickerCloseButton);

        AppPickerAdapter pickerAdapter = new AppPickerAdapter(this);
        pickerList.setAdapter(pickerAdapter);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        searchPicker.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pickerAdapter.filter(s != null ? s.toString() : "");
                emptyPickerText.setVisibility(pickerAdapter.getCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        pickerList.setOnItemClickListener((parent, view, position, id) -> {
            AppPickerAdapter.PickerAppItem item = pickerAdapter.getItem(position);
            if (item.isAlreadyAdded) {
                Toast.makeText(this, R.string.app_already_excluded, Toast.LENGTH_SHORT).show();
                return;
            }
            AppPreferences.addExcludedApp(this, item.packageName);
            item.isAlreadyAdded = true;
            pickerAdapter.notifyDataSetChanged();
            Toast.makeText(this, R.string.package_added_toast, Toast.LENGTH_SHORT).show();
            reloadList();
            dialog.dismiss();
        });

        dialog.show();

        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);
            Set<String> currentlyExcluded = AppPreferences.getExcludedPaymentApps(this);
            String myPackage = getPackageName();

            List<AppPickerAdapter.PickerAppItem> pickerItems = new ArrayList<>();
            for (ApplicationInfo info : installedApps) {
                if (info.packageName.equals(myPackage)) {
                    continue;
                }
                CharSequence label = pm.getApplicationLabel(info);
                String name = label != null ? label.toString() : info.packageName;
                Drawable icon = pm.getApplicationIcon(info);
                boolean isAdded = currentlyExcluded.contains(info.packageName);

                pickerItems.add(new AppPickerAdapter.PickerAppItem(
                        info.packageName,
                        name,
                        icon,
                        isAdded
                ));
            }

            pickerItems.sort((a, b) -> a.appName.compareToIgnoreCase(b.appName));

            mainHandler.post(() -> {
                loadingProgress.setVisibility(View.GONE);
                pickerAdapter.setData(pickerItems);
                emptyPickerText.setVisibility(pickerItems.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showAddCustomPackageDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_package);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText input = dialog.findViewById(R.id.packageInputText);
        Button cancel = dialog.findViewById(R.id.packageCancelButton);
        Button add = dialog.findViewById(R.id.packageAddButton);

        cancel.setOnClickListener(v -> dialog.dismiss());
        add.setOnClickListener(v -> {
            String pkg = input.getText() != null ? input.getText().toString().trim() : "";
            if (pkg.length() < 3 || !pkg.contains(".")) {
                Toast.makeText(this, R.string.invalid_package_name, Toast.LENGTH_SHORT).show();
                return;
            }
            AppPreferences.addExcludedApp(this, pkg);
            Toast.makeText(this, R.string.package_added_toast, Toast.LENGTH_SHORT).show();
            reloadList();
            dialog.dismiss();
        });

        dialog.show();
    }
}
