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
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.openam.federation.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;

/**
 * Extracts the valid <code>RelayState</code> URL list from a hosted ID-FF entity's
 * extended metadata. Mirrors {@code org.forgerock.openam.saml2.plugins.ValidRelayStateExtractor}
 * for the ID-FF protocol.
 * <p>
 * A <code>null</code> return means "this entity configures no allow-list", which the
 * caller is free to read as "unrestricted". "The allow-list could not be read" is a
 * different answer and must never be confused with the first one, so it is signalled
 * with {@link ConfigurationUnavailableException} instead: a directory outage must not
 * silently open the allow-list of an entity that does configure one.
 */
public class ValidRelayStateExtractor
        implements ValidDomainExtractor<ValidRelayStateExtractor.IDFFEntityInfo> {

    private final Supplier<IDFFMetaManager> metaManagerSupplier;

    public ValidRelayStateExtractor() {
        // Resolved per call rather than cached: FSUtils.getIDFFMetaManager() retries
        // after a transient failure, so an extractor built while the SMS was still
        // unreachable must not keep answering with a null manager until a restart.
        this(FSUtils::getIDFFMetaManager);
    }

    ValidRelayStateExtractor(final IDFFMetaManager metaManager) {
        this(() -> metaManager);
    }

    ValidRelayStateExtractor(final Supplier<IDFFMetaManager> metaManagerSupplier) {
        this.metaManagerSupplier = metaManagerSupplier;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ConfigurationUnavailableException if the entity's allow-list could not be
     *  read. Callers must treat this as "reject", never as "no allow-list configured".
     */
    @Override
    public Collection<String> extractValidDomains(final IDFFEntityInfo entityInfo) {
        final IDFFMetaManager metaManager = metaManagerSupplier.get();
        if (metaManager == null) {
            throw new ConfigurationUnavailableException(
                "IDFF meta manager is unavailable for " + entityInfo, null);
        }

        final BaseConfigType config;
        try {
            if (IFSConstants.SP.equalsIgnoreCase(entityInfo.role)) {
                config = metaManager.getSPDescriptorConfig(entityInfo.realm, entityInfo.entityID);
            } else {
                config = metaManager.getIDPDescriptorConfig(entityInfo.realm, entityInfo.entityID);
            }
        } catch (final IDFFMetaException ime) {
            throw new ConfigurationUnavailableException(
                "unable to retrieve extended configuration for " + entityInfo, ime);
        } catch (final RuntimeException re) {
            // the SMS/JAXB layer throws unchecked too (a null realm, a cache miss);
            // an unchecked failure is still a failed read, not an empty allow-list
            throw new ConfigurationUnavailableException(
                "unable to retrieve extended configuration for " + entityInfo, re);
        }

        if (config == null) {
            // The role resolved from the metaAlias but carries no extended config:
            // the metadata is inconsistent, so we cannot claim the list is empty.
            throw new ConfigurationUnavailableException(
                "entity config is null for " + entityInfo, null);
        }

        final Map attrs = IDFFMetaUtils.getAttributes(config);
        if (attrs != null) {
            final Collection<String> values =
                toPatterns(attrs.get(IFSConstants.RELAY_STATE_URL_LIST), entityInfo);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        }
        return null;
    }

    /**
     * Reads the allow-list out of the raw attribute map. The value crosses an untyped
     * {@code Map} from the JAXB layer, so an unchecked cast would surface a
     * {@code ClassCastException} from inside {@code RedirectUrlValidator} as a bare
     * {@code RuntimeException} - losing the "could not read" / "not configured" distinction
     * this class exists to preserve.
     *
     * @param value the raw attribute value, may be <code>null</code>.
     * @param entityInfo the entity being read, for the failure message.
     * @return the patterns, or <code>null</code> when the attribute is absent.
     * @throws ConfigurationUnavailableException if the value is not a collection of strings.
     */
    private static Collection<String> toPatterns(
        final Object value, final IDFFEntityInfo entityInfo) {

        if (value == null) {
            return null;
        }
        if (!(value instanceof Collection)) {
            throw new ConfigurationUnavailableException(IFSConstants.RELAY_STATE_URL_LIST
                + " is a " + value.getClass().getName() + ", not a collection, for "
                + entityInfo, null);
        }
        final List<String> patterns = new ArrayList<String>();
        for (final Object element : (Collection<?>) value) {
            if (!(element instanceof String)) {
                throw new ConfigurationUnavailableException(IFSConstants.RELAY_STATE_URL_LIST
                    + " holds a " + (element == null ? "null" : element.getClass().getName())
                    + ", not a String, for " + entityInfo, null);
            }
            patterns.add((String) element);
        }
        return patterns;
    }

    /**
     * Signals that the allow-list of an entity could not be read. Distinct from a
     * <code>null</code> result, which means the entity configures no allow-list.
     */
    public static final class ConfigurationUnavailableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        ConfigurationUnavailableException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * A domain object that uniquely identifies a hosted ID-FF entity in the configuration.
     */
    public static final class IDFFEntityInfo {

        private final String realm;
        private final String entityID;
        private final String role;

        private IDFFEntityInfo(final String realm, final String entityID, final String role) {
            this.realm = realm;
            this.entityID = entityID;
            this.role = role;
        }

        /**
         * Creates a new IDFFEntityInfo based on the provided details.
         *
         * @param realm The realm where the hosted entity belongs to.
         * @param entityID The entity ID of the hosted entity.
         * @param role The role of the hosted entity ({@code IFSConstants.SP} or {@code IFSConstants.IDP}).
         * @return The IDFFEntityInfo corresponding to the provided details.
         */
        public static IDFFEntityInfo from(final String realm, final String entityID, final String role) {
            return new IDFFEntityInfo(realm, entityID, role);
        }

        @Override
        public String toString() {
            return "IDFFEntityInfo{" + "realm=" + realm + ", entityID=" + entityID + ", role=" + role + '}';
        }
    }
}
