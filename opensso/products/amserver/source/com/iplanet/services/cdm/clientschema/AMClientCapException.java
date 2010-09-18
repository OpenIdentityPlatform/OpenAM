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
 * $Id: AMClientCapException.java,v 1.3 2008/06/25 05:41:33 qcheng Exp $
 *
 */

package com.iplanet.services.cdm.clientschema;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.locale.L10NMessage;
import com.sun.identity.shared.debug.Debug;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * AMClientCapException: (critical) Thrown to indicate that the Attribute
 * Abstraction layer could not establish a connection with the backend service
 * provider (DSAME) OR the abstraction layer could not instantiate the actual
 * implementation objects.
 */
public class AMClientCapException extends Exception implements L10NMessage {
    private static AMResourceBundleCache amCache = AMResourceBundleCache
            .getInstance();

    private static Debug debug = Debug.getInstance("amClientSchema");

    private String bundleName = null;

    private String errorCode = null;

    private Object[] args = null;

    private String message = null;

    /**
     * Construct an exception with the specified msg
     * 
     * @param msg
     *            message describing the error
     */
    public AMClientCapException(String msg) {
        super(msg);
        message = msg;
    }

    /**
     * Construct an exception with the specified msg
     * 
     * @param rbName
     *            is the Resource Bundle name
     * @param eCode
     *            is the Error Code
     * @param a
     *            is the message arguments array
     */
    public AMClientCapException(String rbName, String eCode, Object[] a) {
        bundleName = rbName;
        errorCode = eCode;
        args = a;
        message = getL10NMessage(java.util.Locale.ENGLISH);
    }

    /**
     * Returns the error code
     * 
     * @return a String
     */
    public String getErrorCode() {
        return (errorCode);
    }

    /**
     * Returns the resource bundle name
     * 
     * @return a String
     */
    public String getResourceBundleName() {
        return (bundleName);
    }

    /**
     * Returns the message arguments array
     * 
     * @return an Array of message arguments.
     */
    public Object[] getMessageArgs() {
        return (args);
    }

    /**
     * Returns the localized message for a given locale
     * 
     * @param locale
     *            is the Locale
     * @return a String which is the localized message
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
     * Returns a message
     * 
     * @return a String
     */
    public String getMessage() {
        return message;
    }

}
