package com.maniu.androidmutilvideo.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;

public class CameraManager {
    private Camera camera;
    private boolean isOpened;
    private int currentCamera=Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int previewWidth;
    private int previewHeight;

    public void openCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == currentCamera) {
                camera = Camera.open(i);
                break;
            }
        }
        if (camera == null) {
            camera = Camera.open();
        }
        Camera.Parameters parms = camera.getParameters();
        CameraManager.choosePreviewSize(parms, previewWidth, previewHeight);
        isOpened = true;
    }

    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d("david", "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }
        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
    }
    public boolean isOpened() {
        return isOpened;
    }

    public void setPreview(SurfaceTexture surfaceTexture) {
        try {
            camera.setPreviewTexture(surfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startPreview() {
        camera.startPreview();
    }

}
