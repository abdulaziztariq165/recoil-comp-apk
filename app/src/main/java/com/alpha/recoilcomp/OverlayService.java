package com.alpha.recoilcomp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import rikka.shizuku.Shizuku;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundServiceSafely();
        createOverlay();
    }

    private void startForegroundServiceSafely() {
        String CHANNEL_ID = "overlay_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Recoil Comp Shizuku")
                .setContentText("Touch Injection Active")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
        try {
            startForeground(1, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        // إعدادات عشان اللمس يعدي للعبة
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        WebView overlayWebView = new WebView(this);
        overlayWebView.setBackgroundColor(0x00000000);
        overlayWebView.getSettings().setJavaScriptEnabled(true);
        overlayWebView.getSettings().setDomStorageEnabled(true);
        overlayWebView.setWebViewClient(new WebViewClient());
        
        overlayWebView.addJavascriptInterface(new ShizukuOverlayBridge(), "NativeOverlay");
        overlayWebView.loadUrl("file:///android_asset/index.html");

        overlayView = overlayWebView;
        
        try {
            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class ShizukuOverlayBridge {
        @JavascriptInterface
        public void injectTap(final int x, final int y) {
            new Thread(() -> {
                try {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        java.lang.reflect.Method method = Shizuku.class.getDeclaredMethod(
                                "newProcess", String[].class, String[].class, String.class);
                        method.setAccessible(true);
                        String[] cmd = new String[]{"sh", "-c", "input tap " + x + " " + y};
                        Process process = (Process) method.invoke(null, cmd, null, null);
                        if (process != null) process.waitFor();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }

        @JavascriptInterface
        public void injectSwipe(final int x1, final int y1, final int x2, final int y2, final int duration) {
            new Thread(() -> {
                try {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        java.lang.reflect.Method method = Shizuku.class.getDeclaredMethod(
                                "newProcess", String[].class, String[].class, String.class);
                        method.setAccessible(true);
                        String[] cmd = new String[]{"sh", "-c", "input swipe " + x1 + " " + y1 + " " + x2 + " " + y2 + " " + duration};
                        Process process = (Process) method.invoke(null, cmd, null, null);
                        if (process != null) process.waitFor();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && windowManager != null) {
            try { windowManager.removeView(overlayView); } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
