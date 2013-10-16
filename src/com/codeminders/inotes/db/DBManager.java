package com.codeminders.inotes.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.codeminders.inotes.Constants;
import com.codeminders.inotes.imap.HeaderUtils;
import com.codeminders.inotes.model.Note;
import org.json.JSONObject;

import java.util.*;

public class DBManager {
    private DBCreator dbCreator;

    public DBManager(Context context) {
        dbCreator = new DBCreator(context);
    }

    public void writeNote(Note note) {
        Note noteWithSameId = null;
        int id = note.getId();
        if (id != -1) {
            noteWithSameId = getNote(id);
        }
        ContentValues cv = new ContentValues();
        cv.put("date", note.getDate().getTime());
        cv.put("title", note.getTitle());
        cv.put("note", note.getNote());
        cv.put("account", note.getAccount());
        Map<String, String> map = note.getHeaders();
        JSONObject jsonObject = new JSONObject(map);
        cv.put("headers", jsonObject.toString());
        cv.put("newNote", 1); 

        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            if (noteWithSameId == null) {
                db.insert("notes", null, cv);
            } else {
                db.update("notes", cv, "_id = ?", new String[]{Integer.toString(id)});
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            if (db != null) {
                db.close();
            }
        }
        if (noteWithSameId != null && !noteWithSameId.getAccount().equals(Constants.LOCAL_ACCOUNT_NAME)) {
            addToCleanupQueue(noteWithSameId, note.getDate().getTime());
        }
    }

    private void addToCleanupQueue(Note note, long time) {
        String id = note.getHeader(HeaderUtils.INOTES_ID_HEADER);
        if (isInCleanQueue(id)) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("date", time);
        cv.put("account", note.getAccount());
        cv.put("identifier", note.getHeader(HeaderUtils.INOTES_ID_HEADER));

        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            db.insert("clear", null, cv);
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
    }

    private void removeFromCleanupQueue(String identifier) {
        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            db.delete("clear", "identifier = ?", new String[]{identifier});
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
    }

    private boolean isInCleanQueue(String id) {
        boolean exist = false;
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select _id from clear where identifier = ?", new String[]{id});
            try {
                if (cursor.getCount() > 0) {
                    exist = true;
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return exist;
    }

    public void deleteNote(int id) {
        Note note = getNote(id);
        if (Constants.LOCAL_ACCOUNT_NAME.equals(note.getAccount())) {
            addToCleanupQueue(note, new Date().getTime());
        }

        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            db.delete("notes", "_id = " + id, null);
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
            removeFromCleanupQueue(note.getHeader(HeaderUtils.INOTES_ID_HEADER));
        } finally {
            db.close();
        }
    }

    public void deleteNotes(String account) {
        if (!Constants.LOCAL_ACCOUNT_NAME.equals(account)) {
            List<Note> notes = getNotes(account);
            for (Note note : notes) {
                addToCleanupQueue(note, new Date().getTime());
            }
        }
        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            db.delete("notes", "account = ?", new String[]{account});
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
    }

    public void deleteNotes() {
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select account from notes", null);
            try {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    deleteNotes(cursor.getString(0));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        } finally {
            db.close();
        }
    }

    private Note getNote(int id) {
        Note note = null;
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from notes where _id = " + id, null);
            try {
                if (cursor.moveToFirst()) {
                    note = getNote(cursor);
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return note;
    }

    public List<Note> getNotes() {
        List<Note> notes = new ArrayList<Note>();
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from notes order by date desc", null);
            try {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    notes.add(getNote(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return notes;
    }

    public List<Note> getNotes(String account) {
        List<Note> notes = new ArrayList<Note>();
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from notes where account = ? order by date desc", new String[]{account});
            try {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    notes.add(getNote(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return notes;
    }

    public List<Note> getNotesByDate(String account, long lastSync, long currentSync) {
        List<Note> notes = new ArrayList<Note>();
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select * from notes where account = ? and date > ? and date < ? order by date desc",
                    new String[]{account, Long.toString(lastSync), Long.toString(currentSync)});
            try {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    notes.add(getNote(cursor));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return notes;
    }

    private Note getNote(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getInt(0));
        note.setDate(new Date(cursor.getLong(1)));
        note.setTitle(cursor.getString(2));
        note.setNote(cursor.getString(3));
        note.setAccount(cursor.getString(4));
        note.setHeaders(HeaderUtils.getHeaders(cursor.getString(5)));
        note.setNewNote(cursor.getInt(6) == 1);

        return note;
    }

    public void setLastSyncTime(String account, Date date) {
        boolean accountExist = isSyncAccountExist(account);

        ContentValues syncInfoValues = new ContentValues();
        syncInfoValues.put("account", account);
        syncInfoValues.put("date", date.getTime());

        ContentValues notesValues = new ContentValues();
        notesValues.put("newNote", 0);

        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            if (accountExist) {
                db.update("syncinfo", syncInfoValues, "account = ?", new String[]{account});
            } else {
                db.insert("syncinfo", null, syncInfoValues);
            }
            db.update("notes", notesValues, "newNote = ? and date < ?", new String[]{"1", Long.toString(date.getTime())});
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
    }

    public long getLastSyncTime(String account) {
        long time = 0;
        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            Cursor cursor = db.rawQuery("select date from syncinfo where account = ?", new String[]{account});
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    time = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return time;
    }

    public List<String> getAccounts() {
        List<String> accounts = new ArrayList<String>();
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select distinct account from syncinfo", null);
            try {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    accounts.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return accounts;
    }

    public void deleteAccount(String account) {
        deleteNotes(account);
        deleteCleanQueue(account);
        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            db.delete("syncinfo", "account = ?", new String[]{account});
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
    }

    private boolean isSyncAccountExist(String account) {
        boolean exist = false;
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select _id from syncinfo where account = ?", new String[]{account});
            try {
                if (cursor.getCount() > 0) {
                    exist = true;
                }
            } finally {
                cursor.close();
            }
        } finally {
            db.close();
        }
        return exist;
    }

    public List<String> getNotesIdentifiersToDelete(String account, long date) {
        List<String> identifiers = new ArrayList<String>();
        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            Cursor cursor = db.rawQuery("select identifier from clear where account = ? and date < ?", new String[]{account, Long.toString(date)});
            try {
                cursor.moveToFirst();

                while (!cursor.isAfterLast()) {
                    identifiers.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            } finally {
                cursor.close();
            }
            db.delete("clear", "account = ? and date < ?", new String[]{account, Long.toString(date)});
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return identifiers;
    }

    public void deleteCleanQueue(String account) {
        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            db.delete("clear", "account = ?", new String[]{account});
        } finally {
            db.close();
        }
    }

    public int getNotesCount() {
        int count = 0;
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select _id from notes", null);
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return count;
    }

    public int getNotesCount(String account) {
        int count = 0;
        SQLiteDatabase db = dbCreator.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("select _id from notes where account = ?", new String[]{account});
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
        return count;
    }

    public void setOldNote(Note note) {
        ContentValues cv = new ContentValues();
        cv.put("newNote", 0);
        SQLiteDatabase db = dbCreator.getWritableDatabase();
        try {
            db.update("notes", cv, "_id = ?", new String[]{Integer.toString(note.getId())});
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        } finally {
            db.close();
        }
    }

}
