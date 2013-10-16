package com.codeminders.inotes.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.codeminders.inotes.Constants;
import com.codeminders.inotes.Utils;
import com.codeminders.inotes.imap.HeaderUtils;
import com.codeminders.inotes.model.Note;
import org.json.JSONObject;

import java.util.*;

public class DBUpgradeHelper {
    List<Note> notes = new ArrayList<Note>();
    Map<String, Long> syncInfo = new HashMap<String, Long>();
    SQLiteDatabase db;

    DBUpgradeHelper(SQLiteDatabase db) {
        this.db = db;
    }

    public void exportNotesFromDB() {
        syncInfo.put(Constants.LOCAL_ACCOUNT_NAME, 0L);
        try {
            Cursor cursor = db.rawQuery("select * from syncinfo", null);
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    syncInfo.put(cursor.getString(1), cursor.getLong(2));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            for (Map.Entry<String, Long> entry : syncInfo.entrySet()) {
                getOldNotes(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        }
    }

    private void getOldNotes(String account, long time) {
        try {
            Cursor cursor = db.rawQuery("select * from notes where account = ? and date > ?", new String[]{account, Long.toString(time)});
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    notes.add(getNewNote(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        }
    }

    private Note getNewNote(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getInt(0));
        note.setDate(new Date(cursor.getLong(1)));
        note.setTitle(cursor.getString(2));
        note.setNote(cursor.getString(3));
        note.setAccount(cursor.getString(4));
        note.setHeaders(HeaderUtils.getHeaders(cursor.getString(5)));
        try {
            note.addHeader(HeaderUtils.INOTES_ID_HEADER, Utils.getIdentifier(note));
        } catch (Exception ignored) {
        }

        return note;
    }

    public void importNotesToDB() {
        ContentValues cv;
        for (Note note : notes) {
            cv = new ContentValues();
            cv.put("date", note.getDate().getTime());
            cv.put("title", note.getTitle());
            cv.put("note", note.getNote());
            cv.put("account", note.getAccount());
            Map<String, String> map = note.getHeaders();
            JSONObject jsonObject = new JSONObject(map);
            cv.put("headers", jsonObject.toString());
            cv.put("newNote", 1);

            try {
                db.insert("notes", null, cv);
            } catch (Exception e) {
                Log.e(Constants.TAG, e.toString());
            }
        }
    }

}
