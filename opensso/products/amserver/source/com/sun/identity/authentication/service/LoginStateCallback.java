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
 * $Id: LoginStateCallback.java,v 1.2 2008/06/25 05:42:05 qcheng Exp $
 *
 */



package com.sun.identity.authentication.service;

import javax.security.auth.callback.Callback;

/**
 * <p> Class implementations of <code>Callback</code> interface.
 * It is passed to a <code>CallbackHandler</code>, allowing underlying 
 * auth services to interact with a calling components to retrieve login state.
 */
public class LoginStateCallback implements Callback {
    private LoginState loginState = null;

    /**
     * Creates <code>LoginStateCallback</code> object.
     */
    public LoginStateCallback() {
    }
    
    /**
     * Returns Login State.
     *
     * @return Login State.
     */
    public LoginState getLoginState() {
        return loginState;
    }

    /**
     * Sets Login State.
     *
     * @param state Login State.
     */
    public void setLoginState(LoginState state) {
        loginState = state;
    }
}
