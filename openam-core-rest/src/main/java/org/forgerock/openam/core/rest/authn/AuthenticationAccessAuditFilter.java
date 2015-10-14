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
package org.forgerock.openam.core.rest.authn;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getTrackingIdFromSSOToken;
import static org.forgerock.openam.audit.AMAuditEventBuilderUtils.getUserId;
import static org.forgerock.openam.audit.AuditConstants.*;
import static org.forgerock.openam.audit.AuditConstants.TrackingIdKey.*;
import static org.forgerock.openam.core.rest.authn.RestAuthenticationConstants.*;
import static org.forgerock.openam.utils.JsonValueBuilder.toJsonValue;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.AuditEventFactory;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.audit.AbstractHttpAccessAuditFilter;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.services.context.Context;

/**
 * Responsible for logging access audit events for authentication requests.
 *
 * @since 13.0.0
 */
public class AuthenticationAccessAuditFilter extends AbstractHttpAccessAuditFilter {

    private static final Debug debug = Debug.getInstance("amAudit");

    private final AuthIdHelper authIdHelper;

    /**
     * Create a new filter for the given component and restlet.
     *
     * @param authIdHelper The helper to use for reading authentication JWTs.
     * @param auditEventPublisher The publisher responsible for logging the events.
     * @param auditEventFactory The factory that can be used to create the events.
     */
    @Inject
    public AuthenticationAccessAuditFilter(AuthIdHelper authIdHelper,
            AuditEventPublisher auditEventPublisher, AuditEventFactory auditEventFactory) {
        super(Component.AUTHENTICATION, auditEventPublisher, auditEventFactory);
        this.authIdHelper = authIdHelper;
    }

    @Override
    protected Set<String> getTrackingIdsForAccessAttempt(Request request) {
        try {
            String jsonString = request.getEntity().getString();
            if (isNotEmpty(jsonString)) {
                JsonValue jsonValue = toJsonValue(jsonString);
                if (jsonValue.isDefined(AUTH_ID)) {
                    populateContextFromAuthId(jsonValue.get(AUTH_ID).asString());
                }
            }
        } catch (IOException e) {
            //Do nothing
        }

        return super.getTrackingIdsForAccessAttempt(request);
    }

    @Override
    protected String getUserIdForAccessOutcome(Response response) {
        String userId = super.getUserIdForAccessOutcome(response);
        if (isNotEmpty(userId)) {
            return userId;
        }

        String tokenId = AuditRequestContext.getProperty(TOKEN_ID);
        if (isNotEmpty(tokenId)) {
            populateContextFromTokenId(tokenId);
        }

        return super.getUserIdForAccessOutcome(response);
    }

    @Override
    protected Set<String> getTrackingIdsForAccessOutcome(Response response) {
        String tokenId = AuditRequestContext.getProperty(TOKEN_ID);
        String sessionId = AuditRequestContext.getProperty(SESSION_ID);
        String authId = AuditRequestContext.getProperty(AUTH_ID);
        if (isNotEmpty(tokenId)) {
            populateContextFromTokenId(tokenId);

        } else if (isNotEmpty(sessionId)) {
            AuditRequestContext.putProperty(AUTH.toString(), getContextIdFromSessionId(sessionId));

        } else if (isNotEmpty(authId)) {
            populateContextFromAuthId(authId);
        }

        return super.getTrackingIdsForAccessOutcome(response);
    }

    @Override
    protected String getRealm(Context context) {
        if (context.containsContext(RealmContext.class)) {
            return context.asContext(RealmContext.class).getResolvedRealm();
        }
        return null;
    }

    private void populateContextFromTokenId(String tokenId) {
        try {
            SSOToken token = SSOTokenManager.getInstance().createSSOToken(tokenId);
            AuditRequestContext.putProperty(USER_ID, getUserId(token));
            AuditRequestContext.putProperty(SESSION.toString(), getTrackingIdFromSSOToken(token));
        } catch (SSOException e) {
            debug.warning("No SSO Token found when trying to audit an authentication request.");
        }
    }

    private void populateContextFromAuthId(String authId) {
        try {
            String sessionId = authIdHelper.reconstructAuthId(authId).getClaimsSet().getClaim(SESSION_ID, String.class);
            if (isEmpty(sessionId)) {
                return;
            }

            String contextId = getContextIdFromSessionId(sessionId);
            if (isNotEmpty(contextId)) {
                AuditRequestContext.putProperty(AUTH.toString(), contextId);
            }
        } catch (RestAuthException e) {
            debug.warning("No session ID found when trying to audit an authentication request.");
        }
    }

    private String getContextIdFromSessionId(String sessionId) {
        InternalSession session = AuthD.getSession(new SessionID(sessionId));
        return session == null ? "" : session.getProperty(Constants.AM_CTX_ID);
    }

}
