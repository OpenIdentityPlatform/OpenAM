/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SystemAppTokenProvider.java,v 1.3 2008/06/25 05:43:57 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.security;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * This is an default implementation of <code>AppSSOTokenProvider</code>.
 * It creates application single sign on token with credentials of
 * user name and password which are parameters to the constructor.
 */
/* iPlanet-PUBLIC-CLASS */
public class SystemAppTokenProvider implements AppSSOTokenProvider
{
    /**
     * Application module name
     */
    private static final String MODULE_APPLICATION = "Application";

    private String appUserName;
    private String appPassword;

    /**
     * Constructs a <code>SystemAppTokenProvider</code>.
     *
     * @param appUserName identity to authenticate.
     * @param appPassword password of <code>appUserName</code>.
     */
    public SystemAppTokenProvider(String appUserName, String appPassword) {
        this.appUserName = appUserName;
        this.appPassword = appPassword;
    }


    /**
     * Returns Application single sign on token.
     *
     * @return application single sign on token.
     */
    public SSOToken getAppSSOToken() {
	SSOToken ssoToken = null;

        try {
            AuthContext authContext = new AuthContext("/");
            authContext.login(
                AuthContext.IndexType.MODULE_INSTANCE, MODULE_APPLICATION);

            if (authContext.hasMoreRequirements()) {
                Callback[] callbacks = authContext.getRequirements();

                if (callbacks != null) {
                    addLoginCallbackMessage(callbacks, appUserName, appPassword);
                    authContext.submitRequirements(callbacks);
                }
            }

            if (authContext.getStatus() == AuthContext.Status.SUCCESS) {
                ssoToken = authContext.getSSOToken();
            }
        } catch (AuthLoginException ale) {
            AdminTokenAction.debug.error(
		"SystemAppTokenProvider.getAppSSOToken()", ale);
        } catch (UnsupportedCallbackException usce) {
            AdminTokenAction.debug.error(
		"SystemAppTokenProvider.getAppSSOToken()", usce);
        } catch (Exception e) {
            AdminTokenAction.debug.error(
		"SystemAppTokenProvider.getAppSSOToken()", e);
        }

	return ssoToken;
    }

    /**
     * Adds callback message
     *
     * @param callbacks  array of callbacks
     * @param appUserName  application user name
     * @param appPassword for application user
     */
    private void addLoginCallbackMessage(
	Callback[] callbacks,
	String appUserName,
	String appPassword)
	throws UnsupportedCallbackException
    {
	for (int i = 0; i < callbacks.length; i++) {
	    if (callbacks[i] instanceof NameCallback) {
		NameCallback nameCallback = (NameCallback) callbacks[i];
		nameCallback.setName(appUserName);
	    } else if (callbacks[i] instanceof PasswordCallback) {
		PasswordCallback pwdCallback = (PasswordCallback) callbacks[i];
		pwdCallback.setPassword(appPassword.toCharArray());
	    }
	}
    }
}
