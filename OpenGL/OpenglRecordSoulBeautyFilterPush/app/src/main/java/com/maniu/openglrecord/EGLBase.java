package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
// 新的GL线程  编码耗时
//FBP  显示器
//    FBO   GlsurfceView  所在的线程

//   传递给    HandlerThread  FBO数据传递着


//     EGLDisplay    绘制 gl_FragColor ----》


//  EGLDisplay  --->
public class EGLBase {
//    mEglDisplay    放空  绘制   fbo
    private EGLDisplay mEglDisplay;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;

    private ScreenFilter mScreenFilter;
    public EGLBase(Context context, int width, int height, Surface surface, EGLContext eglContext) {
        //配置EGL环境
        createEGL(eglContext);
        //把Surface贴到  mEglDisplay ，发生关系
        int[] attrib_list = {
                EGL14.EGL_NONE
        };
        // 绘制线程中的图像 就是往这个mEglSurface 上面去画   mEglDisplay  画板   mEglSurface 画布   mEglContext 上下文
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, attrib_list, 0);
        // 绑定当前线程的显示设备及上下文， 之后操作opengl，就是在这个虚拟显示上操作
        if (!EGL14.eglMakeCurrent(mEglDisplay,mEglSurface,mEglSurface,mEglContext)) {
            throw  new RuntimeException("eglMakeCurrent 失败！");
        }
        //像虚拟屏幕画
        mScreenFilter = new ScreenFilter(context);
        mScreenFilter.setSize(width,height);


    }
//
    private void createEGL(EGLContext eglContext) {

        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }
        //初始化显示器
        int[] version = new int[2];
        // 12.1020203
        //major：主版本 记录在 version[0]
        //minor : 子版本 记录在 version[1]
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize failed");
        }

        // egl 根据我们配置的属性 选择一个配置
        int[] attrib_list = {
                EGL14.EGL_RED_SIZE, 8, // 缓冲区中 红分量 位数
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, //egl版本 2
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        // attrib_list：属性列表+属性列表的第几个开始
        // configs：获取的配置 (输出参数)
        //num_config: 长度和 configs 一样就行了
        if (!EGL14.eglChooseConfig(mEglDisplay, attrib_list, 0,
                configs, 0, configs.length, num_config, 0)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }


        mEglConfig = configs[0];
        int[] ctx_attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, //egl版本 2
                EGL14.EGL_NONE
        };
        //创建EGL上下文
        // 3 share_context: 共享上下文 传绘制线程(GLThread)中的EGL上下文 达到共享资源的目的 发生关系
        mEglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, eglContext, ctx_attrib_list
                , 0);
        // 创建失败
        if (mEglContext == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("EGL Context Error.");
        }
    }
//    假设外部已经把  textureId 给你了  不断地额在调用 textureId  数据
    public void draw(int textureId,long timestamp){
//textureId   --->数据     硬解 byte[]     int   gpu
//mEglSurface  opengl  东西   android   surface   cpu 的
//         gpu eglSwapBuffers
        // 绑定当前线程的显示设备及上下文， 之后操作opengl，就是在这个虚拟显示上操作
        if (!EGL14.eglMakeCurrent(mEglDisplay,mEglSurface,mEglSurface,mEglContext)) {
            throw  new RuntimeException("eglMakeCurrent 失败！");
        }
        //画画 画到虚拟屏幕上  上一个环境FBO 的数据   纹理ID   -----》  sccreenfilter 虚拟滤镜
        mScreenFilter.onDraw(textureId);

        //刷新eglsurface的时间戳
        EGLExt.eglPresentationTimeANDROID(mEglDisplay,mEglSurface,timestamp);

//        ？ 交换surface
        EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);

    }


    public void release() {
//        销毁  mEglSurface  和  mEglDisplay绑定关系
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
        EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);
//          mEglDisplay    设值成没有上下文
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
//          mEglDisplay   mEglContext 进行销毁
        EGL14.eglReleaseThread();
//         关机  mEglDisplay
        EGL14.eglTerminate(mEglDisplay);
    }
}
