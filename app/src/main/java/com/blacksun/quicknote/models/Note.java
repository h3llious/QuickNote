package com.blacksun.quicknote.models;

import android.database.Cursor;

import com.blacksun.quicknote.data.NoteContract;

public class Note {
    private String title;
    private String content;
    private long id;
    private long dateCreated;
    private long dateModified;
    private int sync;
    private int deleted;

    public Note(String title, String content, long id, long dateCreated, long dateModified) {
        this.title = title;
        this.content = content;
        this.id = id;
        this.dateCreated = dateCreated;
        this.dateModified = dateModified;
    }

    public Note() {
    }

    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    public int getSync() {
        return sync;
    }

    public void setSync(int sync) {
        this.sync = sync;
    }

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

    public static Note getNoteFromCursor(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getLong(cursor.getColumnIndex(NoteContract.NoteEntry.ID)));
        note.setTitle(cursor.getString(cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_NOTE_TITLE)));
        note.setContent(cursor.getString(cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_NOTE_CONTENT)));
        note.setDateCreated(cursor.getLong(cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_NOTE_CRETIME)));
        note.setDateModified(cursor.getLong(cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_NOTE_MODTIME)));
        note.setSync(cursor.getInt(cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_NOTE_SYNC)));
        note.setDeleted(cursor.getInt(cursor.getColumnIndex(NoteContract.NoteEntry.COLUMN_NOTE_DELETED)));
        return note;
    }
}
