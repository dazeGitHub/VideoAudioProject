package com.maniu.openglimage;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.ViewGroup;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private GLSurfaceView mSurfaceView;
    private int mTextureId =-1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewGroup container = (ViewGroup) findViewById(R.id.main);
        mSurfaceView = new GLSurfaceView(this);
        mSurfaceView.setEGLContextClientVersion(2);
        container.addView(mSurfaceView);

        final ImageFilter filter = new ImageFilter(this);
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.demo);
        mSurfaceView.setRenderer(new GLSurfaceView.Renderer() {

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//              bitmap--- opengl
                mTextureId = filter.init(bitmap);
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
//              告诉 openGL 窗口大小
                GLES20.glViewport(0, 0, width, height);
            }

//          GLSurfaceView   onDrawFrame  摄像机    camerax 最终触发  onDrawFrame() 方法
            @Override
            public void onDrawFrame(GL10 gl) {
                filter.drawFrame(mTextureId);
            }
        });
    }
}