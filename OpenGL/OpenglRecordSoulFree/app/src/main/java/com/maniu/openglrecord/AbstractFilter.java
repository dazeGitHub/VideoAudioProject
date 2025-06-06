package com.maniu.openglrecord;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;

import android.content.Context;
import android.opengl.GLES20;

import com.maniu.openglrecord.utils.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class AbstractFilter {
    float[] VERTEX = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };

    float[] TEXTURE = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };


    protected int vMatrix;


    public int program;
    //句柄  gpu中  vPosition
    public    int vPosition;
    public   int vCoord;
    public   int vTexture;

    FloatBuffer vertexBuffer; //顶点坐标缓存区

    FloatBuffer textureBuffer; // 纹理坐标
    public int mWidth;
    public int mHeight;



    public AbstractFilter(Context context, int vertexShaderId, int fragmentShaderId) {
        String vertexSharder = OpenGLUtils.readRawTextFile(context, vertexShaderId);
//  先编译    再链接   再运行  程序
        String fragSharder = OpenGLUtils.readRawTextFile(context, fragmentShaderId);

        program = OpenGLUtils.loadProgram(vertexSharder, fragSharder);

        vPosition = GLES20.glGetAttribLocation(program, "vPosition");//0
        //接收纹理坐标，接收采样器采样图片的坐标
        vCoord = GLES20.glGetAttribLocation(program, "vCoord");//1
        //采样点的坐标
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");
        vMatrix = GLES20.glGetUniformLocation(program,
                "vMatrix");

//        建立通道

        vertexBuffer =  ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);

        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);


    }
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;




    }
//    图片 纹理   责任链    上一个传递给你的是哪个纹理ID
    public int onDraw(int texture) {
        GLES20.glViewport(0, 0, mWidth, mHeight);
//        使用程序
        GLES20.glUseProgram(program);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition,2,GL_FLOAT, false,0,vertexBuffer);
//        生效
        GLES20.glEnableVertexAttribArray(vPosition);


        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT,
                false, 0, textureBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);


        GLES20.glActiveTexture(GL_TEXTURE0);

//生成一个采样  纹理使用的是什么采样器 ，两种 1   普通采样  限定在GPU
//          GL_TEXTURE_2D  GPU内存1     GL_TEXTURE_EXTERNAL_OES 外接设备
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(vTexture, 0);

        beforeDraw();
//真正开始渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        return texture;

    }
    public void beforeDraw() {

    }


    public void release(){
        GLES20.glDeleteProgram(program);
    }



}
