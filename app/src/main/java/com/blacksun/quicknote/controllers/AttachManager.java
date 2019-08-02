package com.blacksun.quicknote.controllers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.blacksun.quicknote.data.NoteContract;
import com.blacksun.quicknote.models.Attachment;

import java.util.ArrayList;

public class AttachManager {
    private Context mContext;
    private static AttachManager sNoteManagerInstance = null;

    public static AttachManager newInstance(Context context) {

        if (sNoteManagerInstance == null) {
            sNoteManagerInstance = new AttachManager(context.getApplicationContext());
        }

        return sNoteManagerInstance;
    }

    private AttachManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public long create(Attachment attach) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID, attach.getNote_id());
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE, attach.getType());
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_PATH, attach.getPath());

        Uri result = mContext.getContentResolver().insert(NoteContract.AttachEntry.CONTENT_URI, values);
        long id = Long.parseLong(result.getLastPathSegment());
        Log.i("Log Cursor", " create attach name  " + id + " ");
        return id;
    }


    public long add(Attachment attach) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID, attach.getNote_id());
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE, attach.getType());
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_PATH, attach.getPath());
        values.put(NoteContract.AttachEntry.ID, attach.getId());

        Uri result = mContext.getContentResolver().insert(NoteContract.AttachEntry.CONTENT_URI, values);
        long id = Long.parseLong(result.getLastPathSegment());
        Log.i("Log Cursor", " Add existing attach name  " + id + " ");
        return id;
    }

    public void update(Attachment attach) {
        ContentValues values = new ContentValues();
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID, attach.getNote_id());
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_TYPE, attach.getType());
        values.put(NoteContract.AttachEntry.COLUMN_ATTACH_PATH, attach.getPath());

        mContext.getApplicationContext().getContentResolver().update(NoteContract.AttachEntry.CONTENT_URI,
                values, NoteContract.AttachEntry.ID + "=" + attach.getId(), null);

    }

    public void delete(Attachment attach) {
        mContext.getContentResolver().delete(
                NoteContract.AttachEntry.CONTENT_URI, NoteContract.AttachEntry.ID + "=" + attach.getId(), null);
    }

    public ArrayList<Attachment> getAttach(long noteId, String type) {
        ArrayList<Attachment> attaches = new ArrayList<Attachment>();

        String selection;
        String[] selectionArgs;
        if (type.equals(NoteContract.AttachEntry.ANY_TYPE)) {
            selection = NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID + "=?";
            selectionArgs = new String[]{
                    Long.toString(noteId)
            };

        } else {
            //original
            selection = NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID + "=? AND " + NoteContract.AttachEntry.COLUMN_ATTACH_TYPE + "=?";
            selectionArgs = new String[]{
                    Long.toString(noteId),
                    type
            };
        }


        Cursor cursor = mContext.getContentResolver().query(NoteContract.AttachEntry.CONTENT_URI, null, selection, selectionArgs, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                attaches.add(Attachment.getAttachFromCursor(cursor));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return attaches;
    }

//    public boolean isExist(long attachId) {
////        ArrayList<Attachment> attaches = new ArrayList<Attachment>();
//
//        String selection = NoteContract.AttachEntry.ID + "=?";
//        String[] selectionArgs = new String[]{
//                Long.toString(attachId)
//        };
//
//        Cursor cursor = mContext.getContentResolver().query(NoteContract.AttachEntry.CONTENT_URI, null, selection, selectionArgs, null);
//        if (cursor.getCount() > 0) {
//            return true;
//        } else
//            return false;
//    }

    public ArrayList<Attachment> getAllAttaches() {
        ArrayList<Attachment> attaches = new ArrayList<>();
        Cursor cursor = mContext.getContentResolver().query(NoteContract.AttachEntry.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                attaches.add(Attachment.getAttachFromCursor(cursor));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return attaches;
    }
}
