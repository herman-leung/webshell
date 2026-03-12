package com.shengma.webshell;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.camera.core.ExperimentalGetImage;

@OptIn(markerClass = ExperimentalGetImage.class)
public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "ScanActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        previewView = findViewById(R.id.previewView);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnTorch = findViewById(R.id.btnTorch);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 闪光灯按钮
        btnTorch.setOnClickListener(v -> toggleTorch());

        // 初始化ML Kit条码扫描器
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_CODABAR,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    private void toggleTorch() {
        if (camera != null) {
            if (camera.getCameraInfo().hasFlashUnit()) {
                if (camera.getCameraInfo().getTorchState().getValue() == TorchState.ON) {
                    camera.getCameraControl().enableTorch(false);
                    Toast.makeText(this, "闪光灯已关闭", Toast.LENGTH_SHORT).show();
                } else {
                    camera.getCameraControl().enableTorch(true);
                    Toast.makeText(this, "闪光灯已开启", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "设备不支持闪光灯", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // 预览用例
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 图像分析用例 - 用于条码扫描
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    processImage(image);
                });

                // 选择后置摄像头
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // 绑定用例到生命周期
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                // CameraX默认支持双指变焦，不需要额外设置
                previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

                Log.i(TAG, "相机启动成功，支持变焦");

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "相机启动失败", e);
                Toast.makeText(this, "相机启动失败", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(@NonNull androidx.camera.core.ImageProxy imageProxy) {
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            if (rawValue != null && !rawValue.isEmpty()) {
                                Log.i(TAG, "扫码成功: " + rawValue);

                                // 返回结果
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("SCAN_RESULT", rawValue);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                                return;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "扫码失败", e);
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能扫码", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        // 显式解绑相机资源
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}