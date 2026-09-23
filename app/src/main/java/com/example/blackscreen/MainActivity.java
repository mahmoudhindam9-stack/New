package com.example.blackscreen;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildSimpleUi();

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        }
    }

    private void buildSimpleUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(28), dp(56), dp(28), dp(28));
        root.setBackgroundColor(0xFF0E0E0E);

        TextView title = new TextView(this);
        title.setText("Black Screen Overlay");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("زر عائم يخفي الشاشة باللون الأسود مع استمرار تشغيل الفيديو والصوت تحتها.\n\nبعد التفعيل: اضغط مرتين بسرعة على الشاشة السوداء للعودة.");
        info.setTextColor(0xFFB8B8B8);
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.topMargin = dp(28);
        root.addView(info, infoLp);

        Button permission = new Button(this);
        permission.setText("السماح بالظهور فوق التطبيقات");
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(-1, -2);
        pLp.topMargin = dp(32);
        root.addView(permission, pLp);
        permission.setOnClickListener(v -> openOverlaySettings());

        Button start = new Button(this);
        start.setText("تشغيل الزر العائم");
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-1, -2);
        sLp.topMargin = dp(16);
        root.addView(start, sLp);
        start.setOnClickListener(v -> startOverlay());

        setContentView(root);
    }

    private void openOverlaySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "فعّل السماح بالظهور فوق التطبيقات أولًا", Toast.LENGTH_LONG).show();
            openOverlaySettings();
            return;
        }

        Intent serviceIntent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "الزر العائم ظهر فوق التطبيقات", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
