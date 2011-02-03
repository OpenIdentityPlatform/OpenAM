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
 * $Id: ResBundleUtils.java,v 1.3 2008/06/25 05:43:45 qcheng Exp $
 *
 */



package com.sun.identity.policy;

import com.sun.identity.shared.locale.Locale;
import java.util.ResourceBundle;

/**
 * Class to model resource bundle to be used to get messages from, 
 * using the default locale, specified in AMConfig.properties or OS locale
 * if AMConfig.properties does not have locale defined
 */
public class ResBundleUtils {
    /** resource bundle name */
    public static final String rbName = "amPolicy";


    public static ResourceBundle bundle = Locale.getInstallResourceBundle(
					  rbName);

    /**
     * Returns localized string for the default locale specified in AMConfig.
     * properties or if null based on the default OS locale.
     * @param key key to look up the localized message for
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
     * Returns localized formatted string
     *
     * @param key key to look up the localized message for
     * @param  params  parameters to be applied to the message
     * 
     * @return localized string or <code>key</code> if localized string is
     * missing, uses locale as set in AMConfig.properties or default OS locale.
     */
    public static String getString(String key, Object[] params) {
	String localizedStr = Locale.getString(bundle, key, params);
	if (localizedStr == null) {
	    localizedStr = key;
	}
	return localizedStr;
    }
}
