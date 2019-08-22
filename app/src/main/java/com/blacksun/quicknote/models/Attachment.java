package com.blacksun.quicknote.models;

import android.database.Cursor;

import com.blacksun.quicknote.data.NoteContract;

public class Attachment {
    private long id;
    private long note_id;
    private String type;
    private String path;

    public Attachment(long id, long note_id, String type, String path) {
        this.id = id;
        this.note_id = note_id;
        this.type = type;
        this.path = path;
    }

    public Attachment() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNote_id() {
        return note_id;
    }

    public void setNote_id(long note_id) {
        this.note_id = note_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public static Attachment getAttachFromCursor(Cursor cursor){
        Attachment attach = new Attachment();
        attach.setId(cursor.getLong(cursor.getColumnIndex(NoteContract.AttachEntry.ID)));
        attach.setNote_id(cursor.getLong(cursor.getColumnIndex(NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID)));
        attach.setPath(cursor.getString(cursor.getColumnIndex(NoteContract.AttachEntry.COLUMN_ATTACH_PATH)));
        attach.setType(cursor.getString(cursor.getColumnIndex(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE)));
        return attach;
    }
}
