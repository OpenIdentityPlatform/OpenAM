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
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.plugins.whitelist.ValidWReplyExtractor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.forgerock.openam.shared.security.whitelist.FailSafeRedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.forgerock.openam.utils.StringUtils;

/**
 * Validates the wreply URLs that the WS-Federation endpoints receive as request parameters.
 *
 * <p>This validator does not treat a missing <code>wreplyList</code> as permission to redirect anywhere: with no
 * whitelist configured it accepts only URLs pointing back at this server. It also works for endpoints that are not
 * deployed under a metaAlias, where the hosted entity cannot be resolved from the request URI and no whitelist is
 * consulted at all - see
 * {@link #pointsBackAtThisServer(HttpServletRequest, String)}.
 *
 * <p>Deployments that need cross domain wreply URLs and cannot populate the list of every hosted entity can set
 * {@link Constants#FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST} to restore the old behaviour. The property is read on
 * every call, so it can be changed without a restart, and it only decides what an <em>empty</em> list means: a
 * populated list rules either way, and so does a list that could not be read - a configuration store that cannot be
 * reached, or a metaAlias whose entity could not be resolved, is never taken for an entity without a list.
 *
 * <p>The static methods are the entry points the JSPs and the servlets use. They delegate to an instance holding the
 * collaborators, which the tests build with stubs of their own.
 */
public final class WReplyValidator {

    private static final Debug DEBUG = Debug.getInstance(WSFederationConstants.AM_WSFEDERATION);

    /**
     * Resolves the hosted entity a metaAlias belongs to.
     */
    interface EntityResolver {

        /**
         * Returns the entity ID the metaAlias belongs to.
         *
         * @param metaAlias The metaAlias to resolve, never <code>null</code>.
         * @return The entity ID, or <code>null</code> when the metaAlias belongs to no hosted entity.
         * @throws WSFederationMetaException If the metadata could not be read.
         */
        String getEntityByMetaAlias(String metaAlias) throws WSFederationMetaException;

        /**
         * Returns the role the metaAlias is deployed under.
         *
         * @param metaAlias The metaAlias to resolve, never <code>null</code>.
         * @return The role, or {@link SAML2Constants#UNKNOWN_ROLE} when it can not be told apart.
         * @throws WSFederationMetaException If the metadata could not be read.
         */
        String getRoleByMetaAlias(String metaAlias) throws WSFederationMetaException;
    }

    /**
     * Resolves the hosted entity a metaAlias belongs to, and the role it is deployed for, through the WS-Federation
     * meta manager.
     *
     * <p>A seam: the meta manager needs a configuration store behind it, which a unit test has no business
     * bootstrapping.
     */
    static class HostedEntities implements EntityResolver {

        @Override
        public final String getEntityByMetaAlias(final String metaAlias) throws WSFederationMetaException {
            return entityId(metaAlias);
        }

        @Override
        public final String getRoleByMetaAlias(final String metaAlias) throws WSFederationMetaException {
            return role(metaAlias);
        }

        /**
         * The entity ID the metaAlias belongs to.
         *
         * @param metaAlias The metaAlias of the hosted entity.
         * @return The entity ID.
         * @throws WSFederationMetaException If the entity could not be resolved.
         */
        String entityId(String metaAlias) throws WSFederationMetaException {
            return WSFederationUtils.getMetaManager().getEntityByMetaAlias(metaAlias);
        }

        /**
         * The role the metaAlias is deployed for.
         *
         * @param metaAlias The metaAlias of the hosted entity.
         * @return The role, {@link SAML2Constants#IDP_ROLE} or {@link SAML2Constants#SP_ROLE}.
         * @throws WSFederationMetaException If the role could not be resolved.
         */
        String role(String metaAlias) throws WSFederationMetaException {
            return WSFederationUtils.getMetaManager().getRoleByMetaAlias(metaAlias);
        }
    }

    /**
     * Holds the default instance, which drags in the WS-Federation configuration on first use.
     */
    private static final class Holder {

        private static final WReplyValidator DEFAULT =
                new WReplyValidator(new ValidWReplyExtractor(), new HostedEntities());

        private static volatile WReplyValidator instance = DEFAULT;
    }

    /**
     * Test seam: puts the given validator behind the static entry points, so that a test can see the realm, entity
     * and role they forward. <code>null</code> restores the validator that reads the configuration.
     *
     * @param replacement The validator to use, or <code>null</code>.
     */
    static void setInstance(WReplyValidator replacement) {
        Holder.instance = replacement == null ? Holder.DEFAULT : replacement;
    }

    /**
     * The wreply validator, with the escape hatch for deployments that cannot populate a <code>wreplyList</code>.
     */
    private static final class WReplyUrlValidator
            extends FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo> {

        WReplyUrlValidator(ValidDomainExtractor<ValidWReplyExtractor.WSFederationEntityInfo> extractor) {
            super(extractor);
        }

        @Override
        protected boolean allowAnyUrlWithoutWhitelist() {
            // Read on every call, so it can be changed without a restart. The validator only asks once it knows
            // the entity has no list, so a populated one - and a list that could not be read - still rules.
            return SystemPropertiesManager.getAsBoolean(Constants.FEDERATION_ALLOW_ANY_URL_WITHOUT_WHITELIST, false);
        }
    }

    private final FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo> validator;

    private final EntityResolver entityResolver;

    WReplyValidator(final FailSafeRedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo> validator,
            final EntityResolver entityResolver) {
        this.validator = validator;
        this.entityResolver = entityResolver;
    }

    /**
     * Creates a validator reading the wreply list through the provided extractor, with the escape hatch the
     * production instance carries. Package private: production reaches the configuration through a single shared
     * instance, tests supply their own.
     *
     * @param extractor The extractor of the configured wreply patterns.
     * @param entityResolver The resolver of the hosted entity behind a metaAlias.
     */
    WReplyValidator(final ValidDomainExtractor<ValidWReplyExtractor.WSFederationEntityInfo> extractor,
            final EntityResolver entityResolver) {
        this(new WReplyUrlValidator(extractor), entityResolver);
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
        return Holder.instance.validate(request, null, null, wreply, null);
    }

    /**
     * Tells whether it is safe to redirect to the given wreply URL, consulting only the whitelist of the given role.
     *
     * @param request The request being processed, consulted as a secondary view of this server's origin, may be
     * <code>null</code>.
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param wreply The URL to validate.
     * @param role The role serving this endpoint, {@link SAML2Constants#UNKNOWN_ROLE} or <code>null</code> when the
     * caller can not tell, in which case both roles are consulted.
     * @return <code>true</code> if the wreply URL may be used in a redirect.
     */
    public static boolean isValidForEntity(HttpServletRequest request, String realm, String entityId, String wreply,
            String role) {
        return Holder.instance.validate(request, realm, entityId, wreply, role);
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
    public static boolean isValidForRequest(HttpServletRequest request, String wreply) {
        return Holder.instance.validateForRequest(request, wreply);
    }

    /**
     * Tells whether it is safe to redirect to the given wreply URL, taking the hosted entity from the provided
     * metaAlias.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param metaAlias The metaAlias of the hosted entity, may be <code>null</code>.
     * @param wreply The URL to validate.
     * @return <code>true</code> if the wreply URL may be used in a redirect.
     */
    public static boolean isValidForMetaAlias(HttpServletRequest request, String metaAlias, String wreply) {
        return Holder.instance.validateForMetaAlias(request, metaAlias, wreply);
    }

    /**
     * Validates the wreply against the whitelist of the given hosted entity.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param realm The realm of the hosted entity, may be <code>null</code>.
     * @param entityId The entity ID of the hosted entity, may be <code>null</code>.
     * @param wreply The URL to validate.
     * @param role The role serving this endpoint, <code>null</code> when the caller can not tell, in which case
     * both roles are consulted.
     * @return <code>true</code> if the wreply URL may be used in a redirect.
     */
    boolean validate(final HttpServletRequest request, final String realm, final String entityId, final String wreply,
            final String role) {
        return validator.isRedirectUrlValid(request, wreply, entityInfos(realm, entityId, role));
    }

    /**
     * Validates the wreply against the whitelist of the entity the metaAlias belongs to.
     *
     * @param request The request being processed, may be <code>null</code>.
     * @param metaAlias The metaAlias of the hosted entity, may be <code>null</code>.
     * @param wreply The URL to validate.
     * @return <code>true</code> if the wreply URL may be used in a redirect.
     */
    boolean validateForMetaAlias(final HttpServletRequest request, final String metaAlias, final String wreply) {
        if (StringUtils.isEmpty(wreply)) {
            // There is nothing to redirect to, which the validator answers the same way. Saying so here keeps the
            // metadata of the hosted entity unread on the requests that carry no wreply at all - the overwhelming
            // majority, since WSFederationActionFactory asks about every WS-Federation request it serves.
            return false;
        }
        String realm = null;
        String entityId = null;
        String role = null;
        if (metaAlias != null) {
            realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
            try {
                entityId = entityResolver.getEntityByMetaAlias(metaAlias);
                if (StringUtils.isEmpty(entityId)) {
                    // The meta manager answers null rather than throwing for a metaAlias no hosted entity is
                    // deployed under. The request still names one, so this is not an endpoint without a list: it is
                    // judged like an entity whose list could not be read, or the escape hatch would read it as one
                    // without a list.
                    DEBUG.warning("WReplyValidator: no hosted entity is deployed under the metaAlias");
                    return validator.isOwnOriginRedirectUrl(request, wreply);
                }
                // The metaAlias names one role, so only that role's whitelist is consulted. Taking both in as
                // well would let an IDP wreplyList widen what the SP list actually serving the endpoint allows.
                role = entityResolver.getRoleByMetaAlias(metaAlias);
            } catch (WSFederationMetaException wsfe) {
                // The whitelist of the hosted entity can not be read, so there is nothing left to validate against.
                DEBUG.warning("WReplyValidator: cannot resolve the entity the metaAlias names", wsfe);
                return false;
            } catch (RuntimeException re) {
                // Metadata that is present but incomplete surfaces here rather than as a checked exception. It
                // leaves just as little to validate against, so it is answered the same way instead of escaping
                // as a 500.
                DEBUG.warning("WReplyValidator: cannot resolve the entity the metaAlias names", re);
                return false;
            }
        }
        return validate(request, realm, entityId, wreply, role);
    }

    /**
     * Validates the wreply against the whitelist of the entity addressed by the request.
     *
     * @param request The request being processed.
     * @param wreply The URL to validate.
     * @return <code>true</code> if the wreply URL may be used in a redirect.
     */
    boolean validateForRequest(final HttpServletRequest request, final String wreply) {
        return validateForMetaAlias(request, WSFederationMetaUtils.getMetaAliasByUri(request.getRequestURI()), wreply);
    }

    /**
     * Names the configurations the whitelist may be read from. When the caller knows which role serves the endpoint
     * only that one is consulted; otherwise both are and their whitelists are taken together, so that a dual role
     * entity does not have the list of one role shadow the other's. This only ever applies to an entity resolved
     * from a metaAlias or named by trusted code: an entity the caller names is not trustworthy enough to select a
     * whitelist at all, which is what {@link #pointsBackAtThisServer(HttpServletRequest, String)} is for.
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
