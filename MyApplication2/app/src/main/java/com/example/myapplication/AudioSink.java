package com.example.myapplication;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class AudioSink implements AudioTrack.OnPlaybackPositionUpdateListener {
    private AudioTrack mTrack;
    private String TAG = "AudioSink";
    private int mFrameRate = -1;
    private AudioSinkCallback mCallback = null;
    private int mPosition = 0;
    private int mLastPos = 0;

    AudioSink(AudioSinkCallback callback) {
        mCallback = callback;
    }

    public boolean setFormat(MediaFormat format) {
        int ch = AudioFormat.CHANNEL_OUT_MONO;
        Log.i(TAG, "setFormat: mime "+ format.getString(MediaFormat.KEY_MIME));
        Log.i(TAG, "setFormat: ch "+ format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        if (format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 2) {
            ch = AudioFormat.CHANNEL_OUT_STEREO;
        }
        Log.i(TAG, "setFormat: rate "+ format.getInteger(MediaFormat.KEY_SAMPLE_RATE));

        mFrameRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int bufSize = AudioTrack.getMinBufferSize(
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                ch, AudioFormat.ENCODING_PCM_16BIT);
        mTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                        .setChannelMask(ch)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .build();

        mTrack.setPlaybackPositionUpdateListener(this);
        mTrack.setPositionNotificationPeriod(1024);

        mTrack.play();
        return true;
    }
    
    public void onMarkerReached(AudioTrack track) {
        Log.i(TAG, "onMarkerReached: called");
    }

    public void onPeriodicNotification(AudioTrack track) {
        Log.d(TAG, "onPeriodicNotification: called");
        float framerate = (float)(mFrameRate)/1000;
        Log.d(TAG, "getPosition: framerate" + framerate);
        float position = (float)(track.getPlaybackHeadPosition()) / framerate;
        Log.d(TAG, "getPosition: pos=" + position);

        mPosition = (int)(position);
        if (Math.abs(mPosition - mLastPos) >= 500) {
            if (mCallback != null) {
                mCallback.notifyPlayPosition(mPosition);
                mLastPos = mPosition;
            }
        }
    }
    
    public boolean pause(boolean val) {
        if (mTrack == null) {
            return false;
        }
        synchronized (mTrack) {
            if (val == true) {
                mTrack.pause();
            } else {
                mTrack.play();
            }
        }
        return true;
    }

    public int getPosition() {
        return mPosition;
    }

    public boolean write(ByteBuffer buffer) {
        Log.d(TAG, "write: " + buffer.remaining());
        //int writeSize = mTrack.write(buffer.array(),0,buffer.remaining());
        int writeSize = mTrack.write(buffer, buffer.remaining(), AudioTrack.WRITE_BLOCKING);
        Log.d(TAG, "write: " + writeSize);
        return true;
    }

    public boolean stop() {
        mTrack.stop();
        return true;
    }
    public boolean release() {
        mTrack.release();
        return true;
    }

    public boolean flush() {
        mTrack.flush();
        try {
            Thread.sleep(100);
        }catch (InterruptedException e) {
        }
        mPosition = 0;
        mLastPos = 0;
        return true;
    }
}

