package com.codeminders.inotes.auth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import com.codeminders.inotes.R;
import com.codeminders.inotes.model.AccountInfo;

public class ConfigurationActivity extends Activity {
    private static final String SSL_PORT = "993";
    private static final String PORT = "143";
    private AccountInfo accountInfo = new AccountInfo();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_configuration);

        Button button = (Button) findViewById(R.id.config_ok_button);
        final EditText hostEdit = (EditText) findViewById(R.id.imap_server_edit);
        final EditText portEdit = (EditText) findViewById(R.id.imap_port_edit);
        final CheckBox sslCheckbox = (CheckBox) findViewById(R.id.useSSL_checkbox);

        Intent intent = getIntent();
        if (intent != null) {
            accountInfo = intent.getParcelableExtra("account");
            String host = accountInfo.getHost();
            String port = accountInfo.getPort();
            boolean useSSL = accountInfo.isUseSSL();
            if (host != null && port != null) {
                hostEdit.setText(host);
                portEdit.setText(port);
                sslCheckbox.setChecked(useSSL);
            } else {
                portEdit.setText(PORT);
            }
        }

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String host = hostEdit.getText().toString();
                String port = portEdit.getText().toString();
                if (TextUtils.isEmpty(host) || TextUtils.isEmpty(port)) {
                    showDialog(0);
                } else {
                    if (accountInfo != null) {
                        accountInfo.setHost(host);
                        accountInfo.setPort(port);
                        accountInfo.setUseSSL(sslCheckbox.isChecked());
                    }
                    Intent intent = new Intent();
                    intent.putExtra("account", accountInfo);
                    setResult(0, intent);
                    finish();
                }
            }
        });

        sslCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    portEdit.setText(SSL_PORT);
                } else {
                    portEdit.setText(PORT);
                }
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.empty_fields))
                .setNegativeButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).create();
    }

}
