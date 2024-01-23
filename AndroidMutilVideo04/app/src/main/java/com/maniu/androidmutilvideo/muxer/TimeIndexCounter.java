package com.maniu.androidmutilvideo.muxer;

public class TimeIndexCounter {
    //    上一次编码的时间
    private long lastTimeUs = -1;
//    帧数据
    private int timeIndex;

    public void calcTotalTime(long currentTimeUs) {
        if (lastTimeUs <= 0) {
            this.lastTimeUs = currentTimeUs;
        }

        int delta = (int) (currentTimeUs - lastTimeUs);
        this.lastTimeUs = currentTimeUs;
        timeIndex += Math.abs(delta / 1000);
    }


    public void reset() {
        lastTimeUs = 0;
        timeIndex = 0;
    }

    public int getTimeIndex() {
        return timeIndex;
    }


}
