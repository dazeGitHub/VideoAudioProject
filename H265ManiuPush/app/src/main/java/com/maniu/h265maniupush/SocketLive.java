package com.maniu.h265maniupush;

import android.media.projection.MediaProjection;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SocketLive {
    //    sps  pps
//    10min
//socket    android 中点对点通信只有  C/S 架构, 只有 Socket
    private static final String TAG = "David";
    //     另外一台设备的socket    ---》   发送数据
    private WebSocket webSocket;
    CodecLiveH264 codecLiveH264;

    public void start(MediaProjection mediaProjection) {
        webSocketServer.start();
        codecLiveH264 = new CodecLiveH264(this, mediaProjection);
        codecLiveH264.startLive();
    }

    public void sendData(byte[] bytes) {
        Log.i("TAG", "SocketLive sendData()  webSocket = " + webSocket + " bytes.length = " + bytes.length);
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
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

    private WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(9007)) {
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
            Log.i("TAG", "WebSocketServer onOpen(), webSocket =" + webSocket);
            SocketLive.this.webSocket = webSocket;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }

        @Override
        public void onMessage(WebSocket conn, String message) {

        }

        @Override
        public void onError(WebSocket conn, Exception ex) {

        }

        @Override
        public void onStart() {

        }
    };
}
