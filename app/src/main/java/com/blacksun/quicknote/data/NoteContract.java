package com.blacksun.quicknote.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

//for the whole database
public class NoteContract {
    public static final String CONTENT_AUTHORITY = "com.blacksun.quicknote";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_NOTES = "notes";

    private NoteContract(){
    }

    //Entry for each table
    public static final class NoteEntry implements BaseColumns {

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of notes.
         */
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTES;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single pet.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_NOTES;

        public final static String TABLE_NAME = "notes";
        public final static String ID = BaseColumns._ID;
        public final static String COLUMN_NOTE_TITLE = "title";
        public final static String COLUMN_NOTE_CONTENT = "content";
        public final static String COLUMN_NOTE_MODTIME = "modified_time";
        public final static String COLUMN_NOTE_CRETIME = "created_time";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_NOTES);
    }
}
