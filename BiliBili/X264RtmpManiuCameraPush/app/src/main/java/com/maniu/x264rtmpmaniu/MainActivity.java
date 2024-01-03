package com.maniu.x264rtmpmaniu;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import com.maniu.x264rtmpmaniu.camerax.VideoChanel;
import com.maniu.x264rtmpmaniu.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private LivePusher livePusher;
    VideoChanel videoChanel;
    TextureView textureView;
    static {
        System.loadLibrary("x264rtmpmaniu");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        textureView = findViewById(R.id.textureView);
        livePusher = new LivePusher(this, 800, 480, 800_000, 15, Camera.CameraInfo.CAMERA_FACING_BACK);
        videoChanel = new VideoChanel(this, textureView,livePusher);
        livePusher.setVideoChannel(videoChanel);
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
        return false;
    }

    public void startLive(View view) {
        livePusher.startLive("rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_524987038_52393108&key=60c1c9c16504355c743812b4c3ceb28c&schedule=rtmp&pflag=1");
     }
}