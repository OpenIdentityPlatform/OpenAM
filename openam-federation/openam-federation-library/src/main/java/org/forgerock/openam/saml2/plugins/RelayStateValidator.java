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
import com.sun.identity.shared.debug.Debug;
import jakarta.servlet.http.HttpServletRequest;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.utils.StringUtils;

/**
 * Validates the RelayState URLs that the SAML2 JSPs receive as request parameters.
 *
 * <p>This validator does not treat a missing <code>relayStateUrlList</code> as permission to redirect anywhere: with
 * no whitelist configured it accepts only URLs pointing back at this server.
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
     * Holds the default instance, which drags in the SAML2 configuration on first use.
     */
    private static final class Holder {

        private static final RelayStateValidator INSTANCE = new RelayStateValidator(
                new FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo>(
                        new ValidRelayStateExtractor()),
                new EntityResolver() {

                    @Override
                    public String getEntityByMetaAlias(final String metaAlias) throws SAML2Exception {
                        return SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(metaAlias);
                    }
                });
    }

    private final FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo> validator;

    private final EntityResolver entityResolver;

    RelayStateValidator(final FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo> validator,
            final EntityResolver entityResolver) {
        this.validator = validator;
        this.entityResolver = entityResolver;
    }

    /**
     * Tells whether it is safe to redirect to the given RelayState URL.
     *
     * <p>Unlike {@link com.sun.identity.wsfederation.common.WReplyValidator}, an unknown role does not mean "both
     * whitelists": {@link ValidRelayStateExtractor} reads the IdP configuration for anything that is not
     * <code>SPRole</code>, and that contract is left as it is, because every SAML2 caller knows its own role. Name
     * the role.
     *
     * @param request The request being processed, consulted as a secondary view of this server's origin.
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity; anything other than <code>SPRole</code> reads the IdP whitelist.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    public static boolean isValidForEntity(HttpServletRequest request, String realm, String entityId,
            String relayState, String role) {
        return Holder.INSTANCE.validate(request, realm, entityId, relayState, role);
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
        return Holder.INSTANCE.validateForMetaAlias(request, metaAlias, relayState, role);
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
        return Holder.INSTANCE.validateForRequest(request, relayState, role);
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
     * {@link ValidRelayStateExtractor} reads the IdP configuration for anything that is not <code>SPRole</code>,
     * and that contract belongs to the extractor.
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
