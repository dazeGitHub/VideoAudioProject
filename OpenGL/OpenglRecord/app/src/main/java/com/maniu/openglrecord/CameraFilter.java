package com.maniu.openglrecord;

import static android.opengl.GLES20.GL_TEXTURE0;

import android.content.Context;
import android.opengl.GLES20;

public class CameraFilter  extends AbstractFilter {
    private float[] mtx;
    private int vMatrix;

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vert, R.raw.camera_frag);
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");
    }

    @Override
    public void beforeDraw() {
        GLES20.glActiveTexture(GL_TEXTURE0);
        //在 camera_vert.vert 中的 vMatrix 就能接收到值了
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);
    }

    public void setTransformMatrix(float[] mtx) {
        this.mtx = mtx;
    }
}
