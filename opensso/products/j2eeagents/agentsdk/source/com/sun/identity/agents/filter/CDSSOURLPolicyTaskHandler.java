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
 *
 */

package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.policy.AmWebPolicyResult;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for URL policy enforcement.
 * </p>
 */
public class CDSSOURLPolicyTaskHandler extends URLPolicyTaskHandler 
implements ICDSSOURLPolicyTaskHandler {    
   
    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException if this task handler could not be initialized
     */
    public CDSSOURLPolicyTaskHandler(Manager manager) 
        throws AgentException 
    {
        super(manager);
    }   
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
    }
    
    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_CDSSO_URL_POLICY_TASK_HANDLER_NAME;
    }
   
    public AmFilterResult getServeDataResult(AmFilterRequestContext ctx,
                                               AmWebPolicyResult policyResult)
        throws AgentException 
    {
        if (isLogMessageEnabled()) {
            logMessage("CDSSOURLPolicyTaskHandler: insufficient credentials - "
                     + "Redirecting for Authentication");
        }
               
        // We need to save the Original request. Otherwise we will loose
        // the original request.
        String gotoURL = ctx.populateGotoParameterValue();
        CDSSOContext cdssoContext = getCDSSOContext();
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
         
        String authnRequestID = cdssoContext.getSAMLHelper().generateID(); 
                       
        response.addCookie(cdssoContext.createCDSSOCookie(gotoURL, 
            request.getMethod(), authnRequestID));   
                  
        // Reset the Original SSOToken
        response.addCookie(cdssoContext.getRemoveSSOTokenCookie());
       
        AmFilterResult result = null;
        if (cdssoContext.isAuthnResponseEnabled()) {
            result = cdssoContext.getRedirectResult(ctx, policyResult, 
                authnRequestID);
        } else { 
            // IS 6.0 sp1 CDC servlet bug workaround. Instead of redirecting to
            // CDCServlet, we redirect to Auth Login URL with the goto value set
            // to CDSSO redirect URL
            gotoURL = cdssoContext.getCDSSORedirectURL(request);
            result = ctx.getAuthRedirectResult(policyResult, gotoURL);
        }       
        return result;                
    }
   
    private CDSSOContext getCDSSOContext() {
        return (CDSSOContext) getSSOContext();
    }
}
