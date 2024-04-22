package com.example.spotiflop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
    private static ArrayList<Song> songs = null;

    public SongAdapter(ArrayList<Song> songs) {
        SongAdapter.songs = songs;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
                    }
                }
            });
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
        holder.textViewAuthor.setText(song.getAuthor());
        holder.playButton.setText("Play");
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}