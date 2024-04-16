package com.example.spotiflop;

public class Song {
    private int id;
    private String title;
    private String author;
    private String path;

    public Song(int id, String title, String author, String path) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

