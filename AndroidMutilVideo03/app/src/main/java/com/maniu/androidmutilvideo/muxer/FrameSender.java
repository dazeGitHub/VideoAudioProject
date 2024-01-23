package com.maniu.androidmutilvideo.muxer;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import com.maniu.androidmutilvideo.muxer.pool.BufferInfoEx;
import com.maniu.androidmutilvideo.muxer.pool.FramePool;

import java.util.LinkedList;
import java.util.List;

public class FrameSender {
    private static final int KEEP_COUNT = 30;
    private static final int MESSAGE_READY_TO_CLOSE = 4;
    private static final int MSG_ADD_FRAME = 3;
    private static final int MSG_START = 2;
    private List<FramePool.Frame> frameQueue = new LinkedList<>();
    private FramePool framePool = new FramePool(KEEP_COUNT + 10);

    private Handler sendHandler;

    private FrameSenderCallback frameSenderCallback;
    public FrameSender(final FrameSenderCallback frameSenderCallback) {
        this.frameSenderCallback = frameSenderCallback;

        final HandlerThread sendHandlerThread = new HandlerThread("out_thread");
        sendHandlerThread.start();
        sendHandler = new Handler(sendHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == MSG_START) {
//                  输出线程
                    frameSenderCallback.onStart();
                }else  if (msg.what == MSG_ADD_FRAME) {
//                      输出线程
                    if (msg.obj != null) {
                        addFrame((FramePool.Frame) msg.obj);
                    }
                    sendFrame(msg.arg1);
                }else   if (msg.what == MESSAGE_READY_TO_CLOSE) {
                    if (msg.obj != null) {
                        addFrame((FramePool.Frame) msg.obj);
                    }
                    sendFrame(msg.arg1);
                    frameSenderCallback.close();
                    sendHandlerThread.quitSafely();
                }
            }
        };
    }

    private void addFrame(FramePool.Frame frame) {
        frameQueue.add(frame);
        FramePool.Frame.sortFrame(frameQueue);
    }

    private void sendFrame(int keepCount) {
        FramePool.Frame sendFrame = frameQueue.remove(0);
        if (sendFrame.type == FramePool.Frame.TYPE_VIDEO) {
            frameSenderCallback.onSendVideo(sendFrame);
        } else if(sendFrame.type == FramePool.Frame.TYPE_AUDIO) {
            frameSenderCallback.onSendAudio(sendFrame);
        }
    }

    public void sendCloseMessage() {
        Message message = Message.obtain();
        message.arg1 = 0;
        message.what = MESSAGE_READY_TO_CLOSE;
        sendHandler.sendMessage(message);
    }

//    发送一个编码视频帧的  消息
//    data
    public void sendAddFrameMessage(byte[] data, int offset, int length, BufferInfoEx bufferInfo, int type) {
//        为了防止 发生内存抖动
        FramePool.Frame frame = framePool.obtain(data, offset, length, bufferInfo, type);
        Message message = Message.obtain();
        message.what = MSG_ADD_FRAME;
        message.arg1 = KEEP_COUNT;
        message.obj = frame;
        sendHandler.sendMessage(message);
    }

    public void sendStartMessage() {
        Message message = Message.obtain();
        message.what = MSG_START;
        sendHandler.sendMessage(message);//发生一个开始消息
    }

    public interface FrameSenderCallback {
        void onStart();
        void close();
        void onSendVideo(FramePool.Frame sendFrame);
        void onSendAudio(FramePool.Frame sendFrame);

    }
}
