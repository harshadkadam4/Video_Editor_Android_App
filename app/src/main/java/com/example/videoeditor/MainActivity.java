package com.example.videoeditor;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ClippingMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.transformer.DefaultAssetLoaderFactory;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.EditedMediaItemSequence;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {


    private PlayerView playerView;
    private ExoPlayer player;
    Button selectVideo, save;
    RangeSlider slider;
    ImageView trim, crop;
    TextView tv_trim, tv_crop,startSec,endSec;
    private FileOp fileOp = new FileOp();
    //Uri inputUri;

    ActivityResultLauncher<Intent> resultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.video);
        selectVideo = findViewById(R.id.selectVideo);
        save = findViewById(R.id.save);
        slider = findViewById(R.id.slider);
        trim = findViewById(R.id.trim);
        crop = findViewById(R.id.crop);
        tv_trim = findViewById(R.id.tv_trim);
        tv_crop = findViewById(R.id.tv_crop);
        startSec = findViewById(R.id.startSec);
        endSec = findViewById(R.id.endSec);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        registerResult();

        selectVideo.setOnClickListener(v -> pickVideo());

        slider.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                startSec.setText(slider.getValues().get(0).toString());
                endSec.setText(slider.getValues().get(1).toString());
            }
        });

        trim.setOnClickListener(v -> {
            float startMs = slider.getValues().get(0) * 1000;
            float endMs = slider.getValues().get(1) * 1000;

            File cacheDir = this.getCacheDir();
            File cachedVideoFile = new File(cacheDir,"cached_video.mp4");
            File copyCachedVideoFile = new File(cacheDir,"copy_cached_video.mp4");

            fileOp.copyFile(cachedVideoFile,copyCachedVideoFile);

            //FFMPEG command in text file
            File externalDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            File ffmpegCommandFile = new File(externalDir, "ffmpeg_command.txt");

            try {
                String startTime = ffTimeString(startMs);
                String endTime = ffTimeString(endMs);

                FileWriter writer = new FileWriter(ffmpegCommandFile);
                writer.write("ffmpeg -i "+copyCachedVideoFile.getAbsolutePath()+" -ss "+startTime+" -to "+endTime+" -c:v copy -c:a copy "+cachedVideoFile.getAbsolutePath());
                writer.close();
                Toast.makeText(this, "Created "+ffmpegCommandFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
            catch (IOException e)
            {
                Toast.makeText(this, ""+e, Toast.LENGTH_SHORT).show();
            }


            //String inputPath = getRealPathFromVideoURI(this,inputUri);
            boolean success = VideoTrimmer.trimVideo(copyCachedVideoFile.getAbsolutePath(), cachedVideoFile.getAbsolutePath(), (long)startMs, (long) endMs);

            if (success) {
                Toast.makeText(this, "Saved in Cache", Toast.LENGTH_SHORT).show();

                Uri cacheVideUri = Uri.fromFile(cachedVideoFile);

                //inputUri = FileProvider.getUriForFile(this,"com.example.videoeditor.fileprovider",cachedVideoFile);
                MediaItem mediaItem = MediaItem.fromUri(cacheVideUri);

                player.setMediaItem(mediaItem);
                player.prepare();
                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                        if(playbackState == Player.STATE_READY)
                        {
                            long durationMs = player.getDuration();

                            if(durationMs != C.TIME_UNSET)
                            {
                                slider.setValueTo(durationMs / 1000f);
                                slider.setValues(0f, durationMs / 1000f);
                                startSec.setText(slider.getValues().get(0).toString());
                                endSec.setText(slider.getValues().get(1).toString());
                            }
                            player.play();
                        }
                    }
                });


            }

        /*    if(inputUri != null)
            {
                float startMs = slider.getValues().get(0) * 1000;
                float endMs = slider.getValues().get(1) * 1000;

                trimVide(this,inputUri.toString(),(long) startMs, (long) endMs);
            }
            else{
                Toast.makeText(this, "Select Video", Toast.LENGTH_SHORT).show();
            }

         */
        });

        save.setOnClickListener(v -> {

            AlertDialog.Builder builder =new AlertDialog.Builder(this);
            builder.setTitle("Enter File Name");

            final EditText input = new EditText(this);

            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String outputPath = "/storage/emulated/0/Movies/"+input.getText().toString()+".mp4";
                    File outputFile = new File(outputPath);

                    File cacheDir = getApplicationContext().getCacheDir();
                    File cachedVideoFile = new File(cacheDir,"cached_video.mp4");

                    fileOp.saveFile(MainActivity.this, cachedVideoFile, outputFile);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

            /*
            String inputPath = getRealPathFromVideoURI(this,inputUri);
            String outputPath = "/storage/emulated/0/Movies/output_trimmed.mp4";

            float startMs = slider.getValues().get(0) * 1000;
            float endMs = slider.getValues().get(1) * 1000;

            boolean success = VideoTrimmer.trimVideo(inputPath, outputPath, (long)startMs, (long) endMs); // Trim from 5s to 15s
            if (success) {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            }

             */
        });

    }

    //Videp Picking
    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        resultLauncher.launch(intent);
    }

    private void registerResult() {
        resultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getData() != null && result.getData().getData() != null) {
                            Uri videoUri = result.getData().getData();
                            //inputUri = videoUri;

                            // Load file in Cache
                            try {
                                File cacheDir = getApplicationContext().getCacheDir();
                                File cachedVideoFile = new File(cacheDir,"cached_video.mp4");

                                FileOp.copyUriToFile(MainActivity.this,videoUri,cachedVideoFile);
                            } catch (IOException e)
                            {
                                e.printStackTrace();
                            }

                            // Duration Calculation
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(getApplicationContext(), videoUri);
                            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            long timeMillisec = Long.parseLong(time);
                            float timeInsec = timeMillisec / 1000f;

                            //float timeInsec = video.getDuration();
                            slider.setValueTo(timeInsec);
                            slider.setValues(0f, timeInsec);

                            MediaItem mediaItem = new MediaItem.Builder().setUri(videoUri).build();
                            player.setMediaItem(mediaItem);
                            player.prepare();
                            player.play();
                            //video.start();
                            save.setVisibility(View.VISIBLE);
                            slider.setVisibility(View.VISIBLE);
                            trim.setVisibility(View.VISIBLE);
                            crop.setVisibility(View.VISIBLE);
                            tv_trim.setVisibility(View.VISIBLE);
                            tv_crop.setVisibility(View.VISIBLE);
                            startSec.setVisibility(View.VISIBLE);
                            endSec.setVisibility(View.VISIBLE);

                            startSec.setText(slider.getValues().get(0).toString());
                            endSec.setText(slider.getValues().get(1).toString());

                        } else {
                            Toast.makeText(MainActivity.this, "Video Not Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    //Trim Video
    @OptIn(markerClass = UnstableApi.class)
    public void trimVide(Context context, String videoUri, long startMs, long endMs)
    {
        MediaItem mediaItem = new MediaItem.Builder().setUri(Uri.parse(videoUri)).build();
        MediaSource mediaSource = new DefaultMediaSourceFactory(context).createMediaSource(mediaItem);

        ClippingMediaSource clippedSource = new ClippingMediaSource(mediaSource,
                Util.msToUs(startMs), Util.msToUs(endMs));

        player.setMediaSource(clippedSource);
        player.prepare();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if(playbackState == Player.STATE_READY)
                {
                    long durationMs = player.getDuration();

                    if(durationMs != C.TIME_UNSET)
                    {
                        slider.setValueTo(durationMs / 1000f);
                        slider.setValues(0f, durationMs / 1000f);
                    }
                    player.play();
                }
            }
        });
    }


    private void copyUriToFile(Context context, Uri uri, File file) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        outputStream.close();
    }


    public String getRealPathFromVideoURI(Context context, Uri videoUri) {
        String filePath = null;
        try {
            Cursor cursor = context.getContentResolver().query(videoUri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    String fileName = cursor.getString(nameIndex);
                    File file = new File(context.getCacheDir(), fileName);
                    copyUriToFile(context, videoUri, file);
                    filePath = file.getAbsolutePath();
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }

    public String ffTimeString(float milliSec)
    {
        int hours = (int) (milliSec / (1000 * 60 *60));
        int minutes = (int) ((milliSec % (1000 * 60 *60)) / (1000 * 60));
        int seconds = (int) ((milliSec % (1000 * 60)) / 1000);
        int milliSeconds = (int) (milliSec % 1000);

        return String.format("%02d:%02d:%02d:%02d",hours,minutes,seconds,milliSeconds);
    }

}






