package com.maniu.ffmpegmusicplayer.player;

import android.text.TextUtils;
import android.util.Log;

import com.maniu.ffmpegmusicplayer.listener.IPlayerListener;
import com.maniu.ffmpegmusicplayer.listener.WlOnParparedListener;
import com.maniu.ffmpegmusicplayer.log.MyLog;
import com.maniu.ffmpegmusicplayer.opengl.MNGLSurfaceView;

/**
 * Created by yangw on 2018-2-28.
 */

public class MNPlayer {
    static {
        System.loadLibrary("native-lib");
    }
    private String source;//数据源
    private WlOnParparedListener mnOnParparedListener;

    private IPlayerListener playerListener;
    private MNGLSurfaceView davidView;
    private int duration = 0;
    public void setPlayerListener(IPlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    public MNPlayer()
    {}

    /**
     * 设置数据源
     * @param source
     */
    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * 设置准备接口回调
     * @param mnOnParparedListener
     */
    public void setWlOnParparedListener(WlOnParparedListener mnOnParparedListener)
    {
        this.mnOnParparedListener = mnOnParparedListener;
    }

    public void parpared()
    {
        if(TextUtils.isEmpty(source))
        {
            MyLog.d("source not be empty");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_parpared(source);
            }
        }).start();

    }
    public void onCallLoad(boolean load)
    {

//        native   网络不行     再       肯定有
    }
    public void start()
    {
        if(TextUtils.isEmpty(source))
        {
            MyLog.d("source is empty");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_start();
            }
        }).start();
    }


    /**
     * c++回调java的方法
     */
    public void onCallParpared()
    {
        if(mnOnParparedListener != null)
        {
            mnOnParparedListener.onParpared();
        }
    }
//    native 回调 应用层的入口
    public void onCallRenderYUV(int width, int height, byte[] y, byte[] u, byte[] v)
    {
//        opengl  的java版本
        if( this.davidView != null)
        {
            this.davidView.setYUVData(width, height, y, u, v);
        }


    }
    public int getDuration() {
        return duration;
    }
    public void onCallTimeInfo(int currentTime, int totalTime)
    {
        if (playerListener == null) {
            return;
        }
        duration = totalTime;
        playerListener.onCurrentTime(currentTime, totalTime);
    }
    public void seek(int secds) {
        n_seek(secds);
    }
    public native void n_parpared(String source);
    public native void n_start();
    private native void n_seek(int secds);
    private native void n_resume();
    private native void n_pause();
    private native void n_mute(int mute);
    private native void n_volume(int percent);
    private native void n_speed(float speed);
    private native void n_pitch(float pitch);
    public void setSpeed(float speed) {
        n_speed(speed);

    }
    public void setVolume(int percent)
    {
        if(percent >=0 && percent <= 100)
        {
            n_volume(percent);
        }
    }

    public void stop()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_stop();
            }
        }).start();
    }

    private native void n_stop();
    public void pause() {
        n_pause();
    }
    public void setPitch(float pitch) {
        n_pitch(pitch);
    }
    public void resume() {
        n_resume();
    }

    public void setMute(int mute) {
        n_mute(mute);
    }

    public void setMNGLSurfaceView(MNGLSurfaceView davidView) {
        this.davidView = davidView;
        Log.i("David", "setMNGLSurfaceView: --------------设值"+this.hashCode());

    }


}
