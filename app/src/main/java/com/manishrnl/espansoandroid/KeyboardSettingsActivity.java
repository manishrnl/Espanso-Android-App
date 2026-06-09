package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

public final class KeyboardSettingsActivity extends Activity {
    private Button appThemeButton;
    private Button keyboardThemeButton;
    private Button keyHeightButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_keyboard_settings);

        appThemeButton = findViewById(R.id.appThemeButton);
        keyboardThemeButton = findViewById(R.id.keyboardThemeButton);
        keyHeightButton = findViewById(R.id.keyHeightButton);
        Switch numberRowSwitch = findViewById(R.id.numberRowSwitch);
        Switch symbolHintsSwitch = findViewById(R.id.symbolHintsSwitch);
        Switch suggestionBarSwitch = findViewById(R.id.suggestionBarSwitch);
        Switch hapticSwitch = findViewById(R.id.hapticSwitch);
        Switch soundSwitch = findViewById(R.id.soundSwitch);

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        appThemeButton.setOnClickListener(view -> showAppThemeDialog());
        keyboardThemeButton.setOnClickListener(view -> showKeyboardThemeDialog());
        keyHeightButton.setOnClickListener(view -> showKeyHeightDialog());

        numberRowSwitch.setChecked(AppPreferences.isNumberRowEnabled(this));
        symbolHintsSwitch.setChecked(AppPreferences.areSymbolHintsEnabled(this));
        suggestionBarSwitch.setChecked(AppPreferences.isSuggestionBarEnabled(this));
        hapticSwitch.setChecked(AppPreferences.isHapticFeedbackEnabled(this));
        soundSwitch.setChecked(AppPreferences.isSoundFeedbackEnabled(this));

        numberRowSwitch.setOnCheckedChangeListener((button, checked) ->
                AppPreferences.setNumberRowEnabled(this, checked));
        symbolHintsSwitch.setOnCheckedChangeListener((button, checked) ->
                AppPreferences.setSymbolHintsEnabled(this, checked));
        suggestionBarSwitch.setOnCheckedChangeListener((button, checked) ->
                AppPreferences.setSuggestionBarEnabled(this, checked));
        hapticSwitch.setOnCheckedChangeListener((button, checked) ->
                AppPreferences.setHapticFeedbackEnabled(this, checked));
        soundSwitch.setOnCheckedChangeListener((button, checked) ->
                AppPreferences.setSoundFeedbackEnabled(this, checked));

        refreshLabels();
    }

    private void showAppThemeDialog() {
        String[] labels = {
                getString(R.string.theme_system),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };
        String[] values = {
                AppPreferences.THEME_SYSTEM,
                AppPreferences.THEME_LIGHT,
                AppPreferences.THEME_DARK
        };
        showChoiceDialog(
                R.string.theme,
                labels,
                values,
                AppPreferences.getAppTheme(this),
                value -> {
                    AppPreferences.setAppTheme(this, value);
                    recreate();
                }
        );
    }

    private void showKeyboardThemeDialog() {
        String[] labels = {
                getString(R.string.theme_follow_app),
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_oled)
        };
        String[] values = {
                AppPreferences.KEYBOARD_THEME_FOLLOW_APP,
                AppPreferences.KEYBOARD_THEME_LIGHT,
                AppPreferences.KEYBOARD_THEME_DARK,
                AppPreferences.KEYBOARD_THEME_OLED
        };
        showChoiceDialog(
                R.string.keyboard_theme,
                labels,
                values,
                AppPreferences.getKeyboardTheme(this),
                value -> {
                    AppPreferences.setKeyboardTheme(this, value);
                    refreshLabels();
                }
        );
    }

    private void showKeyHeightDialog() {
        String[] labels = {
                getString(R.string.keyboard_height_compact),
                getString(R.string.keyboard_height_standard),
                getString(R.string.keyboard_height_tall)
        };
        String[] values = {"44", "50", "56"};
        showChoiceDialog(
                R.string.keyboard_height,
                labels,
                values,
                String.valueOf(AppPreferences.getKeyHeight(this)),
                value -> {
                    AppPreferences.setKeyHeight(this, Integer.parseInt(value));
                    refreshLabels();
                }
        );
    }

    private void showChoiceDialog(
            int title,
            String[] labels,
            String[] values,
            String current,
            ChoiceHandler handler
    ) {
        int selected = 0;
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(current)) {
                selected = index;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    handler.onChoice(values[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void refreshLabels() {
        appThemeButton.setText(
                getString(
                        R.string.setting_value,
                        getString(R.string.theme),
                        AppPreferences.appThemeLabel(this)
                )
        );
        keyboardThemeButton.setText(
                getString(
                        R.string.setting_value,
                        getString(R.string.keyboard_theme),
                        AppPreferences.keyboardThemeLabel(this)
                )
        );
        keyHeightButton.setText(
                getString(
                        R.string.setting_value,
                        getString(R.string.keyboard_height),
                        AppPreferences.keyHeightLabel(this)
                )
        );
    }

    private interface ChoiceHandler {
        void onChoice(String value);
    }
}
