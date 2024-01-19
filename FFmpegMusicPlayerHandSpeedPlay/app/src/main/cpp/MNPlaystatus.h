
#ifndef MYMUSIC_WLPLAYSTATUS_H
#define MYMUSIC_WLPLAYSTATUS_H


class MNPlaystatus {

public:
    bool exit;
    bool seek = false;
    bool pause = false;
//    正在努力加载
    bool load = true;
public:
    MNPlaystatus();

};


#endif //MYMUSIC_WLPLAYSTATUS_H
