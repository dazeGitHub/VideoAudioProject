package com.maniu.openglrecord;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import com.maniu.openglrecord.utils.CameraHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRender implements GLSurfaceView.Renderer , Preview.OnPreviewOutputUpdateListener, SurfaceTexture.OnFrameAvailableListener {
    private CameraView cameraView;
    private CameraHelper cameraHelper;
    private SurfaceTexture mCameraTexure;
    private  int[] textures;
    float[] mtx = new float[16];
    private CameraFilter cameraFilter;

    private ScreenFilter mScreenFilter;

    public CameraRender(CameraView cameraView) {
        this.cameraView = cameraView;
        LifecycleOwner lifecycleOwner = (LifecycleOwner) cameraView.getContext();
//        打开摄像头
//        cameraHelper = new CameraHelper(lifecycleOwner, this);


    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d("david", "onSurfaceCreated: "+Thread.currentThread().getName());
        textures = new int[1];
        mCameraTexure.attachToGLContext(textures[0]);
        mCameraTexure.setOnFrameAvailableListener(this);
        cameraFilter = new CameraFilter(cameraView.getContext());
        mScreenFilter = new ScreenFilter(cameraView.getContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        cameraFilter.setSize(width,height);
        mScreenFilter.setSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mCameraTexure.updateTexImage();
        mCameraTexure.getTransformMatrix(mtx);
        cameraFilter.setTransformMatrix(mtx);
        cameraFilter.onDraw(textures[0]);
    }
    //当有数据 过来的时候   onFrameAvailable 一次
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        cameraView.requestRender();
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        mCameraTexure=output.getSurfaceTexture();
    }
}
