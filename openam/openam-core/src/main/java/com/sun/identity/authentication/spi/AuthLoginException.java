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
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.security.auth.login.LoginException;

/**
 * This class is for handling message localization in LoginException.
 *
 * @supported.api
 */
public class AuthLoginException extends LoginException implements L10NMessage {

    private static AMResourceBundleCache amCache = AMResourceBundleCache.getInstance();

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
        super(message);
        initCause(nestedException);
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
        initCause(nestedException);
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
        super(message);
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
        initCause(nestedException);
        _bundleName = rbName;
        _errorCode = errorCode;
        _args = args;

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
    public String getL10NMessage(Locale locale) {
        String result = super.getMessage();

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
        Throwable nestedException = getCause();
        if (nestedException != null) {
            if (nestedException instanceof L10NMessage) {
                L10NMessage lex = (L10NMessage) nestedException;
                chainedMessage = lex.getL10NMessage(locale);
            } else {
                chainedMessage = nestedException.getMessage();
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
        return getL10NMessage(Locale.ENGLISH);
    }
}
