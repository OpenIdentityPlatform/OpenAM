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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.modules.session;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.auth.shared.AuthUtilsWrapper;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.forgerock.openam.guice.InjectorHolder;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * A local implementation of a SSOToken Session module, that is designed to be deployed in an OpenAM deployment
 * not remotely, i.e. protecting resources on another server which uses OpenAM for authentication.
 *
 * The SSOToken module will validate the presents and validity of a SSOToken ID on a request, if present and valid then
 * the request is allowed to proceed. The responsibilities of this module are only to validate but never to issue
 * a SSOToken, for this the client must authenticate before trying to access the resource again.
 *
 * @author Phill Cunnington
 */
public class LocalSSOTokenSessionModule implements ServerAuthModule {

    private static final String REQUESTER_URL_PARAM = "requester";

    private final AuthUtilsWrapper authUtilsWrapper;

    private AuthnRequestUtils requestUtils;
    private SSOTokenFactory factory;
    private CallbackHandler handler;

    /**
     * Default constructor is initialised by the framework.
     */
    public LocalSSOTokenSessionModule() {
        authUtilsWrapper = new AuthUtilsWrapper();
    }

    /**
     * Test constructor exposes required dependencies.
     *
     * @param requestUtils Required for processing ServletRequests.
     * @param factory Required for generation of SSOTokens.
     * @param authUtilsWrapper Required for getting cookie name.
     */
    public LocalSSOTokenSessionModule(AuthnRequestUtils requestUtils, SSOTokenFactory factory,
            AuthUtilsWrapper authUtilsWrapper) {
        this.requestUtils = requestUtils;
        this.factory = factory;
        this.authUtilsWrapper = authUtilsWrapper;
    }

    /**
     * @return False if the dependencies have not been initialised.
     */
    private boolean isInitialised() {
        return getFactory() != null;
    }

    /**
     * Use Guice to initalise the dependencies.
     */
    private void initDependencies() {
        factory = InjectorHolder.getInstance(SSOTokenFactory.class);
        requestUtils = InjectorHolder.getInstance(AuthnRequestUtils.class);
    }

    /**
     * No initialisation required for this module.
     *
     * @param requestPolicy {@inheritDoc}
     * @param responsePolicy {@inheritDoc}
     * @param handler {@inheritDoc}
     * @param options {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
            Map options) throws AuthException {
        this.handler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class[] getSupportedMessageTypes() {
        return new Class[]{HttpServletRequest.class, HttpServletResponse.class};
    }

    /**
     * Validates the request by checking the validity of the SSOToken ID from the AM cookie.
     * <p>
     * If the SSOToken ID is a restricted token then the request must also contain a url parameter "requester" which
     * must contain the application SSOToken ID of the application the restricted token was issued for.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException If there is a problem validating the request.
     */
    @Override
    public AuthStatus validateRequest(final MessageInfo messageInfo, final Subject clientSubject,
            Subject serviceSubject) throws AuthException {
        if (!isInitialised()) {
            initDependencies();
        }

        final HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String requester = request.getParameter(REQUESTER_URL_PARAM);
        if (requester != null) {
            try {
                SSOToken requesterToken = getFactory().getTokenFromId(requester);
                if (getFactory().isTokenValid(requesterToken)) {
                    Object o = RestrictedTokenContext.doUsing(requesterToken, new RestrictedTokenAction() {
                        public Object run() throws Exception {
                            return validate(request, messageInfo, clientSubject);
                        }
                    });
                    return (AuthStatus) o;
                }
            } catch (Exception ex) {
                throw new AuthException("An error occurred whilst trying to use restricted token.");
            }
        }
        return validate(request, messageInfo, clientSubject);
    }

    /**
     * Gets the AM cookie name, as set by AM.
     *
     * @return The AM cookie name.
     */
    private String getCookieHeaderName() {
        return authUtilsWrapper.getCookieName();
    }

    /**
     * Validates the request by attempting to retrieve the SSOToken ID from the cookies on the request.
     * If the SSOToken ID cookie is not present then the method returns AuthStatus.SEND_FAILURE, otherwise if it is
     * present it is then used to retrieve the actual SSOToken from the SSOTokenManager, if valid then
     * AuthStatus.SUCCESS will be returned, otherwise AuthStatus.SEND_FAILURE will be returned.
     *
     * @param request The HttpServletRequest.
     * @param messageInfo A contextual object that encapsulates the client request and server response objects, and
     *                    that may be used to save state across a sequence of calls made to the methods of this
     *                    interface for the purpose of completing a secure message exchange.
     * @param clientSubject A Subject that represents the source of the service request. It is used by the method
     *                      implementation to store Principals and credentials validated in the request.
     * @return AuthStatus.SUCCESS if the SSOToken ID is valid, otherwise AuthStatus.SEND_FAILURE.
     * @throws AuthException If there is a problem validating the request.
     */
    private AuthStatus validate(HttpServletRequest request, MessageInfo messageInfo, Subject clientSubject)
            throws AuthException {

        String tokenId = getRequestUtils().getTokenId(request);
        if (StringUtils.isEmpty(tokenId)) {
            tokenId = request.getHeader(getCookieHeaderName());
        }
        if (!StringUtils.isEmpty(tokenId)) {
            SSOToken ssoToken = getFactory().getTokenFromId(tokenId);

            if (ssoToken != null) {

                int authLevel;
                try {
                    authLevel = ssoToken.getAuthLevel();
                    String name = ssoToken.getPrincipal().getName();
                    handler.handle(new Callback[]{new CallerPrincipalCallback(clientSubject, name)});

                    clientSubject.getPrincipals().add(ssoToken.getPrincipal());
                } catch (SSOException e) {
                    throw new AuthException(e.getMessage());
                } catch (UnsupportedCallbackException e) {
                    throw new AuthException(e.getMessage());
                } catch (IOException e) {
                    throw new AuthException(e.getMessage());
                }

                Map<String, Object> context =
                        (Map<String, Object>) messageInfo.getMap().get("org.forgerock.authentication.context");
                context.put("authLevel", authLevel);
                context.put("tokenId", ssoToken.getTokenID().toString());
                //TODO add more properties to context map

                return AuthStatus.SUCCESS;
            }
        }
        return AuthStatus.SEND_FAILURE;
    }

    /**
     * This module will always return AuthStatus.SEND_SUCCESS as it is not designed to return a SSOToken, ony to
     * validate that one is present on the request.
     *
     * @param messageInfo {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    /**
     * No cleaning for the Subject is required for this module.
     *
     * @param messageInfo {@inheritDoc}
     * @param subject {@inheritDoc}
     * @throws AuthException {@inheritDoc}
     */
    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
    }

    /**
     * @return The AuthRequestUtils, Guice initialised if it has not been assigned.
     */
    public AuthnRequestUtils getRequestUtils() {
        return requestUtils;
    }

    /**
     * @return The SSOTokenFactory, Guice initialised if it has not been assigned.
     */
    public SSOTokenFactory getFactory() {
        return factory;
    }
}
