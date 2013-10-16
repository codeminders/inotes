package com.codeminders.inotes.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.codeminders.inotes.Constants;

public class DBCreator extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "inotes.db";
    private static final int DATABASE_VERSION = 2;

    private static final String CREATE_TABLE_NOTES =
            "create table notes"
                    + " (_id integer primary key autoincrement, "
                    + "date integer, "
                    + "title text, "
                    + "note text, "
                    + "account text, "
                    + "headers text, "
                    + "newNote integer);";

    private static final String CREATE_TABLE_SYNC_INFO =
            "create table syncinfo"
                    + " (_id integer primary key autoincrement, "
                    + "account text, "
                    + "date integer);";

    private static final String CREATE_TABLE_NOTES_TO_DELETE =
            "create table clear"
                    + " (_id integer primary key autoincrement, "
                    + "date integer, "
                    + "account text, "
                    + "identifier text);";

    public DBCreator(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTES);
        db.execSQL(CREATE_TABLE_SYNC_INFO);
        db.execSQL(CREATE_TABLE_NOTES_TO_DELETE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DBUpgradeHelper upgradeHelper = new DBUpgradeHelper(db);
        try {
            upgradeHelper.exportNotesFromDB();
        } catch (Exception e) {
            Log.e(Constants.LOCAL_ACCOUNT_NAME, e.getMessage());
        }
        db.execSQL("drop table if exists notes");
        db.execSQL("drop table if exists syncinfo");
        db.execSQL("drop table if exists clear");
        onCreate(db);

        upgradeHelper.importNotesToDB();
    }

}