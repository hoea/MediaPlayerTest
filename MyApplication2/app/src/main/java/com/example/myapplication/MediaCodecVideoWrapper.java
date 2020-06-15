package com.example.myapplication;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static android.media.MediaCodecList.ALL_CODECS;

public class MediaCodecVideoWrapper extends MediaCodecCommonWrapper {
    String TAG = "MediaCodecVideoWrapper";
    MediaClock mClock;
    Surface mSufrace = null;
    private int outputIndex = -1;
    MediaCodec.BufferInfo mBufferInfo = null;

    public boolean flush() {
        stopThread();
        mBuffers.clear();

        mCodec.flush();
        outputIndex = -1;
        mBufferInfo = null;

        mWorkFlg = true;
        mThread = new Thread(this);
        mThread.start();
        return true;
    }

    MediaCodecVideoWrapper(Surface surface) {
        super();
        mSufrace = surface;
        mType = "VIDEO";
    }
    public boolean init(MediaFormat format)
    {
        Log.i(TAG, "init: called");
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        String hoge = codeclist.findDecoderForFormat(format);
        if (hoge == null) {
            Log.w(TAG, "init: invalid codec mime = " + format.getString(MediaFormat.KEY_MIME));
            ArrayList<MediaCodecInfo> list = findDecoderInfoFromMime(format.getString(MediaFormat.KEY_MIME));
            if (list.size() != 0) {
                hoge = list.get(0).getName();
            }
        }
        if (hoge == null) {
            Log.e(TAG, "init: not found codec" + format.getString(MediaFormat.KEY_MIME));
            return false;
        }
        try {
            mCodec = MediaCodec.createByCodecName(hoge);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        mCodec.configure(format,mSufrace,0,null);
        //mCodec.configure(format,null,0,null);

        return true;
    }
    public boolean setMediaClock(MediaClock clock) {
        mClock = clock;
        return true;
    }
    @Override
    protected boolean processOutputFormat() {
        if (outputIndex < 0) {
            mBufferInfo = new MediaCodec.BufferInfo();
            outputIndex = mCodec.dequeueOutputBuffer(mBufferInfo,100);

        }
        if (outputIndex >= 0) {
            ByteBuffer output = mCodec.getOutputBuffer(outputIndex);
            //Image image= mCodec.getOutputImage(outputIndex);
            Log.d(TAG, "processOutputFormat: " + output);
            Log.d(TAG, "processOutputFormat: " + outputIndex +
                    " pts=" + mBufferInfo.presentationTimeUs +
                    " time=" + mClock.getPosition());
            int clockTime = mClock.getPosition();
            int timediff = (int)(mBufferInfo.presentationTimeUs/1000) - clockTime;

            if (mSufrace == null) { // no surface --> drop frame
                Log.w(TAG, "processOutputFormat: surface is not available");
                mCodec.releaseOutputBuffer(outputIndex,false);
                outputIndex = -1;
                mBufferInfo = null;
                return true;
            }
            if (timediff > 50 /*ms*/) {
                // not display yet.
                Log.w(TAG, "processOutputFormat: not yet " + timediff + ":" +
                        (int)(mBufferInfo.presentationTimeUs/1000) + ":" + clockTime);
                return false;
            } else if (timediff < 0) {
                // drop frame
                Log.w(TAG, "processOutputFormat: drop frame" + timediff + ":" +
                        (int)(mBufferInfo.presentationTimeUs/1000) + ":" + clockTime);
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
