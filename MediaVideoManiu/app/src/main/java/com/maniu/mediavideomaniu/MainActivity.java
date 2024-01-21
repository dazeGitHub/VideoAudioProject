package com.maniu.mediavideomaniu;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Surface;
import android.view.View;

import com.maniu.mediavideomaniu.media.MediaPlayerHelper;

public class MainActivity extends AppCompatActivity {
    private MediaPlayerHelper mediaPlayer = new MediaPlayerHelper();
    private Surface mediaSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mediaSurface = new Surface();
//        播放
    }

    public void startVideo(View view) {

        if ((mediaPlayer.isPlaying() || mediaPlayer.isLooping())) {
            return;
        }

        mediaPlayer.playMedia(this, mediaSurface);

    }
}