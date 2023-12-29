package com.maniu.webrtcmaniua;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;

public class LocalSurfaceView extends SurfaceView implements SurfaceHolder.Callback,
        Camera.PreviewCallback {
    private Camera mCamera;
    private Camera.Size size;
    EncodecPushLiveH264 encodecPushLiveH264;
    byte[] buffer;
    public LocalSurfaceView(Context context) {
        super(context);
    }

    public LocalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }
//
    public void startCapture(SocketLive.SocketCallback socketCallback) {
        encodecPushLiveH264 = new EncodecPushLiveH264(socketCallback,size.width, size.height);
        encodecPushLiveH264.startLive();
    }

//    data      NV21   ----》NV12       数据横向
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (encodecPushLiveH264 != null) {
            encodecPushLiveH264.encodeFrame(data);
        }
        mCamera.addCallbackBuffer(data);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        startPreview();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }
    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//        流程
        Camera.Parameters parameters = mCamera.getParameters();
        //获取摄像头的最佳尺寸
        size = parameters.getPreviewSize();

        try {
            mCamera.setPreviewDisplay(getHolder());
//            横着
            mCamera.setDisplayOrientation(90);
//          buffer 的大小和 width height 编码格式 有关
            buffer = new byte[size.width * size.height * 3 / 2];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
//            输出数据怎么办
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
