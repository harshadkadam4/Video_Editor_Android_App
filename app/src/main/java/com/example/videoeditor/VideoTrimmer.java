package com.example.videoeditor;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoTrimmer {

    @SuppressLint("WrongConstant")
    public static boolean trimVideo(String inputPath, String outputPath, long startMs, long endMs) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(inputPath);
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                Log.e("VideoTrimmer", "No video track found in file");
                return false;
            }

            extractor.selectTrack(trackIndex);
            MediaFormat format = extractor.getTrackFormat(trackIndex);

            // Initialize MediaMuxer
            MediaMuxer muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int videoTrackIndex = muxer.addTrack(format);
            muxer.start();

            // Seek to the start time
            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) break; // End of stream

                long sampleTimeUs = extractor.getSampleTime();
                if (sampleTimeUs > endMs * 1000) break; // Stop when reaching the end time

                bufferInfo.size = sampleSize;
                bufferInfo.presentationTimeUs = sampleTimeUs;
                bufferInfo.flags = extractor.getSampleFlags();
                bufferInfo.offset = 0;

                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo);
                extractor.advance();
            }

            muxer.stop();
            muxer.release();
            extractor.release();
            Log.d("VideoTrimmer", "Video trimmed successfully: " + outputPath);
            return true;

        } catch (IOException e) {
            Log.e("VideoTrimmer", "Error trimming video: " + e.getMessage());
            return false;
        }
    }

    private static int selectTrack(MediaExtractor extractor) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }
}
