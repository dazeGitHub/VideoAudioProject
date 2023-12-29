package com.maniu.rtmpbibili;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

//推送层   维持这样的队列
public class ScreenLive extends Thread {
    private String url;
    private MediaProjection mediaProjection;

    static {
        System.loadLibrary("native-lib");
    }
// 队列
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();

// 正在执行     isLive    关闭
    private boolean isLiving;

//生产者入口
    public void addPackage(RTMPPackage rtmpPackage) {

        if (!isLiving) {
            return;
        }
        queue.add(rtmpPackage);
    }
//    开启 推送模式
    public void startLive(String url, MediaProjection mediaProjection) {
        this.url = url;
        this.mediaProjection = mediaProjection;
        start();
    }

    @Override
    public void run() {
        //1推送到
        if (!connect(url)) {
            Log.i("david", "run: ----------->推送失败");
            return;
        }
//        开启线程
//
        VideoCodec videoCodec = new VideoCodec(this);
        videoCodec.startLive(mediaProjection);
        isLiving = true;
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//
            if (rtmpPackage.getBuffer() != null && rtmpPackage.getBuffer().length != 0) {
                Log.i("maniu", "run: ----------->推送 "+ rtmpPackage.getBuffer().length);

                sendData(rtmpPackage.getBuffer(), rtmpPackage.getBuffer()
                        .length , rtmpPackage.getTms());
            }


//            消费者
        }
    }

    private native boolean sendData(byte[] data, int len, long tms);

    private native boolean connect(String url);




//
}
