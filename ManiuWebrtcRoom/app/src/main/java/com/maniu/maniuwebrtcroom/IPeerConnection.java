package com.maniu.maniuwebrtcroom;
//webrtc
//Peer : 同龄人
//IPeerConnection 的两个作用:  1. 有人进来   2. 分发数据
public interface IPeerConnection {
    //房间里面有人加入
    public void newConnection(String remoteIp);

    //       分发数据
    public void remoteReceiveData(String remoteIp, byte[] data);

}
