package com.example.documentscanningandchange;

import android.app.Application;
import android.util.Log;

import com.maxvision.tbs.TbsUtils;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsDownloader;
import com.tencent.smtt.sdk.TbsListener;

public class MyApplication extends Application {

    public static String token;

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化tbs，默认标题是返回
        TbsUtils.init(this, "");
        boolean need = TbsDownloader.needDownload(MyApplication.this, false);
        if (need) {
            //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
            QbSdk.setDownloadWithoutWifi(true);
            QbSdk.setTbsListener(new TbsListener() {
                @Override
                public void onDownloadFinish(int progress) {
                    Log.d("QbSdk", "onDownloadFinish -->下载X5内核完成：" + progress);
                    //若是progress ==100 的情况下才表示 内核加载成功， 否则重新 加载
                    if (progress != 100) {
                        TbsDownloader.startDownload(MyApplication.this);
                    }
                }

                @Override
                public void onInstallFinish(int progress) {
                    Log.d("QbSdk", "onInstallFinish -->安装X5内核进度：" + progress);

                }

                @Override
                public void onDownloadProgress(int progress) {
                    Log.d("QbSdk", "onDownloadProgress -->下载X5内核进度：" + progress);
                }
            });


            QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

                @Override
                public void onViewInitFinished(boolean arg0) {
                    // TODO Auto-generated method stub
                    //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                    Log.d("QbSdk", " x5 onViewInitFinished is " + arg0);
                }

                @Override
                public void onCoreInitFinished() {
                    Log.d("QbSdk", " x5 内核加载成功 ");
                    Log.d("QbSdk", " x5 内核版本号:" + QbSdk.getTbsVersion(MyApplication.this));
                }
            };
            //x5内核初始化接口
            QbSdk.initX5Environment(MyApplication.this, cb);
        }

    }
}
