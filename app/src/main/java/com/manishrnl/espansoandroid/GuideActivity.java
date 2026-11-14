package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public final class GuideActivity extends Activity {
    private static final String STATE_PENDING_RESTRICTED_SETUP =
            "pending_restricted_setup";

    private boolean pendingRestrictedSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_guide);

        pendingRestrictedSetup = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_PENDING_RESTRICTED_SETUP);
        findViewById(R.id.guideBackButton).setOnClickListener(view -> finish());
        findViewById(R.id.guideCreateShortcut).setOnClickListener(view ->
                startActivity(new Intent(this, EditorActivity.class)));
        findViewById(R.id.guideAccessibility).setOnClickListener(view ->
                openAccessibilitySetup());
        findViewById(R.id.guideRestrictedSetup).setOnClickListener(view ->
                startRestrictedSetup());
        findViewById(R.id.guideKeyboardSettings).setOnClickListener(view ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        findViewById(R.id.guideChooseKeyboard).setOnClickListener(view -> {
            InputMethodManager manager =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.showInputMethodPicker();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingRestrictedSetup) {
            pendingRestrictedSetup = false;
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_PENDING_RESTRICTED_SETUP, pendingRestrictedSetup);
        super.onSaveInstanceState(outState);
    }

    private void openAccessibilitySetup() {
        if (AppPreferences.isAccessibilityDisclosureAccepted(this)) {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.accessibility_disclosure_title)
                .setMessage(R.string.accessibility_disclosure_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.continue_label, (dialog, which) -> {
                    AppPreferences.setAccessibilityDisclosureAccepted(this, true);
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                })
                .show();
    }

    private void startRestrictedSetup() {
        pendingRestrictedSetup = true;
        Toast.makeText(
                this,
                R.string.restricted_settings_toast,
                Toast.LENGTH_LONG
        ).show();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}
