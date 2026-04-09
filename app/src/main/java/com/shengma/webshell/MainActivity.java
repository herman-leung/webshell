package com.shengma.webshell;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.database.Cursor;

import retrofit2.Retrofit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import android.app.AlertDialog;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;

import android.provider.Settings;
import androidx.annotation.NonNull;
import java.io.OutputStream;
import android.content.ContentValues;
// import okhttp3.Callback;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    // 权限请求码（统一用一个，避免冲突）
    private static final int REQUEST_CODE = 1001;

    //    private static final String DEFAULT_URL = "https://znh5.smshj.com";
//    private static final String DEFAULT_URL = "http://192.168.1.46:5500/index.html";
    // private static final String DEFAULT_URL = "https://192.168.1.46:80/";
   private static String DEFAULT_URL = "https://twm-h5.smshj.com/";

    private static final String BASE_APK_URL = "https://kmgapi.test.smshj.com/";
    private static final String TAG = "GraphqlDemo";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzczMTA3NzY5fQ.7MF69Q7XJCSrE5qL-2wms3yw6D17r5sTHo7SYGVJMao";

    // TODO: apk文件名
    private static final String APK_FILE_NAME = "新平台外贸apk"; // 从接口获取的文件名（全局变量）
    private static final String APK_FILE_NAME_OLD = "老平台外贸apk"; // 从接口获取的文件名（全局变量）

    // ========== 新增：FileProvider 授权的包名后缀（需和xml配置一致） ==========
    // 使用 getPackageName() 动态获取包名，支持多 flavor
    private String getFileProviderAuthority() {
        return getPackageName() + ".fileprovider";
    }

    // 权限请求码
    private static final int REQUEST_PERMISSION_CODE = 1001;
    // 安装权限请求码（Android 8.0+）
    private static final int REQUEST_INSTALL_PERMISSION_CODE = 1002;
    
    private String OSS_BASE_URL = ""; // 从接口获取的OSS地址（全局变量）
    // APK 保存路径（应用内部存储，无需额外权限）
    private File apkFile;

    // 添加相册选择器
    private ActivityResultLauncher<Intent> galleryLauncher;
    // 添加拍照选择器
    private ActivityResultLauncher<Intent> cameraLauncher;
    // 添加扫码选择器
    private ActivityResultLauncher<Intent> scanLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int len = getPackageName().split("\\.").length;
        String packageName = getPackageName().split("\\.")[len - 1]; // 获取包名并转换为大写
        if(packageName.equals("webshell")) { // 新平台
            // 新平台
            DEFAULT_URL = "https://twm-h5.smshj.com/";
        } else {
            // 老平台
            DEFAULT_URL = "https://znh5.smshj.com";
        }


        webView = new WebView(this);
        setContentView(webView);

        initWebView();
        // checkPermission();
        initBackHandler();
        // 初始化相册选择器
        initGalleryLauncher();
        // 初始化拍照选择器
        initCameraLauncher();
        // 初始化扫码选择器
        initScanLauncher();
        // 处理intent参数（核心）
        handleIntent(getIntent());

        try{
            if(packageName.equals("webshell")) { // 新平台
                getAPKFile(APK_FILE_NAME);
            } else {
                getAPKFile(APK_FILE_NAME_OLD);
            }

        } catch (Exception e) {
            Log.e(TAG, "获取APK文件失败", e);
        }
    }

    // 添加相册选择器初始化方法
    private void initGalleryLauncher() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.i("MainActivity", "相册返回 - ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Log.i("MainActivity", "Intent data: " + (data != null ? "有数据" : "无数据"));
                        if (data != null) {
                            Uri uri = data.getData();
                            Log.i("MainActivity", "选中的图片URI: " + uri);
                            String imagePath = getPathFromUri(uri);
                            Log.i("MainActivity", "转换后的路径: " + imagePath);
                            if (imagePath != null && !imagePath.isEmpty()) {
                                // 获取图片Base64
                                String base64 = getImageBase64(imagePath);
                                if (base64 != null && !base64.isEmpty()) {
                                    // 获取图片类型
                                    String imageType = imagePath.toLowerCase().endsWith(".jpg") || imagePath.toLowerCase().endsWith(".jpeg") ? "jpeg" : "png";
                                    // 直接传Base64给前端
                                    String jsCode = "javascript:onGalleryImageSelected('data:image/" + imageType + ";base64," + base64 + "')";
                                    Log.i("MainActivity", "执行JS回调，Base64长度: " + base64.length());
                                    webView.evaluateJavascript(jsCode, null);
                                    Toast.makeText(MainActivity.this, getString(R.string.toast_image_loaded), Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e("MainActivity", "Base64转换失败");
                                    Toast.makeText(MainActivity.this, getString(R.string.toast_base64_failed), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e("MainActivity", "图片路径为空");
                                Toast.makeText(MainActivity.this, getString(R.string.toast_image_path_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.i("MainActivity", "用户取消选择相册");
                    }
                });
    }

    // 添加拍照选择器初始化方法
    private void initCameraLauncher() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.i("MainActivity", "拍照返回 - ResultCode: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        // 获取拍照的图片路径
                        String imagePath = getCameraImagePath();
                        if (imagePath != null && !imagePath.isEmpty()) {
                            // 获取图片Base64
                            String base64 = getImageBase64(imagePath);
                            if (base64 != null && !base64.isEmpty()) {
                                // 获取图片类型
                                String imageType = imagePath.toLowerCase().endsWith(".jpg") || imagePath.toLowerCase().endsWith(".jpeg") ? "jpeg" : "png";
                                // 直接传Base64给前端
                                String jsCode = "javascript:onCameraImageCaptured('data:image/" + imageType + ";base64," + base64 + "')";
                                Log.i("MainActivity", "执行JS回调，Base64长度: " + base64.length());
                                webView.evaluateJavascript(jsCode, null);
                                Toast.makeText(MainActivity.this, getString(R.string.toast_camera_success), Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("MainActivity", "Base64转换失败");
                                Toast.makeText(MainActivity.this, getString(R.string.toast_base64_failed), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("MainActivity", "拍照图片路径为空");
                            Toast.makeText(MainActivity.this, getString(R.string.toast_camera_image_failed), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.i("MainActivity", "用户取消拍照");
                    }
                });
    }

    // 添加扫码选择器初始化方法
    private void initScanLauncher() {
        scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.i("MainActivity", "扫码返回 - ResultCode: " + result.getResultCode());
                    try {
                        Intent data = result.getData();
                        if (data != null && result.getResultCode() == RESULT_OK) {
                            String scanContent = data.getStringExtra("SCAN_RESULT");
                            if (scanContent != null && !scanContent.isEmpty()) {
                                Log.i("MainActivity", "扫码结果: " + scanContent);
                                // 传结果给前端
                                String jsCode = "javascript:onScanResult('" + scanContent + "')";
                                webView.evaluateJavascript(jsCode, null);
                                // TODO: 隐藏提示？
                                // Toast.makeText(MainActivity.this, "扫码成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.i("MainActivity", "扫码内容为空");
                                Toast.makeText(MainActivity.this, getString(R.string.toast_scan_failed), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.i("MainActivity", "用户取消扫码或无数据");
                            Toast.makeText(MainActivity.this, getString(R.string.toast_scan_cancel), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "处理扫码结果异常", e);
                        Toast.makeText(MainActivity.this, getString(R.string.toast_scan_process_failed), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 添加Uri转文件路径方法
    private String getPathFromUri(Uri uri) {
        String path = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            Log.i("MainActivity", "开始查询URI: " + uri);
            cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                Log.i("MainActivity", "Cursor行数: " + cursor.getCount());
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(projection[0]);
                    path = cursor.getString(columnIndex);
                    Log.i("MainActivity", "获取到路径: " + path);
                } else {
                    Log.w("MainActivity", "Cursor为空，未获取到数据");
                }
            } else {
                Log.e("MainActivity", "Cursor查询结果为null");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "getPathFromUri异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return path;
    }

    // 打开相册选择照片
    public void openGallery() {
        Log.i("MainActivity", "打开相册选择照片");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // 打开相机拍照
    public void openCamera() {
        Log.i("MainActivity", "打开相机拍照");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 创建临时文件来保存拍照图片
        File photoFile = createImageFile();
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this, getFileProviderAuthority(), photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, getString(R.string.toast_create_image_failed), Toast.LENGTH_SHORT).show();
        }
    }

    // 创建图片文件
    private File createImageFile() {
        try {
            // 创建文件名
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            // 保存文件路径
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            Log.e("MainActivity", "创建图片文件失败", e);
            return null;
        }
    }

    // 当前拍照图片路径
    private String currentPhotoPath;
    
    // 待处理的Base64文件数据和文件名
    public String pendingSaveBase64Data;
    public String pendingSaveFileName;
    
    // 待处理的操作类型
    public String pendingOperation; // 可能的值：scanCode, saveBase64File, takePhoto, selectPhoto

    // 获取拍照图片路径
    private String getCameraImagePath() {
        return currentPhotoPath;
    }

    // 打开扫码器
    public void openScanner() {
        Log.i("MainActivity", "打开扫码器");
        try {
            // 使用ML Kit扫码（支持变焦）
            Intent scanIntent = new Intent(this, ScanActivity.class);
            scanLauncher.launch(scanIntent);
        } catch (Exception e) {
            Log.e("MainActivity", "打开扫码器失败", e);
            Toast.makeText(this, getString(R.string.toast_scan_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 获取图片Base64
    private String getImageBase64(String imagePath) {
        Log.i("MainActivity", "getImageBase64: " + imagePath);
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                Log.e("MainActivity", "文件不存在: " + imagePath);
                return null;
            }
            
            // 读取文件转Base64
            byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
            String base64 = android.util.Base64.encodeToString(fileContent, android.util.Base64.NO_WRAP);
            Log.i("MainActivity", "Base64生成成功，长度: " + base64.length());
            return base64;
        } catch (Exception e) {
            Log.e("MainActivity", "getImageBase64异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
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

// 【deprecated】检查是否有相机权限
                    // // 检查是否有相机权限
                    // if (ContextCompat.checkSelfPermission(MainActivity.this, 
                    //         Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    //     // 有相机权限，授予 WebView 权限
                    //     for (String resource : request.getResources()) {
                    //         if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                    //             request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    //             return;
                    //         }
                    //     }
                    // } else {
                    //     // 没有相机权限，检查是否已经拒绝过权限
                    //     if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, 
                    //             Manifest.permission.CAMERA)) {
                    //         // 用户已经拒绝过权限并勾选了"不再询问"，弹出提示引导用户去设置页面
                    //         new AlertDialog.Builder(MainActivity.this)
                    //                 .setTitle("摄像头权限")
                    //                 .setMessage("需要摄像头权限才能使用此功能，请在设置中开启")
                    //                 .setPositiveButton("去设置", (dialog, which) -> {
                    //                     // 跳转到应用设置页面
                    //                     Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    //                     intent.setData(Uri.parse("package:" + getPackageName()));
                    //                     startActivityForResult(intent, REQUEST_PERMISSION_CODE);
                    //                 })
                    //                 .setNegativeButton("取消", null)
                    //                 .show();
                    //     } else {
                    //         // 首次请求权限，正常请求
                    //         ActivityCompat.requestPermissions(MainActivity.this, 
                    //                 new String[]{Manifest.permission.CAMERA}, 
                    //                 REQUEST_PERMISSION_CODE);
                    //     }
                    // }
                    // // 拒绝 WebView 权限请求
                    // request.deny();

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
                     Manifest.permission.READ_MEDIA_VIDEO,
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

        // 供H5调用：打开相册选择照片
        @JavascriptInterface
        public void selectPhoto() {
            Log.i("MainActivity", "selectPhoto called");
            
            // 检查读取存储权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 检查读取媒体图片权限
                if (ActivityCompat.checkSelfPermission(mActivity,
                        Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    // 权限不足，请求权限
                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            REQUEST_PERMISSION_CODE);
                    
                    // 保存操作类型，以便权限授予后继续执行
                    mActivity.pendingOperation = "selectPhoto";
                    return;
                }
            } else {
                // Android 12以下检查读取存储权限
                if (ActivityCompat.checkSelfPermission(mActivity,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 权限不足，请求权限
                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_CODE);
                    
                    // 保存操作类型，以便权限授予后继续执行
                    mActivity.pendingOperation = "selectPhoto";
                    return;
                }
            }
            
            mActivity.openGallery();
        }

                                    

        @JavascriptInterface
        public boolean checkCameraPermission() {
            Context context = getApplicationContext();
             return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface
        public void h5ApplyCameraPermission() {
            ActivityCompat.requestPermissions(
                MainActivity.this,
                new String[]{Manifest.permission.CAMERA},
                REQUEST_PERMISSION_CODE
            );
        }

        // 供H5调用：打开相机拍照
        @JavascriptInterface
        public void takePhoto() {
            Log.i("MainActivity", "takePhoto called");
            
            // 检查相机权限
            if (ActivityCompat.checkSelfPermission(mActivity,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // 权限不足，请求权限
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_PERMISSION_CODE);
                
                // 保存操作类型，以便权限授予后继续执行
                mActivity.pendingOperation = "takePhoto";
                return;
            }
            
            mActivity.openCamera();
        }

        // 供H5调用：打开扫码器
        @JavascriptInterface
        public void scanCode() {
            Log.i("MainActivity", "scanCode called");
            
            // 检查相机权限
            if (ActivityCompat.checkSelfPermission(mActivity,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // 权限不足，请求权限
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_PERMISSION_CODE);
                
                // 保存操作类型，以便权限授予后继续执行
                mActivity.pendingOperation = "scanCode";
                return;
            }
            
            mActivity.openScanner();
        }

        // 供H5调用的保存文件方法（必须加@JavascriptInterface注解）
        @SuppressLint("StringFormatInvalid")
        @JavascriptInterface
        public void saveFile(String fileUrl, String fileName) {
            Log.i("MainActivity", "fileUrl" + fileUrl);
            // 子线程执行文件下载（避免阻塞主线程）
            new Thread(() -> {
                try {
                    // 1. 从URL下载文件字节流
                    byte[] fileData = downloadFile(fileUrl);
                    if (fileData == null) {
                        // 文件下载失败，请检查URL
                        showToast(getString(R.string.toast_file_download_failed));
                        return;
                    }

                    // 2. 保存文件到本地（手机下载目录）
                    boolean success = saveFileToLocal(fileData, fileName);
                    if (success) {
                        // 文件保存成功：/下载/ + 文件名
                        showToast(getString(R.string.toast_file_save_success, fileName));
                        // 回调H5告知成功（可选）
                        callJsFunction("onFileSaveSuccess('" + fileName + "')");
                    } else {
                        // 文件保存失败
                        showToast(getString(R.string.toast_file_save_failed));
                        // 回调H5告知失败（可选）
                        callJsFunction("onFileSaveError('" + getString(R.string.toast_file_save_failed) + "')");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 保存异常： + 异常信息
                    showToast(getString(R.string.toast_save_exception, e.getMessage()));
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
                    // 文件链接无法访问：
                    showToast(getString(R.string.toast_file_link_inaccessible) + conn.getResponseCode());
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * 供H5调用：保存Base64编码的文件
         * @param base64Data 纯Base64字符串（需去掉前缀如data:image/png;base64,）
         * @param fileName 保存的文件名（如test.png、document.pdf）
         */
        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName) {
            Log.i("MainActivity", "开始保存Base64文件：" + fileName);
            
            // 检查存储权限（Android 10以下需要）
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(mActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 权限不足，请求权限
                    ActivityCompat.requestPermissions(mActivity,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_CODE);
                    
                    // 保存参数，以便权限授予后继续执行
                    mActivity.pendingSaveBase64Data = base64Data;
                    mActivity.pendingSaveFileName = fileName;
                    mActivity.pendingOperation = "saveBase64File";
                    
                    return;
                }
            }
            
            // 权限已获取，执行保存操作
            performSaveBase64File(base64Data, fileName);
        }
        
        // 执行保存Base64文件的操作
        private void performSaveBase64File(String base64Data, String fileName) {
            // 子线程执行文件保存（避免阻塞主线程）
            new Thread(() -> {
                try {
                    // 1. 校验Base64数据
                    if (base64Data == null || base64Data.isEmpty()) {
                        // base64数据为空
                        showToast(getString(R.string.toast_base64_empty));
                        callJsFunction("onFileSaveError('" + getString(R.string.toast_base64_empty) + "')");
                        return;
                    }

                    // 2. 去除Base64前缀（兼容H5可能传入的带前缀格式）
                    String pureBase64 = base64Data;
                    if (pureBase64.contains(",")) {
                        pureBase64 = pureBase64.split(",")[1];
                    }

                    // 3. Base64解码为字节数组
                    byte[] fileData = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                    if (fileData.length == 0) {
                        // base64数据解码失败
                        showToast(getString(R.string.toast_base64_decode_failed));
                        callJsFunction("onFileSaveError('" + getString(R.string.toast_base64_decode_failed) + "')");
                        return;
                    }

                    // 4. 复用原有保存逻辑写入文件
                    boolean success = saveFileToLocal(fileData, fileName);
                    if (success) {
                        // showToast("Base64文件保存成功：/下载/" + fileName);
                        showToast(getString(R.string.toast_base64_save_success) + fileName);
                        callJsFunction("onFileSaveSuccess('" + fileName + "')");
                    } else {
                        // Base64文件保存失败
                        showToast(getString(R.string.toast_base64_save_failed));
                        callJsFunction("onFileSaveError('" + getString(R.string.toast_base64_save_failed) + "')");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Base64保存异常：
                    showToast(getString(R.string.toast_base64_save_exception, e.getMessage()));
                    callJsFunction("onFileSaveError('" + getString(R.string.toast_base64_save_exception, e.getMessage()) + "')");
                }
            }).start();
        }

        // 保存字节流到本地文件（下载目录）
        private boolean saveFileToLocal(byte[] data, String fileName) {
            try {
                // 获取安卓公共下载目录
                File downloadDir = getDownloadDirectory();
                // 创建文件
                File file = new File(downloadDir, fileName);
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.write(data);
                fos.flush();
                fos.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        // ========== 核心修改：重写openDocument方法 ==========
        @JavascriptInterface
        public void openDocument(String filePath, String fileType) {
            Log.i("MainActivity", "openDocument called with filePath: " + filePath + ", fileType: " + fileType);
            // 1. 校验参数
            if (filePath == null || filePath.isEmpty()) {
                // 文件路径不能为空
                showToast(getString(R.string.toast_file_path_empty));
                callJsFunction("onOpenDocumentError('" + getString(R.string.toast_file_path_empty) + "')");
                return;
            }

            // 2. 检查文件是否存在
            File file = new File(filePath);
            if(!file.exists()) {
                // 文件不存在:
                showToast(getString(R.string.toast_file_not_exist) + filePath);
                callJsFunction("onOpenDocumentError('" + getString(R.string.toast_file_not_exist) + filePath + "')");
                return;
            }

            try {
                // 3. 获取正确的MIME类型
                String mimeType = getMimeType(fileType, filePath);
                if (mimeType == null) {
                    // 文件不存在
                    showToast(getString(R.string.toast_unsupported_file_type) + fileType);
                    callJsFunction("onOpenDocumentError('" + getString(R.string.toast_unsupported_file_type) + fileType + "')");
                    return;
                }

                // 4. 创建打开文件的Intent
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri fileUri;

                // 5. 适配Android 7.0+的FileProvider机制（核心修复）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // 使用FileProvider生成安全的Uri
                    fileUri = FileProvider.getUriForFile(mActivity, mActivity.getFileProviderAuthority(), file);
                    // 授予临时读取权限
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    // 7.0以下直接使用文件Uri
                    fileUri = Uri.fromFile(file);
                }

                // 6. 设置Intent数据和类型
                intent.setDataAndType(fileUri, mimeType);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // 7. 检查是否有应用能处理该Intent（避免崩溃）
                List<ResolveInfo> resolveInfos = mActivity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (resolveInfos.isEmpty()) {
                    // 未找到可打开该文件的应用
                    showToast(mActivity.getString(R.string.toast_no_app_to_open_file));
                    callJsFunction("onOpenDocumentError('" + mActivity.getString(R.string.toast_no_app_to_open_file) + "')");
                    return;
                }

                // 8. 启动Intent打开文件
                mActivity.startActivity(intent);
                
                // 9. 回调成功
                // showToast("文件打开成功");
                callJsFunction("onOpenDocumentSuccess('" + fileNameFromPath(filePath) + "')");

            } catch (Exception e) {
                e.printStackTrace();
                // 打开文件失败
                String errorMsg = mActivity.getString(R.string.toast_open_file_failed) + e.getMessage();
                showToast(errorMsg);
                callJsFunction("onOpenDocumentError('" + errorMsg + "')");
            }
        }

        // 辅助方法：获取文件MIME类型（兼容fileType参数和文件后缀）
        private String getMimeType(String fileType, String filePath) {
            // 优先使用传入的fileType
            if (fileType != null && !fileType.isEmpty()) {
                switch (fileType.toLowerCase()) {
                    case "doc": return "application/msword";
                    case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    case "xls": return "application/vnd.ms-excel";
                    case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    case "ppt": return "application/vnd.ms-powerpoint";
                    case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    case "pdf": return "application/pdf";
                    case "txt": return "text/plain";
                    case "jpg":
                    case "jpeg": return "image/jpeg";
                    case "png": return "image/png";
                    case "gif": return "image/gif";
                    case "mp4": return "video/mp4";
                    case "zip": return "application/zip";
                    case "rar": return "application/x-rar-compressed";
                    default: break;
                }
            }

            // 备用方案：从文件路径解析后缀
            String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
            return getMimeType(extension, "");
        }

        // 辅助方法：从路径提取文件名
        private String fileNameFromPath(String filePath) {
            int lastSlash = filePath.lastIndexOf(File.separator);
            return lastSlash != -1 ? filePath.substring(lastSlash + 1) : filePath;
        }

        // 显示Toast（必须在主线程执行）
        private void showToast(String msg) {
            mActivity.runOnUiThread(() -> Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show());
        }

        // 调用H5的JS函数（回调结果）
        private void callJsFunction(String jsCode) {
            mActivity.runOnUiThread(() -> webView.evaluateJavascript("javascript:" + jsCode, null));
        }
    }

    // ======== 新增：兼容下载目录的方法 ========
    private File getDownloadDirectory() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 9 及以下仍然使用公共下载目录
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        //     // Android 10+ 使用应用专属下载目录，无需请求外部存储权限
        //     dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        //     if (dir != null && !dir.exists()) {
        //         dir.mkdirs();
        //     }
        // } else {
        // }
        return dir;
    }

    /**
     * Android 10+ 将文件保存到公共 Download 目录。
     * 返回一个可写的 OutputStream；调用端负责写入并关闭流。
     */
    private OutputStream openPublicDownloadStream(String fileName) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("无法创建 MediaStore 条目");
            }
            // 保存Uri供安装时使用
            apkMediaUri = uri;
            return getContentResolver().openOutputStream(uri);
        } else {
            // 9 及以下仍旧使用传统路径（需 WRITE_EXTERNAL_STORAGE 权限）
            File downloadDir = getDownloadDirectory();
            if (!downloadDir.exists()) downloadDir.mkdirs();
            File file = new File(downloadDir, fileName);
            return new FileOutputStream(file);
        }
    }

    // 获取版本名称
    public String getVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return String.valueOf(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 获取包名
    public String getPackageName() {
        Context context = getApplicationContext();
        // Log.i(TAG, "包名： " + context.getPackageName());
        return context.getPackageName();
    }

    // 从接口获取APK文件信息并下载（核心方法）
    private void getAPKFile(String fileName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_APK_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GraphqlApiService apiService = retrofit.create(GraphqlApiService.class);
        // 3. 构建请求参数
        // 3.1 构建 GraphQL 查询语句（和 curl 中的 query 一致）
        // 给查询语句加上名字，和我们在 GraphqlRequest 中传入的 operationName 保持一致
        String query = "query getDeviceFirmware($type: Int, $pageSize: Int, $currentPage: Int, $name: String) {\n" +
                "  result: deviceFirmwareWithPagination(\n" +
                "    type: $type\n" +
                "    pageSize: $pageSize\n" +
                "    currentPage: $currentPage\n" +
                "    name: $name\n" +
                "  ) {\n" +
                "    nodes {\n" +
                "      id\n" +
                "      name\n" +
                "      fileName\n" +
                "      verify\n" +
                "      fileUrl\n" +
                "      versionName\n" +
                "      type\n" +
                "      desc\n" +
                "    }\n" +
                "    pageInfo {\n" +
                "      totalCount\n" +
                "      pageSize\n" +
                "      currentPage\n" +
                "    }\n" +
                "  }\n" +
                "}";

        // 3.2 构建 variables 参数（和 curl 中的 variables 一致）
        GraphqlRequest.Variables variables = new GraphqlRequest.Variables(10, 1, fileName);

        // 3.3 构建请求体
        GraphqlRequest requestBody = new GraphqlRequest("getDeviceFirmware", variables, query);

        // 4. 构建并发送请求
        // 先准备一个 HeaderMap，方便后续如果要改头只需调整这里
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("accept", "*/*");
        headers.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        headers.put("authorization", "Bearer " + TOKEN);
        headers.put("cache-control", "no-cache");
        headers.put("content-type", "application/json");
        headers.put("origin", "https://kmg.test.smshj.com");
        headers.put("pragma", "no-cache");
        headers.put("priority", "u=1, i");
        headers.put("referer", "https://kmg.test.smshj.com/");
        headers.put("sec-ch-ua", "\"Not:A-Brand\";v=\"99\", \"Microsoft Edge\";v=\"145\", \"Chromium\";v=\"145\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("sec-fetch-dest", "empty");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-site", "same-site");
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36 Edg/145.0.0.0");

        Call<GraphqlResponse> call = apiService.sendGraphql(headers, requestBody);

        // 5. 异步发送请求（Retrofit 自动处理子线程，回调切主线程）
        call.enqueue(new Callback<GraphqlResponse>() {
            @Override
            public void onResponse(Call<GraphqlResponse> call, Response<GraphqlResponse> response) {
                // 请求成功（HTTP 状态码 200+）
                if (response.isSuccessful() && response.body() != null) {
                    GraphqlResponse graphqlResponse = response.body();
                    // 获取固件列表
                    GraphqlResponse.Result result = graphqlResponse.getData() != null ? graphqlResponse.getData().getResult() : null;
                    Log.i(TAG, "请求成功，解析结果中..." + " " + result + " " + response.code());
                    if (result != null && result.getNodes() != null && !result.getNodes().isEmpty()) {
                        // 遍历固件列表（示例：打印第一个固件名称）
                        String firstFirmwareName = result.getNodes().get(0).getName();
                        Log.d(TAG, "第一个固件名称：" + firstFirmwareName);
                        Log.d(TAG, "第一个固件 文件名：" + result.getNodes().get(0).getFileName());
                        Log.d(TAG, "第一个固件 版本名：" + result.getNodes().get(0).getVersionName());
                        Log.d(TAG, "第一个固件 文件地址：" + result.getNodes().get(0).getFileUrl());
                        OSS_BASE_URL = result.getNodes().get(0).getFileUrl();
                        // 初始化下载文件位置（使用URL中的文件名）
                        try {
                            String fileName = OSS_BASE_URL.substring(OSS_BASE_URL.lastIndexOf('/') + 1);
                            // 同样使用兼容方法获取目录
                            File downloadDir = getDownloadDirectory();
                            apkFile = new File(downloadDir, fileName);
                            Log.i(TAG, "apkFile initialized: " + apkFile.getAbsolutePath());
                        } catch (Exception e) {
                            Log.e(TAG, "初始化apkFile失败", e);
                        }
                        // 更新 UI（比如 Toast 显示）
                        // Toast.makeText(MainActivity.this, "获取到新版本：" + firstFirmwareName, Toast.LENGTH_SHORT).show();
                        // 获取分页总数
                        int totalCount = result.getPageInfo().getTotalCount();
                        Log.d(TAG, "固件总数：" + totalCount);

                        // BigDecimal versionName = new BigDecimal(result.getNodes().get(0).getVersionName());
                        // BigDecimal currentVersionName = new BigDecimal(getVersionName());
                        String versionName = result.getNodes().get(0).getVersionName();
                        String currentVersionName = getVersionName();
                        Log.i(TAG, "新版版本号：" + versionName);
                        Log.i(TAG, "当前版本号：" + currentVersionName);
                        if (!versionName.equals(currentVersionName)) {
                            showConfirmOnlyDialog();
                        }
                    } else {
                        Log.w(TAG, "响应成功但数据为空");
                    }
                } else {
                    // 响应失败（比如 HTTP 401/404/500）
                    Log.e(TAG, "响应失败：状态码=" + response.code());
                    Toast.makeText(MainActivity.this, getString(R.string.toast_response_failed) + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GraphqlResponse> call, Throwable t) {
                // 请求失败（网络错误、URL 错误等）
                Log.e(TAG, "请求失败：" + t.getMessage(), t);
                Toast.makeText(MainActivity.this, getString(R.string.toast_request_failed) + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    // 弹窗实例，用于在下载过程中更新UI
    private AlertDialog updateDialog;
    // 进度条和进度文本引用
    private ProgressBar progressBar;
    private TextView tvProgress;
    // MediaStore返回的APK文件Uri（Android 10+）
    private Uri apkMediaUri;

    /**
     * 显示「只能点确定关闭」的弹窗
     */
    private void showConfirmOnlyDialog() {
         // 1. 加载自定义布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_layout, null);

        // 2. 构建弹窗（隐藏系统默认标题，使用自定义布局）
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        // 核心：禁止外部关闭（空白处/返回键无效）
        builder.setCancelable(false);

        // 3. 创建弹窗并设置背景透明（避免系统默认的方形背景）
        updateDialog = builder.create();
        updateDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // 关键：透明背景

        // 4. 绑定确定按钮点击事件
        Button btnConfirm = dialogView.findViewById(R.id.btn_dialog_confirm);
        // 获取进度条和进度文本引用
        progressBar = dialogView.findViewById(R.id.progress_bar);
        tvProgress = dialogView.findViewById(R.id.tv_progress);
        
        btnConfirm.setOnClickListener(v -> {
            // 先检查权限，权限通过后再显示进度条
            checkPermissionAndDownload();
        });

        // 可选：动态修改弹窗标题/内容
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvContent = dialogView.findViewById(R.id.tv_dialog_content);
        // 提示
        tvTitle.setText(getString(R.string.update_dialog_title)); // 自定义标题
        // 有新版本发布,请安装后使用
        tvContent.setText(getString(R.string.update_dialog_content)); // 自定义内容

        // 5. 显示弹窗
        updateDialog.show();

        // 可选：设置弹窗宽度（比如占屏幕80%）
        updateDialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.8),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
        );
    }
    private void checkPermissionAndDownload() {
        Log.i(TAG, "检查权限并下载更新");
        // 1. 检查 Android 8.0+ 安装未知来源应用权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PackageManager pm = getPackageManager();
                if (pm != null) {
                    // 修复：通过反射调用 canRequestPackageInstalls()，避免编译版本兼容问题
                    boolean canInstall = false;
                    try {
                        Method method = pm.getClass().getMethod("canRequestPackageInstalls");
                        canInstall = (boolean) method.invoke(pm);
                    } catch (NoSuchMethodException e) {
                        // 极端情况：设备系统无此方法，默认视为无权限
                        Log.w(TAG, "设备不支持 canRequestPackageInstalls 方法", e);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        Log.e(TAG, "调用 canRequestPackageInstalls 方法失败", e);
                    }

                    if (!canInstall) {
                        // 跳转到设置页面开启安装权限
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + getPackageName()));
                        // 兼容 Android X：如果是 AppCompatActivity，用 startActivityForResult
                        startActivityForResult(intent, REQUEST_INSTALL_PERMISSION_CODE);
                        return;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "检查安装权限时出错", e);
            }
        }

        // 2. 检查存储权限（仅Android 10以下需要）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_CODE);
                return;
            }
        }

        // 权限全部通过，显示进度条并开始下载
        if (updateDialog != null && updateDialog.isShowing()) {
            Button btnConfirm = updateDialog.findViewById(R.id.btn_dialog_confirm);
            if (btnConfirm != null) {
                // 禁用确定按钮，防止重复点击
                btnConfirm.setEnabled(false);
                // 下载中...
                btnConfirm.setText(getString(R.string.downloading));
            }
            // 显示进度条
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (tvProgress != null) tvProgress.setVisibility(View.VISIBLE);
        }
        // 开始下载
        downloadApkFromOSS();
    }

    /**
     * 从 OSS 下载 APK 文件
     */
    private void downloadApkFromOSS() {
        // 开始下载APK
        Toast.makeText(this, getString(R.string.toast_start_download_apk), Toast.LENGTH_SHORT).show();

        // 先确保 apkFile 已经被设置
        if (apkFile == null) {
            try {
                String fileName = OSS_BASE_URL != null && OSS_BASE_URL.contains("/")
                        ? OSS_BASE_URL.substring(OSS_BASE_URL.lastIndexOf('/') + 1)
                        : "update.apk";
                File downloadDir = getDownloadDirectory();
                apkFile = new File(downloadDir, fileName);
                Log.i(TAG, "apkFile lazy-init: " + apkFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "初始化apkFile失败", e);
            }
        }

        // 创建 OkHttp 请求
        Log.i(TAG, "OSS_BASE_URL: " + OSS_BASE_URL);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(OSS_BASE_URL)
                .build();

        // 异步下载（避免阻塞主线程）
        client.newCall(request).enqueue(new okhttp3.Callback(){
            @Override
            public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                // 下载失败（主线程更新UI）
                runOnUiThread(() -> {
                    // 下载失败:
                    Toast.makeText(MainActivity.this, getString(R.string.toast_download_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "下载失败", e);
                    // 恢复确定按钮状态
                    if (updateDialog != null && updateDialog.isShowing()) {
                        Button btnConfirm = updateDialog.findViewById(R.id.btn_dialog_confirm);
                        if (btnConfirm != null) {
                            btnConfirm.setEnabled(true);
                            // 确定
                            btnConfirm.setText(getString(R.string.btn_confirm));
                        }
                        // 隐藏进度条
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (tvProgress != null) tvProgress.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        // 下载失败:响应码
                        Toast.makeText(MainActivity.this, getString(R.string.toast_download_failed_response) + response.code(), Toast.LENGTH_SHORT).show();
                        // 恢复确定按钮状态
                        if (updateDialog != null && updateDialog.isShowing()) {
                            Button btnConfirm = updateDialog.findViewById(R.id.btn_dialog_confirm);
                            if (btnConfirm != null) {
                                btnConfirm.setEnabled(true);
                                // 确定
                                btnConfirm.setText(getString(R.string.btn_confirm));
                            }
                            // 隐藏进度条
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            if (tvProgress != null) tvProgress.setVisibility(View.GONE);
                        }
                    });
                    return;
                }

                // 读取响应流并写入文件
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    long responseLength = response.body() != null ? response.body().contentLength() : -1;
                    Log.i(TAG, "响应长度: " + responseLength);
                    inputStream = response.body().byteStream();
                    Log.i(TAG, "inputStream: " + inputStream);
                    Log.i(TAG, "apkFile: " + apkFile);
                    // 根据Android版本选择不同的输出流
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 10+ 使用MediaStore API写入公共目录
                        outputStream = openPublicDownloadStream(apkFile.getName());
                    } else {
                        // Android 10以下直接写入文件
                        outputStream = new FileOutputStream(apkFile);
                    }
                    Log.i(TAG, "outputStream: " + outputStream);
                    byte[] buffer = new byte[4096];
                    int len;
                    long downloaded = 0; // 已下载字节数
                    while ((len = inputStream.read(buffer)) != -1) {
                        downloaded += len;
                        Log.i(TAG, "下载进度: " + downloaded + "/" + responseLength);
                        outputStream.write(buffer, 0, len);
                        
                        // 更新进度条
                        if (responseLength > 0 && progressBar != null && tvProgress != null) {
                            final int progress = (int) ((downloaded * 100) / responseLength);
                            runOnUiThread(() -> {
                                progressBar.setProgress(progress);
                                tvProgress.setText(progress + "%");
                            });
                        }
                    }
                    outputStream.flush();

                    // 下载完成，主线程调用安装程序
                    runOnUiThread(() -> {
                        // 隐藏进度条，恢复按钮状态
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (tvProgress != null) tvProgress.setVisibility(View.GONE);
                        if (updateDialog != null && updateDialog.isShowing()) {
                            Button btnConfirm = updateDialog.findViewById(R.id.btn_dialog_confirm);
                            if (btnConfirm != null) {
                                btnConfirm.setEnabled(true);
                                // 安装
                                btnConfirm.setText(getString(R.string.btn_install));
                                btnConfirm.setOnClickListener(v -> {
                                    // 安装APK
                                    installApk();
                                    // 关闭弹窗
                                    // updateDialog.dismiss();
                                });
                            }
                        }
                    });
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.toast_download_complete), Toast.LENGTH_SHORT).show();
                        installApk();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "下载失败", e);
                    // 主线程提示
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, getString(R.string.toast_download_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // 恢复确定按钮状态
                        if (updateDialog != null && updateDialog.isShowing()) {
                            Button btnConfirm = updateDialog.findViewById(R.id.btn_dialog_confirm);
                            if (btnConfirm != null) {
                                btnConfirm.setEnabled(true);
                                btnConfirm.setText(getString(R.string.btn_confirm));
                            }
                            // 隐藏进度条
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                            if (tvProgress != null) tvProgress.setVisibility(View.GONE);
                        }
                    });
                } finally {
                    // 关闭流
                    if (inputStream != null) inputStream.close();
                    if (outputStream != null) outputStream.close();
                }
            }
        });
    }

     /**
     * 调用系统安装程序安装 APK
     */
    private void installApk() {
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = null;

        // 根据Android版本选择不同的安装方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && apkMediaUri != null) {
            // Android 10+ 使用MediaStore返回的Uri
            apkUri = apkMediaUri;
            // 授予临时读取权限
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (apkFile != null && apkFile.exists()) {
            // Android 10以下或MediaStore Uri不可用时，使用传统文件路径
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android 7.0+ 必须用 FileProvider 封装 Uri
                apkUri = FileProvider.getUriForFile(this,
                        getFileProviderAuthority(), // 和Manifest中的authorities一致
                        apkFile);
                // 授予临时读取权限
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                // Android 7.0以下直接使用文件Uri
                apkUri = Uri.fromFile(apkFile);
            }
        } else {
            Toast.makeText(this, "APK文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        // 设置安装参数
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 启动安装界面
        try {
            startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_install_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "安装失败", e);
        }
    }

    /**
     * 权限请求/安装权限设置回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PERMISSION_CODE) {
            // 检查安装权限是否开启
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                   checkPermissionAndDownload(); // 权限已开，继续下载
                } else {
                    // 请开启安装未知来源应用权限
                    Toast.makeText(this, getString(R.string.toast_enable_unknown_sources), Toast.LENGTH_SHORT).show();
                }
            }   
        }
    }

    /**
     * 存储权限请求回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予，检查是否有待处理的操作
                if (pendingOperation != null) {
                    switch (pendingOperation) {
                        case "scanCode":
                            openScanner();
                            break;
                        case "saveBase64File":
                            if (pendingSaveBase64Data != null && pendingSaveFileName != null) {
                                new JsBridge(this).performSaveBase64File(pendingSaveBase64Data, pendingSaveFileName);
                                // 清空待处理数据
                                pendingSaveBase64Data = null;
                                pendingSaveFileName = null;
                            }
                            break;
                        case "takePhoto":
                            openCamera();
                            break;
                        case "selectPhoto":
                            openGallery();
                            break;
                    }
                    // 清空待处理操作
                    pendingOperation = null;
                }
            } else {
                // 检查是否勾选了"不再询问"
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0]);
                if (!showRationale) {
                    // 用户勾选了"不再询问"，引导用户去设置页面
                    String permissionName = "权限";
                    if (permissions[0].equals(Manifest.permission.CAMERA)) {
                        permissionName = "相机权限";
                    } else if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) || 
                               permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                               permissions[0].equals(Manifest.permission.READ_MEDIA_IMAGES)) {
                        permissionName = "存储权限";
                    }
                    
                    new AlertDialog.Builder(this)
                            // .setTitle("权限提示")
                            .setTitle(getString(R.string.permission_dialog_title))
                            // 需要 xxx 权限才能执行此操作, 请在设置中开启
                            .setMessage(getString(R.string.permission_dialog_message, permissionName)) // 使用占位符格式化字符串
                            .setPositiveButton(getString(R.string.btn_settings), (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivityForResult(intent, REQUEST_PERMISSION_CODE);
                            })
                            // 取消
                            .setNegativeButton(getString(R.string.btn_cancel), null)
                            .show();
                } else {
                    // 用户拒绝了权限但未勾选"不再询问"
                    String permissionName = "权限";
                    if (permissions[0].equals(Manifest.permission.CAMERA)) {
                        permissionName = "相机权限";
                    } else if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) || 
                               permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                               permissions[0].equals(Manifest.permission.READ_MEDIA_IMAGES)) {
                        permissionName = "存储权限";
                    }
                    // 您需要授予 xxx 权限才能执行此操作
                    Toast.makeText(this, getString(R.string.permission_dialog_message, permissionName), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}