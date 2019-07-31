package com.blacksun.quicknote.data;

import com.blacksun.quicknote.models.Attachment;
import com.blacksun.quicknote.models.Note;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

public class NoteManager {
    private Context mContext;
    private static NoteManager sNoteManagerInstance = null;

    public static NoteManager newInstance(Context context) {

        if (sNoteManagerInstance == null) {
            sNoteManagerInstance = new NoteManager(context.getApplicationContext());
        }

        return sNoteManagerInstance;
    }

    private NoteManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    //(C)RUD
    public long create(Note note) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_TITLE, note.getTitle());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CONTENT, note.getContent());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CRETIME, System.currentTimeMillis());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_MODTIME, System.currentTimeMillis());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_SYNC, NoteContract.NoteEntry.NOT_SYNCED);
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_DELETED, NoteContract.NoteEntry.NOT_DELETED);

        //test new img path
//        if (!TextUtils.isEmpty(note.getImagePath()))
//            values.put(NoteContract.NoteEntry.COLUMN_NOTE_IMG, note.getImagePath());

        //values.put(NoteContract.NoteEntry.ID, note.getId());
        Uri result = mContext.getContentResolver().insert(NoteContract.NoteEntry.CONTENT_URI, values);
        if (result != null) {
            long id = Long.parseLong(result.getLastPathSegment());
            Log.i("Log Cursor", " create note name  " + id + " ");
            return id;
        } else
            return -1;
    }

    //just add existing note into db
    public long add(Note note) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_TITLE, note.getTitle());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CONTENT, note.getContent());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CRETIME, note.getDateCreated());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_MODTIME, note.getDateModified());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_SYNC, NoteContract.NoteEntry.SYNCED);
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_DELETED, note.getDeleted());
        values.put(NoteContract.NoteEntry.ID, note.getId());
        Uri result = mContext.getContentResolver().insert(NoteContract.NoteEntry.CONTENT_URI, values);
        if (result != null) {
            long id = Long.parseLong(result.getLastPathSegment());
            Log.i("Log Cursor", " Add existing note name  " + id + " ");
            return id;
        } else
            return -1;
    }

    //C(R)UD
    public ArrayList<Note> getAllNotes() {
        ArrayList<Note> notes = new ArrayList<>();
        Cursor cursor = mContext.getContentResolver().query(NoteContract.NoteEntry.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                notes.add(Note.getNoteFromCursor(cursor));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return notes;
    }

    public Note getNote(long noteId) {
        Note note = null;

        String selection = NoteContract.NoteEntry._ID + "=?";
        String[] selectionArgs = new String[]{
                Long.toString(noteId)
        };


        Cursor cursor = mContext.getContentResolver().query(NoteContract.NoteEntry.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast())
                note = Note.getNoteFromCursor(cursor);
            cursor.close();
        }
        return note;
    }

    //CR(U)D
    public void update(Note note) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_TITLE, note.getTitle());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CONTENT, note.getContent());
//        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CRETIME, note.getDateCreated());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_MODTIME, System.currentTimeMillis());
//        if (!TextUtils.isEmpty(note.getImagePath()))
//            values.put(NoteContract.NoteEntry.COLUMN_NOTE_IMG, note.getImagePath());

        mContext.getApplicationContext().getContentResolver().update(NoteContract.NoteEntry.CONTENT_URI,
                values, NoteContract.NoteEntry.ID + "=" + note.getId(), null);

    }

    public void sync(Note note) {
        ContentValues values = new ContentValues();
//        values.put(NoteContract.NoteEntry.COLUMN_NOTE_TITLE, note.getTitle());
//        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CONTENT, note.getContent());
//        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CRETIME, note.getDateCreated());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_SYNC, NoteContract.NoteEntry.SYNCED);
//        if (!TextUtils.isEmpty(note.getImagePath()))
//            values.put(NoteContract.NoteEntry.COLUMN_NOTE_IMG, note.getImagePath());

        mContext.getApplicationContext().getContentResolver().update(NoteContract.NoteEntry.CONTENT_URI,
                values, NoteContract.NoteEntry.ID + "=" + note.getId(), null);

    }

    public void disable(Note note) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_TITLE, "(DELETED)" + note.getTitle());
//        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CONTENT, note.getContent());
//        values.put(NoteContract.NoteEntry.COLUMN_NOTE_CRETIME, note.getDateCreated());
        values.put(NoteContract.NoteEntry.COLUMN_NOTE_DELETED, NoteContract.NoteEntry.DELETED);
//        if (!TextUtils.isEmpty(note.getImagePath()))
//            values.put(NoteContract.NoteEntry.COLUMN_NOTE_IMG, note.getImagePath());

        mContext.getApplicationContext().getContentResolver().update(NoteContract.NoteEntry.CONTENT_URI,
                values, NoteContract.NoteEntry.ID + "=" + note.getId(), null);

    }

    //CRU(D)
    public void delete(Note note) {
        mContext.getContentResolver().delete(
                NoteContract.NoteEntry.CONTENT_URI, NoteContract.NoteEntry.ID + "=" + note.getId(), null);
    }
}
