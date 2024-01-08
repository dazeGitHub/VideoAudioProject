package com.maniu.openglrecord;

import static android.opengl.GLES20.GL_TEXTURE0;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.maniu.openglrecord.utils.OpenGLUtils;

public class CameraFilter1 extends AbstractFilter {
    private float[] matrix;
//fbo
    private int[] mFrameBuffers;

//    纹理
    private int[] mFrameBufferTextures;
    public CameraFilter1(Context context) {
        super(context, R.raw.camera_vert, R.raw.camera_frag);
    }
    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);

        //fbo的创建 (缓存)
        //1、创建fbo （离屏屏幕）
        mFrameBuffers = new int[1];
        // 1、创建几个fbo 2、保存fbo id的数据 3、从这个数组的第几个开始保存
        GLES20.glGenFramebuffers(mFrameBuffers.length,mFrameBuffers,0);

        //2、创建属于fbo的纹理
        mFrameBufferTextures = new int[1]; //用来记录纹理id
        //创建纹理
        OpenGLUtils.glGenTextures(mFrameBufferTextures);

//        游离的纹理

        //让fbo与 纹理发生关系
        //创建一个 2d的图像
        // 目标 2d纹理+等级 + 格式 +宽、高+ 格式 + 数据类型(byte) + 像素数据
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // 让fbo与纹理绑定起来 ， 后续的操作就是在操作fbo与这个纹理上了
        // 下面两行代码有前后关系
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        //这不是解绑的意思, 代表前面已经设置值完成
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
    }

    @Override
    public int onDraw(int textureId) {

        //设置显示窗口
        GLES20.glViewport(0, 0, mWidth, mHeight);
//        你不需要绘制到屏幕  opengl 知道    不需要绘制到屏幕, 而是绘制到 FBO 里面去
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,mFrameBuffers[0]);

        //不调用的话就是默认的操作glsurfaceview中的纹理了。显示到屏幕上了
        //这里我们还只是把它画到fbo中(缓存)
        //使用着色器
        GLES20.glUseProgram(program);
        //传递坐标
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(vPosition);

//      这句话不一样
        GLES20.glUniformMatrix4fv(vMatrix,1,false,matrix,0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //因为这一层是摄像头后的第一层，所以需要使用扩展的  GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(vTexture, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        //返回fbo的纹理id
        return mFrameBufferTextures[0];
    }
}
