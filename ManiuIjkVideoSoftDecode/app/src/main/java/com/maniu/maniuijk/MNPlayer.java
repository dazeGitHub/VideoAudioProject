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
//直播  rtm 协议 直播 万能
//    获取到视频的宽高
    public void onSizeChange(int width, int heigth) {
        float ratio = width / (float) heigth;
        int screenWidth = surfaceView.getContext().getResources().getDisplayMetrics().widthPixels;
        int videoWidth = 0;
        int videoHeigth = 0;
        videoWidth = screenWidth;
        videoHeigth = (int) (screenWidth / ratio);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(videoWidth, videoHeigth);
        surfaceView.setLayoutParams(lp); //调整surfaceview控件的大小
    }


}
