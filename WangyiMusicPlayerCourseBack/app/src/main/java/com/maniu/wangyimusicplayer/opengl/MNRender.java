package com.maniu.wangyimusicplayer.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.maniu.wangyimusicplayer.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MNRender implements GLSurfaceView.Renderer{
//  1  坐标系          2  glsl 语言  片元    顶点程序   3  传值     4   渲染

//世界坐标系   确定形状
    private final float[] vertexData ={

            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f

    };
//    纹理坐标系哦  输出
    private final float[] textureData ={
            0f,1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;

//GPU 的变量
    private int avPosition_yuv;
    private int afPosition_yuv;



//定位GPU的位置
    private int sampler_y;
    private int sampler_u;
    private int sampler_v;



    private Context context;

//总程序 int   java 没有 GPU
    private int program_yuv;

    private int width_yuv;
    private int height_yuv;
    private ByteBuffer y;
    private ByteBuffer u;
    private ByteBuffer v;
//    三个纹理
    private int[] textureId_yuv;

    public MNRender(Context context)
    {
        this.context = context;
//        建立传送值的额通道
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(textureData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureData);
        textureBuffer.position(0);
    }
//      第一步       建立通道    2   定位   GPU   的变量     3   传值
    private void initRenderYUV()
    {
//        读取片元和顶点程序
        String vertexSource = MNShaderUtil.readRawTxt(context, R.raw.vertex_shader);
        String fragmentSource = MNShaderUtil.readRawTxt(context, R.raw.fragment_shader);

//创建总程序
        program_yuv = MNShaderUtil.createProgram(vertexSource, fragmentSource);
//定位GPU的变量
        avPosition_yuv = GLES20.glGetAttribLocation(program_yuv, "av_Position");
        afPosition_yuv = GLES20.glGetAttribLocation(program_yuv, "af_Position");


//定位片元 的变量的位置
        sampler_y = GLES20.glGetUniformLocation(program_yuv, "sampler_y");
        sampler_u = GLES20.glGetUniformLocation(program_yuv, "sampler_u");
        sampler_v = GLES20.glGetUniformLocation(program_yuv, "sampler_v");

//新建三个图层  纹理
        textureId_yuv = new int[3];
        GLES20.glGenTextures(3, textureId_yuv, 0);
        for(int i = 0; i < 3; i++)
        {
//            使用第几个图层
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId_yuv[i]);
//
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initRenderYUV();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }
//View   onDraw   渲染
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        renderYUV();


//渲染 opengl 的数据   渲染MNGLSurfaceView
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
//    cpu 1   gpu 2
    public void setYUVRenderData(int width, int height, byte[] y, byte[] u, byte[] v)
    {
        this.width_yuv = width;
        this.height_yuv = height;
// 单人床   99号

//        cpu-->gpu传递数据
        this.y = ByteBuffer.wrap(y);
        this.u = ByteBuffer.wrap(u);
        this.v = ByteBuffer.wrap(v);
        Log.d("david", "-------------->视频  " + y.length);

    }
    private void renderYUV()
    {

        if(width_yuv > 0 && height_yuv > 0 && y != null && u != null && v != null)
        {
            GLES20.glUseProgram(program_yuv);
//        OPENGL  过程        上下 代码
//        开始使用这个变量
            GLES20.glEnableVertexAttribArray(avPosition_yuv);
            GLES20.glVertexAttribPointer(avPosition_yuv, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);
//        开始使用这个变量
            GLES20.glEnableVertexAttribArray(afPosition_yuv);
            GLES20.glVertexAttribPointer(afPosition_yuv, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);
//         纹理是总层
//创建图层
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//纹理和 图层绑定
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId_yuv[0]);
//丢Y的数据  已经丢了
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                    GLES20.GL_LUMINANCE, width_yuv, height_yuv,
                    0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, y);




            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId_yuv[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width_yuv / 2, height_yuv / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, u);



            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId_yuv[2]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, width_yuv / 2, height_yuv / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, v);
//传值
            GLES20.glUniform1i(sampler_y, 0);
            GLES20.glUniform1i(sampler_u, 1);
            GLES20.glUniform1i(sampler_v, 2);

            y.clear();
            u.clear();
            v.clear();
            y = null;
            u = null;
            v = null;
        }


    }
}
