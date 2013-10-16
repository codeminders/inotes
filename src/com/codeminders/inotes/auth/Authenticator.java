package com.codeminders.inotes.auth;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.codeminders.inotes.Constants;
import com.codeminders.inotes.imap.ImapSession;
import com.codeminders.inotes.model.AccountInfo;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import java.io.IOException;

public class Authenticator extends AbstractAccountAuthenticator {
    private final Context a_context;

    public Authenticator(Context context) {
        super(context);
        a_context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType,
                             String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Bundle reply = new Bundle();

        Intent i = new Intent(a_context, AuthenticatorActivity.class);
        i.setAction(Constants.INTENT_LOGIN);
        i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        reply.putParcelable(AccountManager.KEY_INTENT, i);

        return reply;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
        if (options != null && options.containsKey(AccountManager.KEY_PASSWORD)) {
            final String password = options.getString(AccountManager.KEY_PASSWORD);
            final String host = options.getString("host");
            final String port = options.getString("port");
            final boolean useSSL = options.getBoolean("useSSL");
            boolean verified;
            try {
                AccountInfo accountInfo = new AccountInfo();
                accountInfo.setEmail(account.name);
                accountInfo.setPassword(password);
                accountInfo.setHost(host);
                accountInfo.setPort(port);
                accountInfo.setUseSSL(useSSL);
                verified = onlineConfirmPassword(accountInfo) != null;
            } catch (IOException e) {
                Log.e(Constants.TAG, "Verification error", e);
                verified = false;
            }
            final Bundle result = new Bundle();
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, verified);
            return result;
        }
        // Launch AuthenticatorActivity to confirm credentials
        final Intent intent = new Intent(a_context, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AuthenticatorActivity.PARAM_CONFIRM_CREDENTIALS, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType,
                               Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features)
            throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType,
                                    Bundle options) {
        final Intent intent = new Intent(a_context, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AuthenticatorActivity.PARAM_CONFIRM_CREDENTIALS, false);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    public static String onlineConfirmPassword(AccountInfo accountInfo) throws IOException {
        String username = accountInfo.getEmail();
        String password = accountInfo.getPassword();
        String host = accountInfo.getHost();
        try {
            ImapSession imapSession = new ImapSession(host, accountInfo.getPort(), accountInfo.isUseSSL());

            Session session = imapSession.getSession();
            Store store = session.getStore(imapSession.getProtocol());
            store.connect(host, username, password);

            Folder notesFolder = store.getFolder("Notes");
            notesFolder.open(Folder.READ_WRITE);
        } catch (Exception e) {
            throw new IOException("Unable to access to notes folder");
        }
        return username;
    }
}