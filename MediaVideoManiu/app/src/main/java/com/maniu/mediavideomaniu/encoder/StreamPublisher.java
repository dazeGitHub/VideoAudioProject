package com.maniu.mediavideomaniu.encoder;

import android.media.MediaCodec;

import com.maniu.mediavideomaniu.encoder.audio.AACEncoder;
import com.maniu.mediavideomaniu.encoder.video.H264Encoder;
import com.maniu.mediavideomaniu.muxer.interfaces.IMuxer;

public class StreamPublisher {

    private IMuxer muxer;
//不编码
    private H264Encoder h264Encoder;

    private AACEncoder aacEncoder;
    public void prepareEncoder(  StreamPublisherParam param) {
        h264Encoder = new H264Encoder(param);
        aacEncoder = new AACEncoder(param);
        aacEncoder.setOnDataComingCallback(new AACEncoder.OnDataComingCallback() {
            byte[] writeBuffer = new byte[param.audioBitRate / 8];
//            录音器  录到声音  时机  是在   已经 把pcm 的数据 放到了编码器中
            @Override
            public void onComing() {
//              录音器录制到声音后就会调用 onComing() 方法
                MediaCodecInputStream mediaCodecInputStream = aacEncoder.getMediaCodecInputStream();
//                是我们主动发起读数据的请求 ， 读完 就回调  onReadOnce
                mediaCodecInputStream.readAll(writeBuffer,new MediaCodecInputStream.OnReadAllCallback(){
                    @Override
                    public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                        if (readSize <= 0) {
                            return;
                        }

//                        音视频 剪辑   分装成视频文件
                        muxer.writeAudio(buffer, 0, readSize, bufferInfo);
                    }
                });

            }
        });
//        视频什么获取  输出  什么时候  输出
        MediaCodecInputStream mediaCodecInputStream = h264Encoder.getMediaCodecInputStream();
         byte[] writeBuffer = new byte[param.videoBitRate / 8 / 2];
        mediaCodecInputStream.readAll(writeBuffer, new MediaCodecInputStream.OnReadAllCallback() {
            @Override
            public void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo bufferInfo) {
                if (readSize <= 0) {
                    return;
                }
                muxer.writeVideo(buffer, 0, readSize, bufferInfo);

            }
        });



    }

}
