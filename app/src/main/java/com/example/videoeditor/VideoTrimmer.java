package com.example.videoeditor;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoTrimmer {

    @SuppressLint("WrongConstant")
    public static boolean trimVideo(String inputPath, String outputPath, long startMs, long endMs) {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        try {
            // Set data sources for both video and audio
            videoExtractor.setDataSource(inputPath);
            audioExtractor.setDataSource(inputPath);

            // Select the video track
            int videoTrackIndex = selectTrack(videoExtractor, "video/");
            if (videoTrackIndex < 0) {
                Log.e("VideoTrimmer", "No video track found in file");
                return false;
            }
            videoExtractor.selectTrack(videoTrackIndex);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(videoTrackIndex);

            // Select the audio track
            int audioTrackIndex = selectTrack(audioExtractor, "audio/");
            if (audioTrackIndex < 0) {
                Log.e("VideoTrimmer", "No audio track found in file");
                return false;
            }
            audioExtractor.selectTrack(audioTrackIndex);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(audioTrackIndex);

            // Initialize MediaMuxer for the output file
            MediaMuxer muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // Add both video and audio tracks to the muxer
            int videoOutputTrackIndex = muxer.addTrack(videoFormat);
            int audioOutputTrackIndex = muxer.addTrack(audioFormat);

            muxer.start();

            // Seek to the start time for both video and audio
            videoExtractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            audioExtractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            ByteBuffer videoBuffer = ByteBuffer.allocate(1024 * 1024);
            ByteBuffer audioBuffer = ByteBuffer.allocate(1024 * 1024);

            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();

            // Process video and audio
            while (true) {
                // Read video sample
                int videoSampleSize = videoExtractor.readSampleData(videoBuffer, 0);
                if (videoSampleSize < 0) break; // End of stream

                long videoSampleTimeUs = videoExtractor.getSampleTime();
                if (videoSampleTimeUs > endMs * 1000) break; // Stop when reaching the end time

                videoBufferInfo.size = videoSampleSize;
                videoBufferInfo.presentationTimeUs = videoSampleTimeUs;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                videoBufferInfo.offset = 0;

                muxer.writeSampleData(videoOutputTrackIndex, videoBuffer, videoBufferInfo);
                videoExtractor.advance();

                // Read audio sample
                int audioSampleSize = audioExtractor.readSampleData(audioBuffer, 0);
                if (audioSampleSize < 0) break; // End of stream

                long audioSampleTimeUs = audioExtractor.getSampleTime();
                if (audioSampleTimeUs > endMs * 1000) break; // Stop when reaching the end time

                audioBufferInfo.size = audioSampleSize;
                audioBufferInfo.presentationTimeUs = audioSampleTimeUs;
                audioBufferInfo.flags = audioExtractor.getSampleFlags();
                audioBufferInfo.offset = 0;

                muxer.writeSampleData(audioOutputTrackIndex, audioBuffer, audioBufferInfo);
                audioExtractor.advance();
            }

            muxer.stop();
            muxer.release();
            videoExtractor.release();
            audioExtractor.release();

            Log.d("VideoTrimmer", "Video and Audio trimmed successfully: " + outputPath);
            return true;

        } catch (IOException e) {
            Log.e("VideoTrimmer", "Error trimming video: " + e.getMessage());
            return false;
        }
    }

    private static int selectTrack(MediaExtractor extractor, String mimePrefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(mimePrefix)) {
                return i;
            }
        }
        return -1;
    }
}
