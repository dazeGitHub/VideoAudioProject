package com.maniu.openglimage;

import static android.opengl.GLES20.GL_TEXTURE_2D;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

//加载opengl程序
public class ImageFilter {

//  对 CPU 无用, 对 GPU 才有意义
    protected int mProgId;

//  世界坐标系
//  只画一个矩形, 所以有 4 个点就行了, 如果图像很复杂, 就需要多个坐标点(可以通过工具生成)
    static final float COORD1[] = {
            -1.0f, -1.0f, //左下角的点
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
    };

//  纹理坐标系 (Android 布局坐标系)
    static final float TEXTURE_COORD1[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
    };

//  这个是测试书上写的 (错误的), 坐标原点在左下角
//    static final float TEXTURE_COORD1[] = {
//            0.0f, 0.0f,
//            1.0f, 0.0f,
//            0.0f, 1.0f,
//            1.0f, 1.0f,
//    };
//    视频

    protected int mInputTexture;
    private String mVertexShader;
    private String mFragmentShader;

    private FloatBuffer mPositionBuffer;
    private FloatBuffer mCoordBuffer;
    //对 CPU 无用, 对 GPU 才有意义
    protected int mPosition;
    //顶点程序
    protected int vCoord;

    public ImageFilter(Context context) {
        this(OpenGLUtils.readRawTextFile(context,R.raw.base_vert), OpenGLUtils.readRawTextFile(context,R.raw.base_frag));

    }
    public ImageFilter(String vertexShader, String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    public void initShader() {
//      加载程序    mProgId    总程序 在哪里
        mProgId = OpenGLUtils.loadProgram(mVertexShader, mFragmentShader);

//      找到 gpu 的变量的步骤 :
//       、1. 定位变量在GPU的位置
//         2. FLoteBuffer   建立通道,   COORD1  传给  FLoteBuffer
//         3. 把 FLoteBuffer 给到 GPU 的变量   1  初始化1     ondrawFrame2

        mPosition = GLES20.glGetAttribLocation(mProgId, "vPosition");
        vCoord = GLES20.glGetAttribLocation(mProgId, "vCoord");
        mInputTexture = GLES20.glGetUniformLocation(mProgId, "inputImageTexture");
    }

    //加载顶点程序
    public void loadVertex() {
        float[] coord = COORD1;
        float[] texture_coord = TEXTURE_COORD1;

        mPositionBuffer = ByteBuffer.allocateDirect(coord.length * 4) //base_vert.vert 中 vec4 表示 4 个字节
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mPositionBuffer.put(coord).position(0);

        mCoordBuffer = ByteBuffer.allocateDirect(texture_coord.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mCoordBuffer.put(texture_coord).position(0);
    }

    public int  init(Bitmap bitmap) {
        initShader();
        loadVertex();
        return initTexture(bitmap);
    }

//  生成纹理, 用来接收 Bitmap
    private int initTexture(Bitmap bitmap) {
        int[] textures = new int[1];

//      创建纹理
        GLES20.glGenTextures(1, textures, 0); //(生成几个纹理, 纹理数组, 偏移量)

//      使用纹理
        GLES20.glBindTexture(GL_TEXTURE_2D, textures[0]);

//      下面针对 GLES20  的操作都是针对与 textures 纹理
//      GL_NEAREST 产生了颗粒状的图案, 能够清晰看到组成纹理的像素, 而 GL_LINEAR能够产生更加平滑的图案, 很难看出单个的纹理像素。GL_LINEAR 可以产生更真实的输出
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR); //缩小时选择 GLES20.GL_LINEAR

//      设置纹理属性 (例如 环绕方式, 展示内容)
//      第一个参数: 代表纹理目标类型, 使用的是2D纹理, 因此纹理目标是 GL_TEXTURE_2D
//      第二个参数: 指定设置的选项与应用的纹理轴。s为水平方向, t为垂直方向。
//      第三个参数: 传递一个环绕方式, 在这个例子中 OpenGL 会给当前激活的纹理设定纹理环绕方式为 GL_MIRRORED_REPEAT。
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

//      有请 bitmap
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

//      openGL 的原理 : 将 Bitmap 采样到矩形中去
        return textures[0];
    }

    public int drawFrame(int glTextureId){
        GLES20.glUseProgram(mProgId);
//      传值
        mPositionBuffer.position(0); //防止从之前的最后读取, 重置为 0
//      将数据从 mPositionBuffer 传递给 gpu mPosition, 这样 gpu 中就能受到 cpu 中的形状数据
        GLES20.glVertexAttribPointer(mPosition, 2, GLES20.GL_FLOAT, false, 0, mPositionBuffer);//size 如果是二维的是 2, 立体的是 3
        GLES20.glEnableVertexAttribArray(mPosition);

        mCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mCoordBuffer);
        GLES20.glEnableVertexAttribArray(vCoord);

//      图层和纹理进行绑定
        GLES20.glActiveTexture(0);//激活第 0 个图层
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, glTextureId);//绑定纹理
        GLES20.glUniform1i(mInputTexture, 0);

//      绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 把第0图层  解绑纹理的意思
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        return glTextureId;
    }
}
