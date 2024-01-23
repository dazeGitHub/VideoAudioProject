package com.maniu.androidmutilvideo;

import android.graphics.SurfaceTexture;
import android.util.Log;

import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.glview.texture.gles.GLThread;
import com.maniu.androidmutilvideo.camera.CameraManager;
import com.maniu.androidmutilvideo.encoder.StreamPublisher;
import com.maniu.androidmutilvideo.encoder.StreamPublisherParam;
import com.maniu.androidmutilvideo.muxer.MP4Muxer;
import com.maniu.androidmutilvideo.muxer.interfaces.IMuxer;

import java.io.IOException;
import java.util.List;

//监听 cameraPreviewTextureView 创建
// 打开摄像头   给   cameraPreviewTextureView  ---》提供 摄像头数据
public class CameraStreamPublisher {
    StreamPublisher streamPublisher;
    CameraManager cameraManager;
    private CameraPreviewTextureView cameraPreviewTextureView;
    private IMuxer muxer;
    public CameraStreamPublisher(CameraPreviewTextureView cameraPreviewTextureView) {
        this.cameraPreviewTextureView = cameraPreviewTextureView;
        cameraManager = new CameraManager();
    }
    public void initCameraTexture() {
//        当我们CameraPreviewTextureView 构建好 时输出

//       cameraPreviewTextureView  他所在 EGL线程的 上下文什么时候创建       回调这个
        cameraPreviewTextureView.setOnCreateGLContextListener(new GLThread.OnCreateGLContextListener() {
            @Override
            public void onCreate(EglContextWrapper eglContext) {
                muxer = new MP4Muxer();
                streamPublisher = new StreamPublisher(eglContext, muxer);
            }
        });




//        cameraPreviewTextureView 创建的时候 会回调这个方法
        cameraPreviewTextureView.setSurfaceTextureCreatedListener(new GLMultiTexProducerView.SurfaceTextureCreatedListener() {
//            GLTexture  纹理  创建好了  丢给你 2
            @Override
            public void onCreated(List<GLTexture> producedTextureList) {
                Log.d("david", "  CameraStreamPublisher initCameraTexture ");
//                视频流和     producedTextureList  第二个纹理进行绑定了
                if (onSurfacesCreatedListener != null) {
                    onSurfacesCreatedListener.onCreated(producedTextureList);
                }
                //            GLTexture  纹理  1  和摄像头绑定


                GLTexture mediaTexture = producedTextureList.get(1);
//                给他纹理
                streamPublisher.addSharedTexture(new GLTexture(mediaTexture.getRawTexture(),
                        mediaTexture.getSurfaceTexture()));


                GLTexture texture =producedTextureList.get(0);
//surface
                SurfaceTexture surfaceTexture = texture.getSurfaceTexture();
//                如果摄像头渲染一帧  ---》 会不断 onFrameAvailable
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        Log.d("david", "onFrameAvailable可用");
                        cameraPreviewTextureView.requestRenderAndWait();
//                        触发编码一帧
                        streamPublisher.drawAFrame();
                    }
                });
                cameraManager.setPreview(surfaceTexture);
                cameraManager.startPreview();
            }
        });
    }

//    把纹理回调出去
    private OnSurfacesCreatedListener onSurfacesCreatedListener;
    public void setOnSurfacesCreatedListener(OnSurfacesCreatedListener onSurfacesCreatedListener) {
        this.onSurfacesCreatedListener = onSurfacesCreatedListener;
    }

    public void startPublish() throws IOException {
        streamPublisher.start();
    }
    public void prepareEncoder(StreamPublisherParam param) {
        streamPublisher.prepareEncoder(param);
    }

    public boolean isStart() {
        return streamPublisher != null && streamPublisher.isStart();
    }


    public interface OnSurfacesCreatedListener {
        void onCreated(List<GLTexture> producedTextureList);
    }

    public void resumeCamera() {
        if (cameraManager.isOpened()) return;
        cameraManager.openCamera();
        initCameraTexture();
        cameraPreviewTextureView.onResume();
    }

    public void closeAll() {
        streamPublisher.close();
    }
}
