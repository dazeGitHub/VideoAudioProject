package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.maniu.openglrecord.utils.OpenGLUtils;

public class CameraFilter extends AbstractFboFilter {



//     有 1  没有2
    private float[] matrix;

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vert, R.raw.camera_frag);
        textureBuffer.clear();
    }
    @Override
    public void beforeDraw() {
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, matrix, 0);
    }

    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }
}
