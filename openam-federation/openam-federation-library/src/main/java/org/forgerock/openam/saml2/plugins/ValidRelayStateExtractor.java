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
 * Portions Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.debug.Debug;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

/**
 * Extracts the valid relaystate URL list from the hosted SAML entity's configuration.
 *
 * <p>Per {@link ValidDomainExtractor#extractValidDomains(Object)} an empty collection means the configuration was
 * read and holds no <code>relayStateUrlList</code>, while <code>null</code> means it could not be read at all - the
 * configuration store is unreachable, or the role whose list applies is not one of the two an entity can host.
 * Callers that fail safe refuse to leave this server in the latter case.
 */
public class ValidRelayStateExtractor implements ValidDomainExtractor<ValidRelayStateExtractor.SAMLEntityInfo> {

    private static final Debug DEBUG = Debug.getInstance("libSAML2");

    @Override
    public Collection<String> extractValidDomains(final SAMLEntityInfo entityInfo) {
        final boolean spRole = SAML2Constants.SP_ROLE.equalsIgnoreCase(entityInfo.role);
        if (!spRole && !SAML2Constants.IDP_ROLE.equalsIgnoreCase(entityInfo.role)) {
            // An entity can host both roles, each with a relayStateUrlList of its own, so a role that could not be
            // resolved leaves it unknown which of the two lists governs the redirect. Reading it as the IdP one -
            // which is what falling through to the else branch below used to do - judges an SP flow by the wrong
            // list, so it is reported as a configuration that could not be read instead.
            DEBUG.warning("ValidRelayStateExtractor.getValidDomains: no relayStateUrlList applies to the "
                    + "unresolved role of " + entityInfo);
            return null;
        }

        try {
            final BaseConfigType config;
            final Map<String, List<String>> attrs;

            if (spRole) {
                config = getSPSSOConfig(entityInfo.realm, entityInfo.entityID);
            } else {
                config = getIDPSSOConfig(entityInfo.realm, entityInfo.entityID);
            }

            if (config == null) {
                // The entity does not host that role, so there is no list to read rather than one that could not
                // be read - the same answer as an entity that hosts the role with no relayStateUrlList in it.
                DEBUG.warning("ValidRelayStateExtractor.getValidDomains: Entity config is null for entityInfo: "
                        + entityInfo);
                return Collections.emptyList();
            }
            attrs = SAML2MetaUtils.getAttributes(config);
            if (attrs == null) {
                DEBUG.warning("ValidRelayStateExtractor.getValidDomains: Cannot find extended attributes");
                return Collections.emptyList();
            }

            final List<String> values = attrs.get(SAML2Constants.RELAY_STATE_URL_LIST);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        } catch (final SAML2MetaException sme) {
            // The configuration could not be read, which is not the same answer as an entity without a list.
            DEBUG.warning("Unable to retrieve extended configuration", sme);
            return null;
        }
        return Collections.emptyList();
    }

    /**
     * The configuration of the entity in its SP role.
     *
     * <p>A seam: the meta manager needs a configuration store behind it, which a unit test has no business
     * bootstrapping.
     *
     * @param realm The realm of the hosted entity.
     * @param entityId The entity ID of the hosted entity.
     * @return The configuration, <code>null</code> when the entity does not host that role.
     * @throws SAML2MetaException If the configuration could not be read.
     */
    BaseConfigType getSPSSOConfig(final String realm, final String entityId) throws SAML2MetaException {
        return new SAML2MetaManager().getSPSSOConfig(realm, entityId);
    }

    /**
     * The configuration of the entity in its IdP role.
     *
     * @param realm The realm of the hosted entity.
     * @param entityId The entity ID of the hosted entity.
     * @return The configuration, <code>null</code> when the entity does not host that role.
     * @throws SAML2MetaException If the configuration could not be read.
     */
    BaseConfigType getIDPSSOConfig(final String realm, final String entityId) throws SAML2MetaException {
        return new SAML2MetaManager().getIDPSSOConfig(realm, entityId);
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

        /**
         * The realm the hosted entity belongs to.
         *
         * @return The realm.
         */
        public String getRealm() {
            return realm;
        }

        /**
         * The entity ID of the hosted entity.
         *
         * @return The entity ID.
         */
        public String getEntityID() {
            return entityID;
        }

        /**
         * The role the hosted entity is acting in, which decides whose <code>relayStateUrlList</code> applies.
         *
         * @return The role, {@link SAML2Constants#SP_ROLE} or {@link SAML2Constants#IDP_ROLE}.
         */
        public String getRole() {
            return role;
        }

        @Override
        public String toString() {
            return "SAMLEntityInfo{" + "realm=" + realm + ", entityID=" + entityID + ", role=" + role + '}';
        }
    }
}
