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
 * $Id: PsuedoAmAgentFilter.java,v 1.1 2009/01/30 12:09:41 kalpanakm Exp $
 *
 */

package com.sun.opensso.agents.jsr196;

import com.sun.identity.agents.filter.AmAgentBaseFilter;
import com.sun.identity.agents.filter.AmFilterMode;
import com.sun.identity.agents.filter.AmFilterManager;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.filter.AmFilterResultStatus;
import com.sun.identity.agents.arch.Manager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * 
 * PsuedoAmAgentFilter is used to get the access into the agentsdk as a Filter
 * 
 * @author kalpana
 * 
 */
public class PsuedoAmAgentFilter extends AmAgentBaseFilter {
    
    private PsuedoAmFilter filter;
    
    protected AmFilterMode getDefaultFilterMode() {
        return AmFilterMode.MODE_ALL;
    }

    /* (non-Javadoc)
     * @see com.sun.identity.agents.filter.AmAgentBaseFilter#getAllowedFilterModes()
     */
    protected AmFilterMode[] getAllowedFilterModes() {
        return ALLOWED_MODES; 
    }

    private static final AmFilterMode[] ALLOWED_MODES = new AmFilterMode[] { 
        //TODO: Need to Test all the modes.
            AmFilterMode.MODE_NONE, AmFilterMode.MODE_SSO_ONLY, 
            AmFilterMode.MODE_J2EE_POLICY, AmFilterMode.MODE_URL_POLICY, 
            AmFilterMode.MODE_ALL
    };
    
    /**
     * Helps in initializing the Filter framework for the provider
     */
    
    public void initializeForJSR196(){
        
        try {        
            filter = (PsuedoAmFilter) AmFilterManager.getAmFilterInstance(getDefaultFilterMode());   
            filter.initialize(getDefaultFilterMode());                        
        } catch(Exception e) {
                        
        }                        
    }       
    
    /**
     * 
     * gets the loginurl
     *     
     * @param req
     * @param res
     * @return
     * @throws java.lang.Exception
     */
    
    public String getLoginURL(HttpServletRequest req, HttpServletResponse res) throws Exception {
        return filter.getLoginURL(req, res);
    }
    
    /**
     * 
     * @return manager associated with the filter component
     */
    
    public Manager getManager() {
       return filter.getManager();                     
    }    
    
    /**
     * Checks whether the request is allowed or not based on the AgentProperties
     * @param req
     * @param res
     * @return true if the request is allowed , false if the request needs
     *         authentication
     */
    
    boolean getConfigurationResult(HttpServletRequest req, HttpServletResponse res) {
        AmFilterResult result = filter.isAccessAllowed(req, res);
        boolean status = true;
        switch(result.getStatus().getIntValue()) {
            case AmFilterResultStatus.INT_STATUS_CONTINUE :
                status = true;
                break;

            case AmFilterResultStatus.INT_STATUS_FORBIDDEN :
                status = false;
                break;

            case AmFilterResultStatus.INT_STATUS_REDIRECT :
                status = false;
                break;

            case AmFilterResultStatus.INT_STATUS_SERVE_DATA :
                status = true;
                break;

            case AmFilterResultStatus.INT_STATUS_SERVER_ERROR :
                 status = false;
                 break;
            }
      
        return status;
    }      
}
