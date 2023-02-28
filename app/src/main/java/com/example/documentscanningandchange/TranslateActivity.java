package com.example.documentscanningandchange;

import static com.example.documentscanningandchange.AuthService.getAuth;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TranslateActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private Uri photoUri;
    private ImageView btn;
    private ImageView image;
    private Bitmap bitmap;

    private File file, file1, file2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);
        btn = findViewById(R.id.imagebtn);
        image = findViewById(R.id.image);

        btn.setOnClickListener(view -> {
            Log.d("fuck", "tnnd就是拍不了照");
            file = new File(getExternalFilesDir(null), "outputimage.jpg");
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

        findViewById(R.id.translate).setOnClickListener(view -> {

            if (file == null) {
                Toast.makeText(this, "当前没有照片！请拍照后保存", Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(this, "翻译中，请稍等", Toast.LENGTH_SHORT).show();
            saveBitmap(compressScale(bitmap), this);
            file1 = new File(getExternalFilesDir(null), "outputimage1.jpg");
            picTrans(file1);
        });

        findViewById(R.id.recording).setOnClickListener(view -> {

            if (TextUtils.isEmpty(MyApplication.token)) {
                MyApplication.token = getAuth();
                if (TextUtils.isEmpty(MyApplication.token)) {
                    Toast.makeText(this, "获取token失败！", Toast.LENGTH_SHORT).show();
                } else {
                    showPopWindow();
                }
            } else {
                showPopWindow();
            }

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

    public void picTrans(File file) {

        OkHttpClient okHttpClient = new OkHttpClient();

        if (TextUtils.isEmpty(MyApplication.token)) {
            MyApplication.token = getAuth();
            if (TextUtils.isEmpty(MyApplication.token)) {
                Toast.makeText(this, "获取token失败！", Toast.LENGTH_SHORT).show();
            } else {
                picTrans(file);
            }
        } else {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("image", file.getName(), RequestBody.create(MediaType.parse("multipart/form-data"), file)).addFormDataPart("from", "en").addFormDataPart("to", "zh").addFormDataPart("v", "3").addFormDataPart("paste", "1").build();
            Request request = new Request.Builder().url("https://aip.baidubce.com/file/2.0/mt/pictrans/v1?access_token=" + MyApplication.token).post(requestBody).build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String result = Objects.requireNonNull(response.body()).string();
                        String base64Str;
                        JSONObject jsonObject = JSONObject.parseObject(result);
                        if (jsonObject.getString("error_code").equals("0")) {
                            base64Str = jsonObject.getJSONObject("data").getString("pasteImg");
                            runOnUiThread(() -> {
                                image.setImageBitmap(stringToBitmap(base64Str));
                                //((TextView)findViewById(R.id.result)).setText(result);
                            });
                        }
                        Log.d(TAG, "doPostAsync: " + result);

                    }
                }
            });
        }

    }

    public Bitmap stringToBitmap(String string) {
        //将字符串转换成Bitmap类型
        Bitmap bitmap = null;
        try {
            byte[] bitmapArray;
            bitmapArray = Base64.decode(string, Base64.DEFAULT);
            bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    /**
     * 图片按比例大小压缩方法
     *
     * @param image （根据Bitmap图片压缩）
     * @return
     */
    public static Bitmap compressScale(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        // 判断如果图片大于1M,进行压缩避免在生成图片（BitmapFactory.decodeStream）时溢出
        if (baos.toByteArray().length / 1024 > 1024) {
            baos.reset();// 重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        //分辨率不能大于该数值
        float hh = 512f;
        float ww = 512f;
        // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if ((w == h || w > h) && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if ((w == h || w < h) && h > hh) { // 如果高度高的话根据高度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0) be = 1;
        newOpts.inSampleSize = be; // 设置缩放比例
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        isBm = new ByteArrayInputStream(baos.toByteArray());
        bitmap = BitmapFactory.decodeStream(isBm, null, newOpts);
        return bitmap;
    }

    static void saveBitmap(Bitmap bm, Context mContext) {

        Log.d("Save Bitmap", "Ready to save picture");

        //指定我们想要存储文件的地址

        String TargetPath = mContext.getExternalFilesDir(null) + "/";

        Log.d("Save Bitmap", "Save Path=" + TargetPath);

        //判断指定文件夹的路径是否存在

        if (!fileIsExist(TargetPath)) {

            Log.d("Save Bitmap", "TargetPath isn't exist");

        } else {

            //如果指定文件夹创建成功，那么我们则需要进行图片存储操作

            File saveFile = new File(TargetPath, "outputimage1.jpg");

            try {

                FileOutputStream saveImgOut = new FileOutputStream(saveFile);

                // compress - 压缩的意思

                bm.compress(Bitmap.CompressFormat.JPEG, 80, saveImgOut);

                //存储完成后需要清除相关的进程

                saveImgOut.flush();

                saveImgOut.close();

                Log.d("Save Bitmap", "The picture is save to your phone!");

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }

    }

    static boolean fileIsExist(String fileName) {

        //传入指定的路径，然后判断路径是否存在

        File file = new File(fileName);

        if (file.exists())

            return true;

        else {

            //file.mkdirs() 创建文件夹的意思

            return file.mkdirs();

        }

    }

    //设置PopWindow的主方法
    public void showPopWindow() {
        View parent = ((ViewGroup) this.findViewById(android.R.id.content)).getChildAt(0);
        View popView = View.inflate(this, R.layout.popup_window, null);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels / 4;

        final PopupWindow popupWindow = new PopupWindow(popView, width, height);
        popupWindow.setAnimationStyle(R.style.pop_anim);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);//设置点击外部消失;

        SoundRecordingButton soundRecordingButton = popView.findViewById(R.id.record);
        soundRecordingButton.setOnSoundRecordingFinishListener(item -> {

            if (!TextUtils.isEmpty(item.path)) {
                try {
                    String base64Str = getFileContentAsBase64(item.path);
                    Log.i(TAG, "showPopWindow: " + base64Str.length());

                    OkHttpClient okHttpClient = new OkHttpClient();

                    MediaType mediaType = MediaType.parse("application/json");
                    //RequestBody requestBody = new FormBody.Builder().add("from", "en").add("to", "zh").add("voice", base64Str).add("format", "wav").build();
                    //Request request = new Request.Builder().url("https://aip.baidubce.com/rpc/2.0/mt/v2/speech-translation?access_token=" + MyApplication.token).post(requestBody).addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").build();
                    RequestBody body = RequestBody.create(mediaType, "{\"from\":\"en\",\"to\":\"zh\",\"format\":\"wav\",\"voice\":\""+base64Str+"\"}");
                    Request request = new Request.Builder()
                            .url("https://aip.baidubce.com/rpc/2.0/mt/v2/speech-translation?access_token=" + MyApplication.token)
                            .method("POST", body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json")
                            .build();
                    okHttpClient.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String result = Objects.requireNonNull(response.body()).string();
                                Log.d(TAG, "doPostAsync: " + result);
                                JSONObject jsonObject = JSONObject.parseObject(result);

                                if (!jsonObject.containsKey("error_code")) {
                                    String target = jsonObject.getJSONObject("result").getString("target");
                                    runOnUiThread(() -> {
                                        ((TextView) findViewById(R.id.result)).setText(target);
                                    });
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(TranslateActivity.this, jsonObject.getString("error_code"), Toast.LENGTH_SHORT).show();
                                    });
                                }

                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "翻译失败！", Toast.LENGTH_SHORT).show();
                }
            }

            popupWindow.dismiss();
        });

        //设置popupwindow的背景颜色
        ColorDrawable dw = new ColorDrawable(Color.WHITE);
        popupWindow.setBackgroundDrawable(dw);
        //设置打开popupWindow的位置
        popupWindow.showAtLocation(parent, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

    }

    static String getFileContentAsBase64(String path) throws IOException {
        byte[] b = Files.readAllBytes(Paths.get(path));
        return java.util.Base64.getEncoder().encodeToString(b);
    }


    /**
     * 语音文件转base64
     */
    public static String PDFToBase64(File file) {
        Base64Encoder encoder = new Base64Encoder();
        FileInputStream fin = null;
        BufferedInputStream bin = null;
        ByteArrayOutputStream baos = null;
        BufferedOutputStream bout = null;
        try {
            fin = new FileInputStream(file);
            bin = new BufferedInputStream(fin);
            baos = new ByteArrayOutputStream();
            bout = new BufferedOutputStream(baos);
            byte[] buffer = new byte[1024];
            int len = bin.read(buffer);
            while (len != -1) {
                bout.write(buffer, 0, len);
                len = bin.read(buffer);
            }
            //刷新此输出流并强制写出所有缓冲的输出字节
            bout.flush();
            byte[] bytes = baos.toByteArray();
            return encoder.encode(bytes).trim();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fin.close();
                bin.close();
                bout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    private static class Base64Encoder {
        private final char last2byte = (char) Integer.parseInt("00000011", 2);
        private final char last4byte = (char) Integer.parseInt("00001111", 2);
        private final char last6byte = (char) Integer.parseInt("00111111", 2);
        private final char lead6byte = (char) Integer.parseInt("11111100", 2);
        private final char lead4byte = (char) Integer.parseInt("11110000", 2);
        private final char lead2byte = (char) Integer.parseInt("11000000", 2);
        private final char[] encodeTable = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};

        /**
         * Base64 encoding.
         *
         * @param from The src data.
         * @return
         */
        public String encode(byte[] from) {
            StringBuffer to = new StringBuffer((int) (from.length * 1.34) + 3);
            int num = 0;
            char currentByte = 0;
            for (int i = 0; i < from.length; i++) {
                num = num % 8;
                while (num < 8) {
                    switch (num) {
                        case 0:
                            currentByte = (char) (from[i] & lead6byte);
                            currentByte = (char) (currentByte >>> 2);
                            break;
                        case 2:
                            currentByte = (char) (from[i] & last6byte);
                            break;
                        case 4:
                            currentByte = (char) (from[i] & last4byte);
                            currentByte = (char) (currentByte << 2);
                            if ((i + 1) < from.length) {
                                currentByte |= (from[i + 1] & lead2byte) >>> 6;
                            }
                            break;
                        case 6:
                            currentByte = (char) (from[i] & last2byte);
                            currentByte = (char) (currentByte << 4);
                            if ((i + 1) < from.length) {
                                currentByte |= (from[i + 1] & lead4byte) >>> 4;
                            }
                            break;
                    }
                    to.append(encodeTable[currentByte]);
                    num += 6;
                }
            }
            if (to.length() % 4 != 0) {
                for (int i = 4 - to.length() % 4; i > 0; i--) {
                    to.append("=");
                }
            }
            return to.toString();
        }
    }


}