package com.example.documentscanningandchange;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.maxvision.tbs.TbsUtils;
import com.zlylib.fileselectorlib.FileSelector;
import com.zlylib.fileselectorlib.bean.EssFile;
import com.zlylib.fileselectorlib.utils.Const;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
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
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.from_select:
                FileSelector.from(this).setMaxCount(1) // 只能选择一个
                        .setFileTypes("pdf", "doc", "docx", "xlsx") //设置文件类型
                        .setSortType(FileSelector.BY_NAME_ASC) //设置名字排序
                        .requestCode(REQUEST_FROM_CODE) //设置返回码
                        .setTargetPath("/storage/emulated/0/") //设置默认目录
                        .start();
                break;
            case R.id.from_preview:
                if (from == null) {
                    Toast.makeText(this, "还未选择目标文件!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!from.isExits()) {
                    Toast.makeText(this, "目标文件目前不存在!", Toast.LENGTH_SHORT).show();
                }
                if (from.isDirectory()) {
                    Toast.makeText(this, "无法预览文件夹!", Toast.LENGTH_SHORT).show();
                }

                //需要内容使用，filePath为路径比如：/data/user/0/...../files/TestDoc.doc
                TbsUtils.loadFileType(this, from.getAbsolutePath(), from.getName());
                break;
            case R.id.to_select:
                FileSelector.from(this).setMaxCount(1).onlyShowFolder().onlySelectFolder() // 只能选择一个
                        .setSortType(FileSelector.BY_NAME_ASC) //设置名字排序
                        .requestCode(REQUEST_TO_CODE) //设置返回码
                        .setTargetPath("/storage/emulated/0/") //设置默认目录
                        .start();
                break;
            case R.id.to_preview:

                if (to == null) {
                    Toast.makeText(this, "还未选择目标文件!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!to.isExits()) {
                    Toast.makeText(this, "目标文件目前不存在!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (to.isDirectory()) {
                    Toast.makeText(this, "无法预览文件夹!", Toast.LENGTH_SHORT).show();
                    return;
                }
                TbsUtils.loadFileType(this, to.getAbsolutePath(), to.getName());
                break;
            case R.id.change:
                if (to == null) {
                    Toast.makeText(this, "还未选择目标文件夹!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (from == null) {
                    Toast.makeText(this, "还未选择目标文件!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (currentChangeType != CHANGE_TYPE_PDF && currentChangeType != CHANGE_TYPE_EXCEL && currentChangeType != CHANGE_TYPE_WORD) {
                    Toast.makeText(this, "请选择转换类型", Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(this, "转换中...请稍等", Toast.LENGTH_SHORT).show();
                String suffix = from.getAbsolutePath().split("\\.")[from.getAbsolutePath().split("\\.").length - 1];
                try {
                    String new_url = "";
                    if (suffix.equals("doc") || suffix.equals("docx")) {

                        Document document = new Document(from.getAbsolutePath());
                        if (currentChangeType == CHANGE_TYPE_PDF) {
                            // word 转pdf
                            if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("docx", "pdf");
                            } else {
                                new_url = to.getAbsolutePath().replace(from.getName(), "") + from.getName().replace("docx", "pdf");
                            }
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.word);
                            document.save(new_url, SaveFormat.PDF);
                        } else if (currentChangeType == CHANGE_TYPE_EXCEL) {
                            Toast.makeText(this, "word无法转换为excel", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "word无法转换为word", Toast.LENGTH_SHORT).show();
                        }

                    } else if (suffix.equals("xlsx")) {
                        Workbook workbook = new Workbook(from.getAbsolutePath());// Save the document in PDF format
                        if (currentChangeType == CHANGE_TYPE_WORD) {
                            // Excel转word
                            if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("xlsx", "doc");
                            } else {
                                new_url = to.getAbsolutePath().replace(from.getName(), "") + from.getName().replace("xlsx", "doc");
                            }
                            workbook.save(new_url, com.aspose.cells.SaveFormat.DOCX);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.word);
                        } else if (currentChangeType == CHANGE_TYPE_EXCEL) {
                            Toast.makeText(this, "excel无法转换为excel", Toast.LENGTH_SHORT).show();
                        } else {
                            // Excel转Pdf
                            if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("xlsx", "pdf");
                            } else {
                                new_url = to.getAbsolutePath().replace(from.getName(), "") + from.getName().replace("xlsx", "pdf");
                            }
                            PdfSaveOptions options = new PdfSaveOptions();
                            options.setCompliance(PdfCompliance.PDF_A_1_A);// Save the document in PDF format
                            workbook.save(new_url, options);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.pdf);
                        }

                    } else {
                        com.aspose.pdf.Document pdf = new com.aspose.pdf.Document(from.getAbsolutePath());

                        if (currentChangeType == CHANGE_TYPE_WORD) {
                            Toast.makeText(this, "pdf无法转换为word", Toast.LENGTH_SHORT).show();
                          /*  if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("pdf", "docx");
                            } else {
                                new_url = to.getAbsolutePath().replace(from.getName(), "") + from.getName().replace("pdf", "docx");
                            }
                            pdf.save(new_url, com.aspose.pdf.SaveFormat.Doc);*/

                        } else if (currentChangeType == CHANGE_TYPE_EXCEL) {

                            // Pdf转word
                            if (to.isDirectory()) {
                                new_url = to.getAbsolutePath() + from.getName().replace("pdf", "xlsx");
                            } else {
                                new_url = to.getAbsolutePath().replace(from.getName(), "") + from.getName().replace("pdf", "xlsx");
                            }
                            pdf.save(new_url, com.aspose.pdf.SaveFormat.Excel);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.word);
                        } else {
                            Toast.makeText(this, "pdf无法转换为pdf", Toast.LENGTH_SHORT).show();
                        }
                    }

                    Toast.makeText(this, "转换完成", Toast.LENGTH_SHORT).show();
                    to = new EssFile(new_url);
                    ((TextView) findViewById(R.id.to_name)).setText(to.getAbsolutePath());

                } catch (Exception e) {
                    Toast.makeText(this, "转换失败", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                break;
            case R.id.choose_type:
                final String[] items = {"Word", "Excel", "PDF"};
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("转换类型")//设置对话框的标题
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
                            ((TextView) findViewById(R.id.choose_type)).setText(String.format("当前转换类型：%s", items[which]));
                            dialog13.dismiss();
                        }).setNegativeButton("取消", (dialog12, which) -> dialog12.dismiss()).create();

                dialog.show();
                break;
            case R.id.from_delete:
                if (from == null || !from.getFile().delete()) {
                    Toast.makeText(this, "删除的目标为空或不为文件!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(this, "删除成功！", Toast.LENGTH_SHORT).show();
                }
                ((ImageView) findViewById(R.id.from_type_image)).setImageResource(android.R.color.transparent);
                ((TextView) findViewById(R.id.from_name)).setText("");
                from = null;
                break;
            case R.id.to_delete:
                if (to == null || !to.getFile().delete()) {
                    Toast.makeText(this, "删除的目标为空或不为文件!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(this, "删除成功！", Toast.LENGTH_SHORT).show();
                }
                ((ImageView) findViewById(R.id.to_type_image)).setImageResource(android.R.color.transparent);
                ((TextView) findViewById(R.id.to_name)).setText("");
                to = null;
                break;
            case R.id.scan:

                if (to == null) {
                    Toast.makeText(this, "还未选择目标文件夹!", Toast.LENGTH_SHORT).show();
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
                    Bitmap bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(photoUri));
                    Toast.makeText(this, "转换中，请稍等", Toast.LENGTH_SHORT).show();
                    scanBitmap(this, bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }


        }
    }

    public void scanBitmap(Context activity, Bitmap bitmap) {
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

                    JSONObject result = JSONObject.parseObject(response.body().string());
                    if (!result.containsKey("error_code")) {
                        String new_url;
                        Log.d(TAG, "doPostAsync: " + result);
                        if (to.isDirectory()) {
                            new_url = to.getAbsolutePath() + "扫描结果.pdf";
                        } else {
                            new_url = to.getAbsolutePath().replace(to.getName(), "") + "扫描结果.pdf";
                        }

                        String DEST2 = new_url;//文件路径
                        PdfFont sysFont = null;//中文设置
                        sysFont = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H", false);
                        PdfDocument pdfDoc = new PdfDocument(new com.itextpdf.kernel.pdf.PdfWriter(DEST2));
                        com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc);//构建文档对象
                        PageSize pageSize = new PageSize(bitmap.getHeight(), bitmap.getWidth());
                        pdfDoc.addNewPage();
                        Log.i(TAG, "onResponse: " + bitmap.getWidth() + "    " + bitmap.getHeight());

                        doc.getPdfDocument().setDefaultPageSize(pageSize);
                       /* PdfPage page = pdfDoc.addNewPage();
                        PdfCanvas pdfCanvas = new PdfCanvas(page);
                        Rectangle[] columns = {new Rectangle(6, 650, 100, 30)};  //几个Rectangle对应几个位置
                        pdfCanvas.rectangle(columns[0]);
                        Canvas canvas = new Canvas(pdfCanvas, pdfDoc, columns[0]);
                        Paragraph p = new Paragraph("hssssas").setBold();   //Bold为设置粗体
                        canvas.add(p);*/

                        Text text = new Text(String.format("Page %d", pdfDoc.getNumberOfPages() - 1));
                        //前面这个text主要是设置背景色为白色，如果text的位置上面有内容就会覆盖掉内容
                        doc.add(new Paragraph(text).setFixedPosition(
                                pdfDoc.getNumberOfPages(), 549, 742, 100)); //这里面width取决于留空的宽度，这里我们尽量取大一点

                        doc.add(new Paragraph(text).setFixedPosition(
                                pdfDoc.getNumberOfPages(), 449, 642, 100)); //这里面width取决于留空的宽度，这里我们尽量取大一点

                        /*Paragraph paragraph = new Paragraph(""); //段落方法
                        paragraph.setFont(sysFont);//自定义中文
                        doc.add(paragraph);//段落添加到文档中*/

                        JSONObject word, word_location;

                        try {
                            /*JSONArray words = result.getJSONArray("results");
                            for (int i = 0; i < 1; i++) {

                                word = words.getJSONObject(i).getJSONObject("words");
                                word_location = word.getJSONObject("words_location");

                                Log.i(TAG, "onResponse: " + word_location.getIntValue("left"));
                                Log.i(TAG, "onResponse: " + word_location.getIntValue("top"));
                                Log.i(TAG, "onResponse: " + word_location.getIntValue("width"));
                                Log.i(TAG, "onResponse: " + word.getString("word"));
                                Log.i(TAG, "onResponse: --------------------------------------- ");

                                Text text = new Text(word.getString("word"));
                                //前面这个text主要是设置背景色为白色，如果text的位置上面有内容就会覆盖掉内容  word_location.getIntValue("left") word_location.getIntValue("top")

                                doc.add(new Paragraph(text).setFont(sysFont).setFixedPosition(pdfDoc.getNumberOfPages(), word_location.getIntValue("left") >> 2, word_location.getIntValue("left") >> 2, word_location.getIntValue("width")));

                            }*/

                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(activity, "扫描失败！", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        } finally {
                            doc.close();//关闭文档流
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(activity, "扫描完成！", Toast.LENGTH_SHORT).show();
                            to = new EssFile(new_url);
                            ((ImageView) findViewById(R.id.to_type_image)).setImageResource(R.mipmap.pdf);
                            ((TextView) findViewById(R.id.to_name)).setText(new_url);
                        });


                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(activity, "图片不清晰！", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    /*
     * bitmap转base64
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

}
