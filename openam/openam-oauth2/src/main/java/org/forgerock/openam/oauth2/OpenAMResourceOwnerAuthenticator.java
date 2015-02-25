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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerAuthenticator;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.security.AccessController;
import java.util.ArrayList;

/**
 * Authenticates a resource owner from the credentials provided on the request.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMResourceOwnerAuthenticator implements ResourceOwnerAuthenticator {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final RealmNormaliser realmNormaliser;

    /**
     * Constructs a new OpenAMResourceOwnerAuthenticator.
     *
     * @param realmNormaliser An instance of the RealmNormaliser.
     */
    @Inject
    public OpenAMResourceOwnerAuthenticator(RealmNormaliser realmNormaliser) {
        this.realmNormaliser = realmNormaliser;
    }

    /**
     * {@inheritDoc}
     */
    public ResourceOwner authenticate(OAuth2Request request) {
        SSOToken token = null;
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            token = mgr.createSSOToken(ServletUtils.getRequest(request.<Request>getRequest()));
        } catch (Exception e){
            logger.warning("ResourceOwnerAuthenticatorImpl:: No SSO Token in request", e);
        }
        if (token == null) {


            final String username = request.getParameter("username");
            final char[] password = request.getParameter("password") == null ? null :
                    request.<String>getParameter("password").toCharArray();
            final String realm = realmNormaliser.normalise(request.<String>getParameter("realm"));
            return authenticate(username, password, realm);
        } else {
            try {
                final AMIdentity id = IdUtils.getIdentity(
                        AccessController.doPrivileged(AdminTokenAction.getInstance()),
                        token.getProperty(Constants.UNIVERSAL_IDENTIFIER));
                return new OpenAMResourceOwner(token.getProperty("UserToken"), id);
            } catch (Exception e) {
                logger.error("ResourceOwnerAuthenticatorImpl:: Unable to create ResourceOwner", e);
            }
        }
        return null;
    }

    private ResourceOwner authenticate(String username, char[] password, String realm) {

        ResourceOwner ret = null;
        AuthContext lc = null;
        try {
            lc = new AuthContext(realm);
            lc.login();
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
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
                            "Missing requirements");
                }
                lc.submitRequirements(callbacks);
            }

            // validate the password..
            if (lc.getStatus() == AuthContext.Status.SUCCESS) {
                try {
                    // package up the token for transport..
                    ret = createResourceOwner(lc);
                } catch (Exception e) {
                    logger.error( "ResourceOwnerAuthenticatorImpl::authContext: "
                            + "Unable to get SSOToken", e);
                    // we're going to throw a generic error
                    // because the system is likely down..
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
                }
            }
        } catch (AuthLoginException le) {
            logger.error("ResourceOwnerAuthenticatorImpl::authContext AuthException", le);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, le);
        } finally {
            if (lc != null && AuthContext.Status.SUCCESS.equals(lc.getStatus())) {
                try {
                    lc.logout();
                    logger.message("Logged user out in ResourceOwnerAuthenticatorImpl.authenticate.");
                } catch (AuthLoginException e) {
                    logger.error("Exception caught logging out of AuthContext after successful login: " + e, e);
                }
            }
        }
        return ret;
    }

    private ResourceOwner createResourceOwner(AuthContext authContext) throws Exception {
        SSOToken token = authContext.getSSOToken();
        final AMIdentity id = IdUtils.getIdentity(
                AccessController.doPrivileged(AdminTokenAction.getInstance()),
                token.getProperty(Constants.UNIVERSAL_IDENTIFIER));
        return new OpenAMResourceOwner(token.getProperty("UserToken"), id);
    }
}
