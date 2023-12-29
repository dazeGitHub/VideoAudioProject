package com.maniu.h264maniu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

        }
        return false;
    }
    H264Player h264Player;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        initSurface();
        //通过 MediaPlayer 看 dsp cpu gpu 如何协作, MediaPlayer 只能播放 mp4 和 3gp, 所以说傻瓜式播放器
//        MediaPlayer mediaPlayer = new MediaPlayer();
//        mediaPlayer.setSurface();
//        mediaPlayer.setDataSource();
//        mediaPlayer.start();
    }
//SurfaceView  画框   Surface 画布
//    画师   准备
    private void initSurface() {
        Log.e("TAG", Environment.getExternalStorageDirectory().getAbsolutePath());
        SurfaceView surface = (SurfaceView) findViewById(R.id.preview);
        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Surface surface1 = surfaceHolder.getSurface();
                //绘制是通过 Surface (画布), 控件是 SurfaceView (画框)
                h264Player = new H264Player(
                        new File(Environment.getExternalStorageDirectory(), "splice1.h2642")
                                .getAbsolutePath() //splice1.h2642  test.h264  one.h264  two.h264
                        ,surface1);
                h264Player.play();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

            }
        });

    }
}