package com.example.blackscreen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

public class OverlayService extends Service {
    private static final String CHANNEL_ID = "black_screen";
    private static final int NOTIFICATION_ID = 7001;
    private static final long DOUBLE_TAP_TIMEOUT_MS = 450L;

    private WindowManager windowManager;
    private TextView floatingButton;
    private FrameLayout blackOverlay;
    private WindowManager.LayoutParams buttonParams;
    private boolean blackMode;
    private long firstTapAt;
    private float downX;
    private float downY;
    private int startX;
    private int startY;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createNotificationChannel();
        startForegroundCompat();
        showFloatingButton();
    }

    private void startForegroundCompat() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                1,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Keeps the floating black-screen control available");
        manager.createNotificationChannel(channel);
    }

    private void showFloatingButton() {
        if (floatingButton != null || blackMode) return;

        floatingButton = new TextView(this);
        floatingButton.setText("ON");
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setTextSize(13f);
        floatingButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        floatingButton.setGravity(Gravity.CENTER);
        floatingButton.setBackgroundResource(R.drawable.button_off);
        floatingButton.setElevation(dp(6));

        buttonParams = new WindowManager.LayoutParams(
                dp(58),
                dp(58),
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = dp(18);
        buttonParams.y = dp(180);

        floatingButton.setOnTouchListener((v, event) -> handleButtonTouch(event));
        windowManager.addView(floatingButton, buttonParams);
    }

    private boolean handleButtonTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX();
                downY = event.getRawY();
                startX = buttonParams.x;
                startY = buttonParams.y;
                return true;
            case MotionEvent.ACTION_MOVE:
                int dx = (int) (event.getRawX() - downX);
                int dy = (int) (event.getRawY() - downY);
                buttonParams.x = Math.max(0, startX + dx);
                buttonParams.y = Math.max(0, startY + dy);
                windowManager.updateViewLayout(floatingButton, buttonParams);
                return true;
            case MotionEvent.ACTION_UP:
                int moveDistance = (int) (Math.abs(event.getRawX() - downX) + Math.abs(event.getRawY() - downY));
                if (moveDistance < dp(12)) enterBlackMode();
                return true;
            default:
                return false;
        }
    }

    private void enterBlackMode() {
        if (blackOverlay != null) return;

        blackOverlay = new FrameLayout(this);
        blackOverlay.setBackgroundColor(Color.BLACK);
        blackOverlay.setClickable(true);
        blackOverlay.setOnTouchListener((v, event) -> handleBlackScreenTouch(event));

        WindowManager.LayoutParams blackParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE
        );
        blackParams.gravity = Gravity.TOP | Gravity.START;
        blackParams.screenBrightness = 0.0f;
        if (Build.VERSION.SDK_INT >= 28) {
            blackParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }

        windowManager.addView(blackOverlay, blackParams);
        removeFloatingButton();
        blackMode = true;
    }

    private boolean handleBlackScreenTouch(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;

        long now = System.currentTimeMillis();
        if (now - firstTapAt <= DOUBLE_TAP_TIMEOUT_MS) {
            exitBlackMode();
        } else {
            firstTapAt = now;
        }
        return true;
    }

    private void exitBlackMode() {
        if (blackOverlay != null) {
            try {
                windowManager.removeView(blackOverlay);
            } catch (Exception ignored) {
            }
            blackOverlay = null;
        }
        firstTapAt = 0L;
        blackMode = false;
        showFloatingButton();
    }

    private void removeFloatingButton() {
        if (floatingButton == null) return;
        try {
            windowManager.removeView(floatingButton);
        } catch (Exception ignored) {
        }
        floatingButton = null;
    }

    private int overlayWindowType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        removeFloatingButton();
        if (blackOverlay != null) {
            try {
                windowManager.removeView(blackOverlay);
            } catch (Exception ignored) {
            }
            blackOverlay = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
