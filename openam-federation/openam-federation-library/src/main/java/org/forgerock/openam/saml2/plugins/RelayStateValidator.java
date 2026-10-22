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
package org.forgerock.openam.saml2.plugins;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.forgerock.openam.utils.StringUtils;

/**
 * Validates the RelayState URLs that the SAML2 JSPs receive as request parameters.
 *
 * <p>This validator does not treat a missing <code>relayStateUrlList</code> as permission to redirect anywhere: with
 * no whitelist configured it accepts only URLs pointing back at this server.
 *
 * <p>Deployments that need cross domain RelayState URLs and cannot populate the list of every hosted entity can set
 * {@link Constants#FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST} to restore the old behaviour. The property is read on
 * every call, so it can be changed without a restart, and it only decides what an <em>empty</em> list means: a
 * populated list rules either way, and so does a list that could not be read - a configuration store that cannot be
 * reached, or a metaAlias whose entity could not be resolved, is never taken for an entity without a list.
 *
 * <p>The static methods are the entry points the JSPs use. They delegate to an instance holding the collaborators,
 * which the tests build with stubs of their own.
 */
public final class RelayStateValidator {

    private static final Debug DEBUG = Debug.getInstance("libSAML2");

    /**
     * Resolves the hosted entity a metaAlias belongs to.
     */
    interface EntityResolver {

        /**
         * Returns the entity ID the metaAlias belongs to.
         *
         * @param metaAlias The metaAlias to resolve, never <code>null</code>.
         * @return The entity ID, or <code>null</code> when the metaAlias belongs to no hosted entity.
         * @throws SAML2Exception If the metadata could not be read.
         */
        String getEntityByMetaAlias(String metaAlias) throws SAML2Exception;
    }

    /**
     * Resolves the hosted entity a metaAlias belongs to through the SAML2 meta manager.
     *
     * <p>A seam: the meta manager needs a configuration store behind it, which a unit test has no business
     * bootstrapping.
     */
    static class HostedEntities implements EntityResolver {

        @Override
        public final String getEntityByMetaAlias(final String metaAlias) throws SAML2Exception {
            return entityId(metaAlias);
        }

        /**
         * The entity ID the metaAlias belongs to.
         *
         * @param metaAlias The metaAlias of the hosted entity.
         * @return The entity ID.
         * @throws SAML2Exception If the entity could not be resolved.
         */
        String entityId(String metaAlias) throws SAML2Exception {
            return SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
        }
    }

    /**
     * Holds the default instance, which drags in the SAML2 configuration on first use.
     */
    private static final class Holder {

        private static final RelayStateValidator DEFAULT =
                new RelayStateValidator(new ValidRelayStateExtractor(), new HostedEntities());

        private static volatile RelayStateValidator instance = DEFAULT;
    }

    /**
     * Test seam: puts the given validator behind the static entry points, so that a test can see the realm, entity
     * and role they forward. <code>null</code> restores the validator that reads the configuration.
     *
     * @param replacement The validator to use, or <code>null</code>.
     */
    static void setInstance(RelayStateValidator replacement) {
        Holder.instance = replacement == null ? Holder.DEFAULT : replacement;
    }

    /**
     * The RelayState validator, with the escape hatch for deployments that cannot populate a
     * <code>relayStateUrlList</code>.
     */
    private static final class RelayStateUrlValidator
            extends FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo> {

        RelayStateUrlValidator(ValidDomainExtractor<ValidRelayStateExtractor.SAMLEntityInfo> extractor) {
            super(extractor);
        }

        @Override
        protected boolean allowAnyUrlWithoutWhitelist() {
            // Read on every call, so it can be changed without a restart. The validator only asks once it knows
            // the entity has no list, so a populated one - and a list that could not be read - still rules.
            return SystemPropertiesManager.getAsBoolean(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, false);
        }
    }

    private final FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo> validator;

    private final EntityResolver entityResolver;

    RelayStateValidator(final FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo> validator,
            final EntityResolver entityResolver) {
        this.validator = validator;
        this.entityResolver = entityResolver;
    }

    /**
     * Creates a validator reading the RelayState list through the provided extractor, with the escape hatch the
     * production instance carries. Package private: production reaches the configuration through a single shared
     * instance, tests supply their own.
     *
     * @param extractor The extractor of the configured RelayState patterns.
     * @param entityResolver The resolver of the hosted entity behind a metaAlias.
     */
    RelayStateValidator(final ValidDomainExtractor<ValidRelayStateExtractor.SAMLEntityInfo> extractor,
            final EntityResolver entityResolver) {
        this(new RelayStateUrlValidator(extractor), entityResolver);
    }

    /**
     * Tells whether it is safe to redirect to the given RelayState URL.
     *
     * <p>Unlike {@link com.sun.identity.wsfederation.common.WReplyValidator}, an unknown role does not mean "both
     * whitelists": {@link ValidRelayStateExtractor} reports a role that is neither <code>SPRole</code> nor
     * <code>IDPRole</code> as a configuration that could not be read, because every SAML2 caller knows its own role.
     * Name the role.
     *
     * @param request The request being processed, consulted as a secondary view of this server's origin.
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity, <code>SPRole</code> or <code>IDPRole</code>.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    public static boolean isValidForEntity(HttpServletRequest request, String realm, String entityId,
            String relayState, String role) {
        return Holder.instance.validate(request, realm, entityId, relayState, role);
    }

    /**
     * Tells whether it is safe to redirect to the given RelayState URL, taking the hosted entity from the provided
     * metaAlias.
     *
     * @param request The request being processed.
     * @param metaAlias The metaAlias of the hosted entity, may be <code>null</code>.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    public static boolean isValidForMetaAlias(HttpServletRequest request, String metaAlias, String relayState,
            String role) {
        return Holder.instance.validateForMetaAlias(request, metaAlias, relayState, role);
    }

    /**
     * Tells whether it is safe to redirect to the given RelayState URL, taking the hosted entity from the metaAlias
     * in the request URI. A request naming none is measured against this server's own origin; the
     * <code>metaAlias</code> request parameter is deliberately not consulted, so that the caller cannot choose
     * which hosted entity's whitelist judges their redirect.
     *
     * @param request The request being processed.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    public static boolean isValidForRequest(HttpServletRequest request, String relayState, String role) {
        return Holder.instance.validateForRequest(request, relayState, role);
    }

    /**
     * Validates the RelayState against the whitelist of the given hosted entity.
     *
     * @param request The request being processed.
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    boolean validate(final HttpServletRequest request, final String realm, final String entityId,
            final String relayState, final String role) {
        final ValidRelayStateExtractor.SAMLEntityInfo entityInfo = entityInfo(realm, entityId, role);
        if (entityInfo == null) {
            // No hosted entity means no relayStateUrlList to consult, so the origin of this server is all that is
            // left to decide. The RelayState itself stays out of the message: it has not been accepted yet, and a
            // value carrying CRLF would forge lines in the plain text debug file.
            DEBUG.message("RelayStateValidator: no hosted entity to read a relayStateUrlList from, "
                    + "the RelayState is only accepted when it points back at this server");
        }
        return validator.isRedirectUrlValid(request, relayState, entityInfo);
    }

    /**
     * Names the configuration whose whitelist applies. The role is carried through rather than resolved here:
     * {@link ValidRelayStateExtractor} reports a role that is neither <code>SPRole</code> nor <code>IDPRole</code>
     * as a configuration that could not be read, and that contract belongs to the extractor.
     *
     * <p>Package private so that the selection can be tested without a live configuration store.
     *
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param role The role of the hosted entity, may be <code>null</code>.
     * @return The configuration to read the whitelist from, <code>null</code> when there is no hosted entity to
     * read it for.
     */
    static ValidRelayStateExtractor.SAMLEntityInfo entityInfo(String realm, String entityId, String role) {
        if (StringUtils.isEmpty(realm) || StringUtils.isEmpty(entityId)) {
            return null;
        }
        return ValidRelayStateExtractor.SAMLEntityInfo.from(realm, entityId, role);
    }

    /**
     * Validates the RelayState against the whitelist of the entity the metaAlias belongs to.
     *
     * @param request The request being processed.
     * @param metaAlias The metaAlias of the hosted entity, may be <code>null</code>.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    boolean validateForMetaAlias(final HttpServletRequest request, final String metaAlias, final String relayState,
            final String role) {
        if (StringUtils.isEmpty(relayState)) {
            // There is nothing to redirect to, which the validator answers the same way. Saying so here keeps the
            // metadata of the hosted entity unread on the requests that carry no RelayState at all, which is most
            // of them.
            return false;
        }
        String realm = null;
        String entityId = null;
        if (metaAlias != null) {
            realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            try {
                entityId = entityResolver.getEntityByMetaAlias(metaAlias);
            } catch (SAML2Exception se) {
                // The whitelist of the hosted entity can not be read, so there is nothing left to validate against.
                DEBUG.warning("RelayStateValidator: cannot resolve the entity the metaAlias names", se);
                return false;
            } catch (RuntimeException re) {
                // Metadata that is present but incomplete surfaces here rather than as a checked exception. It
                // leaves just as little to validate against, so it is answered the same way instead of escaping
                // as a 500.
                DEBUG.warning("RelayStateValidator: cannot resolve the entity the metaAlias names", re);
                return false;
            }
            if (StringUtils.isEmpty(entityId)) {
                // The meta manager answers null rather than throwing for a metaAlias no hosted entity is deployed
                // under. The request still names one, so this is not an endpoint without a list: it is judged like
                // an entity whose list could not be read, or the escape hatch would read it as one without a list.
                DEBUG.warning("RelayStateValidator: no hosted entity is deployed under the metaAlias");
                return validator.isOwnOriginRedirectUrl(request, relayState);
            }
        }
        return validate(request, realm, entityId, relayState, role);
    }

    /**
     * Validates the RelayState against the whitelist of the entity addressed by the request.
     *
     * @param request The request being processed.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    boolean validateForRequest(final HttpServletRequest request, final String relayState, final String role) {
        // Only the metaAlias the endpoint was addressed with is honoured. The request parameter of the same
        // name used to stand in for it, but every endpoint left on this method is deployed under a metaAlias in
        // its path - CreateSAML2HostedProviderTemplate writes the SLO and MNI locations that way - so the
        // parameter could only ever let a caller name a different hosted entity and have that entity's
        // relayStateUrlList judge their redirect. An endpoint carrying no metaAlias falls back to the origin.
        return validateForMetaAlias(request, SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI()), relayState,
                role);
    }
}
