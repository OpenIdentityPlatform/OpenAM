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

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import org.apache.commons.lang3.StringUtils;

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
import javax.servlet.http.Cookie;
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

    private static final Debug DEBUG = Debug.getInstance("amIdentityServices");

    private CallbackHandler handler;

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
     * Gets the name for the SSOToken ID cookie, which can be changed on-the-fly so is retrieved for each request that
     * is processed.
     *
     * @return The SSOToken ID cookie name;
     */
    protected String getSSOTokenCookieName() {
        return SystemProperties.get("com.iplanet.am.cookie.name");
    }

    /**
     * Gets the SSOTokenManager.
     *
     * @return The SSOTokenManager instance.
     * @throws SSOException If the SSOTokenManager instance could not be retrieved.
     */
    protected SSOTokenManager getSSOTokenManager() throws SSOException {
        return SSOTokenManager.getInstance();
    }

    /**
     * Validates the request by attempting to retrieve the SSOToken ID from the cookies on the request.
     * If the SSOToken ID cookie is not present then the method returns AuthStatus.SEND_FAILURE, otherwise if it is
     * present it is then used to retrieve the actual SSOToken from the SSOTokenManager, if valid then
     * AuthStatus.SUCCESS will be returned, otherwise AuthStatus.SEND_FAILURE will be returned.
     *
     * @param messageInfo {@inheritDoc}
     * @param clientSubject {@inheritDoc}
     * @param serviceSubject {@inheritDoc}
     * @return {@inheritDoc}
     * @throws AuthException If there is a problem validating the request.
     */
    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {

        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();

        String tokenId = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : request.getCookies()) {
                if (getSSOTokenCookieName().equals(cookie.getName())) {
                    tokenId = cookie.getValue();
                    break;
                }
            }
        }

        try {
            if (!StringUtils.isEmpty(tokenId)) {
                SSOTokenManager mgr = getSSOTokenManager();
                SSOToken ssoToken = mgr.createSSOToken(tokenId);

                if (ssoToken != null) {
                    handler.handle(new Callback[]{
                            new CallerPrincipalCallback(clientSubject, ssoToken.getPrincipal().getName())
                    });
                    Map<String, Object> context = (Map<String, Object>) messageInfo.getMap().get("org.forgerock.security.context");
                    context.put("authLevel", ssoToken.getAuthLevel());
                    //TODO add more properties to context map

                    return AuthStatus.SUCCESS;
                }
            }
        } catch (SSOException e) {
            DEBUG.error("SSOToken not valid", e);
        } catch (UnsupportedCallbackException e) {
            DEBUG.error("Error setting user principal", e);
            throw new AuthException(e.getMessage());
        } catch (IOException e) {
            DEBUG.error("Error setting user principal", e);
            throw new AuthException(e.getMessage());
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
}
