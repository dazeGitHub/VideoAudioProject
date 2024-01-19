package com.maniu.ffmpegmusicplayer;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

public class ManiuView  extends View {
    public ManiuView(Context context) {
        super(context);
    }
    public void set() {
        invalidate();

    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
