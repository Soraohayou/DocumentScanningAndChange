package com.example.documentscanningandchange;

import static com.example.documentscanningandchange.AuthService.getAuth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.scan).setOnClickListener(view -> checkFilePermission());
        findViewById(R.id.camera).setOnClickListener(view -> checkCameraPermission());
        findViewById(R.id.change).setOnClickListener(view -> toNewActivity(ChangeActivity.class));
        findViewById(R.id.translate).setOnClickListener(view -> toNewActivity(QCCodeActivity.class));
        findViewById(R.id.qrcode).setOnClickListener(view -> checkCameraAndRecordingPermission());

        new Thread() {
            @Override
            public void run() {
                super.run();
                MyApplication.token = getAuth();
            }
        }.start();

    }

    public void toNewActivity(Class clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }

    private void checkCameraAndRecordingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            toNewActivity(TranslateActivity.class);
        } else {
            XXPermissions.with(this).permission(Permission.CAMERA).permission(Permission.READ_EXTERNAL_STORAGE).permission(Permission.WRITE_EXTERNAL_STORAGE).permission(Permission.RECORD_AUDIO).request((permissions, all) -> {
                if (all) {
                    //获取权限成功
                    toNewActivity(TranslateActivity.class);
                } else {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        toNewActivity(TranslateActivity.class);
                    } else {
                        //获取部分权限成功，但部分权限未正常授予
                        Toast.makeText(MainActivity.this, "请获取全部权限", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            toNewActivity(CameraActivity.class);
        } else {
            XXPermissions.with(this).permission(Permission.CAMERA).permission(Permission.READ_EXTERNAL_STORAGE).permission(Permission.WRITE_EXTERNAL_STORAGE).request((permissions, all) -> {
                if (all) {
                    //获取权限成功
                    toNewActivity(CameraActivity.class);
                } else {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        toNewActivity(CameraActivity.class);
                    } else {
                        //获取部分权限成功，但部分权限未正常授予
                        Toast.makeText(MainActivity.this, "请获取全部权限", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void checkFilePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            toNewActivity(ScanActivity.class);
        } else {
            XXPermissions.with(this).permission(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE).request(new OnPermissionCallback() {
                @Override
                public void onGranted(List<String> permissions, boolean all) {
                    if (all) {
                        toNewActivity(ScanActivity.class);
                    } else {
                        //获取部分权限成功，但部分权限未正常授予
                        Toast.makeText(MainActivity.this, "请获取全部权限", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

}
