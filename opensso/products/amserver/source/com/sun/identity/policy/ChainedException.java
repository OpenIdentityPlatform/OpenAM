/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ChainedException.java,v 1.3 2008/06/25 05:43:43 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy;

import java.io.*;

import java.util.ResourceBundle;
//import java.util.Locale;

import java.text.MessageFormat;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.L10NMessage;

/** 
 * Base class for supporting chaining of exceptions.
 */
class ChainedException extends Exception implements L10NMessage {

    private String _message;
    private Throwable _nestedException;

    private static AMResourceBundleCache amCache = 
    AMResourceBundleCache.getInstance();
    private String _bundleName ;
    private String _errorCode  ;
    private Object[] _args     ;
    private ResourceBundle _bundle;

    /**
     * Constructor
     * @param message message of this exception
     * @param nestedException Exception caught by the code block throwing 
     * this exception
     */
    public ChainedException(String message, Throwable nestedException) {
        _message = message;
        _nestedException = nestedException;
    }

    /**
     * Constructor
     * @param nestedException Exception caught by the code block throwing 
     * this exception
     */
    public ChainedException(Throwable nestedException) {
        _nestedException = nestedException;
        if (_nestedException instanceof L10NMessage) {
            L10NMessage lex = (L10NMessage) _nestedException;
            _message = lex.getMessage();
            _bundleName = lex.getResourceBundleName();
            _errorCode = lex.getErrorCode();
            _args = lex.getMessageArgs();
        }
    }

    /**
     * Constructor
     * @param message message of this exception
     */
    public ChainedException(String message) {
        _message = message;
    }

    /**
     * Constructor
     * Constructs an instance of <code> ChainedException </code> to pass the 
     * localized error message
     * At this level, the locale of the caller is not known and it is
     * not possible to throw localized error message at this level.
     * Instead this constructor provides Resource Bundle name and errorCode
     * for correctly locating the error messsage. The default getMessage() 
     * will always return English messages only. This is consistent with
     * current JRE
     * @param rbName - ResourceBundle Name to be used for getting 
     * localized error message.
     * @param errorCode - Key to resource bundle. You can use 
     * ResourceBundle rb = ResourceBundle.getBundle (rbName,locale);
     * String localizedStr = rb.getString(errorCode)
     * @param args - arguments to message. If it is not present pass them
     * as null 
     * @param nestedException - The root cause of this error
     */
    public ChainedException(
        String rbName,
        String errorCode,
        Object[] args,Throwable nestedException
    ) {
        _bundleName = rbName;
        _errorCode = errorCode;
        _args = args;
        _nestedException = nestedException;
        _message = getCompleteL10NMessage (java.util.Locale.ENGLISH);
    }

    /**
     * @param locale <code>Locale</code> to be used.
     * @return String localized error message.
     * @see #ChainedException(String, String, Object[], Throwable)
     */
    public String getL10NMessage (java.util.Locale locale) {
        String result = _message;

        if (_errorCode != null && _bundleName != null && locale != null) {
            _bundle = amCache.getResBundle (_bundleName, locale);
            String mid  = _bundle.getString (_errorCode);
            if (_args == null || _args.length == 0) {
                result = mid;
            } else {
                result = MessageFormat.format (mid,_args);
            }
        }

        return result;
    }

    /**
     * @param locale <code>Locale</code> to be used.
     * @return String localized error message.
     * @see #ChainedException(String, String, Object[], Throwable)
     */
    public String getCompleteL10NMessage (java.util.Locale locale) {
        String result = _message;

        if (_errorCode != null && _bundleName != null && locale != null) {
            _bundle = amCache.getResBundle (_bundleName, locale);
            String mid  = _bundle.getString (_errorCode);
            if (_args == null || _args.length == 0) {
                result = mid;
            } else {
                result = MessageFormat.format (mid,_args);
            }
        }
        String chainedMessage = null;
        if ( _nestedException != null ) {
            if (_nestedException instanceof L10NMessage) {
                L10NMessage lex = (L10NMessage) _nestedException;
                chainedMessage = lex.getL10NMessage(locale);
            } else {
                chainedMessage = _nestedException.getMessage();
            }
        }
        if ( chainedMessage != null ) {
            if (result != null) {
                result = result + "\n" + chainedMessage;
            } else {
                result = chainedMessage;
            }
        }
        return result;
    }
    

    /**
     * @return ResourceBundle Name associated with this error message.
     * @see #ChainedException(String, String, Object[], Throwable)
     * @see #getL10NMessage(java.util.Locale) - to get localized error message.
     */
    public String getResourceBundleName() {
        return _bundleName;
    }
     
    /**
     * @return Error code  associated with this error message.
     * @see #ChainedException(String, String, Object[], Throwable)
     * @see #getL10NMessage(java.util.Locale) - to get localized error message.
     */
    public String getErrorCode() {
        return _errorCode;
    }
     
    /**
     * @return arguments for formatting   this error message.
     * You need to use <code>MessageFormat</code> class to format the message
     * It can be null.
     * @see #ChainedException(String, String, Object[], Throwable)
     * @see #getL10NMessage(java.util.Locale) - to get localized error message.
     */
    public Object[] getMessageArgs() {
        return _args;
    }

    /** Gets messages of the exceptions including  the chained exceptions
     *  @return messages of the exceptions including chained exceptions.
     *          The returned string is formed by concatenating messages of 
     *          all the exceptions, with a new line separator,  starting 
     *          from this exception, all the 
     *          way to the root exception, by following the chained 
     *          exceptions.
     */
    public String getMessage() {
        String message = _message;
        String chainedMessage = null;
        if ( _nestedException != null ) {
            chainedMessage = _nestedException.getMessage();
        }
        if ( chainedMessage != null ) {
            if ( message != null ) {
                message = _message + "\n" + chainedMessage;
            } else {
                message = chainedMessage;
            } 
        } else {
            message = _message;
        }
        return message;
    }

    /** Prints the stack trace of the root exception to standard error 
     *  stream. Also prints the messages of all the exceptions starting 
     *  from top exception to the root exception, at the top of stack trace
     */
    public void printStackTrace() {
        System.err.println(fetchStackTrace());
    }

    /** Prints the stack trace of the root exception to a PrintWriter
     *  Also prints the messages of all the exceptions starting from top 
     *  exception to the root exception, at the top of stack trace
     *
     * @param pw PrintWriter to which to print the stack trace
     */
    public void printStackTrace(PrintWriter pw) {
        pw.println(fetchStackTrace());
    }

    /** Prints the stack trace of the root exception to a PrintStream
     *  Also prints the messages of all the exceptions starting from top 
     *  exception to the root exception, at the top of stack trace
     *
     * @param ps PrintStream to which to print the stack trace
     */
    public void printStackTrace(PrintStream ps) {
        ps.println(fetchStackTrace());
    }

    private String getLocalMessage() {
        return _message;
    }

    private String fetchStackTrace() {
        /*
         * We get the stack trace for the root exception.
         * We prepend the class names and messages of all the exceptions 
         * starting from this exception to the root exception, as header.
         * We number the exceptions in the header.
         * TODO:
         * We would like to cross reference the numbers from the header,
         * on the stack trace lines, to show which line corresponds to
         * the point at which the respective exception is thrown.
         */    
        String stackString = null;
        StringBuilder messageBuffer = new StringBuilder();
        int exCount = 1;
        Throwable rootException = this;
        messageBuffer.append(rootException.getClass().getName()).
                append("(").append(exCount).append(")" + ":").
                append(((ChainedException) rootException).getLocalMessage()).append("\n");

        while ((rootException instanceof ChainedException) &&
            (((ChainedException)rootException)._nestedException != null)
        ) {
            rootException = ((ChainedException)rootException)._nestedException;
            exCount++;
            if ( rootException instanceof ChainedException ) {
                messageBuffer.append(rootException.getClass().getName()).
                        append("(").append(exCount).append(")" + ":").
                        append(((ChainedException) rootException).getLocalMessage()).append("\n");
            } else {
                messageBuffer.append(rootException.getClass().getName()).
                        append("(").append(exCount).append(")" + ":").
                        append(rootException.getMessage()).append("\n");
            }
        }

        if ( rootException == this ) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ((ChainedException)rootException).printExceptionStackTrace(pw);
            stackString =  sw.getBuffer().toString();
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

    public Throwable getNestedException () {
        return _nestedException;
    }
}
