package com.maniu.openglbbc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import androidx.camera.core.Preview;
import androidx.lifecycle.LifecycleOwner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//opengl GLSurfaceView  使用的是什么版本
//如果要使用 opengl, 那么一定要继承自 GLSurfaceView
public class CameraView extends GLSurfaceView implements Preview.OnPreviewOutputUpdateListener, GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private SurfaceTexture mCameraTexture;
    ScreenFilter screenFilter;

//   gpu 数据所在地方
    private  int textures = 0;

    public CameraView(Context context) {
        super(context);
    }
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
//
        setEGLContextClientVersion(2); //使用 opengl 2.0
//      设置 opengl 渲染方式: 手动 或 自动
        setRenderer(this);
//      默认渲染方式为 自动渲染 RENDERMODE_CONTINUOUSLY

//      手动渲染需要调用该方法
//      requestRender();
//      手动渲染设置渲染方式为 RENDERMODE_WHEN_DIRTY, 只有在创建和调用 requestRender() 时才会刷新
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        initCamera();
    }

    private void initCamera() {
        CameraHelper cameraHelper = new CameraHelper((LifecycleOwner) getContext(), this);
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        mCameraTexture = output.getSurfaceTexture(); //将接收到的图像数据封装都了 SurfaceTexture
//      mCameraTexture---- GLSurfaceView  只能等 SurfaceView  准备好了   才能够绑定, 所以要监听 SurfaceView 什么时候准备好
//      当 SurfaceView 准备好后会回调 onSurfaceCreated() 方法
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraTexture.attachToGLContext(textures);
//      监听摄像头的数据     什么时候刷新成功
        mCameraTexture.setOnFrameAvailableListener(this); //监听触发 onFrameAvailable(SurfaceTexture) 方法
        screenFilter = new ScreenFilter(getContext());
//      面向过程
    }

    // 每次有数据 的时候  都丢到这里, 之前的项目 X264RtmpManiu 触发 analyze(ImageProxy, rotationDegrees) 方法
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//      手动渲染, 触发 onDrawFrame(GL10)
        requestRender();
    }

//    onDraw
    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {

    }

//  onDraw  ---> onDrawFrame
    @Override
    public void onDrawFrame(GL10 gl10) {
//      gpu 要做的事情 :    确定形状    上色
        mCameraTexture.updateTexImage();
//      opengl 渲染
    }
}
