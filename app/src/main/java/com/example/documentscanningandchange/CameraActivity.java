package com.example.documentscanningandchange;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    private File picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        findViewById(R.id.button).setOnClickListener(view ->{
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //调用系统相机
            String fileName = DateFormat.format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.CHINA)) + ".jpg";
            picture = new File(Environment.getExternalStorageDirectory()
                    .getAbsolutePath()+"/service",fileName);
            Uri imageUri = Uri.fromFile(picture);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);          //直接使用，没有缩小
            startActivityForResult(intent, 100);// 100 是请求码
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri = Uri.fromFile(picture);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(uri);
        this.sendBroadcast(intent);
    }

    public void saveQNext(Bitmap image, Context context, String fileName, int quality) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "艾跳跳";
        Log.i("TAG", "saveQNext: >>> " + path);
        // 创建文件夹
        mkdir(path);
        // 文件名称
        Log.i("TAG", "saveQNext: " + fileName);
        File file = new File(path, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            // 通过io流的方式来压缩保存图片
            image.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            fos.close();
            // 保存图片后发送广播通知更新数据库
            Uri uri = Uri.fromFile(file);
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void saveQUp(Bitmap image, Context context, String fileName, int quality) {
        // 文件夹路径
        String imageSaveFilePath = Environment.DIRECTORY_DCIM + File.separator + "艾跳跳";
        Log.i("TAG", "文件夹目录 >>> " + imageSaveFilePath);
        mkdir(imageSaveFilePath);
        // 文件名字
        Log.d("TAG", "文件名字 >>> " + fileName);
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.TITLE, fileName);
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, fileName);
        //该媒体项在存储设备中的相对路径，该媒体项将在其中保留
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, imageSaveFilePath);
        Uri uri = null;
        OutputStream outputStream = null;
        ContentResolver localContentResolver = context.getContentResolver();
        try {
            uri = localContentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            outputStream = localContentResolver.openOutputStream(uri);
            // Bitmap图片保存
            // 1、宽高比例压缩
            // 2、压缩参数
            image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (uri != null) {
                localContentResolver.delete(uri, null, null);
            }
        } finally {
            image.recycle();
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}