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
 * $Id: IdRepoException.java,v 1.8 2009/11/19 18:18:47 bhavnab Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.idm;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.ldap.LDAPException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * The exception class whose instance is thrown if there is any error during the
 * operation of objects of the <code>com.sun.identity.sms</code> package. This
 * class maps the exception that occurred at a lower level to a high level
 * error. Using the exception status code <code>getExceptionCode()</code> the
 * errors are categorized as a <code>ABORT</code>, <code>RETRY</code>,
 * <code>CONFIG_PROBLEM</code> or <code>LDAP_OP_FAILED</code> (typically a
 * bug).
 *
 * @supported.all.api
 */
public class IdRepoException extends Exception implements L10NMessage {

    // Static variable
    private transient AMResourceBundleCache amCache = AMResourceBundleCache
            .getInstance();

    private transient Debug debug = AMIdentityRepository.debug;

    // Instance variables
    private String message;

    private String bundleName;

    private String errorCode;

    private Object[] args;

    private LDAPException rootCause = null;

    private String ldapErrCode = null;

    public IdRepoException() {
    }

    /**
     * @param msg
     *            The message provided by the object which is throwing the
     *            exception
     */
    public IdRepoException(String msg) {
        message = msg;
    }

    public IdRepoException(String msg, String errorCode) {
        message = msg;
        this.errorCode = errorCode;
    }

   /**
     * This constructor is used to pass the localized error message At this
     * level, the locale of the caller is not known and it is not possible to
     * throw localized error message at this level. Instead this constructor
     * provides Resource Bundle name ,error code and LDAP error code ( in case
     * of LDAP related exception for correctly locating the
     * error message. The default <code>getMessage()</code> will always return
     * English messages only. This is in consistent with current JRE.
     *
     * @param rbName
     *            Resource bundle Name to be used for getting localized error
     *            message.
     * @param errorCode
     *            Key to resource bundle. You can use <code>ResourceBundle rb =
     *        ResourceBunde.getBundle(rbName,locale);
     *        String localizedStr = rb.getString(errorCode)</code>.
     * @param ldapErrCode
     *            ldap error code
     * @param args
     *            arguments to message. If it is not present pass the as null.
     */
    public IdRepoException(String rbName, String errorCode,
        String ldapErrCode,Object[] args)
    {
        this.bundleName = rbName;
        this.errorCode = errorCode;
        this.ldapErrCode = ldapErrCode;
        this.args = args;
        this.message = getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * This constructor is used to pass the localized error message At this
     * level, the locale of the caller is not known and it is not possible to
     * throw localized error message at this level. Instead this constructor
     * provides Resource Bundle name and error code for correctly locating the
     * error message. The default <code>getMessage()</code> will always return
     * English messages only. This is in consistent with current JRE.
     * 
     * @param rbName
     *            Resource bundle Name to be used for getting localized error
     *            message.
     * @param errorCode
     *            Key to resource bundle. You can use <code>ResourceBundle rb =
     *        ResourceBunde.getBundle(rbName,locale);
     *        String localizedStr = rb.getString(errorCode)</code>.
     * @param args
     *            arguments to message. If it is not present pass the as null.
     */
    public IdRepoException(String rbName, String errorCode, Object[] args) {
        this.bundleName = rbName;
        this.errorCode = errorCode;
        this.args = args;
        this.message = getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * Returns a localized error message
     * 
     * @param locale
     *            Uses the locale object to create the appropriate localized
     *            error message
     * @return localized error message.
     * @see #IdRepoException(String, String, Object[])
     */
    public String getL10NMessage(java.util.Locale locale) {
        String result = errorCode;
        if (bundleName != null && locale != null) {
            ResourceBundle bundle = amCache.getResBundle(bundleName, locale);
            String mid = Locale.getString(bundle, errorCode, debug);
            if (args == null || args.length == 0) {
                result = mid;
            } else {
                result = MessageFormat.format(mid, args);
            }
        }
        return result;
    }

    /**
     * Returns <code>ResourceBundle</code> Name associated with this error
     * message.
     * 
     * @return <code>ResourceBundle</code> name associated with this error
     *         message.
     * @see #IdRepoException(String, String, Object[])
     */
    public String getResourceBundleName() {
        return bundleName;
    }

    /**
     * Returns error code associated with this error message.
     * 
     * @return Error code associated with this error message.
     * @see #IdRepoException(String, String, Object[])
     */
    public String getErrorCode() {
        return errorCode;
    }

     /**
      * Returns the LDAP error code associated with this error message.
      *
      * @return Error code associated with this error message and null if
      *      not caused by <code>LDAPException</code>.
      * @see #IdRepoException(String, String, Object[])
      */
     public String getLDAPErrorCode() {
         return ldapErrCode;
     }

     /**
      * Replace the LDAP error code associated with this error message.
      *
      * @see #IdRepoException(String, String, Object[])
      */
     public void setLDAPErrorCode(String errorCode) {
         ldapErrCode = errorCode;
     }

    /**
     * Returns arguments for formatting this error message.
     * 
     * @return arguments for formatting this error message. You need to use
     *         <code>MessageFormat</code> class to format the message It can
     *         be null.
     * @see #IdRepoException(String, String, Object[])
     */
    public Object[] getMessageArgs() {
        return args;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        String msg = message;
        if (msg != null && msg.length() > 0) {
            buf.append("Message:");
            buf.append(msg);
            buf.append("\n");
        }
        return buf.toString();
    }

    /**
     * Returns the error message of this exception.
     * 
     * @return String representing the error message
     */
    public String getMessage() {
        return message;
    }
}
