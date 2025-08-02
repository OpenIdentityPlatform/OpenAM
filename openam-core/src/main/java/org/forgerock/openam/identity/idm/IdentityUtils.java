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
 * Copyright 2016 ForgeRock AS.
 * Portions Copyrighted 2005 Sun Microsystems Inc.
 * Portions Copyrighted 2025 3A Systems LLC.
 */
package org.forgerock.openam.identity.idm;

import jakarta.inject.Singleton;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;

/**
 * Collection of helper functions for {@link AMIdentity}.
 */
@Singleton
public class IdentityUtils {

    private static final Debug DEBUG = Debug.getInstance("amIdm");

    /**
     * Returns the matching DN from the AM SDK for this entry. This utility is
     * required by auth.
     *
     * @param id  {@code AMIdentity} object.
     * @return {@code DN} of the object, as represented in the datastore.
     */
    public static String getDN(AMIdentity id) {
        if (id.getDN() != null) {
            return id.getDN();
        } else {
            return id.getUniversalId();
        }
    }

    /**
     * Determines the name of the identity based on the provided universal ID.
     *
     * @param universalId The universal ID of the identity.
     * @return The name of the identity, or null if the name could not be determined.
     */
    public String getIdentityName(String universalId) {
        try {
            return new AMIdentity(null, universalId).getName();
        } catch (IdRepoException ire) {
            DEBUG.warning("Unable to retrieve username from universal ID: {}", universalId, ire);
        }

        return null;
    }

    /**
     * Determines the universal ID of the user based on the provided details.
     *
     * @param identityName The name of the identity.
     * @param idType The type of the identity.
     * @param realm The realm this identity belongs to.
     * @return The universal ID based on the provided parameters.
     */
    public String getUniversalId(String identityName, IdType idType, String realm) {
        return new AMIdentity(null, null, identityName, idType, realm).getUniversalId();
    }

    /**
     * @param ssoToken The user's SSO Token
     * @return True if the user SSO Token corresponds to CASPA (C Application Server Policy Agent) or JASPA (Java
     * Application Server Policy Agent)
     */
    public static boolean isCASPAorJASPA(SSOToken ssoToken) {

        try {
            if (ssoToken != null) {
                AMIdentity identity = new AMIdentity(ssoToken);
                String agentType = AgentConfiguration.getAgentType(identity);
                return AgentConfiguration.AGENT_TYPE_J2EE.equalsIgnoreCase(agentType)
                        || AgentConfiguration.AGENT_TYPE_WEB.equalsIgnoreCase(agentType);
            }
        } catch (IdRepoException|SSOException ignored) {
        }
        return false;
    }
}
