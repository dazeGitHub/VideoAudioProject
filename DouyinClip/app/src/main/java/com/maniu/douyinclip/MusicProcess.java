package com.maniu.douyinclip;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MusicProcess {

    public static void mixAudioTrack(final String videoInput,//
                                     final String audioInput,
                                     final String output,
                                     final Integer startTimeUs,
                                     final Integer endTimeUs,
                                     int videoVolume,
                                     int aacVolume) throws Exception {

//      mp3  混音     压缩  数据    pcm
        File cacheDir = Environment.getExternalStorageDirectory();

//      待生成的 pcm
        final File videoPcmFile = new File(cacheDir, "video" + ".pcm");

//      视频原始 pcm
        decodeToPCM(videoInput, videoPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

//      视频音乐 wav
        File videoWavFile = new File(cacheDir, videoPcmFile.getName() + ".wav");
        new PcmToWavUtil(
                44100,  AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT
        ).pcmToWav(videoPcmFile.getAbsolutePath(), videoWavFile.getAbsolutePath());

//      背景音乐原始 pcm
        File audioPcmFile = new File(cacheDir, "audio" + ".pcm");
        decodeToPCM(audioInput, audioPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);

//      背景音乐 wav
        File audioWavFile = new File(cacheDir, audioPcmFile.getName() + ".wav");
        new PcmToWavUtil(
                44100,  AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT
        ).pcmToWav(audioPcmFile.getAbsolutePath(), audioWavFile.getAbsolutePath());

//      混音 输出到 adjustedPcm 文件
        File adjustedPcm = new File(cacheDir, "混合后的"  + ".pcm");
        mixPcm(videoPcmFile.getAbsolutePath(), audioPcmFile.getAbsolutePath(),
                adjustedPcm.getAbsolutePath()
                , videoVolume, aacVolume);

        File wavFile = new File(cacheDir, adjustedPcm.getName() + ".wav");
        new PcmToWavUtil(
 44100,  AudioFormat.CHANNEL_IN_STEREO,
2, AudioFormat.ENCODING_PCM_16BIT
        ).pcmToWav(adjustedPcm.getAbsolutePath(), wavFile.getAbsolutePath());
    }

    public static void mixPcm(String pcm1Path, String pcm2Path, String toPath
            , int vol1, int vol2) throws IOException {

        //先转换为 float 防止精度丢失
        float volume1 = vol1 / 100f * 1;
        float volume2 = vol2 / 100f * 1; //音量 max 是 100

//      待混音的两条数据流, 混音后还可以还原, 不过自己要写 傅里叶 变换, 比较复杂
        FileInputStream is1 = new FileInputStream(pcm1Path);
        FileInputStream is2 = new FileInputStream(pcm2Path);
        boolean end1 = false, end2 = false;

//        输出的数据流
        FileOutputStream fileOutputStream = new FileOutputStream(toPath);
        byte[] buffer1 = new byte[2048]; //每次取 1024 * 2 = 2k 个字节, 要不一个字节一个字节的相加太麻烦了
        byte[] buffer2 = new byte[2048];
        byte[] buffer3 = new byte[2048];

        short temp1, temp2; //temp1 代表 pcm1 的一个采样点, temp2 代表 pcm2 的一个采样点
        while (!end1 || !end2) {//有一个读完就停止
            if (!end2) {
                end2 = (is2.read(buffer2) == -1);
            }
            if (!end1) {
                end1 = (is1.read(buffer1) == -1);
            }
            int voice = 0;
            for (int i = 0; i < buffer2.length; i += 2) { //一个采样点2个字节, 所以 i += 2
//              前 低字节  1  后面低字节 2, temp1 和 temp2 就是声量值
//              范围 -32768 ~ 32767
                temp1 = (short) ((buffer1[i] & 0xff) | (buffer1[i + 1] & 0xff) << 8);//后面是高字节要 左移8位
                temp2 = (short) ((buffer2[i] & 0xff) | (buffer2[i + 1] & 0xff) << 8);

                voice = (int) (temp1 * volume1 + temp2 * volume2);
                if (voice > 32767) {
                    voice = 32767;
                }else if (voice < -32768) {
                    voice = -32768;
                }
//
                buffer3[i] = (byte) (voice & 0xFF);
                buffer3[i + 1] = (byte) ((voice >>> 8) & 0xFF);
            }
            fileOutputStream.write(buffer3);
        }
        is1.close();
        is2.close();
        fileOutputStream.close();
    }


    //将视频音频文件 中 startTime 到 endTime 之间的片段转换为 pcm
    @SuppressLint("WrongConstant")
    public static void decodeToPCM(String musicPath,
                                   String outPath, int startTime, int endTime) throws Exception {

        if (endTime < startTime) {
            return;
        }

//      之前是 解码 视频数据, yuv -> h264 或 h264 -> yuv
//      音频  视频  是1  不是2
        MediaExtractor mediaExtractor = new MediaExtractor(); //读取 mp4 或 rmvb
//      设值路径
        mediaExtractor.setDataSource(musicPath);

//      告诉 mediaExtractor 读的是哪条轨道
//      音频索引
        int audioTrack = selectTrack(mediaExtractor);

        //选择轨道
        mediaExtractor.selectTrack(audioTrack);

//      使用 MediaExtractor.SEEK_TO_CLOSEST_SYNC  会很 耗费内存 和 cpu
//      seek 操作很难优化, 只能 UI优化 或 缓存优化:
//           UI优化  : 先缓存每帧的图片, seek 的时候并不播放, 只是展示之前缓存的图片, 点击播放的时候才从 seek 处的 I 帧播放
//           缓存优化 : 每隔 200ms 读取一帧, 缓存图片, 图片数量会非常多
//      会长多 肯定 剪影  500  800M

//      seek 到下一个 I 帧, SEEK_TO_PREVIOUS_SYNC 表示上一个 I 帧
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_NEXT_SYNC);
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audioTrack);

        //audioFormat.getString((MediaFormat.KEY_MIME) 值例如为 audio/mp4a-latm
        MediaCodec mediaCodec = MediaCodec.createDecoderByType(audioFormat.getString((MediaFormat.KEY_MIME)));
        mediaCodec.configure(audioFormat, null, null, 0);
        mediaCodec.start();

        //打断点可以查看 MediaFormat 对象的 max-input-size, 所有帧的大小都不能超过 max-input-size
        int maxBufferSize = 100 * 1000;
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;//100k
        }

        File pcmFile = new File(outPath);
        FileChannel writeChannel = new FileOutputStream(pcmFile).getChannel();
//      如果 ByteBuffer 对象太大, 会造成内存浪费 (例如 10M), 但是写少了 (例如 10k), 会导致异常
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true) {
            int inIndex = mediaCodec.dequeueInputBuffer(1000);
            if (inIndex >= 0) {
//              获取到视频容器里面读取的当前时间戳
                long sampleTimeUs = mediaExtractor.getSampleTime();
                if (sampleTimeUs == -1) { //视频读取到了末尾
                    break;
                }else if (sampleTimeUs < startTime) { //如果 sampleTimeUs 在 startTime 前面, 那么丢弃该帧
                    //sampleTimeUs 就是 mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_NEXT_SYNC) 后的 millis
//                  丢弃的意思
                    mediaExtractor.advance();
                }else if  (sampleTimeUs > endTime) {
                    break; //结束 while 循环
                }

//              mediaExtractor 读取数据到 buffer 中
                bufferInfo.size = mediaExtractor.readSampleData(buffer, 0);
                bufferInfo.presentationTimeUs = sampleTimeUs;
                bufferInfo.flags = mediaExtractor.getSampleFlags(); //帧是否可用

//              读取的 content 是压缩数据, 还没有放到 dsp 芯片进行解码
                byte[] content = new byte[buffer.remaining()];
                buffer.get(content);
                FileUtils.writeContent(content);

                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inIndex);
                inputBuffer.put(content);
                mediaCodec.queueInputBuffer(inIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
//                释放上一帧的压缩数据
                mediaExtractor.advance();
            }

//          读取完数据后进行解码
            int outIndex = -1;
            outIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 1_000);
            if (outIndex >= 0) {
//              此时 decodeOutputBuffer 是原始数据
                ByteBuffer decodeOutputBuffer = mediaCodec.getOutputBuffer(outIndex);
                writeChannel.write(decodeOutputBuffer);
                mediaCodec.releaseOutputBuffer(outIndex, false);
            }
        }

        writeChannel.close();
        mediaExtractor.release();
        mediaCodec.stop();
        mediaCodec.release();
    }

//  寻找音频轨索引
    public static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
//          vlc 的媒体信息可以这样得到 :
//          轨道配置信息, 码流读取, sps pps 解析
            MediaFormat format = extractor.getTrackFormat(i);
//          轨道类型
            String mime = format.getString(MediaFormat.KEY_MIME);
//          创建视频解码器 : MediaCodec mediaCodec = MediaCodec.createEncoderByType("video/avc");
//          创建音频解码器 : MediaCodec audioDecoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
            if (mime.startsWith("audio")) {
                return i;
            }
        }
        return -1;
    }
}