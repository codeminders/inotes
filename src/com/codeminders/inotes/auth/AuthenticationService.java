package com.codeminders.inotes.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticationService extends Service {

    private static Authenticator accountAuthenticator = null;

    public AuthenticationService() {
        super();
    }

    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = getAuthenticator().getIBinder();
        }

        return ret;
    }

    private Authenticator getAuthenticator() {
        if (accountAuthenticator == null) {
            accountAuthenticator = new Authenticator(this);
        }

        return accountAuthenticator;
    }

}
