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
 * $Id: OpenSSOJACCPolicyConfigurationFactory.java,v 1.1 2009/01/30 12:09:40 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr115;

import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyConfiguration;
import com.sun.enterprise.security.provider.PolicyConfigurationFactoryImpl;

/**
 *
 * OpenSSOJACCPolicyConfigurationFactory is a factory class that is responsible 
 * for providing implementation to OpenSSOJACCPolicyConfiguration class. 
 * 
 * Factory is enrolled into the container as part of the configuration setting.
 * 
 * @see javax.security.jacc.PolicyConfigurationFactory
 * 
 */
public class OpenSSOJACCPolicyConfigurationFactory 
        extends PolicyConfigurationFactory {
    
    PolicyConfigurationFactoryImpl defaultFactory ;
    
    /**
     *  @see javax.security.jacc.PolicyConfigurationFactory
     */   
    
    public OpenSSOJACCPolicyConfigurationFactory() {
        defaultFactory = new PolicyConfigurationFactoryImpl();
    }
    
    /**
     *  @see javax.security.jacc.PolicyConfigurationFactory
     */       
    
    public PolicyConfiguration getPolicyConfiguration(String contextID, boolean remove)
            throws javax.security.jacc.PolicyContextException {              
        if (SharedState.isAdminApp(contextID))
            return defaultFactory.getPolicyConfiguration(contextID, remove);
        else
            return OpenSSOJACCPolicyConfiguration.getPolicyConfig(contextID, remove);
    }
    
    /**
     *  @see javax.security.jacc.PolicyConfigurationFactory
     */       
        
    public boolean inService(String contextID)
            throws javax.security.jacc.PolicyContextException {   
        if (SharedState.isAdminApp(contextID))
            return defaultFactory.inService(contextID);
        else
            return OpenSSOJACCPolicyConfiguration.inService(contextID);
    }

}
