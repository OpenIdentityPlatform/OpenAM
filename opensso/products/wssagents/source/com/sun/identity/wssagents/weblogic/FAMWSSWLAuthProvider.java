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
 * $Id: FAMWSSWLAuthProvider.java,v 1.4 2008/08/19 19:15:12 veiming Exp $
 *
 */

package com.sun.identity.wssagents.weblogic;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import weblogic.management.security.ProviderMBean;
import weblogic.security.provider.PrincipalValidatorImpl;
import weblogic.security.spi.IdentityAsserterV2;
import weblogic.security.spi.PrincipalValidator;
import weblogic.security.spi.AuthenticationProviderV2;
import weblogic.security.spi.SecurityServices;

/**
 * This class serves as a customized authentication provider for WebLogic.
 * It is used by OpenSSO webservices security plugins to set the
 * user principals to the weblogic server.
 */
public class FAMWSSWLAuthProvider implements AuthenticationProviderV2 {

    /**
     * Method declaration
     *
     * @param mbean 
     * @param services
     *
     */
    public void initialize(ProviderMBean mbean, SecurityServices services) {
    }
    
    /**
     * Method declaration
     *
     * @return
     *
     * @see
     */
    public String getDescription() {
        return "FAMWSS Authentication Provider";
    }
    
    /**
     * Method declaration
     *
     * @see
     */
    public void shutdown() {
    }
    
    /**
     * Method declaration
     *
     * @return
     *
     * @see
     */
    public AppConfigurationEntry getLoginModuleConfiguration() {
        return getAppConfigurationEntry();
    }
    
    /**
     * Method declaration
     *
     * @return
     *
     * @see
     */
    public AppConfigurationEntry getAssertionModuleConfiguration() {
        return getAppConfigurationEntry();
    }
    
    /**
     * Method declaration
     *
     * @return
     *
     * @see
     */
    public IdentityAsserterV2 getIdentityAsserter() {
        return null;
    }
    
    
    /**
     * Since we use the default WebLogic impl of the JAAS security principal,
     * we need to use the WebLogic impl of the principal validator
     *
     * @return
     *
     * @see
     */
    public PrincipalValidator getPrincipalValidator() {
        return new PrincipalValidatorImpl();
    }
    
    /**
     * Method declaration
     *
     * @return
     *
     * @see
     */
    private AppConfigurationEntry getAppConfigurationEntry() {
        AppConfigurationEntry entry =
                new AppConfigurationEntry(
                "com.sun.identity.wssagents.weblogic.FAMWSSWLLoginModule",
                 LoginModuleControlFlag.OPTIONAL, new HashMap());
        
        return entry;
    }
    
}


