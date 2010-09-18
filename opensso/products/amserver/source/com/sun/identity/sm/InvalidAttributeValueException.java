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
 * $Id: InvalidAttributeValueException.java,v 1.6 2008/06/25 05:44:04 qcheng Exp $
 *
 */

package com.sun.identity.sm;

import com.sun.identity.shared.locale.Locale;
import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * @see java.lang.Exception
 * @see java.lang.Throwable
 *
 * @supported.all.api
 */
public class InvalidAttributeValueException extends SMSException {
    private String resourceBundleName;

    private String rbName;

    private String attributeI18nKey;

    private String errCode;

    /**
     * Constructs an <code>InvalidAttributeValueException</code> with no
     * specified detail message.
     */
    public InvalidAttributeValueException() {
        super();
    }

    /**
     * Constructs an <code>InvalidAttributeValueException</code> with the
     * specified detail message.
     * 
     * @param s
     *            the detail message.
     */
    public InvalidAttributeValueException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>InvalidAttributeValueException</code> with the
     * specified error code. It can be used to pass localized error message.
     * 
     * @param rbName
     *            Resource Bundle name where localized error message is located.
     * @param errorCode
     *            error code or message id to be used for
     *            <code>ResourceBundle.getString()</code> to locate error
     *            message
     * @param args
     *            any arguments to be used for error message formatting
     *            <code>getMessage()</code> will construct error message using
     *            English resource bundle.
     */
    public InvalidAttributeValueException(String rbName, String errorCode,
            Object[] args) {
        super(rbName, errorCode, args);
        resourceBundleName = rbName;
        errCode = errorCode;

        if (args.length > 2) {
            this.rbName = (String) args[1];
            this.attributeI18nKey = (String) args[2];
        }
    }

    /**
     * Returns a localized error message
     * 
     * @param locale
     *            Uses the locale object to create the appropriate localized
     *            error message
     * @return localized error message.
     */
    public String getL10NMessage(java.util.Locale locale) {
        String message = errCode;

        if ((resourceBundleName == null) || (locale == null)
                || (attributeI18nKey == null) || (rbName == null)) {
            message = super.getL10NMessage(locale);
        } else {
            ResourceBundle bundle = amCache.getResBundle(resourceBundleName,
                    locale);
            String mid = Locale.getString(bundle, errCode, debug);
            ResourceBundle serviceResouceBundle = amCache.getResBundle(rbName,
                    locale);
            String localizedAttributeName = Locale.getString(
                    serviceResouceBundle, attributeI18nKey, debug);
            String[] argsEx = { localizedAttributeName };
            message = MessageFormat.format(mid, (Object[])argsEx);
        }

        return message;
    }
}
