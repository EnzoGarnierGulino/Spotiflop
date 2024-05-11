package com.example.spotiflop;

import static android.os.SystemClock.sleep;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomappbar.BottomAppBar;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import Demo.PrinterPrx;
import Demo.StreamingInfo;

import com.squareup.picasso.Picasso;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private static ArrayList<Song> songs = null;
    private LibVLC libVLC;
    private org.videolan.libvlc.MediaPlayer mediaPlayer;
    private boolean streaming = false;
    private PrinterPrx printerPrx;
    private Timer endOfSongDetectionTimer = new Timer();
    private Thread endOfSongDetectionThread;
    protected BottomAppBar songbar;
    private String streamURL;
    private TextView songName;
    private TextView bottomBarButton;
    private boolean hasPlayed = false;


    public SongAdapter(ArrayList<Song> songs, BottomAppBar songbar, Context context, PrinterPrx printerPrx) {
        SongAdapter.songs = songs;
        libVLC = new LibVLC(context, new ArrayList<String>());
        this.songbar = songbar;
        songName = songbar.findViewById(R.id.songName);
        songName.setText("No song playing");
        bottomBarButton = songbar.findViewById(R.id.playPause);
        bottomBarButton.setText("▶️");
        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
        this.printerPrx = printerPrx;

        bottomBarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPlayed) {
                    return;
                }
                if (streaming) {
                    bottomBarButton.setText("▶️");
                    printerPrx.playPauseMusic();
                    if (endOfSongDetectionThread != null) {
                        endOfSongDetectionThread.interrupt();
                    }
                    streaming = false;
                    endOfSongDetectionTimer.cancel();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mediaPlayer.pause();
                        }
                    }).start();
                } else {
                    streaming = true;
                    setupMediaPlayer(streamURL);
                    mediaPlayer.play();
                    bottomBarButton.setText("⏸️");
                    long duration = printerPrx.playPauseMusic();
                    endOfSongDetectionTimer.cancel();
                    endOfSongDetectionTimer = new Timer();
                    endOfSongDetectionTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            setupEndSongDetection();
                        }
                    }, duration);
                }
            }
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewAuthor;
        public ImageView coverart;


        public ViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            coverart = itemView.findViewById(R.id.coverart);

            coverart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Song clickedSong = songs.get(position);
                        songName.setText(clickedSong.getTitle());
                        playSong(clickedSong.getTitle());
                    }
                }
            });

        }
    }

    private void setupMediaPlayer(String url) {
        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
        Media media = new Media(libVLC, Uri.parse(url));
//        media.setDefaultMediaPlayerOptions();
//        media.addOption("--network-caching=<1000ms>");
//        media.addOption("--no-video");
        mediaPlayer.setMedia(media);
    }

    public void playSong(String title) {
        hasPlayed = true;
        if (streaming) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.stop();
                }
            }).start();
            streaming = false;
            bottomBarButton.setText("▶️");
            return;
        }
        StreamingInfo info = printerPrx.playMusic(title);
        System.out.println("url : " + info.url + " duration : " + info.duration + " ip : " + info.clientIP);
        streamURL = info.url;
        if (!streaming) {
            setupMediaPlayer(streamURL);
            mediaPlayer.play();
            bottomBarButton.setText("⏸️");
            streaming = true;
            long durationMS = info.duration;

            endOfSongDetectionTimer.cancel();
            endOfSongDetectionTimer = new Timer();
            endOfSongDetectionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    setupEndSongDetection();
                }
            }, durationMS);
        }
    }

    private void setupEndSongDetection() {
        if (endOfSongDetectionThread != null) {
            endOfSongDetectionThread.interrupt();
        }
        endOfSongDetectionThread = new Thread(() -> startEndSongDetection());
        endOfSongDetectionThread.start();
    }

    private void startEndSongDetection() {
        List<Integer> list = new ArrayList<>();
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            int readBytes = Objects.requireNonNull(mediaPlayer.getMedia()).getStats().demuxReadBytes;
            if (!list.isEmpty() && readBytes != 0 && readBytes == list.get(list.size() - 1)) {
                list.add(readBytes);
                System.out.println(list);
                onSongFinished();
                break;
            }
            else {
                list.add(readBytes);
                sleep(500);
            }
        }
    }

    private void onSongFinished() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        System.out.println("Song finished");
        hasPlayed = false;
        streaming = false;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                songName.setText("No song playing");
                bottomBarButton.setText("▶️");
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.stop();
            }
        }).start();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.textViewTitle.setText(song.getTitle());
        holder.textViewAuthor.setText(song.getAuthor());
        Picasso.get().load(song.getCoverart()).into(holder.coverart);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}