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

public class CDSSOTaskHandler extends SSOTaskHandler 
implements ICDSSOTaskHandler {
    
    public CDSSOTaskHandler(Manager manager) 
    {
        super(manager);        
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
        throws AgentException 
    {
        super.initialize(context, mode);
    }
            
    protected AmFilterResult doSSOLogin(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        
        CDSSOContext cdssoContext = getCDSSOContext();

        if(cdssoContext.getLoginAttemptLimit() > 0) {
            int loginAttempt = cdssoContext.getLoginAttemptValue(ctx);
            if (loginAttempt >= 0) {
                if (loginAttempt < cdssoContext.getLoginAttemptLimit()) {

                    if (isLogWarningEnabled()) {
                        logWarning("CDSSOTaskHandler: Login attempt number "
                                   + loginAttempt + " failed for request URI: "
                                   + request.getRequestURI());
                    }
                    
                    result = setCookiesAndGetRedirectResult(ctx, loginAttempt);
                } else { // Login attempts have exceeded the limit

                    if(isLogWarningEnabled()) {
                        logWarning(
                            "CDSSOTaskHandler: number of login attempts have "  
                            + "exceeded the set limit. Access blocked for " 
                            + "request URI: " + request.getRequestURI());
                    }

                    result = ctx.getBlockAccessResult();
                }
            } else {

                if(isLogWarningEnabled()) {
                    logWarning(
                        "CDSSOTaskHandler: Invalid value found for counter " 
                        + "cookie. Denying access to request URI: "
                        + request.getRequestURI());
                }

                result = ctx.getBlockAccessResult();
            }
        } else {             
            result = setCDSSOCookieAndGetRedirectResult(ctx);
        }

        return result;
    }
    
    /**
     * Returns a boolean value indicating if this task handler is enabled or
     * not.
     * 
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return true;
    }    
    
    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_CDSSO_TASK_HANDLER_NAME;
    }
    
    private AmFilterResult setCookiesAndGetRedirectResult(
        AmFilterRequestContext ctx, 
        int loginAttempt) 
        throws AgentException
    {                       
        CDSSOContext cdssoContext = getCDSSOContext();
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        
        String authnRequestID = "";                            
        if (loginAttempt == 0) { // Set the request ID
            String gotoURL = ctx.populateGotoParameterValue();
            authnRequestID =  cdssoContext.getSAMLHelper().generateID();        
            response.addCookie(cdssoContext.createCDSSOCookie(gotoURL, 
                request.getMethod(), authnRequestID));
        } else {
            // Get the requestID from the cookie set previously
            authnRequestID = cdssoContext.getAuthnRequestID(ctx);                            
        }
        
        response.addCookie(cdssoContext.getNextLoginAttemptCookie(
            loginAttempt));
                    
        return cdssoContext.getRedirectResult(ctx, null, authnRequestID);     
    }
    
    private AmFilterResult setCDSSOCookieAndGetRedirectResult(
        AmFilterRequestContext ctx) throws AgentException
    {
        // We need to save the Original request. Otherwise we will loose
        // the original request.
        String gotoURL = ctx.populateGotoParameterValue();
        CDSSOContext cdssoContext = getCDSSOContext();
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        
        String authnRequestID = cdssoContext.getSAMLHelper().generateID(); 
                       
        response.addCookie(cdssoContext.createCDSSOCookie(gotoURL, 
            request.getMethod(), authnRequestID));                                    

        return cdssoContext.getRedirectResult(ctx, null, authnRequestID);        
    }
    
    private CDSSOContext getCDSSOContext() {
        return (CDSSOContext) getSSOContext();
    }
}
