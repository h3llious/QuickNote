package com.blacksun.quicknote.models;

public class Note {
    private String title;
    private String content;
    private long id;
    private long dateCreated;
    private long dateModified;

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getId() {
        return id;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public long getDateModified() {
        return dateModified;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDateCreated(long dateCreated) {
        this.dateCreated = dateCreated;
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }
}
