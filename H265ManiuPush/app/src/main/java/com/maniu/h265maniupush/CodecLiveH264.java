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
import android.util.Log;
import android.view.Surface;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CodecLiveH264 extends Thread {
    //    录屏
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private int width = 720;
    private int height = 1280;
    private byte[] sps_pps_buf;
    VirtualDisplay virtualDisplay;
    public static final int NAL_I = 5;
    public static final int NAL_SPS = 7;
    private SocketLive socketLive;
    public CodecLiveH264(SocketLive socketLive, MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
        this.socketLive = socketLive;
    }
    public void startLive() {
        try {
//            mediacodec  中间联系人      dsp芯片   帧
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(KEY_BIT_RATE, width * height);
            format.setInteger(KEY_FRAME_RATE, 20);
            format.setInteger(KEY_I_FRAME_INTERVAL, 1);
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
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
//                    sps
                    ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferId);
//                    byte[] outData = new byte[bufferInfo.size];
//                    byteBuffer.get(outData);
//                    writeContent(outData);
                    dealFrame(byteBuffer, bufferInfo);
                    mediaCodec.releaseOutputBuffer(outputBufferId, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    //sps 和 pps
    //00 00 00 01 67 42C0298D680B40A1A01E1108D4 00 00 00 01 68 CE01A835C8
    private void dealFrame(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
//0x67      0  1        2       5 位
        int offset = 4;
        if (bb.get(2) == 0x01) { //兼容 分隔符为 00 00 01 的情况
            offset = 3;
        }
        int type = (bb.get(offset) & 0x1F); //帧类型

//        sps  只会输出一份  非常宝贵
        if (type == NAL_SPS) {//帧类型为 7 就是 sps       //while 循环第一次走到这
            sps_pps_buf = new byte[bufferInfo.size];
            bb.get(sps_pps_buf);
            Log.e("david", "正在获取 sps 帧数据");
        } else if (NAL_I == type) {//把 sps 和 pps 加到 I 帧的最前面   //while 循环第 2 和 n 次

            final byte[] frameBytes = new byte[bufferInfo.size];
            bb.get(frameBytes);//45459  I 帧的长度就是 bytes.length

            //创建新的数组 newBuf, 长度等于 sps, pps + I 帧 的长度
            byte[] newBuf = new byte[sps_pps_buf.length + frameBytes.length];

            //先将 sps_pps_buf 的内容拷贝到 新数组 newBuf
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);

            //再将 frameBytes 的内容拷贝到 新数组 newBuf
            System.arraycopy(frameBytes, 0, newBuf, sps_pps_buf.length, frameBytes.length);

            //使用 socket 发送 newBuf
            socketLive.sendData(newBuf);

            Log.i("david", "正在发送 I 帧数据  " + Arrays.toString(newBuf));
        }else { //其他帧
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            this.socketLive.sendData(bytes);
            Log.i("david", "正在发送其他帧数据  " + Arrays.toString(bytes));
        }
    }

    //将数据以 十六进制的形式输出到 txt 文件中
    //每发生一次换行, 说明执行了一次该方法
    public String writeContent(byte[] array) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(Environment.getExternalStorageDirectory() + "/codecH264.txt", true);
            writer.write(sb.toString());
            writer.write("\n");//这里会换行
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
        return sb.toString();
    }

    //将数据以 二进制的形式 输出到 codec.h264 文件中
    public void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(Environment.getExternalStorageDirectory() + "/codec.h264", true);
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
