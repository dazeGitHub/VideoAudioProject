package com.maniu.openglrecord;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.maniu.openglrecord.utils.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaEncoderAndPush extends Thread  {

    static {
        System.loadLibrary("native-lib");
    }
    private   int mWidth;
    private   int mHeight;
    MediaCodec mediaCodec;
//    Mediacodec   Opnegl  桥梁    提供    ---》opengl  交换
private   Context mContext;
    private EGLContext mGlContext;

//  mSurface 是 MediaCodec 和 opengl 的桥梁, Mediacodec 只管输出, opengl 负责输入渲染到 surface 里面
    private Surface mSurface;
    private Handler mHandler;
    EGLBase eglEnv;
    private boolean isStart;

    private long timeStamp;
    private long startTime;

    private String url = "rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_524987038_52393108&key=60c1c9c16504355c743812b4c3ceb28c&schedule=rtmp&pflag=1";
    public MediaEncoderAndPush(Context context, EGLContext glContext, int width, int
            height) {
        mContext = context.getApplicationContext();
        mWidth = width;
        mHeight = height;
        mGlContext = glContext;
        start();
    }
    @Override
    public void run() {
        //1推送到
        if (!connect(url)) {
            Log.i("david", "run: ----------->推送失败");
            return;
        }
    }
    public void startPush( ) throws IOException {


        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,
                mWidth, mHeight);
        //颜色空间 从 surface当中获得
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
                .COLOR_FormatSurface);
        //码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        //帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        //关键帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        mediaCodec = MediaCodec.createEncoderByType("video/avc");
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//         mediacode 提供一个场地
        mSurface=  mediaCodec.createInputSurface();
        mediaCodec.start();


        HandlerThread handlerThread = new HandlerThread("codec-gl");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                eglEnv =  new EGLBase(mContext, mWidth, mHeight, mSurface, mGlContext);
                isStart = true;
            }
        });

    }

    public void encodeFrame(final int textureId, final long timestamp) {

        if (!isStart) {
            return;
        }

        mHandler.post(new Runnable() {
            public void run() {
                eglEnv.draw(textureId,timestamp);
                codec();
            }
        });
    }
    private void codec( ) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int index =mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        if (index >= 0) {
            ByteBuffer buffer = mediaCodec.getOutputBuffer(index);
//            有特效的
            byte[] outData = new byte[bufferInfo.size];

            buffer.get(outData);
            if (startTime == 0) {
                // 微妙转为毫秒
                startTime = bufferInfo.presentationTimeUs / 1000;
            }

            FileUtils.writeBytes(outData);
            FileUtils.writeContent(outData);
            long pts=(bufferInfo.presentationTimeUs / 1000) - startTime;
            sendData(outData, outData.length, pts);
            mediaCodec.releaseOutputBuffer(index, false);
//            7天  听
        }





    }

    private native boolean connect(String url);

    private native boolean sendData(byte[] data, int len, long tms);

}
