package com.maniu.androidmutilvideo.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.chillingvan.canvasgl.util.Loggers;
import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;
import com.maniu.androidmutilvideo.muxer.interfaces.BaseMuxer;
import com.maniu.androidmutilvideo.muxer.interfaces.IMuxer;
import com.maniu.androidmutilvideo.muxer.pool.BufferInfoEx;
import com.maniu.androidmutilvideo.muxer.pool.FramePool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

//输出播放的文件
//如果能输出到 rtmp服务器, 那么也能输出到 音视频通话 音视频会议
public class MP4Muxer extends BaseMuxer {
    public static final int TYPE_VIDEO = 1;
    public static final int TYPE_AUDIO = 2;

    MediaMuxer mMuxer;//用该对象就能输出 mp4
//    轨道数量
    private int trackCnt = 0;

    private boolean isStart;
    private Integer videoTrackIndex = null;
    private Integer audioTrackIndex = null;
    private StreamPublisherParam params;
    private FrameSender frameSender;
    @Override
    public int open(StreamPublisherParam params) {

        isStart = false;
        trackCnt = 0;
        videoTrackIndex = null;
        audioTrackIndex = null;
        this.params = params;

        try {
            mMuxer = new MediaMuxer(params.outputFilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        frameSender = new FrameSender(new FrameSender.FrameSenderCallback() {
            @Override
            public void onStart() {
                mMuxer.start();
            }

            @Override
            public void close() {
                isStart = false;
                if (mMuxer != null) {
                    mMuxer.stop();
                    mMuxer.release();
                    mMuxer = null;
                }
            }

            @Override
            public void onSendVideo(FramePool.Frame sendFrame) {
                if (isStart) {
                    mMuxer.writeSampleData(videoTrackIndex, ByteBuffer.wrap(sendFrame.data), sendFrame.bufferInfo.getBufferInfo());
                }
            }

            @Override
            public void onSendAudio(FramePool.Frame sendFrame) {
                if (isStart) {
                    mMuxer.writeSampleData(audioTrackIndex, ByteBuffer.wrap(sendFrame.data), sendFrame.bufferInfo.getBufferInfo());
                }
            }
        });
        return 0;
    }

    @Override
    public void writeVideo(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
        addTrackAndReadyToStart(TYPE_VIDEO);
//        发送一个
        frameSender.sendAddFrameMessage(buffer, offset, length,
                new BufferInfoEx(bufferInfo, videoTimeIndexCounter.getTimeIndex()),
                FramePool.Frame.TYPE_VIDEO);
    }
// 直播推流 主持人
    private void addTrackAndReadyToStart(int type) {
//        视频轨没有初始化
        if (videoTrackIndex == null && type ==TYPE_VIDEO) {
            MediaFormat videoOutputMediaFormat = params.getVideoOutputMediaFormat();
            videoTrackIndex=mMuxer.addTrack(videoOutputMediaFormat);
            trackCnt++;
        } else if (audioTrackIndex == null && type == TYPE_AUDIO) {
            MediaFormat audioOutputMediaFormat = params.getAudioOutputMediaFormat();
            audioTrackIndex = mMuxer.addTrack(audioOutputMediaFormat);
            trackCnt++;
        }
//      如果 trackCnt 等于 2, 说明 视频轨 和 音频轨  已经添加了(初始化好了)
//
//      这里属于编码线程, 所以不能在这里调用, 编码线程 和 输入线程 绝对不能有交集
//        mMuxer.start();
        if (trackCnt == 2) {
            frameSender.sendStartMessage();
        }
    }


    @Override
    public void writeAudio(byte[] buffer, int offset, int length, MediaCodec.BufferInfo bufferInfo) {
//        byte[] buffer1 = new byte[buffer.length];
//        list.add(buffer1);
        addTrackAndReadyToStart(FramePool.Frame.TYPE_AUDIO);
        frameSender.sendAddFrameMessage(buffer, offset, length, new BufferInfoEx(bufferInfo, audioTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_AUDIO);

    }

    @Override
    public int close() {
        if (frameSender != null) {
            frameSender.sendCloseMessage();
        }
        return 0;
    }
}
