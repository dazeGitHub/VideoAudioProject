package com.maniu.androidmutilvideo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;
import com.maniu.androidmutilvideo.player.MediaPlayerHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Surface mediaSurface;
    private CameraPreviewTextureView cameraPreviewTextureView;
//辅助工具类
    CameraStreamPublisher cameraStreamPublisher;
    private MediaPlayerHelper mediaPlayer = new MediaPlayerHelper();

    private Handler handler;
    private HandlerThread handlerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        cameraPreviewTextureView = findViewById(R.id.camera_produce_view);

        cameraStreamPublisher = new CameraStreamPublisher(cameraPreviewTextureView);
//设值监听 cameraPreviewTextureView 创建的时候
        cameraStreamPublisher.setOnSurfacesCreatedListener(new CameraStreamPublisher.OnSurfacesCreatedListener() {
            @Override
            public void onCreated(List<GLTexture> producedTextureList) {
//                producedTextureList    视频流 数据源   摄像头数据源码   producedTextureList[1]  包含了 视频画面的
//                能1  不能2
                GLTexture texture =producedTextureList.get(1);
//                包装surface  texture.getSurfaceTexture 是不是 屏幕        离屏缓冲
                mediaSurface = new Surface(texture.getSurfaceTexture());
            }
        });
        initCameraStreamPublisher();
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);

        }
        return false;
    }

    private void initCameraStreamPublisher() {
        handlerThread = new HandlerThread("StreamPublisherOpen");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {

                if ((mediaPlayer.isPlaying() || mediaPlayer.isLooping())) {
                    return;
                }
                mediaPlayer.playMedia(MainActivity.this, mediaSurface);
                StreamPublisherParam.Builder builder = new StreamPublisherParam.Builder();
                StreamPublisherParam streamPublisherParam = builder
                        .setWidth(1080)
                        .setHeight(750)
                        .setVideoBitRate(1500 * 1000)
                        .setFrameRate(30)
                        .setIframeInterval(1)
                        .setSamplingRate(44100)
                        .setAudioBitRate(19200)
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .createStreamPublisherParam();
                String  outputDir = new File(Environment.getExternalStorageDirectory(), "/test_mp4_encode.mp4").getAbsolutePath();
                streamPublisherParam.outputFilePath = outputDir;
                streamPublisherParam.setInitialTextureCount(2);
                cameraStreamPublisher.prepareEncoder(streamPublisherParam);
                try {
                    cameraStreamPublisher.startPublish();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        };






    }

    public void clickStartTest(View view) {
        TextView textView = (TextView) view;
//        子线程  如果是还没开始   我开始录制     已经开始  结束
        if (cameraStreamPublisher.isStart()) {
            cameraStreamPublisher.closeAll();
//            结束
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            textView.setText("START");
        }else
        {
            cameraStreamPublisher.resumeCamera();
            handler.sendEmptyMessage(1);
            textView.setText("STOP");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraStreamPublisher.resumeCamera();
    }
}