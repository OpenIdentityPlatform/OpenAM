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
 * $Id: NotenforcedListTaskHandler.java,v 1.6 2009/10/15 23:22:29 leiming Exp $
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
import com.sun.identity.agents.common.INotenforcedURIHelper;
import com.sun.identity.agents.common.ISSOTokenValidator;
import com.sun.identity.agents.util.StringUtils;

/**
 *  FIXME: Rename this class to remove the word LIST from all places.
 * <p>
 * This task handler provides the necessary functionality to evaluate incoming
 * requests against the Notenforced List as set in the Agent Configuration.
 * </p>
 */
public class NotenforcedListTaskHandler extends AmFilterTaskHandler
implements INotenforcedListTaskHandler {

    private LogoutHelper helper;

    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @param accessDeniedURI the access denied URI that is being used in the
     * system. This entry is always not-enforced by the Agent.
     * @throws AgentException if this task handler could not be initialized.
     */
    public NotenforcedListTaskHandler(Manager manager)
    {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        boolean cacheEnabled = getConfigurationBoolean(
                CONFIG_NOTENFORCED_LIST_CACHE_FLAG, 
                DEFAULT_NOTENFORCED_LIST_CACHE_FLAG);
        
        boolean isInverted = getConfigurationBoolean(
                CONFIG_INVERT_NOTENFORCED_LIST_FLAG,
                DEFAULT_INVERT_NOTENFORCED_LIST_FLAG);
        
        int cacheSize = getConfigurationInt(
                CONFIG_NOTENFORCED_LIST_CACHE_SIZE,
                DEFAULT_NOTENFORCED_LIST_CACHE_SIZE)/2;
        
        String[] notenforcedURIs = getConfigurationStrings(
                CONFIG_NOTENFORCED_LIST);
        
        CommonFactory cf = new CommonFactory(getModule());
        setNotEnforcedListURIHelper(cf.newNotenforcedURIHelper(
                isInverted, cacheEnabled, cacheSize, notenforcedURIs));
        
        pathInfoIgnored = getConfigurationBoolean(
                CONFIG_IGNORE_PATH_INFO, DEFAULT_IGNORE_PATH_INFO);
        helper = new LogoutHelper(this);
    }

    /**
     * Checks to see if the incoming request is to be notenforced and
     * suggests any action needed to handle such requests appropriately.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries 
     * information about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     * @throws AgentException if the processing of this request results in an
     * unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        HttpServletRequest request = ctx.getHttpServletRequest();
        String requestURL;
        if (getPathInfoIgnored()) {
            requestURL = StringUtils.removePathInfo(request);
        } else {
            requestURL = ctx.getPolicyDestinationURL();
        }
        if (isLogMessageEnabled()) {
            logMessage(
                "NotenforcedListTaskHandler: pathInfoIgnored=" + pathInfoIgnored
                + "; requestURL=" + requestURL
                + "; pathinfo=" + request.getPathInfo());
        }       
        String accessDeniedURI = ctx.getAccessDeniedURI();
        if(isNotEnforcedURI(requestURL, accessDeniedURI)) {
            if(isLogMessageEnabled()) {
                logMessage("NotenforcedListTaskHandler: The request URI "
                           + requestURL + " was found in Not Enforced List");
            }

            refreshSessionIdletime(ctx);

            result = ctx.getContinueResult();
            result.markAsNotEnforced();

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
     * or not.
     * @return true if the task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return isModeSSOOnlyActive() 
                                && getNotEnforcedListURIHelper().isActive();
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_NOT_ENFORCED_LIST_TASK_HANDLER_NAME;
    }

    /**
     * For not-enforced URLs, session validation is called to
     * reset session idletime only if the property is set to true.
     */
    private void refreshSessionIdletime(AmFilterRequestContext ctx) {

        if (!getConfigurationBoolean(
                CONFIG_NOTENFORCED_REFRESH_SESSION_IDLETIME,
                DEFAULT_CONFIG_NOTENFORCED_REFRESH_SESSION_IDLETIME)) {
            return;
        }

        // call session validation to reset session idle time.
        ISSOContext ssoContext = getSSOContext();
        ISSOTokenValidator tokenValidator = ssoContext.getSSOTokenValidator();
        tokenValidator.validate(ctx.getHttpServletRequest());

    }

    public boolean getPathInfoIgnored() {
        return pathInfoIgnored;
    }
    
    private boolean isNotEnforcedURI(String uri, String accessDeniedURI) {
        return getNotEnforcedListURIHelper().isNotEnforced(uri, accessDeniedURI);
    }

    private void setNotEnforcedListURIHelper(INotenforcedURIHelper helper) {
        _notEnforcedListURIHelper = helper;
    }

    private INotenforcedURIHelper getNotEnforcedListURIHelper() {
        return _notEnforcedListURIHelper;
    }

    private INotenforcedURIHelper _notEnforcedListURIHelper;
    private boolean pathInfoIgnored = false;
}
