package com.blacksun.quicknote.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "quick_note.db";
    private static final int DATABASE_VERSION = 1;



    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + NoteContract.NoteEntry.TABLE_NAME);
        onCreate(db);
    }

    private static final String CREATE_TABLE_NOTE = "CREATE TABLE "
            + NoteContract.NoteEntry.TABLE_NAME
            + "("
            + NoteContract.NoteEntry.ID + " integer PRIMARY KEY AUTOINCREMENT, "
            + NoteContract.NoteEntry.COLUMN_NOTE_TITLE + " text NOT NULL, "
            + NoteContract.NoteEntry.COLUMN_NOTE_CONTENT + " text, "
            + NoteContract.NoteEntry.COLUMN_NOTE_MODTIME + " integer NOT NULL, "
            + NoteContract.NoteEntry.COLUMN_NOTE_CRETIME + " integer NOT NULL" + ")";
}
