package com.example.documentscanningandchange;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @description:
 * @author: admin
 * @date: 2023/1/17
 * @email: 1145338587@qq.com
 */
public class SoundRecordingButton extends AppCompatImageButton {

    private static final String TAG = "SoundRecordingButton";

    private final Context context;

    /**
     * 录音数队列
     */
    private ConcurrentLinkedQueue<byte[]> audioQueue = new ConcurrentLinkedQueue<>();
    private ThreadPoolExecutor mExecutor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private AudioTrack mAudioTrack;
    private AudioRecord mAudioRecord;
    private int mRecorderBufferSize;
    private byte[] mAudioData;

    /*默认数据*/
    private final int mSampleRateInHZ = 8000; //采样率
    private final int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;  //位数
    private final int mChannelConfig = AudioFormat.CHANNEL_IN_MONO;   //声道

    private long event_down_time = 0;

    private boolean isRecording = false;
    private String mTmpFileAbs = "";

    private VideoFastReplyBean videoFastReplyBean;

    private boolean isPlaying = false;

    private MediaPlayer player = new MediaPlayer();

    // 监听
    private onSoundRecordingFinishListener onSoundRecordingFinishListener;
    private onPlayCompleteListener onPlayCompleteListener;

    public SoundRecordingButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initData();
    }

    private void initData() {
        mRecorderBufferSize = AudioRecord.getMinBufferSize(mSampleRateInHZ, mChannelConfig, mAudioFormat);
        mAudioData = new byte[320];
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && getVisibility() != GONE) {
            Toast.makeText(context, "权限申请失败", Toast.LENGTH_SHORT).show();
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mSampleRateInHZ, mChannelConfig, mAudioFormat, mRecorderBufferSize);
        // mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRateInHZ, mChannelConfig, mAudioFormat, mRecorderBufferSize * 2
        // , AudioTrack.MODE_STREAM);

        videoFastReplyBean = new VideoFastReplyBean();

        player.setOnCompletionListener(mediaPlayer -> {
            onPlayCompleteListener.onPlayFinish(videoFastReplyBean);
        });
    }

    public VideoFastReplyBean getVideoFastReplyBean() {
        return videoFastReplyBean;
    }

    public void setOnSoundRecordingFinishListener(onSoundRecordingFinishListener onSoundRecordingFinishListener) {
        this.onSoundRecordingFinishListener = onSoundRecordingFinishListener;
    }

    void onClickStart() {
        if (isRecording) {
            return;
        }

        Toast.makeText(context, "开始录制！", Toast.LENGTH_SHORT).show();

        setImageResource(R.mipmap.ic_perfect_playing);

        event_down_time = System.currentTimeMillis();

        String tmpName = "recording";
        final File tmpFile = createFile(tmpName + ".pcm");
        final File tmpOutFile = createFile(tmpName + ".wav");
        videoFastReplyBean.path = tmpOutFile.getAbsolutePath();

        isRecording = true;
        mAudioRecord.startRecording();
        mExecutor.execute(() -> {
            try {
                FileOutputStream outputStream = new FileOutputStream(tmpFile.getAbsoluteFile());
                while (isRecording) {
                    int readSize = mAudioRecord.read(mAudioData, 0, mAudioData.length);
                    outputStream.write(mAudioData);
                }
                outputStream.close();
                pcmToWave(tmpFile.getAbsolutePath(), tmpOutFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void onClickEnd() {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        videoFastReplyBean.duration = (System.currentTimeMillis() - event_down_time) / 1000;
        mAudioRecord.stop();
        onSoundRecordingFinishListener.soundRecordingFinish(videoFastReplyBean);

    }

    private void pcmToWave(String inFileName, String outFileName) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen = 0;
        long longSampleRate = mSampleRateInHZ;
        long totalDataLen = totalAudioLen + 36;
        int channels = 1;//你录制是单声道就是1 双声道就是2（如果错了声音可能会急促等）
        long byteRate = 16 * longSampleRate * channels / 8;

        byte[] data = new byte[mRecorderBufferSize];

        try {
            in = new FileInputStream(inFileName);
            out = new FileOutputStream(outFileName);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            player.setDataSource(videoFastReplyBean.path);
            player.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            isPlaying = false;
            onPlayCompleteListener.onPlayError(videoFastReplyBean);
            // 重置
            player.reset();
        }

    }

    /**
     * 播放、暂停
     */
    public void play() {
        if (!TextUtils.isEmpty(videoFastReplyBean.path)) {
            if (isPlaying) {
                // 暂停
                player.pause();
            } else {
                // 播放
                player.start();
            }
            isPlaying = !isPlaying;
        }
    }

    /**
     * 重置
     */
    public void reset() {
        setImageDrawable(ContextCompat.getDrawable(context, R.mipmap.ic_perfect_start));
        player.stop();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setOnPlayErrorListener(SoundRecordingButton.onPlayCompleteListener onPlayErrorListener) {
        this.onPlayCompleteListener = onPlayErrorListener;
    }

    private File createFile(String name) {
        File fileEx = context.getExternalFilesDir(null);
        String dirPath = fileEx.getAbsolutePath() + "/AudioRecord/";
        Log.i(TAG, "createFile: " + dirPath + "/" + name);
        //String dirPath = Environment.getExternalStorageDirectory().getPath()
        return createFile(dirPath, name);
    }

    /**
     * 创建文件夹---之所以要一层层创建，是因为一次性创建多层文件夹可能会失败！
     */
    public static boolean createFileDir(File dirFile) {
        if (dirFile == null) return true;
        if (dirFile.exists()) {
            return true;
        }
        File parentFile = dirFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            //父文件夹不存在，则先创建父文件夹，再创建自身文件夹
            return createFileDir(parentFile) && createFileDir(dirFile);
        } else {
            boolean mkdirs = dirFile.mkdirs();
            boolean isSuccess = mkdirs || dirFile.exists();
            if (!isSuccess) {
                Log.e("FileUtil", "createFileDir fail " + dirFile);
            }
            return isSuccess;
        }
    }

    public static File createFile(String dirPath, String fileName) {
        try {
            File dirFile = new File(dirPath);
            if (!dirFile.exists()) {
                if (!createFileDir(dirFile)) {
                    Log.e(TAG, "createFile dirFile.mkdirs fail");
                    return null;
                }
            } else if (!dirFile.isDirectory()) {
                boolean delete = dirFile.delete();
                if (delete) {
                    return createFile(dirPath, fileName);
                } else {
                    Log.e(TAG, "createFile dirFile !isDirectory and delete fail");
                    return null;
                }
            }
            File file = new File(dirPath, fileName);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Log.e(TAG, "createFile createNewFile fail");
                    return null;
                }
            }
            return file;
        } catch (Exception e) {
            Log.e(TAG, "createFile fail :" + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /*
    任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
    FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的，
     */
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (1 * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    private void showToast(String content) {
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onClickStart();
                break;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP:
                onClickEnd();
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @FunctionalInterface
    public interface onSoundRecordingFinishListener {
        void soundRecordingFinish(VideoFastReplyBean videoFastReplyBean);
    }

    public interface onPlayCompleteListener {
        void onPlayError(VideoFastReplyBean videoFastReplyBean);

        void onPlayFinish(VideoFastReplyBean videoFastReplyBean);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isRecording){
            mAudioRecord.stop();
        }
        mAudioRecord.release();
        player.stop();
        player.release();
        player = null;
    }
}
