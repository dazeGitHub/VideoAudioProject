package com.maniu.webrtcmaniua;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SocketLive.SocketCallback   {
    LocalSurfaceView localSurfaceView;
    Surface surface;
    DecodecPlayerLiveH264 decodecPlayerLiveH264;
    SurfaceView removeSurfaceView;
    AudioRecorderLive audioRecoderLive;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        checkPermission();
    }
    private void initView() {
        localSurfaceView = findViewById(R.id.localSurfaceView);
        removeSurfaceView = findViewById(R.id.removeSurfaceView);
        removeSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                decodecPlayerLiveH264 = new DecodecPlayerLiveH264();
                decodecPlayerLiveH264.initDecoder(surface);
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });

    }
    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
            }, 1);

        }
        return false;
    }
//音视频  p2p     回音消除
    public void connect(View view) {
        localSurfaceView.startCaptrue(this);
        audioRecoderLive = new AudioRecorderLive(localSurfaceView.getSocketLive());
        audioRecoderLive.initPlay();
        audioRecoderLive.startRecord(this);
    }
//源源不断被网络调用的
    @Override
    public void callBack(byte[] data) {
        if (data[0] == 1) {//如果是视频
            if (decodecPlayerLiveH264 != null) {
                decodecPlayerLiveH264.callBack(data);
            }
        }else {
            if (audioRecoderLive != null) {
                audioRecoderLive.doPlay(data);
            }
        }
    }
}