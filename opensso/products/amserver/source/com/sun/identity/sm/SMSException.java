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
 * $Id: SMSException.java,v 1.7 2009/01/28 05:35:03 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.sun.identity.authentication.internal.InvalidAuthContextException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.shared.locale.Locale;
import com.iplanet.am.sdk.AMException;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.event.EventException;
import com.iplanet.services.util.XMLException;
import com.iplanet.sso.SSOException;
import com.iplanet.ums.IUMSConstants;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import com.sun.identity.shared.ldap.LDAPException;

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
public class SMSException extends Exception implements L10NMessage {

    // Static variable
    transient AMResourceBundleCache amCache = AMResourceBundleCache
            .getInstance();

    transient Debug debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);

    // Instance variables
    private int exceptionStatus = STATUS_NONE;

    private Throwable rootCause;

    private String message;

    private String bundleName = IUMSConstants.UMS_BUNDLE_NAME;

    private String errorCode;

    private Object[] args;

    /**
     * Default constructor for <code> SMSException </code>
     */
    public SMSException() {
        super();
        exceptionStatus = STATUS_NONE;
    }

    /**
     * @param status
     *            The exception status code.
     * @param errorCode
     *            Key to resource bundle.
     */
    public SMSException(int status, String errorCode) {
        super();
        exceptionStatus = status;
        this.errorCode = errorCode;
        this.message = getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * @param status
     *            The Exception status code.
     * @param exMessage
     *            The message provided by the object which is throwing the
     *            exception
     * @param errorCode
     *            Key to resource bundle.
     */
    public SMSException(int status, String exMessage, String errorCode) {
        exceptionStatus = status;
        this.errorCode = errorCode;
        this.message = exMessage + ": " +
            getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * @param msg
     *            The message provided by the object which is throwing the
     *            exception
     */
    public SMSException(String msg) {
        exceptionStatus = STATUS_NONE;
        this.message = msg;
    }

    /**
     * @param msg
     *            The message provided by the object which is throwing the
     *            exception
     * @param errorCode
     *            Key to resource bundle.
     */
    public SMSException(String msg, String errorCode) {
        exceptionStatus = STATUS_NONE;
        this.errorCode = errorCode;
        this.message = msg + ": " + getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * Constructs an <code>SMSException</code>.
     * 
     * @param t
     *            The <code>Throwable</code> object provided by the object
     *            which is throwing the exception
     * @param errorCode
     *            Key to resource bundle.
     */
    public SMSException(Throwable t, String errorCode) {
        // super(t); (can be used with JDK 1.4 and higher)
        rootCause = t;
        this.errorCode = errorCode;
        this.message = getL10NMessage(java.util.Locale.ENGLISH);
        exceptionMapper();
    }

    /**
     * Constructs an <code>SMSException</code>.
     * 
     * @param message
     *            exception message.
     * @param t
     *            The <code>Throwable</code> object provided by the object
     *            which is throwing the exception.
     * @param errorCode
     *            Key to resource bundle.
     */
    public SMSException(String message, Throwable t, String errorCode) {
        // super(message, t); (can be used with JDK 1.4 and higher)
        rootCause = t;
        this.errorCode = errorCode;
        this.message = message + ": " +
            getL10NMessage(java.util.Locale.ENGLISH);
        exceptionMapper();
    }

    /**
     * Constructs an <code>SMSException</code>.
     * 
     * @param rbName
     *            Resource bundle Name to be used for getting localized error
     *            message.
     * @param message
     *            exception message.
     * @param t
     *            The <code>Throwable</code> object provided by the object
     *            which is throwing the exception.
     * @param errorCode
     *            Key to resource bundle.
     */
    public SMSException(String rbName, String message, Throwable t,
            String errorCode) {
        // super(message, t); (can be used with JDK 1.4 and higher)
        rootCause = t;
        this.errorCode = errorCode;
        this.bundleName = rbName;
        this.message = message + ": " +
            getL10NMessage(java.util.Locale.ENGLISH);
        if (rootCause != null && !(rootCause instanceof AMException)) {
            exceptionMapper();
        }
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
    public SMSException(String rbName, String errorCode, Object[] args) {
        exceptionStatus = STATUS_NONE;
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
     * @see #SMSException(String, String, Object[])
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
     * @see #SMSException(String, String, Object[])
     */
    public String getResourceBundleName() {
        return bundleName;
    }

    /**
     * Returns error code associated with this error message.
     * 
     * @return Error code associated with this error message.
     * @see #SMSException(String, String, Object[])
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns arguments for formatting this error message.
     * 
     * @return arguments for formatting this error message. You need to use
     *         <code>MessageFormat</code> class to format the message It can
     *         be null.
     * @see #SMSException(String, String, Object[])
     */
    public Object[] getMessageArgs() {
        return args;
    }

    /**
     * Returns the status code for this exception.
     * 
     * @return Integer representing the exception status code
     */
    public int getExceptionCode() {
        return exceptionStatus;
    }

    /**
     * The this package can set the exception code.
     * 
     * @param status
     *            The exception status code.
     */
    void setExceptionCode(int status) {
        exceptionStatus = status;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (exceptionStatus != -1) {
            buf.append("SMSException Exception Code:");
            buf.append(exceptionStatus);
            buf.append('\n');
        }
        String msg = message;
        if (msg != null && msg.length() > 0) {
            buf.append("Message:");
            buf.append(msg);
            buf.append("\n");
        }

        if (rootCause != null) {
            buf.append("--------------------------------------------------\n");
            buf.append("The lower level exception message\n");
            buf.append(rootCause.getMessage());
            buf.append('\n');
            buf.append("The lower level exception:\n");
            StringWriter sw = new StringWriter(100);
            rootCause.printStackTrace(new PrintWriter(sw));
            buf.append(sw.toString());
            buf.append('\n');
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

    private String getString(String msgID) {
        errorCode = msgID;
        ResourceBundle bundle = null;
        if (bundleName != null) {
            bundle = amCache.getResBundle(bundleName, java.util.Locale.ENGLISH);
        }
        return (Locale.getString(bundle, msgID, debug));
    }

    private void exceptionMapper() {
        if (rootCause == null) {
            return;
        }
        if (rootCause instanceof LDAPException) {
            message = mapLDAPException();
        } else if (rootCause instanceof LDAPServiceException) {
            // do nothing
        } else if (rootCause instanceof EventException) {
            exceptionStatus = STATUS_ABORT;
            message = getString(IUMSConstants.SMS_EVENT_NOTIFICATION_FAILED);
        } else if (rootCause instanceof XMLException) {
            exceptionStatus = STATUS_ABORT;
            message = getString(IUMSConstants.SMS_XML_PARSER_EXCEPTION);
        } else if (rootCause instanceof InvalidAuthContextException) {
            message = getString(IUMSConstants.SMS_AUTHENTICATION_ERROR);
            exceptionStatus = STATUS_ABORT;
        } else if (rootCause instanceof SSOException) {
            message = getString(IUMSConstants.SMS_AUTHENTICATION_ERROR);
            exceptionStatus = STATUS_ABORT;
        } else {
            message = getString(IUMSConstants.SMS_UNKNOWN_EXCEPTION_OCCURRED);
            exceptionStatus = STATUS_UNKNOWN_EXCEPTION;
        }
    }

    private String mapLDAPException() {
        int resultCode = ((LDAPException) rootCause).getLDAPResultCode();

        String message = null;

        switch (resultCode) {
        // ////////////////////////////////
        // Errors that need to be handled
        // ////////////////////////////////

        // Helpless errors
        // All errors are helpless situations
        // but some are more helpless than the others.
        // These errors are either problems in connection
        // or configuration. So, some can be retired and
        // some are already busted.
        case LDAPException.SERVER_DOWN:
        case LDAPException.OTHER:
            message = getString(IUMSConstants.SMS_SERVER_DOWN);
            exceptionStatus = STATUS_RETRY;
            break;
        case LDAPException.LDAP_NOT_SUPPORTED:
            message = getString(IUMSConstants.SMS_LDAP_NOT_SUPPORTED);
            exceptionStatus = STATUS_ABORT;
            break;
        case LDAPException.BUSY:
            message = getString(IUMSConstants.SMS_LDAP_SERVER_BUSY);
            exceptionStatus = STATUS_RETRY;
            break;

        case LDAPException.INVALID_CREDENTIALS:
            message = getString("INVALID_CREDENTIALS");
            exceptionStatus = STATUS_CONFIG_PROBLEM;
            break;

        // Application must show exactly what is happening
        case LDAPException.NO_SUCH_OBJECT:
            message = getString(IUMSConstants.SMS_NO_SUCH_OBJECT);
            exceptionStatus = STATUS_LDAP_OP_FAILED;
            break;

        case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
            message = getString(IUMSConstants.SMS_INSUFFICIENT_ACCESS_RIGHTS);
            exceptionStatus = STATUS_NO_PERMISSION;
            break;

        case LDAPException.ADMIN_LIMIT_EXCEEDED:
            message = getString(IUMSConstants.SMS_ADMIN_LIMIT_EXCEEDED);
            exceptionStatus = STATUS_ABORT;
            break;

        case LDAPException.TIME_LIMIT_EXCEEDED:
            message = getString(IUMSConstants.SMS_TIME_LIMIT_EXCEEDED);
            exceptionStatus = STATUS_ABORT;
            break;

        case LDAPException.REFERRAL:
            message = getString(IUMSConstants.SMS_LDAP_REFERRAL_EXCEPTION);
            exceptionStatus = STATUS_CONFIG_PROBLEM;
            break;

        // We screwed up with something
        case LDAPException.OBJECT_CLASS_VIOLATION:
        case LDAPException.NAMING_VIOLATION:
        case LDAPException.CONSTRAINT_VIOLATION:
        case LDAPException.INVALID_DN_SYNTAX:
        case LDAPException.ENTRY_ALREADY_EXISTS:
        case LDAPException.ATTRIBUTE_OR_VALUE_EXISTS:
        case LDAPException.PROTOCOL_ERROR:
        case LDAPException.UNDEFINED_ATTRIBUTE_TYPE:
            SMSEntry.debug.error(rootCause.toString());
            message = getString(IUMSConstants.SMS_LDAP_OPERATION_FAILED);
            exceptionStatus = STATUS_LDAP_OP_FAILED;
            break;

        // Exception code that means logical operation.
        case LDAPException.COMPARE_TRUE:
        case LDAPException.COMPARE_FALSE:
        case LDAPException.LDAP_PARTIAL_RESULTS:
            exceptionStatus = STATUS_QUO_ANTE;
            break;

        default:
            message = getString(IUMSConstants.SMS_UNEXPECTED_LDAP_EXCEPTION);
            exceptionStatus = STATUS_UNKNOWN_EXCEPTION;
        }
        return message;
    }

    // Error codes
    /** No status code is set */
    public static int STATUS_NONE = -1;

    /** Retry connection to data store */
    public static int STATUS_RETRY = 0;

    /** Repeated retry to data store failed */
    public static int STATUS_REPEATEDLY_FAILED = 0;

    /** Status to abort operation */
    public static int STATUS_ABORT = 1;

    /**
     * If root LDAP cause is <code>LDAP_PARTIAL_RESULTS </code> then this
     * status is set
     */
    public static int STATUS_QUO_ANTE = 2;

    /**
     * If root LDAP cause is an LDAP exception with one of the following error
     * codes then this status is set.
     * <p>
     * 
     * <PRE>
     * 
     * NO_SUCH_OBJECT OBJECT_CLASS_VIOLATION NAMING_VIOLATION
     * CONSTRAINT_VIOLATION INVALID_DN_SYNTAX ENTRY_ALREADY_EXISTS
     * ATTRIBUTE_OR_VALUE_EXISTS PROTOCOL_ERROR UNDEFINED_ATTRIBUTE_TYPE
     * 
     * </PRE>
     */

    public static int STATUS_LDAP_OP_FAILED = 3;

    /**
     * If the root LDAP exception is <code> INVALID_CREDENTIALS </code> or
     * <code> REFERRAL </code> then this status is set
     */
    public static int STATUS_CONFIG_PROBLEM = 4;

    /** If root cause is other than any of those listed in other status codes */
    public static int STATUS_UNKNOWN_EXCEPTION = 5;

    /** If the root LDAP cause is <code> INSUFFICIENT_ACCESS_RIGHTS </code> */
    public static int STATUS_NO_PERMISSION = 8;

    /** the operation is not allowed. */
    public static int STATUS_NOT_ALLOW = 9;

}
