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
 * $Id: LocalLogoutTaskHandler.java,v 1.3 2008/06/25 05:51:47 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.security.Principal;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.ICookieResetHelper;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for local logout in J2EE Policy mode.
 * </p>
 */
public class LocalLogoutTaskHandler extends AmFilterTaskHandler 
implements ILocalLogoutTaskHandler {

    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException if this task handler could not be initialized
     */
    public LocalLogoutTaskHandler(Manager manager) throws AgentException {
        super(manager);
        
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        setGlobalLogoutHandler();
        initCookieResetHelper();

    }
    
    public boolean isActive() {
        return isModeJ2EEPolicyActive();
    }
    
    private void initCookieResetHelper() throws AgentException {
        CookieResetInitializer cookieResetInitializer = 
                        new CookieResetInitializer(getManager());
        CommonFactory cf = new CommonFactory(getModule());
        setCookieResetHelper(cf.newCookieResetHelper(cookieResetInitializer));
    }
    
    /**
     * Processes the incoming requests for local logout and suggests
     * any necessary action needed in order to achieve this functionality.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries 
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle logout.
     * @throws AgentException if this request cannot be handled by the task
     * handler successfully.
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;        
        //For new feature to allow session data to not be destroyed when a user 
        //authenticates to AM server and new session is created. RFE issue #763         
        if (!isSessionBindingEnabled()) {
            return result;
        }
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        IJ2EELogoutHandler logoutHandler = getLogoutHandler(
                getApplicationName(ctx.getHttpServletRequest()));
        String userName = ctx.getUserId();
        String userDN = ctx.getSSOValidationResult().getUserPrincipal();
        String authUser = request.getRemoteUser();
        AmFilterMode filterMode = ctx.getFilterMode();

         if ( ctx.getSSOValidationResult() != null && logoutHandler != null) {
            if (logoutHandler.needToLogoutUser(request, response, userName,
                                               userDN, filterMode, null)){
               boolean logoutResult = false;
                try {
                   logoutResult = logoutHandler.logout(request, response, null);
                } catch(Exception ex) {
                   logError(
                       "AmFilter: Exception caught while doing local logout");
                   throw new AgentException("Error in local logout", ex);
                }

               if(logoutResult) {
                  if(isLogMessageEnabled()) {
                       logMessage(
                          "AmFilter: logout successful for user  "
                          + authUser );
                  }

                  doCookiesReset(ctx);
                  result = ctx.getRedirectToSelfResult();
               } else {
                  if(isLogWarningEnabled()) {
                       logWarning(
                           "amFilter: logout failed for user  "
                           + authUser + ", Access will be denied" );
                  }
                  result = ctx.getBlockAccessResult();
               }
           } else {
               if(isLogMessageEnabled()) {
                       logMessage("AmFilter: local logout skipped"
                           + " SSO User => " + userName
                           + ", principal  =>" + authUser);
               }
            }
         }
         return result;

    }

    private void doCookiesReset(AmFilterRequestContext ctx)
    {
        HttpServletRequest request = ctx.getHttpServletRequest();
        HttpServletResponse response = ctx.getHttpServletResponse();
        ICookieResetHelper cookieResetHelper = getCookieResetHelper();
        if (cookieResetHelper != null && cookieResetHelper.isActive()) {
            cookieResetHelper.doCookiesReset(request, response);
        }
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_LOCAL_LOGOUT_TASK_HANDLER_NAME;
    }


    private boolean needToLogoutLocally(
            HttpServletRequest request,
            String ssoUser) {

        boolean needToLogout = false;
        if (ssoUser != null) {
            Principal user = request.getUserPrincipal();
            String userName = null;
            if (user != null) {
                userName = user.getName();
            }
            if ( userName != null &&
                 !userName.equals(ssoUser)){
                needToLogout = true;
            }
            if (!needToLogout) {
                String remoteUser = request.getRemoteUser();
                if (remoteUser != null &&
                    !remoteUser.equals(ssoUser)) {
                    needToLogout = true;
                }
            }
        }
        return needToLogout;
    }

    private boolean doLocalLogout(
            IJ2EELogoutHandler logoutHandler,
            HttpServletRequest request,
            HttpServletResponse response)
            throws AgentException {

        if(isLogMessageEnabled()) {
            logMessage(
                "LocalLogoutTaskHandler: doing local logout");
        }

        boolean logoutResult = false;

        try {
           logoutResult = logoutHandler.logout(request, response, null);
        } catch(Exception ex) {
            if(isLogMessageEnabled()) {
                logMessage(
                    "LocalLogoutTaskHandler: Exception caught while doing " 
                        + "local logout");
            }
            throw new AgentException("Error in local logout", ex);
        }
        return logoutResult;
    }

    private void setGlobalLogoutHandler() throws AgentException {

        String logoutHandlerClassName = 
            getResolver().getGlobalJ2EELogoutHandlerImpl();

        try {
                _globalLogoutHandler = (IJ2EELogoutHandler) Class.forName(
                                logoutHandlerClassName).newInstance();
        } catch(Exception ex) {
            throw new AgentException(
               "Unable to load Global Logout handler:"
                    + logoutHandlerClassName, ex);
        }
    }

    private IJ2EELogoutHandler getGlobalLogoutHandler() {
        return _globalLogoutHandler;
    }

    private void setCookieResetHelper(ICookieResetHelper cookieHelper){
        _cookieResetHelper = cookieHelper;
    }

    protected ICookieResetHelper getCookieResetHelper(){
        return _cookieResetHelper;
    }



    /**
     * Method getJ2EEAuthHandler
     *
     *
     * @return
     *
     */
    private IJ2EELogoutHandler getLogoutHandler(String applicationName)
           throws AgentException {

        IJ2EELogoutHandler result =
            (IJ2EELogoutHandler) getLogoutHandlers().get(applicationName);
        if (result == null)  {
            synchronized(this) {
                result = (IJ2EELogoutHandler) getLogoutHandlers().get(
                                                     applicationName);
                if (result == null) {
                    Map handlerMap = getManager().getConfigurationMap(
                                                CONFIG_LOGOUT_HANDLER_MAP);
                    String className = null;
                    if (handlerMap != null) {
                        className = (String) handlerMap.get(applicationName);
                    }
                    if(className != null) {
                        try {
                            result = (IJ2EELogoutHandler)
                                  Class.forName(className).newInstance();
                        } catch (Exception ex) {
                            throw new AgentException(
                                "Unable to initialize logout handler: "
                                + className + " for application: " 
                                + applicationName, ex);
                        }
                    }

                    if (result == null) {
                        if (isLogMessageEnabled()) {
                            logMessage(
                               "AmFilter: using global logout handler for app: "
                               + applicationName);
                        }
                        result = getGlobalLogoutHandler();
                    }
                    getLogoutHandlers().put(applicationName, result);
                }
            }
        }
        return result;

    }

    private Hashtable getLogoutHandlers() {
            return _logoutHandlers;
    }

    private IJ2EELogoutHandler  _globalLogoutHandler;
    private Hashtable _logoutHandlers = new Hashtable();
    private ICookieResetHelper _cookieResetHelper;
}
