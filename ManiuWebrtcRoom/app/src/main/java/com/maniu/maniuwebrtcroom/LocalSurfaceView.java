package com.maniu.maniuwebrtcroom;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class LocalSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    EncoderPlayerLiveH264 encoderPlayerLiveH264;
    private Camera.Size size;
    //    Camera 2  CameraX
    private Camera mCamera;
    public void startCapture(SocketLive socketLive) {
        encoderPlayerLiveH264 = new EncoderPlayerLiveH264(size.width, size.height);
        encoderPlayerLiveH264.startCapture(socketLive);
    }
    public LocalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }
    byte[] buffer;
    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = mCamera.getParameters();
        size = parameters.getPreviewSize();
        try {
            mCamera.setPreviewDisplay(getHolder());
            mCamera.setDisplayOrientation(90);
            buffer = new byte[size.width * size.height * 3 / 2];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
//            输出数据怎么办
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //    摄像头  捕获
    public void onPreviewFrame(byte[] bytes, Camera camera) {
//        摄像头 捕获  照片是  横着的  1    竖着的   2  手机竖着拍
        if (encoderPlayerLiveH264 != null) {
//            cpu
            encoderPlayerLiveH264.encodeFrame(bytes);
        }
        mCamera.addCallbackBuffer(bytes);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
