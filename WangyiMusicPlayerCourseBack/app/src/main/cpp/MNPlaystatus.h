//
// Created by maniu on 2022/8/5.
//

#ifndef WANGYIMUSICPLAYER_MNPLAYSTATUS_H
#define WANGYIMUSICPLAYER_MNPLAYSTATUS_H


class MNPlaystatus {

public:
    bool exit;
    bool seek = false;
    bool pause=false;
    bool play = false;
    //    正在努力加载
    bool load = true;
public:
    MNPlaystatus();

};


#endif //WANGYIMUSICPLAYER_MNPLAYSTATUS_H
