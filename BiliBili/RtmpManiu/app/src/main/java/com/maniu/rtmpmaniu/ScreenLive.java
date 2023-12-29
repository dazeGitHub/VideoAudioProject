package com.maniu.rtmpmaniu;

import android.media.projection.MediaProjection;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 用来传输 RTMPPackage 数据包
 */
public class ScreenLive extends Thread {
    // 我的直播间 - 开播设置 https://link.bilibili.com/p/center/index#/my-room/start-live 服务器地址 和 串流密钥 都要复制
    //    rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_345162489_81809986&key=6ba7ec38481c5dd2b3f8e4fb2b5fb8e0&schedule=rtmp&pflag=1
    private String url;
    private MediaProjection mediaProjection;

    // 队列, LinkedBlockingQueue 是阻塞队列, 如果 LinkedBlockingQueue 没有数据就会阻塞住
    private LinkedBlockingQueue<RTMPPackage> queue = new LinkedBlockingQueue<>();

    static {
        System.loadLibrary("rtmpmaniu");
    }

    // 正在执行     isLive    关闭
    private boolean isLiving;

    //入口方法
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

    //发送数据前, 先建立连接
    private native boolean connect(String url);

    private native void testNativeCrash();

    @Override
    public void run() {
        isLiving = true;
        if (!connect(url)) {
            Log.i("david", "run: ----------->推送失败");
            return;
        }
        while (isLiving) {
            RTMPPackage rtmpPackage = null;
            try {
                rtmpPackage = queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            发送数据
        }
    }
}
