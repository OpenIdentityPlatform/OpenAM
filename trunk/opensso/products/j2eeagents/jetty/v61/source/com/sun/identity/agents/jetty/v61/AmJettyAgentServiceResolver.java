/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: AmJettyAgentServiceResolver.java,v 1.1 2009/01/21 18:39:39 kanduls Exp $
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.jetty.v61;

import com.sun.identity.agents.arch.ServiceResolver;
import com.sun.identity.agents.filter.GenericJ2EELogoutHandler;
import com.sun.identity.agents.filter.J2EEAuthenticationHandler;
import com.sun.identity.agents.realm.GenericExternalVerificationHandler;

/**
 * This class overrides the default service resolvers J2EE auth handler to
 * return a AmJettyAgent specific J2EE auth handler
 */
public class AmJettyAgentServiceResolver extends ServiceResolver {

    /* (non-Javadoc)
     * @see ServiceResolver#getGlobalJ2EEAuthHandlerImpl()
     */
    public String getGlobalJ2EEAuthHandlerImpl() {
        return J2EEAuthenticationHandler.class.getName();
    }
    
    /* (non-Javadoc)
     * @see ServiceResolver#getGlobalJ2EELogoutHandlerImpl()
     */
    public String getGlobalJ2EELogoutHandlerImpl() {
        return GenericJ2EELogoutHandler.class.getName();
    }
    
    /* (non-Javadoc)
     * @see ServiceResolver#getGlobalVerificationHandlerImpl()
     */
    public String getGlobalVerificationHandlerImpl() {
        return GenericExternalVerificationHandler.class.getName();
    }
    
    /* (non-Javadoc)
     * @see ServiceResolver#getRealmMembershipCacheFlag()
     */
    public boolean getRealmMembershipCacheFlag() {
        return true;
    }
}
