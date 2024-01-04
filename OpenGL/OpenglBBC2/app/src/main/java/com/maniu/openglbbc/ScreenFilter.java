package com.maniu.openglbbc;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

//  数据  流动
//opengl   死板    灵活性
public class ScreenFilter {
    int program;
//    String conten = "#version 120\n" +
//            "//片元 main  坐标值\n" +
//            "varying vec2 aCoord;\n" +
//            "\n" +
//            "void main() {\n" +
//            "    texture2D();\n" +
//            "\n" +
//            "\n" +
//            "}\n";
//世界坐标系花   数据 Cpu  1  GPU  2
//数据
//    opengl  一年
//    特效    5个特效
//    VERTEX  形状    --》 vPosition
    float[] VERTEX = {
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,

        1.0f, 1.0f
    };
    //    输出坐标系
    float[] TEXTURE = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };
//gpu    地址
    private   int vPosition;
    private   int vCoord;

    private   int vTexture;
    private   int vMatrix;
    FloatBuffer vertexBuffer;
    FloatBuffer textureBuffer; // 纹理坐标
    public ScreenFilter(Context context) {
// opengl加载

        String vertexSharder = readRawTextFile(context, R.raw.camera_vert);

//        java 源码  ---class --》 jar   ---》虚拟机
// 顶点程序   gpu 创建顶点程序
//        片元程序  opengl是面向过程的
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vShader, vertexSharder);
//java  javac  class
        //编译（配置）
        GLES20.glCompileShader(vShader);
//编译成功
        //查看配置 是否成功
        int[] status = new int[1];
        GLES20.glGetShaderiv(vShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            //失败
            throw new IllegalStateException("load vertex shader:" + GLES20.glGetShaderInfoLog
                    (vShader));
        }
//        -----------------------------------------片元----------------------
        String fragSharder =  readRawTextFile(context, R.raw.camera_frag);
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragSharder);
//java  javac  class
        //编译（配置）
        GLES20.glCompileShader(fShader);
//编译成功
        //查看配置 是否成功
        status = new int[1];
        GLES20.glGetShaderiv(fShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            //失败
            throw new IllegalStateException("load fragment shader:" + GLES20.glGetShaderInfoLog
                    (fShader));
        }
//----------------------创建一个总程序------------exe------
        program=GLES20.glCreateProgram();
        //         加载顶点程序
        GLES20.glAttachShader(program, vShader);
        //         加载片元程序
        GLES20.glAttachShader(program, fShader);

        //链接着色器程序  gou 激活状态
        GLES20.glLinkProgram(program);
//第一步建立ByteBuffer
        vertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);



        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);
    }
//    渲染一次  N次
    public void onDraw(int mWidth,int mHeight,float[] mtx,int textures) {
//        GPU
        GLES20.glViewport(0, 0, mWidth, mHeight);

        GLES20.glUseProgram(program);
//        原因  第二次读  重新读
        vertexBuffer.position(0);
        textureBuffer.position(0);
        //定位到GPU的变量地址
//定位到GPU的变量地址
        vPosition=  GLES20.glGetAttribLocation(program, "vPosition");
        vCoord=  GLES20.glGetAttribLocation(program, "vCoord");
//        定位到 GPu 的片元程序的 变量vTexture
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");
        //变换矩阵， 需要将原本的vCoord（01,11,00,10） 与矩阵相乘
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");
//将cpu的数据 传给GPU vPosition
//        1    GPU的 变量地址
//        2          每个坐标  有几个值组成
//        3

//
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//        GPU启动 了变量
        GLES20.glEnableVertexAttribArray(vPosition);

//-------------------------------------vCoord------------------------------
//
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
//        GPU启动 了变量
        GLES20.glEnableVertexAttribArray(vCoord);

//
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        绑定一个采样器  摄像头的内容  textures
        GLES20.glBindTexture(GLES20.GL_TEXTURE0, textures);

//        传值    片元  变量    不需要
//        定位到  片元的变量 第一次  懵 opengl  输出视频
//        GPU  摄像头 采集数据    现在第几个图层
        GLES20.glUniform1i(vTexture, 0);
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

//        通知GPU渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    }

//    opengl  1  确定形状   栅格化    渲染     显示
    public   String readRawTextFile(Context context, int rawId) {
        InputStream is = context.getResources().openRawResource(rawId);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder sb = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


}
