package com.maniu.maniuwebrtcoom.webrtc.socket;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.maniu.maniuwebrtcoom.ChatRoomActivity;
import com.maniu.maniuwebrtcoom.MainActivity;
import com.maniu.maniuwebrtcoom.webrtc.peersonnction.PeerConnectionManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

//链接房间服务器
public class WebSocketManager {
    PeerConnectionManager peerConnectionManager;



    private static final String TAG = "David";
    private MainActivity activity;
    private WebSocketClient mWebSocketClient;

    public WebSocketManager(MainActivity activity, PeerConnectionManager peerConnectionManager) {

        this.activity = activity;
        this.peerConnectionManager = peerConnectionManager;
    }

//链接sokect  socket
    public void connect(String wss) {
//
        URI uri = null;
        try {
            uri = new URI(wss);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
//                房间服务器    跳转到  聊天界面
                ChatRoomActivity.openActivity(activity);
                Log.i(TAG, "onOpen: " );
            }

            @Override
            public void onMessage(String message) {
                Log.i(TAG, "onMessage: "+message);
//                本地预览
                Map map = JSON.parseObject(message, Map.class);
                String eventName = (String) map.get("eventName");
                if (eventName.equals("_peers")) {
                    hanleJoinRoom(map);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i(TAG, "onClose: ");
            }

            @Override
            public void onError(Exception ex) {
                Log.i(TAG, "onError: ");

            }
        };

        if (wss.startsWith("wss")) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustManagerTest()}, new SecureRandom());
                SSLSocketFactory factory = null;
                if (sslContext != null) {
                    factory = sslContext.getSocketFactory();
                }
                if (factory != null) {
                    mWebSocketClient.setSocket(factory.createSocket());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mWebSocketClient.connect();
    }
//请求
    public void joinRoom(String roomId) {
//        请求  http     socket 请求
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");
        Map<String, String> childMap = new HashMap<>();
        childMap.put("room", roomId);
        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        mWebSocketClient.send(jsonString);
    }
//    响应  基于他业务
    private void hanleJoinRoom(Map map) {
        Map data = (Map) map.get("data");
        JSONArray arr;
        if (data != null) {
            arr = (JSONArray) data.get("connections");
            String js = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName);
            ArrayList<String> connections = (ArrayList<String>) JSONObject.parseArray(js, String.class);

            String myId = (String) data.get("you");
            peerConnectionManager.joinToRoom(this, connections,true, myId);

        }

    }

    // 忽略证书
    public static class TrustManagerTest implements X509TrustManager {


        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
