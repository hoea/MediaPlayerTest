package com.example.myapplication;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaCodecAudioWrapper extends MediaCodecCommonWrapper implements MediaClock {
    AudioTrack mTrack;
    String TAG = "MediaCodecAudioWrapper";
    private int mFrameRate = -1;

    MediaCodecAudioWrapper(MediaClockCallback callback) {
        super();
        mType = "AUDIO";
        mCallback = callback;
    }
    @Override
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
    int mLastPos = 0;
    MediaClockCallback mCallback;
    @Override
    public int getPosition() {
        if (mTrack == null) {
            return -1;
        }
        float framerate = (float)(mFrameRate)/1000;
        Log.d(TAG, "getPosition: framerate" + framerate);
        float position = (float)(mTrack.getPlaybackHeadPosition()) / framerate;
        Log.d(TAG, "getPosition: pos=" + position);
        if (Math.abs(position - mLastPos) >= 500) {
            if (mCallback != null) {
                mCallback.notifyPlayPosition((int)position);
                mLastPos = (int)position;
            }
        }
        return (int)(position);
    }
    @Override
    protected boolean processOutputFormat() {
        MediaCodec.BufferInfo output = new MediaCodec.BufferInfo();
        int outputIndex = mCodec.dequeueOutputBuffer(output,100);
        if (outputIndex >= 0) {
            Log.d(TAG, "processOutputFormat: " + outputIndex);
            ByteBuffer outBuffer = mCodec.getOutputBuffer(outputIndex);
            write(outBuffer);
            mCodec.releaseOutputBuffer(outputIndex,false);
            outputIndex = -1;
        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            Log.i(TAG, "run: output format changed");
            MediaFormat outformat = mCodec.getOutputFormat();
            setFormat(outformat);
        }
        return true;
    }
    protected boolean setFormat(MediaFormat format) {
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
        mTrack.play();
        return true;
    }
    protected boolean write(ByteBuffer buffer) {
        Log.d(TAG, "write: " + buffer.remaining());
        //int writeSize = mTrack.write(buffer.array(),0,buffer.remaining());
        synchronized (mTrack) {
            int writeSize = mTrack.write(buffer, buffer.remaining(), AudioTrack.WRITE_BLOCKING);
            Log.d(TAG, "write: " + writeSize);
        }
        return true;
    }
}
