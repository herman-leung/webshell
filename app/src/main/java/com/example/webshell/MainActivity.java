package com.example.webshell;

import android.Manifest;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.database.Cursor;
import com.google.zxing.integration.android.IntentIntegrator;
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    // 权限请求码（统一用一个，避免冲突）
    private static final int REQUEST_CODE = 1001;

    //    private static final String DEFAULT_URL = "https://znh5.smshj.com";
//    private static final String DEFAULT_URL = "http://192.168.1.46:5500/index.html";
    // private static final String DEFAULT_URL = "https://192.168.1.46:80/";
   private static final String DEFAULT_URL = "https://twm-h5.smshj.com/";

    // ========== 新增：FileProvider 授权的包名后缀（需和xml配置一致） ==========
    //    TODO:
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.webshell.fileprovider";

    // 添加相册选择器
    private ActivityResultLauncher<Intent> galleryLauncher;
    // 添加拍照选择器
    private ActivityResultLauncher<Intent> cameraLauncher;
    // 添加扫码选择器
    private ActivityResultLauncher<Intent> scanLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        initWebView();
        checkPermission();
        initBackHandler();
        // 初始化相册选择器
        initGalleryLauncher();
        // 初始化拍照选择器
        initCameraLauncher();
        // 初始化扫码选择器
        initScanLauncher();
        // 处理intent参数（核心）
        handleIntent(getIntent());
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
                                    Toast.makeText(MainActivity.this, "图片已加载", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e("MainActivity", "Base64转换失败");
                                    Toast.makeText(MainActivity.this, "Base64转换失败", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e("MainActivity", "图片路径为空");
                                Toast.makeText(MainActivity.this, "获取图片路径失败", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(MainActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("MainActivity", "Base64转换失败");
                                Toast.makeText(MainActivity.this, "Base64转换失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("MainActivity", "拍照图片路径为空");
                            Toast.makeText(MainActivity.this, "获取拍照图片失败", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(MainActivity.this, "扫码失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.i("MainActivity", "用户取消扫码或无数据");
                            Toast.makeText(MainActivity.this, "扫码取消", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "处理扫码结果异常", e);
                        Toast.makeText(MainActivity.this, "扫码结果处理失败", Toast.LENGTH_SHORT).show();
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
            Uri photoURI = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "扫码功能异常：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            mActivity.openGallery();
        }

        // 供H5调用：打开相机拍照
        @JavascriptInterface
        public void takePhoto() {
            Log.i("MainActivity", "takePhoto called");
            mActivity.openCamera();
        }

        // 供H5调用：打开扫码器
        @JavascriptInterface
        public void scanCode() {
            Log.i("MainActivity", "scanCode called");
            mActivity.openScanner();
        }

        // 供H5调用的保存文件方法（必须加@JavascriptInterface注解）
        @JavascriptInterface
        public void saveFile(String fileUrl, String fileName) {
            Log.i("MainActivity", "fileUrl" + fileUrl);
            // 子线程执行文件下载（避免阻塞主线程）
            new Thread(() -> {
                try {
                    // 1. 从URL下载文件字节流
                    byte[] fileData = downloadFile(fileUrl);
                    if (fileData == null) {
                        showToast("文件下载失败，请检查URL");
                        return;
                    }
                    System.out.println("开始下载？？？？");

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

        /**
         * 供H5调用：保存Base64编码的文件
         * @param base64Data 纯Base64字符串（需去掉前缀如data:image/png;base64,）
         * @param fileName 保存的文件名（如test.png、document.pdf）
         */
        @JavascriptInterface
        public void saveBase64File(String base64Data, String fileName) {
            Log.i("MainActivity", "开始保存Base64文件：" + fileName);
            // 子线程执行文件保存（避免阻塞主线程）
            new Thread(() -> {
                try {
                    // 1. 校验Base64数据
                    if (base64Data == null || base64Data.isEmpty()) {
                        showToast("Base64数据为空");
                        callJsFunction("onFileSaveError('Base64数据为空')");
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
                        showToast("Base64解码失败");
                        callJsFunction("onFileSaveError('Base64解码失败')");
                        return;
                    }

                    // 4. 复用原有保存逻辑写入文件
                    boolean success = saveFileToLocal(fileData, fileName);
                    if (success) {
                        showToast("Base64文件保存成功：/下载/" + fileName);
                        callJsFunction("onFileSaveSuccess('" + fileName + "')");
                    } else {
                        showToast("Base64文件保存失败");
                        callJsFunction("onFileSaveError('保存失败')");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("Base64保存异常：" + e.getMessage());
                    callJsFunction("onFileSaveError('" + e.getMessage() + "')");
                }
            }).start();
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
                showToast("文件路径不能为空");
                callJsFunction("onOpenDocumentError('文件路径不能为空')");
                return;
            }

            // 2. 检查文件是否存在
            File file = new File(filePath);
            if(!file.exists()) {
                showToast("文件不存在：" + filePath);
                callJsFunction("onOpenDocumentError('文件不存在：" + filePath + "')");
                return;
            }

            try {
                // 3. 获取正确的MIME类型
                String mimeType = getMimeType(fileType, filePath);
                if (mimeType == null) {
                    showToast("不支持的文件类型：" + fileType);
                    callJsFunction("onOpenDocumentError('不支持的文件类型：" + fileType + "')");
                    return;
                }

                // 4. 创建打开文件的Intent
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri fileUri;

                // 5. 适配Android 7.0+的FileProvider机制（核心修复）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // 使用FileProvider生成安全的Uri
                    fileUri = FileProvider.getUriForFile(mActivity, FILE_PROVIDER_AUTHORITY, file);
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
                    showToast("未找到可打开该文件的应用");
                    callJsFunction("onOpenDocumentError('未找到可打开该文件的应用')");
                    return;
                }

                // 8. 启动Intent打开文件
                mActivity.startActivity(intent);
                
                // 9. 回调成功
                showToast("文件打开成功");
                callJsFunction("onOpenDocumentSuccess('" + fileNameFromPath(filePath) + "')");

            } catch (Exception e) {
                e.printStackTrace();
                String errorMsg = "打开文件失败：" + e.getMessage();
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
            mActivity.runOnUiThread(() -> Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show());
        }

        // 调用H5的JS函数（回调结果）
        private void callJsFunction(String jsCode) {
            mActivity.runOnUiThread(() -> webView.evaluateJavascript("javascript:" + jsCode, null));
        }
    }
}