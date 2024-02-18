package com.zyz.maniuwebrtcroomnew1.webrtc.socket;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.zyz.maniuwebrtcroomnew1.MainActivity;
import com.zyz.maniuwebrtcroomnew1.ChatRoomActivity;
import com.zyz.maniuwebrtcroomnew1.webrtc.peerconnection.PeerConnectionManager;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

//连接房间服务器
public class WebSocketManager {
    private String TAG = "TAG";
    private MainActivity mActivity;
    private WebSocketClient mWebSocketClient;
    private PeerConnectionManager mPeerConnectionManager;

    public WebSocketManager(MainActivity activity, PeerConnectionManager peerConnectionManager){
        this.mActivity = activity;
        this.mPeerConnectionManager = peerConnectionManager;
    }

//  连接 socket 服务器
    public void connect(String wss){
        URI uri = null;
        try{
            uri = new URI(wss);
        }catch (URISyntaxException e){
            e.printStackTrace();
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
//              和房间服务器连接成功, 才跳转到聊天界面
                ChatRoomActivity.openActivity(mActivity);
            }

            @Override
            public void onMessage(String message) {
                Log.i(TAG, "onMessage: " + message);
//              发送后会得到响应
//              {
//                  "eventName": "_peers",
//                  "data": {
//                      "connection": [],   //当前只有自己连接, 那么 connection 数组就是空的
//                      "you": "881022acb-bb5e-4c3c-91da-25cf1ed93d35"  //每个客户端都会被分配唯一标识, 这就是唯一标识
//                  }
//              }
                Map map = JSON.parseObject(message, Map.class);
                String eventName = (String) map.get("eventName");
                //eventName 有好几种事件类型 :
                //_peers            房间服务器加入成功
                //_new_peer         房间服务器多了一个人
                //_offer            主动发起请求 请求视频通话
                //_ice_candidate    交换 sdp
                //_answer           被动方同意或者拒绝请求 回答
                //_join             加入房间服务器
                if(eventName.equals("_peers")){
                    //进入房间
                    handJoinRoom(map);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {

            }

            @Override
            public void onError(Exception ex) {

            }
        };

        //设置 wss 证书
        if(wss.startsWith("wss")){
            try{
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustManagerTest()}, new SecureRandom());
                SSLSocketFactory factory = null;
                if(sslContext != null){
                    factory = sslContext.getSocketFactory();
                }
                if(factory != null){
                    mWebSocketClient.setSocket(factory.createSocket());
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        mWebSocketClient.connect();
    }

    public void joinRoom(String roomId) {
//      请求 http socket 请求
//      请求的 json 格式 :
//      {
//        "data" {
//           "room": "6665"
//        },
//        "eventName": "__join"
//      }
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");
        Map<String, String> childMap = new HashMap<>();
        childMap.put("room", roomId);
        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send -> " + jsonString);
        mWebSocketClient.send(jsonString);
    }

    public void handJoinRoom(Map map){
        Map data = (Map) map.get("data");
        JSONArray arr;
        if(data != null){
//          arr = (JSONArray) data.get("connections");
//          得到自己的唯一标识
            String myId = (String) data.get("you");
            mPeerConnectionManager.joinToRoom(this, true, myId);
        }
    }

    //忽略证书
    public static class TrustManagerTest implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
