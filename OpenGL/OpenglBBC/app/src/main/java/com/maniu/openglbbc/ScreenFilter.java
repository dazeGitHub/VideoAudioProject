package com.maniu.openglbbc;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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



    }
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
