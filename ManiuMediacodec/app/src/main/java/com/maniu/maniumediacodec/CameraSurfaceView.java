package com.maniu.maniumediacodec;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.IOException;

//原 LocalSurfaceView
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    H264EncoderForCamera h264Encode;
//  让 mCamera 显示到 surfaceveiw
    private Camera.Size size;
    private Camera mCamera;

    byte[] buffer;

    public CameraSurfaceView(Context context) {
        super(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void startPreview() {
//      如果是后置摄像头是 CAMERA_FACING_BACK, 前置摄像头是 CAMERA_FACING_FRONT
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters parameters = mCamera.getParameters();
//      得到预览尺寸
        size = parameters.getPreviewSize();
        try {
            mCamera.setPreviewDisplay(getHolder());
            //这里只是将显示旋转 90 度, 但是 onPreviewFrame(data) 中 data 预览并没有旋转 90 度
            mCamera.setDisplayOrientation(90);
            //预览宽高可以算出 yuv 有多大 因为 Y : u : v = 4 : 1 : 1,
            //所以 总大小 = size.width * size.height (1 + 1/4 + 1/4) = size.width * size.height * (3 / 2)
            buffer = new byte[size.width * size.height * 3 / 2];
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

// nv21    古老 android
//  通过 mCamera.setPreviewCallbackWithBuffer(this); 设置回调后
//  摄像头每次都回调该方法, bytes 里面是 yuv 数据
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
//        bytes  yuv  图片  手机竖着, 拍摄的 画面是横着的
//      因为 data 画面是横着的, 所以如果要竖着, 编码时 宽和高 是要交换的
        if (h264Encode == null) {
            this.h264Encode = new H264EncoderForCamera(size.width, size.height);
            h264Encode.startLive();
        }
//        数据
        h264Encode.encodeFrame(bytes);
        mCamera.addCallbackBuffer(bytes);
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
}
