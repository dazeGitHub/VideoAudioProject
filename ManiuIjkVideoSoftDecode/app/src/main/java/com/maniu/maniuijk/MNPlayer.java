package com.maniu.maniuijk;

import android.view.Surface;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

public class MNPlayer {
    static {
        System.loadLibrary("maniuijk");
    }

    SurfaceView surfaceView;

    public MNPlayer(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }
//没有问题  鲜花
    public native void play(String url, Surface surface);

//  直播  rtm 协议 直播 万能
//  根据视频的宽高 width 和 height 计算控件的宽高 videoWidth 和 videoHeight
    public void onSizeChange(int width, int height) {
        float ratio = width / (float) height;
        int screenWidth = surfaceView.getContext().getResources().getDisplayMetrics().widthPixels;
        int videoWidth = 0;
        int videoHeight = 0;
        videoWidth = screenWidth;
        videoHeight = (int) (screenWidth / ratio);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(videoWidth, videoHeight);
        surfaceView.setLayoutParams(lp); //调整 surfaceView 控件的大小
    }
}
