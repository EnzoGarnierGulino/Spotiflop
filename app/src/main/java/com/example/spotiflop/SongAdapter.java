package com.example.spotiflop;

import static android.os.SystemClock.sleep;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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
    private TextView nextSong;
    private ArrayList<Song> queue = new ArrayList<Song>();
    private boolean hasPlayed = false;
    private FloatingActionButton playPauseButton;
    private final int playImg = R.drawable.play_img;
    private final int pauseImg = R.drawable.pause_img;


    public SongAdapter(ArrayList<Song> songs, BottomAppBar songbar, Context context, PrinterPrx printerPrx) {
        SongAdapter.songs = songs;
        libVLC = new LibVLC(context, new ArrayList<String>());
        this.songbar = songbar;
        songName = songbar.findViewById(R.id.songName);
        nextSong = songbar.findViewById(R.id.nextSong);
        songName.setText("No song playing");
        playPauseButton = songbar.findViewById(R.id.playPauseButton);
        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
        this.printerPrx = printerPrx;

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPlayed) {
                    return;
                }
                if (streaming) {
                    playPauseButton.setImageResource(playImg);
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
                    playPauseButton.setImageResource(pauseImg);
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
        public Button addToQueue;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            coverart = itemView.findViewById(R.id.coverart);
            addToQueue = itemView.findViewById(R.id.addToQueue);

            coverart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Song clickedSong = songs.get(position);
                        songName.setText(clickedSong.getTitle());
                        playSong(clickedSong.getQueryName());
                    }
                }
            });

            addToQueue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Song clickedSong = songs.get(position);
                        queue.add(clickedSong);
                        nextSong.setText("- Next: " + queue.get(0).getTitle());
                        Snackbar.make(songbar, "Song successfully added to queue !", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void setupMediaPlayer(String url) {
        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
        Media media = new Media(libVLC, Uri.parse(url));
        mediaPlayer.setMedia(media);
    }

    public void playSong(String queryName) {
        hasPlayed = true;
        if (streaming) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.stop();
                }
            }).start();
            streaming = false;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    playPauseButton.setImageResource(playImg);
                }
            });

        }
        StreamingInfo info = printerPrx.playMusic(queryName);
        if (info == null) {
            Snackbar.make(songbar, "Song not found", Snackbar.LENGTH_LONG).show();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    songName.setText("No song playing");
                }
            });
            return;
        }
        Handler handler2 = new Handler(Looper.getMainLooper());
        handler2.post(new Runnable() {
            @Override
            public void run() {
                String[] words = queryName.split("_");
                StringBuilder transformedString = new StringBuilder();
                for (String word : words) {
                    if (word.length() > 0) {
                        transformedString.append(Character.toUpperCase(word.charAt(0)))
                                .append(word.substring(1))
                                .append(" ");
                    }
                }
                String result = transformedString.toString().trim();
                if (!result.isEmpty()) {
                    songName.setText(result);
                }
            }
        });
        System.out.println("url : " + info.url + " duration : " + info.duration + " ip : " + info.clientIP);
        streamURL = info.url;
        if (!streaming) {
            sleep(500);
            setupMediaPlayer(streamURL);
            mediaPlayer.play();
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    playPauseButton.setImageResource(pauseImg);
                }
            });
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
                if (!queue.isEmpty()) {
                    playSong(queue.get(0).getQueryName());
                    songName.setText(queue.get(0).getTitle());
                    queue.remove(0);
                    if (!queue.isEmpty()) {
                        nextSong.setText("- Next: " + queue.get(0).getTitle());
                    } else {
                        nextSong.setText("");
                    }
                } else {
                    songName.setText("No song playing");
                    playPauseButton.setImageResource(playImg);
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.stop();
            }
        }).start();
    }

    public void processAction(LLMResponse llmResponse) {
        if (llmResponse.getAction().equals("play")) {
            String parsedSong = llmResponse.getSubject().replace(" ", "_").toLowerCase();
            playSong(parsedSong);
        }
        if (llmResponse.getAction().equals("pause")) {
            if (streaming) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        playPauseButton.setImageResource(playImg);
                    }
                });
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
            }
        }
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
        holder.textViewAuthor.setText("By " + song.getAuthor());
        Picasso.get().load(song.getCoverart()).into(holder.coverart);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}