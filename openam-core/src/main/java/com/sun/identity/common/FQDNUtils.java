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
 * $Id: FQDNUtils.java,v 1.6 2008/08/19 19:08:59 veiming Exp $
 *
 */

package com.sun.identity.common;

import com.sun.identity.shared.Constants;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;

/**
 * This class provides utility methods to check if a host name is valid; and to
 * get fully qualified host name from a partial (virtual) host name.
 */
public class FQDNUtils {
    private static FQDNUtils instance = new FQDNUtils();
    private Map fqdnMap;
    private String defaultHostName;
    private Set validHostNames;

    /**
     * Constructs a <code>FQDNUtils</code> object by reading default host name
     * and <code>FQDN</code> map information from
     * <code>AMConfig.properties</code> file.
     */
    private FQDNUtils() {
        init();
    }

    public static FQDNUtils getInstance() {
        return instance;
    }

    public void init() {
        fqdnMap = getFQDNMapFromAMConfig();
        defaultHostName = SystemProperties.get(Constants.AM_SERVER_HOST);
        validHostNames = getLowerCaseValuesFromMap(fqdnMap);
        validHostNames.add(defaultHostName.toLowerCase());
    }

    /**
     * Constructs a <code>FQDNUtils</code> object with a <code>FQDN</code>
     * map and a default host name.
     * 
     * @param fqdnMap
     *            <code>FQDN</code> map.
     * @param defaultHostName
     *            default host name.
     */
    public FQDNUtils(Map fqdnMap, String defaultHostName) {
        this.fqdnMap = fqdnMap;
        this.defaultHostName = defaultHostName;
        validHostNames = getLowerCaseValuesFromMap(fqdnMap);
        validHostNames.add(defaultHostName.toLowerCase());
    }

    private Map getFQDNMapFromAMConfig() {
        Map map = new HashMap();
        String prefix = Constants.AM_FQDN_MAP + "[";
        int prefixLen = prefix.length();
        Enumeration e = (SystemProperties.getAll()).propertyNames();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();

            if (key.startsWith(prefix)) {
                int idx = key.indexOf(']', prefixLen + 1);

                if (idx != -1) {
                    String partialName = key.substring(prefixLen, idx);
                    partialName = partialName.trim();

                    if (partialName.length() > 0) {
                        map.put(partialName, SystemProperties.get(key));
                    }
                }
            }
        }

        return map;
    }

    private Set getLowerCaseValuesFromMap(Map map) {
        Set set = new HashSet();
        for (Iterator iter = map.values().iterator(); iter.hasNext();) {
            set.add(((String) iter.next()).toLowerCase());
        }
        return set;
    }

    /**
     * Returns true if a host name is valid. <code>hostname</code> is valid if
     * it is contained in the list of fully qualified host names. Or, it is the
     * default host name where OpenSSO is installed.
     * 
     * @param hostname
     *            host name
     * @return true if a host name is valid.
     */
    public boolean isHostnameValid(String hostname) {
        return (hostname != null)
                && validHostNames.contains(hostname.toLowerCase());
    }

    /**
     * Returns null if a given host name is valid. Otherwises, this method looks
     * up the fully qualified <code>DN</code> map for matching entry. Default
     * host name is returned if there is no matching entry.
     * 
     * @param hostname
     *            host name.
     * @return fully qualified host name of <code>hostname</code>
     */
    public String getFullyQualifiedHostName(String hostname) {
        String fqHostName = null;

        if (!isHostnameValid(hostname)) {
            fqHostName = (String) fqdnMap.get(hostname);

            if ((fqHostName == null) || (fqHostName.length() == 0)) {
                fqHostName = defaultHostName;
            }
        }

        return fqHostName;
    }
}
