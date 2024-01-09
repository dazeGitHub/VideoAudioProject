package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLES20;

public class ScaleFilter extends AbstractFboFilter {

    private int time=0;
    public ScaleFilter(Context context) {
        super(context, R.raw.scale_vert, R.raw.scale_frag);
//        time = GLES20.glGetUniformLocation(program,
//                "time");
    }
    @Override
    public void beforeDraw() {
        GLES20.glUniform1f(time,System.currentTimeMillis()/100000f);
    }


}
