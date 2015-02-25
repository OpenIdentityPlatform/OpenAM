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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.modules.session;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.rest.router.RestEndpointManager;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.util.Map;

/**
 * Module to route endpoints to correct ServerAuthModule
 */
public class OpenAMSessionModule implements ServerAuthModule {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");
    private EndpointMatcher endpointMatcher;
    private final OptionalSSOTokenSessionModule optionalSSOTokenSessionModule;
    private final LocalSSOTokenSessionModule localSSOTokenSessionModule;

    /**
     * Constructs an instance of the RouterModule.
     */
    public OpenAMSessionModule() {
        optionalSSOTokenSessionModule = new OptionalSSOTokenSessionModule();
        localSSOTokenSessionModule = new LocalSSOTokenSessionModule();
    }

    @Override
    public void initialize(MessagePolicy messagePolicy, MessagePolicy messagePolicy2, CallbackHandler callbackHandler, Map map) throws AuthException {
        endpointMatcher = new EndpointMatcher("/json", InjectorHolder.getInstance(RestEndpointManager.class));

        optionalSSOTokenSessionModule.initialize(messagePolicy, messagePolicy2, callbackHandler, map);

        localSSOTokenSessionModule.initialize(messagePolicy, messagePolicy2, callbackHandler, map);
        init();
    }

    /**
     * Constructs an instance of the RouterModule.
     * <p>
     * Used by tests.
     *
     * @param endpointManager An instance of the RestEndpointManager.
     */
    public OpenAMSessionModule(final RestEndpointManager endpointManager, OptionalSSOTokenSessionModule
            optionalSSOTokenSessionModule, LocalSSOTokenSessionModule localSSOTokenSessionModule) {
        this.optionalSSOTokenSessionModule = optionalSSOTokenSessionModule;
        this.localSSOTokenSessionModule = localSSOTokenSessionModule;
        endpointMatcher = new EndpointMatcher("/json", endpointManager);
        init();
    }

    /**
     * Initialises the endpoint matcher with endpoint exceptions.
     */
    private void init() {
        /*
         * Only the REST Authentication Endpoint is unprotected. Other endpoints that don't need to be authenticated
         * as a "real" user can use the anonymous use to authenticate first, then use an Authorization Filter to
         * allow the anonymous user access.
         * <p>
         * Only need to specific the actual endpoint for the root path that this filter is registered against.
         */
        endpointMatcher.endpoint(RestEndpointManager.AUTHENTICATE, HttpMethod.GET);
        endpointMatcher.endpoint(RestEndpointManager.AUTHENTICATE, HttpMethod.POST);
        endpointMatcher.endpoint(RestEndpointManager.USERS, HttpMethod.POST, "_action", "register", "confirm",
                "forgotPassword", "forgotPasswordReset", "anonymousCreate");
        endpointMatcher.endpoint(RestEndpointManager.SERVER_INFO, HttpMethod.GET);
        endpointMatcher.endpoint(RestEndpointManager.SESSIONS, HttpMethod.POST, "_action", "validate");
    }

    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject subject, Subject subject2) throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(contextPath.length());

        if (endpointMatcher.match(request)) {
            DEBUG.message("Path: " + path + " Method: " + request.getMethod() + " Added as exception. Not protected");
            return optionalSSOTokenSessionModule.validateRequest(messageInfo, subject, subject2);
        } else {
            DEBUG.message("Path: " + path + " Method: " + request.getMethod() + " Protected resource.");
            return localSSOTokenSessionModule.validateRequest(messageInfo, subject, subject2);
        }
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject subject) throws AuthException {
        return localSSOTokenSessionModule.secureResponse(messageInfo, subject);
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        localSSOTokenSessionModule.cleanSubject(messageInfo, subject);
    }
}
