package com.example.myapplication;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.media.MediaCodecList.ALL_CODECS;

public class MediaCodecCommonWrapper implements Runnable {
    private String TAG = "MediaCodecCommonWrapper";
    protected MediaCodec mCodec;
    protected String mMimeTypes;

    MediaCodecCommonWrapper()
    {
        mWorkFlg = true;
    }

    public static void getcodecs() {
        Log.i("Hoge", "getcodecs: called");
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        MediaCodecInfo[] codecInfos = codeclist.getCodecInfos();
        Log.i("Hoge", "getcodecs: " + codecInfos.length);
        for (int i=0;i < codecInfos.length;i++) {
            Log.i("Hoge", "getcodecs: " + codecInfos[i]);
            String types[] = codecInfos[i].getSupportedTypes();
            for (int j=0;j < types.length;j++) {
                Log.i("Hoge", "getcodecs: name:" + codecInfos[i].getName() + " type:" + types[j] );
            }
        }
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
        mCodec.configure(format,null,0,null);
        mCodec.start();
        new Thread(this).start();
        return true;
    }
    class HogeBuffer {
        public long pts;
        public ByteBuffer buffer;
    }
    List<HogeBuffer> mBuffers = new ArrayList<HogeBuffer>();

    public boolean write(ByteBuffer buffer, long pts) {
        if (mBuffers.size() >= 50) {
            return false;
        }
        HogeBuffer tmp = new HogeBuffer();
        tmp.pts = pts;
        tmp.buffer = ByteBuffer.allocate(8192);
        tmp.buffer.put(buffer);
        synchronized (mBuffers) {
            mBuffers.add(tmp);
        }
        return true;
    }
    private boolean mWorkFlg;
    public void run() {
        Log.i(TAG, "run: start!");
        int inputIndex = -1;
        ByteBuffer inputBuffer = null;
        while (mWorkFlg == true) {
            if (inputIndex < 0 && inputBuffer == null) {
                inputIndex = mCodec.dequeueInputBuffer(10);
                Log.i(TAG, "run: getInputIndex= " + inputIndex);
            }
            if (inputIndex >= 0 && inputBuffer == null) {
                inputBuffer = mCodec.getInputBuffer(inputIndex);
                Log.i(TAG, "run: getIhnputBuffer success" + inputBuffer + " limit:" + inputBuffer.limit());
            }
            if (inputBuffer != null) {
                synchronized (mBuffers) {
                    if (mBuffers.size() > 0) {
                        HogeBuffer buffer = mBuffers.get(0);
                        mBuffers.remove(0);
                        Log.i(TAG, "run: remove pts=" + buffer.pts + ", size=" + buffer.buffer.remaining());
                        inputBuffer.put(buffer.buffer.array(),0,buffer.buffer.remaining());
                        mCodec.queueInputBuffer(inputIndex,0,inputBuffer.remaining(),buffer.pts,0);
                        inputBuffer = null;
                        inputIndex = -1;

                    }
                }
            }
            processOutputFormat();
        }
    }
    protected boolean processOutputFormat() {
        return false;
    }
}
