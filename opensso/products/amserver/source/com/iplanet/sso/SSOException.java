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
 * $Id: SSOException.java,v 1.5 2008/06/25 05:41:42 qcheng Exp $
 *
 */

package com.iplanet.sso;

import com.sun.identity.shared.locale.Locale;
import com.sun.identity.shared.locale.L10NMessageImpl;

/**
 * This <code>SSOException</code> is thrown when there are single sign on token 
 * operation error.
 *
 * @supported.all.api
 */
public class SSOException extends L10NMessageImpl {

    private static final long serialVersionUID = 8947757932659270975L;

    /**
     * Constructs a <code>SSOException</code> with a detail message.
     * @param msg The message provided by the object that is throwing the
     * exception.
     */
    public SSOException(String msg) {
        super(msg);
    }

    /**
     * Constructs a <code>SSOException</code> with a detailed localizable error
     * message.
     *
     * @param bundleName Resource Bundle Name to be used for getting 
     *        localized error message.
     * @param errCode Key to resource bundle. You can use
     * 
     * <pre>
     *  ResourceBundle rb = ResourceBunde.getBundle (rbName,locale);
     *  String localizedStr = rb.getString(errorCode)
     * </pre>
     * 
     * @param args Arguments to message. If arguments to the message are not
     *            present, pass null.
     */
    public SSOException(String bundleName, String errCode, Object[] args) {
        super(bundleName, errCode, args);
    }

    /**
     * Constructs a <code>SSOException</code> with a specified throwable cause 
     * and detailed message.
     *
     * @param t The <code>Throwable</code> object provided by the object that is
     *        throwing the exception.
     */
    public SSOException(Throwable t) {
        super(t);
    }

    /**
     * Returns the localized message for this exception. Default locale is used
     * to retrieve this message from properties file, which is the locale of OS.
     * 
     * @return the localized message for this exception.
     */
    public String getL10NMessage() {
        return getL10NMessage(Locale.getDefaultLocale());
    }
}
