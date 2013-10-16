package com.codeminders.inotes.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import com.codeminders.inotes.Constants;

public class SyncAccountInfo implements SyncInfo {
    private Account account;

    public SyncAccountInfo(Account account) {
        this.account = account;
    }

    public boolean isSyncActive() {
        return ContentResolver.isSyncActive(account, Constants.AUTHORITIES);
    }

}
