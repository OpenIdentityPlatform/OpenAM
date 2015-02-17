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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.utils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.AccessController;

/**
 * @see org.forgerock.openam.forgerockrest.utils.SoapSTSAgentIdentity
 */
public class SoapSTSAgentIdentityImpl implements SoapSTSAgentIdentity {
    /*
    These two Strings are used to identify SoapSTS agents. The AgentService identifies the base AgentService defined
    in AgentService.xml. The SoapSTSAgent is the name of the SubSchema entry defining Soap STS Agents.
     */
    private static final String AGENT_SERVICE_NAME = "AgentService";
    private static final String SOAP_STS_SCHEMA_NAME = "SoapSTSAgent";

    private final Debug debug;

    @Inject
    SoapSTSAgentIdentityImpl(@Named("frRest") Debug debug) {
        this.debug = debug;
    }

    @Override
    public boolean isSoapSTSAgent(SSOToken token) {
        AMIdentity identity;
        try {
            identity = IdUtils.getIdentity(token);
        } catch (Exception e) {
            debug.error("Exception occurred in SoapSTSAgentIdentityImpl obtaining identity corresponding to SSOToken: " + e, e);
            return false;
        }
        /*
        Perform a pre-check to insure that we are at least dealing with an agent before instantiating a ServiceConfigManager.
         */
        if (!IdType.AGENT.equals(identity.getType())) {
            return false;
        }
        ServiceConfig agentService;
        try {
            agentService =
                    new ServiceConfigManager(AGENT_SERVICE_NAME, getAdminToken()).getOrganizationConfig(identity.getRealm(), null);
        } catch (Exception e) {
            debug.error("Exception occurred in SoapSTSAgentIdentityImpl obtaining base " +
                    "AgentService ServiceConfig instance." + e, e);
            return false;
        }
        try {
            final ServiceConfig soapSTSAgent = agentService.getSubConfig(identity.getName());
            return (soapSTSAgent != null) && SOAP_STS_SCHEMA_NAME.equals(soapSTSAgent.getSchemaID());
        } catch (SSOException e) {
            /*
             Should only enter this block if the return from getAdminToken is an invalid token
             */
            return false;
        } catch (SMSException e) {
            /*
            Should only enter this block if an error occurs accessing LDAP.
             */
            return false;
        }
    }

    private SSOToken getAdminToken()  {
        return AccessController.doPrivileged(AdminTokenAction.getInstance());
    }
}
