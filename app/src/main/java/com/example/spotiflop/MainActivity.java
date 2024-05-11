package com.example.spotiflop;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotiflop.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import Demo.PrinterPrx;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ArrayList<Song> songs;
    private RecyclerView recyclerView;
    private SongAdapter adapter;

    private BottomAppBar songbar;
    private static PrinterPrx printerPrx;
    private static com.zeroc.Ice.Communicator communicator;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        songbar = findViewById(R.id.songbar);


        setSupportActionBar(binding.toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @OptIn(markerClass = UnstableApi.class) @Override
            public void onClick(View view) {
                Snackbar.make(view, "Parlez !", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
                startRecording();
            }
        });

        try {
            String[] customArgs = new String[]{"--Ice.MessageSizeMax=0"};
            communicator = com.zeroc.Ice.Util.initialize(customArgs);
            com.zeroc.Ice.ObjectPrx base = communicator.stringToProxy("SimplePrinter:tcp -h 192.168.1.12 -p 10000");
            PrinterPrx printer = PrinterPrx.checkedCast(base);
            if (printer == null) {
                throw new Error("Invalid proxy");
            }
            MainActivity.printerPrx = printer;
        } catch (Exception e) {
            e.printStackTrace();
        }

        String songListString = printerPrx.getSongList();
        parseSongString(songListString);
    }

    private void parseSongString(String songListString) {
        try {
            JSONArray jsonArray = new JSONArray(songListString);
            songs = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("id");
                String title = jsonObject.getString("title");
                String author = jsonObject.getString("author");
                String coverart = jsonObject.getString("coverart");
                songs.add(new Song(id, title, author, coverart));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(songs, songbar, this, printerPrx);
        recyclerView.setAdapter(adapter);
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
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String transcription = result.get(0);
                    Log.d("Transcription", transcription);
                    makeRequest(transcription);
                    Snackbar.make(binding.getRoot(), transcription, Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    private void makeRequest(String transcription) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Define the URL for the request
                    String url = "http://82.66.48.233:42690/getObjectAndSubject?query=" + transcription;

                    // Create a URL object from the string URL
                    URL serverUrl = new URL(url);

                    // Create a HttpURLConnection object to open the connection
                    HttpURLConnection conn = (HttpURLConnection) serverUrl.openConnection();

                    // Set the request method to POST
                    conn.setRequestMethod("POST");

                    // Set the content type
                    conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);

                    // Get the response code
                    int responseCode = conn.getResponseCode();
                    Log.d("HTTP Response Code", String.valueOf(responseCode));

                    // Read the response from the input stream
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Parse the response JSON to extract the action and object
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String action = jsonResponse.getString("action");
                    String object = jsonResponse.getString("sujet");

                    // Display the action and object (e.g., using a Snackbar)
                    String message = "Action: " + action + ", Object: " + object;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                        }
                    });

                    // Disconnect the HttpURLConnection
                    conn.disconnect();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
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

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}