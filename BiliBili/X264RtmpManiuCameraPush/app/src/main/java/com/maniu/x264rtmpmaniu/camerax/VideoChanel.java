package com.maniu.x264rtmpmaniu.camerax;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
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

import com.maniu.x264rtmpmaniu.LivePusher;
import com.maniu.x264rtmpmaniu.utils.ImageUtil;
import java.util.concurrent.locks.ReentrantLock;
//camerax简单
public class VideoChanel implements Preview.OnPreviewOutputUpdateListener, ImageAnalysis.Analyzer {
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
//CameraX  适配摄像头宽高    摄像头
    int width = 480;
    int height = 640;
    private TextureView textureView;
    private HandlerThread handlerThread;
    LivePusher livePusher;
    private MediaCodec mediaCodec;
    private boolean isLiving;


//    CameraX  数据   拍照   视频分析 预览  数据cpu--->gpu    摄像头  gpu 能 1 不能  2 能
//    lifecycleOwner activity
    public VideoChanel(LifecycleOwner lifecycleOwner, TextureView textureView, LivePusher livePusher) {
        this.livePusher = livePusher;
//        初始化
        this.textureView = textureView;
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        ImageAnalysisConfig imageAnalysisConfig =  new ImageAnalysisConfig.Builder() .
                 setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setLensFacing(currentFacing)
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetResolution(new Size(width, height))
                .build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer(this);

        PreviewConfig previewConfig = new PreviewConfig.Builder()  .setTargetResolution(new Size(width, height))
        .setLensFacing(currentFacing) //前置或者后置摄像头
        .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);
        CameraX.bindToLifecycle(lifecycleOwner,preview,imageAnalysis);

    }
    private ReentrantLock lock = new ReentrantLock();
    private byte[] y;
    private byte[] u;
    private byte[] v;
    // 图像帧数据，全局变量避免反复创建，降低gc频率
    private byte[] nv21;
    byte[] nv21_rotated;
    byte[] nv12;
    @Override
    public void analyze(ImageProxy image, int rotationDegrees) {
        if (!isLiving) {
            return;
        }

        Log.i("david", "analyze: ");
        // 开启直播并且已经成功连接服务器才获取i420数据
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        lock.lock();
        // 重复使用同一批byte数组，减少gc频率
        if (y == null) {
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
//             初始化native层 编码
            this.livePusher.native_setVideoEncInfo(image.getHeight(),
                    image.getWidth(), 10, 640_000);
        }
        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);
            int stride = planes[0].getRowStride();
            Size size = new Size(image.getWidth(), image.getHeight());
            int width = size.getHeight();
            int heigth = planes[0].getRowStride();
            if (nv21 == null) {
                nv21 = new byte[heigth * width * 3 / 2];
                nv21_rotated = new byte[heigth * width * 3 / 2];
            }
            ImageUtil.yuvToNv21(y, u, v, nv21, heigth, width);
            ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, heigth, width);
//        一帧画面nv21_rotated 数据的起点
            livePusher.native_pushVideo(nv21_rotated);
        }
        lock.unlock();

    }
    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        SurfaceTexture surfaceTexture = output.getSurfaceTexture();
        if (textureView.getSurfaceTexture() != surfaceTexture) {
            if (textureView.isAvailable()) {
                // 当切换摄像头时，会报错
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);
                parent.requestLayout();
            }
            textureView.setSurfaceTexture(surfaceTexture);
        }
    }
    public void startLive() {
        isLiving = true;
    }
}
