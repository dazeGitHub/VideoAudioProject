package com.maniu.androidmutilvideo;

import android.graphics.SurfaceTexture;
import android.util.Log;

import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.maniu.androidmutilvideo.camera.CameraManager;
import com.maniu.androidmutilvideo.encoder.StreamPublisher;

import java.util.List;

//监听 cameraPreviewTextureView 创建
// 打开摄像头   给   cameraPreviewTextureView  ---》提供 摄像头数据
public class CameraStreamPublisher {
    StreamPublisher streamPublisher;
    CameraManager cameraManager;
    private CameraPreviewTextureView cameraPreviewTextureView;
    public CameraStreamPublisher(CameraPreviewTextureView cameraPreviewTextureView) {
        this.cameraPreviewTextureView = cameraPreviewTextureView;
        cameraManager = new CameraManager();
    }
    public void initCameraTexture() {
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

                GLTexture texture =producedTextureList.get(0);
//surface
                SurfaceTexture surfaceTexture = texture.getSurfaceTexture();
//                如果摄像头渲染一帧  ---》 会不断 onFrameAvailable
                surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        Log.d("david", "onFrameAvailable可用");
                        cameraPreviewTextureView.requestRenderAndWait();
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



    public interface OnSurfacesCreatedListener {
        void onCreated(List<GLTexture> producedTextureList);
    }

    public void resumeCamera() {
        if (cameraManager.isOpened()) return;
        cameraManager.openCamera();
        initCameraTexture();
        cameraPreviewTextureView.onResume();
    }
}
