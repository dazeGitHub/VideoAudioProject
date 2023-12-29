package com.maniu.h265maniupush;

import static android.media.MediaFormat.KEY_BIT_RATE;
import static android.media.MediaFormat.KEY_FRAME_RATE;
import static android.media.MediaFormat.KEY_I_FRAME_INTERVAL;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.view.Surface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CodecLiveH265 extends Thread {
    //    录屏
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private int width = 720;
    private int height = 1280;

    public static final int NAL_I = 19;
    public static final int NAL_VPS = 32;
    VirtualDisplay virtualDisplay;
    private byte[] vps_sps_pps_buf;
    public CodecLiveH265(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }
    public void startLive() {
        try {
//          hevc 表示 h265     mediacodec  中间联系人      dsp芯片   帧
            MediaFormat format = MediaFormat.createVideoFormat("video/hevc", width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(KEY_BIT_RATE, width * height);
            format.setInteger(KEY_FRAME_RATE, 20);
            format.setInteger(KEY_I_FRAME_INTERVAL, 1);
            mediaCodec = MediaCodec.createEncoderByType("video/hevc");
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();
            //创建场地
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "-display",
                    width, height, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface,
                    null, null);

        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }
    @Override
    public void run() {
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            try {
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo,
                        10000);
                if (outputBufferId >= 0) {
                    ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferId);
//                    byte[] outData = new byte[bufferInfo.size];
//                    byteBuffer.get(outData);
//                    writeBytes(outData);
                    dealFrame(byteBuffer, bufferInfo);
                    mediaCodec.releaseOutputBuffer(outputBufferId, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
//0x67      0  1        2       5 位
        //bb 数据:  00 00 00 01 40 xx
        int offset = 4;
        if (bb.get(2) == 0x01) {
            offset = 3;
        }
        //因为 h265 中 vps 和 sps 和 pps 是连在一起的, 所以只需要判断帧类型是 NAL_VPS 就够了
        //并且统一使用一个 vps_sps_pps_buf 即可
        int type = (bb.get(offset) & 0x7E)>>1;
        if (type == NAL_VPS) {
            vps_sps_pps_buf = new byte[bufferInfo.size];
            bb.get(vps_sps_pps_buf);
        }
    }

    public void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(Environment.getExternalStorageDirectory() + "/codec.h265", true);
            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
