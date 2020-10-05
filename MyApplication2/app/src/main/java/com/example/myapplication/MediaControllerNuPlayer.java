package com.example.myapplication;

import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.channels.ScatteringByteChannel;

public class MediaControllerNuPlayer implements MediaControllerBase{
    final private String TAG = "MediaControllerNuPlayer";
    MediaPlayer mMediaPlayer;
    MediaControllerCallback mCallback;
    
    public MediaControllerNuPlayer(MediaControllerCallback callback) {
        Log.i(TAG, "MediaControllerNuPlayer: called");
        mCallback = callback;
        return;
    }

    public boolean initialize(Surface surface) {
        Log.i(TAG, "initialize: called");
        mMediaPlayer = new MediaPlayer();
        return true;
    }
    public boolean prepare(String filename) {
        Log.i(TAG, "prepare: filename " + filename);
        try {
            mMediaPlayer.setDataSource(filename);
            mMediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public boolean start(){
        Log.i(TAG, "start: ");
        mMediaPlayer.start();
        return true;
    }
    public boolean seek(int position){
        return false;
    }
    public boolean pause(){
        return false;
    }
    public boolean stop(){
        return false;
    }
}
