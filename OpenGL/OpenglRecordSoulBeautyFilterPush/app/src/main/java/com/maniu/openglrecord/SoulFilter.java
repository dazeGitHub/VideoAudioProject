package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLES20;

public class SoulFilter extends AbstractFboFilter {
//顶点程序
private int mixturePercent;
//    有1
//    没有 2
    float scale = 0.0f; //缩放，越大就放的越大
//     句柄
    private int scalePercent;
    float mix = 0.0f; //透明度，越大越透明
//     直接传

//    形状
    public SoulFilter(Context context ) {
        super(context, R.raw.soul_vert, R.raw.soul_frag);
        scalePercent = GLES20.glGetUniformLocation(program, "scalePercent");
        mixturePercent = GLES20.glGetUniformLocation(program, "mixturePercent");

    }
//    会 一次 1   N次 2   摄像头   本身 循环
    public void beforeDraw() {

        GLES20.glUniform1f(scalePercent, scale + 1);
        GLES20.glUniform1f(mixturePercent, 1.0f - mix);
        scale += 0.08f;
        mix += 0.08f;
        if (scale >= 1.0) {
            scale = 0.0f;
        }
        if (mix >= 1.0) {
            mix = 0.0f;
        }
    }
}
