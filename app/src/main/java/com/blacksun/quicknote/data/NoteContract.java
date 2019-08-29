package com.blacksun.quicknote.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

//for the whole database
public class NoteContract {
    static final String CONTENT_AUTHORITY_NOTE = "com.blacksun.quicknote.data.NoteProvider";
    static final String CONTENT_AUTHORITY_ATTACH = "com.blacksun.quicknote.data.AttachProvider";
    static final String PATH_NOTES = "notes";

    static final String PATH_ATTACHES = "attaches";

    private NoteContract(){
    }

    //Entry for each table
    public static final class NoteEntry implements BaseColumns {

        static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY_NOTE);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of notes.
         */
        static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY_NOTE + "/" + PATH_NOTES;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single pet.
         */
        static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY_NOTE + "/" + PATH_NOTES;

        public final static String TABLE_NAME = "notes";
        public final static String ID = BaseColumns._ID;
        public final static String COLUMN_NOTE_TITLE = "title";
        public final static String COLUMN_NOTE_CONTENT = "content";
        public final static String COLUMN_NOTE_MODTIME = "modified_time";
        public final static String COLUMN_NOTE_CRETIME = "created_time";
        //new sync
        public final static String COLUMN_NOTE_SYNC = "sync";
        public final static int NOT_SYNCED = 0;
        public final static int SYNCED = 1;

        //new column to track deleted notes
        public final static String COLUMN_NOTE_DELETED = "deleted";
        public final static int NOT_DELETED = 0;
        public final static int DELETED = 1;

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_NOTES);
    }

    public static final class AttachEntry implements  BaseColumns {
        static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY_ATTACH);

        static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE+"/"+ CONTENT_AUTHORITY_ATTACH +"/" + PATH_ATTACHES;

        static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY_ATTACH + "/" + PATH_ATTACHES;

        public final static String TABLE_NAME = "attaches";
        public final static String ID = BaseColumns._ID;
        public final static String COLUMN_ATTACH_NOTE_ID = "note_id";
        public final static String COLUMN_ATTACH_TYPE = "type";
        public final static String COLUMN_ATTACH_PATH = "path";

        public final static Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_ATTACHES);

        public final static String FILE_TYPE = "file";
        public final static String IMAGE_TYPE = "image";
        public final static String ANY_TYPE = "any";
    }
}