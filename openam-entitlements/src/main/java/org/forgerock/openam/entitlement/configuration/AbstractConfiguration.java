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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.entitlement.configuration;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

import javax.security.auth.Subject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.forgerock.openam.entitlement.utils.EntitlementUtils.*;

/**
 * This class is the base for entitlement configuration and contains common tasks for interacting with the
 * persisted entitlement configuration model.
 */
public abstract class AbstractConfiguration {

    /**
     * Get the organization configuration for the sunEntitlementService service.
     * @param subject The subject used to retrieve the SSO token.
     * @param realm The realm from which to retrieve it.
     * @return The organization configuration, which is guaranteed to not be null.
     * @throws SMSException If the sub configuration could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    protected ServiceConfig getOrgConfig(Subject subject, String realm) throws SMSException, SSOException {
        final SSOToken token = getSSOToken(subject);
        if (token == null) {
            throw new SSOException("Could not find Admin token.");
        }
        ServiceConfig orgConfig = new ServiceConfigManager(SERVICE_NAME, token).getOrganizationConfig(realm, null);
        if (orgConfig == null) {
            throw new SMSException("Configuration '" + SERVICE_NAME + "' in realm '" + realm +
                    "' could not be retrieved.");
        }
        return orgConfig;
    }

    /**
     * Get the sub configuration from the organization configuration for the sunEntitlementService service.
     * @param subject The subject used to retrieve the SSO token.
     * @param realm The realm from which to retrieve it.
     * @param configName The name of the sub configuration.
     * @return The sub configuration, which is guaranteed to not be null.
     * @throws SMSException If the sub configuration could not be read.
     * @throws SSOException If the Admin token could not be found.
     */
    protected ServiceConfig getSubOrgConfig(Subject subject, String realm, String configName) throws SMSException,
            SSOException {

        final ServiceConfig config = getOrgConfig(subject, realm).getSubConfig(configName);
        if (config == null) {
            throw new SMSException("Configuration '" + configName + "' in organization '" + SERVICE_NAME +
                    "' could not be retrieved.");
        }
        return config;
    }

    /**
     * Prepare the attribute map for entry that is about to changed.
     * @param attributes The map holding the attributes to be modified.
     * @param serviceID The ID of the entry that is being modified.
     */
    protected void prepareAttributeMap(Map<String, Set<String>> attributes, String serviceID) {
        attributes.put(SMSEntry.ATTR_SERVICE_ID, Collections.singleton(serviceID));

        final Set<String> setObjectClass = new HashSet<String>();
        setObjectClass.add(SMSEntry.OC_TOP);
        setObjectClass.add(SMSEntry.OC_SERVICE_COMP);
        attributes.put(SMSEntry.ATTR_OBJECTCLASS, setObjectClass);
    }

}
