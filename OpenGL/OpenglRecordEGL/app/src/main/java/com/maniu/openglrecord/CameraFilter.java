package com.maniu.openglrecord;

import static android.opengl.GLES20.GL_TEXTURE0;

import android.content.Context;
import android.opengl.GLES20;

import com.maniu.openglrecord.utils.OpenGLUtils;

public class CameraFilter  extends AbstractFilter {
    private float[] mtx;
    private int vMatrix;


    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        //1、创建fbo （离屏屏幕）
        mFrameBuffers = new int[1];
//bitmap  渲染

//          图层

//          纹理


 //fbo的创建 (缓存)
        //1、创建fbo （离屏屏幕） 数据  缓冲区   ----》   缓冲区
        GLES20.glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);

        //2、创建属于fbo的纹理
        mFrameBufferTextures = new int[1]; //用来记录纹理id
        //创建纹理
        OpenGLUtils.glGenTextures(mFrameBufferTextures);
        //让fbo与 纹理发生关系
        //创建一个 2d的图像
        // 目标 2d纹理+等级 + 格式 +宽、高+ 格式 + 数据类型(byte) + 像素数据
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mFrameBufferTextures[0]);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,width,height,
                0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE, null);


        // 让fbo与纹理绑定起来 ， 后续的操作就是在操作fbo与这个纹理上了
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameBuffers[0]);

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
        //解绑
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);


    }
//主线程 1   GL线程  2
    public CameraFilter(Context context) {
        super(context, R.raw.camera_vert, R.raw.camera_frag);
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");

    }
//主线程 1
    @Override
    public int onDraw(int texture) {

        return mFrameBufferTextures[0];
    }

    @Override
    public void beforeDraw() {
        GLES20.glActiveTexture(GL_TEXTURE0);
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

    }
    public void setTransformMatrix(float[] mtx) {
        this.mtx = mtx;
    }
}
