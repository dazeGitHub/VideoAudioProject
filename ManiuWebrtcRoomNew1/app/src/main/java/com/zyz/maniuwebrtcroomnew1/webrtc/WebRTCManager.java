package com.zyz.maniuwebrtcroomnew1.webrtc;

import com.zyz.maniuwebrtcroomnew1.ChatRoomActivity;
import com.zyz.maniuwebrtcroomnew1.MainActivity;
import com.zyz.maniuwebrtcroomnew1.webrtc.peerconnection.PeerConnectionManager;
import com.zyz.maniuwebrtcroomnew1.webrtc.socket.WebSocketManager;

public class WebRTCManager {
    private WebSocketManager mWebSocketManager;
    private PeerConnectionManager mPeerConnectionManager;
    private String roomId = "";
    private static final WebRTCManager ourInstance = new WebRTCManager();

    public static WebRTCManager getInstance(){return ourInstance;}

    private WebRTCManager(){

    }

    public void connect(MainActivity activity, String roomId){
        PeerConnectionManager peerConnectionManager = new PeerConnectionManager();
        mWebSocketManager = new WebSocketManager(activity, peerConnectionManager);
        //房间服务器搭建已经有教学视频了, 下面的地址就是房间服务器的地址
        //socket 通信不加密就是 ws 相当于 http, wss 相当于 https
        mWebSocketManager.connect("wss://8.210.234.39/wss");
    }

    public void joinRoom(ChatRoomActivity chatRoomActivity, EglBase eglBase){
        mPeerConnectionManager.initContext(chatRoomActivity, eglBase);
        mWebSocketManager.joinRoom(roomId);
    }
}
