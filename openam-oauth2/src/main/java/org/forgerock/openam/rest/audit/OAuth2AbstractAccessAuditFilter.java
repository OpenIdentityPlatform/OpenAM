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

import static org.forgerock.openam.audit.AuditConstants.TrackingIdKey.SESSION;
import static org.forgerock.openam.audit.AuditConstants.USER_ID;

import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import org.forgerock.oauth2.core.IntrospectableToken;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.Token;
import org.forgerock.openam.audit.AuditConstants;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.OpenIdConnectToken;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for logging access audit events for all OAuth2-based filters. Common functionality is here, a filter
 * may overwrite this functionality if there is a known difference in access or outcome details for that filter.
 *
 * @since 13.0.0
 */
public abstract class OAuth2AbstractAccessAuditFilter extends AbstractRestletAccessAuditFilter {

    private final OAuth2RequestFactory requestFactory;
    private final Logger logger = LoggerFactory.getLogger("oauth2");

    /**
     * Create a new filter for the given component and restlet.
     *
     * @param component The component for which events will be logged.
     * @param restlet The restlet for which events will be logged.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     * @param requestFactory The factory that provides access to OAuth2Request
     */
    OAuth2AbstractAccessAuditFilter(AuditConstants.Component component, Restlet restlet,
            AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory,
            OAuth2RequestFactory requestFactory, RestletBodyAuditor requestDetailCreator,
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
        for (Token t : requestFactory.create(request).getTokens()) {
            AuditConstants.TrackingIdKey key = t.getAuditTrackingIdKey();
            String trackingId = t.getAuditTrackingId();
            if (key != null && trackingId != null) {
                AuditRequestContext.putProperty(key.toString(), trackingId);
            }
        }
        SSOToken ssoToken = getSSOToken(request);
        if (ssoToken != null) {
            try {
                AuditRequestContext.putProperty(SESSION.toString(), ssoToken.getProperty(Constants.AM_CTX_ID));
            } catch (SSOException e) {
                logger.debug("Could not get tracking ID for session", e);
            }
        } else {
            String sessionTrackingId = (String) request.getAttributes().get(Constants.AM_CTX_ID);
            if (sessionTrackingId != null) {
                AuditRequestContext.putProperty(SESSION.toString(), sessionTrackingId);
            }
        }
    }

    private String getUserId(Request request) {
        for (Token t : requestFactory.create(request).getTokens()) {
            if (t instanceof IntrospectableToken) {
                return ((IntrospectableToken) t).getResourceOwnerId();
            } else if (t instanceof OpenIdConnectToken) {
                return ((OpenIdConnectToken) t).get(OAuth2Constants.JWTTokenParams.SUB).asString();
            }
        }
        SSOToken ssoToken = getSSOToken(request);
        try {
            return ssoToken == null ? null : ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        } catch (SSOException e) {
            logger.debug("Could not get user ID for session", e);
            return null;
        }
    }

    private SSOToken getSSOToken(Request request) {
        SSOToken token;
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            token = mgr.createSSOToken(ServletUtils.getRequest(request));
        } catch (Exception e) {
            logger.debug("Could not get session", e);
            return null;
        }
        return token;
    }

}
