/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.agents.jetty.v7;

import java.io.IOException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.realm.AmRealmManager;
import org.eclipse.jetty.plus.jaas.callback.ObjectCallback;
import org.eclipse.jetty.plus.jaas.spi.AbstractLoginModule;
import org.eclipse.jetty.plus.jaas.spi.UserInfo;

/**
 *
 * @author Peter Major
 */
public class AgentLoginModule extends AbstractLoginModule {

    private static IModuleAccess moduleAccess = AmRealmManager.getModuleAccess();

    @Override
    public boolean login() throws LoginException {

        try {
            if (getCallbackHandler() == null) {
                logMessage("No callback handler");
                throw new LoginException("No callback handler");
            }

            Callback[] callbacks = configureCallbacks();
            getCallbackHandler().handle(callbacks);

            String webUserName = ((NameCallback) callbacks[0]).getName();

            Object webCredential = ((ObjectCallback) callbacks[1]).getObject(); //first check if ObjectCallback has the credential
            if (webCredential == null) {
                webCredential = ((PasswordCallback) callbacks[2]).getPassword(); //use standard PasswordCallback
            }
            if ((webUserName == null) || (webCredential == null)) {
                setAuthenticated(false);
                return isAuthenticated();
            }

            UserInfo userInfo = getUserInfo(webUserName);

            if (userInfo == null) {
                setAuthenticated(false);
                return isAuthenticated();
            }

            setAuthenticated(userInfo.checkCredential(webCredential));
            setCurrentUser(new JAASUserInfo(userInfo));
            return isAuthenticated();
        } catch (IOException e) {
            logMessage(e.getMessage());
            throw new LoginException(e.toString());
        } catch (UnsupportedCallbackException e) {
            logMessage(e.getMessage());
            throw new LoginException(e.toString());
        } catch (Exception e) {
            logMessage(e.getMessage());
            throw new LoginException(e.toString());
        }
    }

    @Override
    public UserInfo getUserInfo(String username) throws Exception {
        return new AgentUserInfo(username, null, null);
    }

    private void logMessage(String message) {

        if (moduleAccess != null && moduleAccess.isLogMessageEnabled()) {
            moduleAccess.logMessage(message);
        }
    }
}
