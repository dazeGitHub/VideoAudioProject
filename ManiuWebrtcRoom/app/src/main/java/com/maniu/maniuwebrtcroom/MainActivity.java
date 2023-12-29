package com.maniu.maniuwebrtcroom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

//
public class MainActivity extends AppCompatActivity implements IPeerConnection {
    LocalSurfaceView localTextureView;
    List<Surface> surfaceList = new ArrayList<>();
    List<DecodecPlayerLiveH264> decoderList = new ArrayList<>();
    SocketLive socketLive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        checkPermission();
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
    private void initView() {
        localTextureView = findViewById(R.id.localSurfaceView);
        SurfaceView surfaceView = findViewById(R.id.removeSurfaceView);
        SurfaceView surfaceView1 = findViewById(R.id.removeSurfaceView1);
        SurfaceView surfaceView2 = findViewById(R.id.removeSurfaceView2);
        ArrayList<SurfaceView> surfaceViews = new ArrayList<>();
        surfaceViews.add(surfaceView);
        surfaceViews.add(surfaceView1);
        surfaceViews.add(surfaceView2);

        for (SurfaceView view : surfaceViews) {
            view.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                    surfaceList.add(surfaceHolder.getSurface());
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

                }
            });
        }
        socketLive = new SocketLive(this);
        socketLive.start(this);
    }

    //有人进入  解码器    1个人
    //有人进入
    private int surfaceIndex = 0;
//h264 码流深入
    @Override
    public void newConnection(String remoteIp) {
        DecodecPlayerLiveH264 decodecPlayerLiveH264 = new DecodecPlayerLiveH264();
        decodecPlayerLiveH264.initDecoder(remoteIp, surfaceList.get(surfaceIndex++));
        decoderList.add(decodecPlayerLiveH264);
    }


    public void connectRoom(View view) {
        localTextureView.startCapture(socketLive);
//        socketLive.start(this);
    }


//    有n个人在视频会议，那么每个人都有一个server，
//    和n-1个client，另外，，是这样吗？

    //    ip---decodecPlayerLiveH264
    @Override
    public void remoteReceiveData(String remoteIp, byte[] data) {
//  joinRoom事件  ip---decodecPlayerLiveH264
        DecodecPlayerLiveH264 decodecPlayerLiveH264 = findDecodec(remoteIp);
        if (decodecPlayerLiveH264 != null) {
            decodecPlayerLiveH264.drawSurface(data);
        }
    }

    //找到指定 ip 的解码器
    private DecodecPlayerLiveH264 findDecodec(String remoteIp) {
        for (DecodecPlayerLiveH264 decodecPlayerLiveH264 : decoderList) {
            if (decodecPlayerLiveH264.getRemoteIp().equals(remoteIp)) {
                return decodecPlayerLiveH264;
            }
        }
        return null;
    }
}