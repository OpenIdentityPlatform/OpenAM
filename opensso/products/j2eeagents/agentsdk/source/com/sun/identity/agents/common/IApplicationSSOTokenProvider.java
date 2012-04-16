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
 * $Id: IApplicationSSOTokenProvider.java,v 1.3 2009/04/02 00:02:11 leiming Exp $
 *
 */

package com.sun.identity.agents.common;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;

/**
 * The interface for ApplicationSSOTokenProvider
 */
public interface IApplicationSSOTokenProvider {
    public abstract void initialize();

    public abstract SSOToken getApplicationSSOToken(boolean addHook)
            throws AgentException;
    
    public static final String MODULE_APPLICATION = "Application";
}
