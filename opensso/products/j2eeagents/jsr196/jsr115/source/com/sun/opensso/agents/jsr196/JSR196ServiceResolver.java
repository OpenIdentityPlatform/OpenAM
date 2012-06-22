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
 * $Id: JSR196ServiceResolver.java,v 1.1 2009/01/30 12:09:41 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr196;

import com.sun.identity.agents.arch.ServiceResolver;
import com.sun.identity.agents.filter.GenericJ2EELogoutHandler;
import com.sun.identity.agents.realm.GenericExternalVerificationHandler;
import com.sun.identity.agents.filter.J2EEAuthenticationHandler;

/**
 *
 * @author kalpana
 * 
 * The <code>JSR196ServiceResolver</code> provides the necessary means to access
 * the read-only configuration information for the Agent runtime. 
 * 
 */
public class JSR196ServiceResolver extends ServiceResolver {
    /*
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
    
    /**
     * 
     * @return the ClassName of the implementation of AmFilter
     */
           
    @Override
    public String getAmFilterImpl() {
        return PsuedoAmFilter.class.getName();
    }
    
    /**
     * 
     * @return the dummy URLPolicyTaskHandler's class name
     */
    
    @Override
    public String getURLPolicyTaskHandlerImpl() {
        return JSR196URLPolicyTaskHandler.class.getName();
    }
    

}
