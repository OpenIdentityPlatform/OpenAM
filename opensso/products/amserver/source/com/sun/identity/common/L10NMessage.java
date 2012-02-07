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
 * $Id: L10NMessage.java,v 1.4 2008/06/25 05:42:26 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.Locale;

/**
 * This interface provides access to error code and resource bundle name to
 * provide localised error message.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.locale.L10NMessage}
 */
public interface L10NMessage {

    /**
     * Use this method to get localized error message directly.
     * 
     * @param loc Locale of the error message.
     * @return localized error message.
     */
    public String getL10NMessage(Locale loc);

    /**
     * Use this method to chain this exception with another to get localized
     * error messge use getL10NMessage method
     * 
     * @return ResourceBundle Name associated with this error message.
     */
    public String getResourceBundleName();

    /**
     * Use this method to chain this exception with another to get localized
     * error messge use getL10NMessage method
     * 
     * @return Error code associated with this error message.
     */
    public String getErrorCode();

    /**
     * Use this method to chain this exception with another to get localized
     * error messge use getL10NMessage method
     * 
     * @return arguments for formatting this error message. You need to use
     *         MessageFormat class to format the message It can be null.
     */
    public Object[] getMessageArgs();

    /**
     * Use this method to get error message in default ENGLISH locale.
     * 
     * @return gets error message
     */
    public String getMessage();

}
