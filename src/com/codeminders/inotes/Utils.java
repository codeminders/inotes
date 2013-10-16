package com.codeminders.inotes;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import com.codeminders.inotes.model.Note;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

    public static String[] getAccounts(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.ACCOUNT_TYPE));
        String[] accountsNames = new String[accounts.length + 1];
        accountsNames[0] = context.getString(R.string.local_account);
        for (int i = 1; i < accountsNames.length; i++) {
            accountsNames[i] = accounts[i - 1].name;
        }
        return accountsNames;
    }

    public static void share(Context context, String text) {
        text = Html.fromHtml(text).toString().concat("\n\n\n" + context.getString(R.string.share_signature));
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share) + ":"));
    }

    public static String getIdentifier(Note note) throws NoSuchAlgorithmException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(note.getDate().getTime())
                .append(note.getTitle())
                .append(note.getNote())
                .append(note.getAccount());
        String plaintext = stringBuilder.toString();
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.reset();
        m.update(plaintext.getBytes());
        byte[] digest = m.digest();
        BigInteger bigInt = new BigInteger(1, digest);
        String hash = bigInt.toString(16);
        while (hash.length() < 32) {
            hash = "0" + hash;
        }
        return hash;
    }
}
