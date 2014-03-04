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

package org.forgerock.openam.authz.filter;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.authz.AuthorizationContext;
import org.forgerock.authz.AuthorizationModule;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.auth.shared.AuthnRequestUtils;
import org.forgerock.openam.auth.shared.SSOTokenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

/**
 * Responsible for merely logging the request with the requests token id, if present, and not applying any
 * authorization to the request, just letting it pass through to the resource.
 *
 * @since 10.2.0
 */
@Singleton
public class PassThroughAuthorizationModule implements AuthorizationModule {

    private final Logger logger = LoggerFactory.getLogger(PassThroughAuthorizationModule.class);

    private final SSOTokenFactory ssoTokenFactory;
    private final AuthnRequestUtils requestUtils;


    /**
     * Constructs a new instance of the PassThroughAuthorizationModule.
     *
     * @param ssoTokenFactory An instance of the SSOTokenFactory.
     * @param requestUtils An instance of the AuthnRequestUtils.
     */
    @Inject
    public PassThroughAuthorizationModule(SSOTokenFactory ssoTokenFactory, AuthnRequestUtils requestUtils) {
        this.ssoTokenFactory = ssoTokenFactory;
        this.requestUtils = requestUtils;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise(final JsonValue config) {
    }

    /**
     * Performs no filtering or application of authorization to the request, just tries to get as much information about
     * the user/client making the request to log the request.
     *
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean authorize(HttpServletRequest request, AuthorizationContext context) {

        // Request MAY contain the TokenID of the user.
        String tokenId = requestUtils.getTokenId(request);
        if (tokenId == null) {
            logger.debug("Request made without TokenID");
        }

        String authorizationDebugMessage = "%s Accessed using PassThroughAuthorizationModule, with unauthenticated "
                + "user. Letting request through.";
        String[] authorizationDebugMessageParams = new String[]{request.getRequestURI()};

        if (tokenId != null) {
            // Must generate a valid SSOToken from this TokenID.
            SSOToken token = ssoTokenFactory.getTokenFromId(tokenId);
            if (token == null) {
                logger.debug("%s Accessed using PassThroughAuthorizationModule, with %s, but failed to get SSOToken "
                        + "for it. Letting request through.", request.getRequestURI(), tokenId);
                return true;
            }

            try {
                String name = token.getPrincipal().getName();
                String realm = token.getProperty("Organization");
                authorizationDebugMessage = "%s Accessed using PassThroughAuthorizationModule by user, %s on realm, "
                        + "%s with token, %s. Letting request through.";
                authorizationDebugMessageParams = new String[]{request.getRequestURI(), name, realm,
                        token.getTokenID().toString()};
            } catch (SSOException e) {
                logger.error("Error getting AMIdentity for token. Not letting request through.", e);
                return false;
            }
        }

        logger.debug(authorizationDebugMessage, authorizationDebugMessageParams);

        return true;
    }

    /**
     * Does nothing in this impl.
     */
    public void destroy() {
    }
}
