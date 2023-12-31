package com.maniu.x264rtmpmaniu.camerax;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

import com.maniu.x264rtmpmaniu.utils.FileUtils;
import com.maniu.x264rtmpmaniu.utils.ImageUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

//该类使用 CameraX 简单, Camera2 使用起来比较难, 需要单独讲一节课
//CameraX 优化了 Camera2, 将数据分为好几块 (视频分析, 预览)
public class VideoChanel implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer {
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;

//  CameraX  已经适配了摄像头的宽高
    int width = 480;
    int height = 640;
    private TextureView textureView;
    private HandlerThread handlerThread;
    private byte[] y;
    private byte[] u;
    private byte[] v;

//  这四个用来测试数据是否正常的
    private MediaCodec mediaCodec;
    private byte[] nv21;
    byte[] nv21_rotated;
    byte[] nv12;

//    CameraX  数据   拍照   视频分析 预览  数据cpu--->gpu    摄像头  gpu 能 1 不能  2 能
//    lifecycleOwner activity
    public VideoChanel(LifecycleOwner lifecycleOwner, TextureView textureView) {
        this.textureView = textureView;
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();

        ImageAnalysisConfig imageAnalysisConfig =  new ImageAnalysisConfig.Builder() .
                 setCallbackHandler(new Handler(handlerThread.getLooper())) //图片分析一定要子线程
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE) //发生卡顿时就用最新的
                .setTargetResolution(new Size(width, height))
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        //public interface Analyzer { void analyze(ImageProxy image, int rotationDegrees) }
        //会将 yuv 以 ImageProxy 的形式传给开发者, 等同于 Camera1 中的 PreviewCallback 接口
        imageAnalysis.setAnalyzer(this);

        PreviewConfig previewConfig = new PreviewConfig.Builder()
            .setTargetResolution(new Size(width, height))
            .setLensFacing(currentFacing) //前置或者后置摄像头
            .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);

        //ImageAnalysis 图片分析, VideoCapture 录像, Preview 预览, ImageCapture 拍照
        //直播使用的是 ImageAnalysis (将每一帧的 yuv 数据给开发者) 和 Preview
        //因为需要的是一帧帧的画面, 不使用 VideoCapture, VideoCapture 是直接把视频录制好了
        //bindToLifecycle(LifecycleOwner lifecycleOwner, UseCase... useCases)
        CameraX.bindToLifecycle(lifecycleOwner, preview, imageAnalysis);
    }

//  预览只和 TextureView 有关
    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (textureView.getSurfaceTexture() != surfaceTexture) {
            if (textureView.isAvailable()) {
                // 如果不处理, 那么当横竖屏切换时, 会报错
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);
                parent.requestLayout();
                textureView.setSurfaceTexture(surfaceTexture);
            }
        }
    }

    private ReentrantLock lock = new ReentrantLock();

//  子线程
    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        Log.i("david", "analyze: ");
//      必须在子线程, 所以用 lock 锁住
        lock.lock();
//        planes[0]  y
//        planes[1]  u
//        planes[2]  v

//      PlaneProxy 中有 ByteBuffer getBuffer(); 方法
        ImageProxy.PlaneProxy[] planes = image.getPlanes(); //获取视频数据 yuv
//      Camera1 将返回的是 nv21 数据, CameraX 将 yuv 三个数组都返回
//      width  height
        if (y == null) {
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()]; //planes[0].getBuffer().position() 等同于 0
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
        }
        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);

            //image.getWidth(), image.getHeight() 和 width, height 不同, 前者是经过适配的
            Size size = new Size(image.getWidth(), image.getHeight());
            int width = size.getHeight();
            int height = image.getWidth();

            if (nv21 == null) {
                nv21 = new byte[height * width * 3 / 2];
                nv21_rotated = new byte[height * width * 3 / 2];
            }

//          查看编码的数据 是不是正常 :
//          mediaCodec  输出 h264 文件, 然后查看 h264 文件, 如果是正常的, 说明数据是正确的
//          防止客户端说视频花, 那么推流端可以保存视频然后查看, 防止扯皮
            if (mediaCodec == null) {
                initCodec(size);
            }
            ImageUtil.yuvToNv21(y, u, v, nv21, height, width);
            ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, height, width);
            byte[] temp = ImageUtil.nv21toNV12(nv21_rotated, nv12);

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int inIndex = mediaCodec.dequeueInputBuffer(100000);
            if (inIndex >= 0) {
                ByteBuffer byteBuffer = mediaCodec.getInputBuffer(inIndex);
                byteBuffer.clear();
                byteBuffer.put(temp, 0, temp.length);
                mediaCodec.queueInputBuffer(inIndex, 0, temp.length,
                        0, 0);
            }
            int outIndex = mediaCodec.dequeueOutputBuffer(info, 100000);
            if (outIndex >= 0) {
                ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outIndex);
                byte[] bytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                FileUtils.writeBytes(bytes);
                FileUtils.writeContent(bytes);
                Log.e("rtmp", "ba = " + bytes.length + "");
                mediaCodec.releaseOutputBuffer(outIndex, false);
            }
        }
//      视频不花
        lock.unlock();
    }

    private void initCodec(Size size) {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");

            final MediaFormat format = MediaFormat.createVideoFormat("video/avc",
                    size.getHeight(), size.getWidth());
            //设置帧率
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 8000_000);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);//2s一个I帧
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
