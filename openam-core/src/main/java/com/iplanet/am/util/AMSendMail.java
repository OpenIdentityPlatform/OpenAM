/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AMSendMail.java,v 1.6 2009/12/22 19:57:19 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.iplanet.am.util;

import com.sun.identity.shared.Constants;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/*
 * This is a send mail utility class which can be used to send notifications to
 * the users if some event occurs.
 */
public class AMSendMail {

    private static String mailServerHost = SystemProperties.get(
            Constants.AM_SMTP_HOST, "localhost");
    private static String mailServerPort = SystemProperties.get(
            Constants.SM_SMTP_PORT, "25");
    private static Properties props = new Properties();


    static {
        // Set the host smtp address
        props.put("mail.smtp.host", mailServerHost);
        props.put("mail.smtp.port", mailServerPort);
    }

    /**
     * Posts e-mail messages to users. This method will wait on for the timeouts
     * when the specified host is down. Use this method in a separate thread so
     * that it will not hang when the mail server is down.
     *
     * @param recipients A String array of e-mail addresses to be sent to 
     * @param subject The e-mail subject
     * @param message The content contained in the e-mail
     * @param from The sending e-mail address
     * @exception MessagingException if there is any error in sending e-mail
     */
    public void postMail(String recipients[], String subject, String message,
            String from) throws MessagingException {
        postMail(recipients, subject, message, from, "UTF-8");
    }

    /**
     * Posts e-mail messages to users. This method will wait on for the timeouts
     * when the specified host is down. Use this method in a separate thread so
     * that it will not hang when the mail server is down.
     *
     * @param recipients A String array of e-mail addresses to be sent to
     * @param subject The e-mail subject
     * @param message The content contained in the e-mail
     * @param from The sending e-mail address 
     * @param charset The charset used in e-mail encoding
     * @exception MessagingException if there is any error in sending e-mail
     */

    public void postMail(String recipients[], String subject, String message,
            String from, String charset) throws MessagingException {
        boolean debug = false;

        // create some properties and get the default mail Session
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(debug);

        // create a message object
        MimeMessage msg = new MimeMessage(session);

        // set the from and to address
        InternetAddress addressFrom = new InternetAddress(from);
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[recipients.length];

        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = new InternetAddress(recipients[i]);
        }

        msg.setRecipients(Message.RecipientType.TO, addressTo);

        // Setting the Subject and Content Type
        if (charset == null) {
            msg.setSubject(subject);
            msg.setContent(message, "text/plain");
        } else {
            charset = BrowserEncoding.mapHttp2JavaCharset(charset);
            msg.setSubject(subject, charset);
            msg.setContent(message, "text/plain; charset=" + charset);
        }

        // Transport the message now
        Transport.send(msg);
    }

    /**
     * Posts e-mail messages to users. This method will wait on for the timeouts
     * when the specified host is down. Use this method in a separate thread so
     * that it will not hang when the mail server is down.
     *  
     * @param recipients A String array of e-mail addresses to be sent to
     * @param subject The e-mail subject
     * @param message The content contained in the e-mail
     * @param from The sending e-mail address
     * @param charset The charset used in e-mail encoding
     * @param host The host name to connect to send e-mail
     * @param port The host port to connect to send e-mail 
     * @param user The user name used to authenticate to the host
     * @param password The user password used to authenticate to the host
     * @param ssl A boolean to indicate whether SSL is needed to connect to the host 
     * @exception MessagingException if there is any error in sending e-mail
     */

    public void postMail(String recipients[], String subject, String message,
            String from, String charset, String host, String port,
            String user, String password, boolean ssl)
            throws MessagingException {

        boolean debug = false;

        Properties moduleProps = new Properties();

        moduleProps.put("mail.smtp.host", host);
        moduleProps.put("mail.debug", "true");
        moduleProps.put("mail.smtp.port", port);
        moduleProps.put("mail.smtp.socketFactory.port", port);
        if (ssl) {
            moduleProps.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        }
        moduleProps.put("mail.smtp.socketFactory.fallback", "false");

        Session session;
        // create some properties and get the mail Session
        if (user == null) {
            session = Session.getInstance(moduleProps);
        } else {
            moduleProps.put("mail.smtp.auth", "true");
            session = Session.getInstance(moduleProps, new AMUserNamePasswordAuthenticator(user, password));
        }

        session.setDebug(debug);

        // create a message object
        MimeMessage msg = new MimeMessage(session);

        // set the from and to address
        InternetAddress addressFrom = new InternetAddress(from);
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[recipients.length];

        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = new InternetAddress(recipients[i]);
        }

        msg.setRecipients(Message.RecipientType.TO, addressTo);

        // Setting the Subject and Content Type
        if (charset == null) {
            msg.setSubject(subject);
            msg.setContent(message, "text/plain");
        } else {
            charset = BrowserEncoding.mapHttp2JavaCharset(charset);
            msg.setSubject(subject, charset);
            msg.setContent(message, "text/plain; charset=" + charset);
        }

        // Transport the message now
        Transport.send(msg);
    }

    public static void main(String[] args) {

        String from = "<" + "ganesh@iplanet.com" + ">";
        String[] to = {"malla@sun.com", "ganesh@iplanet.com"};
        String sub = "Hello Bond";
        String msg = "Have fun dude";

        try {
            AMSendMail sm = new AMSendMail();
            sm.postMail(to, sub, msg, from);
        } catch (MessagingException ex) {
            System.out.println("Message Exception occured");
            ex.printStackTrace();
        }
    }
}
