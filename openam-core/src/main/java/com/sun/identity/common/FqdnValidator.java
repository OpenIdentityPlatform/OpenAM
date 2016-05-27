/*
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
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */
package com.sun.identity.common;

import java.util.HashMap;
import java.util.Map;

import org.forgerock.openam.utils.StringUtils;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.configuration.ConfigurationListener;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.shared.Constants;

/**
 * This class determines whether a host name is valid; and allows to retrieve fully qualified host name from a partial
 * (virtual) host name.
 */
public enum FqdnValidator implements ConfigurationListener {

    INSTANCE;
    private final String defaultHostName;
    private volatile Map<String, String> fqdnMap;

    FqdnValidator() {
        defaultHostName = SystemProperties.get(Constants.AM_SERVER_HOST).toLowerCase();
        initialize();
        ConfigurationObserver.getInstance().addListener(this);
    }

    public static FqdnValidator getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        fqdnMap = getFqdnMap();
    }

    private Map<String, String> getFqdnMap() {
        Map<String, String> map = new HashMap<>();
        String prefix = Constants.AM_FQDN_MAP + "[";
        int prefixLen = prefix.length();

        for (String key : SystemProperties.getAll().stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                int idx = key.indexOf(']', prefixLen + 1);

                if (idx != -1) {
                    String partialName = key.substring(prefixLen, idx);
                    partialName = partialName.trim();
                    String value = SystemProperties.get(key);

                    if (!partialName.isEmpty() && StringUtils.isNotEmpty(value)) {
                        map.put(partialName.toLowerCase(), value.toLowerCase());
                    }
                }
            }
        }

        map.put(defaultHostName, defaultHostName);

        return map;
    }

    /**
     * Returns <code>true</code> if a host name is valid. <code>hostname</code> is valid if it is contained in the list
     * of fully qualified host names. Or, it is the default host name where OpenAM is installed.
     * 
     * @param hostname host name.
     * @return true if a host name is valid.
     */
    public boolean isHostnameValid(String hostname) {
        return hostname != null && fqdnMap.values().contains(hostname.toLowerCase());
    }

    /**
     * Returns null if a given host name is valid. Otherwise, this method looks up the fully qualified domain name map
     * for matching entry. Default host name is returned if there is no matching entry.
     * 
     * @param hostname host name.
     * @return fully qualified host name of <code>hostname</code>.
     */
    public String getFullyQualifiedHostName(String hostname) {
        String fqHostName = null;

        if (!isHostnameValid(hostname)) {
            fqHostName = fqdnMap.get(hostname.toLowerCase());

            if (StringUtils.isEmpty(fqHostName)) {
                fqHostName = defaultHostName;
            }
        }

        return fqHostName;
    }

    @Override
    public void notifyChanges() {
        initialize();
    }
}
