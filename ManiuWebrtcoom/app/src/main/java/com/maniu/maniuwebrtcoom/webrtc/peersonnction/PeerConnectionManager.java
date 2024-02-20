package com.maniu.maniuwebrtcoom.webrtc.peersonnction;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.maniu.maniuwebrtcoom.ChatRoomActivity;
import com.maniu.maniuwebrtcoom.webrtc.socket.WebSocketManager;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerConnectionManager {
    private static final String TAG = "david";
//    webrtc中
//    摄像头的数据获取 Camera2
//    音频源的获取   AudioTrak

    //视频源
    private String myId;
//视频  true    false 音视频
    private boolean videoEnable;
    private ExecutorService executor;

    private PeerConnectionFactory factory;

    private ChatRoomActivity context;
    private EglBase rootEglBase ;
    private MediaStream mediaStream;
    WebSocketManager webSocketManager;
    private ArrayList<String> connectionIdArray;
    private Map<String, Peer> connectionPeerDic;
    private ArrayList<PeerConnection.IceServer> ICEServers;

    public PeerConnectionManager() {
        executor = Executors.newSingleThreadExecutor();
        connectionIdArray = new ArrayList<>();
        connectionPeerDic = new HashMap<>();
        ICEServers = new ArrayList<>();
        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder("turn:8.210.234.39:3478?transport=udp")
                .setUsername("ddssingsong").setPassword("123456").createIceServer();
        ICEServers.add(iceServer1);
    }

    public void initContext(ChatRoomActivity context , EglBase rootEglBase){
        this.context = context;
        this.rootEglBase = rootEglBase;
    }
    public void joinToRoom(WebSocketManager javaWebSocket,ArrayList<String> connections , boolean isVideoEnable,
                           String myId) {
        this.myId = myId;
        this.videoEnable = isVideoEnable;
        this.webSocketManager = javaWebSocket;
        connectionIdArray.addAll(connections);
//建立本地预览
        executor.execute(new Runnable() {
            @Override
            public void run() {
//                本地 webrtc   狂
                if (factory == null) {
//                    webrtc    链接 者个工厂
//                     添加视频流 音频流
//                      完成了 传输层  有人进来了   给每个人  发送 链接请求
//                      p2p 链接
                    factory = creteConnectionFactory();
//                    MediaCodec mediaCodec = MediaCodec.createDecoderByType();
//                    MediaFormat mediaFormat = new MediaFormat();//MediaConstraints
//                    mediaCodec.configure();

                }
                if (mediaStream == null) {
                    createLoaclStream();
                }
//              建立链接  只是空的链接  没有初始化 (即没有向打洞服务器说明要将连接发过去)
                createPeerConnections();
//              给房间服务器的其他人 发送一个offer
                createOffers();
            }
        });
    }

    private void createOffers() {
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()) {
            Peer mPeer = entry.getValue();
            //createOffer 就是获取本地到服务端的 sdp, 如果响应成功会回调 Peer 中的 onCreateSuccess() 方法
            mPeer.pc.createOffer(mPeer, offerOrAnswerConstraint());
        }
    }

    private MediaConstraints offerOrAnswerConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")); //想让听到声音就设置为 true
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", String.valueOf(videoEnable)));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    private void createPeerConnections() {
        for (String str : connectionIdArray) {
            Peer peer = new Peer(str);
            connectionPeerDic.put(str, peer);
        }
    }

    //使用 Peer 封装 PeerConnection, 因为需要根据 userId 找到 PeerConnection
    private class Peer implements SdpObserver {
        private PeerConnection pc;
        private String userId;

        public Peer(String userId) {
            this.userId = userId;
            pc = createPeerConnection();
        }
//nat 网络层 最基本报账           路  32  tchp   地址池  dhcp
        @Override
        public void onCreateSuccess(SessionDescription origSdp) {
            //description 就是 sdp 协议, 描述了网络连接经过了哪些交换机 和 路由器
            Log.v(TAG, "3  PeerConnectionManager  sdp回写成功       " + origSdp.description);
        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    }

    private PeerConnection createPeerConnection() {
        if (factory == null) {
            factory = creteConnectionFactory();
        }
//        peerconnection  打洞  客户端 能 1 不能2
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(ICEServers);
        return factory.createPeerConnection(rtcConfiguration, new ICEObserver());
    }

//  sdp交换完了 之后    需要用的到暂时没有用到
    private class ICEObserver implements PeerConnection.Observer {

        //对方挂断、同意、拒绝时会调用该方法
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        //当从 wifi 切换到 手机网络 时, 信号改变会触发该方法
        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {

        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }

        @Override
        public void onAddStream(MediaStream mediaStream) {

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }
    }

    private void createLoaclStream() {
        mediaStream = factory.createLocalMediaStream("ARDAMS");

//      添加一个总流
        AudioSource audioSource= factory.createAudioSource(createAudioConstraints());
//      音频是数据源    创建一个音频轨道
        AudioTrack audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        mediaStream.addTrack(audioTrack);

//      音频轨道创建成功   成功音频轨道的数据源
        if (videoEnable) {
//                        VideoSource   实例化
//          摄像头 前置 后置     ---》caeram1  camera2



            //            视频源  摄像头的捕获设备
            VideoCapturer  videoCapturer= createVideoCapture();
//                        摄像头
            VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
//          WEBRTC音视频通话  他已经帮你做了
            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
            videoCapturer.initialize(surfaceTextureHelper, context,videoSource.getCapturerObserver());
            videoCapturer.startCapture(320, 240, 10);//(宽度, 高度, 关键帧间隔)
//                        视频轨道 已经关联视频源
            VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
//                        视频源  ----数据 需要绘制到本地预览的view
            mediaStream.addTrack(videoTrack);
            if (context != null) {
//                for(int i = 0; i < 5; i++){
                context.onSetLocalStream(mediaStream, myId); //如果想添加多个, 可以使用 for 循环加 myId + index
//                }
            }
        }
    }
    private VideoCapturer createVideoCapture() {
        VideoCapturer videoCapturer = null;
        if (Camera2Enumerator.isSupported(context)) {
//camera2
            Camera2Enumerator enumerator = new Camera2Enumerator(context);
//            前置摄像头的捕获
            videoCapturer = createCameraCapture(enumerator);
        }else {
            Camera1Enumerator enumerator = new Camera1Enumerator(true);
            videoCapturer = createCameraCapture(enumerator);
        }
        return videoCapturer;

    }
//    意思  获取前置前置摄像头   后置摄像头
    private VideoCapturer createCameraCapture(CameraEnumerator enumerator) {

        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer= enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }

        }
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer= enumerator.createCapturer(deviceName, null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;

    }

    //    googEchoCancellation   回音消除
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
//
    //    googNoiseSuppression   噪声抑制
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";

    //    googAutoGainControl    自动增益控制
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    //    googHighpassFilter     高通滤波器
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";

    private MediaConstraints createAudioConstraints() {
//        ashmap

        MediaConstraints audioConstraints = new MediaConstraints();
//        webrtc 集成   比较复杂
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true")); //回音消除
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true")); //噪音消除
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));//自动增益
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));//高通滤波器
        return audioConstraints;
    }

    private PeerConnectionFactory creteConnectionFactory() {
// 编码
//  音频编码  视频编码
// 解码
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).
                createInitializationOptions());
        VideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true);
        VideoDecoderFactory decoderFactory=new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory= PeerConnectionFactory.builder().setOptions(options).setAudioDeviceModule(JavaAudioDeviceModule.builder(context)
                .createAudioDeviceModule()).setVideoDecoderFactory(decoderFactory).setVideoEncoderFactory(encoderFactory).createPeerConnectionFactory();
        return peerConnectionFactory;
    }


}
