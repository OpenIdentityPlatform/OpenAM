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
 * $Id: AuthLoginException.java,v 1.4 2008/06/25 05:42:06 qcheng Exp $
 *
 */

/*
 * Portions Copyright 2011-2013 ForgeRock AS
 */
package com.sun.identity.authentication.spi;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.L10NMessage;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import javax.security.auth.login.LoginException;

/**
 * This class is for handling message localization in LoginException.
 *
 * @supported.api
 */
public class AuthLoginException extends LoginException implements L10NMessage {

    private String _message;

    private Throwable _nestedException;

    private static AMResourceBundleCache amCache = AMResourceBundleCache
            .getInstance();

    private String _bundleName = null;

    private String _errorCode = null;

    private Object[] _args = null;

    private ResourceBundle _bundle = null;

    /**
     * Constructs an exception with given message and the nested exception.
     * 
     * @param message
     *            message of this exception
     * @param nestedException
     *            Exception caught by the code block throwing this exception
     */
    public AuthLoginException(String message, Throwable nestedException) {
        _message = message;
        _nestedException = nestedException;
    }

    /**
     * Constructs an <code>AuthLoginException</code> with given
     * <code>Throwable</code>.
     * 
     * @param nestedException
     *            Exception nested in the new exception.
     *
     * @supported.api
     */
    public AuthLoginException(Throwable nestedException) {
        _nestedException = nestedException;
        if (nestedException instanceof L10NMessage) {
            _errorCode = ((L10NMessage) nestedException).getErrorCode();
        }
    }

    /**
     * Constructs a new <code>AuthLoginException</code> with the given
     * message.
     * 
     * @param message
     *            message for this exception. This message can be later
     *            retrieved by <code>getMessage()</code> method.
     *
     * @supported.api
     */
    public AuthLoginException(String message) {
        _message = message;
    }

    /**
     * Constructs an instance of <code> AuthLoginException </code> to pass the
     * localized error message At this level, the locale of the caller is not
     * known and it is not possible to throw localized error message at this
     * level. Instead this constructor provides Resource Bundle name and
     * <code>errorCode</code> for correctly locating the error message. The
     * default <code>getMessage()</code> will always return English messages
     * only. This is consistent with current JRE.
     * 
     * @param rbName
     *            Resource Bundle Name to be used for getting localized error
     *            message.
     * @param errorCode
     *            Key to resource bundle. You can use
     * 
     * <pre>
     *  ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     *  String localizedStr = rb.getString(errorCode)
     * </pre>
     * 
     * @param args
     *            arguments to message. If it is not present pass them as null
     * @param nestedException
     *            The nested <code>Throwable</code>.
     *
     * @supported.api
     */
    public AuthLoginException(String rbName, String errorCode, Object[] args,
            Throwable nestedException) {

        _bundleName = rbName;
        _errorCode = errorCode;
        _args = args;
        _nestedException = nestedException;

    }

    /**
     * Constructs a new <code>AuthLoginException</code> without a nested
     * <code>Throwable</code>.
     * 
     * @param rbName
     *            Resource Bundle Name to be used for getting localized error
     *            message.
     * @param errorCode
     *            Key to resource bundle. You can use
     * 
     * <pre>
     *  ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     *  String localizedStr = rb.getString(errorCode)
     * </pre>
     * 
     * @param args
     *            arguments to message. If it is not present pass them as null
     *
     * @supported.api
     */
    public AuthLoginException(String rbName, String errorCode, Object[] args) {
        this(rbName, errorCode, args, null);
    }

    /**
     * Returns the localized message of the given locale.
     * 
     * @param locale
     *            the locale in which the message will be returned.
     * @return String localized error message.
     *
     * @supported.api
     */
    public String getL10NMessage(java.util.Locale locale) {
        String result = _message;

        if (_bundleName != null && locale != null && _errorCode != null) {
            _bundle = amCache.getResBundle(_bundleName, locale);
            String mid = _bundle.getString(_errorCode);
            if (_args == null || _args.length == 0) {
                result = mid;
            } else {
                result = MessageFormat.format(mid, _args);
            }
        }
        String chainedMessage = null;
        if (_nestedException != null) {
            if (_nestedException instanceof L10NMessage) {
                L10NMessage lex = (L10NMessage) _nestedException;
                chainedMessage = lex.getL10NMessage(locale);
            } else {
                chainedMessage = _nestedException.getMessage();
            }
        }
        if (result == null) {
            result = chainedMessage;
        } else if (chainedMessage != null) {
            result = result + "\n" + chainedMessage;
        }
        return result;
    }

    /**
     * Returns the resource bundle name.
     * 
     * @return Resource Bundle Name associated with this error message.
     * @see #getL10NMessage(java.util.Locale).
     *
     * @supported.api
     */
    public String getResourceBundleName() {
        return _bundleName;
    }

    /**
     * Returns the error code.
     * 
     * @return Error code associated with this error message.
     *
     * @supported.api
     */
    public String getErrorCode() {
        return _errorCode;
    }

    /**
     * Returns the error message arguments.
     * 
     * @return arguments for formatting this error message. You need to use
     *         <code>MessageFormat</code> class to format the message. It can
     *         be null.
     *
     * @supported.api
     */
    public Object[] getMessageArgs() {
        return _args;
    }

    /**
     * Gets messages of the exceptions including the nested exceptions.
     * 
     * @return messages of the exceptions including nested exceptions. The
     *         returned string is formed by concatenating messages of all the
     *         exceptions, with a new line separator, starting from this
     *         exception, all the way to the root exception, by following the
     *         nested exceptions. The message returned is always in English
     *         locale. To get localized message, use the getL10NMessage(Locale)
     *         method.
     *
     * @supported.api
     */
    public String getMessage() {
        return getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * Prints the stack trace of the root exception to standard error stream.
     * Also prints the messages of all the exceptions starting from top
     * exception to the root exception, at the top of stack trace
     *
     * @supported.api
     */
    public void printStackTrace() {
        System.err.println(fetchStackTrace());
    }

    /**
     * Prints the stack trace of the root exception to a
     * <code>PrintWriter</code>. Also prints the messages of all the
     * exceptions starting from top exception to the root exception, at the top
     * of stack trace
     * 
     * @param pw
     *            <code>PrintWriter</code> to which to print the stack trace.
     *
     * @supported.api
     */
    public void printStackTrace(PrintWriter pw) {
        pw.println(fetchStackTrace());
    }

    /**
     * Prints the stack trace of the root exception to a
     * <code>PrintStream</code> Also prints the messages of all the exceptions
     * starting from top exception to the root exception, at the top of stack
     * trace
     * 
     * @param ps
     *            <code>PrintStream</code> to which to print the stack trace
     *
     * @supported.api
     */
    public void printStackTrace(PrintStream ps) {
        ps.println(fetchStackTrace());
    }

    private String getLocalMessage(java.util.Locale locale) {
        String result = _message;

        if (_bundleName != null && locale != null) {
            _bundle = amCache.getResBundle(_bundleName, locale);
            String mid = _bundle.getString(_errorCode);
            if (_args == null || _args.length == 0) {
                result = mid;
            } else {
                result = MessageFormat.format(mid, _args);
            }
        }
        return result;
    }

    private String getLocalMessage() {
        return getLocalMessage(java.util.Locale.ENGLISH);
    }

    private String fetchStackTrace() {

        /*
         * We get the stack trace for the root exception. We prepend the class
         * names and messages of all the exceptions starting from this exception
         * to the root exception, as header. We number the exceptions in the
         * header. TODO: We would like to cross reference the numbers from the
         * header, on the stack trace lines, to show which line corresponds to
         * the point at which the respective exception is thrown.
         */
        String stackString = null;
        StringBuilder messageBuffer = new StringBuilder();
        int exCount = 1;
        Throwable rootException = this;
        messageBuffer.append(rootException.getClass().getName()).
                append("(").append(exCount).append(")").
                append(":").append(((AuthLoginException) rootException).getLocalMessage())
                .append("\n");
        while ((rootException instanceof AuthLoginException)
            && (((AuthLoginException) rootException)._nestedException != null)) 
        {
            rootException = 
                ((AuthLoginException) rootException)._nestedException;
            exCount++;
            if (rootException instanceof AuthLoginException) {
                messageBuffer.append(rootException.getClass().getName()).
                        append("(").append(exCount).append(")").
                        append(":").append(((AuthLoginException) rootException).getLocalMessage()).
                        append("\n");
            } else {
                messageBuffer.append(rootException.getClass().getName()).
                        append("(").append(exCount).append(")").
                        append(":").append(rootException.getMessage()).
                        append("\n");
            }
        }

        if (rootException == this) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ((AuthLoginException) rootException).printExceptionStackTrace(pw);
            stackString = sw.getBuffer().toString();
        } else {
            StringWriter rootStackWriter = new StringWriter();
            StringWriter thisStackWriter = new StringWriter();
            rootException.printStackTrace(new PrintWriter(rootStackWriter));
            this.printExceptionStackTrace(new PrintWriter(thisStackWriter));
            StringBuffer rootStackTrace = rootStackWriter.getBuffer();
            rootStackTrace.insert(0, messageBuffer.toString());
            stackString = rootStackTrace.toString();
        }
        return stackString;
    }

    private void printExceptionStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
    }
}
