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
package org.forgerock.openam.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.debug.Debug;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

/**
 * Extracts the valid relaystate URL list from the hosted SAML entity's configuration.
 */
public class ValidRelayStateExtractor implements ValidDomainExtractor<ValidRelayStateExtractor.SAMLEntityInfo> {

    private static final Debug DEBUG = Debug.getInstance("libSAML2");

    @Override
    public Collection<String> extractValidDomains(final SAMLEntityInfo entityInfo) {
        try {
            BaseConfigType config;
            final Map<String, List<String>> attrs;

            final SAML2MetaManager metaManager = new SAML2MetaManager();

            if (SAML2Constants.SP_ROLE.equalsIgnoreCase(entityInfo.role)) {
                config = metaManager.getSPSSOConfig(entityInfo.realm, entityInfo.entityID);
            } else {
                config = metaManager.getIDPSSOConfig(entityInfo.realm, entityInfo.entityID);
            }

            if (config == null) {
                DEBUG.warning("ValidRelayStateExtractor.getValidDomains: Entity config is null for entityInfo: "
                        + entityInfo);
                return null;
            }
            attrs = SAML2MetaUtils.getAttributes(config);
            if (attrs == null) {
                DEBUG.warning("ValidRelayStateExtractor.getValidDomains: Cannot find extended attributes");
                return null;
            }

            final List<String> values = attrs.get(SAML2Constants.RELAY_STATE_URL_LIST);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        } catch (final SAML2MetaException sme) {
            DEBUG.warning("Unable to retrieve extended configuration", sme);
        }
        return null;
    }

    /**
     * A domain object that helps to uniquely identify a given SAML entity in the configuration.
     */
    public static final class SAMLEntityInfo {

        private final String realm;
        private final String entityID;
        private final String role;

        private SAMLEntityInfo(final String realm, final String entityID, final String role) {
            this.realm = realm;
            this.entityID = entityID;
            this.role = role;
        }

        /**
         * Creates a new SAMLEntityInfo object based on the provided details.
         *
         * @param realm The realm where the hosted entity belongs to.
         * @param entityID The entity ID of the hosted entity.
         * @param role The role of the hosted entity (e.g. SPRole or IDPRole).
         * @return The SAMLEntityInfo object corresponding to the provided details.
         */
        public static SAMLEntityInfo from(final String realm, final String entityID, final String role) {
            return new SAMLEntityInfo(realm, entityID, role);
        }

        @Override
        public String toString() {
            return "SAMLEntityInfo{" + "realm=" + realm + ", entityID=" + entityID + ", role=" + role + '}';
        }
    }
}
