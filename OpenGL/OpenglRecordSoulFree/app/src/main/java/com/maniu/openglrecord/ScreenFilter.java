package com.maniu.openglrecord;

import android.content.Context;
//分屏滤镜   +   灰白滤镜
//灵魂出窍     贴纸滤镜
public class ScreenFilter extends  AbstractFilter{
    public ScreenFilter(Context context) {
        super(context, R.raw.video_vert, R.raw.video_frag);
    }
}
