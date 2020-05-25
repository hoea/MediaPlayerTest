package com.example.myapplication;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaExtractorWrapper implements Runnable {
    final private String TAG = "MediaExtractorWrapper";
    MediaCodecCommonWrapper mWrapper[];
    String mFilename = null;
    MediaExtractor mExtractor;
    Thread mThread = null;

    public boolean init(String filename, Surface surface) {
        mFilename = filename;
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(mFilename);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        int numTracks = mExtractor.getTrackCount();
        mWrapper = new MediaCodecCommonWrapper[numTracks];
        Log.i(TAG, "extract: " + numTracks);
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.indexOf("audio") != -1) {
                mWrapper[i] = new MediaCodecAudioWrapper();
                if (mWrapper[i].init(format,null) == false) {
                    Log.e(TAG, "extract: invalid format" + mime );
                    return false;
                }
                Log.i(TAG, "create codec: " + mime);
                mExtractor.selectTrack(i);

            } else if (mime.indexOf("video") != -1) {
                mWrapper[i] = new MediaCodecVideoWrapper();
                if (mWrapper[i].init(format,surface) == false) {
                    Log.e(TAG, "extract: invalid format" + mime );
                    return false;
                }
                Log.i(TAG, "create codec: " + mime);
                mExtractor.selectTrack(i);
            }
        }
        MediaCodecVideoWrapper video = (MediaCodecVideoWrapper)getMediaCodec("VIDEO");
        MediaClock audio = (MediaClock)getMediaCodec("AUDIO");
        video.setMediaClock(audio);
        return true;
    }

    public void extract() {
        mThread = new Thread(this);
        mThread.start();
    }
    public boolean pause() {
        return getMediaCodec("AUDIO").pause();
    }

    private MediaCodecCommonWrapper getMediaCodec(String type) {
        for (int i = 0; i < mWrapper.length;i++) {
            if (mWrapper[i].getType() == type) {
                return mWrapper[i];
            }
        }
        return null;
    }

    public void run() {
        while (true) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(128*1024);
            if (mExtractor.readSampleData(inputBuffer,0) <0) {
                break;
            }
            int trackIndex = mExtractor.getSampleTrackIndex();
            long presentationTimeUs = mExtractor.getSampleTime();
            Log.d(TAG, "extract: " + trackIndex + ":" + presentationTimeUs +
                    " remaining=" + inputBuffer.remaining() +
                    " pos=" + inputBuffer.position() +
                    " limit=" + inputBuffer.limit() +
                    " capa=" + inputBuffer.capacity());

            boolean writeResult = false;
            while (true) {
                writeResult = mWrapper[trackIndex].write(inputBuffer,presentationTimeUs);
                if (writeResult == false){
                    try {
                        Thread.sleep(100); //3000ミリ秒Sleepする
                    } catch (InterruptedException e) {
                    }
                } else {
                    break;
                }
            }
            mExtractor.advance();
        }
        mExtractor.release();
        mExtractor = null;
    }
}
