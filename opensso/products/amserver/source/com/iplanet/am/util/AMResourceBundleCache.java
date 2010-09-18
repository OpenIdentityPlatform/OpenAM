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
 * $Id: AMResourceBundleCache.java,v 1.3 2008/06/25 05:41:27 qcheng Exp $
 *
 */

package com.iplanet.am.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A singleton class that cache resource bundle object
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.locale.AMResourceBundleCache}
 */
public class AMResourceBundleCache {
    private static AMResourceBundleCache instance;

    private HashMap mapBundles = new HashMap(30);

    private Debug debug = null;

    private AMResourceBundleCache() {
        debug = Debug.getInstance("amSDK");
    }

    /**
     * gets instance of AMResourceBundleCache
     * 
     * @return instance of AMResourceBundleCache
     */
    public static AMResourceBundleCache getInstance() {
        if (instance == null) {
            instance = new AMResourceBundleCache();
        }
        return instance;
    }

    /**
     * gets resource bundle from cache
     * 
     * @param name
     *            of bundle
     * @param locale
     *            of bundle
     * @return resource bundle
     */
    public ResourceBundle getResBundle(String name, Locale locale) {
        ResourceBundle resBundle = null;
        Map map = (Map) mapBundles.get(name);

        if (map != null) {
            resBundle = (ResourceBundle) map.get(locale);
        }
        if (resBundle == null) {
            try {
                resBundle = ResourceBundle.getBundle(name, locale);
            } catch (MissingResourceException mre) {
                debug.error("AMResourceBundleCache.getResBundle", mre);
            }

            synchronized (mapBundles) {
                if (map == null) {
                    map = new HashMap(5);
                    mapBundles.put(name, map);
                }
                map.put(locale, resBundle);
            }
        }

        return resBundle;
    }

    /** clears all resource bundle objects */
    public void clear() {
        synchronized (mapBundles) {
            mapBundles.clear();
        }
    }
}
