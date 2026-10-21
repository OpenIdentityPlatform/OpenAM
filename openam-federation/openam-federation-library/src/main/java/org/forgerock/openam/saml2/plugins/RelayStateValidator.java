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
import com.sun.identity.saml2.meta.SAML2MetaManager;
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
 */
public final class RelayStateValidator {

    private static final Debug DEBUG = Debug.getInstance("libSAML2");

    /**
     * Holds the validator, which drags in the SAML2 configuration on first use.
     */
    private static final class Holder {

        private static final FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo> VALIDATOR =
                new FailSafeRedirectUrlValidator<ValidRelayStateExtractor.SAMLEntityInfo>(
                        new ValidRelayStateExtractor());
    }

    private RelayStateValidator() {
    }

    /**
     * Tells whether it is safe to redirect to the given RelayState URL.
     *
     * <p>Unlike {@link com.sun.identity.wsfederation.common.WReplyValidator}, an unknown role does not mean "both
     * whitelists": {@link ValidRelayStateExtractor} reads the IdP configuration for anything that is not
     * <code>SPRole</code>, and that contract is left as it is, because every SAML2 caller knows its own role. Name
     * the role.
     *
     * @param request The request being processed, used to establish the origin of this server.
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity; anything other than <code>SPRole</code> reads the IdP whitelist.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    public static boolean isValid(HttpServletRequest request, String realm, String entityId, String relayState,
            String role) {
        return Holder.VALIDATOR.isRedirectUrlValid(request, relayState, entityInfo(realm, entityId, role));
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
     * Tells whether it is safe to redirect to the given RelayState URL, taking the hosted entity from the provided
     * metaAlias.
     *
     * @param request The request being processed.
     * @param metaAlias The metaAlias of the hosted entity, may be <code>null</code>.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    public static boolean isValid(HttpServletRequest request, String metaAlias, String relayState, String role) {
        String realm = null;
        String entityId = null;
        if (metaAlias != null) {
            realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
            // The meta manager is left null when the SAML2Utils static initialiser fails, and an NPE out of a
            // JSP is a 500 rather than a rejected RelayState.
            SAML2MetaManager metaManager = SAML2Utils.getSAML2MetaManager();
            if (metaManager == null) {
                DEBUG.warning("RelayStateValidator.isValid(): the SAML2 meta manager is not available");
            } else {
                try {
                    entityId = metaManager.getEntityByMetaAlias(metaAlias);
                } catch (SAML2Exception se) {
                    DEBUG.warning("RelayStateValidator.isValid(): cannot resolve the entity of metaAlias "
                            + metaAlias, se);
                }
            }
        }
        return isValid(request, realm, entityId, relayState, role);
    }

    /**
     * Tells whether it is safe to redirect to the given RelayState URL, taking the hosted entity from the metaAlias
     * in the request URI, or failing that from the <code>metaAlias</code> request parameter.
     *
     * <p>The parameter fallback lets the caller choose which entity's <code>relayStateUrlList</code> is consulted,
     * which is why it exists only here and not in {@link #isValid(HttpServletRequest, String, String, String)}:
     * callers that know their hosted entity should name it. What the fallback can reach is bounded by what an
     * administrator has whitelisted somewhere - an entity the caller invents has no list, which leaves only URLs
     * pointing back at this server.
     *
     * @param request The request being processed.
     * @param relayState The URL to validate.
     * @param role The role of the hosted entity.
     * @return <code>true</code> if the RelayState URL may be used in a redirect.
     */
    public static boolean isValidForRequest(HttpServletRequest request, String relayState, String role) {
        String metaAlias = SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI());
        if (metaAlias == null) {
            metaAlias = request.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        }
        return isValid(request, metaAlias, relayState, role);
    }
}
