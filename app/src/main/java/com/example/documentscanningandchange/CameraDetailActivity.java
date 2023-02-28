package com.example.documentscanningandchange;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * @description:
 * @author: admin
 * @date: 2023/2/24
 * @email: 1145338587@qq.com
 */
public class CameraDetailActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";

    private TextureView textureView;
    private HandlerThread handlerThread;
    private Handler mCameraHandler;
    private CameraManager cameraManager;
    //最佳的预览尺寸
    private Size previewSize;
    //最佳的拍照尺寸
    private Size mCaptureSize;
    private String mCameraId;

    private CameraDevice cameraDevice;

    private CaptureRequest.Builder captureRequestBuilder;

    private CaptureRequest captureRequest;

    private CameraCaptureSession mCameraCaptureSession;

    private Button btn_photo;

    private ImageReader imageReader;

    private static final SparseArray ORIENTATION = new SparseArray();


    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_demo);
        textureView = findViewById(R.id.textureView);
        btn_photo = findViewById(R.id.btn_photo);
        btn_photo.setOnClickListener(OnClick);
    }

    private final View.OnClickListener OnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //获取摄像头的请求
            try {
                CaptureRequest.Builder cameraDeviceCaptureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                cameraDeviceCaptureRequest.addTarget(imageReader.getSurface());
                //获取摄像头的方向
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        Toast.makeText(CameraDetailActivity.this, "拍照结束，相片已保存！", Toast.LENGTH_SHORT).show();
                        unLockFocus();
                    }
                };
                //设置拍照方向
                cameraDeviceCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, (Integer) ORIENTATION.get(rotation));
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.capture(cameraDeviceCaptureRequest.build(), mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            //获取图像的缓冲区
            //获取文件的存储权限及操作
        }
    };

    private void unLockFocus() {
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startCameraThread();
        if (!textureView.isAvailable()) {
            textureView.setSurfaceTextureListener(mTextureListener);
        } else {
            startPreview();
        }
    }

    TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            //SurfaceTexture组件可用的时候,设置相机参数，并打开摄像头
            //设置摄像头参数
            setUpCamera(width, height);
            //打开摄像头
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            //尺寸发生变化的时候
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            //组件被销毁的时候
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            //组件更新的时候
        }
    };

    private void setUpCamera(int width, int height) {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //拿到摄像头的id
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                //得到摄像头的参数
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map != null) { //找到摄像头能够输出的，最符合我们当前屏幕能显示的最小分辨率
                    previewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                        @Override
                        public int compare(Size o1, Size o2) {
                            return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                        }
                    });
                }
                //建立ImageReader准备存储照片
                setUpImageReader();
                mCameraId = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    private void setUpImageReader() {
        imageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraHandler.post(new ImageSaver(reader.acquireNextImage()));
            }

        }, mCameraHandler);
    }

    //存储图片的过程
    private class ImageSaver implements Runnable {
        private Image image;

        public ImageSaver(Image image) {
            this.image = image;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[byteBuffer.remaining()];
            byteBuffer.get(data);
//            String path =getFilesDir();
            String path = Environment.getExternalStorageDirectory() + "/DCIM/CameraV2/";
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String fileName = timeStamp + "IMG_" + timeStamp + ".jpg";
            File file = new File(path);

            //判断当前的文件目录是否存在，如果不存在就创建这个文件目录
            if (!file.exists()) {
                file.mkdir();
            }

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(fileName);
                fileOutputStream.write(data, 0, data.length);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //得到最佳的预览尺寸
    private Size getOptimalSize(Size[] outputSizes, int width, int height) {
        ArrayList<Size> arrayList = new ArrayList<>();
        for (Size option : outputSizes) {
            if (width > height) { //横屏
                if (option.getWidth() > width && option.getHeight() > height) {
                    arrayList.add(option);
                }
            } else { //竖屏
                if (option.getWidth() > height && option.getHeight() > width) {
                    arrayList.add(option);
                }
            }
        }
        if (arrayList.size() > 1) {
            return Collections.min(arrayList, new Comparator<Size>() {
                @Override
                public int compare(Size o1, Size o2) {
                    return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
                }
            });

        }
        return outputSizes[0];
    }

    private void openCamera() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }
        try {
            cameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) { //摄像头打开
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) { //摄像头关闭
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {//摄像头出现错误
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    //开始预览
    private void startPreview() {
        //建立图像缓冲区
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

        //得到界面的显示对象
        Surface surface = new Surface(surfaceTexture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            //建立通道(CaptureRequest和CaptureSession会话)
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureRequest = captureRequestBuilder.build();
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(captureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }

    //开启摄像头线程
    private void startCameraThread() {
        handlerThread = new HandlerThread("myHandlerThread");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
    }
}
