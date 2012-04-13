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
 * $Id: LocalAuthTaskHandler.java,v 1.3 2008/06/25 05:51:47 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.security.Principal;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.SSOValidationResult;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for local authentication in J2EE Policy mode.
 * </p>
 */
public class LocalAuthTaskHandler extends AmFilterTaskHandler 
implements ILocalAuthTaskHandler {

    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException if this task handler could not be initialized
     */
    public LocalAuthTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        setGlobalAuthenticationHandler();
    }

    /**
     * Processes the incoming requests for local authentication and suggests
     * any necessary action needed in order to achieve this functionality.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries 
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     * @throws AgentException if this request cannot be handled by the task
     * handler successfully.
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        boolean authResult = authenticate(ctx);
        if(!authResult) {
            if(isLogWarningEnabled()) {
                logWarning(
                    "LocalAuthTaskHandler: Local authentication failed for : "
                    + ctx.getHttpServletRequest().getRequestURI() 
                    + ", SSO Token: " 
                    + ctx.getSSOValidationResult().getSSOTokenString());
            }

            result = ctx.getBlockAccessResult();
        }
        return result;
    }


    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_LOCAL_AUTH_TASK_HANDLER_NAME;
    }
    
    public boolean isActive() {
        return isModeJ2EEPolicyActive();
    }
    
    protected boolean authenticate(AmFilterRequestContext ctx) 
    throws AgentException {
        boolean authResult  = false;
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        IJ2EEAuthenticationHandler authHandler = getAuthenticationHandler(
                getApplicationName(ctx.getHttpServletRequest()));
        String userName = ctx.getSSOValidationResult().getUserId();
        String ssoTokenID = ctx.getSSOValidationResult().getSSOTokenString();
        

        Principal existingPrincipal = request.getUserPrincipal();
        if (existingPrincipal != null && 
                existingPrincipal.getName().equals(userName)) {
            if (isLogMessageEnabled()) {
                logMessage("LocalAuthTaskHandler: Principal is set, bypassing "
                                + "local authentication for " + userName);
            }
            authResult = true;
        } else {
            if (isLogMessageEnabled()) {
                logMessage("LocalAuthTaskHandler: No principal found. " 
                        + "Initiating local authentication for " + userName);
            }
            if (isSessionBindingEnabled()) {
              authResult = doLocalAuthWithSessionBinding(authHandler,
                  request, response,
                  ctx.getSSOValidationResult());
            } else {
                authResult = doLocalAuthWithoutSessionBinding(authHandler,
                    request, response, ctx.getSSOValidationResult());
            }
        }

        return authResult;        
    }

    /**
     * Method getJ2EEAuthHandler
     *
     *
     * @return
     *
     */
    private IJ2EEAuthenticationHandler getAuthenticationHandler(
            String applicationName)  throws AgentException {
        IJ2EEAuthenticationHandler result =
                (IJ2EEAuthenticationHandler) getAuthenticationHandlers().get(
                applicationName);
        if (result == null)  {
            synchronized(this) {
                result = (IJ2EEAuthenticationHandler)
                       getAuthenticationHandlers().get(applicationName);
                if (result == null) {
                    Map handlerMap = getConfigurationMap(
                            CONFIG_AUTH_HANDLER_MAP);
                    String className = null;
                    if (handlerMap != null) {
                        className = (String) handlerMap.get(applicationName);
                    }
                    if(className != null) {
                        try {
                            result = (IJ2EEAuthenticationHandler)
                                    Class.forName(className).newInstance();
                        } catch (Exception ex) {
                            throw new AgentException(
                                "Unable to initialize authentication handler: "
                                + className + " for application: " 
                                + applicationName, ex);
                        }
                    }

                    if (result == null) {
                        if (isLogMessageEnabled()) {
                            logMessage(
                                "AmFilter: using global authentication handler " 
                                + "for app: "
                                + applicationName);
                        }
                        result = getGlobalAuthenticationHandler();
                    }
                    getAuthenticationHandlers().put(applicationName, result);
                }
            }
        }
        return result;
    }

    private Hashtable getAuthenticationHandlers() {
        return _authenticationHandlers;
    }



    /**
     * Method setJ2EEAuthHandler
     *
     * @throws AgentException
     *
     */
    private void setGlobalAuthenticationHandler() throws AgentException {

        String globalAuthHandlerClassName = 
                getResolver().getGlobalJ2EEAuthHandlerImpl();
        try {
            _globalAuthenticationHandler = (IJ2EEAuthenticationHandler) 
                    Class.forName(globalAuthHandlerClassName).newInstance();
        } catch(Exception ex) {
            throw new AgentException(
                    "Unable to load Global Authentication handler", ex);
        }
    }


    /**
     * Method getGlobalAuthenticationHandler
     *
     * @returns IJ2EEAuthenticationHandler
     *
     */
    private IJ2EEAuthenticationHandler getGlobalAuthenticationHandler() {
           return _globalAuthenticationHandler;
    }

    private boolean doLocalAuthWithSessionBinding(
            IJ2EEAuthenticationHandler authHandler,
            HttpServletRequest request, HttpServletResponse response,
            SSOValidationResult ssoValidationResult) {

        if(isLogMessageEnabled()) {
            logMessage(
                "LocalAuthTaskHandler: doing local authentication with " 
                + "session binding");
        }

        HttpSession session  = request.getSession(true);
        String      password = null;
        String userName = ssoValidationResult.getUserId();

        try {
            password = ssoValidationResult.getTransportString();
        } catch(Exception ex) {
            if(isLogMessageEnabled()) {
                logMessage(
                    "LocalAuthTaskHandler: Exception caught while doing local " 
                    + "authentication",
                    ex);
            }
        }

        boolean authResult = false;

        if(password != null) {
            authResult = authHandler.authenticate(userName, password,
                                                  request, response, null);
        }

        if( !authResult) {
            if(isLogMessageEnabled()) {
                logMessage(
                    "LocalAuthTaskHandler: Local authentication failed, " 
                    + "invalidating session.");
            }

            session.invalidate();
        } else {
            if(isLogMessageEnabled()) {
                logMessage(
                    "LocalAuthTaskHandler: Local authenticatio successful for "
                    + userName + ", session is valid.");
            }
        }

        return authResult;
    }

    private boolean doLocalAuthWithoutSessionBinding(
            IJ2EEAuthenticationHandler authHandler,
            HttpServletRequest request, HttpServletResponse response,
            SSOValidationResult ssoValidationResult) {

        if(isLogMessageEnabled()) {
            logMessage(
                "LocalAuthTaskHandler: doing local authentication without " 
                + "session binding");
        }

        HttpSession session         = request.getSession(false);
        boolean     existingSession = (session != null);

        if( !existingSession) {
            if(isLogMessageEnabled()) {
                logMessage(
                    "LocalAuthTaskHandler: session does not exist, will be " 
                    + "created for authentication only");
            }

            session = request.getSession(true);
        }

        String userName = ssoValidationResult.getUserId();
        String password = null;

        try {
            password = ssoValidationResult.getTransportString();
        } catch(Exception ex) {
            if(isLogMessageEnabled()) {
                logMessage(
                    "LocalAuthTaskHandler: Exception caught while doing local " 
                    + "authentication",
                    ex);
            }
        }

        boolean authResult = false;

        if(password != null) {
            authResult = authHandler.authenticate(userName, password,
                                                  request, response, null);
        }

        if( !existingSession) {
            if(isLogMessageEnabled()) {
                logMessage(
                    "LocalAuthTaskHandler: invalidating session after " 
                    + "authentication");
            }

            session.invalidate();
        }

        if(isLogMessageEnabled()) {
            if(authResult) {
                logMessage(
                    "LocalAuthTaskHandler: Local authentication successful for "
                    + userName);
            } else {
                logMessage("LocalAuthTaskHandler: Local authentication failed");
            }
        }

        return authResult;
    }

    private IJ2EEAuthenticationHandler      _globalAuthenticationHandler;
    private Hashtable _authenticationHandlers = new Hashtable();

}
