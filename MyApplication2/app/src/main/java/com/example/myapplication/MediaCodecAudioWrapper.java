package com.example.myapplication;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.provider.MediaStore;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaCodecAudioWrapper extends MediaCodecCommonWrapper implements MediaClock {
    AudioSink mSink;
    String TAG = "MediaCodecAudioWrapper";

    MediaCodecAudioWrapper(MediaClockCallback callback) {
        super();
        mType = "AUDIO";
        mCallback = callback;
        mSink = new AudioSink();
    }
    @Override
    public boolean pause(boolean val) {
        if (mSink == null) {
            return false;
        }
        return mSink.pause(val);
    }

    int mLastPos = 0;
    MediaClockCallback mCallback;
    @Override
    public int getPosition() {
        int position = mSink.getPosition();
        if (Math.abs(position - mLastPos) >= 500) {
            if (mCallback != null) {
                mCallback.notifyPlayPosition((int)position);
                mLastPos = (int)position;
            }
        }
        return position;
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
