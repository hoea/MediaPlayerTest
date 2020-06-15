package com.example.myapplication;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaExtractorWrapper implements Runnable {
    final private String TAG = "MediaExtractorWrapper";

    String mFilename = null;
    MediaExtractor mExtractor;
    Thread mThread = null;
    boolean mRunning = false;

    MediaExtractorWrapperCallback mCallback = null;

    MediaExtractorWrapper(MediaExtractorWrapperCallback callback) {
        mCallback = callback;
    }

    public boolean selectTrack(int idx) {
        Log.i(TAG, "selectTrack: " + idx);
        if (mExtractor == null) {
            return false;
        }
        mExtractor.selectTrack(idx);
        return true;
    }

    public boolean unselectTrack(int idx) {
        Log.i(TAG, "unselectTrack: " + idx);
        if (mExtractor == null) {
            return false;
        }
        mExtractor.unselectTrack(idx);
        return true;
    }

    public boolean stopThread() {
        if (mExtractor == null) {
            return false;
        }
        mRunning = false;
        while(mThread.isAlive() == true) {
            Log.i(TAG, "stop: wait for finishing Extractor ");
            try {
                Thread.sleep(100);
            }catch (InterruptedException e) {
            }
        }
        Log.i(TAG, "stopThread: finished");
        return true;
    }

    public boolean seek(int position) {
        if (mExtractor == null) {
            return false;
        }
        mExtractor.seekTo((long)position, 0);
        return true;
    }

    public MediaFormat[] getContentsInfos(String filename) {
        mFilename = filename;
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(mFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int numTracks = mExtractor.getTrackCount();
        Log.i(TAG, "extract: " + numTracks);
        MediaFormat formats[] = new MediaFormat[numTracks];
        for (int i = 0; i < numTracks; i++) {
            formats[i] = mExtractor.getTrackFormat(i);
            Log.i(TAG, "getContentsInfos: idx=" + i + ":" + formats[i]);
        }
        return formats;
    }

    public void start() {
        mThread = new Thread(this);
        mRunning = true;
        mThread.start();
    }

    public boolean stop() {
        if (mThread.isAlive() == false) {
            Log.w(TAG, "stop: not running");
            return true;
        }
        mRunning = false;
        while(mThread.isAlive() == true) {
            Log.i(TAG, "stop: wait for finishing Extractor ");
            try {
                Thread.sleep(100);
            }catch (InterruptedException e) {
            }
        }
        mExtractor.release();
        mExtractor = null;
        return true;
    }


    public void run() {
        while (mRunning == true) {
            long bufSize = mExtractor.getSampleSize();
            if (bufSize == -1) {
                // EOF
                break;
            }
            ByteBuffer inputBuffer = ByteBuffer.allocate((int)bufSize);
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
            while (mRunning == true) {
                writeResult = mCallback.writeCallback(inputBuffer,presentationTimeUs,trackIndex);
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

        mRunning = false;
    }
}
