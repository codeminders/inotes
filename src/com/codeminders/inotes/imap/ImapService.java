package com.codeminders.inotes.imap;

import android.content.Context;
import android.text.Html;
import com.codeminders.inotes.Utils;
import com.codeminders.inotes.db.DBManager;
import com.codeminders.inotes.model.AccountInfo;
import com.codeminders.inotes.model.Note;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SearchTerm;
import java.util.*;

public class ImapService extends ImapSession {
    private String user;
    private String password;
    private Context context;

    public ImapService(AccountInfo accountInfo, Context context) {
        super(accountInfo.getHost(), accountInfo.getPort(), accountInfo.isUseSSL());
        this.context = context;
        user = accountInfo.getEmail();
        password = accountInfo.getPassword();
    }

    public List<Note> getNotes() throws Exception {
        Folder notesFolder = null;
        List<Note> notes;
        try {
            notesFolder = getNotesFolder();
            notes = messagesToNotes(notesFolder.getMessages());
        } finally {
            closeConnection(notesFolder);
        }

        return notes;
    }

    private void closeConnection(Folder notesFolder) throws Exception {
        if (notesFolder != null) {
            Store store = notesFolder.getStore();
            notesFolder.close(true);
            store.close();
        }
    }

    public void addNotes(List<Note> notes) throws Exception {
        Folder notesFolder = null;
        try {
            notesFolder = getNotesFolder();
            notesFolder.appendMessages(notesToMessages(notes));
            setNotesStatusToOld(notes);
        } finally {
            closeConnection(notesFolder);
        }
    }

    private void setNotesStatusToOld(List<Note> notes) {
        DBManager dbManager = new DBManager(context);
        for (Note note: notes) {
            dbManager.setOldNote(note);
        }
    }

    private Message[] notesToMessages(List<Note> notes) throws Exception {
        Message[] messages = new Message[notes.size()];
        for (int i = 0; i < notes.size(); i++) {
            messages[i] = noteToMessage(notes.get(i));
        }

        return messages;
    }

    private Message noteToMessage(Note note) throws Exception {
        Message message = new MimeMessage(getSession());
        String subject = note.getTitle();
        message.setSubject(subject.equals("") ? null : subject);
        message.setSentDate(note.getDate());
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(user));
        message.setFrom(new InternetAddress(user));
        message.setContent(note.getNote(), "text/html");
        Map<String, String> headers = note.getHeaders();
        for (String key : headers.keySet()) {
            message.addHeader(key, headers.get(key));
        }

        return message;
    }

    private Folder getNotesFolder() throws Exception {
        Session imapSession = getSession();
        Store store = imapSession.getStore("imaps");
        store.connect(imapHost, user, password);

        Folder notesFolder = store.getFolder("Notes");
        notesFolder.open(Folder.READ_WRITE);
        return notesFolder;
    }

    private List<Note> messagesToNotes(Message[] messages) throws Exception {
        List<Note> notes = new ArrayList<Note>();
        List<Message> messagesWithEmptyId = new ArrayList<Message>();
        for (Message message : messages) {
            String[] serverId = message.getHeader(HeaderUtils.INOTES_ID_HEADER);
            if (serverId == null) {
                messagesWithEmptyId.add(noteToMessage(messageToNote(message)));
                message.setFlag(Flags.Flag.DELETED, true);
            } else {
                notes.add(messageToNote(message));
            }
        }
        for (Message message : messagesWithEmptyId) {
            message.addHeader(HeaderUtils.INOTES_ID_HEADER, Utils.getIdentifier(messageToNote(message)));
            notes.add(messageToNote(message));
        }

        Folder notesFolder = null;
        try {
            notesFolder = getNotesFolder();
            Message[] mess = new Message[messagesWithEmptyId.size()];
            int i = 0;
            for(Message message: messagesWithEmptyId) {
                mess[i++] = message;
            }
            notesFolder.appendMessages(mess);
        } finally {
            closeConnection(notesFolder);
        }

        return notes;
    }

    private Note messageToNote(Message message) throws Exception {
        Note note = new Note();
        String title = message.getSubject();
        note.setTitle(title != null ? title : "");
        note.setNote(getNoteBody(message));
        note.setAccount(user);
        note.setHeaders(HeaderUtils.getHeaders(message));
        Date sentDate = message.getSentDate();
        if (sentDate == null) {
            sentDate = new Date();
            note.setDate(sentDate);
            message.setFlag(Flags.Flag.DELETED, true);
            List<Note> notes = new ArrayList<Note>();
            notes.add(note);
            addNotes(notes);
        } else {
            note.setDate(sentDate);
        }

        return note;
    }

    private String getNoteBody(Message message) throws Exception {
        if (message.getContentType().toLowerCase().startsWith("text/plain") || message.getContentType().toLowerCase().startsWith("text/html")) {
            return (String) message.getContent();
        } else if (message.getContentType().toLowerCase().startsWith("multipart/")) {
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (bodyPart.getContentType().toLowerCase().startsWith("text/plain")) {
                    return (String) bodyPart.getContent();
                } else if (bodyPart.getContentType().toLowerCase().startsWith("text/html")) {
                    return Html.fromHtml((String) bodyPart.getContent()).toString();
                }
            }
        }
        return "";
    }

    public void deleteNotes(List<String> notesToDelete) throws Exception {
        if (notesToDelete.size() == 0) {
            return;
        }
        Folder notesFolder = null;
        try {
            notesFolder = getNotesFolder();
            Message[] messages = getMessagesByIds(notesFolder, notesToDelete);
            for (Message message : messages) {
                message.setFlag(Flags.Flag.DELETED, true);
            }
        } finally {
            closeConnection(notesFolder);
        }
    }

    @SuppressWarnings("serial")
    private Message[] getMessagesByIds(Folder notesFolder, final List<String> notes) throws Exception {
        return notesFolder.search(new SearchTerm() {
            @Override
            public boolean match(Message message) {
                try {
                    for (String note: notes) {
                        String[] id = message.getHeader(HeaderUtils.INOTES_ID_HEADER);
                        if (id != null && note.equals(id[0])) {
                            return true;
                        }
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    public void checkForServersDeletedNotes(Context context, String account) throws Exception {
        Folder notesFolder = null;
        try {
            notesFolder = getNotesFolder();
            Message[] messages = notesFolder.getMessages();
            DBManager dbManager = new DBManager(context);
            List<Note> notes = dbManager.getNotes(account);

            for (Note note : notes) {
                if (!note.isNewNote() && !findInServer(note, messages)) {
                    dbManager.deleteNote(note.getId());
                }
            }
        } finally {
            closeConnection(notesFolder);
        }
    }

    private boolean findInServer(Note note, Message[] messages) {
        try {
            for (Message message : messages) {
                String[] id = message.getHeader(HeaderUtils.INOTES_ID_HEADER);
                if (id != null && id[0].equals(note.getHeader(HeaderUtils.INOTES_ID_HEADER))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

}
