package com.example.documentscanningandchange;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.scan).setOnClickListener(view -> toNewActivity(ScanActivity.class));
        findViewById(R.id.change).setOnClickListener(view -> toNewActivity(ChangeActivity.class));
        findViewById(R.id.translate).setOnClickListener(view -> toNewActivity(QCCodeActivity.class));
        findViewById(R.id.qrcode).setOnClickListener(view -> toNewActivity(TranslateActivity.class));
    }

    public void toNewActivity(Class clazz) {
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }

}
