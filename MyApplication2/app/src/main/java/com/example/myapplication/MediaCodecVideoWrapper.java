package com.example.myapplication;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecList.ALL_CODECS;

public class MediaCodecVideoWrapper extends MediaCodecCommonWrapper {
    String TAG = "MediaCodecVideoWrapper";
    MediaClock mClock;

    MediaCodecVideoWrapper() {
        mType = "VIDEO";
    }

    public boolean init(MediaFormat format, Surface surface)
    {
        Log.i(TAG, "init: called");
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        String hoge = codeclist.findDecoderForFormat(format);
        Log.i(TAG, "init: codecName " + hoge);
        if (hoge == null) {
            Log.e(TAG, "init: invalid codec mime = " + format.getString(MediaFormat.KEY_MIME));
            return false;
        }

        try {
            mCodec = MediaCodec.createByCodecName(hoge);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        mCodec.configure(format,surface,0,null);
        mCodec.start();
        new Thread(this).start();
        return true;
    }
    public boolean setMediaClock(MediaClock clock) {
        mClock = clock;
        return true;
    }
    private int outputIndex = -1;
    MediaCodec.BufferInfo mBufferInfo = null;
    @Override
    protected boolean processOutputFormat() {
        if (outputIndex < 0) {
            mBufferInfo = new MediaCodec.BufferInfo();
            outputIndex = mCodec.dequeueOutputBuffer(mBufferInfo,100);
        }
        if (outputIndex >= 0) {
            Log.i(TAG, "processOutputFormat: " + outputIndex +
                    " pts=" + mBufferInfo.presentationTimeUs +
                    " time=" + mClock.getPosition());
            int timediff = (int)(mBufferInfo.presentationTimeUs/1000) - mClock.getPosition();
            if (timediff > 50 /*ms*/) {
                // not display yet.
                return false;
            } else if (timediff < 0) {
                // drop frame
                mCodec.releaseOutputBuffer(outputIndex,false);
                outputIndex = -1;
                mBufferInfo = null;
                return true;
            } else {
                mCodec.releaseOutputBuffer(outputIndex,true);
                outputIndex = -1;
                mBufferInfo = null;
            }


        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            Log.i(TAG, "run: output format changed");
            MediaFormat outformat = mCodec.getOutputFormat();
            Log.i(TAG, "processOutputFormat: " + outformat);
        }
        return true;
    }

}
