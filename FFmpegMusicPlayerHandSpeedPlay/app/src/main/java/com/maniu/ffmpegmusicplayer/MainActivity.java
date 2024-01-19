package com.maniu.ffmpegmusicplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.maniu.ffmpegmusicplayer.listener.IPlayerListener;
import com.maniu.ffmpegmusicplayer.listener.WlOnParparedListener;
import com.maniu.ffmpegmusicplayer.log.MyLog;
import com.maniu.ffmpegmusicplayer.musicui.utils.DisplayUtil;
import com.maniu.ffmpegmusicplayer.opengl.MNGLSurfaceView;
import com.maniu.ffmpegmusicplayer.player.MNPlayer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private MNPlayer mnPlayer;
    private TextView tvTime;
    private MNGLSurfaceView mnGLSurfaceView;
    private SeekBar seekBar;
    private int position;
    private boolean seek = false;
    List<String> paths = new ArrayList<>();
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
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

        mnPlayer.setWlOnParparedListener(new WlOnParparedListener() {
            @Override
            public void onParpared() {
                MyLog.d("准备好了，可以开始播放声音了");
                mnPlayer.start();
            }
        });
//音视频面试   30道 心里分析  切入点   步骤 
       File file = new File(Environment.getExternalStorageDirectory(),"input.rmvb");
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
        mnPlayer.stop();
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
}
