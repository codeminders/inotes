package com.codeminders.inotes.imap;

import javax.mail.Session;
import java.util.Properties;

public class ImapSession {
    protected String imapHost;
    protected String imapPort;
    private boolean useSSL;
    private Session session;

    public ImapSession(String imapHost, String imapPort, boolean useSSL) {
        this.imapHost = imapHost;
        this.imapPort = imapPort;
        this.useSSL = useSSL;
    }

    public Session getSession() {
        if (session != null) {
            return session;
        }
        Properties props = new Properties();
        if (useSSL) {
            props.setProperty("mail.store.protocol", "imaps");
            props.setProperty("mail.imaps.host", imapHost);
            props.setProperty("mail.imaps.port", imapPort);
            props.setProperty("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.setProperty("mail.imaps.socketFactory.fallback", "false");
            session = Session.getInstance(props);
        } else {
            props.setProperty("mail.store.protocol", "imap");
            props.setProperty("mail.imap.host", imapHost);
            props.setProperty("mail.imap.port", imapPort);
            session = Session.getDefaultInstance(props);
        }
        return session;
    }

    public String getProtocol() {
        if (session != null) {
            return session.getProperty("mail.store.protocol");
        } else {
            return null;
        }
    }

}
