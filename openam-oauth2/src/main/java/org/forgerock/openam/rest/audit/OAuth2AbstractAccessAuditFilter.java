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
* Copyright 2015-2016 ForgeRock AS.
*/
package org.forgerock.openam.rest.audit;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.Token;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

import com.sun.identity.idm.AMIdentity;

import java.util.Set;

import static org.forgerock.openam.audit.AuditConstants.USER_ID;
import static org.forgerock.openam.audit.AuditConstants.TrackingIdKey.OAUTH2_ACCESS;

/**
 * Responsible for logging access audit events for all OAuth2-based filters. Common functionality is here, a filter
 * may overwrite this functionality if there is a known difference in access or outcome details for that filter.
 *
 * @since 13.0.0
 */
public abstract class OAuth2AbstractAccessAuditFilter extends AbstractRestletAccessAuditFilter {

    OAuth2RequestFactory<?, Request> requestFactory;
    Class<?>[] TOKEN_CLASS = { AccessToken.class, RefreshToken.class, AuthorizationCode.class };

    /**
     * Create a new filter for the given component and restlet.
     *
     * @param component The component for which events will be logged.
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     * @param requestFactory The factory that provides access to OAuth2Request
     */
    public OAuth2AbstractAccessAuditFilter(AuditConstants.Component component, Restlet restlet,
            AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory,
            OAuth2RequestFactory<?, Request> requestFactory, RestletBodyAuditor requestDetailCreator,
            RestletBodyAuditor responseDetailCreator) {
        super(component, restlet, auditEventPublisher, auditEventFactory, requestDetailCreator, responseDetailCreator);
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

        putUserIdInAuditRequestContext(request);

        return super.getUserIdForAccessAttempt(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getTrackingIdsForAccessAttempt(Request request) {
        putTrackingIdsIntoAuditRequestContext(request);

        return super.getTrackingIdsForAccessAttempt(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUserIdForAccessOutcome(Request request, Response response) {
        String userId = super.getUserIdForAccessOutcome(request, response);
        if (StringUtils.isNotEmpty(userId)) {
            return userId;
        }

        putUserIdInAuditRequestContext(request);

        return super.getUserIdForAccessOutcome(request, response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getTrackingIdsForAccessOutcome(Request request, Response response) {
        putTrackingIdsIntoAuditRequestContext(request);

        return super.getTrackingIdsForAccessOutcome(request, response);
    }

    private void putUserIdInAuditRequestContext(Request request) {
        String userId = getUserId(request);
        if (userId != null) {
            AuditRequestContext.putProperty(USER_ID, userId);
        }
    }

    private void putTrackingIdsIntoAuditRequestContext(Request request) {
        String trackingId = getTrackingId(request);
        if (trackingId != null) {
            AuditRequestContext.putProperty(OAUTH2_ACCESS.toString(), trackingId);
        }
    }

    private String getUserId(Request request) {
        for (Class clazz : TOKEN_CLASS) {
            JsonValue token = null; 
            token = (JsonValue) retrieveTokenFromRequest(request, clazz);
            if (token != null) {
                String userId = getUserIdFromToken(token);
                if (userId != null) {
                    return userId;
                }
            }
        }

        return null;
    }

    private String getUserIdFromToken(JsonValue token) {
        String username = getTokenProperty(OAuth2Constants.CoreTokenParams.USERNAME, token);
        String realm = getTokenProperty(OAuth2Constants.CoreTokenParams.REALM, token);
        
        if (username == null || realm == null) {
            return null;
        }

        CoreWrapper cw = new CoreWrapper();
        AMIdentity identity = cw.getIdentity(username, realm);
        return (identity == null) ? null : identity.getUniversalId();
    }

    private String getTrackingId(Request request) {
        for (Class clazz : TOKEN_CLASS) {
            JsonValue token = null; 
            token = (JsonValue) retrieveTokenFromRequest(request, clazz);
            if (token != null) {
                String trackingId = getTokenProperty(OAuth2Constants.CoreTokenParams.AUDIT_TRACKING_ID, token);
                if (trackingId != null) {
                    return trackingId;
                }
            }
        }

        return null;
    }

    private <T extends Token> T retrieveTokenFromRequest(Request request, Class<T> clazz) {
        return requestFactory.create(request).getToken(clazz);
    }

    private String getTokenProperty(String propertyName, JsonValue token) {
        if (!token.isDefined(propertyName)) {
            return null;
        }

        if (token.get(propertyName).isNotNull()) {
            if (token.get(propertyName).isCollection()) {
                return (String) token.get(propertyName).asList().get(0);
            }
            
            if (token.get(propertyName).isString()) {
                return token.get(propertyName).asString(); // TODO: Return this value?
            }
        }
        return null;
    }
}
