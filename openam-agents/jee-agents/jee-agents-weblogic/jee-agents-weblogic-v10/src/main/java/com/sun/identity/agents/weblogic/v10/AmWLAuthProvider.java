/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AmWLAuthProvider.java,v 1.3 2008/06/25 05:52:22 qcheng Exp $
 *
 */

package com.sun.identity.agents.weblogic.v10;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import weblogic.management.security.ProviderMBean;
import weblogic.security.provider.PrincipalValidatorImpl;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.SecurityServices;

import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.arch.IModuleAccess;

/**
 * This class serves as an customized Authenication provider for WebLogic.
 *
 */
public class AmWLAuthProvider implements AuthenticationProviderV2 {

    
    /**
     * @see weblogic.security.spi.SecurityProvider
     */
    public void initialize(ProviderMBean mbean, SecurityServices services) {
        IModuleAccess modAccess = AmRealmManager.getModuleAccess();
        if (modAccess.isLogMessageEnabled()) {
            modAccess.logMessage(
                "AmWLAuthProvider: Initialize AMAuthProvider " + _agentType);
        }
        
        // FIXME
        // Looks like a classloader issue. The system class loader does
        // not seem to have an access to the class loader loading the mbeans
        controlFlag = LoginModuleControlFlag.OPTIONAL;
    }
    
    /**
     * @see weblogic.security.spi.SecurityProvider
     */
    public String getDescription() {
        return "Agent Authentication Provider";
    }
    
    /**
     * @see weblogic.security.spi.SecurityProvider
     */
    public void shutdown() {
        IModuleAccess modAccess = AmRealmManager.getModuleAccess();
        if (modAccess.isLogMessageEnabled()) {
            modAccess.logMessage(
                    "AmWLAuthProvider: AMAuthProvider.shutdown "+ _agentType);
        }
    }
    
    /**
     * @return the JAAS configuration specific to this Authentication provider
     *         that is needed to properly execute login authentication in 
     *         this security realm.
     *
     * @see weblogic.security.spi.AuthenticationProviderV2
     */
    public AppConfigurationEntry getLoginModuleConfiguration() {
        return getAppConfigurationEntry();
    }
    
    /**
     *
     * @return the JAAS configuration specific to an Identity Assertion 
     *         provider that is needed to properly execute identity 
     *         assertion in this security realm.
     *
     * @see weblogic.security.spi.AuthenticationProviderV2
     */
    public AppConfigurationEntry getAssertionModuleConfiguration() {
        return getAppConfigurationEntry();
    }
    
    /**
     * @return the Authentication provider's associated Identity Assertion provider.
     *
     * @see weblogic.security.spi.AuthenticationProviderV2
     */
    public IdentityAsserterV2 getIdentityAsserter() {
        return null;
    }
    
    
    /**
     * Since we use the default WebLogic impl of the JAAS security principal,
     * we need to use the WebLogic impl of the principal validator
     *
     * @return  this Authentication provider's associated Principal Validation provider.
     *
     * @see weblogic.security.spi.AuthenticationProviderV2
     */
    public PrincipalValidator getPrincipalValidator() {
        return new PrincipalValidatorImpl();
    }
    

    private AppConfigurationEntry getAppConfigurationEntry() {
        AppConfigurationEntry entry =
                new AppConfigurationEntry(
                "com.sun.identity.agents.weblogic.v10.AmWLLoginModule",
                controlFlag, new HashMap());
        
        return entry;
    }
    
    private String		        description;
    private LoginModuleControlFlag      controlFlag;
    private static String _agentType = "weblogic10";
}


