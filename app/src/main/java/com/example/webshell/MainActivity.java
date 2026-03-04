package com.example.webshell;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.Intent;
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final int REQUEST_CODE = 1001;

//    private static final String DEFAULT_URL = "https://twm-h5.smshj.com";
    private static final String DEFAULT_URL = "https://znh5.smshj.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        initWebView();
        checkPermission();
        initBackHandler();

//        webView.loadUrl("https://twm-h5.smshj.com/");
//        webView.loadUrl("https://znh5.smshj.com");
//        webView.loadUrl("https://baidu.com");

        // 处理intent参数（核心）
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        if(intent != null && intent.hasExtra("jump_url")) {
            String targetUrl = intent.getStringExtra("jump_url");
            webView.loadUrl(targetUrl);
        } else {
            webView.loadUrl(DEFAULT_URL);
        }
    }

    private void initWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // 清除历史缓存（建议在启动时执行一次）
        webView.clearCache(true);
        webView.clearHistory();

        // 推荐开启
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                            return;
                        }
                    }
                });
            }
        });
    }

    /**
     * 系统返回手势处理
     */
    private void initBackHandler() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            finish();
                        }
                    }
                });
    }

    private void checkPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (Build.VERSION.SDK_INT >= 33) {

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.READ_MEDIA_IMAGES
                            },
                            REQUEST_CODE);
                }

            } else {

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            },
                            REQUEST_CODE);
                }
            }
        }
    }
}