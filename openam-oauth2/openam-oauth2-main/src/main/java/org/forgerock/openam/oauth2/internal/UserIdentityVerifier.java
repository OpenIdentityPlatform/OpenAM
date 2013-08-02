/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.internal;

import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.provider.AbstractIdentityVerifier;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.provider.impl.OpenAMUser;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.ext.servlet.ServletUtils;

import javax.servlet.http.HttpServletRequest;


/**
 * Verifies an OpenAMUser.
 * This class is only consumed by the PasswordServerResource in the resource owner credentials flow.
 */
public class UserIdentityVerifier extends AbstractIdentityVerifier<OpenAMUser> {

    /**
     * Constructor.
     * <p/>
     *
     */
    public UserIdentityVerifier() {
    }

    @Override
    protected OpenAMUser createUser(AuthContext authContext) throws Exception {
        SSOToken token = authContext.getSSOToken();
        return new OpenAMUser(token.getProperty("UserToken"), token);
    }

    /**
     * Returns the user identifier.
     *
     * @param request
     *            The request to inspect.
     * @param response
     *            The response to inspect.
     * @return The user identifier.
     */
    protected String getIdentifier(Request request, Response response) {
        if (null != OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.USERNAME, String.class)){
            return OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.USERNAME, String.class);
        } else {
            return null;
        }
    }

    /**
     * Returns the secret provided by the user.
     *
     * @param request
     *            The request to inspect.
     * @param response
     *            The response to inspect.
     * @return The secret provided by the user.
     */
    protected char[] getSecret(Request request, Response response) {
        if (null != OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.PASSWORD, String.class)){
            return OAuth2Utils.getRequestParameter(request, OAuth2Constants.Params.PASSWORD, String.class).toCharArray();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int verify(Request request, Response response) {
        int result = RESULT_INVALID;
        String identifier = getIdentifier(request, response);
        char[] secret = getSecret(request, response);
        if (null == identifier || null == secret) {
            result = RESULT_MISSING;
        } else {
            OpenAMUser user = authenticate(request, identifier, secret);
            if (null != user) {
                result = RESULT_VALID;
                request.getClientInfo().setUser(user);
            }
        }

        return result;
    }

    public OpenAMUser authenticate(Request request, String username, char[] password) {
        HttpServletRequest httpRequest = ServletUtils.getRequest(request);

        SSOToken token = null;
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            token = mgr.createSSOToken(httpRequest);
        } catch (Exception e){
            OAuth2Utils.DEBUG.warning("UserIdentityVerifier:: No SSO Token in request", e);
        }
        if (token == null){
            return authenticate(username, password, OAuth2Utils.getRealm(request));
        } else {
            try {
                return new OpenAMUser(token.getProperty("UserToken"), token);
            } catch (Exception e){
                OAuth2Utils.DEBUG.error("UserIdentityVerifier:: Unable to create OpenAMUser", e);
            }
        }
        return null;
    }
}
