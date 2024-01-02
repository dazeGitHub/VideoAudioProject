package com.maniu.x264rtmpmaniu;

import android.app.Activity;
import android.util.Log;

import com.maniu.x264rtmpmaniu.camerax.VideoChanel;
import com.maniu.x264rtmpmaniu.utils.FileUtils;

public class LivePusher {
    private VideoChanel videoChannel;
//    初始化摄像头
    public LivePusher(MainActivity activity, int width, int height, int bitrate,
                  int fps, int cameraId) {
//        videoChannel = new  VideoChanel(activity);
        native_init();
    }

    public void setVideoChannel(VideoChanel videoChannel) {
        this.videoChannel = videoChannel;
    }

    public native void native_init();

    public native void native_start(String path);

    public native void native_setVideoEncInfo(int width, int height, int fps, int bitrate);
//    yuv数据推过来
    public native void native_pushVideo(byte[] data);

    public native void native_stop();

    public native void native_release();
//native 层 回调
    private void postData(byte[] data) {
        Log.i("david", "postData: "+data.length);
        FileUtils.writeBytes(data);
        FileUtils.writeContent(data);

    }

    public void startLive(String path) {
        native_start(path);
        if (videoChannel == null) {
            return;
        }
        videoChannel.startLive();
    }



}
