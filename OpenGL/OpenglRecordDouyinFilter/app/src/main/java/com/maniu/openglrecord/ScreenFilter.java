package com.maniu.openglrecord;

import android.content.Context;

public class ScreenFilter extends  AbstractFilter{
    public ScreenFilter(Context context) {
        super(context, R.raw.video_vert, R.raw.video_frag);
    }
}
