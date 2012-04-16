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
 * $Id: ResBundleUtils.java,v 1.4 2008/06/25 05:43:24 qcheng Exp $
 *
 */

package com.sun.identity.delegation;

import java.util.ResourceBundle;

import com.sun.identity.shared.locale.Locale;

 /**
  * This is a utility class providing methods to obtain localized 
  * messages for the delegation service.
  */
public class ResBundleUtils {
    /** 
     * resource bundle name 
     */
    public static final String rbName = "amDelegation";

    /**
     * Resource bundle to be used to get messages from, using the default
     * locale, specified in AMConfig.properties or OS locale if
     * AMConfig.properties does not have locale defined
     */

    public static ResourceBundle bundle = Locale
            .getInstallResourceBundle(rbName);

    /**
     * gets localized string for the default locale specified in
     * AMConfig.properties or if null based on the default OS locale.
     * 
     * @param key to localized string
     * @return localized string or <code>key</code> if localized string is
     *         missing
     */
    public static String getString(String key) {
        String localizedStr = Locale.getString(bundle, key);
        if (localizedStr == null) {
            localizedStr = key;
        }
        return localizedStr;
    }

    /**
     * gets localized formatted string
     * 
     * @param key to localized string
     * @param params parameters to be applied to the message
     * 
     * @return localized string or <code>key</code> if localized string is
     *         missing, uses locale as set in AMConfig.properties or default OS
     *         locale.
     */
    public static String getString(String key, Object[] params) {
        String localizedStr = Locale.getString(bundle, key, params);
        if (localizedStr == null) {
            localizedStr = key;
        }
        return localizedStr;
    }
}
