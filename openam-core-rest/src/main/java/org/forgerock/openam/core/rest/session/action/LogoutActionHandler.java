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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.rest.session.action;

import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.AdviceContext;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.rest.session.SessionResource;
import org.forgerock.openam.session.SessionConstants;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Handler for 'logout' action
 */
public class LogoutActionHandler implements ActionHandler {

    private static final Debug LOGGER = Debug.getInstance(SessionConstants.SESSION_DEBUG);

    private AuthUtilsWrapper authUtilsWrapper;
    private SSOTokenManager ssoTokenManager;



    /**
     * Constructs a LogoutActionHandler instance
     *
     * @param ssoTokenManager An instance of the SSOTokenManager.
     * @param authUtilsWrapper An instance of the AuthUtilsWrapper.
     */
    public LogoutActionHandler(SSOTokenManager ssoTokenManager, AuthUtilsWrapper authUtilsWrapper) {
        this.authUtilsWrapper = authUtilsWrapper;
        this.ssoTokenManager = ssoTokenManager;
    }

    /**
     * This class serves as a mocked HttpServletResponse, which will be passed to the AuthUtils#logout method,
     * specifically to handle when the PersistentCookieAuthModule needs to clear an existing session-jwt cookie,
     * which it does by adding an expired session-jwt cookie to the response. Because the HttpServletResponse is
     * not available to CREST services, but rather headers in the response are set via the AdviceContext, this
     * class will take set cookies and translate them into the AdviceContext associated with the current CREST
     * Context.
     */
    private final class HeaderCollectingHttpServletResponse extends HttpServletResponseWrapper {

        private static final String SET_COOKIE_HEADER = "Set-Cookie";

        private final AdviceContext adviceContext;

        private HeaderCollectingHttpServletResponse(HttpServletResponse response, AdviceContext adviceContext) {
            super(response);
            this.adviceContext = adviceContext;
        }

        @Override
        public void addCookie(Cookie cookie) {
            adviceContext.putAdvice(SET_COOKIE_HEADER,
                    new org.forgerock.caf.http.SetCookieSupport().generateHeader(cookie));

        }

        @Override
        public void setHeader(String name, String value) {
            adviceContext.putAdvice(name, value);
        }

        @Override
        public void addHeader(String name, String value) {
            adviceContext.putAdvice(name, value);
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handle(String tokenId, Context context,
            ActionRequest request) {
        try {
            JsonValue jsonValue = logout(tokenId, context);
            return newResultPromise(newActionResponse(jsonValue));
        } catch (InternalServerErrorException e) {
            if (LOGGER.errorEnabled()) {
                LOGGER.error("SessionResource.actionInstance :: Error performing logout for token "
                        + tokenId, e);
            }
            return e.asPromise();
        }
    }

    /**
     * Logs out a user.
     *
     * @param tokenId The id of the Token to invalidate
     * @throws InternalServerErrorException If the tokenId is invalid or could not be used to logout.
     */
    private JsonValue logout(String tokenId, Context context) throws InternalServerErrorException {

        SSOToken ssoToken;
        try {
            if (tokenId == null) {
                if (LOGGER.messageEnabled()) {
                    LOGGER.message("SessionResource.logout() :: Null Token Id.");
                }
                throw new InternalServerErrorException("Null Token Id");
            }
            ssoToken = ssoTokenManager.createSSOToken(tokenId);
        } catch (SSOException ex) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(SessionResource.KEYWORD_RESULT, "Token has expired");
            if (LOGGER.messageEnabled()) {
                LOGGER.message("SessionResource.logout() :: Token ID, " + tokenId + ", already expired.");
            }
            return new JsonValue(map);
        }
        HttpServletResponse httpServletResponse = null;
        final AdviceContext adviceContext = context.asContext(AdviceContext.class);
        if (adviceContext == null) {
            if (LOGGER.warningEnabled()) {
                LOGGER.warning("No AdviceContext in Context, and thus no headers can be set in the HttpServletResponse.");
            }
        } else {
            httpServletResponse = new HeaderCollectingHttpServletResponse(new UnsupportedResponse(), adviceContext);
        }
        AttributesContext requestContext = context.asContext(AttributesContext.class);
        Map<String, Object> requestAttributes = requestContext.getAttributes();
        final HttpServletRequest httpServletRequest = (HttpServletRequest) requestAttributes.get(HttpServletRequest.class.getName());

        String sessionId;
        Map<String, Object> map = new HashMap<>();

        if (ssoToken != null) {
            sessionId = ssoToken.getTokenID().toString();

            try {
                authUtilsWrapper.logout(sessionId, httpServletRequest, httpServletResponse);
            } catch (SSOException e) {
                if (LOGGER.errorEnabled()) {
                    LOGGER.error("SessionResource.logout() :: Token ID, " + tokenId +
                            ", unable to log out associated token.");
                }
                throw new InternalServerErrorException("Error logging out", e);
            }

            //equiv to LogoutViewBean's POST_PROCESS_LOGOUT_URL usage
            String papRedirect = authUtilsWrapper.getPostProcessLogoutURL(httpServletRequest);
            if (!StringUtils.isBlank(papRedirect)) {
                map.put("goto", papRedirect);
            }
        }

        map.put("result", "Successfully logged out");
        LOGGER.message("SessionResource.logout() :: Successfully logged out token, {}", tokenId);
        return new JsonValue(map);
    }
}
