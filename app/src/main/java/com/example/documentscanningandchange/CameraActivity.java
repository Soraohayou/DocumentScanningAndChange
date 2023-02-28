package com.example.documentscanningandchange;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private File picture;

    private Uri photoUri;
    private ImageView btn;
    private ImageView image;
    private Bitmap bitmap;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        btn = findViewById(R.id.imagebtn);
        image = findViewById(R.id.image);

        btn.setOnClickListener(view -> {
            Log.d("fuck", "tnnd就是拍不了照");
            File file = new File(getExternalFilesDir(null), "outputimage.jpg");
            Log.d("fuck", file.getAbsolutePath());
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            photoUri = FileProvider.getUriForFile(this, "com.example.documentscanningandchange.fileprovider", file);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, 1);
        });

        findViewById(R.id.save).setOnClickListener(view -> {

            if (bitmap == null) {
                Toast.makeText(this, "当前没有照片！请拍照后保存", Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(this, "保存中，请稍等", Toast.LENGTH_SHORT).show();
            saveBitmap(this, bitmap);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                ContentResolver contentResolver = getContentResolver();
                bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(photoUri));
                image.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveBitmap(Context activity, Bitmap bitmap) {

        //设置图片名称，要保存png，这里后缀就是png，要保存jpg，后缀就用jpg
        String imageName = System.currentTimeMillis() + "code.png";
        //Android Q  10为每个应用程序提供了一个独立的在外部存储设备的存储沙箱，没有其他应用可以直接访问您应用的沙盒文件
        File f = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File file = new File(f.getPath() + "/" + imageName);//创建文件
        //        file.getParentFile().mkdirs();
        try {
            //文件输出流
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            //压缩图片，如果要保存png，就用Bitmap.CompressFormat.PNG，要保存jpg就用Bitmap.CompressFormat.JPEG,质量是100%，表示不压缩
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            //写入，这里会卡顿，因为图片较大
            fileOutputStream.flush();
            //记得要关闭写入流
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 下面的步骤必须有，不然在相册里找不到图片，若不需要让用户知道你保存了图片，可以不写下面的代码。
        // 把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(activity.getContentResolver(), file.getAbsolutePath(), imageName, null);
            Toast.makeText(this, "保存成功，请您到 相册/图库 中查看", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "saveBitmap: 2");
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
//            // 最后通知图库更新
        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(file.getPath()))));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁当前页面后
    }
}