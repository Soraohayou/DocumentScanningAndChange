package com.example.documentscanningandchange;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.freddy.silhouette.widget.button.SleTextButton;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

public class QCCodeActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SCAN_ONE = 99;
    private static final int REQUEST_CODE_PERMISSION = 98;
    //必须的权限
    private final String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qccode);

        findViewById(R.id.button).setOnClickListener(v -> {
            //判断操作系统的版本，Android 6.0及以上需要动态申请权限
            this.requestPermissions(permissions, REQUEST_CODE_PERMISSION);
        });

    }

    //开启默认扫码模式
    private void startDefaultScanMode() {
        HmsScanAnalyzerOptions options = new HmsScanAnalyzerOptions.Creator().setHmsScanTypes(HmsScan.QRCODE_SCAN_TYPE, HmsScan.DATAMATRIX_SCAN_TYPE).create();
        ScanUtil.startScan(this, REQUEST_CODE_SCAN_ONE, options);
    }

    //申请权限后的返回结果处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "需要开启该相机和存储权限才能正常使用扫码功能！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            startDefaultScanMode();
        }
    }

    //扫码结果处理
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_ONE) {
            SleTextButton viewById = findViewById(R.id.result);
            //解析出扫码结果对象，并toast一下
            HmsScan obj = data.getParcelableExtra(ScanUtil.RESULT);
            if (obj != null) {
                viewById.setText(obj.originalValue);
            } else {
                Toast.makeText(this, "无结果输出", Toast.LENGTH_SHORT).show();
                viewById.setText("");
            }
        }
    }

}