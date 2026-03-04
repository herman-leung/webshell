package com.example.webshell;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;
import android.webkit.ConsoleMessage;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    // 权限请求码（统一用一个，避免冲突）
    private static final int REQUEST_CODE = 1001;

    private static final String DEFAULT_URL = "https://znh5.smshj.com";
//    private static final String DEFAULT_URL = "http://192.168.1.46:5500/index.html";
//    private static final String DEFAULT_URL = "https://192.168.1.46:8080/";
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
        // 允许 JS 访问文件
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        // 允许混合内容
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // ========== 关键修改1：注入JS桥接类（保留你的原有注入逻辑，补充实现） ==========
        webView.addJavascriptInterface(new JsBridge(this), "AndroidJsBridge");

        // 清除历史缓存
        webView.clearCache(true);
        webView.clearHistory();

        webView.setWebViewClient(new WebViewClient(){
            // Android 5.0+ 忽略证书校验
            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                // 跳过所有SSL证书错误（包括过期、不被信任等）
                handler.proceed(); // 关键：允许加载，不调用handler.cancel()
                // 注意：不要删除这行日志，方便排查问题
                android.util.Log.e("SSL_ERROR", "证书错误类型：" + error.getPrimaryError());
            }

            // 保留你原有WebViewClient的其他逻辑（如果有）
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

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
     * 系统返回手势处理（保留你的原有逻辑）
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

    /**
     * ========== 关键修改2：完善权限检查（补充文件读写权限） ==========
     */
    private void checkPermission() {
        // 定义需要申请的权限数组
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE // 文件写入权限
            };
        } else { // Android 6-12
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE // 文件写入权限
            };
        }

        // 检查是否有未授权的权限
        boolean needRequest = false;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        // 申请权限
        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);
        }
    }

    /**
     * ========== 关键修改3：实现JsBridge类（文件保存核心逻辑） ==========
     */
    public class JsBridge {
        private MainActivity mActivity;

        // 构造方法，传入Activity上下文
        public JsBridge(MainActivity activity) {
            this.mActivity = activity;
        }

        // 供H5调用的保存文件方法（必须加@JavascriptInterface注解）
        @JavascriptInterface
        public void saveFile(String fileUrl, String fileName) {
            // 子线程执行文件下载（避免阻塞主线程）
            new Thread(() -> {
                try {
                    // 1. 从URL下载文件字节流
                    byte[] fileData = downloadFile(fileUrl);
                    if (fileData == null) {
                        showToast("文件下载失败，请检查URL");
                        return;
                    }

                    // 2. 保存文件到本地（手机下载目录）
                    boolean success = saveFileToLocal(fileData, fileName);
                    if (success) {
                        showToast("文件保存成功：/下载/" + fileName);
                        // 回调H5告知成功（可选）
                        callJsFunction("onFileSaveSuccess('" + fileName + "')");
                    } else {
                        showToast("文件保存失败");
                        // 回调H5告知失败（可选）
                        callJsFunction("onFileSaveError('保存失败')");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("保存异常：" + e.getMessage());
                    callJsFunction("onFileSaveError('" + e.getMessage() + "')");
                }
            }).start();
        }

        // 下载文件字节流
        private byte[] downloadFile(String fileUrl) {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000); // 10秒连接超时
                conn.setReadTimeout(10000);    // 10秒读取超时
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, len);
                    }
                    // 关闭流
                    bos.close();
                    is.close();
                    conn.disconnect();
                    return bos.toByteArray();
                } else {
                    showToast("文件链接无法访问：" + conn.getResponseCode());
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // 保存字节流到本地文件（下载目录）
        private boolean saveFileToLocal(byte[] data, String fileName) {
            try {
                // 获取安卓公共下载目录
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs(); // 创建目录
                }

                // 创建文件
                File file = new File(downloadDir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
                fos.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // 显示Toast（必须在主线程执行）
        private void showToast(String msg) {
            mActivity.runOnUiThread(() -> Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show());
        }

        // 调用H5的JS函数（回调结果）
        private void callJsFunction(String jsCode) {
            mActivity.runOnUiThread(() -> webView.evaluateJavascript("javascript:" + jsCode, null));
        }
    }
}