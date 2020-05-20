package com.example.myapplication;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaExtractorWrapper {
    final private String TAG = "MediaExtractorWrapper";
    MediaCodecCommonWrapper mWrapper[];

    public void extract(String filename, Surface surface) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(filename);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int numTracks = extractor.getTrackCount();
        mWrapper = new MediaCodecCommonWrapper[numTracks];
        Log.i(TAG, "extract: " + numTracks);
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.indexOf("audio") != -1) {
                mWrapper[i] = new MediaCodecAudioWrapper();
                if (mWrapper[i].init(format,null) == false) {
                    Log.e(TAG, "extract: invalid format" + mime );
                    return;
                }
                Log.i(TAG, "create codec: " + mime);
                extractor.selectTrack(i);

            } else if (mime.indexOf("video") != -1) {
                mWrapper[i] = new MediaCodecVideoWrapper();
                if (mWrapper[i].init(format,surface) == false) {
                    Log.e(TAG, "extract: invalid format" + mime );
                    return;
                }
                Log.i(TAG, "create codec: " + mime);
                extractor.selectTrack(i);
            }
        }

        while (true) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(128*1024);

            if (extractor.readSampleData(inputBuffer,0) <0) {
                break;
            }
            int trackIndex = extractor.getSampleTrackIndex();
            long presentationTimeUs = extractor.getSampleTime();
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
            extractor.advance();
        }

        extractor.release();
        extractor = null;
    }
}
