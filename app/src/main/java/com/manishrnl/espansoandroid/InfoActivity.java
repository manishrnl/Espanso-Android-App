package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public final class InfoActivity extends Activity {
    public static final String EXTRA_PAGE = "page";
    public static final String PAGE_PRIVACY = "privacy";
    public static final String PAGE_DEVELOPER = "developer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_info);

        findViewById(R.id.infoBackButton).setOnClickListener(view -> finish());
        String page = getIntent().getStringExtra(EXTRA_PAGE);
        if (PAGE_DEVELOPER.equals(page)) {
            showDeveloperPage();
        } else {
            showPrivacyPage();
        }
    }

    private void showPrivacyPage() {
        findViewById(R.id.infoHeroLogo).setVisibility(View.GONE);
        findViewById(R.id.infoHeroIcon).setVisibility(View.VISIBLE);

        setText(R.id.infoEyebrow, R.string.privacy_eyebrow);
        setText(R.id.infoTitle, R.string.privacy_policy);
        setText(R.id.infoSubtitle, R.string.privacy_subtitle_extended);

        findViewById(R.id.infoDeveloperContainer).setVisibility(View.GONE);
        findViewById(R.id.infoPrivacyContainer).setVisibility(View.VISIBLE);

        bindLink(R.id.infoPrivacyPolicyWeb, R.string.privacy_policy_web_url);
        bindLink(R.id.infoPrivacyWebsite, R.string.developer_website_url);
        bindLink(R.id.infoPrivacyContact, R.string.developer_contact_url);
        bindLink(R.id.infoPrivacyGithub, R.string.github_url);
        findViewById(R.id.infoPrivacyDeveloper).setOnClickListener(view -> showDeveloperPage());

        scrollToTop();
    }

    private void showDeveloperPage() {
        findViewById(R.id.infoHeroLogo).setVisibility(View.VISIBLE);
        findViewById(R.id.infoHeroIcon).setVisibility(View.GONE);

        setText(R.id.infoEyebrow, R.string.developer_eyebrow);
        setText(R.id.infoTitle, R.string.developer_name);
        setText(R.id.infoSubtitle, R.string.company_name);
        setText(R.id.infoContent, R.string.developer_profile_content);

        findViewById(R.id.infoPrivacyContainer).setVisibility(View.GONE);
        findViewById(R.id.infoDeveloperContainer).setVisibility(View.VISIBLE);
        findViewById(R.id.infoDeveloperLinks).setVisibility(View.VISIBLE);

        bindLink(R.id.infoWebsite, R.string.developer_website_url);
        bindLink(R.id.infoInstagram, R.string.instagram_url);
        bindLink(R.id.infoFacebook, R.string.facebook_url);
        bindLink(R.id.infoGithub, R.string.github_url);
        findViewById(R.id.infoPhone).setOnClickListener(view ->
                startActivity(new Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse(getString(R.string.developer_phone_uri))
                )));
        findViewById(R.id.infoDonate).setOnClickListener(view ->
                startActivity(new Intent(this, DonateActivity.class)));

        scrollToTop();
    }

    private void scrollToTop() {
        ScrollView scrollView = findViewById(R.id.infoScrollView);
        if (scrollView != null) {
            scrollView.post(() -> scrollView.scrollTo(0, 0));
        }
    }

    private void bindLink(int viewId, int urlResource) {
        findViewById(viewId).setOnClickListener(view ->
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(urlResource))
                )));
    }

    private void setText(int viewId, int textResource) {
        TextView view = findViewById(viewId);
        view.setText(textResource);
    }
}
