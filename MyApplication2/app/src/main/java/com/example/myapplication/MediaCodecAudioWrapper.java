package com.example.myapplication;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaCodecAudioWrapper extends MediaCodecCommonWrapper implements MediaClock, AudioSinkCallback {
    private AudioSink mSink;
    private String TAG = "MediaCodecAudioWrapper";
    private MediaClockCallback mCallback;

    MediaCodecAudioWrapper(MediaClockCallback callback) {
        super();
        mType = "AUDIO";
        mCallback = callback;
        mSink = new AudioSink(this);
    }
    @Override
    public boolean pause(boolean val) {
        if (mSink == null) {
            return false;
        }
        return mSink.pause(val);
    }
    public boolean flush() {
        stopThread();
        mBuffers.clear();

        mCodec.flush();
        //mSink = null;

        mWorkFlg = true;
        mThread = new Thread(this);
        mThread.start();
        return true;
    }

    @Override
    public int getPosition() {
        if (mSink == null) {
            return 0;
        }
        return mSink.getPosition() + mBaseTime;
    }

    @Override
    public void notifyPlayPosition(long pos) {
        if (mCallback != null) {
            mCallback.notifyPlayPosition(pos + mBaseTime);
        }
    }

    @Override
    protected boolean processOutputFormat() {
        MediaCodec.BufferInfo output = new MediaCodec.BufferInfo();
        int outputIndex = mCodec.dequeueOutputBuffer(output,100);
        if (outputIndex >= 0) {
            Log.d(TAG, "processOutputFormat: " + outputIndex);
            ByteBuffer outBuffer = mCodec.getOutputBuffer(outputIndex);
            mSink.write(outBuffer);
            mCodec.releaseOutputBuffer(outputIndex,false);
            outputIndex = -1;
        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            Log.i(TAG, "run: output format changed");
            MediaFormat outformat = mCodec.getOutputFormat();
            //mSink = new AudioSink(this);
            mSink.setFormat(outformat);
        }
        return true;
    }

    int mBaseTime = 0;
    @Override
    public boolean setBaseTime(int position) {
        mBaseTime = position;
        return false;
    }
}
