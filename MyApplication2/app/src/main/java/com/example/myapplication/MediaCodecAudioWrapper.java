package com.example.myapplication;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaCodecAudioWrapper extends MediaCodecCommonWrapper implements MediaClock, AudioSinkCallback {
    AudioSink mSink;
    String TAG = "MediaCodecAudioWrapper";

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


    MediaClockCallback mCallback;
    @Override
    public int getPosition() {
        return mSink.getPosition();
    }

    @Override
    public void notifyPlayPosition(long pos) {
        if (mCallback != null) {
            mCallback.notifyPlayPosition(pos);
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
            mSink.setFormat(outformat);
        }
        return true;
    }
}
