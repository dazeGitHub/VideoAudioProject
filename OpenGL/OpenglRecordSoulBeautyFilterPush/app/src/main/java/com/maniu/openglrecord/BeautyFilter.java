package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLES20;

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
        GLES20.glUniform1i(width, mWidth);
        GLES20.glUniform1i(height,mHeight);
    }
}
