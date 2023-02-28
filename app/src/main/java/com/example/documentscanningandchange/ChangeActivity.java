package com.example.documentscanningandchange;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aspose.pdf.Document;
import com.aspose.pdf.Image;
import com.aspose.pdf.Page;
import com.aspose.pdf.Rectangle;
import com.bumptech.glide.Glide;
import com.zlylib.fileselectorlib.FileSelector;
import com.zlylib.fileselectorlib.bean.EssFile;
import com.zlylib.fileselectorlib.utils.Const;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class ChangeActivity extends AppCompatActivity {

    private ArrayList<String> list = new ArrayList<>();
    private EssFile to;
    private final int REQUEST_FROM_CODE = 1;
    private final int REQUEST_TO_CODE = 2;


    private RecyclerView recyclerView;

    private adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);
        findViewById(R.id.from_select).setOnClickListener(view -> FileSelector.from(ChangeActivity.this).setMaxCount(9) // 只能选择一个
                .setFileTypes("jpg", "png") //设置文件类型
                .setSortType(FileSelector.BY_NAME_ASC) //设置名字排序
                .requestCode(REQUEST_FROM_CODE) //设置返回码
                .setTargetPath("/storage/emulated/0/") //设置默认目录
                .start());
        findViewById(R.id.from_preview).setOnClickListener(view -> {
            FileSelector.from(this).setMaxCount(1).onlyShowFolder().onlySelectFolder() // 只能选择一个
                    .setSortType(FileSelector.BY_NAME_ASC) //设置名字排序
                    .requestCode(REQUEST_TO_CODE) //设置返回码
                    .setTargetPath("/storage/emulated/0/") //设置默认目录
                    .start();
        });
        findViewById(R.id.from_change).setOnClickListener(view -> {

            if (list.size() > 0) {

                // Instantiate Document Object
                Document doc = new Document();
                Toast.makeText(this, "转换中", Toast.LENGTH_SHORT).show();

                list.forEach(item -> {
                    try {
                        // Add a page to pages collection of document
                        Page page = doc.getPages().add();

                        // Load the source image file to Stream object
                        java.io.FileInputStream fs = null;

                        fs = new java.io.FileInputStream(item);


                        // Set margins so image will fit, etc.
                        page.getPageInfo().getMargin().setBottom(0);
                        page.getPageInfo().getMargin().setTop(0);
                        page.getPageInfo().getMargin().setLeft(0);
                        page.getPageInfo().getMargin().setRight(0);
                        page.setCropBox(new Rectangle(0, 0, 400, 400));

                        // Create an image object
                        Image image1 = new Image();

                        // Add the image into paragraphs collection of the section
                        page.getParagraphs().add(image1);
                        // Set the image file stream
                        image1.setImageStream(fs);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });

                // Save resultant PDF file
                doc.save(to.getAbsolutePath() + "ImageList.pdf");
                Toast.makeText(ChangeActivity.this, "转换完成！", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "未选中图片", Toast.LENGTH_SHORT).show();
            }

        });

        findViewById(R.id.from_delete).setOnClickListener(view -> {
            if (list.size() > 0) {
                list.forEach(item -> {
                    File file = new File(item);
                    file.delete();
                });
                list.clear();
                adapter.notifyDataSetChanged();
            }
        });

        adapter = new adapter();
        recyclerView = findViewById(R.id.b);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FROM_CODE) {
            if (data != null) {
                list = data.getStringArrayListExtra(Const.EXTRA_RESULT_SELECTION);
                adapter.notifyDataSetChanged();
            }
        } else if (requestCode == REQUEST_TO_CODE) {

            if (data != null) {
                ArrayList<String> essFile = data.getStringArrayListExtra(Const.EXTRA_RESULT_SELECTION);
                to = new EssFile(essFile.get(0));
                ((TextView) findViewById(R.id.from_name)).setText(to.getAbsolutePath());
                ((ImageView) findViewById(R.id.from_type_image)).setImageResource(R.mipmap.folder);
            }
        }
    }

    class adapter extends RecyclerView.Adapter<viewHolder> {

        @NonNull
        @Override
        public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new viewHolder(LayoutInflater.from(ChangeActivity.this).inflate(R.layout.item_recycle, null, false));
        }

        @Override
        public void onBindViewHolder(@NonNull viewHolder holder, int position) {
            Glide.with(ChangeActivity.this).load(list.get(position)).into(holder.view);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    class viewHolder extends RecyclerView.ViewHolder {

        ImageView view;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView.findViewById(R.id.item);
        }
    }

}