package com.example.myapplication;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.media.MediaCodecList.ALL_CODECS;

public class MediaCodecCommonWrapper implements Runnable {
    private String TAG = "MediaCodecCommonWrapper";
    private MediaCodec mCodec;
    private String mMimeTypes;

    MediaCodecCommonWrapper()
    {
        mWorkFlg = true;
    }

    private void getcodecs() {
        Log.i(TAG, "getcodecs: called");
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        MediaCodecInfo[] codecInfos = codeclist.getCodecInfos();
        Log.i(TAG, "getcodecs: " + codecInfos.length);
        for (int i=0;i < codecInfos.length;i++) {
            Log.i(TAG, "getcodecs: " + codecInfos[i]);
            String types[] = codecInfos[i].getSupportedTypes();
            for (int j=0;j < types.length;j++) {
                Log.i(TAG, "getcodecs: name:" + codecInfos[i].getName() + " type:" + types[j] );
            }
        }
    }

    public void init(MediaFormat format)
    {
        Log.i(TAG, "init: called");
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        String hoge = codeclist.findDecoderForFormat(format);
        Log.i(TAG, "init: " + hoge);

        try {
            mCodec = MediaCodec.createByCodecName(hoge);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCodec.configure(format,null,0,null);

            new Thread(this).start();
        return;
    }
    class HogeBuffer {
        public long pts;
        public ByteBuffer buffer;
    }
    List<HogeBuffer> mBuffers = new ArrayList<HogeBuffer>();
    public boolean write(ByteBuffer buffer, long pts) {
        HogeBuffer tmp = new HogeBuffer();
        tmp.pts = pts;
        tmp.buffer = buffer;
        synchronized (mBuffers) {
            mBuffers.add(tmp);
        }
        return true;
    }
    private boolean mWorkFlg;
    public void run() {
        /*
        Log.i(TAG, "run: start!");
        int inputIndex = -1;
        ByteBuffer inputBuffer = null;
        while (mWorkFlg == true) {
            if (inputIndex == -1 && inputBuffer == null) {
                inputIndex = mCodec.dequeueInputBuffer(10);
            }
            if (inputIndex > 0 && inputBuffer == null) {
                ByteBuffer inputbuffer = mCodec.getInputBuffer(inputIndex);
            }
            if (inputBuffer != null) {
                synchronized (mBuffers) {
                    if (mBuffers.size() > 0) {
                        HogeBuffer buffer = mBuffers.get(0);
                        mBuffers.remove(0);
                        Log.i(TAG, "run: remove pts=" + buffer.pts);
                        try {
                            mCodec.queueInputBuffer(inputIndex,0,1024*1024,buffer.pts,0);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        inputBuffer = null;
                        inputIndex = -1;
                    }
                }
            }
        }
         */
        while (mWorkFlg == true) {
            synchronized (mBuffers) {
                if (mBuffers.size() > 0) {
                    HogeBuffer buffer = mBuffers.get(0);
                    mBuffers.remove(0);
                    Log.i(TAG, "run: remove pts=" + buffer.pts);
                }
            }
        }
    }
}
