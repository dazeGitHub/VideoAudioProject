package com.maniu.openglrecord;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {
//

    CameraView cameraView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        cameraView = findViewById(R.id.cameraView);
//        你作为程序   java 方法  静态方法   ----》
//        面向过程     基于 gl线程上下文
//        EGL14.eglBindAPI(1);
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);

        }
//        DexClassLoader dexClassLoader=    new DexClassLoader("", "", "", dexClassLoader);
        return false;
    }

    public void startRecord(View view) {
        cameraView.startRecord();
    }

    public void stopRecord(View view) {
        cameraView.stopRecord();
    }
}