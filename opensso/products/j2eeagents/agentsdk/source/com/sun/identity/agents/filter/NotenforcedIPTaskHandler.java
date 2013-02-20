/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NotenforcedIPTaskHandler.java,v 1.3 2009/05/26 22:47:58 leiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.agents.filter;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.INotenforcedIPHelper;

/**
 * <p>
 * Provides the necessary functionality to evaluate incoming
 * requests against the Not Enforced IP address list as set
 * in the Agent Configuration
 * </p>
 */

public class NotenforcedIPTaskHandler extends AmFilterTaskHandler
        implements INotenforcedIPTaskHandler {

    private LogoutHelper helper;

    /**
     * The constructor takes a <code>Manager</code> instance in order
     * to gain access to the infrastructure services such as configuration
     * and log access
     *
     * @param manager the <code>Manager</code> for the <code>filter</code> 
     * subsystem
     */
    
    public NotenforcedIPTaskHandler(Manager manager)  {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode)
    throws AgentException {
        super.initialize(context, mode);
        boolean cacheEnabled = getConfigurationBoolean(
                CONFIG_NOTENFORCED_IP_CACHE_FLAG,
                DEFAULT_NOTENFORCED_IP_CACHE_FLAG);
        
        int cacheSize = getConfigurationInt(CONFIG_NOTENFORCED_IP_CACHE_SIZE,
                DEFAULT_NOTENFORCED_IP_CACHE_SIZE)/2;
        
        boolean isInverted = getConfigurationBoolean(
                CONFIG_INVERT_NOTENFORCED_IP_FLAG,
                DEFAULT_INVERT_NOTENFORCED_IP_FLAG);
        
        String[] entries = getConfigurationStrings(CONFIG_NOTENFORCED_IP_LIST);
        
        CommonFactory cf = new CommonFactory(getModule());
        setNotenforcedIPHelper(cf.newNotenforcedIPHelper(
                cacheEnabled, cacheSize, isInverted, entries));
        helper = new LogoutHelper(this);
    }
    
    /**
     * Evaluates IP address of the incoming request against not
     * enforced IP address list as specified in the agent configuration
     * <br>
     * @param ctx an <code>AmFilterRequestContext</code> object
     * that carries information about the incoming request and
     * response objects
     * @return <code>null</code> when enforcement is necessary or an
     * <code>AmFilterResult</code> object indicating the necessary
     * action required to handle the non-enforcement
     */
    
    public AmFilterResult process(AmFilterRequestContext ctx)
    throws AgentException {
        AmFilterResult result = null;
        if (isNotEnforceIP(ctx)) {
            //if non enforcement is true, skip the rest
            result = ctx.getContinueResult();
            result.markAsNotEnforced();

            HttpServletRequest request = ctx.getHttpServletRequest();
            if (isModeJ2EEPolicyActive()) {
                if ((request.getUserPrincipal() != null || request.getRemoteUser() != null)
                        && !getSSOTokenValidator().validate(request).isValid()) {
                    try {
                        helper.doLogout(ctx);
                        //if the logout was successful we need to redirect the
                        //user, because the current request will still see the
                        //just logged out user.
                        result = ctx.getRedirectToSelfResult();
                    } catch (AgentException ae) {
                        logWarning("Unable to log out the user while processing not enforced URI's", ae);
                    }
                }
            }
        }
        return result;
    }
    
    
    /**
     * Returns a boolean value indicating if this task handler is enabled
     * @return true if this task handler is enabled, otherwise returns "false"
     */
    public boolean isActive() {
        return isModeSSOOnlyActive() && (getNotenforcedIPHelper().isActive());
    }
    
    /**
     * Returns this task handlers name
     * @return String containing name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_IP_ENFORCER_TASK_HANDLER_NAME;
    }
    
    
    /**
     * Method isNotEnforceIP
     * Returns true if client IP address matches any not enforced list
     * IP address and not subject to authentication or authorization
     *
     * @param ctx is a <code>AmFilterRequestContext</code> object containing
     * <code>HttpServletRequest</code> and <code>HttpServletResponse</code> 
     * objects
     * @return true if there is a match of the client IP address against the
     * specified list of IP addresses and enforcement is <b>not required</b>
     */
    private boolean isNotEnforceIP(AmFilterRequestContext ctx) {

        // form login url should not be in not-enforced-list
        if (ctx.isFormLoginRequest()) {
            return false;
        }
        //return true is there is a match
        String clientIP = getClientIP(ctx);
        boolean notEnforced = getNotenforcedIPHelper().isNotenforced(clientIP);
        if (notEnforced) {
            if(isLogMessageEnabled()) {
                logMessage("NotenforcedIPTaskHandler: The client IP" + clientIP
                        + " was found in Not Enforced IP List");
            }
        }
        return notEnforced;
    }
    
    /**
     * Method getClientIP
     * Returns a String representation of the client IP address in network
     * format
     * @param ctx is a <code>AmFilterRequestContext</code> object containing
     * <code>HttpServletRequest</code> and <code>HttpServletResponse</code> 
     * objects
     * @return remote_ip a String representation of client ip address
     */
    private String getClientIP(AmFilterRequestContext ctx) {
        HttpServletRequest request = ctx.getHttpServletRequest();
        return getSSOTokenValidator().getClientIPAddress(request);
    }
    
    /**
     * Method getNotEnforcedIPManager
     * Returns a <code>NotEnforcedIPManager</code> object
     * @return _notEnforcedIPManager an instance of <code>NotEnforcedIPManager
     * </code>
     */
    private  INotenforcedIPHelper getNotenforcedIPHelper() {
        return _notenforcedIPHelper;
    }
    
    /**
     * Method setNotEnforcedIPManager
     * Sets the <code>NotEnforcedIPManager</code> object to an instance variable
     */
    private void setNotenforcedIPHelper(INotenforcedIPHelper helper) {
        _notenforcedIPHelper = helper;
    }
    
    private INotenforcedIPHelper _notenforcedIPHelper;
}
