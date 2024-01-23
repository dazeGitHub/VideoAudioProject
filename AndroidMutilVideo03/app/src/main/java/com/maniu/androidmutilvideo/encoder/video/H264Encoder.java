package com.maniu.androidmutilvideo.encoder.video;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.view.Surface;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.MultiTexOffScreenCanvas;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.maniu.androidmutilvideo.encoder.MediaCodecInputStream;
import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;

import java.io.IOException;
import java.util.List;

public class H264Encoder {
    //openg 一个屏幕        surface拿到数据
    private Surface mInputSurface;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;
    MediaCodec mEncoder;
    private boolean isStart;

    //     读取变流   内部
    private MediaCodecInputStream mediaCodecInputStream;


//    画布   ------》 id
    protected  EncoderCanvas offScreenCanvas;
    public H264Encoder(StreamPublisherParam params, EglContextWrapper eglCtx) {
//        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, params.width,
//                params.height);
        MediaFormat format = params.createVideoMediaFormat();
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        format.setInteger(MediaFormat.KEY_BIT_RATE, params.videoBitRate);

        format.setInteger(MediaFormat.KEY_FRAME_RATE, params.frameRate);
//        I帧
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, params.iframeInterval);
        try {
            mEncoder = MediaCodec.createEncoderByType(params.videoMIMEType);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mEncoder.createInputSurface();
            mEncoder.start();


        } catch (IOException e) {

            e.printStackTrace();
        }

        mediaCodecInputStream = new MediaCodecInputStream(mEncoder, new MediaCodecInputStream.MediaFormatCallback() {
            @Override
            public void onChangeMediaFormat(MediaFormat mediaFormat) {
                params.setVideoOutputMediaFormat(mediaFormat);
            }
        });
//        在里面 封装一个EGL环境  设值windowsurface
        offScreenCanvas = new EncoderCanvas(params.width, params.height, eglCtx);

//  eglEnv.draw(textureId,timestamp);
//        offScreenCanvas.requestRender();
    }

    public void addSharedTexture(GLTexture texture) {
//把处理好滴纹理 给到  虚拟  offScreenCanvas
        offScreenCanvas.addConsumeGLTexture(texture);

    }

    public MediaCodecInputStream getMediaCodecInputStream() {
        return mediaCodecInputStream;
    }

//外部手动 调用requestRender
    public void requestRender() {
//一旦被调用    数据 不断渲染offScreenCanvas
        offScreenCanvas.requestRender();
//        开始编码     写到文件 可以1 不可以 2
    }

    public void start() {
        offScreenCanvas.start();
        isStart = true;
    }

    public void close() {
        if (!isStart) return;

        mediaCodecInputStream.close();

        synchronized (mEncoder) {
            mEncoder.stop();
            mEncoder.release();
        }
        isStart = false;
    }

    public boolean isStart() {
        return isStart;
    }

    //封装egl环境       交换 数据
    private class EncoderCanvas extends MultiTexOffScreenCanvas {
//        虚拟显示器
        public EncoderCanvas(int width, int height, EglContextWrapper eglCtx) {
            super(width, height, eglCtx, H264Encoder.this.mInputSurface);
        }

        @Override
        protected void onGLDraw(ICanvasGL canvas, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {

        }
    }

}
