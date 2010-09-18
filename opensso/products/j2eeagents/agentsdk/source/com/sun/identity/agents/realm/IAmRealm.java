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
 * $Id: IAmRealm.java,v 1.3 2008/06/25 05:51:58 qcheng Exp $
 *
 */

package com.sun.identity.agents.realm;

import java.util.Set;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.common.SSOValidationResult;

/**
 * The interface for agent realm implementation
 */
public interface IAmRealm extends IRealmConfigurationConstants {
    public abstract void initialize() throws AgentException;
    
    public abstract AmRealmAuthenticationResult authenticate(
            SSOValidationResult ssoValidationResult);

    public abstract AmRealmAuthenticationResult authenticate(String userName,
            String transportString);
       
    /**
     ** @return null if getRealmMembershipCacheFlag = false and do does not
     *         need to check the cache since it is not enabled.
     *         and if getRealmMembershipCacheFlag = true then returns the set of
     *         memberships for the userName.
     */
    public abstract Set getMemberships(String userName);

    public static final int FETCH_LEVEL_NONE = 0;

    public static final int FETCH_ROLES = 1;

    public static final int FETCH_FILTERED_ROLES = 2;

    public static final int FETCH_GROUPS = 4;

    public static final int FETCH_DYNAMIC_GROUPS = 8;

    public static final int FETCH_LEVEL_ALL = 15;

}
