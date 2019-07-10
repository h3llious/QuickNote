package com.blacksun.quicknote.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AttachProvider extends ContentProvider {
    public static final String LOG_TAG = AttachProvider.class.getSimpleName();
    private DatabaseHelper noteHelper;

    private static final int ATTACH = 200;
    private static final int ATTACHES = 201;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(NoteContract.CONTENT_AUTHORITY_ATTACH, NoteContract.PATH_ATTACHES, ATTACHES);
        sUriMatcher.addURI(NoteContract.CONTENT_AUTHORITY_ATTACH, NoteContract.PATH_ATTACHES + "/#", ATTACH);
    }

    @Override
    public boolean onCreate() {
        noteHelper = new DatabaseHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = noteHelper.getReadableDatabase();
        Cursor cursor = null;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case ATTACHES:
                cursor = database.query(NoteContract.AttachEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case ATTACH:
                selection = NoteContract.AttachEntry.ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(NoteContract.AttachEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ATTACHES:
                return NoteContract.AttachEntry.CONTENT_LIST_TYPE;
            case ATTACH:
                return NoteContract.AttachEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Long noteId = values.getAsLong(NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID);
        if (noteId == null)
            throw new IllegalArgumentException("Attachment must have its note id");
        final int match = sUriMatcher.match(uri);

        if (match == ATTACHES)
            return insertAttach(uri, values);
        else
            throw new IllegalArgumentException("Insertion is not supported for " + uri);
    }

    private Uri insertAttach(Uri uri, ContentValues values) {
        SQLiteDatabase db = noteHelper.getWritableDatabase();
        long id = db.insert(NoteContract.AttachEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = noteHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ATTACHES:
                return database.delete(NoteContract.AttachEntry.TABLE_NAME, selection, selectionArgs);
            case ATTACH:
                selection = NoteContract.AttachEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return database.delete(NoteContract.AttachEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ATTACHES:
                return updateAttach(uri, values, selection, selectionArgs);
            case ATTACH:
                selection = NoteContract.AttachEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateAttach(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateAttach(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = noteHelper.getWritableDatabase();

        if (values.containsKey(NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID)) {
            if (values.getAsLong(NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID) == null) {
                throw new IllegalArgumentException("Attachment must have its note id");
            }
        }

        if (values.size() == 0)
            return 0;

        return database.update(NoteContract.AttachEntry.TABLE_NAME, values, selection, selectionArgs);
    }
}
