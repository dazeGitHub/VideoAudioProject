package com.maniu.wangyimusicplayer;

import static com.maniu.wangyimusicplayer.musicui.widget.DiscView.DURATION_NEEDLE_ANIAMTOR;
import static com.maniu.wangyimusicplayer.service.MusicService.ACTION_OPT_MUSIC_VOLUME;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.maniu.wangyimusicplayer.lisnter.IPlayerListener;
import com.maniu.wangyimusicplayer.lisnter.MNOnParparedListener;
import com.maniu.wangyimusicplayer.musicui.model.MusicData;
import com.maniu.wangyimusicplayer.musicui.utils.DisplayUtil;
import com.maniu.wangyimusicplayer.musicui.widget.BackgourndAnimationRelativeLayout;
import com.maniu.wangyimusicplayer.musicui.widget.DiscView;
import com.maniu.wangyimusicplayer.opengl.MNGLSurfaceView;
import com.maniu.wangyimusicplayer.service.MNPlayer;
import com.maniu.wangyimusicplayer.service.MusicService;


public class MainActivityVideo extends AppCompatActivity {

    private MNPlayer mnPlayer;
    private TextView tvTime;
    private MNGLSurfaceView mnGLSurfaceView;
    private SeekBar seekBar;
    private int position;
    private boolean seek = false;
    List<String> paths = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_video);
        tvTime = findViewById(R.id.tv_time);
        mnGLSurfaceView = findViewById(R.id.wlglsurfaceview);
        seekBar = findViewById(R.id.seekbar);
        checkPermission();
        mnPlayer = new MNPlayer();
        mnPlayer.setMNGLSurfaceView(mnGLSurfaceView);



        File file = new File(Environment.getExternalStorageDirectory(),"input.mkv");
        paths.add(file.getAbsolutePath());
        file = new File(Environment.getExternalStorageDirectory(),"input.avi");
        paths.add(file.getAbsolutePath());

        file = new File(Environment.getExternalStorageDirectory(),"input.rmvb");
        paths.add(file.getAbsolutePath());
        paths.add("http://mn.maliuedu.com/music/input.mp4");
        mnPlayer.setPlayerListener(new IPlayerListener() {
            @Override
            public void onLoad(boolean load) {

            }
// 时间   总时间
            @Override
            public void onCurrentTime(int currentTime, int totalTime) {
                if(!seek &&totalTime> 0)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setProgress(currentTime* 100 / totalTime);
                            tvTime.setText( DisplayUtil.secdsToDateFormat(currentTime)
                                    + "/" + DisplayUtil.secdsToDateFormat( totalTime));
                        }
                    });

                }
            }
            @Override
            public void onError(int code, String msg) {

            }

            @Override
            public void onPause(boolean pause) {

            }

            @Override
            public void onDbValue(int db) {

            }

            @Override
            public void onComplete() {

            }

            @Override
            public String onNext() {
                return null;
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position = progress * mnPlayer.getDuration() / 100;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seek = true;
            }
//seek   ---》初始化    这里   所有
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mnPlayer.seek(position);
                seek = false;
            }
        });

    }

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

    public void begin(View view) {

        mnPlayer.setMnOnParparedListener(new MNOnParparedListener() {
            @Override
            public void onParpared() {
                mnPlayer.start();
            }
        });
//音视频面试   30道 心里分析  切入点   步骤
        File file = new File(Environment.getExternalStorageDirectory(),"input.mp4");
        mnPlayer.setSource(file.getAbsolutePath());
//       mnPlayer.setSource("rtmp://58.200.131.2:1935/livetv/cctv1");
//        mnPlayer.setSource("http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8");
//        mnPlayer.setSource("http://mn.maliuedu.com/music/input.mp4");
//        wlPlayer.setSource("/mnt/shared/Other/testvideo/楚乔传第一集.mp4");
//        mnPlayer.setSource("/mnt/shared/Other/testvideo/屌丝男士.mov");
//        wlPlayer.setSource("http://ngcdn004.cnr.cn/live/dszs/index12.m3u8");
        mnPlayer.parpared();
    }

    public void pause(View view) {
        mnPlayer.pause();

    }

    public void resume(View view) {
        mnPlayer.resume();
    }


    public void stop(View view) {
    }


    public void next(View view) {
        //wlPlayer.playNext("/mnt/shared/Other/testvideo/楚乔传第一集.mp4");
    }

    public void speed1(View view) {
        mnPlayer.setSpeed(1.5f);
    }

    public void speed2(View view) {
        mnPlayer.setSpeed(2.0f);
    }

    public void speed(View view) {
        mnPlayer.setSpeed(1.0f);
    }

    public void higthVolume(View view) {
        mnPlayer.setHigth(0.5f);
    }

    public void lowVolume(View view) {
        mnPlayer.setHigth(2f);
    }
}



