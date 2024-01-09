package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLES20;

/**
 * 美颜滤镜
 */
public class BeautyFilter  extends  AbstractFboFilter{

    private int width;
    private int height;
    public BeautyFilter(Context context) {
        super(context, R.raw.beauty_vert, R.raw.beauty_frag2);
        width = GLES20.glGetUniformLocation(program, "width");
        height = GLES20.glGetUniformLocation(program, "height");
    }

    @Override
    public void beforeDraw() {
        super.beforeDraw();
        GLES20.glUniform1i(width, mWidth);  //将 CPU 中的 mWidth 传递给 width
        GLES20.glUniform1i(height,mHeight);
    }
}
