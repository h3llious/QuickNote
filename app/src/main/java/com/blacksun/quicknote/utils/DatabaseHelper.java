package com.blacksun.quicknote.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.blacksun.quicknote.data.NoteContract;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "quick_note.db";
    private static final int DATABASE_VERSION = 5;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTE);
        db.execSQL(CREATE_TABLE_ATTACHMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + NoteContract.NoteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + NoteContract.AttachEntry.TABLE_NAME);
        onCreate(db);
    }

    private static final String CREATE_TABLE_NOTE = "CREATE TABLE "
            + NoteContract.NoteEntry.TABLE_NAME
            + "("
            + NoteContract.NoteEntry.ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + NoteContract.NoteEntry.COLUMN_NOTE_TITLE + " text NOT NULL, "
            + NoteContract.NoteEntry.COLUMN_NOTE_CONTENT + " text, "
            + NoteContract.NoteEntry.COLUMN_NOTE_MODTIME + " integer NOT NULL, "
            + NoteContract.NoteEntry.COLUMN_NOTE_CRETIME + " integer NOT NULL, "
            + NoteContract.NoteEntry.COLUMN_NOTE_SYNC + " integer NOT NULL"
//            +","+ NoteContract.NoteEntry.COLUMN_NOTE_IMG + " text" + ")";
            + ")";
    private static final String CREATE_TABLE_ATTACHMENT = "CREATE TABLE "
            + NoteContract.AttachEntry.TABLE_NAME
            + "("
            + NoteContract.AttachEntry.ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + NoteContract.AttachEntry.COLUMN_ATTACH_NOTE_ID + " integer NOT NULL, "
            + NoteContract.AttachEntry.COLUMN_ATTACH_TYPE + " text NOT NULL, "
            + NoteContract.AttachEntry.COLUMN_ATTACH_PATH + " text NOT NULL )";
}
