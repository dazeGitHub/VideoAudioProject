package com.maniu.openglbbc;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

//opengl 的学习方式 : 先知道 数据的 流动
//opengl 比较死板, 没有灵活性
//形状不需要自己画, 摄像头数据中就有形状
public class ScreenFilter {
    int program;

//  使用字符串也行, 但是规范是写到 camera_frag.glsl 文件中
//    String content = "#version 120\n" +
//            "//片元 main  坐标值\n" +
//            "varying vec2 aCoord;\n" +
//            "\n" +
//            "void main() {\n" +
//            "    texture2D();\n" +
//            "\n" +
//            "\n" +
//            "}\n";

//世界坐标系  [(-1.0, 1.0), (1,0, 1.0)
//           (-1.0, -1.0), (1.0, -1.0)], 中心是原点
//
//数据 Cpu  1  GPU  2
//    opengl  一年
//    特效    5个特效
//    VERTEX  形状    --》 vPosition

//  世界坐标系
//  矩形是由三角形组成的, 画的时候是根据世界坐标系画, 三个顶点是一个片元, 实际传 4 个点就行, opengl 会将其做为 6 个点
//  前面是 3 个点, 后面是 3 个点, 共用中间的 (1.0f, -1.0f) 和 (-1.0f, 1.0f) 两个点
    float[] VERTEX = {
        -1.0f, -1.0f, //0.0f, //也可以再加个 z 轴的坐标 0.0f, 这样就是三维坐标
        1.0f, -1.0f,  //0.0f,
        -1.0f, 1.0f,  //0.0f,
        1.0f, 1.0f,   //0.0f,
    };
//  将 CPU 中定义的 VERTEX 传递给 camera_vert.glsl 中的 vPosition

    //输出坐标系(纹理坐标系), 原点是左下点 (安卓是左上点)
    float[] TEXTURE = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };
//  在 openGL 中实现镜像是很快的, 直接反转坐标系即可, 不用和以前一样操作 yuv
//  将 CPU 中定义的 TEXTURE 传递给 camera_vert.glsl 中的 vCoord

    public ScreenFilter(Context context) {
//      opengl 要加载的代码是 vertexShader
        String vertexShader = readRawTextFile(context, R.raw.camera_vert);

//      java 源码 需要编译为 class, 再编译为 jar, 再放到虚拟机
//      顶点程序 就用 GL_VERTEX_SHADER
//      片元程序 就用 GL_FRAGMENT_SHADER
        int vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER); //opengl 面向过程, 这是静态方法
        GLES20.glShaderSource(vShader, vertexShader); //使用 opengl 2 的版本就用 GLES20

//      类似与 java 要经过 javac 编译为 class
//      opengl 也需要编译（配置）
        GLES20.glCompileShader(vShader);

//      查看是否编译成功
        int[] status = new int[1];
        GLES20.glGetShaderiv(vShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            //失败
            throw new IllegalStateException("load vertex shader:" + GLES20.glGetShaderInfoLog
                    (vShader));
        }

//      -----------------------------------------片元----------------------
        String fragShader = readRawTextFile(context, R.raw.camera_frag);
        int fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fShader, fragShader);

//      java  javac  class
        //编译（配置）
        GLES20.glCompileShader(fShader);

//      编译成功
        //查看配置 是否成功
        status = new int[1];
        GLES20.glGetShaderiv(fShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            //失败
            throw new IllegalStateException("load fragment shader:" + GLES20.glGetShaderInfoLog
                    (fShader));
        }

//      ----------------------创建一个总程序 ------------exe------
        program = GLES20.glCreateProgram(); //program 对 CPU 没有意义, 但是对 GPU 是有意义的
        //         加载顶点程序
        GLES20.glAttachShader(program, vShader);
        //         加载片元程序
        GLES20.glAttachShader(program, fShader);

        //链接着色器程序  激活状态
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
