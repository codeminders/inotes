package com.codeminders.inotes.sync;

import java.util.*;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import com.codeminders.inotes.Constants;
import com.codeminders.inotes.db.DBManager;
import com.codeminders.inotes.imap.ImapService;
import com.codeminders.inotes.model.AccountInfo;
import com.codeminders.inotes.model.Note;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final AccountManager accountManager;
    private final Context context;
    private final int[] times = new int[]{
            300, 900, 1800, 3600, 7200, 14400, 28800, 43200, 86440
    };

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        this.context = context;
        accountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        addPeriodicallySync(prefs, account, authority);

        if (prefs.getBoolean(Constants.SYNC_TYPE, false)) {
            NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (info == null || info.getType() != 1) {
                return;
            }
        }

        try {
            DBManager dbManager = new DBManager(context);
            long time = dbManager.getLastSyncTime(account.name);
            long currentTime = new Date().getTime();
            ImapService imapService = new ImapService(getAccountInfo(account), context);
            List<Note> newNotes;
            if (time == 0) {
                newNotes = imapService.getNotes();
                imapService.addNotes(dbManager.getNotesByDate(account.name, time, currentTime));
            } else {
                imapService.deleteNotes(dbManager.getNotesIdentifiersToDelete(account.name, currentTime));
                newNotes = getNewNotes(imapService.getNotes(), time, currentTime);
                imapService.addNotes(dbManager.getNotesByDate(account.name, time, currentTime));
            }
            writeNewNotes(dbManager, newNotes);
            imapService.checkForServersDeletedNotes(context, account.name);
            dbManager.setLastSyncTime(account.name, new Date(currentTime));
        } catch (Exception e) {
            Log.e(Constants.TAG, e.toString());
        }

    }

    private void writeNewNotes(DBManager dbManager, List<Note> notes) {
        for (Note note : notes) {
            dbManager.writeNote(note);
        }
    }

    private List<Note> getNewNotes(List<Note> notes, long lastSync, long currentSync) {
        List<Note> newNotes = new ArrayList<Note>();
        for (Note note : notes) {
            if (currentSync > note.getDate().getTime() && note.getDate().getTime() > lastSync) {
                newNotes.add(note);
            }
        }
        return newNotes;
    }

    private AccountInfo getAccountInfo(Account account) {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setEmail(account.name);
        accountInfo.setHost(accountManager.getUserData(account, "host"));
        accountInfo.setPassword(accountManager.getPassword(account));
        accountInfo.setPort(accountManager.getUserData(account, "port"));
        accountInfo.setUseSSL(accountManager.getUserData(account, "useSSL").equals("true"));

        return accountInfo;
    }

    private void addPeriodicallySync(SharedPreferences prefs, Account account, String authority) {
        int itemPosition = prefs.getInt(Constants.SYNC_INTERVAL, 4);
        ContentResolver.addPeriodicSync(account, authority, new Bundle(), times[itemPosition]);
    }


}
