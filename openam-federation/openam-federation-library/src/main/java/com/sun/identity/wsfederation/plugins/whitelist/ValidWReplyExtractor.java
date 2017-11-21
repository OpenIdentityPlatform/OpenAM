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
 * Copyright 2014-2016 ForgeRock AS.
 */
package com.sun.identity.wsfederation.plugins.whitelist;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;

/**
 * Extracts the valid relaystate URL list from the hosted SAML entity's configuration.
 */
public class ValidWReplyExtractor implements ValidDomainExtractor<ValidWReplyExtractor.WSFederationEntityInfo> {

    private static final Debug DEBUG = WSFederationUtils.debug;

    @Override
    public Collection<String> extractValidDomains(final WSFederationEntityInfo entityInfo) {
        try {
            BaseConfigType config;
            final Map<String, List<String>> attrs;


            if (SAML2Constants.SP_ROLE.equalsIgnoreCase(entityInfo.role)) {
                config = WSFederationUtils.getMetaManager().getSPSSOConfig(entityInfo.realm, entityInfo.entityID);
            } else {
                config = WSFederationUtils.getMetaManager().getIDPSSOConfig(entityInfo.realm, entityInfo.entityID);
            }

            if (config == null) {
                DEBUG.warning("ValidWReplyExtractor.getValidDomains: Entity config is null for entityInfo: "
                        + entityInfo);
                return null;
            }

            attrs = WSFederationMetaUtils.getAttributes(config);
            if (attrs == null) {
                DEBUG.warning("ValidWReplyExtractor.getValidDomains: Cannot find extended attributes");
                return null;
            }

            final List<String> values = attrs.get(WSFederationConstants.WREPLY_URL_LIST);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        } catch (final WSFederationMetaException sme) {
            DEBUG.warning("Unable to retrieve extended configuration", sme);
        }
        return null;
    }

    /**
     * A domain object that helps to uniquely identify a given WS federation entity in the configuration.
     */
    public static final class WSFederationEntityInfo {

        private final String realm;
        private final String entityID;
        private final String role;

        private WSFederationEntityInfo(final String realm, final String entityID, final String role) {
            this.realm = realm;
            this.entityID = entityID;
            this.role = role;
        }

        /**
         * Creates a new WSFederationEntityInfo object based on the provided details.
         *
         * @param realm The realm where the hosted entity belongs to.
         * @param entityID The entity ID of the hosted entity.
         * @param role The role of the hosted entity (e.g. SPRole or IDPRole).
         * @return The WSFederationEntityInfo object corresponding to the provided details.
         */
        public static WSFederationEntityInfo from(final String realm, final String entityID, final String role) {
            return new WSFederationEntityInfo(realm, entityID, role);
        }

        @Override
        public String toString() {
            return "WSFederationEntityInfo{" + "realm=" + realm + ", entityID=" + entityID + ", role=" + role + '}';
        }
    }
}
