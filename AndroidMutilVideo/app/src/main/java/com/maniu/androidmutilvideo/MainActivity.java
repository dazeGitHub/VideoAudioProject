package com.maniu.androidmutilvideo;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.View;

import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.maniu.androidmutilvideo.player.MediaPlayerHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Surface mediaSurface;
    private CameraPreviewTextureView cameraPreviewTextureView;
//辅助工具类
    CameraStreamPublisher cameraStreamPublisher;
    private MediaPlayerHelper mediaPlayer = new MediaPlayerHelper();
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
                GLTexture texture = producedTextureList.get(1);
//                包装surface  texture.getSurfaceTexture 是不是 屏幕        离屏缓冲
                mediaSurface = new Surface(texture.getSurfaceTexture());
            }
        });

//        mediaPlayer.playMedia(this,mediaSurface);
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
    public void clickStartTest(View view) {
        if ((mediaPlayer.isPlaying() || mediaPlayer.isLooping())) {
            return;
        }

        mediaPlayer.playMedia(this, mediaSurface);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraStreamPublisher.resumeCamera();
    }
}