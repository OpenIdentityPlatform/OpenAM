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
 * $Id: AMException.java,v 1.7 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.sso.SSOToken;
import com.iplanet.ums.UMSException;
import com.sun.identity.shared.locale.L10NMessage;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import com.sun.identity.shared.ldap.LDAPException;

/**
 * The <code>AMException</code> is thrown whenever an error is is encountered
 * while performing an operation on the data store.
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public class AMException extends Exception implements L10NMessage {

    private static final long serialVersionUID = -660487903675407220L;

    private String localizedMsg = null;

    private String errorCode = null;

    private Object args[] = null;

    private LDAPException rootCause = null;

    private String ldapErrorMsg = null;

    private String ldapErrCode = null;

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     */
    public AMException(String msg, String errorCode) {
        super(msg);
        this.localizedMsg = msg;
        this.errorCode = errorCode;
    }

    /**
     * Convenience method (protected)
     */
    public AMException(SSOToken token, String errorCode) {
        this.localizedMsg = AMSDKBundle.getString(errorCode, AMCommonUtils
                .getUserLocale(token));
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message.
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param ue
     *            if the root cause is a <code>UMSException</code>.
     */
    public AMException(String msg, String errorCode, UMSException ue) {
        try {
            rootCause = (LDAPException) ue.getRootCause();
            ldapErrCode = Integer.toString(rootCause.getLDAPResultCode());
            ldapErrorMsg = AMSDKBundle.getString(ldapErrCode);
        } catch (Exception e) {
        }
        if (ldapErrorMsg != null) {
            localizedMsg = msg + "::" + ldapErrorMsg;
        } else {
            localizedMsg = msg;
        }
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param token
     *            a valid single sign on token of the user performing the
     *            operation.
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param ue
     *            if the root cause is a <code>UMSException</code>.
     */
    public AMException(SSOToken token, String errorCode, UMSException ue) {
        try {
            rootCause = (LDAPException) ue.getRootCause();
            ldapErrCode = Integer.toString(rootCause.getLDAPResultCode());
            ldapErrorMsg = AMSDKBundle.getString(ldapErrCode);
        } catch (Exception e) {
        }
        String message = AMSDKBundle.getString(errorCode, AMCommonUtils
                .getUserLocale(token));
        if (ldapErrorMsg != null) {
            localizedMsg = message + "::" + ldapErrorMsg;
        } else {
            localizedMsg = message;
        }
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message.
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param args
     *            arguments if the error message needs specific values to be
     *            set.
     */
    public AMException(String msg, String errorCode, Object[] args) {
        super(msg);
        this.localizedMsg = msg;
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * Constructs a new <code>AMException</code> with detailed message.
     * 
     * @param msg
     *            The detailed message
     * @param errorCode
     *            Matches the appropriate entry in
     *            <code>amProfile.properties</code>.
     * @param args
     *            if the error message needs specific values to be set.
     * @param ue
     *            if the root cause is a <code>UMSException</code>.
     */
    public AMException(String msg, String errorCode, Object args[],
            UMSException ue) {
        try {
            rootCause = (LDAPException) ue.getRootCause();
            ldapErrCode = Integer.toString(rootCause.getLDAPResultCode());
            ldapErrorMsg = AMSDKBundle.getString(ldapErrCode);
        } catch (Exception e) {
            // Ignore
        }
        if (ldapErrorMsg != null) {
            localizedMsg = msg + "::" + ldapErrorMsg;
        } else {
            localizedMsg = msg;
        }
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * Returns the error code. This error code can be used with the arguments to
     * construct a localized message.
     * 
     * @return the error code which can be used to map the message to a user
     *         specific locale.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the arguments corresponding to the error code.
     * 
     * @return the arguments corresponding to the error code or null if no
     *         arguments are need to construct the message.
     */
    public Object[] getMessageArgs() {
        return args;
    }

    /**
     * Overrides the default <code>getMessage()</code> method of super class
     * Exception.
     * 
     * @return The error message string.
     */
    public String getMessage() {
        return localizedMsg;
    }

    /**
     * Method to obtain the LDAP error code.
     * 
     * @return The error code, which can be used to map the message to a
     *         specific locale. Returns a null, if not an LDAP error.
     */
    public String getLDAPErrorCode() {
        return ldapErrCode;
    }

    /**
     * Returns the root <code>LDAPException</code> of this
     * <code>AMException</code>, if any.
     * 
     * @return The <code>LDAPException</code> that caused this
     *         <code>AMException</code>. If null, it means no root
     *         <code>LDAPException</code> has been set.
     */
    public LDAPException getLDAPException() {
        return rootCause;
    }

    /**
     * Returns localized error message.
     * 
     * @param locale
     *            locale of the error message.
     * @return Localized error message.
     */
    public String getL10NMessage(Locale locale) {
        String result = errorCode;
        if (locale != null) {
            ResourceBundle rb = AMSDKBundle.getBundleFromHash(locale);
            String mid = com.sun.identity.shared.locale.Locale.getString(
                rb, errorCode, AMCommonUtils.debug);
            result = ((args == null) || (args.length == 0)) ? mid
                    : MessageFormat.format(mid, args);
        }
        return result;
    }

    /**
     * Returns ResourceBundle Name associated with this exception
     * 
     * @return ResourceBundle Name associated with this exception.
     */
    public String getResourceBundleName() {
        return AMSDKBundle.BUNDLE_NAME;
    }
}
