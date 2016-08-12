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

package org.forgerock.openam.authentication.service;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

/**
 * Wrapper class for the javax implementation of LoginContext.
 */
public class LoginContextWrapper implements LoginContext{
    javax.security.auth.login.LoginContext javaxContext;

    /**
     * Create an instance of the wrapper based on an existing LoginContext.
     * @param javaxContext The original LoginContext.
     */
    public LoginContextWrapper(javax.security.auth.login.LoginContext javaxContext) {
        this.javaxContext = javaxContext;
    }


    @Override
    public void login() throws LoginException {
        javaxContext.login();
    }

    @Override
    public void logout() throws LoginException {
        javaxContext.logout();
    }

    @Override
    public Subject getSubject() {
        return javaxContext.getSubject();
    }
}
