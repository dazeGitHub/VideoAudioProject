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
    int program; //可理解为GPU 中的程序
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
//  正常画面
    float[] VERTEX = {
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, 1.0f
    };

//  三角形画面
//    float[] VERTEX = {
//            1.0f, -1.0f,
//            -1.0f, -1.0f,
//            -1.0f, 1.0f,
//            1.0f, 1.0f
//    };

//  旋转画面
//    float[] VERTEX = {
//            -1.0f, -1.0f,
//            -1.0f, 1.0f,
//            1.0f, -1.0f,
//            1.0f, 1.0f
//    };

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
//      camera_frag.glsl    //camera_frag_ver_split_screen.glsl
        String fragSharder =  readRawTextFile(context, R.raw.camera_frag_ver_split_screen);
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
        program = GLES20.glCreateProgram();
        //         加载顶点程序
        GLES20.glAttachShader(program, vShader);
        //         加载片元程序
        GLES20.glAttachShader(program, fShader);

        //链接着色器程序  gou 激活状态
        GLES20.glLinkProgram(program);

//      1. 第一步建立ByteBuffer
//      传递 CPU 中的 VERTEX 到 GPU 中的 camera_vert.glsl 的 vPosition
//      4 个坐标 * 每个坐标2个字节 * 每个点的值是4 (什么意思), order(ByteOrder.nativeOrder()) 方法表示以 native 的方式给内存排序,
        vertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);

//      传递 CPU 中的 TEXTURE 到 GPU 中的 camera_vert.glsl 的 vCoord
        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);
    }

//    渲染一次  N次, 当摄像头出现一帧画面的时就调用该方法
    public void onDraw(int mWidth, int mHeight, float[] mtx, int textures) {
//        GPU
        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glUseProgram(program);

//      防止第二次读取不到
        vertexBuffer.position(0);
        textureBuffer.position(0);

        //2. 定位到GPU的变量地址
//定位到GPU的变量地址
        vPosition =  GLES20.glGetAttribLocation(program, "vPosition");
        vCoord =  GLES20.glGetAttribLocation(program, "vCoord");

//      定位到 GPU 的片元程序的 变量 vTexture
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");
        //变换矩阵， 需要将原本的vCoord（01,11,00,10） 与矩阵相乘
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");

//      3. 通过 GLES20.glVertexAttribPointer(v) 设置 GPU 中的 值, /将cpu的数据 传给GPU vPosition
//      参数一: GPU 的变量地址
//      参数二: 每个坐标是几个值(如果是3D 就传3)
//      参数三: 数据类型 浮点型
//      参数四: normalized
//      参数五: stride
//      参数六: vertexBuffer
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//      传递完值, 需要启动变量
        GLES20.glEnableVertexAttribArray(vPosition);

//-------------------------------------vCoord------------------------------
//
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
//        GPU启动 了变量
        GLES20.glEnableVertexAttribArray(vCoord);

//      激活图层0, 从 GL_TEXTURE0 到 GL_TEXTURE31 共 32 个图层
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//      绑定一个采样器  摄像头的内容  textures
        GLES20.glBindTexture(GLES20.GL_TEXTURE0, textures);

//      绑定图层
//      vTexture 是传递给片元的变量
//      传值    片元  变量    不需要
//      定位到  片元的变量
//      传 0 表示 摄像头 采集的数据 在 GPU 第 0 个图层 (最上层的图层)
        GLES20.glUniform1i(vTexture, 0);

//      将 cpu 中的矩阵 mtx 传递给 gpu 中的矩阵 vMatrix
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

//      通知 GPU 渲染, GL_TRIANGLE_STRIP 表示以三个顶点进行渲染, 0 表示第一个坐标, 4 表示坐标个数
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
