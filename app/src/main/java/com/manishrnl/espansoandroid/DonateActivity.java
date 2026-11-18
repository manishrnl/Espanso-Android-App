package com.manishrnl.espansoandroid;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/**
 * Screen displaying the developer's UPI QR code and payment actions
 * to support independent development of RNL ESPANSO.
 */
public final class DonateActivity extends Activity {

    private Bitmap mCurrentQrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        AppPreferences.applySystemBarAppearance(this);
        setContentView(R.layout.activity_donate);

        findViewById(R.id.donateBackButton).setOnClickListener(v -> finish());

        String upiId = getString(R.string.donate_upi_id);
        String upiName = getString(R.string.donate_upi_name);
        String phone = getString(R.string.donate_developer_phone);

        // Standard UPI URI without keyword-trigger notes to avoid anti-fraud false alarms
        Uri upiUri = new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", upiName)
                .appendQueryParameter("cu", "INR")
                .build();

        TextView upiText = findViewById(R.id.donateUpiIdText);
        upiText.setText(upiId);

        TextView phoneText = findViewById(R.id.donatePhoneText);
        phoneText.setText(phone);

        ImageView qrImageView = findViewById(R.id.donateQrImage);
        qrImageView.setImageResource(R.drawable.navi_qr);
        mCurrentQrBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.navi_qr);

        // Save QR to Gallery button
        Button saveQrButton = findViewById(R.id.donateSaveQrButton);
        saveQrButton.setOnClickListener(v -> saveQrToGallery());

        // Copy UPI ID button
        Button copyButton = findViewById(R.id.donateCopyButton);
        copyButton.setOnClickListener(v -> {
            copyToClipboard("UPI ID", upiId);
            Toast.makeText(this, R.string.donate_copied_toast, Toast.LENGTH_SHORT).show();
        });

        // Copy Mobile Number button
        Button copyPhoneButton = findViewById(R.id.donateCopyPhoneButton);
        copyPhoneButton.setOnClickListener(v -> {
            copyToClipboard("Mobile Number", phone);
            Toast.makeText(this, R.string.donate_phone_copied_toast, Toast.LENGTH_SHORT).show();
        });

        // Generic Pay via Any UPI App button
        Button payButton = findViewById(R.id.donatePayButton);
        payButton.setOnClickListener(v -> launchGenericUpiChooser(upiUri));

        // Direct App Buttons
        findViewById(R.id.donatePhonePeButton).setOnClickListener(v ->
                launchTargetedApp("com.phonepe.app", getString(R.string.donate_app_phonepe), upiUri));

        findViewById(R.id.donateGPayButton).setOnClickListener(v ->
                launchTargetedApp("com.google.android.apps.nbu.paisa.user", getString(R.string.donate_app_gpay), upiUri));

        findViewById(R.id.donatePaytmButton).setOnClickListener(v ->
                launchTargetedApp("net.one97.paytm", getString(R.string.donate_app_paytm), upiUri));

        findViewById(R.id.donateNaviButton).setOnClickListener(v ->
                launchTargetedApp("com.navi.mutualfunds", getString(R.string.donate_app_navi), upiUri));
    }

    private void launchGenericUpiChooser(Uri uri) {
        Intent payIntent = new Intent(Intent.ACTION_VIEW, uri);
        Intent chooser = Intent.createChooser(payIntent, getString(R.string.donate_pay_via_upi_button));
        try {
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, R.string.donate_no_upi_app, Toast.LENGTH_LONG).show();
        }
    }

    private void launchTargetedApp(String packageName, String appName, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(packageName);
        try {
            startActivity(intent);
        } catch (Exception e) {
            // App not found, fall back to generic chooser
            String msg = getString(R.string.donate_app_not_installed, appName);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            launchGenericUpiChooser(uri);
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    private void saveQrToGallery() {
        if (mCurrentQrBitmap == null) {
            Toast.makeText(this, "QR Code not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String title = "RNL_Espanso_UPI_QR_" + (System.currentTimeMillis() / 1000);
            String description = "RNL Espanso UPI Donation QR Code";
            String savedUrl = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    mCurrentQrBitmap,
                    title,
                    description
            );
            if (savedUrl != null) {
                Toast.makeText(this, R.string.donate_qr_saved_toast, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Could not save to gallery. Please take a screenshot.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not save to gallery. Please take a screenshot.", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap generateQrCode(String data, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix =
                    writer.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
