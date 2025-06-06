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

import com.maniu.wangyimusicplayer.musicui.model.MusicData;
import com.maniu.wangyimusicplayer.musicui.utils.DisplayUtil;
import com.maniu.wangyimusicplayer.musicui.widget.BackgourndAnimationRelativeLayout;
import com.maniu.wangyimusicplayer.musicui.widget.DiscView;
import com.maniu.wangyimusicplayer.service.MusicService;


public class MainActivity extends AppCompatActivity implements DiscView.IPlayInfo, View
        .OnClickListener {
    private static final String TAG = "David";
    private DiscView mDisc;
    private Toolbar mToolbar;
    private SeekBar mSeekBar;
    private ImageView mIvPlayOrPause, mIvNext, mIvLast;
    private TextView mTvMusicDuration,mTvTotalMusicDuration;
    private BackgourndAnimationRelativeLayout mRootLayout;
    public static final int MUSIC_MESSAGE = 0;

    public static final String PARAM_MUSIC_LIST = "PARAM_MUSIC_LIST";
    DisplayUtil displayUtil = new DisplayUtil();
    private MusicReceiver mMusicReceiver = new MusicReceiver();
    private List<MusicData> mMusicDatas = new ArrayList<>();
    private int totalTime;
    private int position;
    private boolean playState = false;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        initMusicDatas();
        initView();
        initMusicReceiver();
        DisplayUtil.makeStatusBarTransparent(this);
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    optMusic(ACTION_OPT_MUSIC_VOLUME);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
        }.start();
    }

    private void initMusicReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PLAY);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PAUSE);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_DURATION);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_COMPLETE);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PLAYER_TIME);
        /*注册本地广播*/
        LocalBroadcastManager.getInstance(this).registerReceiver(mMusicReceiver,intentFilter);
    }

    private void initView() {
        mDisc = (DiscView) findViewById(R.id.discview);
        mIvNext = (ImageView) findViewById(R.id.ivNext);
        mIvLast = (ImageView) findViewById(R.id.ivLast);
        mIvPlayOrPause = (ImageView) findViewById(R.id.ivPlayOrPause);
        mTvMusicDuration = (TextView) findViewById(R.id.tvCurrentTime);
        mTvTotalMusicDuration = (TextView) findViewById(R.id.tvTotalTime);
        mSeekBar = (SeekBar) findViewById(R.id.musicSeekBar);
        mRootLayout = (BackgourndAnimationRelativeLayout) findViewById(R.id.rootLayout);
        mDisc.setPlayInfoListener(this);
        mIvLast.setOnClickListener(this);
        mIvNext.setOnClickListener(this);
        mIvPlayOrPause.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position = totalTime* progress / 100;
                mTvMusicDuration.setText(displayUtil.duration2Time(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "onStopTrackingTouch: "+position);
                seekTo(position);
            }
        });
        mTvMusicDuration.setText(displayUtil.duration2Time(0));
        mTvTotalMusicDuration.setText(displayUtil.duration2Time(0));
        mDisc.setMusicDataList(mMusicDatas);
    }
    private void playCurrentTime(int currentTime, int totalTime) {
        mSeekBar.setProgress(currentTime*100/totalTime);
        this.totalTime = totalTime;
        mTvMusicDuration.setText( DisplayUtil.secdsToDateFormat(currentTime,totalTime));
        mTvTotalMusicDuration.setText(DisplayUtil.secdsToDateFormat(totalTime, totalTime));
    }
    private void initMusicDatas() {
        MusicData musicData1 = new MusicData( new File(Environment.getExternalStorageDirectory(),"music4.mp3").getAbsolutePath());
        MusicData musicData2= new MusicData( new File(Environment.getExternalStorageDirectory(),"music3.mp3").getAbsolutePath());
        MusicData musicData3 = new MusicData( new File(Environment.getExternalStorageDirectory(),"music2.mp3").getAbsolutePath());
        MusicData musicData6 = new MusicData(R.raw.music1, R.raw.ic_music1, "等你归来", "程响");
        MusicData musicData7 = new MusicData(R.raw.music2, R.raw.ic_music2, "Nightingale", "YANI");
        MusicData musicData8 = new MusicData(R.raw.music3, R.raw.ic_music3, "Cornfield Chase", "Hans Zimmer");
        mMusicDatas.add(musicData6);
        mMusicDatas.add(musicData7);
        mMusicDatas.add(musicData8);
        ArrayList<String> list = new ArrayList();
        list.add(musicData1.getMusicName());
        list.add(musicData2.getMusicName());
        list.add(musicData3.getMusicName());
        Intent intent = new Intent(this, MusicService.class);
        intent.putStringArrayListExtra(PARAM_MUSIC_LIST, list); //(Serializable)mMusicDatas
        startService(intent);
    }
    @Override
    public void onMusicInfoChanged(String musicName, String musicAuthor) {
        getSupportActionBar().setTitle(musicName);
        getSupportActionBar().setSubtitle(musicAuthor);
    }

    @Override
    public void onMusicPicChanged(int musicPicRes) {
        displayUtil.try2UpdateMusicPicBackground(this,mRootLayout,musicPicRes);
    }

    @Override
    public void onMusicChanged(DiscView.MusicChangedStatus musicChangedStatus) {
        switch (musicChangedStatus) {
            case PLAY:{
                play();
                break;
            }
            case PAUSE:{
                pause();
                break;
            }
            case NEXT:{
                next();
                break;
            }
            case LAST:{
                last();
                break;
            }
            case STOP:{
                stop();
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mIvPlayOrPause) {
            playState = !playState;
            Log.i(TAG, "onClick: ---------"+playState);
            if (playState) {
                mIvPlayOrPause.setImageResource(R.drawable.ic_play);
                pause();
                mDisc.stop();
            }else {
                mIvPlayOrPause.setImageResource(R.drawable.ic_pause);
                resume();
                mDisc.play();
            }


        } else if (v == mIvNext) {
            mDisc.next();
        } else if (v == mIvLast) {
            mDisc.last();
        }
    }

    private void play() {
        optMusic(MusicService.ACTION_OPT_MUSIC_PLAY);
    }

    private void pause() {
        optMusic(MusicService.ACTION_OPT_MUSIC_PAUSE);
    }
    public void resume( ) {
        optMusic(MusicService.ACTION_OPT_MUSIC_RESUME);
    }
    private void stop() {
        mIvPlayOrPause.setImageResource(R.drawable.ic_play);
        mTvMusicDuration.setText(displayUtil.duration2Time(0));
        mTvTotalMusicDuration.setText(displayUtil.duration2Time(0));
        mSeekBar.setProgress(0);
    }

    private void next() {
        mRootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                optMusic(MusicService.ACTION_OPT_MUSIC_NEXT);
            }
        }, DURATION_NEEDLE_ANIAMTOR);
        mTvMusicDuration.setText(displayUtil.duration2Time(0));
        mTvTotalMusicDuration.setText(displayUtil.duration2Time(0));
    }

    private void last() {
        mRootLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                optMusic(MusicService.ACTION_OPT_MUSIC_LAST);
            }
        }, DURATION_NEEDLE_ANIAMTOR);
        mTvMusicDuration.setText(displayUtil.duration2Time(0));
        mTvTotalMusicDuration.setText(displayUtil.duration2Time(0));
    }

    private void complete(boolean isOver) {
        if (isOver) {
            mDisc.stop();
        } else {
            mDisc.next();
        }
    }
    private void optMusic(final String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
    }
    private void seekTo(int position) {
        Intent intent = new Intent(MusicService.ACTION_OPT_MUSIC_SEEK_TO);
        intent.putExtra(MusicService.PARAM_MUSIC_SEEK_TO,position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicService.ACTION_STATUS_MUSIC_PLAY)) {
                mIvPlayOrPause.setImageResource(R.drawable.ic_pause);
                int currentPosition = intent.getIntExtra(MusicService.PARAM_MUSIC_CURRENT_POSITION, 0);
                mSeekBar.setProgress(currentPosition);
                if(!mDisc.isPlaying()){
                    mDisc.playOrPause();
                }
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_PAUSE)) {
                mIvPlayOrPause.setImageResource(R.drawable.ic_play);
                if (mDisc.isPlaying()) {
                    mDisc.playOrPause();
                }
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_DURATION)) {
                int duration = intent.getIntExtra(MusicService.PARAM_MUSIC_DURATION, 0);
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_COMPLETE)) {
                boolean isOver = intent.getBooleanExtra(MusicService.PARAM_MUSIC_IS_OVER, true);
                complete(isOver);
            }else if (action.equals(MusicService.ACTION_STATUS_MUSIC_PLAYER_TIME)) {
                int currentTime = intent.getIntExtra("currentTime", 0);
                int totalTime = intent.getIntExtra("totalTime", 0);
                playCurrentTime(currentTime,totalTime);
            }
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMusicReceiver);
    }

    public void left(View view) {

        optMusic(MusicService.ACTION_OPT_MUSIC_LEFT);
    }

    public void right(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_RIGHT);
    }

    public void center(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_CENTER);
    }

    public void speed(View view) {
    }

    public void pitch(View view) {
    }

    public void speedpitch(View view) {
    }

    public void normalspeedpitch(View view) {



    }
}
