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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.oauth2.core;

import static com.sun.identity.shared.Constants.AM_CTX_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.*;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import jakarta.servlet.http.HttpServletRequest;

import java.security.AccessController;
import java.util.ArrayList;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;

import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.RealmNormaliser;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;

/**
 * Authenticates a resource owner from the credentials provided on the request.
 *
 * @since 12.0.0
 */
@Singleton
public class ResourceOwnerAuthenticator {

    private final Debug logger = Debug.getInstance("amOpenAMResourceOwnerAuthenticator");
    private final RealmNormaliser realmNormaliser;

    /**
     * Constructs a new OpenAMResourceOwnerAuthenticator.
     *
     * @param realmNormaliser An instance of the RealmNormaliser.
     */
    @Inject
    public ResourceOwnerAuthenticator(RealmNormaliser realmNormaliser) {
        this.realmNormaliser = realmNormaliser;
    }

    /**
     * Authenticates a resource owner by extracting the resource owner's credentials from the request and authenticating
     * against the OAuth2 provider's internal user store.
     *
     * @param request The OAuth2 request.
     * @throws NotFoundException if the requested realm doesn't exist
     * @return The authenticated ResourceOwner, or {@code null} if authentication failed.
     */
    public ResourceOwner authenticate(OAuth2Request request) throws NotFoundException {
        final String username = request.getParameter(USERNAME);
        final char[] password = request.getParameter(PASSWORD) == null ? null :
            request.<String>getParameter(PASSWORD).toCharArray();
        try {
            final String realm = realmNormaliser.normalise(request.<String>getParameter(OAuth2Constants.Custom.REALM));
            final String authChain = request.getParameter(AUTH_CHAIN);
            return authenticate(request.<Request>getRequest(), username, password, realm, authChain);
        } catch (org.forgerock.json.resource.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    private ResourceOwner authenticate(Request req, String username, char[] password, String realm, String service) {

        ResourceOwner ret = null;
        AuthContext lc;
        try {
            lc = new AuthContext(realm);
            HttpServletRequest request = ServletUtils.getRequest(req);
            request.setAttribute(ISAuthConstants.NO_SESSION_REQUEST_ATTR, "true");
            if (service != null) {
                lc.login(AuthContext.IndexType.SERVICE, service, null, request,
                        ServletUtils.getResponse(Response.getCurrent()));
            } else {
                lc.login(request, ServletUtils.getResponse(Response.getCurrent()));
            }

            while (lc.hasMoreRequirements()) {
                Callback[] callbacks = lc.getRequirements();
                ArrayList missing = new ArrayList();
                // loop through the requires setting the needs..
                for (int i = 0; i < callbacks.length; i++) {
                    if (callbacks[i] instanceof NameCallback) {
                        NameCallback nc = (NameCallback) callbacks[i];
                        nc.setName(username);
                    } else if (callbacks[i] instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callbacks[i];
                        pc.setPassword(password);
                    } else {
                        missing.add(callbacks[i]);
                    }
                }
                // there's missing requirements not filled by this
                if (missing.size() > 0) {
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing requirements");
                }
                lc.submitRequirements(callbacks);
            }

            // validate the password..
            if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                try {
                    LoginState loginState = lc.getAuthContextLocal().getLoginState();
                    String universalId = loginState.getUserUniversalId(loginState.getUserDN());
                    SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
                    final AMIdentity id = IdUtils.getIdentity(adminToken, universalId);
                    req.getAttributes().put(AM_CTX_ID, loginState.getActivatedSessionTrackingId());
                    ret = new ResourceOwner(id.getName(), id, currentTimeMillis());
                } catch (Exception e) {
                    logger.error("Unable to get SSOToken", e);
                    // we're going to throw a generic error
                    // because the system is likely down..
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
                }
            }
        } catch (AuthLoginException le) {
            logger.error("AuthException", le);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, le);
        }
        return ret;
    }
}
