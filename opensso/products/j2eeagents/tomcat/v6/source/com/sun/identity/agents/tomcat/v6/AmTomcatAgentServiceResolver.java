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
 * $Id: AmTomcatAgentServiceResolver.java,v 1.4 2009/04/01 23:57:02 subbae Exp $
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.agents.tomcat.v6;

import com.sun.identity.agents.arch.ServiceResolver;
import com.sun.identity.agents.filter.GenericJ2EELogoutHandler;
import com.sun.identity.agents.filter.J2EEAuthenticationHandler;
import com.sun.identity.agents.realm.GenericExternalVerificationHandler;

/**
 * This class overrides the default service resolvers J2EE auth handler to
 * return a AmTomcatAgent specific J2EE auth handler
 */
public class AmTomcatAgentServiceResolver extends ServiceResolver {

    public String getGlobalJ2EEAuthHandlerImpl() {
        return J2EEAuthenticationHandler.class.getName();
    }

    /* (non-Javadoc)
     * @see ServiceResolver#getSSOTaskHandlerImpl()
     */
    public String getSSOTaskHandlerImpl() {
        return AmTomcatSSOTaskHandler.class.getName();
    }

    /* (non-Javadoc)
     * @see ServiceResolver#getCDSSOTaskHandlerImpl()
     */
    public String getCDSSOTaskHandlerImpl() {
        return AmTomcatCDSSOTaskHandler.class.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ServiceResolver#getGlobalJ2EELogoutHandlerImpl()
     */
    public String getGlobalJ2EELogoutHandlerImpl() {
        return GenericJ2EELogoutHandler.class.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ServiceResolver#getGlobalVerificationHandlerImpl()
     */
    public String getGlobalVerificationHandlerImpl() {
        return GenericExternalVerificationHandler.class.getName();
    }

    public boolean isLifeCycleMechanismAvailable() {
        return true;
    }
}
