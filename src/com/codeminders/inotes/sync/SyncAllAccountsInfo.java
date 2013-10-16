package com.codeminders.inotes.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import com.codeminders.inotes.Constants;

public class SyncAllAccountsInfo implements SyncInfo {
    private Account[] accounts;

    public SyncAllAccountsInfo(Account[] accounts) {
        this.accounts = accounts;
    }

    public boolean isSyncActive() {
        for (Account account: accounts) {
            if (ContentResolver.isSyncActive(account, Constants.AUTHORITIES)) {
                return true;
            }
        }
        return false;
    }

}
