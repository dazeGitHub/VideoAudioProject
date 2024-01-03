package com.maniu.openglbbc;

import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.LifecycleOwner;

public class CameraHelper {
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private Preview.OnPreviewOutputUpdateListener listener;

    public CameraHelper(LifecycleOwner lifecycleOwner, Preview.OnPreviewOutputUpdateListener listener) {
        this.listener = listener;
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        CameraX.bindToLifecycle(lifecycleOwner, getPreView());
//      CameraX.bindToLifecycle(lifecycleOwner, preview, imageAnalysis);
//      使用 preview 预览时数据不会给开发者, 数据直接给 GPU
//      使用 imageAnalysis 数据是给 CPU 的, 从而开发者可以得到 YUV
    }

    private Preview getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                .setLensFacing(currentFacing) //前置或者后置摄像头
                .build();
//        要不  得到他的数据

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(listener);
        return preview;
    }
}
