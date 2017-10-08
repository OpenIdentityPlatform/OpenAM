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
 */
package org.forgerock.openam.saml2.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;

import org.forgerock.openam.wsfederation.common.ActiveRequestorException;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

/**
 * The default {@link WsFedAuthenticator} implementation that just authenticates using the default authentication chain
 * in the selected realm.
 */
public class DefaultWsFedAuthenticator implements WsFedAuthenticator {

    private static final Debug DEBUG = Debug.getInstance("libWSFederation");

    @Override
    public SSOToken authenticate(HttpServletRequest request, HttpServletResponse response, SOAPMessage soapMessage,
            String realm, String username, char[] password) throws ActiveRequestorException {
        try {
            AuthContext authContext = new AuthContext(realm);
            authContext.login(request, response);

            while (authContext.hasMoreRequirements()) {
                Callback[] callbacks = authContext.getRequirements();
                if (callbacks == null || callbacks.length == 0) {
                    continue;
                }
                List<Callback> missing = new ArrayList<>();
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nc = (NameCallback) callback;
                        nc.setName(username);
                    } else if (callback instanceof PasswordCallback) {
                        PasswordCallback pc = (PasswordCallback) callback;
                        pc.setPassword(password);
                    } else {
                        missing.add(callback);
                    }
                }

                if (missing.size() > 0) {
                    throw ActiveRequestorException.newSenderException("unableToAuthenticate");
                }
                authContext.submitRequirements(callbacks);
            }

            if (AuthContext.Status.SUCCESS.equals(authContext.getStatus())) {
                return authContext.getSSOToken();
            }
        } catch (AuthLoginException ale) {
            DEBUG.error("An error occurred while trying to authenticate the end-user", ale);
        } catch (L10NMessageImpl l10nm) {
            DEBUG.error("An error occurred while trying to obtain the session ID during authentication", l10nm);
        }

        throw ActiveRequestorException.newSenderException("unableToAuthenticate");
    }
}
