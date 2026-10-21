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
package com.sun.identity.wsfederation.common;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.plugins.whitelist.ValidWReplyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.utils.StringUtils;

/**
 * Validates the wreply URLs that the WS-Federation endpoints receive as request parameters.
 *
 * <p>This validator does not treat a missing <code>wreplyList</code> as permission to redirect anywhere: with no
 * whitelist configured it accepts only URLs pointing back at this server. It also works for endpoints that are not
 * deployed under a metaAlias, where the hosted entity cannot be resolved from the request URI and no whitelist is
 * consulted at all - see
 * {@link #pointsBackAtThisServer(HttpServletRequest, String)}.
 */
public final class WReplyValidator {

    private static final Debug DEBUG = Debug.getInstance(WSFederationConstants.AM_WSFEDERATION);

    /**
     * Holds the validator, which drags in the WS-Federation configuration on first use.
     */
    private static final class Holder {

        private static final FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo> VALIDATOR =
                new FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo>(
                        new ValidWReplyExtractor());
    }

    private WReplyValidator() {
    }

    /**
     * Tells whether the given wreply URL points back at this server, consulting no <code>wreplyList</code> at all.
     *
     * <p>This is what an endpoint whose hosted entity is named by the caller has to use. <code>multi.jsp</code>
     * carries no metaAlias - <code>logout.jsp</code> names the entity in its query string - and it answers an
     * unauthenticated request, so the realm and the entity ID it receives are whatever the caller sent. Letting
     * them select the whitelist would mean any hosted entity's <code>wreplyList</code> could authorise a redirect
     * issued for another entity, in another realm; naming no role on top of that would merge that entity's IdP
     * and RP lists. Untrusted input picks no whitelist here, so only this deployment's own URLs are accepted.
     *
     * @param request The request being processed, used to establish the origin of this server.
     * @param wreply The URL to validate.
     * @return <code>true</code> if the wreply URL points back at this server.
     */
    public static boolean pointsBackAtThisServer(HttpServletRequest request, String wreply) {
        return Holder.VALIDATOR.isRedirectUrlValid(request, wreply,
                Collections.<ValidWReplyExtractor.WSFederationEntityInfo>emptyList());
    }

    /**
     * Tells whether it is safe to redirect to the given wreply URL, consulting the whitelist of one role.
     *
     * @param request The request being processed, used to establish the origin of this server.
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param wreply The URL to validate.
     * @param role The role the endpoint was reached for; when <code>null</code> or unknown the whitelists of
     * both roles are honoured, because there is nothing to choose between them.
     * @return <code>true</code> if the wreply URL may be used in a redirect.
     */
    public static boolean isValid(HttpServletRequest request, String realm, String entityId, String wreply,
            String role) {
        return Holder.VALIDATOR.isRedirectUrlValid(request, wreply, entityInfos(realm, entityId, role));
    }

    /**
     * Tells whether it is safe to redirect to the given wreply URL, taking the hosted entity from the metaAlias in
     * the request URI.
     *
     * <p>There is deliberately no fallback to a <code>metaAlias</code> request parameter here: the WS-Federation
     * endpoints reached through this overload carry their metaAlias in the URI, so letting the caller name the
     * entity whose <code>wreplyList</code> is consulted would only add a choice it does not need.
     *
     * @param request The request being processed.
     * @param wreply The URL to validate.
     * @return <code>true</code> if the wreply URL may be used in a redirect.
     */
    public static boolean isValid(HttpServletRequest request, String wreply) {
        if (StringUtils.isEmpty(wreply)) {
            // WSFederationActionFactory asks about every request, most of which carry no wreply at all, and
            // resolving the hosted entity below costs three configuration lookups.
            return false;
        }
        String realm = null;
        String entityId = null;
        String role = null;
        String metaAlias = WSFederationMetaUtils.getMetaAliasByUri(request.getRequestURI());
        if (metaAlias != null) {
            realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
            try {
                WSFederationMetaManager metaManager = WSFederationUtils.getMetaManager();
                entityId = metaManager.getEntityByMetaAlias(metaAlias);
                role = metaManager.getRoleByMetaAlias(metaAlias);
            } catch (WSFederationMetaException wsfe) {
                DEBUG.warning("WReplyValidator.isValid(): cannot resolve the entity of metaAlias " + metaAlias, wsfe);
            }
        }
        return isValid(request, realm, entityId, wreply, role);
    }

    /**
     * Names the configurations whose whitelist applies. A caller that knows which role the endpoint was reached
     * for gets only that one; a caller that can not tell - the entity of a WS-Federation signout may be hosted
     * as the IdP or as the RP - gets both, since picking whichever role happens to be configured first would be
     * arbitrary rather than safer. This only ever applies to an entity resolved from a metaAlias: an entity the
     * caller names is not trustworthy enough to select a whitelist at all, which is what
     * {@link #pointsBackAtThisServer(HttpServletRequest, String)} is for.
     *
     * <p>Package private so that the role selection can be tested without a live configuration store.
     *
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param role The role the endpoint was reached for, may be <code>null</code> or unknown.
     * @return The configurations to read the whitelist from, empty when there is no hosted entity to read it for.
     */
    static List<ValidWReplyExtractor.WSFederationEntityInfo> entityInfos(String realm, String entityId,
            String role) {
        if (StringUtils.isEmpty(realm) || StringUtils.isEmpty(entityId)) {
            return Collections.emptyList();
        }
        if (SAML2Constants.IDP_ROLE.equalsIgnoreCase(role) || SAML2Constants.SP_ROLE.equalsIgnoreCase(role)) {
            return Collections.singletonList(
                    ValidWReplyExtractor.WSFederationEntityInfo.from(realm, entityId, role));
        }
        return Arrays.asList(
                ValidWReplyExtractor.WSFederationEntityInfo.from(realm, entityId, SAML2Constants.IDP_ROLE),
                ValidWReplyExtractor.WSFederationEntityInfo.from(realm, entityId, SAML2Constants.SP_ROLE));
    }
}
