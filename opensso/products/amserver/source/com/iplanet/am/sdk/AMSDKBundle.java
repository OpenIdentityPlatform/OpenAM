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
 * $Id: AMSDKBundle.java,v 1.3 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.sun.identity.shared.locale.Locale;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class AMSDKBundle {
    public static final String BUNDLE_NAME = "amProfile";

    private static Map strLocaleBundles = new HashMap();

    private static Map localeBundles = new HashMap();

    private static ResourceBundle profileBundle = Locale
            .getInstallResourceBundle(BUNDLE_NAME);

    public static String getString(String key) {
        return profileBundle.getString(key);
    }

    public static String getString(String key, Object[] params) {
        return (MessageFormat.format(profileBundle.getString(key), params));
    }

    public static String getString(String key, Object[] params, String locale) {
        ResourceBundle rb = getBundleFromHash(locale);
        return (MessageFormat.format(rb.getString(key), params));
    }

    public static String getString(String key, java.util.Locale locale) {
        return (locale == null) ? getString(key) : getBundleFromHash(locale)
                .getString(key);
    }

    public static String getString(String key, String locale) {
        return (locale == null) ? getString(key) : getBundleFromHash(locale)
                .getString(key);
    }

    private static ResourceBundle getBundleFromHash(String locale) {
        ResourceBundle rb = (ResourceBundle) strLocaleBundles.get(locale);

        if (rb == null) {
            rb = com.sun.identity.shared.locale.Locale.getResourceBundle(
                BUNDLE_NAME, locale);
            if (rb == null) {
                rb = profileBundle;
            }

            strLocaleBundles.put(locale, rb);
        }

        return rb;
    }

    static ResourceBundle getBundleFromHash(java.util.Locale locale) {
        ResourceBundle rb = (ResourceBundle) localeBundles.get(locale);

        if (rb == null) {
            rb = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            if (rb == null) {
                rb = profileBundle;
            }
            localeBundles.put(locale, rb);
        }

        return rb;
    }
}
