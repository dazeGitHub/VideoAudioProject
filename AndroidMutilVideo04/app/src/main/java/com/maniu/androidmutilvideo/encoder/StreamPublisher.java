package com.maniu.androidmutilvideo.encoder;

import android.media.MediaCodec;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.util.Loggers;
import com.maniu.androidmutilvideo.FileUtils;
import com.maniu.androidmutilvideo.encoder.audio.AACEncoder;
import com.maniu.androidmutilvideo.encoder.video.H264Encoder;
import com.maniu.androidmutilvideo.muxer.interfaces.IMuxer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StreamPublisher {

    private IMuxer muxer;
//不编码
    private H264Encoder h264Encoder;

    private AACEncoder aacEncoder;
    private EglContextWrapper eglCtx;
    private StreamPublisherParam param;
    private boolean isStart;

    private Handler writeVideoHandler;
    private HandlerThread writeVideoHandlerThread;
    public StreamPublisher(EglContextWrapper eglCtx, IMuxer muxer) {

        this.eglCtx = eglCtx;
        this.muxer = muxer;


    }
//开始编码
    public void start() throws IOException {
        if (!isStart) {
            if (muxer.open(param) <= 0) {
                Loggers.e("StreamPublisher", "muxer open fail");
                throw new IOException("muxer open fail");
            }

            h264Encoder.start();
            aacEncoder.start();
            isStart = true;
        }
    }
    public void prepareEncoder(  StreamPublisherParam param, H264Encoder.OnDrawListener onDrawListener) {
        this.param = param;
        h264Encoder = new H264Encoder(param,eglCtx);
        for (GLTexture texture :sharedTextureList ) {
            h264Encoder.addSharedTexture(texture);
        }
//        新加
        h264Encoder.setOnDrawListener(onDrawListener);

        aacEncoder = new AACEncoder(param);
        aacEncoder.setOnDataComingCallback(new AACEncoder.OnDataComingCallback() {
            byte[] writeBuffer = new byte[param.audioBitRate / 8];
//            录音器  录到声音  时机  是在   已经 把pcm 的数据 放到了编码器中
            @Override
            public void onComing() {

                MediaCodecInputStream mediaCodecInputStream = aacEncoder.getMediaCodecInputStream();
//                是我们主动发起读数据的请求 ， 读完 就回调  onReadOnce
                mediaCodecInputStream.readAll(writeBuffer,new MediaCodecInputStream.OnReadAllCallback(){
                    @Override
                    public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                        if (readSize <= 0) {
                            return;
                        }
                        Log.d("david", "-------> aacEncoder  onReadOnce");

//                        音视频 剪辑   分装成视频文件
                        muxer.writeAudio(buffer, 0, readSize, bufferInfo);
                    }
                });

            }
        });
        writeVideoHandlerThread = new HandlerThread("WriteVideoHandlerThread");
        writeVideoHandlerThread.start();

        writeVideoHandler = new Handler(writeVideoHandlerThread.getLooper()){
//全局的
             byte[] writeBuffer = new byte[param.videoBitRate / 8 / 2];
//1 s   60次
            @Override
            public void handleMessage(@NonNull Message msg) {
//                当我们handlerMessager被执行的时候    执行现在 HandlerThread
//        视频什么获取  输出  什么时候  输出
                MediaCodecInputStream mediaCodecInputStream = h264Encoder.getMediaCodecInputStream();


//                会  1  不会2
                mediaCodecInputStream.readAll(writeBuffer, new MediaCodecInputStream.OnReadAllCallback() {
                    @Override
                    public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                        if (readSize <= 0) {
                            return;
                        }
                        FileUtils.writeBytes(buffer);
                        FileUtils.writeContent(buffer);
//                        MP4文件  能1  不能2
                        Log.d("david", "-------> h264Encoder  onReadOnce");
                        muxer.writeVideo(buffer, 0, readSize, bufferInfo);
                    }
                });
            }
        };










    }

    public void addSharedTexture(GLTexture outsideTexture) {
        sharedTextureList.add(outsideTexture);
    }
    private List<GLTexture> sharedTextureList = new ArrayList<>();

    public boolean drawAFrame() {
        if (isStart) {
//            请求刷新     canvas  渲染后数据
            h264Encoder.requestRender();
// 当前线程   能1  不能2  阻塞渲染线程  随便发送有一个消息
            writeVideoHandler.sendEmptyMessage(1);
            return true;
        }
        return false;
    }

    public boolean isStart() {
        return isStart;
    }

    public void close() {
        isStart = false;
        if (h264Encoder != null) {
            h264Encoder.close();
        }

        if (aacEncoder != null) {
            aacEncoder.close();
        }

        if (muxer != null) {
            muxer.close();
        }


    }
}
