/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PWResetModelImpl.java,v 1.5 2009/12/18 03:28:29 222713 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 */
package com.sun.identity.password.ui.model;

import com.iplanet.am.util.AMSendMail;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import javax.mail.MessagingException;

/**
 * <code>PWResetModelImpl</code> defines the basic and commonly used 
 * methods used by view beans. 
 */
public class PWResetModelImpl
    implements PWResetModel {

    /**
     * Password reset Debug file name
     */
    private static final String PASSWORD_DEBUG_FILENAME = "amPasswordReset";

    /**
     *  Name of password reset enabled attribute
     */
    private static final String PW_RESET_ENABLED_ATTR =
        "iplanet-am-password-reset-enabled";

    /**
     *  Name of password reset lockout mode attribute.
     */
    private static final String PW_RESET_FAILURE_LOCKOUT_MODE =
        "iplanet-am-password-reset-failure-lockout-mode";

    /**
     *  Name of password reset lockout email address attribute.
     */
    private static final String PW_RESET_LOCKOUT_EMAIL_ADDRESS =
        "iplanet-am-password-reset-lockout-email-address";

    /**
     *  Name of password reset failure duration attribute.
     */
    private static final String PW_RESET_FAILURE_DURATION =
        "iplanet-am-password-reset-failure-duration";

    /**
     *  Name of password reset lockout warn user attribute.
     */
    private static final String PW_RESET_LOCKOUT_WARN_USER =
        "iplanet-am-password-reset-lockout-warn-user";


    /**
     *  Name of password reset failure count attribute.
     */
    private static final String PW_RESET_FAILURE_COUNT =
        "iplanet-am-password-reset-failure-count";

    /**
     *  Name of password reset lockout duration.
     */
    private static final String PW_RESET_FAILURE_LOCKOUT_DURATION =
        "iplanet-am-password-reset-lockout-duration";

    private static final String DEFAULT_MAIL_ATTR = "mail";

    /**
     * Name of the mail attribute.
     */
    private static final String PASSWORD_RESET_MAIL_ATTR_NAME = "openam-password-reset-mail-attribute-name";

    /**
     *  Name of password reset lockout multiplier.
     */
    private static final String PW_RESET_FAILURE_LOCKOUT_MULTIPLIER =
        "sunLockoutDurationMultiplier";

    /**
     *  Name of password reset lockout attribute name.
     */
    private static final String PW_RESET_LOCKOUT_ATTR_NAME =
        "iplanet-am-password-reset-lockout-attribute-name";

    /**
     *  Name of password reset lockout attribute value.
     */
    private static final String PW_RESET_LOCKOUT_ATTR_VALUE =
        "iplanet-am-password-reset-lockout-attribute-value";


    /**
     *  Name for string true value
     */
    public static final String STRING_TRUE = "true";


    /** 
     * Debug object 
     */
    public static Debug debug = Debug.getInstance(PASSWORD_DEBUG_FILENAME);

    /** 
     * SSO token object
     */
    protected SSOToken ssoToken;

    /** 
     * Resource bundle object
     */
    protected ResourceBundle resBundle = null;

    /** 
     * User distinguished name
     */
    protected String userId = null;

    /** 
     * Error message stored in the model
     */
    protected String errorMsg = null;

    /** 
     * Info message stored in the model
     */
    protected String informationMsg = null;

    /** 
     * Reset password message stored in the model
     */
    protected String passwordResetMsg = null;

    /** 
     *  Logger object 
     */
    protected PWResetAdminLog logger = null;
    
    protected ISLocaleContext localeContext = new ISLocaleContext();

    private static int NUM_OF_MILLISECS_IN_MIN = 60 * 1000;
    private static int PW_RESET_FAILURE_DURATION_TIME = 300;
    private static int PW_RESET_FAILURE_LOCKOUT_COUNT = 5;
    private static int PW_RESET_LOCKOUT_USER_WARN_COUNT = 4;

    private String rbName = DEFAULT_RB;
    private boolean pwResetEnable = true;
    
    private boolean pwResetFailureLockoutMode = false;
    private long pwResetFailureLockoutDuration = 0;
    private int pwResetFailureLockoutMultiplier = 0;
    private long pwResetFailureLockoutTime = 
        PW_RESET_FAILURE_DURATION_TIME * NUM_OF_MILLISECS_IN_MIN;

    private int pwResetFailureLockoutCnt = PW_RESET_FAILURE_LOCKOUT_COUNT;
    private String pwResetLockoutNotification = null;
    private int pwResetLockoutUserWarningCnt = PW_RESET_LOCKOUT_USER_WARN_COUNT;
    public String pwResetLockoutAttrName = null;
    public String pwResetLockoutAttrValue = null;
    
    /**
     * Creates a base model for password reset. 
     *
     */
    public PWResetModelImpl() {
        initialize();
    } 


    /**
     * Returns localized string.
     *
     * @param key resource string key.
     * @return localized string.
     */
    public String getLocalizedString(String key) {
        String i18nString = key;
        try {
	    ResourceBundle rb = PWResetResBundleCacher.getBundle(
                DEFAULT_RB, localeContext.getLocale());
	    i18nString = rb.getString(key); 
        } catch (MissingResourceException e) {
            debug.warning("PWResetModelImpl.getLocalizedString", e);
        }
        return i18nString;
    }
    
    /**
     * Returns locale of user.
     *
     * @return user's locale.
     */
    public ISLocaleContext getUserLocaleContext () {
        return localeContext;
    }


    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set to Error.
     *
     * @param message to be sent to the debug file.
     */
    public void debugError(String message) {
        debug.error(message);
    }

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set to Error.
     *
     * @param message to be printed. If message is null it is ignored.
     * @param e <code>printStackTrace</code> will be invoked to print
     *        the stack trace.
     */
    public void debugError(String message, Exception e) {
        debug.error(message, e);
    }

    /**
     * <code>true</code> if warning message is enabled.
     *
     * @return <code>true</code> if warning message is enabled.
     */
    public boolean warningEnabled() {
        return debug.warningEnabled();
    }

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     */
    public void debugWarning(String message) {
        debug.warning(message);
    }

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     * @param e <code>printStackTrace</code> will be invoked to print
     *        the stack trace.
     */
    public void debugWarning(String message, Exception e) {
        debug.warning(message, e);
    }

    /**
     * Returns <code>true</code> if message debugging is enabled.
     *
     * @return <code>true</code> if message debugging is enabled.
     */
    public boolean messageEnabled() {
        return debug.messageEnabled();
    }

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     */
    public void debugMessage(String message) {
        debug.message(message);
    }

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     * @param e <code>printStackTrace</code> will be invoked to print
     *                  the stack trace.
     */
    public void debugMessage(String message, Exception e) {
        debug.message(message, e);
    }

    /**
     * Returns HTML page title
     *
     * @return HTML page title
     */
    public String getHTMLPageTitle() {
        return getLocalizedString("pwConsole.title");
    }

    /**
     * Returns <code>true</code> if the password service is available.
     *
     * @return <code>true</code> if the password service is available.
     */
    public boolean isPasswordResetEnabled() {
        return pwResetEnable;
    }

    /** 
     * Returns the user distinguished name.
     *
     * @return user distinguished name.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Set the user distinguished name.
     *
     * @param value user distinguished name.
     */
    public void setUserDN(String value) {
        userId = value;
    }

    /**
     * Returns title for error message.
     *
     * @return title for error message.
     */
    public String getErrorTitle() {
        return getLocalizedString("error.title");
    }

    /**
     * Returns error message.
     *
     * @return error message.
     */
    public String getErrorMessage() {
        return errorMsg;
    }
  
    /**
     * Returns <code>true</code> if there is an error while processing request.
     *
     * @return <code>true</code> if there is an error while processing request.
     */
     public boolean isError() {
         return ((errorMsg != null) && (errorMsg.length() > 0));
     }

    /**
     * Returns copyright text.
     *
     * @return copyright text.
     */
    public String getCopyRightText() {
        return getLocalizedString("copyright.text");
       
    }

    /**
     * Returns password reset message.
     *
     * @return password reset message.
     */
    public String getPasswordResetMessage() {
        return passwordResetMsg;
    }


    /**
     * Returns a localized error message from an exception. If the exception
     * is of type <code>AMException</code> the error code and any possible
     * arguments will be extracted from the exception and the message will be
     * generated from the code and arguments. All other exception types will
     * return the message from <code>Exception.getMessage</code>.
     *
     * @param ex exception
     * @return Error message localized to users locale.
     */
    public String getErrorString(Exception ex) {
        return ex.getMessage();
    }
   
    /**
     * Returns root suffix.
     *
     * @return root suffix.
     */
    public String getRootSuffix() {
        return SMSEntry.getRootSuffix();
    }
    
    /**
     * Returns service schema.
     * @return service schema.
     */
    protected ServiceSchema getPWResetServiceSchema()
        throws SSOException, SMSException {
        ServiceSchemaManager mgr = new ServiceSchemaManager(
            PW_RESET_SERVICE, ssoToken);
        return mgr.getSchema(SchemaType.ORGANIZATION);
    }

    /**
     * Returns localized string of an attribute in a service
     *
     * @param mgr Service schema manager.
     * @param key localization key of the attribute.
     * @return localized string of an attribute in a service.
     */
    protected String getL10NAttributeName(
        ServiceSchemaManager mgr,
        String key
    ) {
        String i18nName = key;
        try {
            String name = mgr.getI18NFileName();
            if (name != null) {
                ResourceBundle rb = PWResetResBundleCacher.getBundle(
                    name, localeContext.getLocale());
                i18nName = Locale.getString(rb, key, debug);
            }
        } catch (MissingResourceException e) {
            debug.warning("PWResetModelImpl.getL10NAttributeName", e);
        }
        return i18nName;
    }

    /**
     * Returns localized string of an attribute in a service.
     *
     * @param serviceName Name of service.
     * @param key Localization key of the attribute.
     * @return localized string of an attribute in a service.
     */
    public String getL10NAttributeName(String serviceName, String key) {
        String i18nName = key;

        try {
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, ssoToken);
            String name = mgr.getI18NFileName();
            if (name != null) {
                ResourceBundle rb = PWResetResBundleCacher.getBundle(
                    name,  localeContext.getLocale());
                i18nName = Locale.getString(rb, key, debug);
            }
        } catch (MissingResourceException mre) {
            if (debug.warningEnabled()) {
                debug.warning("PWResetModelImpl.getL10NAttributeName: " +
                    "Could not localized str for " + key + " in service " + 
                    serviceName, mre);
            }
        } catch (SSOException e) {
            debug.warning("PWResetModelImpl.getL10NAttributeName", e);
        } catch (SMSException e) {
            debug.warning("PWResetModelImpl.getL10NAttributeName", e);
        }

        return i18nName;
    }

    /**
     * Initializes the data for this model by getting locale, SSO Token
     * and <code>AMStoreConnection</code>
     */
    private void initialize() {
        ssoToken = getSSOToken();
        logger = new PWResetAdminLog(ssoToken);
        resBundle = PWResetResBundleCacher.getBundle(
            rbName, localeContext.getLocale());
    }

    /**
     * Sets the password reset enabled flag depending
     * what is set in the password service for a given realm.
     *
     * @param realm Realm
     */
    public void readPWResetProfile(String realm) {
        
        String value = null;
        try {
            value = getAttributeValue(realm, PW_RESET_ENABLED_ATTR);
        } catch (SSOException e) {
            debug.warning("PWResetModelImpl.readPWResetProfile", e);
        } catch (SMSException e) {
            debug.error("PWResetModelImpl.readPWResetProfile", e);
        }
        if ((value == null) || !value.equals(STRING_TRUE)) {
            pwResetEnable = false;
            informationMsg = getLocalizedString("pwResetDisabled.message");
        }
    }

    protected boolean isAttributeSet(String realm, String attrName)
        throws SSOException, SMSException {
        String value = getAttributeValue(realm, attrName);
        return (value != null) && value.equals(STRING_TRUE);
    }

    protected Set getDefaultAttrValues(ServiceSchema schema, String attrName) {
        Set defaultValues = Collections.EMPTY_SET;
        if (schema != null) {
            AttributeSchema as = schema.getAttributeSchema(attrName);
            if (as != null) {
                defaultValues = as.getDefaultValues();
            }
        }
        return defaultValues;
    }

    protected String getAttributeValue(String realm, String attrName)
        throws SSOException, SMSException {
        Set set = getAttributeValues(realm, attrName);
        return getFirstElement(set);
    }

    protected Set getAttributeValues(String realm, String attrName)
        throws SSOException, SMSException {
        OrganizationConfigManager mgr = new OrganizationConfigManager(
            ssoToken, realm);
        Map attributeValues = mgr.getServiceAttributes(PW_RESET_SERVICE);
        return (Set)attributeValues.get(attrName);
    }
    
    
    /**
     * Returns information message
     *
     * @return information message
     */
    public String getInformationMessage() {
        return informationMsg;
    }

    /**
     * Returns the first <code>String</code> element from the given set.
     * If the set is empty, or null, an empty string will be returned.
     *
     * @param set where element resides
     * @return first String element from the set.
     */
    public static String getFirstElement(Set set) {
        return ((set != null) && !set.isEmpty())
            ? (String)set.iterator().next(): "";
    }

    /**
     * Writes to the log file formatted message.
     *  
     * @param msgId  Id of the message to be written
     * @param userDN  user distinguished name
     */ 
    public void writeLog(String msgId, String userDN) {
         writeLog(msgId, "", userDN);
    }

    /**
     * Writes to the log file formatted message.
     *
     * @param msgId  Id of the message to be written
     * @param msg  additional message to be written
     * @param userDN  user distinguished name
     */
    public void writeLog(String msgId, String msg, String userDN) {
        String[] obj = {userDN};
        logger.doLog(MessageFormat.format(getLocalizedString(msgId), (Object[])obj) 
            + " " + msg);
    }

    /**
     * Returns true if the password reset lockout feature is enabled.
     *
     * @return true if the password reset lockout feature is enabled.
     */
    public boolean isPasswordResetFailureLockoutEnabled() {
        return pwResetFailureLockoutMode;
    }

    /**
     * Returns lockout duration for password reset.
     *
     * @return lockout duration for password reset
     */
    public long getPasswordResetFailureLockoutDuration() {
        return pwResetFailureLockoutDuration;
    }

    /**
     * Returns lockout multiplier for password reset.
     *
     * @return lockout multiplier for password reset
     */
    public int getPasswordResetFailureLockoutMultiplier() {
        return pwResetFailureLockoutMultiplier;
    }


    /**
     * Returns failure lockout time interval for password reset.
     *
     * @return failure duration interval for password reset
     */
    public long getPasswordResetFailureLockoutTime() {
        return pwResetFailureLockoutTime;
    }

    /**
     * Returns failure lockout count for password reset.
     *
     * @return failure lockout count for password reset
     */
    public int getPasswordResetFailureLockoutCount() {
        return pwResetFailureLockoutCnt;
    }

    /**
     * Returns lockout notification email address for password reset.
     *
     * @return lockout notification email address for password reset
     */
    public String getPasswordResetLockoutNotification() {
        return pwResetLockoutNotification;
    }

    /**
     * Returns lockout warn user count for password reset.
     *
     * @return lockout warn user count for password reset
     */
    public int getPasswordResetLockoutUserWarningCount() {
        return pwResetLockoutUserWarningCnt;
    }

    /**
     * Populates the password reset lockout attributes for a given
     * organization distinguished name and stored them in the model.
     *
     * @param orgDN  organization distinguished name
     */
    public void populateLockoutValues(String orgDN) {
        try {
            String value = getAttributeValue(orgDN, PW_RESET_FAILURE_LOCKOUT_MODE);
            if (value != null && value.equals(STRING_TRUE)) {
                pwResetFailureLockoutMode = true;
            }

            value = getAttributeValue(orgDN, PW_RESET_FAILURE_DURATION);
            if (value != null && value.length() > 0) {
                pwResetFailureLockoutTime = Long.parseLong(value);
                pwResetFailureLockoutTime *= NUM_OF_MILLISECS_IN_MIN;
            }

            pwResetLockoutNotification =
                getAttributeValue(orgDN, PW_RESET_LOCKOUT_EMAIL_ADDRESS);

            value = getAttributeValue(orgDN, PW_RESET_LOCKOUT_WARN_USER);
            if (value != null && value.length() > 0) {
                pwResetLockoutUserWarningCnt = Integer.parseInt(value);
            }

            value = getAttributeValue(orgDN, PW_RESET_FAILURE_LOCKOUT_DURATION);
            if (value != null && value.length() > 0) {
                pwResetFailureLockoutDuration = Long.parseLong(value);
                pwResetFailureLockoutDuration *= NUM_OF_MILLISECS_IN_MIN;
            }

            value = getAttributeValue(orgDN, PW_RESET_FAILURE_LOCKOUT_MULTIPLIER);
            if (value != null && value.length() > 0) {
                pwResetFailureLockoutMultiplier = Integer.parseInt(value);
            }

            value = getAttributeValue(orgDN, PW_RESET_FAILURE_COUNT);
            if (value != null && value.length() > 0) {
                pwResetFailureLockoutCnt = Integer.parseInt(value);
            }

            value = getAttributeValue(orgDN, PW_RESET_LOCKOUT_ATTR_NAME); 
            if (value != null && value.trim().length() > 0) {
                pwResetLockoutAttrName = value;
            }

            value = getAttributeValue(orgDN, PW_RESET_LOCKOUT_ATTR_VALUE);
            if (value != null && value.trim().length() > 0) {
                pwResetLockoutAttrValue = value;
            }
        } catch (NumberFormatException e) {
            debug.error("PWResetModelImpl.populateLockoutValues", e);
        } catch (SSOException e) {
            debug.warning("PWResetModelImpl.populateLockoutValues", e);
        } catch (SMSException e) {
            debug.error("PWResetModelImpl.populateLockoutValues", e);
        }
    }

    /**
     * Returns true if the user is locked out from resetting password.
     *
     * @param userDN user distinguished name
     * @param orgDN organization distinguished name
     * @return true if the user is locked out
     */
    public boolean isUserLockout(String userDN, String orgDN) {
        populateLockoutValues(orgDN);
        PWResetAccountLockout pwResetLockout = new PWResetAccountLockout(this);
        return pwResetLockout.isLockout(userDN);
    }

    /**
     * Sents email to the user(s).
     *
     * @param from sender email address
     * @param to user to which email is send to
     * @param subject email subject
     * @param msg email message
     * @param charset charset value
     */
    public void sendEmailToUser(
        String from,
        String to[],
        String subject,
        String msg,
        String charset) 
    {
        AMSendMail sm = new AMSendMail();
        try {
            sm.postMail(to, subject, msg, from, charset);
        } catch (MessagingException ex) {
            debug.error("Could not send email to user " + to, ex);
        }
    }

    /**
     * Returns password reset lockout attribute name.
     *
     * @return password reset lockout attribute name
     */
    public String getPasswordResetLockoutAttributeName() {
        return pwResetLockoutAttrName;
    }

    /**
     * Returns password reset lockout attribute value.
     *
     * @return password reset lockout attribute value
     */
    public String getPasswordResetLockoutAttributeValue() {
        return pwResetLockoutAttrValue;
    }

    /**
     * Returns label for SUN logo.
     *
     * @return label for SUN logo.
     */
    public String getSunLogoLabel() {
        return getLocalizedString("sunLogo.label");
    }

    /**
     * Returns label for product logo.
     *
     * @return label for product logo.
     */
    public String getProductLabel() {
        return getLocalizedString("product.label");
    }

    /**
     * Returns label for Java logo.
     *
     * @return label for Java logo.
     */
    public String getJavaLogoLabel() {
        return getLocalizedString("javaLogo.label");
    }

    protected SSOToken getSSOToken() {
        return (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    }

    /**
     * Sets the user locale.
     *
     * @param localeString localeString.
     */
    public void setUserLocale(String localeString) {
        localeContext.setUserLocale(localeString);
    }

    /**
     * {@inheritDoc}
     */
    public String getMailAttribute(String realm) {
        String mail = DEFAULT_MAIL_ATTR;
        try {
            mail = getAttributeValue(realm, PASSWORD_RESET_MAIL_ATTR_NAME);
        } catch (Exception ex) {
            debug.warning("An error occurred while trying to retrieve the name of the mail attribute", ex);
        }
        return mail;
    }
}
