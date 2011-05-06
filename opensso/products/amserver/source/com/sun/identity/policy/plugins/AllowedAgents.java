/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AllowedAgents.java,v 1.1 2008/09/04 02:40:57 goodearth Exp $
 *
 */


package com.sun.identity.policy.plugins;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.shared.debug.Debug;

/**
 * The class determines the allowed agents for agent profile sharing.
 * This list is computed per realm.
 */
public class AllowedAgents extends ChoiceValues {

    private static final String version = "1.0";

    private static final String agentServiceName = IdConstants.AGENT_SERVICE;

    private static ServiceConfigManager scm = null;

    // Debug file
    Debug debug = Debug.getInstance("AuthAgents");

    /**
     * Creates <code>AllowedAgents</code> object.
     * Default constructor that will be used by the SMS
     * to create an instance of this class.
     */
    public AllowedAgents() {
        // do nothing
    }
    
    /**
     * Returns the choice values and their corresponding localization keys.
     *
     * @return the choice values and their corresponding localization keys.
     */
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }

    /**
     * Returns the choice values from configured environment params.
     * @param envParams map for configured parameters
     * @return the choice values from configured environment params.
     */
    public Map getChoiceValues(Map envParams) {
        String orgDN = null;
        SSOToken adminToken = null;
        
        Map answer = new HashMap(2);
        if (envParams != null) {
            orgDN = (String)envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (orgDN == null || orgDN.length() == 0) {
            orgDN = SMSEntry.getRootSuffix();
        }
        try {
            adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());

            ServiceConfig oc = getOrgConfig(adminToken, orgDN);
            Set agentConfigs = oc.getSubConfigNames();
            if (agentConfigs != null && !agentConfigs.isEmpty()) {
                // Get the agent's schemaID from the config and remove the
                // Agent Authenticator name from the list.
                for (Iterator it = agentConfigs.iterator(); it.hasNext(); ) {
                    String agentName = (String) it.next();
                    ServiceConfig aCfg = oc.getSubConfig(agentName);
                    if (aCfg != null) {
                        String agentType = aCfg.getSchemaID();
                        if (!agentType.equalsIgnoreCase("SharedAgent")) {
                            answer.put(agentName, agentName);
                        }
                    }
                }
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AllowedAgents.getChoiceValues(): SSOException:"
                    + ssoe);
            }
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AllowedAgents.getChoiceValues(): SMSException:"
                    + smse);
            }
        }
        //return the choice values map
        return (answer);
    }

    // Returns the organization configuration of the 'default' group
    // from AgentService.
    private ServiceConfig getOrgConfig(SSOToken token, String realmName) {

        if (debug.messageEnabled()) {
            debug.message("AllowedAgents.getOrgConfig() called. ");
        }
        ServiceConfig orgConfigCache = null;
        try {
            if (scm == null) {
                scm = new ServiceConfigManager(token, agentServiceName,
                    version);
            }
            orgConfigCache = scm.getOrganizationConfig(realmName, null);
        } catch (SMSException smse) {
            if (debug.warningEnabled()) {
                debug.warning("AllowedAgents.getOrgConfig(): "
                        + "Unable to get organization config due to " + smse);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("AllowedAgents.getOrgConfig(): "
                        + "Unable to get organization config due to " + ssoe);
            }
        }
        return (orgConfigCache);
    }
}
