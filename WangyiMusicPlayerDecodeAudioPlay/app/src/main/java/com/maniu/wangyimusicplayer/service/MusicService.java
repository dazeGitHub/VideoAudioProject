package com.maniu.wangyimusicplayer.service;

import static com.maniu.wangyimusicplayer.MainActivity.PARAM_MUSIC_LIST;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.maniu.wangyimusicplayer.lisnter.IPlayerListener;
import com.maniu.wangyimusicplayer.lisnter.MNOnParparedListener;
import com.maniu.wangyimusicplayer.musicui.model.MusicData;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {
    private static final String TAG = "David";
    private MNPlayer mnPlayer;
    /*操作指令*/
    /*操作指令*/
    public static final String ACTION_OPT_MUSIC_PLAY = "ACTION_OPT_MUSIC_PLAY";
    public static final String ACTION_OPT_MUSIC_PAUSE = "ACTION_OPT_MUSIC_PAUSE";
    public static final String ACTION_OPT_MUSIC_RESUME = "ACTION_OPT_MUSIC_RESUME";
    public static final String ACTION_OPT_MUSIC_NEXT = "ACTION_OPT_MUSIC_NEXT";
    public static final String ACTION_OPT_MUSIC_LAST = "ACTION_OPT_MUSIC_LAST";
    public static final String ACTION_OPT_MUSIC_SEEK_TO = "ACTION_OPT_MUSIC_SEEK_TO";
    public static final String ACTION_OPT_MUSIC_LEFT = "ACTION_OPT_MUSIC_LEFT";
    public static final String ACTION_OPT_MUSIC_RIGHT = "ACTION_OPT_MUSIC_RIGHT";
    public static final String ACTION_OPT_MUSIC_CENTER = "ACTION_OPT_MUSIC_CENTER";
    public static final String ACTION_OPT_MUSIC_VOLUME = "ACTION_OPT_MUSIC_VOLUME";
    /*状态指令*/
    public static final String ACTION_STATUS_MUSIC_PLAY = "ACTION_STATUS_MUSIC_PLAY";
    public static final String ACTION_STATUS_MUSIC_PAUSE = "ACTION_STATUS_MUSIC_PAUSE";
    public static final String ACTION_STATUS_MUSIC_COMPLETE = "ACTION_STATUS_MUSIC_COMPLETE";
    public static final String ACTION_STATUS_MUSIC_DURATION = "ACTION_STATUS_MUSIC_DURATION";
    public static final String ACTION_STATUS_MUSIC_PLAYER_TIME = "ACTION_STATUS_MUSIC_PLAYER_TIME";
    public static final String PARAM_MUSIC_DURATION = "PARAM_MUSIC_DURATION";
    public static final String PARAM_MUSIC_SEEK_TO = "PARAM_MUSIC_SEEK_TO";
    public static final String PARAM_MUSIC_CURRENT_POSITION = "PARAM_MUSIC_CURRENT_POSITION";
    public static final String PARAM_MUSIC_IS_OVER = "PARAM_MUSIC_IS_OVER";
    private List<String> mMusicDatas = new ArrayList<>();
    private int mCurrentMusicIndex = 0;
    private MusicReceiver mMusicReceiver = new MusicReceiver();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mMusicDatas = intent.getStringArrayListExtra(PARAM_MUSIC_LIST);
        Log.d("david", " mMusicDatas  " + mMusicDatas.size());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initBoardCastReceiver();
        mnPlayer = new MNPlayer();
        mnPlayer.setMnOnParparedListener(new MNOnParparedListener() {
            @Override
            public void onParpared() {
                mnPlayer.start();
            }
        });
        mnPlayer.setPlayerListener(new IPlayerListener() {
            @Override
            public void onLoad(boolean load) {

            }

            @Override
            public void onCurrentTime(int currentTime, int totalTime) {
                Intent intent = new Intent(ACTION_STATUS_MUSIC_PLAYER_TIME);
                intent.putExtra("currentTime", currentTime);
                intent.putExtra("totalTime", totalTime);
                LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intent);
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
        mnPlayer.parpared();
    }

    private void initBoardCastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_OPT_MUSIC_PLAY);
        intentFilter.addAction(ACTION_OPT_MUSIC_PAUSE);
        intentFilter.addAction(ACTION_OPT_MUSIC_RESUME);
        intentFilter.addAction(ACTION_OPT_MUSIC_NEXT);
        intentFilter.addAction(ACTION_OPT_MUSIC_LAST);
        intentFilter.addAction(ACTION_OPT_MUSIC_SEEK_TO);
        intentFilter.addAction(ACTION_OPT_MUSIC_LEFT);
        intentFilter.addAction(ACTION_OPT_MUSIC_RIGHT);
        intentFilter.addAction(ACTION_OPT_MUSIC_VOLUME);
        intentFilter.addAction(ACTION_OPT_MUSIC_CENTER);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMusicReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMusicReceiver);
    }

    //监听
    private void play(final int index) {
        String path = mMusicDatas.get(index);
        mnPlayer.setSource(path);
        mnPlayer.parpared();
    }

    private void pause() {
        mnPlayer.pause();
    }

    private void resume() {
    }

    private void stop() {
    }

    private void next() {
    }

    private void last() {

    }

    // 出发点      postion    ffmpeg   做 seek
    private void seekTo(int position) {
        mnPlayer.seek(position);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }
    class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive: "+action);
            if (action.equals(ACTION_OPT_MUSIC_PLAY)) {
                play(mCurrentMusicIndex);
            }

            else if (action.equals(ACTION_OPT_MUSIC_LAST)) {
                last();
            }
            else if (action.equals(ACTION_OPT_MUSIC_NEXT)) {
                next();
            }
            else if (action.equals(ACTION_OPT_MUSIC_SEEK_TO)) {
                int position = intent.getIntExtra(MusicService.PARAM_MUSIC_SEEK_TO, 0);
                seekTo(position);
            }
            else if (action.equals(ACTION_OPT_MUSIC_RESUME)) {

                resume();
            }
            else if (action.equals(ACTION_OPT_MUSIC_PAUSE)) {
                pause();
            }
            else if (action.equals(ACTION_OPT_MUSIC_RIGHT)) {
                Log.d("david", "----------ACTION_OPT_MUSIC_RIGHT-->");
                mnPlayer.setMute(0);
            }
            else if (action.equals(ACTION_OPT_MUSIC_LEFT)) {
                Log.d("david", "----------ACTION_OPT_MUSIC_LEFT-->");
                mnPlayer.setMute(1);
            }

            else if (action.equals(ACTION_OPT_MUSIC_CENTER)) {
                mnPlayer.setMute(2);
            }
            else if (action.equals(ACTION_OPT_MUSIC_VOLUME)) {

            }
        }
    }
    int i = 0;
}
