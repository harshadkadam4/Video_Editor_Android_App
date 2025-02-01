package com.example.videoeditor;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.transformer.DefaultAssetLoaderFactory;
import androidx.media3.transformer.TransformationRequest;
import androidx.media3.transformer.Transformer;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.ui.PlayerView;

import com.google.android.material.slider.RangeSlider;

import java.io.File;


public class MainActivity extends AppCompatActivity {


    VideoView video;
    Button selectVideo,save;
    RangeSlider slider;
    ImageView trim;


    ActivityResultLauncher<Intent> resultLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        video = findViewById(R.id.video);
        selectVideo = findViewById(R.id.selectVideo);
        save  = findViewById(R.id.save);
        slider = findViewById(R.id.slider);
        trim  = findViewById(R.id.trim);

        //slider.setValues(0f,7f);

        registerResult();

        selectVideo.setOnClickListener(v -> pickVideo());

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(video);
        video.setMediaController(mediaController);

        trim.setOnClickListener(v -> {

        });

    }

    //Videp Picking
    private void pickVideo()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        resultLauncher.launch(intent);
    }

    private void registerResult()
    {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getData() != null && result.getData().getData() != null){
                            Uri videoUri = result.getData().getData();

                            // Duration Calculation
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(getApplicationContext(),videoUri);
                            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            long timeMillisec = Long.parseLong(time);
                            float timeInsec = timeMillisec/1000;
                            slider.setValueTo(timeInsec);
                            slider.setValues(0f,timeInsec);

                            video.setVideoURI(videoUri);

                            //video.start();
                            video.setOnPreparedListener(mp -> {
                                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                                save.setVisibility(View.VISIBLE);
                                slider.setVisibility(View.VISIBLE);
                                //slider.setValues(0f,timeInsec);
                            });
                            video.start();
                        }
                        else
                        {
                            Toast.makeText(MainActivity.this, "No Video Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }



    //Trim Video
    @OptIn(markerClass = UnstableApi.class)
    public void playTrimmedVideo(Context context, String videoUri, long startMs, long endMs)
    {
        ExoPlayer player = new ExoPlayer.Builder(context).build();

        MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(videoUri)).build();
        MediaSource mediaSource = new DefaultMediaSourceFactory(context).createMediaSource(mediaItem);

        ClippingMediaSource clippedSource = new ClippingMediaSource(mediaSource,
                Util.msToUs(startMs), Util.msToUs(endMs));

        player.setMediaSource(clippedSource);
        player.prepare();
        player.play();
    }



}
















