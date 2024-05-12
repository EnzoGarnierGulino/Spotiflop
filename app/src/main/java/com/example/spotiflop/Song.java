package com.example.spotiflop;

public class Song {
    private int id;
    private String title;
    private String author;
    private String coverart;
    private String queryName;

    public Song(int id, String title, String author, String coverart, String queryName) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.coverart = coverart;
        this.queryName = queryName;
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

    public String getCoverart() { return coverart; }

    public String getQueryName() {
        return queryName;
    }
}

