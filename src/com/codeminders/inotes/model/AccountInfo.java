package com.codeminders.inotes.model;

import android.os.Parcel;
import android.os.Parcelable;

public class AccountInfo implements Parcelable {
    private String host;
    private String port;
    private String email;
    private String password;
    private boolean useSSL;

    public AccountInfo() {
    }

    public AccountInfo(Parcel in) {
        host = in.readString();
        port = in.readString();
        email = in.readString();
        password = in.readString();
        useSSL = in.readInt() == 1;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(host);
        out.writeString(port);
        out.writeString(email);
        out.writeString(password);
        out.writeInt(useSSL?1:0);
    }

    public static final Parcelable.Creator<AccountInfo> CREATOR = new Parcelable.Creator<AccountInfo>() {
        public AccountInfo createFromParcel(Parcel in) {
            return new AccountInfo(in);
        }

        public AccountInfo[] newArray(int size) {
            return new AccountInfo[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

}
