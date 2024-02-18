package com.zyz.maniuwebrtcroomnew1.webrtc.peerconnection;

import com.zyz.maniuwebrtcroomnew1.ChatRoomActivity;
import com.zyz.maniuwebrtcroomnew1.webrtc.socket.WebSocketManager;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 和打洞服务器相关
 */
public class PeerConnectionManager {
    //摄像头的源数据都封装在 webrtc 中, 摄像头的数据获取 Camera2, 音频源的获取 AudioTrack

    //视频源
    private String myId;
//  视频通话为 true, 音频通话为 false
    private boolean videoEnable;
    private ExecutorService executor;
    private PeerConnectionFactory factory;
    private ChatRoomActivity context;

    public PeerConnectionManager(){
        executor = Executors.newSingleThreadExecutor();
    }

    public void initContext(ChatRoomActivity context, EglBase rootEglBase){
        this.context = context;
        this.rootEglBase = rootEglBase;
    }

//  加入房间
//  isVideoEnable 表示视频通话
    public void joinToRoom(WebSocketManager javaWebSocket, boolean isVideoEnable, String myId){
        this.myId = myId;
        this.videoEnable = isVideoEnable;
//      建立本地预览
        executor.execute(new Runnable() {
            @Override
            public void run() {
//             本地 webrtc
                if(factory == null){
                    factory = createConnectionFactory();
                }
            }
        });
    }

    private PeerConnectionFactory createConnectionFactory() {
//      编解码
        PeerConnectionFactory.initialize(PeerConnectionFactory.IntializationOptions.builder(context).createInitializationOptions());
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true); //是否支持 Vp8 编码, 是否支持 H264 编码
        VideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options)
            .setAudioDeviceModule(JavaAudioDeviceModule.builder(context).createAudioDeviceModule())
            .setVideoDecoderFactory(decoderFactory)
            .setVideoEncoderFactory(encoderFactory)
            .createPeerConnectionFactory();

        return peerConnectionFactory;
    }
}
