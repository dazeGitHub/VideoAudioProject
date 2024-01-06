package com.maniu.openglrecord;

import android.content.Context;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.io.IOException;

public class MediaRecorder {
    private Handler mHandler;
    private final Context mContext;
    private final String mPath;
    private final int mWidth;
    private final int mHeight;

    public MediaRecorder(Context context, String path, int width, int height, EGLContext eglContext) {
        mContext = context.getApplicationContext();
        mPath = path;
        mWidth = width;
        mHeight = height;
    }

    //start  主线程
    public void start(float speed) throws IOException {
        HandlerThread handlerThread = new HandlerThread("VideoCodec");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        mHandler = new Handler(looper);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                //opengl环境
                //主 1 子线程2   调用opengl  --》  数据           编码
            }
        });
    }
}
