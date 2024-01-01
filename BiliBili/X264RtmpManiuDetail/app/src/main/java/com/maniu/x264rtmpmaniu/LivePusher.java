package com.maniu.x264rtmpmaniu;

import android.app.Activity;

import com.maniu.x264rtmpmaniu.camerax.VideoChanel;

public class LivePusher {
    private VideoChanel videoChannel;
//    初始化摄像头
    public LivePusher(MainActivity activity, int width, int height, int bitrate,
                  int fps, int cameraId) {
//        videoChannel = new  VideoChanel(activity);

    }


    public native void native_init();

    public native void native_start(String path);

    public native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);
//    yuv数据推过来
    public native void native_pushVideo(byte[] data);

    public native void native_stop();

    public native void native_release();


}
