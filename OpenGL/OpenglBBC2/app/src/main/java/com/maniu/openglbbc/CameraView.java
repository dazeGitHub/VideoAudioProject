package com.maniu.openglbbc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//opengl GLSurfaceView  使用的是什么版本
public class CameraView extends GLSurfaceView implements Preview.OnPreviewOutputUpdateListener, GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private SurfaceTexture mCameraTexure;
    ScreenFilter screenFilter;
//   gpu 数据所在地方, 对 cpu 没有意义, 但是对 gpu 有意义
    private  int textures = 0;

    public CameraView(Context context) {
        super(context);
    }
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
//
        setEGLContextClientVersion(2);
//        opengl 渲染方式
        setRenderer(this);
//          手动  摄像头  啊
//        requestRender();
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        自动
//        16ms   自动渲染
        initCamera();
    }

    private void initCamera() {
        CameraHelper cameraHelper = new CameraHelper
                ((LifecycleOwner)  getContext(),
                        this);
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        mCameraTexure = output.getSurfaceTexture();
//        mCameraTexure---- GLSurfaceView  报错    准备好了   才能够绑定
//        纠正的举矩阵

    }
//
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraTexure.attachToGLContext(textures);
        //        监听摄像头的数据     什么时候刷新成功
        mCameraTexure.setOnFrameAvailableListener(this);
        screenFilter = new ScreenFilter( getContext());
//       面向过程
    }

    // 每次有数据 的时候  都丢到这里  y  u   v   有1  没有 2
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//        手动渲染
        requestRender();
    }

//    onDraw
    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {

    }

//onDraw  ---> onDrawFrame
    @Override
    public void onDrawFrame(GL10 gl10) {
//      gpu       确定形状    上色
        mCameraTexure.updateTexImage();
//      4 x 4 = 16, 即使是设置为 2 x 2 也不影响
        float[] mtx = new float[16];
        mCameraTexure.getTransformMatrix(mtx); //得到纠正的矩阵, 和矩阵相乘可以得到正常的图像, 防止图像拉伸
//      opengl 渲染
        screenFilter.onDraw(getWidth(), getHeight(), mtx, textures);
    }
}
