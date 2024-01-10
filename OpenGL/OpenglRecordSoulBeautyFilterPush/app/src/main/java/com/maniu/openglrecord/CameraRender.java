package com.maniu.openglrecord;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

import androidx.camera.core.Preview;

import com.maniu.openglrecord.utils.CameraHelper;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRender implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private CameraView cameraView;
    private CameraHelper cameraHelper;
    private SurfaceTexture mCameraTexure;
    private int[] textures;
    float[] mtx = new float[16];
    private CameraFilter cameraFilter;
    MediaRecorder mMediaRecorder;
    private ScreenFilter mScreenFilter;
    private BeautyFilter beautyFilter;
    private SoulFilter soulFilter;


    private MediaEncoderAndPush mRecorder;

    public CameraRender(CameraView cameraView) {
        this.cameraView = cameraView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("david", "onSurfaceCreated: " + Thread.currentThread().getName());
        textures = new int[1];
        mCameraTexure.attachToGLContext(textures[0]);
        mCameraTexure.setOnFrameAvailableListener(this);
        cameraFilter = new CameraFilter(cameraView.getContext());
        mScreenFilter = new ScreenFilter(cameraView.getContext());
        beautyFilter = new BeautyFilter(cameraView.getContext());
        soulFilter = new SoulFilter(cameraView.getContext());
//当前线程
        String path = new File(Environment.getExternalStorageDirectory(), "input.mp4").getAbsolutePath();
        EGLContext eglContext = EGL14.eglGetCurrentContext();
//        mMediaRecorder = new MediaRecorder(cameraView.getContext(), path, 480, 640, eglContext);
        mRecorder = new MediaEncoderAndPush(cameraView.getContext() ,
                EGL14.eglGetCurrentContext(),
                480, 640);
    }


    public void startRecord(float speed) {
        try {
//            mMediaRecorder.start(speed);
            mRecorder.startPush();
        } catch (IOException e) {
            Log.i("david", "startRecord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        cameraFilter.setSize(width, height);
        mScreenFilter.setSize(width, height);
        if (beautyFilter != null) {
            beautyFilter.setSize(width, height);
        }
        if (soulFilter != null) {
            soulFilter.setSize(width, height);
        }

    }

    //
    @Override
    public void onDrawFrame(GL10 gl) {
        mCameraTexure.updateTexImage();
        mCameraTexure.getTransformMatrix(mtx);
        cameraFilter.setMatrix(mtx);
//        纹理ID  摄像头的数据
        int id = cameraFilter.onDraw(textures[0]);
        if (beautyFilter != null) {
            id=beautyFilter.onDraw(id);
        }
        if (soulFilter != null) {
            id=  soulFilter.onDraw(id);
        }

//        id      1       textures[0]   2
        id=   mScreenFilter.onDraw(id);

//        id     fbo  的数据  int cpu   gpu
//        id      1       textures[0]   2
//        mMediaRecorder.encodeFrame(id, mCameraTexure.getTimestamp());
        mRecorder.encodeFrame(id, mCameraTexure.getTimestamp());
    }

    //当有数据 过来的时候   onFrameAvailable 一次
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        cameraView.requestRender();
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        mCameraTexure = output.getSurfaceTexture();
    }

    public void stopRecord() {
        mMediaRecorder.stop();
    }

    public void enableBeauty(final boolean isChecked) {
        cameraView.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (isChecked) {
                    beautyFilter = new BeautyFilter(cameraView.getContext());
                    beautyFilter.setSize(cameraView.getWidth(), cameraView.getHeight());
                }else {
                    beautyFilter.release();
                    beautyFilter = null;
                }
            }
        });

    }



}
