package com.maniu.douyinclip;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class MusicProcess {
    private static final long TIMEOUT = 1000;
    public static void mixAudioTrack(final String videoInput,//
                                     final String audioInput,
                                     final String output,
                                     final Integer startTimeUs,
                                     final Integer endTimeUs,
                                     int videoVolume,
                                     int aacVolume) throws Exception {

//mp3  混音     压缩  数据    pcm
        File cacheDir = Environment.getExternalStorageDirectory();
//还没生成
        final File videoPcmFile = new File(cacheDir, "video" + ".pcm");
//
        decodeToPCM(videoInput, videoPcmFile.getAbsolutePath(), startTimeUs, endTimeUs);



//        下载下来的音乐转换城pcm
        File aacPcmFile = new File(cacheDir, "audio"  + ".pcm");
        decodeToPCM(audioInput,
                aacPcmFile.getAbsolutePath(), startTimeUs, endTimeUs );


//        混音


        File  adjustedPcm = new File(cacheDir, "混合后的"  + ".pcm");
        mixPcm(videoPcmFile.getAbsolutePath(), aacPcmFile.getAbsolutePath(),
                adjustedPcm.getAbsolutePath()
                , videoVolume, aacVolume);

        File wavFile = new File(cacheDir, adjustedPcm.getName()
                + ".wav");
        new PcmToWavUtil(44100,  AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(adjustedPcm.getAbsolutePath()
                , wavFile.getAbsolutePath());
        mixVideoAndMusic(videoInput, output, startTimeUs, endTimeUs, wavFile);
//        合并
    }

    @SuppressLint("WrongConstant")
    private static void mixVideoAndMusic(String videoInput, String output, Integer startTimeUs, Integer endTimeUs, File wavFile) throws IOException {

//      -----------------  操作音频  -----------------
//      视频容器, 用来组装视频 和 新的混合后的音频
        MediaMuxer mediaMuxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

//      读取视频的工具类
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(videoInput);

//      可以添加 N个轨道, 音频轨道 或 视频轨道 在前面都可以
        //拿到视频轨道的索引
        int videoIndex = selectTrack(mediaExtractor, false);
        int audioIndex = selectTrack(mediaExtractor, true);

//      视频和音频都有 MediaFormat
//      pcm  mp4
//      添加轨道  视频  索引     音频索引
        MediaFormat videoFormat = mediaExtractor.getTrackFormat(videoIndex);
//      新的视频  的视频轨
        mediaMuxer.addTrack(videoFormat);

//      mediaMuxer 拥有添加音频流的能力
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audioIndex);
        int audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC); //MIMETYPE_AUDIO_AAC 的值是 audio/mp4a-latm
//      添加的轨道会返回轨道索引 (此时返回的是 音频轨道索引)
        int muxerAudioIndex = mediaMuxer.addTrack(audioFormat);

//      开始输出视频任务  2min  50M  *30=1.5G音频
        mediaMuxer.start();
//      如果音频是 wav 格式的, 无损的, 占用空间大, 并且超过了视频的大小, 建议对视频进行编码
//        视频 wav  --》视频 建议编码
//        解码 MediaFormat 1
//        编码 MediaFormat 2  音乐   变

        //原视频的音频的wav  音频文件   一个轨道  原始音频数据的 MediaFormat
        MediaExtractor pcmExtractor = new MediaExtractor();
        pcmExtractor.setDataSource(wavFile.getAbsolutePath());
        int audioTrack = selectTrack(pcmExtractor, true);
        pcmExtractor.selectTrack(audioTrack);
        //原始音频数据的 MediaFormat
        MediaFormat pcmTrackFormat = pcmExtractor.getTrackFormat(audioTrack);

        //最大一帧的 大小
        int maxBufferSize = 0;
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = pcmTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }

        //这里的参数配置(新的混音的音频的参数配置) 要和 原视频的音频的 wav 参数相同
        //这里的 44100 不应该写死, 应该从 MediaFormat 获取, 之后做
        MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);//参数对应-> mime type、采样率、声道数
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);//比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC); //音质等级, 值越大音频等级越高
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize);//解码

        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        开始编码
        encoder.start();

//      自己读取   从 MediaExtractor
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        //这里可以使用两个线程, 每个线程使用一个 MediaCodec
//      是否编码完成
        boolean encodeDone = false;
        while (!encodeDone) {
            int inputBufferIndex = encoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {//如果 inputBufferIndex < 0 说明没找到
//              返回值 是 时间戳 < 0 文件读到了末尾
                long sampleTime = pcmExtractor.getSampleTime();
                if (sampleTime < 0) {//告诉编码结束
                    encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }else {
                    int flags = pcmExtractor.getSampleFlags();
                    int size = pcmExtractor.readSampleData(buffer, 0);
                    ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferIndex);
                    inputBuffer.clear();
                    inputBuffer.put(buffer);
                    inputBuffer.position(0);
//                  通知编码
                    encoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags);
//                  放弃内存，  一定要写  不写不能读到新的数据
                    pcmExtractor.advance();
                }
            }
//          输出的容器的索引
            int outIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);

//          每次处理音频的一帧
            while (outIndex >= 0) {
                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    encodeDone = true;
                    break;
                }
//              通过索引 得到 编码好的数据在哪个容器
                ByteBuffer encodeOutputBuffer = encoder.getOutputBuffer(outIndex);
//              数据写进去了, 使用的是新的音频轨道索引 muxerAudioIndex, 而不是原视频的音频轨道索引 audioIndex
                mediaMuxer.writeSampleData(muxerAudioIndex, encodeOutputBuffer, info);
//              清空容器数据  方便下次读
                encodeOutputBuffer.clear();
//              把编码器的数据释放 ，方便 dsp 下一帧存
                encoder.releaseOutputBuffer(outIndex, false);
                outIndex = encoder.dequeueOutputBuffer(info, TIMEOUT);
            }
        }

//      -----------------  操作视频  -----------------

//      选择视频轨道而不是音频轨道
        if (audioTrack >= 0) {
            mediaExtractor.unselectTrack(audioTrack);
        }
        mediaExtractor.selectTrack(videoIndex);

//      seek 到 startTimeUs 时间戳的  前一个 I帧
        mediaExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//      根据视频里面最大帧来确定视频 buffer 的大小
        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        buffer = ByteBuffer.allocateDirect(maxBufferSize);//视频不能使用音频的 buffer

//      如果是给视频加水印, 就是在原始数据中操作, 那么就需要先解码后编码
//      这里不是加水印, 不用先解码后编码, 直接截取原视频的 起止时间的视频片段即可
        while (true) {
            long sampleTimeUs = mediaExtractor.getSampleTime();
            if (sampleTimeUs == -1) {
                break;
            }
            if (sampleTimeUs < startTimeUs) {
                mediaExtractor.advance();
                continue;
            }
            if (endTimeUs != null && sampleTimeUs > endTimeUs) {
                break;
            }
            info.presentationTimeUs = sampleTimeUs - startTimeUs+600;
            info.flags = mediaExtractor.getSampleFlags();
            info.size = mediaExtractor.readSampleData(buffer, 0);
            if (info.size < 0) {
                break;
            }
//          写入视频数据
            mediaMuxer.writeSampleData(videoIndex, buffer, info);
//          一定要写 advance
            mediaExtractor.advance();
        }

        pcmExtractor.release();
        mediaExtractor.release();
        encoder.stop();
        encoder.release();
        mediaMuxer.release();
    }

    public static void mixPcm(String pcm1Path, String pcm2Path, String toPath
            , int vol1, int vol2) throws IOException {

        float volume1=vol1 / 100f * 1;
        float volume2=vol2 / 100f * 1;
//待混音的两条数据流 还原   傅里叶  复杂
        FileInputStream is1 = new FileInputStream(pcm1Path);
        FileInputStream is2 = new FileInputStream(pcm2Path);
        boolean end1 = false, end2 = false;
//        输出的数据流
        FileOutputStream fileOutputStream = new FileOutputStream(toPath);
        byte[] buffer1 = new byte[2048];
        byte[] buffer2 = new byte[2048];
        byte[] buffer3 = new byte[2048];
        short temp2, temp1;
        while (!end1 || !end2) {

            if (!end2) {
                end2 = (is2.read(buffer2) == -1);
            }
            if (!end1) {
                end1 = (is1.read(buffer1) == -1);
            }
            int voice = 0;
//2个字节
            for (int i = 0; i < buffer2.length; i += 2) {
//前 低字节  1  后面低字节 2  声量值
//                32767         -32768
                temp1 = (short) ((buffer1[i] & 0xff) | (buffer1[i + 1] & 0xff) << 8);
                temp2 = (short) ((buffer2[i] & 0xff) | (buffer2[i + 1] & 0xff) << 8);

                voice = (int) (temp1*volume1 + temp2*volume2);
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

    @SuppressLint("WrongConstant")
    public static void decodeToPCM(String musicPath,
                                   String outPath, int startTime, int endTime) throws Exception {

        if (endTime < startTime) {
            return;
        }

//        解码 视频数据     yuv  ---》h264
//        h264  --->yuv

//        音频  视频  是1  不是2

        MediaExtractor mediaExtractor = new MediaExtractor();
//        设值路径
        mediaExtractor.setDataSource(musicPath);
//        音频索引
        int audioTrack =  selectTrack(mediaExtractor,true);
//       剪辑
        //选择轨道
        mediaExtractor.selectTrack(audioTrack);
//        耗费内存 和    cpu
//        seek   UI优化     缓存优化  加载视频       200ms   一帧    缓存图片
//         会长多 肯定 剪影  500  800M
//
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_NEXT_SYNC);
        MediaFormat audioFormat = mediaExtractor.getTrackFormat(audioTrack);

        MediaCodec mediaCodec = MediaCodec.createDecoderByType(audioFormat.getString((MediaFormat.KEY_MIME)));
        mediaCodec.configure(audioFormat, null, null, 0);
        mediaCodec.start();
        int maxBufferSize = 100 * 1000;
        if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }
        File pcmFile = new File(outPath);
        FileChannel writeChannel = new FileOutputStream(pcmFile).getChannel();
//        10M   造成内存浪费     10k   异常
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (true) {

            int inputBufferIndex = mediaCodec.dequeueInputBuffer(1000);

            if (inputBufferIndex >= 0) {
//                获取到       视频容器  里面读取的当前时间戳
                long sampleTimeUs =  mediaExtractor.getSampleTime();
                if (sampleTimeUs == -1) {
                    break;
                }else if (sampleTimeUs < startTime) {
//                    丢弃的意思
                    mediaExtractor.advance();
                }else if  (sampleTimeUs > endTime) {
                    break;
                }
//                mediaExtractor
                info.size=   mediaExtractor.readSampleData(buffer, 0);
                info.presentationTimeUs = sampleTimeUs;
                info.flags = mediaExtractor.getSampleFlags();
//                压缩1   原始数据 2

                byte[] content = new byte[buffer.remaining()];
                buffer.get(content);
//                压缩1   未压缩 原始数据2
                FileUtils.writeContent(content);

                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.put(content);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, info.size, info.presentationTimeUs, info.flags);
//                释放上一帧的压缩数据
                mediaExtractor.advance();
            }

            int outIndex = -1;
            outIndex = mediaCodec.dequeueOutputBuffer(info, 1_000);
            if (outIndex >= 0) {
                ByteBuffer decodeOutputBuffer = mediaCodec.getOutputBuffer(outIndex);
//数据 音频数据     压缩1   原始数据2
                writeChannel.write(decodeOutputBuffer);
                mediaCodec.releaseOutputBuffer(outIndex, false);
            }
        }

        writeChannel.close();
        mediaExtractor.release();
        mediaCodec.stop();
        mediaCodec.release();
    }
//    寻找音频轨
    public static int selectTrack(MediaExtractor extractor, boolean audio) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
//          轨道配置信息， 码流读取 ，sps  pps 解析
            MediaFormat format =   extractor.getTrackFormat(i);
//          轨道类型
            String mime =    format.getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.startsWith("audio")) {
                    return i;
                }
            }else {
                if (mime.startsWith("video")) {
                    return i;
                }
            }

        }
        return -1;
    }
}