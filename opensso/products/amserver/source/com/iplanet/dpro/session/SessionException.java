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
 * $Id: SessionException.java,v 1.3 2008/06/25 05:41:29 qcheng Exp $
 *
 */

package com.iplanet.dpro.session;

import com.sun.identity.shared.locale.L10NMessageImpl;
import com.sun.identity.shared.locale.Locale;

/**
 * A <code>SessionException</code> is thrown if the Naming Service can not
 * find a URL for the session service.
 */

public class SessionException extends L10NMessageImpl {

    /*
     * CONSTRUCTORS
     */

    /**
     * Constructs an instance of the <code>SessionException</code> class.
     * 
     * @param msg
     *            The message provided by the object which is throwing the
     *            exception
     */
    public SessionException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of the <code>SessionException</code> class with
     * localizable error message
     * 
     * @param bundleName Resource Bundle Name to be used for getting localized
     *        error message.
     * @param errCode Key to resource bundle. You can use <code>ResourceBundle
     *        rb = ResourceBunde.getBundle (rbName,locale); String localizedStr
     *        = rb.getString(errorCode)</code>.
     * @param args arguments to message. If it is not present pass the as null.
     */
    public SessionException(String bundleName, String errCode, Object[] args) {
        super(bundleName, errCode, args);
    }

    /**
     * Constructs an instance of the <code>SessionException</code> class.
     * 
     * @param t The <code>Throwable</code> object provided by the object which
     *        is throwing the exception.
     */
    public SessionException(Throwable t) {
        super(t);
    }

    /**
     * Gets the Localized message string.
     */
    public String getL10NMessage() {
        return getL10NMessage(Locale.getDefaultLocale());
    }
}
