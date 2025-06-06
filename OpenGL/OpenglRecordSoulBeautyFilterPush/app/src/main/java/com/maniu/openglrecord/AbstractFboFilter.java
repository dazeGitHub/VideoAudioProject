package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLES20;

import com.maniu.openglrecord.utils.OpenGLUtils;

public class AbstractFboFilter extends AbstractFilter{
//fbo
    private int[] mFrameBuffers;

//    纹理
    private int[] mFrameBufferTextures;
    public AbstractFboFilter(Context context, int vertexShaderId, int fragmentShaderId) {
        super(context, vertexShaderId, fragmentShaderId);
    }


    public void destroyFrameBuffers() {
        //删除fbo的纹理
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        //删除fbo
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        if (mFrameBuffers != null) {
            destroyFrameBuffers();
        }
        //fbo的创建 (缓存)
        //1、创建fbo （离屏屏幕）
        mFrameBuffers = new int[1];
        // 1、创建几个fbo 2、保存fbo id的数据 3、从这个数组的第几个开始保存
        GLES20.glGenFramebuffers(mFrameBuffers.length,mFrameBuffers,0);
        //2、创建属于fbo的纹理
        mFrameBufferTextures = new int[1]; //用来记录纹理id
        //创建纹理
        OpenGLUtils.glGenTextures(mFrameBufferTextures);

        //让fbo与 纹理发生关系
        //创建一个 2d的图像
        // 目标 2d纹理+等级 + 格式 +宽、高+ 格式 + 数据类型(byte) + 像素数据
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        // 让fbo与纹理绑定起来 ， 后续的操作就是在操作fbo与这个纹理上了
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
        //解绑
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);

    }

    @Override
    public int onDraw(int texture) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameBuffers[0]);
        super.onDraw(texture);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
        return mFrameBufferTextures[0];
    }

}
