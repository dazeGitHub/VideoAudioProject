package com.maniu.rtmpmaniu;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

public class ScreenLive extends Thread {
    private static final String TAG = "David";
    //    rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_345162489_81809986&key=6ba7ec38481c5dd2b3f8e4fb2b5fb8e0&schedule=rtmp&pflag=1
    private String url;
    private MediaProjection mediaProjection;


    // 队列
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();
    static {
        System.loadLibrary("rtmpmaniu");
    }
    // 正在执行     isLive    关闭
    private boolean isLiving;

    //    入口方法
//生产者入口
    public void addPackage(RTMPPackage rtmpPackage) {
        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }

    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        start();
    }
    private native boolean connect(String url);

    private native boolean sendData(byte[] data, int len, long tms, int type);

    @Override
    public void run() {
        isLiving = true;
        if (!connect(url)) {
            Log.i("david", "run: ----------->推送失败");
            return;
        }
        VideoCodec videoCodec = new VideoCodec(this);
        videoCodec.startLive(mediaProjection);
        AudioCodec audioCodec = new AudioCodec(this);
        audioCodec.startLive();
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//          发送数据
            Log.i(TAG, "取出数据" );
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i(TAG, "run: ----------->推送 "+ rtmpPackage.getBuffer().length);
                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer()
                        .length , rtmpPackage.getTms(), rtmpPackage.getType());
            }
        }
    }
}
