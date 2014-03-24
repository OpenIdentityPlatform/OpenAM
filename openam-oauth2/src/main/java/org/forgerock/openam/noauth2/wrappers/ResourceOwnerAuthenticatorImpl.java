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

package org.forgerock.openam.noauth2.wrappers;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerAuthenticator;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.oauth2.provider.impl.OpenAMUser;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.util.ArrayList;

/**
 * @since 12.0.0
 */
public class ResourceOwnerAuthenticatorImpl implements ResourceOwnerAuthenticator<OpenAMResourceOwnerAuthentication> {

    public ResourceOwner authenticate(final OpenAMResourceOwnerAuthentication resourceOwnerAuthentication) {

        SSOToken token = null;
        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            token = mgr.createSSOToken(resourceOwnerAuthentication.getRequest());
        } catch (Exception e){
            OAuth2Utils.DEBUG.warning("ResourceOwnerAuthenticatorImpl:: No SSO Token in request", e);
        }
        if (token == null) {
            return authenticate(resourceOwnerAuthentication.getUsername(), resourceOwnerAuthentication.getPassword(),
                    resourceOwnerAuthentication.getRealm());
        } else {
            try {
                return new OpenAMUser(token.getProperty("UserToken"), token);
            } catch (Exception e) {
                OAuth2Utils.DEBUG.error("ResourceOwnerAuthenticatorImpl:: Unable to create ResourceOwner", e);
            }
        }
        return null;
    }

    private ResourceOwner authenticate(String username, char[] password, String realm) {   //TODO share this code with ClientAuthenticatorImpl.

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
                    OAuth2Utils.DEBUG.error( "ResourceOwnerAuthenticatorImpl::authContext: "
                            + "Unable to get SSOToken", e);
                    // we're going to throw a generic error
                    // because the system is likely down..
                    throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
                }
            }
        } catch (AuthLoginException le) {
            OAuth2Utils.DEBUG.error("ResourceOwnerAuthenticatorImpl::authContext AuthException", le);
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, le);
        } finally {
            if (lc != null && AuthContext.Status.SUCCESS.equals(lc.getStatus())) {
                try {
                    lc.logout();
                    if (OAuth2Utils.DEBUG.messageEnabled()) {
                        OAuth2Utils.DEBUG.message("Logged user out in ResourceOwnerAuthenticatorImpl.authenticate.");
                    }
                } catch (AuthLoginException e) {
                    OAuth2Utils.DEBUG.error("Exception caught logging out of AuthContext after successful login: " + e, e);
                }
            }
        }
        return ret;
    }

    protected ResourceOwner createResourceOwner(AuthContext authContext) throws Exception {
        SSOToken token = authContext.getSSOToken();
        return new OpenAMUser(token.getProperty("UserToken"), token);
    }
}
