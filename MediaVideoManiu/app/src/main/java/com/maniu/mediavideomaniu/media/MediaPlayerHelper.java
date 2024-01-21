package com.maniu.mediavideomaniu.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.Surface;
import android.widget.Toast;

import java.io.IOException;

public class MediaPlayerHelper {
    public static final String TEST_VIDEO_MP4 = "test_video.mp4";
    private MediaPlayer mediaPlayer;

    public MediaPlayerHelper() {


    }


    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    public boolean isLooping() {
        if (mediaPlayer != null) {
            return mediaPlayer.isLooping();
        }
        return false;
    }

    public void restart() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }
    public void playMedia(final Context context, Surface mediaSurface) {

        mediaPlayer = new MediaPlayer();
//        mediaPlayer.setVolume(0, 0);
        try {
            AssetFileDescriptor afd = context.getAssets().openFd("test_video.mp4");
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setSurface(mediaSurface);
        mediaPlayer.setLooping(true);

        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
            }
        });

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Toast.makeText(context, "onPrepare --> Start", Toast.LENGTH_SHORT).show();
                mediaPlayer.start();
            }
        });


        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer m) {
                Toast.makeText(context, "End Play", Toast.LENGTH_LONG).show();
                m.stop();
                m.release();
                mediaPlayer = null;
            }
        });

        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}