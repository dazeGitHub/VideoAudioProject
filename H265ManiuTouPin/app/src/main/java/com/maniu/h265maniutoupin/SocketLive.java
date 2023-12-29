package com.maniu.h265maniutoupin;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class SocketLive {
    private static final String TAG = "David";
    private SocketCallback socketCallback;
    MyWebSocketClient myWebSocketClient;
    private WebSocket webSocket;
    private int port;
    public SocketLive(SocketCallback socketCallback, int port) {
        this.socketCallback = socketCallback;
        this.port = port;
    }
    public void start() {
        try {
            //查看手机的 ip
            URI url = new URI("ws://192.168.1.4:9007");
            myWebSocketClient = new MyWebSocketClient(url);
            myWebSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            Log.i(TAG, "MyWebSocketClient 打开 socket  onOpen: ");
        }

        @Override
        public void onMessage(String str) {
            Log.i(TAG, "MyWebSocketClient onMessage str = " + str);
        }
//不断回调他
        @Override
        public void onMessage(ByteBuffer bytes) {
            Log.i(TAG, "MyWebSocketClient onMessage bytes 消息长度  : " + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            socketCallback.callBack(buf);
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            Log.i(TAG, "MyWebSocketClient onClose: ");
        }

        @Override
        public void onError(Exception e) {
            Log.i(TAG, "MyWebSocketClient onError, e = " + e.getMessage());
        }
    }
    public interface SocketCallback{
        void callBack(byte[] data);
    }
}
