package com.maniu.androidmutilvideo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.androidCanvas.IAndroidCanvasHelper;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.HueFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;
import com.chillingvan.canvasgl.util.Loggers;
import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;
import com.maniu.androidmutilvideo.encoder.video.H264Encoder;
import com.maniu.androidmutilvideo.muxer.MP4Muxer;
import com.maniu.androidmutilvideo.player.MediaPlayerHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CameraStreamPublisher cameraStreamPublisher;
    private CameraPreviewTextureView cameraPreviewTextureView;
    private Handler handler;
    private HandlerThread handlerThread;
    private TextView outDirTxt;
    private String outputDir;

    private MediaPlayerHelper mediaPlayer = new MediaPlayerHelper();
    private Surface mediaSurface;

    private IAndroidCanvasHelper drawTextHelper = IAndroidCanvasHelper.Factory.createAndroidCanvasHelper(IAndroidCanvasHelper.MODE.MODE_ASYNC);
    private Paint textPaint;
    private Button startButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTextPaint();
        outputDir = new File(Environment.getExternalStorageDirectory(), "/test_mp4_encode.mp4").getAbsolutePath();
        setContentView(R.layout.activity_main);
        cameraPreviewTextureView = findViewById(R.id.camera_produce_view);
        cameraPreviewTextureView.setOnDrawListener(new H264Encoder.OnDrawListener() {
            @Override
            public void onGLDraw(ICanvasGL canvasGL, List<GLTexture> producedTextures,
                                 List<GLTexture> consumedTextures) {
                GLTexture texture = producedTextures.get(0);
                GLTexture mediaTexture = producedTextures.get(1);
                drawVideoFrame(canvasGL, texture.getSurfaceTexture(), texture.getRawTexture(), mediaTexture);
            }

        });
        outDirTxt = (TextView) findViewById(R.id.output_dir_txt);
        outDirTxt.setText(outputDir);
        startButton = findViewById(R.id.test_camera_button);
        handlerThread = new HandlerThread("StreamPublisherOpen");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                playMedia();
//                StreamPublisherParam streamPublisherParam = new StreamPublisherParam();
//                StreamPublisherParam streamPublisherParam = new StreamPublisherParam(1080, 640, 9500 * 1000, 30, 1, 44100, 19200);
                StreamPublisherParam.Builder builder = new StreamPublisherParam.Builder();
                StreamPublisherParam streamPublisherParam = builder
                        .setWidth(1080)
                        .setHeight(750)
                        .setVideoBitRate(1500 * 1000)
                        .setFrameRate(30)
                        .setIframeInterval(1)
                        .setSamplingRate(44100)
                        .setAudioBitRate(19200)
                        .setAudioSource(MediaRecorder.AudioSource.MIC)
                        .createStreamPublisherParam();
                streamPublisherParam.outputFilePath = outputDir;
                streamPublisherParam.setInitialTextureCount(2);
                cameraStreamPublisher.prepareEncoder(streamPublisherParam, new H264Encoder.OnDrawListener() {
                    @Override
                    public void onGLDraw(ICanvasGL canvasGL, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
                        GLTexture texture = consumedTextures.get(1);
                        GLTexture mediaTexture = consumedTextures.get(0);
                        drawVideoFrame(canvasGL, texture.getSurfaceTexture(), texture.getRawTexture(), mediaTexture);
                        Loggers.i("DEBUG", "gl draw");
                    }

                });
                try {
                    cameraStreamPublisher.startPublish();
                } catch (IOException e) {
                    e.printStackTrace();
//                    ((TextView)findViewById(R.id.test_camera_button)).setText("START");
                }
            }
        };

        cameraStreamPublisher = new CameraStreamPublisher( cameraPreviewTextureView);
        cameraStreamPublisher.setOnSurfacesCreatedListener(new CameraStreamPublisher.OnSurfacesCreatedListener() {
            @Override
            public void onCreated(List<GLTexture> producedTextureList) {
                GLTexture texture = producedTextureList.get(1);
                GLTexture mediaTexture = producedTextureList.get(1);
                cameraStreamPublisher.addSharedTexture(new GLTexture(mediaTexture.getRawTexture(),
                        mediaTexture.getSurfaceTexture()));
                mediaSurface = new Surface(texture.getSurfaceTexture());
            }

        });
    }

    private void initTextPaint() {
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
    }

    private void drawVideoFrame(ICanvasGL canvas, @Nullable SurfaceTexture outsideSurfaceTexture,
                                @Nullable BasicTexture outsideTexture, GLTexture mediaTexture) {
        int width = outsideTexture.getWidth();
        int height = outsideTexture.getHeight();
        SurfaceTexture mediaSurfaceTexture = mediaTexture.getSurfaceTexture();
        mediaTexture.getRawTexture().setIsFlippedVertically(true);
//视频流渲染上去了 requerend
        canvas.drawSurfaceTexture(mediaTexture.getRawTexture(), mediaSurfaceTexture, 0, 0, width, height);
//        摄像头也写下
        TextureFilter textureFilterLT = new BasicTextureFilter();
        canvas.drawSurfaceTexture(outsideTexture,outsideSurfaceTexture,
                width *2/3, height *2/3, width , height,textureFilterLT);
    }
    @Override
    protected void onResume() {
        super.onResume();
        cameraStreamPublisher.resumeCamera();
    }
    @Override
    protected void onPause() {
        super.onPause();
        cameraStreamPublisher.pauseCamera();
        if (cameraStreamPublisher.isStart()) {
            cameraStreamPublisher.closeAll();
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.release();
        }
        startButton.setText("START");
    }

    private void playMedia() {
        if ((mediaPlayer.isPlaying() || mediaPlayer.isLooping())) {
            return;
        }

        mediaPlayer.playMedia(this, mediaSurface);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.release();
        }
        handlerThread.quitSafely();
    }

    public void clickStartTest(View view) {
        TextView textView = (TextView) view;
        if (cameraStreamPublisher.isStart()) {
            cameraStreamPublisher.closeAll();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            textView.setText("START");
        } else {
            cameraStreamPublisher.resumeCamera();
            handler.sendEmptyMessage(1);
            textView.setText("STOP");
        }
    }
}
