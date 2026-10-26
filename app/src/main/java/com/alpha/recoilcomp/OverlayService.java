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
        startForegroundService();
        createOverlay();
    }

    private void startForegroundService() {
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
        startForeground(1, notification);
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, // عشان الاحداثيات تظبط
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        WebView overlayWebView = new WebView(this);
        overlayWebView.setBackgroundColor(0x00000000);
        overlayWebView.getSettings().setJavaScriptEnabled(true);
        overlayWebView.getSettings().setDomStorageEnabled(true);
        overlayWebView.setWebViewClient(new WebViewClient());
        
        // ربط جسر الشوزوكو بالـ HTML
        overlayWebView.addJavascriptInterface(new ShizukuOverlayBridge(), "NativeOverlay");
        
        overlayWebView.loadUrl("file:///android_asset/index.html");

        overlayView = overlayWebView;
        windowManager.addView(overlayView, params);
    }

    // ده الكلاس اللي هيستقبل أوامر الضغط من الـ JavaScript
    public class ShizukuOverlayBridge {
        @JavascriptInterface
        public void injectTap(final int x, final int y) {
            // ننفذ في ثريد منفصل عشان منعملش بلوك للواجهة
            new Thread(() -> {
                try {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        // أمر الشل اللي بيحقن اللمسة في الشاشة
                        Process process = Shizuku.newProcess(
                            new String[]{"sh", "-c", "input tap " + x + " " + y}, 
                            null, null
                        );
                        process.waitFor();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
            windowManager.removeView(overlayView);
        }
    }
}
