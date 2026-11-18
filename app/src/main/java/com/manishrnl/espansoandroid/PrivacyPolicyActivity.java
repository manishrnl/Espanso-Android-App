package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Activity entry point for Privacy Policy.
 * Seamlessly forwards to the comprehensive, DPDP Act 2023-compliant in-app
 * privacy policy in InfoActivity.
 */
public class PrivacyPolicyActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, InfoActivity.class);
        intent.putExtra(InfoActivity.EXTRA_PAGE, InfoActivity.PAGE_PRIVACY);
        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        finish();
    }
}

