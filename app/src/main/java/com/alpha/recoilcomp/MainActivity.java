package com.alpha.recoilcomp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;
    private static final int SHIZUKU_PERMISSION_REQ_CODE = 1235;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // طلب صلاحية Shizuku عند بدء التشغيل
        requestShizukuPermission();

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getInsetsController().hide(
                android.view.WindowInsets.Type.statusBars() |
                android.view.WindowInsets.Type.navigationBars()
            );
        }
    }

    private void requestShizukuPermission() {
        if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return;
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, "Please grant Shizuku permission for touch injection.", Toast.LENGTH_LONG).show();
        }
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQ_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SHIZUKU_PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Shizuku Permission Denied. Touch injection will fail.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() { }

    public class WebAppInterface {
        @JavascriptInterface
        public void startOverlay() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            } else {
                launchOverlayService();
            }
        }

        @JavascriptInterface
        public void stopOverlay() {
            Intent intent = new Intent(MainActivity.this, OverlayService.class);
            stopService(intent);
            Toast.makeText(MainActivity.this, "Overlay Stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                launchOverlayService();
            } else {
                Toast.makeText(this, "Overlay Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Overlay Started with Shizuku Injection!", Toast.LENGTH_SHORT).show();
        moveTaskToBack(true); 
    }
}
