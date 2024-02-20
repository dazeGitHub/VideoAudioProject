package com.maniu.maniuwebrtcoom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.maniu.maniuwebrtcoom.utils.PermissionUtil;
import com.maniu.maniuwebrtcoom.utils.Utils;
import com.maniu.maniuwebrtcoom.webrtc.WebRTCManager;

import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomActivity  extends Activity {
    private WebRTCManager webRTCManager;
    private EglBase rootEglBase;
    private VideoTrack localVideoTrack;
    private Map<String, SurfaceViewRenderer> videoViews = new HashMap<>();
//房间里所有的ID列表
    private List<String> persons = new ArrayList<>();
    FrameLayout wrVideoLayout;
    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        activity.startActivity(intent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
//        聊天  界面    房间发小   再给房间服务器发送器    我要进入哪个房间
        initView();
    }
    private void initView() {
        wrVideoLayout = findViewById(R.id.wr_video_view);
        wrVideoLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams
                .MATCH_PARENT));
        rootEglBase = EglBase.create();
        webRTCManager = WebRTCManager.getInstance();

        if (!PermissionUtil.isNeedRequestPermission(this)) {
            webRTCManager.joinRoom(this, rootEglBase);
        }


    }

//    远端的流数据
    public void onAddRemoteStream(MediaStream stream, String userId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(userId, stream);
            }
        });
    }

//webrtc 不收费
    public void onSetLocalStream(MediaStream stream, String userId) {
//        总流   音频流  视频流
        if (stream.videoTracks.size() > 0) {
//            能理解 1
            localVideoTrack= stream.videoTracks.get(0);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addView(userId, stream);
            }
        });
//
    }


//    调用事件
    private void addView(String userId, MediaStream stream) {
//        不用SurfaceView  采用webrtc给我们提供的SurfaceViewRenderer
        SurfaceViewRenderer surfaceViewRenderer = new SurfaceViewRenderer(this);
        //        初始化SurfaceView
        surfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null);
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        surfaceViewRenderer.setMirror(true);
//        关联
        if (stream.videoTracks.size() > 0) {
            stream.videoTracks.get(0).addSink(surfaceViewRenderer);
        }
        videoViews.put(userId, surfaceViewRenderer);
        persons.add(userId);
        wrVideoLayout.addView(surfaceViewRenderer);
        int size = videoViews.size();
        for (int i = 0; i < size; i++) {
//            surfaceViewRenderer  setLayoutParams
            String peerId = persons.get(i);
            SurfaceViewRenderer renderer1 = videoViews.get(peerId);

            if (renderer1 != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                layoutParams.height = Utils.getWidth(this, size);
                layoutParams.width = Utils.getWidth(this, size);
                layoutParams.leftMargin = Utils.getX(this, size, i);
                layoutParams.topMargin = Utils.getY(this, size, i);
                renderer1.setLayoutParams(layoutParams);
            }
        }
    }
}
