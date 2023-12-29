package com.maniu.webrtcmaniua;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

//音视频通话客户端
public class SocketLive {
    private static final String TAG = "David";
    private SocketCallback socketCallback;
    MyWebSocketClient myWebSocketClient;
    public SocketLive(SocketCallback socketCallback ) {
        this.socketCallback = socketCallback;
    }
    public void start() {
        try {
            URI url = new URI("ws://192.168.1.4:7010");
            myWebSocketClient = new MyWebSocketClient(url);
            myWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendData(byte[] bytes) {
        if (myWebSocketClient!=null&&(myWebSocketClient.isOpen())) {
            myWebSocketClient.send(bytes);
        }
    }

    //type 为 1 视频, type 为 0 音频
    //加 synchronized 是因为音频在子线程
    public synchronized void sendData(byte[] bytes, int type) {
        byte[] newBuf = new byte[bytes.length + 1];
        //最前面的1位单独放类型
        if (type == 1) {
            newBuf[0] = 1;
        }else {
            newBuf[0] = 0;
        }
        System.arraycopy(bytes, 0, newBuf, 1, bytes.length);
        if (myWebSocketClient != null && myWebSocketClient.isOpen()) {
            myWebSocketClient.send(newBuf);
        }
    }
    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            Log.i(TAG, "打开 socket  onOpen: ");
        }

        @Override
        public void onMessage(String s) {
        }
//
        @Override
        public void onMessage(ByteBuffer bytes) {
            Log.i(TAG, "消息长度  : " + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            Log.i(TAG, "onClose: ");
        }

        @Override
        public void onError(Exception e) {
            Log.i(TAG, "onError: ");
        }
    }

    public interface SocketCallback {
        void callBack(byte[] data);
    }
}
