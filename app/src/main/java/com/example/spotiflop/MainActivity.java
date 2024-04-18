package com.example.spotiflop;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.spotiflop.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.Locale;

import Demo.PrinterPrx;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static PrinterPrx printerPrx;
    private static com.zeroc.Ice.Communicator communicator;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private LibVLC libVLC;
    private org.videolan.libvlc.MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        setupMediaPlayer();

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @OptIn(markerClass = UnstableApi.class) @Override
            public void onClick(View view) {
                Snackbar.make(view, "Parlez !", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
//                startRecording();
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.play();
                }

            }
        });

        try {
            String[] customArgs = new String[]{"--Ice.MessageSizeMax=0"};
            communicator = com.zeroc.Ice.Util.initialize(customArgs);
            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("SimplePrinter:tcp -h 192.168.1.43 -p 10000");
            PrinterPrx printer = PrinterPrx.checkedCast(base);
            if (printer == null) {
                throw new Error("Invalid proxy");
            }
            MainActivity.printerPrx = printer;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRecording() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez maintenant...");
        try {
            startActivityForResult(intent, REQUEST_RECORD_AUDIO_PERMISSION);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(),
                    "Reconnaissance vocale non prise en charge sur cet appareil.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMediaPlayer() {
        libVLC = new LibVLC(this);
        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
        String rtspUri = "rtsp://192.168.1.43:8554/stream";
        Media media = new Media(libVLC, Uri.parse(rtspUri));
        media.setDefaultMediaPlayerOptions();
//        media.addOption("--network-caching=<1000ms>");
        media.addOption("--no-video");
        media.addOption("-vvv");
        mediaPlayer.setMedia(media);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String transcription = result.get(0);
                    Log.d("Transcription", transcription);
                    Snackbar.make(binding.getRoot(), transcription, Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}