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
 * $Id: IdRepoBundle.java,v 1.3 2008/06/25 05:43:28 qcheng Exp $
 *
 */

package com.sun.identity.idm;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class IdRepoBundle {

    private static ResourceBundle profileBundle = null;

    private static Map bundles = new HashMap();

    public final static String BUNDLE_NAME = "amIdRepo";
    static {
        profileBundle = com.sun.identity.shared.locale.Locale
                .getInstallResourceBundle(BUNDLE_NAME);
    }

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

    public static String getString(String key, String locale) {
        if (locale == null) {
            return getString(key);
        }
        return getBundleFromHash(locale).getString(key);
    }

    private static ResourceBundle getBundleFromHash(String locale) {

        ResourceBundle rb = (ResourceBundle) bundles.get(locale);
        if (rb == null) {
            rb = com.sun.identity.shared.locale.Locale.getResourceBundle(
                BUNDLE_NAME, locale);
            if (rb == null) {
                rb = profileBundle;
            }

            bundles.put(locale, rb);
        }
        return rb;

    }
}
