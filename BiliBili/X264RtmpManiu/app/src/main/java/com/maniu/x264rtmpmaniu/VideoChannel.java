package com.maniu.x264rtmpmaniu;

import android.app.Activity;
import android.hardware.Camera;

public class VideoChannel implements Camera.PreviewCallback {
    private LivePusher mLivePusher;
    private CameraHelper cameraHelper;
    private boolean isLiving;
    public VideoChannel(LivePusher livePusher, Activity activity,
                        int width, int height, int bitrate, int fps, int cameraId) {
        cameraHelper = new CameraHelper(activity, cameraId, width, height);
        cameraHelper.setPreviewCallback(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
//        yuv   ----
        if (isLiving) {
            mLivePusher.native_pushVideo(data);
        }

    }
}
