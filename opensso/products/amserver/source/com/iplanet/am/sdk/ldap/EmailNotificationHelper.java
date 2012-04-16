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
 * $Id: EmailNotificationHelper.java,v 1.5 2009/01/28 05:34:48 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk.ldap;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMServiceUtils;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMTemplate;
import com.iplanet.am.util.AMSendMail;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.MessagingException;
import com.sun.identity.shared.ldap.util.DN;

/**
 * This class has the functionality to send email notifications to the users
 * listed in the notification list for create, delete & modify
 */
public class EmailNotificationHelper {

    public static final String ADMINISTRATION_SERVICE = 
        "iPlanetAMAdminConsoleService";

    // notification attribute names
    public static final String USER_CREATE_NOTIFICATION_LIST = 
        "iplanet-am-user-create-notification-list";

    public static final String USER_DELETE_NOTIFICATION_LIST = 
        "iplanet-am-user-delete-notification-list";

    public static final String USER_MODIFY_NOTIFICATION_LIST = 
        "iplanet-am-user-modify-notification-list";

    public static final String EMAIL_ATTRIBUTE = "mail";

    private static SSOToken internalToken = CommonUtils.getInternalToken();

    private static Debug debug = CommonUtils.getDebugInstance();

    private AMSendMail mailer = null;

    private String entryDN;

    private String organizationDN;

    private Set createNotifyList = Collections.EMPTY_SET;

    private Set deleteNotifyList = Collections.EMPTY_SET;

    private Set modifyNotifyList = Collections.EMPTY_SET;

    public EmailNotificationHelper(String userDN) {
        entryDN = userDN;
        organizationDN = (new DN(userDN)).getParent().toString();
        mailer = new AMSendMail();
    }

    private void sendEmail(Set notifyList, Map attributes, String fromCode,
            String subjectCode, String messageCode) throws MessagingException {
        String self = AMSDKBundle.getString("504");
        Iterator iter = notifyList.iterator();
        while (iter.hasNext()) {
            // Populate the to string
            StringTokenizer stz = new StringTokenizer(
                    (String) iter.next(), "|");
            String emailStr = stz.nextToken();
            String[] to;
            if (emailStr.equals(self)) {
                Set emails = (Set) attributes.get(EMAIL_ATTRIBUTE);
                if (emails == null || emails.isEmpty()) {
                    continue;
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("EmailNotificationHelper.sendMail(): "
                                + "Converting to array for: " + emails);
                    }
                    to = (String[]) emails.toArray(new String[emails.size()]);
                }
            } else if (emailStr.startsWith(self + ":")) {
                String attrName = emailStr.substring((self + ":").length());
                if (attrName == null || attrName.length() == 0) {
                    continue;
                }
                Set emails = (Set) attributes.get(attrName);
                if (emails == null || emails.isEmpty()) {
                    continue;
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("EmailNotificationHelper.sendMail(): "
                                + "Converting to array for: " + emails);
                    }
                    to = (String[]) emails.toArray(new String[emails.size()]);
                }
            } else {
                to = new String[1];
                to[0] = emailStr;
            }

            // Populate locate and charset
            String locale = null;
            String charset = null;
            if (stz.hasMoreTokens()) {
                locale = stz.nextToken();
                if (stz.hasMoreTokens()) {
                    charset = stz.nextToken();
                }
            }

            // Populate the from, subject & message strings
            String from = AMSDKBundle.getString(fromCode, locale);
            String subject = AMSDKBundle.getString(subjectCode, locale);
            String message = AMSDKBundle.getString(messageCode, locale) + " "
                    + entryDN;

            // Send email notification
            mailer.postMail(to, subject, message, from, charset);
        }
    }

    private Set getNotificationList(String attributeName) {
        Set notifyList = Collections.EMPTY_SET;
        try {
            String organizationDN = DirectoryServicesFactory.getInstance()
                    .getOrganizationDN(internalToken, this.organizationDN);

            // FIXME:
            // TODO: Remove dependency on AMStoreConnection!
            AMStoreConnection amsc = new AMStoreConnection(internalToken);
            AMOrganization amOrg = amsc.getOrganization(organizationDN);
            notifyList = getOrgTypeAttributes(amOrg, ADMINISTRATION_SERVICE,
                    attributeName);
        } catch (AMException ae) {
            debug.error("EmailNotificationHelper.getNotificationList() "
                    + "Unable to get notification List for " + attributeName
                    + " for user: " + entryDN, ae);
        } catch (SSOException e) {
            debug.error("EmailNotificationHelper.getNotificationList() "
                    + "Unable to get notification List for " + attributeName
                    + " for user: " + entryDN, e);
        }
        return notifyList;
    }

    public void setUserCreateNotificationList() {
        createNotifyList = getNotificationList(USER_CREATE_NOTIFICATION_LIST);
    }

    public void setUserDeleteNotificationList() {
        deleteNotifyList = getNotificationList(USER_DELETE_NOTIFICATION_LIST);
    }

    public void setUserModifyNotificationList() {
        modifyNotifyList = getNotificationList(USER_MODIFY_NOTIFICATION_LIST);
    }

    public boolean isPresentUserCreateNotificationList() {
        return (createNotifyList != null && !createNotifyList.isEmpty());
    }

    public boolean isPresentUserDeleteNotificationList() {
        return (deleteNotifyList != null && !deleteNotifyList.isEmpty());
    }

    public boolean isPresentUserModifyNotificationList() {
        return (modifyNotifyList != null && !modifyNotifyList.isEmpty());
    }

    /**
     * The proper setUser<>NotificationList method should be called before
     * calling this method. 
     * 
     * @param attributes
     *            the attributes of the user
     */
    public void sendUserCreateNotification(Map attributes) {
        try {
            if (createNotifyList != null && !createNotifyList.isEmpty()) {
                sendEmail(createNotifyList, attributes, "497", "490", "493");
            }
        } catch (MessagingException e) {
            if (debug.warningEnabled()) {
                debug.warning("EmailNotificationHelper."
                        + "sendUserCreateNotification() Unable to send " 
                        + "email for user: " + entryDN, e);
            }
        }
    }

    /**
     * The proper setUser<>NotificationList method should be called before
     * calling this method.
     * 
     * @param attributes
     *            the attributes of the user
     */
    public void sendUserDeleteNotification(Map attributes) {
        try {
            if (deleteNotifyList != null && !deleteNotifyList.isEmpty()) {
                sendEmail(deleteNotifyList, attributes, "497", "491", "494");
            }
        } catch (MessagingException e) {
            if (debug.warningEnabled()) {
                debug.warning("EmailNotificationHelper."
                        + "sendUserDeleteNotification() Unable to send " 
                        + "email for user: " + entryDN, e);
            }
        }
    }

    // TODO: Refactor this method.
    /**
     * The proper setUser<>NotificationList method should be called before
     * calling this method.
     * 
     * @param token
     *            a valid single sign on token
     * @param attributes
     *            the attribues of the user
     * @param oldAttributes
     *            the previous attributes of the user
     */
    public void sendUserModifyNotification(SSOToken token, Map attributes,
            Map oldAttributes) {
        if (modifyNotifyList == null || modifyNotifyList.isEmpty()) {
            return;
        }

        // TODO: Refactor code to use maps directly
        AttrSet attrSet = CommonUtils.mapToAttrSet(attributes);
        AttrSet oldAttrSet = CommonUtils.mapToAttrSet(oldAttributes);
        try {
            String self = AMSDKBundle.getString("504");
            Iterator iter = modifyNotifyList.iterator();
            while (iter.hasNext()) {
                String val = (String) iter.next();
                StringTokenizer stz = new StringTokenizer(val);
                int toLen = stz.countTokens();
                if (toLen > 0) {
                    String attrName = stz.nextToken().toLowerCase();

                    boolean valuesChanged = false;
                    Attr newAttrVal = null;
                    Attr oldAttrVal = null;
                    StringBuilder newSB = new StringBuilder();
                    StringBuilder oldSB = new StringBuilder();
                    if (attrSet.contains(attrName)) {
                        newAttrVal = attrSet.getAttribute(attrName);
                        if (newAttrVal != null) {
                            String[] newvalues = newAttrVal.getStringValues();
                            for (int i = 0; i < newvalues.length; i++) {
                                newSB.append(newvalues[i]);
                            }
                        }
                    }
                    if (oldAttrSet.contains(attrName)) {
                        oldAttrVal = oldAttrSet.getAttribute(attrName);
                        if (oldAttrVal != null) {
                            String[] oldvalues = oldAttrVal.getStringValues();
                            for (int i = 0; i < oldvalues.length; i++) {
                                oldSB.append(oldvalues[i]);
                            }
                        }
                    }
                    String newStr = newSB.toString();
                    String oldStr = oldSB.toString();
                    valuesChanged = !newStr.equalsIgnoreCase(oldStr);

                    if (valuesChanged) {
                        while (stz.hasMoreTokens()) {
                            StringTokenizer stz2 = new StringTokenizer(stz
                                    .nextToken(), "|");

                            String email = stz2.nextToken();
                            String[] to;

                            if (email.equals(self)) {
                                Set attrNamesSet = new HashSet(1);
                                attrNamesSet.add(EMAIL_ATTRIBUTE);
                                Map emailAttrMap = DirectoryServicesFactory
                                        .getInstance().getAttributes(token,
                                                entryDN, attrNamesSet,
                                                AMObject.USER);
                                Set emails = (Set) emailAttrMap
                                        .get(EMAIL_ATTRIBUTE);
                                if (emails == null || emails.isEmpty()) {
                                    continue;
                                } else {
                                    to = (String[]) emails
                                            .toArray(new String[emails.size()]);
                                }
                            } else if (email.startsWith(self + ":")) {
                                String emailAttrName = email
                                        .substring((self + ":").length());
                                if (emailAttrName == null
                                        || emailAttrName.length() == 0) {
                                    continue;
                                }
                                Set attrNamesSet = new HashSet(1);
                                attrNamesSet.add(emailAttrName);
                                Map emailAttrMap = DirectoryServicesFactory
                                        .getInstance().getAttributes(token,
                                                entryDN, attrNamesSet,
                                                AMObject.USER);
                                Set emails = (Set) emailAttrMap
                                        .get(emailAttrName);
                                if (emails == null || emails.isEmpty()) {
                                    continue;
                                } else {
                                    to = (String[]) emails
                                            .toArray(new String[emails.size()]);
                                }
                            } else {
                                to = new String[1];
                                to[0] = email;
                            }

                            String locale = null;
                            String charset = null;
                            if (stz2.hasMoreTokens()) {
                                locale = stz2.nextToken();
                                if (stz2.hasMoreTokens()) {
                                    charset = stz2.nextToken();
                                }
                            }

                            Attr oldAttr = oldAttrSet.getAttribute(attrName);
                            Attr newAttr = attrSet.getAttribute(attrName);

                            String sub = AMSDKBundle.getString("492", locale);
                            StringBuilder msgSB = new StringBuilder();
                            msgSB.append(AMSDKBundle.getString("495", locale))
                                    .append(" ").append(entryDN).append("\n")
                                    .append(
                                            AMSDKBundle
                                                    .getString("496", locale))
                                    .append(" ").append(attrName).append("\n")
                                    .append(
                                            AMSDKBundle
                                                    .getString("502", locale))
                                    .append("\n");
                            if (oldAttr != null) {
                                String[] values = oldAttr.getStringValues();
                                for (int i = 0; i < values.length; i++) {
                                    msgSB.append("    ").append(values[i])
                                            .append("\n");
                                }
                            }
                            msgSB.append(AMSDKBundle.getString("503", locale))
                                    .append("\n");
                            if (newAttr != null) {
                                String[] values = newAttr.getStringValues();
                                for (int i = 0; i < values.length; i++) {
                                    msgSB.append("    ").append(values[i])
                                            .append("\n");
                                }
                            }

                            String from = AMSDKBundle.getString("497", locale);
                            mailer.postMail(to, sub, msgSB.toString(), from,
                                    charset);
                        }
                    }
                }
            }
        } catch (MessagingException me) {
            if (debug.warningEnabled()) {
                debug.warning("EmailNotificationHelper."
                        + "sendUserModifyNotification() Unable to send " 
                        + "email for user: " + entryDN, me);
            }
        } catch (SSOException e) {
            debug.error("EmailNotificationHelper.sendUserModifyNotification() "
                    + "Error occured while trying to send email for user: "
                    + entryDN, e);
        } catch (AMException ex) {
            debug.error("EmailNotificationHelper.sendUserModifyNotification() "
                    + "Error occured while trying to send email for user: "
                    + entryDN, ex);
        }
    }

    /**
     * Protected method to be used to obtain organization attribute values for a
     * given serviceName and attribute name. Returns a null value if a template
     * value or default value for the attribute does not exist.
     */
    protected Set getOrgTypeAttributes(AMOrganization org, String serviceName,
            String attrName) throws SSOException {
        Set attrValues = null;
        try {
            AMTemplate amTemplate = org.getTemplate(serviceName,
                    AMTemplate.ORGANIZATION_TEMPLATE);
            attrValues = amTemplate.getAttribute(attrName);
            if (debug.messageEnabled()) {
                debug.message("AMOrganizationImpl."
                        + "getOrgTypeAttributes(): "
                        + "obtained from org template " + serviceName + " : "
                        + attrName + "\n" + org.getDN() + " : " + attrValues);
            }
        } catch (AMException ame) {
            // Template not found
            // Get default Service attribues
            try {
                Map defaultValues = AMServiceUtils.getServiceConfig(
                        internalToken, ADMINISTRATION_SERVICE,
                        SchemaType.ORGANIZATION);
                attrValues = (Set) defaultValues.get(attrName);
                if (debug.messageEnabled()) {
                    debug.message("AMOrganizationImpl."
                            + "getOrgTypeAttributes(): "
                            + "obtained from org defaults " + serviceName
                            + " : " + attrName + "\n" + org.getDN() + " : "
                            + attrValues);
                }
            } catch (Exception se) {
                debug.warning("AMOrganizationImpl."
                        + "getOrgTypeAttributes(): "
                        + "Error encountered in retrieving "
                        + "default org attrs for", se);
            }

        }
        return attrValues;
    }

}
