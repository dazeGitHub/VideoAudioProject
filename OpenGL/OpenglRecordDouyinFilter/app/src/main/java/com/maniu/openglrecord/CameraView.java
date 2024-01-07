package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import androidx.lifecycle.LifecycleOwner;

import com.maniu.openglrecord.utils.CameraHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraView extends GLSurfaceView {
    private  CameraRender renderer;
    public CameraView(Context context) {
        super(context);
    }
    private void initCamera() {
        CameraHelper cameraHelper = new CameraHelper
                ((LifecycleOwner)  getContext(),
                        renderer);
    }
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        renderer = new CameraRender(this);
        setEGLContextClientVersion(2);
        setRenderer(renderer);
        initCamera();
        /**
         * 刷新方式：
         *     RENDERMODE_WHEN_DIRTY 手动刷新，調用requestRender();
         *     RENDERMODE_CONTINUOUSLY 自動刷新，大概16ms自動回調一次onDrawFrame方法
         */
        //注意必须在setRenderer 后面。
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    public void startRecord() {
        renderer.startRecord(0.2f);
    }

    public void stopRecord() {
        renderer.stopRecord();
    }
}
