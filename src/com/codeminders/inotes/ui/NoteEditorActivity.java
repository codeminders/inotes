package com.codeminders.inotes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.codeminders.inotes.Constants;
import com.codeminders.inotes.R;
import com.codeminders.inotes.Utils;
import com.codeminders.inotes.db.DBManager;
import com.codeminders.inotes.imap.HeaderUtils;
import com.codeminders.inotes.model.Note;

import java.text.DateFormat;
import java.util.Date;

public class NoteEditorActivity extends Activity {
    private static final int MAX_TITLE_SIZE = 20;
    private static final int EMPTY_NOTE_DIALOG = 0;
    private static final int CHOOSE_ACCOUNT_DIALOG = 1;
    private static final int MOVE_ACCOUNT_DIALOG = 2;
    private static final int NOTHING_TO_SHARE = 3;
    private Note note;
    private String accountName;
    private boolean save = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.note_editor);
        Intent intent = getIntent();
        if (Constants.NOTE_EDIT_ACTION.equals(intent.getAction())) {
            note = intent.getParcelableExtra("note");
            EditText noteEdit = (EditText) findViewById(R.id.note);

            noteEdit.setText(Html.fromHtml(note.getNote()), TextView.BufferType.SPANNABLE);

            String title = String.format(getResources().getString(R.string.title_edit), note.getTitle());
            setTitle(title);

            TextView noteTime = (TextView) findViewById(R.id.time);
            noteTime.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(note.getDate()));
        } else if (Constants.NOTE_CREATE_ACTION.equals(intent.getAction())) {
            accountName = intent.getStringExtra("account");
            if (accountName == null) {
                if (Utils.getAccounts(this).length == 1) {
                    accountName = Constants.LOCAL_ACCOUNT_NAME;
                } else {
                    showDialog(CHOOSE_ACCOUNT_DIALOG);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (save && saveNote()) {
            try{
                note.addHeader(HeaderUtils.INOTES_ID_HEADER, Utils.getIdentifier(note));
            } catch (Exception ignored) {
            }
            writeNoteToDB();
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.note_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (note != null) {
            menu.setGroupVisible(R.id.menu_group_edit, true);
        } else {
            menu.setGroupVisible(R.id.menu_group_edit, false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_discard:
                save = false;
                finish();
                break;
            case R.id.menu_delete:
                if (note == null) {
                    finish();
                } else {
                    DBManager dbManager = new DBManager(this);
                    dbManager.deleteNote(note.getId());
                    finish();
                }
                break;
            case R.id.menu_move:
                showDialog(MOVE_ACCOUNT_DIALOG);
                break;
            case R.id.menu_share:
                String noteBody = note.getNote();
                if (noteBody != null) {
                    Utils.share(this, noteBody);
                } else {
                    showDialog(NOTHING_TO_SHARE);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        super.onCreateDialog(id);

        switch (id) {
            case EMPTY_NOTE_DIALOG:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.nothing_to_save)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                NoteEditorActivity.this.finish();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).create();
            case CHOOSE_ACCOUNT_DIALOG:
                final String[] accounts = Utils.getAccounts(this);
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.save_to)
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                save = false;
                                finish();
                            }
                        })
                        .setItems(accounts, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                accountName = accounts[item].equals(getString(R.string.local_account)) ? Constants.LOCAL_ACCOUNT_NAME : accounts[item];
                            }
                        }).create();
            case MOVE_ACCOUNT_DIALOG:
                final String[] moveAccounts = Utils.getAccounts(this);
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.save_to)
                        .setItems(moveAccounts, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                accountName = moveAccounts[item].equals(getString(R.string.local_account)) ? Constants.LOCAL_ACCOUNT_NAME : moveAccounts[item];
                                note.setAccount(accountName);
                                if (accountName != null && !accountName.equals(Constants.LOCAL_ACCOUNT_NAME)) {
                                    note.setNewNote(true);
                                }
                                note.setDate(new Date());
                                writeNoteToDB();
                                finish();
                            }
                        }).create();
                        case NOTHING_TO_SHARE:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.nothing_to_share)
                        .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).create();
        }
        return null;
    }

    private void writeNoteToDB() {
        DBManager dbManager = new DBManager(NoteEditorActivity.this);
        note.setDate(new Date());
        dbManager.writeNote(note);
        NoteEditorActivity.this.finish();
    }

    private boolean saveNote() {
        if (note == null) {
            note = new Note();
            note.setAccount(accountName);
            HeaderUtils.addDefaultHeader(note);
        }

        EditText editText = (EditText) findViewById(R.id.note);
        String text = editText.getText().toString();
        if (isNoteEmpty(text) || (note.getNote() != null && Html.fromHtml(note.getNote()).toString().equals(text))) {
            return false;
        }
        String title = getNoteTitle(text);
        note.setDate(new Date());
        note.setTitle(title);
        note.setNote(Html.toHtml(editText.getText()));

        return true;
    }

    private boolean isNoteEmpty(String text) {
        return text.replaceAll("\\s", "").length() == 0;
    }

    private String getNoteTitle(String text) {
        String title = note.getTitle();
        if (title != null) {
            return title;
        }
        if (text.length() > MAX_TITLE_SIZE) {
            text = text.substring(0, MAX_TITLE_SIZE);
        }

        return text.split("\n")[0];
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
