/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ISSOContext.java,v 1.4 2008/07/02 18:27:12 leiming Exp $
 *
 */

package com.sun.identity.agents.filter;

import javax.servlet.http.Cookie;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.common.ICookieResetHelper;
import com.sun.identity.agents.common.ISSOTokenValidator;

/**
 * The interface for <code>SSOContext</code>. It encapsulates all the
 * configuration and intializations of agent filter.
 */
public interface ISSOContext {
    public abstract void initialize(AmFilterMode filterMode)
            throws AgentException;

    public abstract int getLoginAttemptValue(AmFilterRequestContext ctx);

    public abstract Cookie getNextLoginAttemptCookie(int currentValue) throws AgentException;

    /**
     * creates and returns a new SSOToken cookie.
     * @param tokenValue - a URL decoded value.
     * @return a SSO Tokem Cookie
     */
    public abstract Cookie[] createSSOTokenCookie(String tokenValue);

    public abstract Cookie getRemoveSSOTokenCookie();

    public abstract boolean isSSOCacheEnabled();

    public abstract int getLoginAttemptLimit();

    public abstract ICookieResetHelper getCookieResetHelper();

    public abstract String getLoginCounterCookieName();

    public abstract ISSOTokenValidator getSSOTokenValidator();
}
