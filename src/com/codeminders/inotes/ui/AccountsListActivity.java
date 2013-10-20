package com.codeminders.inotes.ui;

import android.accounts.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import com.codeminders.inotes.Constants;
import com.codeminders.inotes.R;
import com.codeminders.inotes.auth.AuthenticatorActivity;
import com.codeminders.inotes.db.DBManager;
import com.codeminders.inotes.model.AccountInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountsListActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {
    private static final int AUTH_RESULT = 0;
    private static final int ACTION_DIALOG = 0;
    private Account[] accounts;
    private AccountManager accountManager;
    private String accountName;
    private boolean update;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.accounts_list);
        setTitle(R.string.accounts);
    }

    @Override
    public void onResume() {
        super.onResume();

        loadAccountsList();
        update = true;
        startUpdateThread();
    }

    @Override
    public void onPause() {
        super.onPause();

        update = false;
        synchronized (this) {
            notify();
        }
    }

    public void startUpdateThread() {
        new Thread(new Runnable() {
            public void run() {
                DBManager dbManager = new DBManager(AccountsListActivity.this);
                int count = dbManager.getNotes().size();
                while (update) {
                    if (count != dbManager.getNotes().size()) {
                        handler.sendEmptyMessage(0);
                        count = dbManager.getNotes().size();
                    }
                    try {
                        synchronized (this) {
                            wait(1000);
                        }
                    } catch (InterruptedException e) {
                        Log.d(Constants.TAG, e.toString());
                        update = false;
                    }
                }
            }
        }).start();
    }

    public Handler handler = new Handler(new Handler.Callback() {
        
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 0) {
                loadAccountsList();
            }
            return true;
        }
    });

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.account_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_account:
                Intent intent = new Intent(this, AuthenticatorActivity.class);
                startActivityForResult(intent, AUTH_RESULT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        super.onCreateDialog(id);

        switch (id) {
            case ACTION_DIALOG:
                final CharSequence[] items = {
                        getString(R.string.menu_open),
                        getString(R.string.menu_delete)
                };
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.actions)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                if (item == 0) {
                                    Intent intent = new Intent(AccountsListActivity.this, NotesListActivity.class);
                                    intent.putExtra("accountInfo", getAccountInfo(accountName));
                                    startActivity(intent);
                                } else if (item == 1) {
                                    for (Account account : accounts) {
                                        if (account.name.equals(accountName)) {
                                            final DBManager dbManager = new DBManager(AccountsListActivity.this);
                                            final Handler handler = new Handler(new Handler.Callback() {
                                                @Override
                                                public boolean handleMessage(Message msg) {
                                                    try {
                                                        dbManager.deleteAccount(accountName);
                                                        loadAccountsList();
                                                        dismissDialog(1);
                                                    } catch (Exception e) {
                                                        Log.e(Constants.TAG, e.getMessage());
                                                    }
                                                    return true;
                                                }
                                            });
                                            showDialog(1);
                                            accountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
                                                public void run(AccountManagerFuture<Boolean> future) {
                                                    handler.sendEmptyMessage(0);
                                                }
                                            }, null);
                                            break;
                                        }
                                    }
                                }
                            }
                        }).create();
            default:
                final ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage(getText(R.string.deleting));
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);

                return dialog;
        }
    }

    private void loadAccountsList() {
        SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.accounts_list_item,
                new String[]{"account_list_item_name", "account_notes_count"},
                new int[]{R.id.account_list_item_name, R.id.account_notes_count}
        );

        ListView list = (ListView) findViewById(R.id.accounts_listview);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
        adapter.notifyDataSetChanged();
    }

    private List<? extends Map<String, ?>> getData() {
        DBManager dbManager = new DBManager(this);
        List<Map<String, ?>> items = new ArrayList<Map<String, ?>>();
        Map<String, Object> map = new HashMap<String, Object>();
        accountManager = AccountManager.get(this);
        accounts = accountManager.getAccountsByType(getString(R.string.ACCOUNT_TYPE));

        map.put("account_list_item_name", getString(R.string.all_accounts));
        map.put("account_notes_count", dbManager.getNotesCount());
        items.add(map);
        map = new HashMap<String, Object>();
        map.put("account_list_item_name", getString(R.string.local_account));
        map.put("account_notes_count", dbManager.getNotesCount(Constants.LOCAL_ACCOUNT_NAME));
        items.add(map);
        for (Account account : accounts) {
            map = new HashMap<String, Object>();
            map.put("account_list_item_name", account.name);
            map.put("account_notes_count", dbManager.getNotesCount(account.name));
            items.add(map);
        }

        return items;
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        TextView textView = (TextView) view.findViewById(R.id.account_list_item_name);
        String accountName = textView.getText().toString();
        Intent intent = new Intent(this, NotesListActivity.class);
        if (!accountName.equals(getString(R.string.all_accounts))) {
            intent.putExtra("accountInfo", getAccountInfo(accountName));
        }
        startActivity(intent);

    }

    private AccountInfo getAccountInfo(String name) {
        if (name.equals(getString(R.string.local_account))) {
            AccountInfo accountInfo = new AccountInfo();
            accountInfo.setEmail(Constants.LOCAL_ACCOUNT_NAME);
            return accountInfo;
        }
        for (Account account : accounts) {
            if (account.name.equals(name)) {
                AccountInfo accountInfo = new AccountInfo();
                accountInfo.setEmail(account.name);
                accountInfo.setPassword(accountManager.getPassword(account));
                accountInfo.setHost(accountManager.getUserData(account, "host"));
                accountInfo.setPort(accountManager.getUserData(account, "port"));

                return accountInfo;
            }
        }
        return null;
    }

    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (i > 1) {
            TextView textView = (TextView) view.findViewById(R.id.account_list_item_name);
            accountName = textView.getText().toString();
            showDialog(ACTION_DIALOG);
            return true;
        }
        return false;
    }


}
