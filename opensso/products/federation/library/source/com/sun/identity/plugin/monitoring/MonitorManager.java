/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MonitorManager.java,v 1.1 2009/06/19 02:48:04 bigfatrat Exp $
 *
 */

package com.sun.identity.plugin.monitoring;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.debug.Debug;

/**
 * The <code>MonitorManager</code> is used to get instances of
 * the <code>monitoring.*</code> implementation classes.
 * If the system property com.sun.identity.plugin.monitoring.class
 * is set, this class will be used as the provider instead of the default
 *  implementation.
 */

public final class MonitorManager {
    
    public static final String MONAGENT_PROVIDER_NAME = 
        "com.sun.identity.plugin.monitoring.agent.class";
    public static final String MONAGENT_PROV_IMPL =
        "com.sun.identity.plugin.monitoring.impl.AgentProvider";
    public static final String MONSAML1_PROVIDER_NAME = 
        "com.sun.identity.plugin.monitoring.saml1.class";
    public static final String MONSAML1_PROV_IMPL =
        "com.sun.identity.plugin.monitoring.impl.FedMonSAML1SvcProvider";
    public static final String MONSAML2_PROVIDER_NAME = 
        "com.sun.identity.plugin.monitoring.saml2.class";
    public static final String MONSAML2_PROV_IMPL =
        "com.sun.identity.plugin.monitoring.impl.FedMonSAML2SvcProvider";
    public static final String MONIDFF_PROVIDER_NAME = 
        "com.sun.identity.plugin.monitoring.idff.class";
    public static final String MONIDFF_PROV_IMPL = 
        "com.sun.identity.plugin.monitoring.impl.FedMonIDFFSvcProvider";
    public static final Debug debug = Debug.getInstance("libPlugins");

    private static FedMonAgent agentProvider;
    private static FedMonSAML1Svc saml1SvcProvider;
    private static FedMonSAML2Svc saml2SvcProvider;
    private static FedMonIDFFSvc idffSvcProvider;

    /**
     * Returns an instance of the <code>FedMonAgent</code> object.
     *
     * @return instance of <code>Logger</code> object.
     * @exception LogException if there is an error.
     */
    public static FedMonAgent getAgent() {
        if (agentProvider != null) {
            return agentProvider;
        }
        String pluginName = SystemConfigurationUtil.getProperty(
            MONAGENT_PROVIDER_NAME, MONAGENT_PROV_IMPL);
        try {
            if (pluginName != null && pluginName.length() > 0) {
                Class agtProviderClass =
                        Class.forName(pluginName.trim());
                agentProvider = (FedMonAgent)agtProviderClass.newInstance();
                agentProvider.init();
            }
        } catch (Exception e) {
            debug.error("Error creating FedMonAgent class instance : ", e);
        }
        return agentProvider;
    }

    public static FedMonSAML1Svc getSAML1Svc() {
        if (saml1SvcProvider != null) {
            return saml1SvcProvider;
        }
        String pluginName = SystemConfigurationUtil.getProperty(
            MONSAML1_PROVIDER_NAME, MONSAML1_PROV_IMPL);

        try {
            if (pluginName != null && pluginName.length() > 0) {
                Class samlProviderClass =
                        Class.forName(pluginName.trim());
                saml1SvcProvider =
                    (FedMonSAML1Svc)samlProviderClass.newInstance();
                saml1SvcProvider.init();
            }
        } catch (Exception e) {
            debug.error("Error creating SAML1Svc class instance : ", e);
        }
        return saml1SvcProvider;
    }

    public static FedMonSAML2Svc getSAML2Svc() {
        if (saml2SvcProvider != null) {
            return saml2SvcProvider;
        }
        String pluginName = SystemConfigurationUtil.getProperty(
            MONSAML2_PROVIDER_NAME, MONSAML2_PROV_IMPL);

        try {
            if (pluginName != null && pluginName.length() > 0) {
                Class samlProviderClass =
                        Class.forName(pluginName.trim());
                saml2SvcProvider =
                    (FedMonSAML2Svc)samlProviderClass.newInstance();
                saml2SvcProvider.init();
            }
        } catch (Exception e) {
            debug.error("Error creating SAML2Svc class instance : ", e);
        }
        return saml2SvcProvider;
    }

    public static FedMonIDFFSvc getIDFFSvc() {
        if (idffSvcProvider != null) {
            return idffSvcProvider;
        }
        String pluginName = SystemConfigurationUtil.getProperty(
            MONIDFF_PROVIDER_NAME, MONIDFF_PROV_IMPL);

        try {
            if (pluginName != null && pluginName.length() > 0) {
                Class idffProviderClass =
                        Class.forName(pluginName.trim());
                idffSvcProvider =
                    (FedMonIDFFSvc)idffProviderClass.newInstance();
                idffSvcProvider.init();
            }
        } catch (Exception e) {
            debug.error("Error creating IDFFSvc class instance : ", e);
        }
        return idffSvcProvider;
    }

}
