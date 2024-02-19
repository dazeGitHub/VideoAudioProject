package com.maniu.maniuwebrtcoom.webrtc;

import com.maniu.maniuwebrtcoom.ChatRoomActivity;
import com.maniu.maniuwebrtcoom.MainActivity;
import com.maniu.maniuwebrtcoom.webrtc.peersonnction.PeerConnectionManager;
import com.maniu.maniuwebrtcoom.webrtc.socket.WebSocketManager;

import org.webrtc.EglBase;

public class WebRTCManager {
    private WebSocketManager webSocketManager;
    private PeerConnectionManager peerConnectionManager;
    private String roomId = "";
    private static final WebRTCManager ourInstance = new WebRTCManager();

    public static WebRTCManager getInstance() {
        return ourInstance;
    }

    private WebRTCManager() {

    }

    public void connect(MainActivity activity, String roomId) {
        this.roomId = roomId;
        peerConnectionManager = new PeerConnectionManager();
        webSocketManager = new WebSocketManager(activity,peerConnectionManager);
//        socket     http  ws   wss  https   webrtc
        webSocketManager.connect("wss://8.210.234.39/wss");
    }

    public void joinRoom(ChatRoomActivity chatRoomActivity, EglBase eglBase) {
        peerConnectionManager.initContext(chatRoomActivity, eglBase);
        webSocketManager.joinRoom(roomId);
    }
}
