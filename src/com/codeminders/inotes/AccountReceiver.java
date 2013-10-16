package com.codeminders.inotes;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.codeminders.inotes.db.DBManager;

import java.util.ArrayList;
import java.util.List;

public class AccountReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        List<String> accounts = getAccounts(context);

        DBManager dbManager = new DBManager(context);
        List<String> notesAccounts = dbManager.getAccounts();

        for (String account: notesAccounts) {
            if (!accounts.contains(account)) {
                dbManager.deleteAccount(account);
            }
        }
    }

    private List<String> getAccounts(Context context) {
        List<String> accountsResult = new ArrayList<String>();
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(context.getString(R.string.ACCOUNT_TYPE));
        for (Account account: accounts) {
            accountsResult.add(account.name);
        }

        return accountsResult;
    }

}
