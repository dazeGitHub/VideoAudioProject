package com.maniu.webrtcmaniub;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SocketLive {
    public interface SocketCallback {
        void callBack(byte[] data);
    }
    private WebSocket webSocket;
    private SocketCallback socketCallback;
    public SocketLive(SocketCallback socketCallback ) {
        this.socketCallback = socketCallback;
    }
    public void start() {
        webSocketServer.start();
    }
    public void close() {
        try {
            webSocket.close();
            webSocketServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
//            视频  音频
            webSocket.send(bytes);
        }
    }

    //type 为 1 视频, type 为 0 音频
    public  synchronized void sendData(byte[] bytes, int type) {
        byte[] newBuf = new byte[bytes.length + 1];
//        视频数据包
        if (type == 1) {
            newBuf[0] = 1;
        } else {
            newBuf[0] = 0;
        }
//        接收端 1
        System.arraycopy(bytes, 0, newBuf, 1, bytes.length);
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(newBuf);
        }
    }
    private WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(7010)) {
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            SocketLive.this.webSocket = conn;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }

        @Override
        public void onMessage(WebSocket conn, String message) {

        }
//老张发送过来
        @Override
        public void onMessage(WebSocket conn, ByteBuffer bytes) {
            Log.i("David", "消息长度  : " + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {

        }

        @Override
        public void onStart() {

        }
    };
}
