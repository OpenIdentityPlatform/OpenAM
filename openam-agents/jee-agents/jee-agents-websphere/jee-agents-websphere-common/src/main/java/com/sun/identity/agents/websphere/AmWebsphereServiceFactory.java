/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmWebsphereServiceFactory.java,v 1.2 2008/11/21 22:21:45 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.arch.ServiceResolver;

/**
 * Websphere specific factory class.
 */
public class AmWebsphereServiceFactory {
    
    public static IAmIdentityAsserter getAmIdentityAsserter(Manager manager)
    throws AgentException {
        IAmIdentityAsserter result = null;
        
        String identityAsserterImplClass =
                getServiceResolver().getAmIdentityAsserterImpl();
        
        try {
            result = (IAmIdentityAsserter) ServiceFactory.getServiceInstance(
                    manager, identityAsserterImplClass);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Failed to obtain service instance "
                    + identityAsserterImplClass, ex);
        }
        
        return result;
    }
    
    public static IAmRealmUserRegistry getAmRealmUserRegistry(Manager manager)
    throws AgentException {
        IAmRealmUserRegistry result = null;
        
        String realmUserRegistryImplClass =
                getServiceResolver().getAmRealmUserRegistryImpl();
        
        try {
            result = (IAmRealmUserRegistry) ServiceFactory.getServiceInstance(
                    manager, realmUserRegistryImplClass);
            result.initialize();
        } catch (Exception ex) {
            throw new AgentException("Failed to obtain service instance "
                    + realmUserRegistryImplClass, ex);
        }
        
        return result;
    }
    
    private static AmWebsphereAgentServiceResolverBase getServiceResolver()
    throws AgentException {
        AmWebsphereAgentServiceResolverBase result = null;
        ServiceResolver resolver = AgentConfiguration.getServiceResolver();
        if (resolver instanceof AmWebsphereAgentServiceResolverBase) {
            result = (AmWebsphereAgentServiceResolverBase) resolver;
        } else {
            throw new AgentException("Incompatible service resolver " +
                    "configured: " + resolver.getClass().getName());
        }
        
        return result;
    }
}
