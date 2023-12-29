package com.maniu.douyinclip;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.VideoView;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    VideoView videoView;
//    RangeSeekBar rangeSeekBar;
    SeekBar musicSeekBar;
    SeekBar voiceSeekBar;
    int musicVolume=0;
    int voiceVolume=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission(this);
        videoView = findViewById(R.id.videoView);
//        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        musicSeekBar = findViewById(R.id.musicSeekBar);
        voiceSeekBar = findViewById(R.id.voiceSeekBar);
        musicSeekBar.setMax(100);
        voiceSeekBar.setMax(100);
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                musicVolume = progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        voiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                voiceVolume = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    Runnable runnable;
    int duration = 0;
    @Override
    protected void onResume() {
        super.onResume();
        //在线程中拷贝
        new Thread(){
            @Override
            public void run() {

                final String aacPath = new File(Environment.getExternalStorageDirectory(), "music.mp3").getAbsolutePath();
                final String videoPath = new File(Environment.getExternalStorageDirectory(), "input.mp4").getAbsolutePath();
                try {
                    copyAssets("music.mp3", aacPath); //将 Assets 中的 music.mp3 拷贝到 aacPath
                    copyAssets("input.mp4", videoPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        startPlay(new File(Environment.getExternalStorageDirectory(), "input.mp4").getAbsolutePath());
    }
    private void startPlay(String path) {
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.height = 675;
        layoutParams.width = 1285;
        videoView.setLayoutParams(layoutParams);
        videoView.setVideoPath(path);

        videoView.start();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                duration = mp.getDuration() / 1000;
                mp.setLooping(true);
//                rangeSeekBar.setRange(0, duration);
//                rangeSeekBar.setValue(0, duration);
//                rangeSeekBar.setEnabled(true);
//                rangeSeekBar.requestLayout();
//                rangeSeekBar.setOnRangeChangedListener(new RangeSeekBar.OnRangeChangedListener() {
//                    @Override
//                    public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
//                        videoView.seekTo((int) min * 1000);
//                    }
//                });
                final Handler handler = new Handler();
                runnable = new Runnable() {
                    @Override
                    public void run() {
//                        if (videoView.getCurrentPosition() >= rangeSeekBar.getCurrentRange()[1] * 1000) {
//                            videoView.seekTo((int) rangeSeekBar.getCurrentRange()[0] * 1000);
//                        }
                        handler.postDelayed(runnable, 1000);
                    }
                };
                handler.postDelayed(runnable, 1000);
            }
        });
    }
    public static boolean checkPermission(
            Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

        }
        return false;
    }

    private void copyAssets(String assetsName, String path) throws IOException {
        AssetFileDescriptor assetFileDescriptor = getAssets().openFd(assetsName);
        FileChannel from = new FileInputStream(assetFileDescriptor.getFileDescriptor()).getChannel();
        FileChannel to = new FileOutputStream(path).getChannel();
        from.transferTo(assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength(), to);
    }
    
    public void music(View view) {
//      大片制作的时候(Seekbar 选取视频片段时), 需要如下内容 :
//      剪辑的起始的时间  终止时间, 视频调整后的音乐大小, 原生大小(视频调整前的音乐大小)
//      rangeSeekBar.getCurrentRange()[0];  //起始时间
//      rangeSeekBar.getCurrentRange()[1];  //终止时间

        File cacheDir =  Environment.getExternalStorageDirectory();
        final File videoFile = new File(cacheDir, "input.mp4");
        final File audioFile = new File(cacheDir, "music.mp3");

//      剪辑好的视频输出放哪里
        final File outputFile = new File(cacheDir, "output.mp4");

//      视频处理比较耗时, 放到子线程里
        new Thread(){
            @Override
            public void run() {
                try {
                    MusicProcess.mixAudioTrack(
                            videoFile.getAbsolutePath(),
                            audioFile.getAbsolutePath(),
                            outputFile.getAbsolutePath(),
                            //这里的单位是微秒, startTimeUs = 100秒, endTimeUs = 120 秒
                            (int) (100 * 1000 * 1000), //rangeSeekBar.getCurrentRange()[0] * 1000 * 1000
                            (int) (120 * 1000 * 1000), //rangeSeekBar.getCurrentRange()[1] * 1000 * 1000
                            voiceVolume,
                            musicVolume
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}