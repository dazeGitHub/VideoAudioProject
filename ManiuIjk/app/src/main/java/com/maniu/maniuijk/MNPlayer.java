package com.maniu.maniuijk;

import android.view.Surface;
import android.view.SurfaceView;

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

}
