package com.maniu.maniuwebrtcoom.webrtc.peersonnction;

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

//  Caller   是主叫, Receiver 是被叫
//  如果是后进入来的就是主叫, 先进入的是被叫
    enum Role{Caller, Receiver,}
    private Role role;

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
                    createLocalStream();
                }

//              建立链接  只是空的链接  没有初始化
                createPeerConnections();
//              把自己的数据流  往对方推
//              将视频流集合到  mediaStream, 让 mediaStream 再推给另外一端       编码层 1    解码层2
                addStreams();
//              给房间服务器的其他人 发送一个oofer
                createOffers();
            }
        });
      }

    private void addStreams() {
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()) {
            if (mediaStream == null) {
                createLocalStream();
            }
            entry.getValue().pc.addStream(mediaStream); //对方就能看到画面了
        }
    }

    private void createOffers() {
        for (Map.Entry<String, Peer> entry : connectionPeerDic.entrySet()) {
            role = Role.Caller;
            Peer mPeer = entry.getValue();
//            请求服务器     ---服务器帮你下浮  回调   http 请求    封装的比较狠
            mPeer.pc.createOffer(mPeer, offerOrAnswerConstraint());
        }
    }
    private MediaConstraints offerOrAnswerConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
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
    public void onReceiverAnswer(String socketId, String sdp) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
//                链接对象
                Peer mPeer = connectionPeerDic.get(socketId);
                if (mPeer != null) {
                    SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
//                       pc  设值远端的sdp
//                         sdp   上传 给   打洞服务器
                    mPeer.pc.setRemoteDescription(mPeer,sessionDescription);
                }
            }
        });


    }

    public void onRemoteIceCandidate(String socketId, IceCandidate iceCandidate) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                Peer peer = connectionPeerDic.get(socketId);
                if (peer != null) {
//                    addIceCandidate 在内部会请求
                    peer.pc.addIceCandidate(iceCandidate);
                }
            }
        });

    }

    public void onRemoteJoinToRoom(String socketId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (mediaStream == null) {
                    createLocalStream();
                }
//                链接
                Peer mPeer = new Peer(socketId);
                mPeer.pc.addStream(mediaStream);
                connectionIdArray.add(socketId);
                connectionPeerDic.put(socketId, mPeer);
            }
        });
    }

//    99号技师  主动追我
    public void onReceiveOffer(String socketId, String description) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
//              现在是被叫
                role = Role.Receiver;

                Peer mPeer = connectionPeerDic.get(socketId);

                SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, description);
                if (mPeer != null) {
                    mPeer.pc.setRemoteDescription(mPeer, sdp);

                }
            }
        });
    }

    private class Peer implements SdpObserver , PeerConnection.Observer{
        private PeerConnection pc;
        private String userId;

        public Peer(String socketId) {
            this.userId = socketId;
            pc = createPeerConnection();
        }
//nat 网络层 最基本报账           路  32  tchp   地址池  dhcp
        @Override
        public void onCreateSuccess(SessionDescription origSdp) {
            Log.v(TAG, "3  PeerConnectionManager  sdp回写成功       " + origSdp.description);
//            设值本地的sdp

            //            设置两个方法   本地的sdp
            pc.setLocalDescription(Peer.this,origSdp);
//            另外一段的       客户端B的sdp
//            pc.setRemoteDescription();
        }

        @Override
        public void onSetSuccess() {
//              呼叫
//            链接管道
//                     1   你只是设置了本地  没有设值 远端  本地sdp   主        call   被叫  anser
//                 2     你设值了远端   sdp      不能 进行通话
//                3      ice 交换
            if (pc.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                if (role == Role.Caller) {
                    webSocketManager.sendOffer(userId, pc.getLocalDescription().description);
                } else if (role == Role.Receiver) {
//         别人来了         ---》sdp---》  交换给他人
                    webSocketManager.sendAnswer(userId, pc.getLocalDescription().description);
                }
//1
            }else  if (pc.signalingState() == PeerConnection.SignalingState.HAVE_REMOTE_OFFER) {
                pc.createAnswer(Peer.this, offerOrAnswerConstraint());
//链接状态 是什么状态  2
            }else if (pc.signalingState() == PeerConnection.SignalingState.STABLE) {

                if (role == Role.Receiver) {
                    Log.i(TAG, "onSetSuccess: 最后一步测试");
                    webSocketManager.sendAnswer(userId, pc.getLocalDescription().description);
                }
            }
        }

        @Override
        public void onSetFailure(String s) {

        }


        @Override
        public void onCreateFailure(String s) {

        }
//        --------------------------------------------------------------------------------

        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

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
            Log.i(TAG, "onIceCandidate: "+iceCandidate.toString());
            webSocketManager.sendIceCandidate(userId, iceCandidate);
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }
//  ice交换完了  回调这个
        @Override
        public void onAddStream(MediaStream mediaStream) {
            context.onAddRemoteStream(mediaStream, userId);
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
        private PeerConnection createPeerConnection() {
            if (factory == null) {
                factory = creteConnectionFactory();
            }
//        peerconnection  打洞  客户端 能 1 不能2
            PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(ICEServers);
            return factory.createPeerConnection(rtcConfiguration,  this);
        }
    }

    private void createLocalStream() {
        mediaStream = factory.createLocalMediaStream("ARDAMS");
//                    添加一个总流
        AudioSource audioSource= factory.createAudioSource(createAudioConstraints());
//                    音频是数据源    创建一个音频轨道
        AudioTrack audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        mediaStream.addTrack(audioTrack);
//                    音频轨道创建成功   成功音频轨道的数据源
        if (videoEnable) {
//                        VideoSource   实例化
//摄像头 前置 后置     ---》caeram1  camera2



            //            视频源  摄像头的捕获设备
            VideoCapturer  videoCapturer= createVideoCapture();
//                        摄像头
            VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());
//WEBRTC音视频通话  他已经帮你做了
            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
            videoCapturer.initialize(surfaceTextureHelper, context,videoSource.getCapturerObserver());
            videoCapturer.startCapture(320, 240, 10);
//                        视频轨道 已经关联视频源
            VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
//                        视频源  ----数据 绘制本地预览的view
            mediaStream.addTrack(videoTrack);
            if (context != null) {
                context.onSetLocalStream(mediaStream, myId);
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
        return    videoCapturer;

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
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));
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
