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
 * $Id: PWResetModel.java,v 1.5 2009/12/18 03:26:59 222713 Exp $
 *
 */
/**
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.password.ui.model;

import com.iplanet.sso.SSOException;

/**
 * <code>PWResetModelImpl</code> defines the basic and commonly used
 * methods used by view beans. 
 */
public interface PWResetModel {   
    /**
     * Default resource bundle name
     */
    String DEFAULT_RB = "amPasswordResetModuleMsgs";

    /**
     * Name of password reset service
     */
    String PW_RESET_SERVICE = "iPlanetAMPasswordResetService";

    /**
     * OpenSSO's user service name
     */
    String USER_SERVICE = "iPlanetAMUserService";

    /**
     * Name of user active status.
     */
    String ACTIVE = "Active";

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set to Error.
     *
     * @param message to be sent to the debug file.
     */
    void debugError(String message);

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set to Error.
     *
     * @param message to be printed. If message is null it is ignored.
     * @param e <code>printStackTrace</code> will be invoked to print
     *                  the stack trace.
     */
    void debugError(String message, Exception e);

    /**
     * Returns HTML page title.
     *
     * @return HTML page title.
     */
    String getHTMLPageTitle();

    /**
     * Returns <code>true</code> if the password service is enabled.
     *
     * @return <code>true</code> if the password service is available.
     */
    boolean isPasswordResetEnabled();

    /**
     * Sets the user distinguished name.
     *
     * @param value user distinguished name.
     */
    void setUserDN(String value);

    /** 
     * Returns the user distinguished name.
     *
     * @return user distinguished name.
     */
    String getUserId();
  
    /**
     * Returns title for error message.
     *
     * @return title for error message.
     */
    String getErrorTitle();
    
    /**
     * Returns error message
     *
     * @return error message
     */
    String getErrorMessage();


    /**
     * Returns <code>true</code> if there is an error while processing request
     *
     * @return <code>true</code> if error, false otherwise
     */
    public boolean isError();

    /**
     * Returns copyright text
     *
     * @return copyright text
     */
    String getCopyRightText();

    /**
     * Returns password reset success message.
     *
     * @return password reset success message.
     */
    String getPasswordResetMessage();

    /**
     * Returns root suffix.
     *
     * @return root suffix.
     */
    String getRootSuffix();

    /**
     * Returns <code>true</code> if warning message is enabled.
     *
     * @return <code>true</code> if warning message is enabled.
     */
    boolean warningEnabled();

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     */
    void debugWarning(String message);

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     * @param e <code>printStackTrace</code> will be invoked to print
     *        the stack trace.
     */
    void debugWarning(String message, Exception e);

    /**
     * Returns <code>true</code> if message debugging is enabled.
     *
     * @see com.iplanet.am.util.Debug#messageEnabled
     * @return <code>true</code> if message debugging is enabled
     *        <code>false</code> if message debugging is disabled
     */
    boolean messageEnabled();

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     */
    void debugMessage(String message);

    /**
     * Prints a message to the console debug file. The message will only be
     * written if the the debug level is set greater than Error.
     *
     * @param message to be printed. If message is null it is ignored.
     * @param e <code>printStackTrace</code> will be invoked to print
     *                  the stack trace.
     */
    void debugMessage(String message, Exception e);

    /**
     * Returns information message.
     *
     * @return information message
     */
    String getInformationMessage();

    /**
     * Returns localized string.
     *
     * @param key Resource string key.
     * @return localized string.
     */
    String getLocalizedString(String key);

    /**
     * Return a localized error message from an exception. If the exception
     * is of type <code>AMException</code> the error code and any possible
 * arguments will be extracted from the exception and the message will be
     * generated from the code and arguments. All other exception types will
     * return the message from <code>Exception.getMessage</code>.
     *
     * @param ex The exception.
     * @return error message localized to users locale.
     */
    String getErrorString(Exception ex);

    /**
     * Returns <code>true</code> if the user is locked out from resetting 
     * password.
     *
     * @param userDN user distinguished name.
     * @param orgDN organization distinguished name.
     * @return <code>true</code> if the user is locked out
     */
    boolean isUserLockout(String userDN, String orgDN);

    /**
     * Sets the password reset enabled flag depending what is set in the 
     * password service for a given organization distinguished name.
     *
     * @param orgDN Organization distinguished name
     */
    void readPWResetProfile(String orgDN);

    /**
     * Returns label for SUN logo.
     *
     * @return label for SUN logo.
     */
    String getSunLogoLabel();

    /**
     * Returns label for product logo.
     *
     * @return label for product logo.
     */
    String getProductLabel();

    /**
     * Returns label for Java logo.
     *
     * @return label for Java logo.
     */
    String getJavaLogoLabel();

    /**
     * Sets the user locale.
     *
     * @param localeString localeString.
     */
    void setUserLocale(String localeString);

    /**
     * Returns the name of the mail attribute.
     *
     * @param realm The realm the user belongs to.
     * @return The name of the mail attribute in the provided realm.
     */
    public String getMailAttribute(String realm) throws SSOException;
}
