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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.forgerockrest.authn;

import static org.forgerock.openam.audit.AuditConstants.*;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.audit.AbstractRestletAccessAuditFilter;
import org.forgerock.openam.utils.StringUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;

/**
 * Responsible for logging access audit events for UMA requests.
 *
 * @since 13.0.0
 */
public class UMAAccessAuditFilter extends AbstractRestletAccessAuditFilter {

    private static final Debug debug = Debug.getInstance("amAudit");
    private final OAuth2RequestFactory<Request> requestFactory;
    private final TokenStore tokenStore;

    /**
     * Create a new filter for the given component and restlet.
     *
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     * @param tokenStore The helper to use for reading authentication JWTs.
     * @param requestFactory The factory for creating OAuth2Request instances.
     */
    public UMAAccessAuditFilter(Restlet restlet, AuditEventPublisher auditEventPublisher,
                                AuditEventFactory auditEventFactory, TokenStore tokenStore,
                                OAuth2RequestFactory<Request> requestFactory) {
        super(Component.UMA, restlet, auditEventPublisher, auditEventFactory);
        this.tokenStore = tokenStore;
        this.requestFactory = requestFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUserIdForAccessAttempt(Request request) {
        String userId = super.getUserIdForAccessAttempt(request);

        if (StringUtils.isNotEmpty(userId)) {
            return userId;
        }

        AccessToken accessToken = retrieveAccessToken(request);

        if (accessToken == null) {
            //No token, therefore UMA endpoint with no need for auth, failure to send bearer token in header, or
            //something went wrong during retrieval. Can't get user id, bail out.
            debug.message("org.forgerock.openam.forgerockrest.authn.UMAAccessAuditFilter.getUserIdForAccessAttempt: " +
                    "Unable to retrieve user id from access token.");
            return "";
        }

        String username = getAccessTokenProperty("userName", accessToken);
        String realm = getAccessTokenProperty("realm", accessToken);

        if (username == null || realm == null) {
            return "";
        }

        CoreWrapper cw = new CoreWrapper();
        AMIdentity identity = cw.getIdentity(username, realm);
        userId = identity.getUniversalId();

        AuditRequestContext.putProperty(USER_ID, userId);

        return userId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getContextIdForAccessAttempt(Request request) {
        String contextId = super.getContextIdForAccessAttempt(request);

        if (contextId != null) {
            return contextId;
        }

        AccessToken accessToken = retrieveAccessToken(request);

        contextId = generateContextID(accessToken);

        AuditRequestContext.putProperty(CONTEXT_ID, contextId);

        return contextId;
    }

    private String generateContextID(AccessToken accessToken) {

        String contextId = null;

        if (accessToken != null) {
            contextId = getAccessTokenProperty("id", accessToken);
        }

        return contextId;
    }

    // Currently only handles the type of properties I know to be in there, and which are of interest.
    private String getAccessTokenProperty(String propertyName, AccessToken accessToken) {
        if (!accessToken.isDefined(propertyName)) {
            debug.message("org.forgerock.openam.forgerockrest.authn.UMAAccessAuditFilter.getAccessTokenProperty: " +
                    "Property {} not present in access token.", propertyName);
            return null;
        }

        if (accessToken.get(propertyName).isCollection()) {
            return (String) accessToken.get(propertyName).asList().get(0);
        }

        debug.message("org.forgerock.openam.forgerockrest.authn.UMAAccessAuditFilter.getAccessTokenProperty: " +
                "Property {} is not a JSON collection and therefore cannot be read.", propertyName);

        return null;
    }

    private AccessToken retrieveAccessToken(Request request) {
        AccessToken token;

        ChallengeResponse challengeResponse = request.getChallengeResponse();

        if (challengeResponse == null) {
            debug.message("org.forgerock.openam.forgerockrest.authn.UMAAccessAuditFilter.retrieveAccessToken: " +
                    "Authorization header not found");
            return null;
        }

        String bearerToken = challengeResponse.getRawValue();

        if ("undefined".equals(bearerToken)) {
            debug.message("org.forgerock.openam.forgerockrest.authn.UMAAccessAuditFilter.retrieveAccessToken: " +
                    "Bearer token not found in Authorization header");
            return null;
        }

        OAuth2Request oAuth2Request = requestFactory.create(request);
        try {
            token = tokenStore.readAccessToken(oAuth2Request, bearerToken);
        } catch (ServerException | InvalidGrantException | NotFoundException e) {
            debug.message("org.forgerock.openam.forgerockrest.authn.UMAAccessAuditFilter.retrieveAccessToken: " +
                    "Failure to fetch access token.");
            return null;
        }

        return token;
    }

}
