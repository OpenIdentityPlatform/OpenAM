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
 * $Id: ClientSDKAppSSOProvider.java,v 1.3 2009/04/02 00:02:11 leiming Exp $
 *
 */

package com.sun.identity.agents.arch;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IApplicationSSOTokenProvider;
import com.sun.identity.security.AppSSOTokenProvider;
import com.sun.identity.shared.debug.Debug;

/**
 * The class provides an application Single Sign-On token that is used by the 
 * clientsdk of an agent. In particluar it is used in AgentConfiguration
 * bootStrapClientConfiguration() method to set up the observer to make Debug 
 * hotswapable, when DebugPropertiesObserver.getInstance() is invoked it 
 * eventually results in the com.sun.identity.security.AdminTokenAction.run 
 * method creating an instance of this class.
 *
 * The name of this class must be pushed into system properties so that client
 * SDK can use this name to create a new instance of this class as its plugin to
 * get a token.
 */
public class ClientSDKAppSSOProvider implements AppSSOTokenProvider {
   
   //this class is the plugin used by ClientSDK AdminToken class
   public static final String APP_SSO_PROVIDER_PLUGIN = 
           "com.sun.identity.agents.arch.ClientSDKAppSSOProvider";
   
   public static final String CLIENT_SDK_ADMIN_TOKEN_PROPERTY =  
           "com.sun.identity.security.AdminToken";
   
   public ClientSDKAppSSOProvider() { 
       setDebug(Debug.getInstance(IBaseModuleConstants.BASE_RESOURCE));
   }
    
    /* (non-Javadoc)
     * @see com.sun.identity.security.AppSSOTokenProvider#getAppSSOToken()
     */
    public SSOToken getAppSSOToken() {               
        SSOToken result = null;  
        try {
            CommonFactory cf = new CommonFactory(BaseModule.getModule());
            IApplicationSSOTokenProvider provider =
                    cf.newApplicationSSOTokenProvider();

            result = provider.getApplicationSSOToken(false);
            if (getDebug().messageEnabled()) {
                getDebug().message("ClientSDKAppSSOProvider.getAppSSOToken:" +
                        " got SSO Token =" + result);
            }
        } catch (AgentException aex) {
            getDebug().error("ClientSDKAppSSOProvider.getAppSSOToken: Unable" +
                    " to create AppSSOToken", aex);
        }
        return result;
    }
    
    //used for logging debug messages into agent core log
    private void setDebug(Debug debug) {
        _debug = debug;
    }
    
    private Debug getDebug() {
        return _debug;
    } 

    private Debug _debug;
       
}
