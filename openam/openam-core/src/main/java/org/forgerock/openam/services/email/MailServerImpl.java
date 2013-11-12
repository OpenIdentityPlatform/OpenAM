/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */
package org.forgerock.openam.services.email;

import com.iplanet.am.util.AMSendMail;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

import javax.mail.MessagingException;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MailServerImpl implements MailServer {

    protected Debug debug = null;

    private static String SMTP_HOSTNAME = "forgerockEmailServiceSMTPHostName";
    private static String SMTP_HOSTPORT = "forgerockEmailServiceSMTPHostPort";
    private static String SMTP_USERNAME = "forgerockEmailServiceSMTPUserName";
    private static String SMTP_USERPASSWORD = "forgerockEmailServiceSMTPUserPassword";
    private static String SMTP_SSL_ENABLED = "forgerockEmailServiceSMTPSSLEnabled";
    private static String FROM_ADDRESS = "forgerockEmailServiceSMTPFromAddress";
    private static String SUBJECT = "forgerockEmailServiceSMTPSubject";
    private static String MESSAGE = "forgerockEmailServiceSMTPMessage";

    final static public String SERVICE_NAME = "MailServer";
    final static public String SERVICE_VERSION = "1.0";

    private Map<String, Set<String>> options = null;
    private String smtpHostName = null;
    private String smtpHostPort = null;
    private String smtpUserName = null;
    private String smtpUserPassword = null;
    private String smtpSSLEnabled = null;
    private boolean sslEnabled = true;
    private String from = null;
    private String subject = null;
    private String message = null;


    private static ServiceConfig scm = null;
    private static ServiceConfigManager mgr = null;
    private static AMSendMail sendMail = null;

    /**
     * Default Constructor
     * @param realm in which emails service shall be created
     */
    public MailServerImpl(String realm) {
        debug = Debug.getInstance("amMailServer");
        sendMail = new AMSendMail();
        try {
            mgr = new ServiceConfigManager((SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance()),
                    SERVICE_NAME, SERVICE_VERSION);
            scm = mgr.getOrganizationConfig(realm,null);
            options = scm.getAttributes();
        } catch (Exception e) {
            debug.error("Cannot get ServiceConfigManager", e);
        }
    }

    /**
     * Constructor used for testing
     * @param mgr  Service Configuration Manager
     * @param scm  Service Configuration of the email service
     * @param debug Debug instance to provide useful debugging information
     * @param sendMail AMSendmail instance needed for testing
     * @param options SMTP Options
     */
    public MailServerImpl(ServiceConfigManager mgr, ServiceConfig scm, Debug debug, AMSendMail sendMail,
                          Map<String, Set<String>> options){
        this.options = options;
        this.debug = debug;
        this.sendMail = sendMail;
        this.scm = scm;
        this.mgr = mgr;
    }

    /**
     * Sets the SMTP options
     * @param options SMTP Options
     */
    private void setOptions(Map options) {
        // Tries to set Options passed in, otherwise uses default options from service
        smtpHostName = CollectionHelper.getMapAttr(options, SMTP_HOSTNAME);
        if(smtpHostName == null || smtpHostName.isEmpty()){
            smtpHostName = CollectionHelper.getMapAttr(this.options, SMTP_HOSTNAME);
        }
        smtpHostPort = CollectionHelper.getMapAttr(options, SMTP_HOSTPORT);
        if(smtpHostPort == null || smtpHostPort.isEmpty()){
            smtpHostPort = CollectionHelper.getMapAttr(this.options, SMTP_HOSTPORT);
        }
        smtpUserName = CollectionHelper.getMapAttr(options, SMTP_USERNAME);
        if(smtpUserName == null || smtpUserName.isEmpty()){
            smtpUserName = CollectionHelper.getMapAttr(this.options, SMTP_USERNAME);
        }
        smtpUserPassword = CollectionHelper.getMapAttr(options, SMTP_USERPASSWORD);
        if(smtpUserPassword == null || smtpUserPassword.isEmpty()){
            smtpUserPassword = CollectionHelper.getMapAttr(this.options, SMTP_USERPASSWORD);
        }
        smtpSSLEnabled = CollectionHelper.getMapAttr(options, SMTP_SSL_ENABLED);
        if(smtpSSLEnabled == null || smtpSSLEnabled.isEmpty()){
            smtpSSLEnabled = CollectionHelper.getMapAttr(this.options, SMTP_SSL_ENABLED);
        }
        from = CollectionHelper.getMapAttr(options, FROM_ADDRESS);
        if(from == null || from.isEmpty()){
            from = CollectionHelper.getMapAttr(this.options, FROM_ADDRESS);
        }
        subject = CollectionHelper.getMapAttr(options, SUBJECT);
        if(subject == null ){
            subject = CollectionHelper.getMapAttr(this.options, SUBJECT);
        }
        message = CollectionHelper.getMapAttr(options, MESSAGE);
        if(message == null || message.isEmpty()){
            message = CollectionHelper.getMapAttr(this.options, MESSAGE);
        }

        if (smtpSSLEnabled != null) {
            if (smtpSSLEnabled.equals("Non SSL")) {
                sslEnabled = false;
            }
        }
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    /* * {@inheritDoc}
     */
    public void sendEmail(String to, String subject, String message) throws MessagingException{
        sendEmail(null,to, subject, message, null);
    }

    /* * {@inheritDoc}
     */
    public void sendEmail(String from, String to, String subject,
                          String message, Map options) throws MessagingException{
        if (to == null) {
            return;
        }
        try {
            if(options != null && !options.isEmpty()){
                setOptions(options);
                // make sure that all options are available or updates from global map..
            } else {
                //user global settings...
                setOptions(this.options);
                from = this.from;
            }
            String tos[] = new String[1];
            tos[0] = to;

            if (smtpHostName == null || smtpHostPort == null ||
                    smtpUserName == null || smtpUserPassword == null ||
                    smtpSSLEnabled == null) {
                sendMail.postMail(tos, subject, message, from);
            } else {
                sendMail.postMail(tos, subject, message, from, "UTF-8", smtpHostName,
                        smtpHostPort, smtpUserName, smtpUserPassword,
                        sslEnabled);
            }
            if (debug.messageEnabled()) {
                debug.message("MailServerImpl.sendEmail() : " +
                        "Email sent to : " + to + ".");
            }
        } catch (Exception e) {
            debug.error("SendMessageImple.sendEmail() : " +
                    "Exception in sending Email: " , e);
            throw new MessagingException("Failed to send Email to " + to, e);
        }

    }
}
