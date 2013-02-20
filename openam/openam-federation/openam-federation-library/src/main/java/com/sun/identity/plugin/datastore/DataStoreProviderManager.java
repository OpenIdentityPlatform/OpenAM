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
 * $Id: DataStoreProviderManager.java,v 1.4 2008/08/06 17:28:13 exu Exp $
 *
 */

package com.sun.identity.plugin.datastore;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.common.SystemConfigurationUtil;

/**
 * This is a singleton class used to manage DataStore providers.
 * @supported.all.api
 */
public final class DataStoreProviderManager {
    
    /**
     * Default.
     */
    public static final String DEFAULT = "default";

    /**
     * Prefix for provider attribute.
     */
    public static final String PROVIDER_ATTR_PREFIX =
        "com.sun.identity.plugin.datastore.class.";

    /**
     * Attribute name for default provider.
     */
    public static final String DEFAULT_PROVIDER_ATTR =
        "com.sun.identity.plugin.datastore.class.default";

    private Map providerMap = null;
    private DataStoreProvider defaultProvider = null;
    private static DataStoreProviderManager instance =
        new DataStoreProviderManager();
    private Debug debug = Debug.getInstance("libPlugins");
    private ResourceBundle bundle = Locale.getInstallResourceBundle(
        "libDataStoreProvider");


    private DataStoreProviderManager() {
        providerMap = new HashMap();
        try {
            defaultProvider = getDefaultProvider();
            defaultProvider.init(DEFAULT);
        } catch (DataStoreProviderException de) {
            debug.error("DataStoreProviderManager: "
                + "exception obtaining default provider:", de);
        }
    }

    /**
     * Gets the singleton instance of <code>DataStoreProviderManager</code>.
     * @return The singleton <code>DataStoreProviderManager</code> instance
     * @throws DataStoreProviderException if unable to get the singleton
     *  <code>DataStoreProviderManager</code> instance.
     */
    public static DataStoreProviderManager getInstance()
        throws DataStoreProviderException
    {
        return instance;
    }

  
    /**
     * Gets the provider associated with the component.
     * When <code>null</code> componentName is passed in, default provider
     * is returned.
     * @param componentName component name, such as saml, saml2, id-ff, disco,
     *  authnsvc, and idpp.
     * @return datastore provider for the calling component
     * @throws DataStoreProviderException if an error occurred.
     *
     */
    public DataStoreProvider getDataStoreProvider(String componentName)
        throws DataStoreProviderException
    {
        if ((componentName == null) || (componentName.length() == 0)) {
            return defaultProvider;
        } else {
            DataStoreProvider provider = (DataStoreProvider)
                                        providerMap.get(componentName);
            if (provider != null) {
                return provider;
            }
            String className = SystemConfigurationUtil.getProperty(
                PROVIDER_ATTR_PREFIX + componentName);
            if ((className != null) && (className.length() > 0)) {
                try {
                    provider = (DataStoreProvider)
                        Class.forName(className).newInstance();
                } catch (Exception e) {
                    debug.error("DataStoreProviderManage"
                        + "r.getDataStoreProvider: exception while "
                        + "instanciating provider:" + className + ":", e);
                    throw new DataStoreProviderException(e);
                }
            }
            if (provider == null) {
                if (debug.messageEnabled()) {
                    debug.message("DataStoreProviderMana"
                        + "ger.getDataStoreProvider: no provider specified "
                        + "for " + componentName + ", using default.");
                }
                provider = getDefaultProvider();
            }
            provider.init(componentName);
            synchronized(providerMap) {
                providerMap.put(componentName, provider);
            }
            return provider;
        }
    }

    private DataStoreProvider getDefaultProvider() 
        throws DataStoreProviderException
    {
        String className = 
            SystemConfigurationUtil.getProperty(DEFAULT_PROVIDER_ATTR);
        if ((className == null) || (className.length() == 0)) {
            throw new DataStoreProviderException(
                bundle.getString("defaultProviderNotDefined"));
        }
        try {
            return ((DataStoreProvider)Class.forName(
                className).newInstance());
        } catch (Exception e) {
            throw new DataStoreProviderException(e);
        }
    }
}
