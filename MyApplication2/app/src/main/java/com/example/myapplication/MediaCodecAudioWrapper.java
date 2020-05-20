package com.example.myapplication;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.provider.MediaStore;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaCodecAudioWrapper extends MediaCodecCommonWrapper {
    AudioTrack mTrack;
    String TAG = "MediaCodecAudioWrapper";

    @Override
    protected boolean processOutputFormat() {
        MediaCodec.BufferInfo output = new MediaCodec.BufferInfo();
        int outputIndex = mCodec.dequeueOutputBuffer(output,100);
        if (outputIndex >= 0) {
            Log.i(TAG, "processOutputFormat: " + outputIndex);
            ByteBuffer outBuffer = mCodec.getOutputBuffer(outputIndex);
            write(outBuffer);
            mCodec.releaseOutputBuffer(outputIndex,false);
            outputIndex = -1;
        } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
            Log.i(TAG, "run: output format changed");
            MediaFormat outformat = mCodec.getOutputFormat();
            setFormat(outformat);
        }
        return true;
    }

    protected boolean setFormat(MediaFormat format) {
        int ch = AudioFormat.CHANNEL_OUT_MONO;
        Log.i(TAG, "setFormat: mime "+ format.getString(MediaFormat.KEY_MIME));
        Log.i(TAG, "setFormat: ch "+ format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
        if (format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == 2) {
            ch = AudioFormat.CHANNEL_OUT_STEREO;
        }
        Log.i(TAG, "setFormat: rate "+ format.getInteger(MediaFormat.KEY_SAMPLE_RATE));

        int bufSize = AudioTrack.getMinBufferSize(
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                ch, AudioFormat.ENCODING_PCM_16BIT);
        mTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                        .setChannelMask(ch)
                        .build()
                )
                .setBufferSizeInBytes(bufSize)
                .build();
        mTrack.play();
        return true;
    }

    protected boolean write(ByteBuffer buffer) {
        Log.i(TAG, "write: " + buffer.remaining());
        //int writeSize = mTrack.write(buffer.array(),0,buffer.remaining());
        int writeSize = mTrack.write(buffer,buffer.remaining(),AudioTrack.WRITE_BLOCKING);
        Log.i(TAG, "write: " + writeSize);
        return true;
    }
}
