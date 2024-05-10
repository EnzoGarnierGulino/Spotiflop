package com.example.spotiflop;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import Demo.PrinterPrx;
import Demo.StreamingInfo;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private static ArrayList<Song> songs = null;
    private LibVLC libVLC;
    private org.videolan.libvlc.MediaPlayer mediaPlayer;
    private boolean streaming = false;
    private PrinterPrx printerPrx;
    private Timer endOfSongTimer;

    public SongAdapter(ArrayList<Song> songs, Context context, PrinterPrx printerPrx) {
        SongAdapter.songs = songs;
        libVLC = new LibVLC(context, new ArrayList<String>());
        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
        this.printerPrx = printerPrx;

        mediaPlayer.setEventListener(new org.videolan.libvlc.MediaPlayer.EventListener() {
            @Override
            public void onEvent(org.videolan.libvlc.MediaPlayer.Event event) {
                switch (event.type) {
                    case org.videolan.libvlc.MediaPlayer.Event.EndReached:
                        System.out.println("=========================================EndReached=========================================");
                        break;
                    case MediaPlayer.Event.Paused:
                        System.out.println("=========================================Paused=========================================");
                        break;
                    case MediaPlayer.Event.Stopped:
                        System.out.println("=========================================Stopped=========================================");
                        break;
                    case MediaPlayer.Event.Playing:
                        System.out.println("=========================================Playing=========================================");
                        break;
                }
            }
        });

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewTitle;
        public TextView textViewAuthor;
        public Button playButton;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            playButton = itemView.findViewById(R.id.playButton);

            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Get the position of the clicked item
                    int position = getAdapterPosition();
                    // Check if the position is valid (RecyclerView.NO_POSITION indicates a deleted item)
                    if (position != RecyclerView.NO_POSITION) {
                        // Handle the click event, e.g., pass the position to the activity or perform some action
                        // For example, you can retrieve the Song object associated with the clicked item
                        Song clickedSong = songs.get(position);
                        // Do something with the clicked song, such as playing it
                        System.out.println("clicked on element : " + clickedSong.getTitle());
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
        if (endOfSongTimer != null) {
            endOfSongTimer.cancel();
        }

        if (streaming) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mediaPlayer.stop();
                }
            }).start();
            streaming = false;
            return;
        }
        StreamingInfo info = printerPrx.playMusic(title);
        System.out.println("url : " + info.url + " duration : " + info.duration + " ip : " + info.clientIP);
        if (!streaming) {
            setupMediaPlayer(info.url);
            mediaPlayer.play();
            streaming = true;
            int durationSec = info.duration;

            endOfSongTimer = new Timer();
            endOfSongTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    onSongFinished();
                }
            }, durationSec * 1000L);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<Integer> list = new ArrayList<>();
                        while (true) {
                            int readBytes = mediaPlayer.getMedia().getStats().demuxReadBytes;
                            if (!list.isEmpty() && readBytes != 0 && readBytes == list.get(list.size() - 1)) {
                                list.add(readBytes);
                                System.out.println(list);
                                break;
                            }
                            else {
                                list.add(readBytes);
                            }
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    private void onSongFinished() {
        System.out.println("=========================================Song Finished=========================================");
//        streaming = false;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                mediaPlayer.stop();
//            }
//        }).start();
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
        holder.playButton.setText("Play");
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}