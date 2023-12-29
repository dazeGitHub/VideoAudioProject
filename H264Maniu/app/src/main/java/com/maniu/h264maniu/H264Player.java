package com.maniu.h264maniu;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

//解码比较耗时, 所以实现 Runnable, 放到线程里边
public class H264Player  implements  Runnable {
//    数据源

    private String path;
    //    解码器
    MediaCodec mediaCodec;
//surface    ffmpeg
    //    显示   目的   surface
    private Surface surface;
    public H264Player(String path, Surface surface) {
        this.path = path;
        this.surface = surface;
//      初始化 mediaCodec
        try {
            //解码所以使用 decode, 如果创建视频解码器, 那么 video 开头, 否则 audio 开头
            //本来应该传 ISO 的 video/mpeg4-avc, 但是谷歌工程师为了少写代码, 传 video/avc 就行了, 也可以使用枚举
            //"video/avc" - H.264/AVC video
            //该方法支持的编码格式是有限的 (如  h264  h265  vp8 vp9), 例如 RV40 这种编码格式的视频流就不能硬解码
            mediaCodec = MediaCodec.createDecoderByType("video/avc");
            //mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);

            //MediaFormat 中有 map, 但是 Key 限定死了, 防止传错误的 Key 导致 Dsp 不认识
            //如果是视频就是 createVideoFormat(), 音频就是 createAudioFormat(), 字幕就是 createSubtitleFormat()
            //如果没有 sps 那么这里的宽高就很重要了
            MediaFormat mediaformat = MediaFormat.createVideoFormat("video/avc", 364, 368);
            mediaformat.setInteger(MediaFormat.KEY_FRAME_RATE, 15); //帧率

            //第三个参数是 MediaCrypto, 用来加密, 可以继承自 MediaCrypto
            mediaCodec.configure(mediaformat, surface, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
//      如果 mediaCodec 不为 null, 说明初始化成功, 表明当前支持 硬解 H264
        Log.i("david", "支持: ");
    }
    public void play() {
        mediaCodec.start();
        //mediaCodec 中并没有回调方法来返回解码后的 yuv 数组, 因为设置回调都是在同一个物理设备 (例如 CPU)
        //但是 mediaCodec 是横跨 cpu 和 dsp, 所以不能使用回调, 而是由 dsp 提供一个队列 (数量刚好为 8)
        new Thread(this).start();
    }
    @Override
    public void run() {
//        解码   休息10min
//        这个宽高设置小了会不会有问题，如果解析sps真实数据宽高远远大于设置的宽高

        try {
            decodeH264();
        } catch (Exception e) {
            Log.i("david", "run: "+e.toString());
        }
    }
    private void decodeH264() {
        byte[] bytes = null;
        try {
//            bytes 数组里面
            bytes = getBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int startIndex = 0;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            //startIndex + 2 是为了防止 开始四个字节都是 分隔符, 导致返回的 nextFrameStart 等于 startIndex
            //导致下边的 length = nextFrameStart - startIndex = 0, 从而无法播放
            //也不能 + 1 因为没法区别 三个字节 的分隔符 和 两个字节的分隔符
            int nextFrameStart = findByFrame(bytes, startIndex + 2, bytes.length);
            //        ByteBuffer[] byteBuffers = mediaCodec.getInputBuffers(); //这个是直接拿所有的队列 api 是废弃的
            //返回一个可以用的队列元素的索引, 如果索引小于0, 代表当前没有容器可用
            int inIndex =  mediaCodec.dequeueInputBuffer(10000);
//            8K
            if (inIndex >= 0) {
                //获取队列
                ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
//            到底丢多少      文件大小 99M  3M崩了
//             按帧来丢, 每次为 byteBuffer put 一帧的内容
//                byte
                int length = nextFrameStart - startIndex;
                Log.i("david", "decodeH264: 输入  " + length);
//                丢一部分   数据 到容器 length
                byteBuffer.put(bytes, startIndex, length);
//dsp   bytebuffer   dsp 1        dsp   索引   容器 2
                //这里只需要告诉 mediaCodec 当前使用的第几个容器的索引, 而不需要传递 bytes
                //presentationTimeUs : 时间戳, 解码的时候视频里边有时间戳, 所以传 0 即可
                //但是如果是编码, presentationTimeUs 绝对不能传 0
                mediaCodec.queueInputBuffer(inIndex, 0, length, 0, 0);
                startIndex = nextFrameStart;
            }
//            解码了  索引     索引 已经 3秒   dsp  数据 //            8K 是1  不是
            //如果返回的索引大于等于 0, 说明 dsp 解码完成
            //因为是解码, 所以解码后的数据肯定比解码前的数据大
            int outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,10000);
            if (outIndex >= 0) {
                try {
                    Thread.sleep(33); //如果 h264 视频有 30 帧, 那么每帧就有 33 毫秒, 休眠这么长时间
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //解码完后可用选择是否渲染 render, 如果第二个参数 render 传 true, 那么 MediaCodec 会自动渲染到 Surface 上
                mediaCodec.releaseOutputBuffer(outIndex, true);
            }
        }

    }

    //如果找到了分隔符 00 00 00 01 或 00 00 01, 那么就是下一帧的起始位置
    private int findByFrame( byte[] bytes, int start, int totalSize) {
        for (int i = start; i <= totalSize - 4; i++) {
            if (((bytes[i] == 0x00) && (bytes[i + 1] == 0x00) && (bytes[i + 2] == 0x00) && (bytes[i + 3] == 0x01))
            ||((bytes[i] == 0x00) && (bytes[i + 1] == 0x00) && (bytes[i + 2] == 0x01))) {
                return i;
            }
        }
        return -1;
    }
    public byte[] getBytes(String path) throws IOException {
        InputStream is = new DataInputStream(new FileInputStream(new File(path)));
        int len;
        int size = 1024;
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        while ((len = is.read(buf, 0, size)) != -1)
            bos.write(buf, 0, len);
        buf = bos.toByteArray();
        return buf;
    }
}
