/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.authentication.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Restricts the list of Auth Services (Chains) to those that include Authentication via a Social AuthN provider.
 */
public class ConfiguredSocialAuthServices extends ConfiguredAuthServices {

    private static final String OAUTH2_TYPE = "OAuth";
    private Debug debug = Debug.getInstance("amConsole");

    @Override
    public Map getChoiceValues(Map envParams) {
        Map<String,String> choices = super.getChoiceValues(envParams);
        if (choices.size() > 1) {
            choices.remove(ISAuthConstants.BLANK);
        }
        return choices;
    }

    @Override
    protected Set<String> filterConfigs(Set<String> namedConfigs, ServiceConfig parentConfig, String realm,
            SSOToken adminToken) {
        AMAuthenticationManager authMgr;
        try {
            authMgr = new AMAuthenticationManager(adminToken, realm);
        } catch (AMConfigurationException e) {
            debug.warning("Could not load authentication manager for realm: "+realm, e);
            return Collections.EMPTY_SET;
        }
        Set<String> configs = new TreeSet<String>();
        for (String config : namedConfigs) {
            try {
                ServiceConfig authConfig = parentConfig.getSubConfig(config);
                Set<String> chainConfig = (Set<String>) authConfig.getAttributes().get(AMAuthConfigUtils.ATTR_NAME);
                AppConfigurationEntry[] chain = AMAuthConfigUtils.parseValues(chainConfig.iterator().next());
                for (int i = 0; i < chain.length; i++) {
                    if (getType(authMgr, chain[i]).equals(OAUTH2_TYPE)) {
                        // There's an OAuth2 module in the chain, so this could be a social authn chain
                        configs.add(config);
                    }
                }
            } catch (SMSException e) {
                if (debug.messageEnabled()) {
                    debug.message("Not using auth chain as couldn't get config: "+config, e);
                }
            } catch (SSOException e) {
                if (debug.warningEnabled()) {
                    debug.warning("Invalid SSO Token when trying to get config for " + config, e);
                }
            }
        }
        return configs;
    }

    private String getType(AMAuthenticationManager authMgr, AppConfigurationEntry appConfigurationEntry) {
        return authMgr.getAuthInstanceType(appConfigurationEntry.getLoginModuleName());
    }
}
