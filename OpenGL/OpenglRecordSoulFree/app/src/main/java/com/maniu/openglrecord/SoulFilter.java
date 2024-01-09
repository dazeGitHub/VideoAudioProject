package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLES20;

public class SoulFilter extends AbstractFboFilter {
    private int scalePercent;
    float scale = 0.0f; //缩放，越大就放的越大
    private int mixturePercent;
    float mix = 0.0f; //透明度，越大越透明

    public SoulFilter(Context context) {
        super(context, R.raw.base_vert, R.raw.soul_frag); //顶点程序没有变化(形状就是矩形) 所以使用 R.raw.base_vert
//从gpu 拿到    句柄
        scalePercent = GLES20.glGetUniformLocation(program, "scalePercent");
        mixturePercent = GLES20.glGetUniformLocation(program, "mixturePercent");
    }

//摄像头   会调用  不断的调用   scale越来大
//建议做
    @Override
    public void beforeDraw() {
        super.beforeDraw();
        GLES20.glUniform1f(scalePercent, scale + 1.0f);//scale + 1.0f 最大是 2, 即最大放大两倍
//        1  -  0     1减到 0
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

    @Override
    public int onDraw(int texture) {
         super.onDraw(texture);
//        到两倍终止  片元程序
        return mFrameBuffers[0];
    }
}
