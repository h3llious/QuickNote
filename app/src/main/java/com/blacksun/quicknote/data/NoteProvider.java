package com.blacksun.quicknote.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.blacksun.quicknote.utils.DatabaseHelper;

public class NoteProvider extends ContentProvider {
    public static final String LOG_TAG = NoteProvider.class.getSimpleName();
    private DatabaseHelper noteHelper;

    private static final int NOTE = 100;
    private static final int NOTES = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(NoteContract.CONTENT_AUTHORITY_NOTE, NoteContract.PATH_NOTES, NOTES);
        sUriMatcher.addURI(NoteContract.CONTENT_AUTHORITY_NOTE, NoteContract.PATH_NOTES + "/#", NOTE);
    }

    @Override
    public boolean onCreate() {
        noteHelper = new DatabaseHelper(getContext());
        return false;
    }

    @androidx.annotation.Nullable
    @Override
    public Cursor query(@androidx.annotation.NonNull Uri uri, @androidx.annotation.Nullable String[] projection, @androidx.annotation.Nullable String selection, @androidx.annotation.Nullable String[] selectionArgs, @androidx.annotation.Nullable String sortOrder) {
        SQLiteDatabase database = noteHelper.getReadableDatabase();
        Cursor cursor;

        //distinguish uri type
        int match = sUriMatcher.match(uri);
        switch (match) {
            case NOTES:
                cursor = database.query(NoteContract.NoteEntry.TABLE_NAME, projection
                        , null, null, null, null, sortOrder);
                break;
            case NOTE:
                selection = NoteContract.NoteEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = database.query(NoteContract.NoteEntry.TABLE_NAME, projection, selection, selectionArgs
                        , null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        return cursor;
    }

    @androidx.annotation.Nullable
    @Override
    public String getType(@androidx.annotation.NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case NOTES:
                return NoteContract.NoteEntry.CONTENT_LIST_TYPE;
            case NOTE:
                return NoteContract.NoteEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @androidx.annotation.Nullable
    @Override
    public Uri insert(@androidx.annotation.NonNull Uri uri, @androidx.annotation.Nullable ContentValues values) {
        if (values != null) {
            String title = values.getAsString(NoteContract.NoteEntry.COLUMN_NOTE_TITLE);
            if (title == null) {
                throw new IllegalArgumentException("Note requires a title");
            }

            final int match = sUriMatcher.match(uri);

            if (match == NOTES)
                return insertNote(uri, values);
            else
                throw new IllegalArgumentException("Insertion is not supported for " + uri);

        } else
            throw new IllegalArgumentException("ContentValues must not be null");
    }

    private Uri insertNote(Uri uri, ContentValues values) {
        SQLiteDatabase db = noteHelper.getWritableDatabase();
        long id = db.insert(NoteContract.NoteEntry.TABLE_NAME, null, values);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@androidx.annotation.NonNull Uri uri, @androidx.annotation.Nullable String selection, @androidx.annotation.Nullable String[] selectionArgs) {
        SQLiteDatabase database = noteHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case NOTES:
                return database.delete(NoteContract.NoteEntry.TABLE_NAME, selection, selectionArgs);
            case NOTE:
                selection = NoteContract.NoteEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return database.delete(NoteContract.NoteEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    @Override
    public int update(@androidx.annotation.NonNull Uri uri, @androidx.annotation.Nullable ContentValues values, @androidx.annotation.Nullable String selection, @androidx.annotation.Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        if (values != null)
            switch (match) {
                case NOTES:
                    return updateNote(uri, values, selection, selectionArgs);
                case NOTE:
                    selection = NoteContract.NoteEntry._ID + "=?";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                    return updateNote(uri, values, selection, selectionArgs);
                default:
                    throw new IllegalArgumentException("Update is not supported for " + uri);
            }
        else
            throw new IllegalArgumentException("ContentValues must not be null");
    }

    private int updateNote(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase database = noteHelper.getWritableDatabase();

        if (values.containsKey(NoteContract.NoteEntry.COLUMN_NOTE_TITLE)) {
            if (values.getAsString(NoteContract.NoteEntry.COLUMN_NOTE_TITLE) == null) {
                throw new IllegalArgumentException("Note needs a title");
            }
        }

        if (values.size() == 0)
            return 0;

        return database.update(NoteContract.NoteEntry.TABLE_NAME, values, selection, selectionArgs);
    }
}
