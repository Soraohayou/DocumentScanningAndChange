package com.example.documentscanningandchange;

import static com.example.documentscanningandchange.TranslateActivity.compressScale;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aspose.cells.PdfCompliance;
import com.aspose.cells.PdfSaveOptions;
import com.aspose.cells.Workbook;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import com.freddy.silhouette.widget.button.SleTextButton;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.maxvision.tbs.TbsUtils;
import com.zlylib.fileselectorlib.FileSelector;
import com.zlylib.fileselectorlib.bean.EssFile;
import com.zlylib.fileselectorlib.utils.Const;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ScanActivity";

    private final int REQUEST_FROM_CODE = 1;
    private final int REQUEST_TO_CODE = 2;

    private final int CHANGE_TYPE_WORD = 11;
    private final int CHANGE_TYPE_EXCEL = 12;
    private final int CHANGE_TYPE_PDF = 13;

    private int currentChangeType;

    private EssFile from, to;

    private Uri photoUri;

    private SleTextButton from_select, from_preview, to_select, to_preview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        findView();
        initListener();
    }

    private void findView() {
        from_select = findViewById(R.id.from_select);
        from_preview = findViewById(R.id.from_preview);
        to_select = findViewById(R.id.to_select);
        to_preview = findViewById(R.id.to_preview);
    }

    private void initListener() {
        from_select.setOnClickListener(this);
        from_preview.setOnClickListener(this);
        to_select.setOnClickListener(this);
        to_preview.setOnClickListener(this);
        findViewById(R.id.change).setOnClickListener(this);
        findViewById(R.id.choose_type).setOnClickListener(this);
        findViewById(R.id.from_delete).setOnClickListener(this);
        findViewById(R.id.to_delete).setOnClickListener(this);
        findViewById(R.id.scan).setOnClickListener(this);
        /*findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanBitmap(ScanActivity.this, drawableToBitamp(getResources().getDrawable(R.drawable.a)));
            }
        });*/
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.from_select:
                FileSelector.from(this).setMaxCount(1) // ??????????????????
                        .setFileTypes("pdf", "doc", "docx", "xlsx") //??????????????????
                        .setSortType(FileSelector.BY_NAME_ASC) //??????????????????
                        .requestCode(REQUEST_FROM_CODE) //???????????????
                        .setTargetPath("/storage/emulated/0/") //??????????????????
                        .start();
                break;
            case R.id.from_preview:
                if (from == null) {
                    Toast.makeText(this, "????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!from.isExits()) {
                    Toast.makeText(this, "???????????????????????????!", Toast.LENGTH_SHORT).show();
                }
                if (from.isDirectory()) {
                    Toast.makeText(this, "?????????????????????!", Toast.LENGTH_SHORT).show();
                }

                //?????????????????????filePath??????????????????/data/user/0/...../files/TestDoc.doc
                TbsUtils.loadFileType(this, from.getAbsolutePath(), from.getName());
                break;
            case R.id.to_select:
                FileSelector.from(this).setMaxCount(1).onlyShowFolder().onlySelectFolder() // ??????????????????
                        .setSortType(FileSelector.BY_NAME_ASC) //??????????????????
                        .requestCode(REQUEST_TO_CODE) //???????????????
                        .setTargetPath("/storage/emulated/0/") //??????????????????
                        .start();
                break;
            case R.id.to_preview:

                if (to == null) {
                    Toast.makeText(this, "????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!to.isExits()) {
                    Toast.makeText(this, "???????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (to.isDirectory()) {
                    Toast.makeText(this, "?????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                }
                TbsUtils.loadFileType(this, to.getAbsolutePath(), to.getName());
                break;
            case R.id.change:
                if (to == null) {
                    Toast.makeText(this, "???????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (from == null) {
                    Toast.makeText(this, "????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentChangeType != CHANGE_TYPE_PDF && currentChangeType != CHANGE_TYPE_EXCEL && currentChangeType != CHANGE_TYPE_WORD) {
                    Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(this, "?????????...?????????", Toast.LENGTH_SHORT).show();
                String suffix = from.getAbsolutePath().split("\\.")[from.getAbsolutePath().split("\\.").length - 1];
                try {
                    String new_url = "";
                    if (suffix.equals("doc") || suffix.equals("docx")) {

                        Document document = new Document(from.getAbsolutePath());
                        if (currentChangeType == CHANGE_TYPE_PDF) {

                            // word ???pdf
                            if (to.isDirectory()) {

                                if (from.getAbsolutePath().contains("docx")) {
                                    new_url = to.getAbsolutePath() + from.getName().replace("docx", "pdf");
                                } else {
                                    new_url = to.getAbsolutePath() + from.getName().replace("doc", "pdf");
                                }
                            } else {
                                if (from.getAbsolutePath().contains("docx")) {
                                    new_url = to.getAbsolutePath().substring(0,to.getAbsolutePath().lastIndexOf("/")+1) + from.getName().replace("docx", "pdf");
                                } else {
                                    new_url = to.getAbsolutePath().substring(0,to.getAbsolutePath().lastIndexOf("/")+1) + from.getName().replace("doc", "pdf");
                                }
                            }

                            if (new File(new_url).exists()) {
                                new File(new_url).delete();
                            }

                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.word);
                            document.save(new_url, SaveFormat.PDF);
                        } else if (currentChangeType == CHANGE_TYPE_EXCEL) {
                            Toast.makeText(this, "word???????????????excel", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "word???????????????word", Toast.LENGTH_SHORT).show();
                        }

                    } else if (suffix.equals("xlsx")) {
                        Workbook workbook = new Workbook(from.getAbsolutePath());// Save the document in PDF format
                        if (currentChangeType == CHANGE_TYPE_WORD) {
                            // Excel???word
                            if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("xlsx", "doc");
                            } else {
                                new_url = to.getAbsolutePath().substring(0,to.getAbsolutePath().lastIndexOf("/")+1) + from.getName().replace("xlsx", "doc");
                            }
                            workbook.save(new_url, com.aspose.cells.SaveFormat.DOCX);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.word);
                        } else if (currentChangeType == CHANGE_TYPE_EXCEL) {
                            Toast.makeText(this, "excel???????????????excel", Toast.LENGTH_SHORT).show();
                        } else {
                            // Excel???Pdf
                            if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("xlsx", "pdf");
                            } else {
                                new_url = to.getAbsolutePath().substring(0,to.getAbsolutePath().lastIndexOf("/")+1) + from.getName().replace("xlsx", "pdf");
                            }
                            PdfSaveOptions options = new PdfSaveOptions();
                            options.setCompliance(PdfCompliance.PDF_A_1_A);// Save the document in PDF format
                            workbook.save(new_url, options);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.pdf);
                        }

                    } else {
                        com.aspose.pdf.Document pdf = new com.aspose.pdf.Document(from.getAbsolutePath());

                        if (currentChangeType == CHANGE_TYPE_WORD) {
                            Toast.makeText(this, "pdf???????????????word", Toast.LENGTH_SHORT).show();
                            /*if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("pdf", "docx");
                            } else {
                                new_url = to.getAbsolutePath().replace(from.getName(), "") + from.getName().replace("pdf", "docx");
                            }
                            pdf.save(new_url, com.aspose.pdf.SaveFormat.Doc);*/

                        } else if (currentChangeType == CHANGE_TYPE_EXCEL) {

                            // Pdf???word
                            if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("pdf", "xlsx");
                            } else {
                                new_url = to.getAbsolutePath().substring(0,to.getAbsolutePath().lastIndexOf("/")+1) + from.getName().replace("pdf", "xlsx");
                            }
                            pdf.save(new_url, com.aspose.pdf.SaveFormat.Excel);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.word);
                        } else {
                            Toast.makeText(this, "pdf???????????????pdf", Toast.LENGTH_SHORT).show();
                        }
                    }

                    Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
                    to = new EssFile(new_url);
                    ((TextView) findViewById(R.id.to_name)).setText(to.getAbsolutePath());

                } catch (Exception e) {
                    Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                break;
            case R.id.choose_type:
                final String[] items = {"Word", "Excel", "PDF"};
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("????????????")//????????????????????????
                        .setSingleChoiceItems(items, -1, (dialog13, which) -> {
                            switch (which) {
                                case 0:
                                    currentChangeType = CHANGE_TYPE_WORD;
                                    ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.word);
                                    break;
                                case 1:
                                    currentChangeType = CHANGE_TYPE_EXCEL;
                                    ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.excel);
                                    break;
                                case 2:
                                    currentChangeType = CHANGE_TYPE_PDF;
                                    ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.pdf);
                                    break;
                            }
                            ((TextView) findViewById(R.id.choose_type)).setText(String.format("?????????????????????%s", items[which]));
                            dialog13.dismiss();
                        }).setNegativeButton("??????", (dialog12, which) -> dialog12.dismiss()).create();

                dialog.show();
                break;
            case R.id.from_delete:
                if (from == null || !from.getFile().delete()) {
                    Toast.makeText(this, "????????????????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
                }
                ((ImageView) findViewById(R.id.from_type_image)).setImageResource(android.R.color.transparent);
                ((TextView) findViewById(R.id.from_name)).setText("");
                from = null;
                break;
            case R.id.to_delete:
                if (to == null || !to.getFile().delete()) {
                    Toast.makeText(this, "????????????????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(this, "???????????????", Toast.LENGTH_SHORT).show();
                }
                ((ImageView) findViewById(R.id.to_type_image)).setImageResource(android.R.color.transparent);
                ((TextView) findViewById(R.id.to_name)).setText("");
                to = null;
                break;
            case R.id.scan:

                if (to == null) {
                    Toast.makeText(this, "???????????????????????????!", Toast.LENGTH_SHORT).show();
                    return;
                }

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
                startActivityForResult(intent, 99);

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FROM_CODE) {
            if (data != null) {
                ArrayList<EssFile> essFile = data.getParcelableArrayListExtra(Const.EXTRA_RESULT_SELECTION);

                from = essFile.get(0);
                ((TextView) findViewById(R.id.from_name)).setText(from.getName());

                String suffix = from.getAbsolutePath().split("\\.")[from.getAbsolutePath().split("\\.").length - 1];

                if (suffix.equals("doc")) {
                    ((ImageView) findViewById(R.id.from_type_image)).setImageResource(R.mipmap.word);
                } else if (suffix.equals("xlsx")) {
                    ((ImageView) findViewById(R.id.from_type_image)).setImageResource(R.mipmap.excel);
                } else {
                    ((ImageView) findViewById(R.id.from_type_image)).setImageResource(R.mipmap.pdf);
                }

            }
        } else if (requestCode == REQUEST_TO_CODE) {

            if (data != null) {
                ArrayList<String> essFile = data.getStringArrayListExtra(Const.EXTRA_RESULT_SELECTION);
                to = new EssFile(essFile.get(0));

                ((TextView) findViewById(R.id.to_name)).setText(to.getAbsolutePath());
                ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.folder);
            }
        } else if (requestCode == 99) {
            if (resultCode == RESULT_OK) {
                try {
                    ContentResolver contentResolver = getContentResolver();
                    Bitmap bitmap = compressScale(BitmapFactory.decodeStream(contentResolver.openInputStream(photoUri)));
                    Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
                    scanBitmap(this, bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void scanBitmap(Context activity, Bitmap bitmap) {

        Log.i(TAG, "scanBitmap: bitmap: width=" + bitmap.getWidth() + "   height=" + bitmap.getHeight());

        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("image", bitmapToBase64(bitmap)).build();
        Request request = new Request.Builder().url("https://aip.baidubce.com/rest/2.0/ocr/v1/doc_analysis_office?access_token=" + MyApplication.token).post(requestBody).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {

                    int a = bitmap.getWidth();
                    int b = 549;
                    double widthScale = Math.round(b * 100 / a) / 100.0;
                    ;

                    int c = bitmap.getHeight();
                    int d = 820;
                    double heightScale = Math.round(d * 100 / c) / 100.0;

                    JSONObject result = JSONObject.parseObject(response.body().string());
                    Log.d(TAG, "doPostAsync: " + result);
                    if (!result.containsKey("error_code")) {
                        String new_url;
                        if (to.isDirectory()) {
                            new_url = to.getAbsolutePath() + "????????????.pdf";
                        } else {
                            new_url = to.getAbsolutePath().replace(to.getName(), "") + "????????????.pdf";
                        }

                        String DEST2 = new_url;//????????????
                        PdfFont sysFont = null;//????????????
                        sysFont = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H", false);
                        PdfDocument pdfDoc = new PdfDocument(new com.itextpdf.kernel.pdf.PdfWriter(DEST2));
                        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc);//??????????????????
                        PageSize pageSize = new PageSize(bitmap.getHeight(), bitmap.getWidth());
                        doc.getPdfDocument().setDefaultPageSize(pageSize);
                        pdfDoc.addNewPage();
                        Log.i(TAG, "onResponse: " + bitmap.getWidth() + "    " + bitmap.getHeight());

                        JSONObject word, word_location;

                        try {
                            JSONArray words = result.getJSONArray("results");
                            for (int i = 0; i < words.size(); i++) {

                                word = words.getJSONObject(i).getJSONObject("words");
                                word_location = word.getJSONObject("words_location");

                                Log.i(TAG, "onResponse: " + word_location.getIntValue("left"));
                                Log.i(TAG, "onResponse: " + word_location.getIntValue("top"));
                                Log.i(TAG, "onResponse: " + word_location.getIntValue("width"));
                                Log.i(TAG, "onResponse: " + word.getString("word"));
                                Log.i(TAG, "onResponse: --------------------------------------- ");

                                Text text = new Text(word.getString("word"));
                                //????????????text??????????????????????????????????????????text?????????????????????????????????????????????  word_location.getIntValue("left") word_location.getIntValue("top")

                                doc.add(new Paragraph(text).setFont(sysFont).setFixedPosition(pdfDoc.getNumberOfPages(), word_location.getIntValue("left"), bitmap.getHeight() - word_location.getIntValue("top"), word_location.getIntValue("width")));

                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(activity, "???????????????", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        } finally {
                            doc.close();//???????????????
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(activity, "???????????????", Toast.LENGTH_SHORT).show();
                            to = new EssFile(new_url);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.pdf);
                            ((TextView) findViewById(R.id.to_name)).setText(new_url);
                        });


                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(activity, "??????????????????", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    /*
     * bitmap???base64
     * */
    private static String bitmapToBase64(Bitmap bitmap) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private Bitmap drawableToBitamp(Drawable drawable) {
        //?????????????????????bitmap
        Bitmap bitmap = null;
        //??????????????????
        int width = drawable.getIntrinsicWidth();
        //??????????????????
        int height = drawable.getIntrinsicHeight();
        //???????????????PixelFormat.OPAQUE????????????????????????RGB_565?????????????????????????????????????????????ARGB_8888???????????????????????????????????????
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        //??????????????????Bitmap
        bitmap = Bitmap.createBitmap(width, height, config);
        //???bitmap?????????????????????
        Canvas canvas = new Canvas(bitmap);
        //?????????????????????
        drawable.setBounds(0, 0, width, height);
        //???drawable?????????canvas???
        drawable.draw(canvas);
        return bitmap;
    }

    // ?????????

}
