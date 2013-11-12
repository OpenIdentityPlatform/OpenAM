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
 * $Id: L10NMessageImpl.java,v 1.2 2008/06/25 05:53:04 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.shared.locale;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Convenient implementation of <code>L10NMessage</code> Interface. Extends
 * <code>java.lang.Exception</code> an provides mechanism to provide resource 
 * bundle for error messages
 */
public class L10NMessageImpl 
    extends Exception 
    implements L10NMessage
{
    private static final long serialVersionUID = -4690604178832156822L;
    private AMResourceBundleCache amCache = AMResourceBundleCache.getInstance();
    private String message;
    private String bundleName;
    private String errorCode;
    private Object[] args;
    private ResourceBundle bundle;

    public L10NMessageImpl(String msg) {
        super(msg);
    }

    /**
     * This constructor is used to pass the localized error message At this
     * level, the locale of the caller is not known and it is not possible to
     * throw localized error message at this level. Instead this constructor
     * provides Resource Bundle name and errorCode for correctly locating the
     * error messsage. The default getMessage() will always return English
     * messages only. This is in consistent with current JRE
     * 
     * @param rbName ResourceBundle Name to be used for getting localized error
     *        message.
     * @param errorCode Key to resource bundle. You can use 
     *        <pre>
     *        ResourceBundle rb = ResourceBunde.getBundle(rbName, locale);
     *        String localizedStr = rb.getString(errorCode);
     *        </pre>
     * @param args Arguments to message. If it is not present pass the as null.
     */
    public L10NMessageImpl(String rbName, String errorCode, Object args[]) {
        this.bundleName = rbName;
        this.errorCode = errorCode;
        this.args = args;
        this.message = getL10NMessage(Locale.ENGLISH);
    }

    /**
     * This constructor is used to extract localized error message from
     * throwable.
     * 
     * @param ex if the exception message is instance of L10NMessage, the
     *        bundleName,errorCode and args are extracted from throwable
     */
    public L10NMessageImpl(Throwable ex) {
        super(ex);
        this.message = ex.getMessage();
        if (ex instanceof L10NMessage) {
            L10NMessage lex = (L10NMessage) ex;
            this.bundleName = lex.getResourceBundleName();
            this.errorCode = lex.getErrorCode();
            this.args = lex.getMessageArgs();
        }

    }

    /**
     * Returns localized error message.
     * 
     * @param locale Locale of the error message.
     * @return localized error message.
     * @see #L10NMessageImpl(String, String, Object[])
     */
    public String getL10NMessage(Locale locale) {
        if (errorCode == null) {
            return getMessage();
        }

        String result = message;
        if (bundleName != null && locale != null) {
            bundle = amCache.getResBundle(bundleName, locale);
            String mid = bundle.getString(errorCode);
            if (args == null || args.length == 0) {
                result = mid;
            } else {
                result = MessageFormat.format(mid, args);
            }
        }
        return result;
    }

    /**
     * Returns resource bundle name associated with this error message.
     * 
     * @return resource bundle name associated with this error message.
     * @see #L10NMessageImpl(String, String, Object[])
     */
    public String getResourceBundleName() {
        return bundleName;
    }

    /**
     * Returns error code associated with this error message.
     * 
     * @return error code associated with this error message.
     * @see #L10NMessageImpl(String, String, Object[])
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns arguments for formatting this error message. You need to use
     * <code>MessageFormat</code> class to format the message It can be null.
     * 
     * @return arguments for formatting this error message.
     * @see #L10NMessageImpl(String, String, Object[])
     */
    public Object[] getMessageArgs() {
        return args;
    }

    /**
     * Returns message of the exception.
     *
     * @return Message of the exception.
     */
    public String getMessage() {
        if (message != null) {
            // message is set only if l10n resource bundle is specified
            return message;
        }
        return super.getMessage();
    }
}
