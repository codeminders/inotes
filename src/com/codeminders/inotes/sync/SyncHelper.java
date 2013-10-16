package com.codeminders.inotes.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import com.codeminders.inotes.R;

public class SyncHelper {
    SyncInfo syncInfo;

    public SyncHelper(Context context, String name) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.ACCOUNT_TYPE));
        if (name == null) {
            syncInfo = new SyncAllAccountsInfo(accounts);
        } else {
            for (Account account: accounts) {
                if (account.name.equals(name)) {
                    syncInfo = new SyncAccountInfo(account);
                }
            }
        }
    }

    public boolean isSyncActive() {
        return syncInfo != null && syncInfo.isSyncActive();
    }

}

