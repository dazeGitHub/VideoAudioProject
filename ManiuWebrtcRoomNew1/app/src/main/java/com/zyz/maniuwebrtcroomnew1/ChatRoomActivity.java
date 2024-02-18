package com.zyz.maniuwebrtcroomnew1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import com.zyz.maniuwebrtcroomnew1.utils.PermissionUtil;
import com.zyz.maniuwebrtcroomnew1.webrtc.WebRTCManager;

public class ChatRoomActivity extends AppCompatActivity {
    private WebRTCManager mWebRtcManager;
    private EglBase rootEglBase;

    public static void openActivity(Activity activity){
        Intent intent = new Intent(activity, ChatRoomActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        initView();
    }

    private void initView(){
        rootEglBase = EglBase.create();
        mWebRtcManager = WebRTCManager.getInstance();
        if(!PermissionUtil.isNeedRequestPermission(this)){
            mWebRtcManager.joinRoom(this, rootEglBase);
        }
        rootEglBase.getEglBaseContext();
    }
}