package com.example.myapplication;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Range;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.media.MediaCodecList.ALL_CODECS;

public class MediaCodecCommonWrapper implements Runnable {
    private String TAG = "MediaCodecCommonWrapper";
    protected MediaCodec mCodec;
    protected String mMimeTypes;
    protected String mType = null;
    protected Thread mThread;
    protected List<HogeBuffer> mBuffers = new ArrayList<HogeBuffer>();
    protected boolean mWorkFlg;

    MediaCodecCommonWrapper() {
        mWorkFlg = true;
    }
    public String getType() {
        return mType;
    }
    public boolean pause(boolean val) {
        return true;
    }


    public static void getcodecs() {
        Log.d("Hoge", "getcodecs: called");
        String TAG = "getCodecs";
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        MediaCodecInfo[] codecInfos = codeclist.getCodecInfos();
        Log.i("Hoge", "getcodecs: " + codecInfos.length);
        for (int i=0;i < codecInfos.length;i++) {
            if (codecInfos[i].isEncoder() == true) {
                continue;
            }
            Log.i(TAG, "getcodecs: ----------------------------------------");

            String types[] = codecInfos[i].getSupportedTypes();
            for (int j=0;j < types.length;j++) {
                Log.i("Hoge", "getcodecs: name:" + codecInfos[i].getName() + " type:" + types[j] );
                MediaCodecInfo.CodecCapabilities cap = codecInfos[i].getCapabilitiesForType(types[j]);
                for (int cnt = 0;cnt < cap.colorFormats.length;cnt++) {
                    Log.i(TAG, "getcodecs: colorFormat:" + cap.colorFormats[cnt]);
                }
                for (int cnt = 0;cnt < cap.profileLevels.length;cnt++) {
                    Log.i(TAG, "getcodecs: profileLevels:" + cap.profileLevels[cnt]);
                }
                String[] features = {"FEATURE_AdaptivePlayback",
                        "FEATURE_DynamicTimestamp",
                        "FEATURE_MutipleFrames",
                        "FEATURE_PartialFrame",
                        "FEATURE_SecurePlayback",
                        "FEATURE_TunneledPlayback"};

                for (int cnt = 0;cnt < features.length;cnt++) {
                    Log.i(TAG,features[cnt] + ":" + cap.isFeatureSupported(features[cnt]));
                }

                MediaCodecInfo.AudioCapabilities audiocap = cap.getAudioCapabilities();
                if (audiocap != null) {
                    Log.i(TAG, "getcodecs: audio bitrate range " + audiocap.getBitrateRange());
                    Log.i(TAG, "getcodecs: audio max ch" + audiocap.getMaxInputChannelCount());
                }
                MediaCodecInfo.VideoCapabilities videocap = cap.getVideoCapabilities();
                if (videocap != null) {
                    Log.i(TAG, "getcodecs: video bitrate range " + videocap.getBitrateRange());
                    Log.i(TAG, "getcodecs: video frame rate" + videocap.getSupportedFrameRates());
                    Log.i(TAG, "getcodecs: video widths" + videocap.getSupportedWidths());
                    Log.i(TAG, "getcodecs: video heights" + videocap.getSupportedHeights());
                }
            }
            Log.i(TAG, "getcodecs: ----------------------------------------");
        }
    }

    protected ArrayList<MediaCodecInfo> findDecoderInfoFromMime(String mime) {
        ArrayList<MediaCodecInfo> list = new ArrayList<MediaCodecInfo>();
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        MediaCodecInfo[] codecInfos = codeclist.getCodecInfos();
        for (int i=0;i < codecInfos.length;i++) {
            if (codecInfos[i].isEncoder() == true) {
                continue;
            }
            String types[] = codecInfos[i].getSupportedTypes();
            for (int j=0;j < types.length;j++) {
                if (types[j].equals(mime) == true) {
                    Log.i(TAG, "findDecoderInfoFromMime: fine codec:" + codecInfos[i].getName());
                    list.add(codecInfos[i]);
                }
            }
        }
        return list;
    }

    public boolean init(MediaFormat format) {
        Log.i(TAG, "init: called");
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        String hoge = codeclist.findDecoderForFormat(format);
        Log.i(TAG, "init: codecName " + hoge);
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
        if (mCodec == null) {
            Log.e(TAG, "init: error......" + mCodec);
            return false;
        }
        mCodec.configure(format,null,0,null);
        return true;
    }

    public boolean start() {
        mWorkFlg = true;
        mCodec.start();
        mThread = new Thread(this);
        mThread.start();
        return true;
    }

    public boolean stop(){
        stopThread();
        if (mCodec != null) {
            mCodec.stop();
            mCodec.release();
        }
        return true;
    }
    public boolean flush() {
        stopThread();
        mBuffers.clear();

        mCodec.flush();

        mWorkFlg = true;
        mThread = new Thread(this);
        mThread.start();
        return true;
    }

    protected boolean stopThread() {
        mWorkFlg = false;
        while(mThread.isAlive() == true) {
            Log.i(TAG, "stop: wait for finishing thread  " + mType);
            try {
                Thread.sleep(100);
            }catch (InterruptedException e) {
            }
        }
        mThread = null;
        Log.i(TAG, "stopThread: thread finished.");
        return true;
    }

    class HogeBuffer {
        public long pts;
        public ByteBuffer buffer;
    }

    public boolean write(ByteBuffer buffer, long pts) {
        if (mBuffers.size() >= 50) {
            return false;
        }
        HogeBuffer tmp = new HogeBuffer();
        tmp.pts = pts;
        tmp.buffer = buffer;
        synchronized (mBuffers) {
            mBuffers.add(tmp);
        }
        return true;
    }

    public void run() {
        Log.i(TAG, "run: start!");
        int inputIndex = -1;
        ByteBuffer inputBuffer = null;
        while (mWorkFlg == true) {
            if (inputIndex < 0 && inputBuffer == null) {
                inputIndex = mCodec.dequeueInputBuffer(10);
                Log.d(TAG, "run: getInputIndex= " + inputIndex);
            }
            if (inputIndex >= 0 && inputBuffer == null) {
                inputBuffer = mCodec.getInputBuffer(inputIndex);
                Log.v(TAG, "run: getInputBuffer success" + inputBuffer + " limit:" + inputBuffer.limit());
            }
            if (inputBuffer != null) {
                synchronized (mBuffers) {
                    if (mBuffers.size() > 0) {
                        HogeBuffer buffer = mBuffers.get(0);
                        mBuffers.remove(0);
                        Log.d(TAG, "run: queueInputBuffer :" + mType + " pts=" + buffer.pts + ", size=" + buffer.buffer.limit());
                        inputBuffer.put(buffer.buffer.array(),0,buffer.buffer.limit());
                        mCodec.queueInputBuffer(inputIndex,0,buffer.buffer.limit(),buffer.pts,0);
                        inputBuffer = null;
                        inputIndex = -1;
                    }
                }
            }
            processOutputFormat();
        }
        Log.i(TAG, "run: thread return");
    }

    protected boolean processOutputFormat() {
        return false;
    }
}
