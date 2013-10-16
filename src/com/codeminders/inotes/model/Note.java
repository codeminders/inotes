package com.codeminders.inotes.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Note implements Parcelable {
    private int id = -1;
    private boolean newNote = false;
    private Date date;
    private String title;
    private String note;
    private String account;
    private Map<String, String> headers = new HashMap<String, String>();

    public Note() {
    }

    public Note(Parcel in) {
        id = in.readInt();
        newNote = in.readInt() == 1;
        date = new Date(in.readLong());
        title = in.readString();
        note = in.readString();
        account = in.readString();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            headers.put(in.readString(), in.readString());
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeInt(newNote?1:0);
        out.writeLong(date.getTime());
        out.writeString(title);
        out.writeString(note);
        out.writeString(account);
        out.writeInt(headers.size());
        for (String key: headers.keySet()) {
            out.writeString(key);
            out.writeString(headers.get(key));
        }
    }

    public static final Parcelable.Creator<Note> CREATOR = new Parcelable.Creator<Note>() {
        public Note createFromParcel(Parcel in) {
            return new Note(in);
        }

        public Note[] newArray(int size) {
            return new Note[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isNewNote() {
        return newNote;
    }

    public void setNewNote(boolean newNote) {
        this.newNote = newNote;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

}
