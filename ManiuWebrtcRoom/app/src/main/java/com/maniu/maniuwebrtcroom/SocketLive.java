package com.maniu.maniuwebrtcroom;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
public class SocketLive {
    private int port = 1084;
//    1个Server
    private ManiuSocketServer maniuScoketServer;

//  n个Client, 将自己的数据发给每一个人
    List<MyWebSocketClient> socketClientList = new ArrayList<>();

    //ip 应该是服务器提供, 但是没有服务器, 所以先 ip 写死
    private String[] urls = {"ws://192.168.31.94:", "ws://192.168.31.66:", "ws://192.168.31.141:"};

    public SocketLive(IPeerConnection peerConnection) {
        maniuScoketServer = new ManiuSocketServer(peerConnection);
        maniuScoketServer.start();
    }

//    遍历每一个ip    包含我们自己 ip    过滤掉 已经连接成功
    public void start(final Context context) {
        for (String value : urls) {
            if (value.contains(getLocalIpAddress(context))) { //本机的不要连
                continue;
            }
            boolean isSame = false;
            for (MyWebSocketClient myWebSocketClient : socketClientList) { //已经连接的不要再连
                if (value.contains(myWebSocketClient.getUrl())) {
                    isSame = true;
                    break;
                }
            }
            if (isSame) {
                continue;
            }
            URI url = null;
            try {
                url = new URI(value + port);
                MyWebSocketClient myWebSocketClient = new MyWebSocketClient(value,url);
                myWebSocketClient.connect();
                if (myWebSocketClient.isOpen()) {
                    socketClientList.add(myWebSocketClient);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    //获取本机 ip
    public static String getLocalIpAddress(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            return int2ip(i);
        } catch (Exception ex) {
            return null;
        }
        // return null;
    }
    public static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    //背后默默做出  编码层   和音视频通话 唯一区别   就在这里
    public void sendData(byte[] bytes) {
        for (MyWebSocketClient myWebSocketClient : socketClientList) {
            if (myWebSocketClient.isOpen()) {
                myWebSocketClient.send(bytes);
            }
        }
    }

//  多个 client 端连接多个人
    private class MyWebSocketClient extends WebSocketClient {
        String url;

        public MyWebSocketClient(String url, URI serverURI) {
            super(serverURI);
            this.url = url;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
//            socketClientList.add(this);   //防止 add 多次
        }

        @Override
        public void onMessage(String message) {

        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            socketClientList.remove(this);
        }

        @Override
        public void onError(Exception ex) {
            socketClientList.remove(this);
        }
    }

    //    server 端
    class ManiuSocketServer extends WebSocketServer {
        private IPeerConnection peerConnection;
        public ManiuSocketServer(IPeerConnection peerConnection) {
            super(new InetSocketAddress(port));
            this.peerConnection = peerConnection;
        }
//有人进入到了会议室   并且主动和你链接
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            //getHostAddress() 用来获取远端 ip
            this.peerConnection.newConnection(conn.getRemoteSocketAddress().getAddress().getHostAddress());
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }
//
        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            byte[] buf = new byte[message.remaining()];
            message.get(buf);
            //显示到不同的 surfaceView 上
            peerConnection.remoteReceiveData(
                conn.getRemoteSocketAddress().getAddress().getHostName(),
                buf
            );
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
    }

}
