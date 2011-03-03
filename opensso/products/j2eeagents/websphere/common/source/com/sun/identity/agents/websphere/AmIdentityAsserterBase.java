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
 * $Id: AmIdentityAsserterBase.java,v 1.4 2009/03/07 01:15:39 leiming Exp $
 *
 */

package com.sun.identity.agents.websphere;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WebTrustAssociationException;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.INotenforcedIPHelper;
import com.sun.identity.agents.common.INotenforcedURIHelper;
import com.sun.identity.agents.common.ISSOTokenValidator;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.filter.AmFilterMode;
import com.sun.identity.agents.filter.AmFilterRequestContext;
import com.sun.identity.agents.filter.AmFilterResult;
import com.sun.identity.agents.filter.AmFilterResultStatus;
import com.sun.identity.agents.filter.IAmFilter;
import com.sun.identity.agents.filter.IFilterConfigurationConstants;
import com.sun.identity.agents.realm.AmRealmManager;
import com.sun.identity.agents.realm.IAmRealm;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.agents.util.StringUtils;

/**
 * Abstact class for Websphere/portal asserter.
 */
public abstract class AmIdentityAsserterBase extends AgentBase
        implements IAmIdentityAsserter {
    
    public AmIdentityAsserterBase(Manager manager) {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        
        String strFilterMode = getConfigurationString(
                IFilterConfigurationConstants.CONFIG_FILTER_MODE,
                AmFilterMode.STR_MODE_ALL);
        
        AmFilterMode mode = AmFilterMode.get(strFilterMode);
        
        if (mode == null) {
            throw new AgentException("Unknown filter mode: " + strFilterMode);
        }
        
        if (mode.equals(AmFilterMode.MODE_ALL) ||
                mode.equals(AmFilterMode.MODE_J2EE_POLICY)) {
            setActiveFlag(true);
        } else {
            setActiveFlag(false);
        }
        
        
        // Regardless of which mode the runtime is configured for, the
        // TAI implementation only requries SSO functionality. The rest
        // is delegated to the regular filter.
        //
        // Note that this filter is not an independent filter, but is
        // created within the websphere module.
        setAmFilter(ServiceFactory.getAmFilter(getManager(),
                AmFilterMode.MODE_SSO_ONLY));
        
        // Realm is used for memberships
        setAmRealm(AmRealmManager.getAmRealmInstance());
        
        boolean notEnforcedURIListcacheEnabled = getConfigurationBoolean(
            IFilterConfigurationConstants.CONFIG_NOTENFORCED_LIST_CACHE_FLAG,
            IFilterConfigurationConstants.DEFAULT_NOTENFORCED_LIST_CACHE_FLAG);
        
        boolean isNotEnforcedURIListInverted = getConfigurationBoolean(
            IFilterConfigurationConstants.CONFIG_INVERT_NOTENFORCED_LIST_FLAG,
            IFilterConfigurationConstants.DEFAULT_INVERT_NOTENFORCED_LIST_FLAG);
        
        int notEnforcedURIListCacheSize = getConfigurationInt(
            IFilterConfigurationConstants.CONFIG_NOTENFORCED_LIST_CACHE_SIZE,
            IFilterConfigurationConstants.DEFAULT_NOTENFORCED_LIST_CACHE_SIZE)/2;
        
        String[] notEnforcedURIs = getConfigurationStrings(
                IFilterConfigurationConstants.CONFIG_NOTENFORCED_LIST);
        
        CommonFactory cf = new CommonFactory(getModule());
        setNotEnforcedListURIHelper(cf.newNotenforcedURIHelper(
                isNotEnforcedURIListInverted,
                notEnforcedURIListcacheEnabled,
                notEnforcedURIListCacheSize, notEnforcedURIs));
        
        
        boolean notEnforcedIPListCacheEnabled = getConfigurationBoolean(
            IFilterConfigurationConstants.CONFIG_NOTENFORCED_IP_CACHE_FLAG,
            IFilterConfigurationConstants.DEFAULT_NOTENFORCED_IP_CACHE_FLAG);
        
        int notEnforcedIPListCacheSize = getConfigurationInt(
            IFilterConfigurationConstants.CONFIG_NOTENFORCED_IP_CACHE_SIZE,
            IFilterConfigurationConstants.DEFAULT_NOTENFORCED_IP_CACHE_SIZE)/2;
        
        boolean isNotEnforcedIPListInverted = getConfigurationBoolean(
            IFilterConfigurationConstants.CONFIG_INVERT_NOTENFORCED_IP_FLAG,
            IFilterConfigurationConstants.DEFAULT_INVERT_NOTENFORCED_IP_FLAG);
        
        String[] notEnforcedIPs = getConfigurationStrings(
                IFilterConfigurationConstants.CONFIG_NOTENFORCED_IP_LIST);
        
        setNotEnforcedListIPHelper(cf.newNotenforcedIPHelper(
                notEnforcedIPListCacheEnabled, notEnforcedIPListCacheSize,
                isNotEnforcedIPListInverted, notEnforcedIPs));
        
        setSSOTokenValidator(cf.newSSOTokenValidator());
        
        
        if (isLogMessageEnabled()) {
            logMessage("AmIdentityAsserter: initilaized");
        }
    }
    
    public boolean needToProcessRequest(HttpServletRequest request)
    throws WebTrustAssociationException {
        boolean result = false;
        if (isActive()) {
            result = !isNotenforcedRequest(request);
        }
        
        if (isLogMessageEnabled()) {
            logMessage("AmIdentityAsserter: request uri: "
                    + request.getRequestURI() + ", is enforced: " + result);
        }
        
        return result;
    }

    public TAIResult processRequest(HttpServletRequest request,
            HttpServletResponse response)
            throws WebTrustAssociationFailedException {
        TAIResult result = null;
        try {
            AmFilterResult filterResult =
                    getAmFilter().isAccessAllowed(request, response);
            
            switch(filterResult.getStatus().getIntValue()) {
                case AmFilterResultStatus.INT_STATUS_CONTINUE:
                    SSOValidationResult ssoValidationResult =
                            filterResult.getSSOValidationResult();
                    if (ssoValidationResult != null
                            && ssoValidationResult.isValid()) {
                        result = getAuthenticatedResult(
                                request, response, ssoValidationResult);
                    } else {
                        result = getAnonymousResult(request, response);
                    }
                    break;
                case AmFilterResultStatus.INT_STATUS_FORBIDDEN:
                    result = getForbiddenResult(request, response);
                    break;
                case AmFilterResultStatus.INT_STATUS_REDIRECT:
                    result = getRedirectResult(request,
                            response, filterResult.getRedirectURL());
                    break;
                case AmFilterResultStatus.INT_STATUS_SERVE_DATA:
                    result = getServeDataResult(request,
                            response, filterResult.getDataToServe());
                    break;
                default:
                    throw new AgentException("Invalid filter result: "
                            + filterResult);
            }
        } catch (Exception ex) {
            logError("AmIdentityAsserter: Exception caught, denying access",
                    ex);
            result = getForbiddenResult(request, response);
        }
        
        if (isLogMessageEnabled()) {
            StringBuffer buff = new StringBuffer("TAIResult: status: ");
            buff.append(result.getStatus()).append(", principal: ");
            buff.append(result.getAuthenticatedPrincipal());
            buff.append(", subject: ").append(result.getSubject());
            
            logMessage("AmIdentityAsserter: result => " + buff.toString());
        }
        
        return result;
    }
    
    protected abstract TAIResult getAuthenticatedResult(
            HttpServletRequest request, HttpServletResponse response,
            SSOValidationResult ssoValidationResult)
            throws Exception;
    
    private TAIResult getAnonymousResult(HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {
        return TAIResult.create(HttpServletResponse.SC_OK,
                IUtilConstants.ANONYMOUS_USER_NAME);
    }
    
    private TAIResult getForbiddenResult(HttpServletRequest request,
            HttpServletResponse response)
            throws WebTrustAssociationFailedException {
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException ex) {
            logError("Unable to send 403 error code", ex);
            throw new WebTrustAssociationFailedException(
                    "Invalid response state");
        }
        return TAIResult.create(HttpServletResponse.SC_FORBIDDEN);
    }
    
    private TAIResult getRedirectResult(HttpServletRequest request,
            HttpServletResponse response, String redirectURL)
            throws Exception {
        response.sendRedirect(redirectURL);
        return TAIResult.create(HttpServletResponse.SC_TEMPORARY_REDIRECT);
    }
    
    private TAIResult getServeDataResult(HttpServletRequest request,
            HttpServletResponse response, String dataToServe)
            throws Exception {
        PrintWriter out = null;
        try {
            response.setContentType("text/html");
            out = response.getWriter();
            out.print(dataToServe);
            out.flush();
            out.close();
        } catch(IOException ex) {
            throw ex;
        } finally {
            if(out != null) {
                out.close();
            }
        }
        return TAIResult.create(HttpServletResponse.SC_ACCEPTED);
    }
    
    
    private boolean isNotenforcedRequest(HttpServletRequest request) {
    	String appName = getApplicationName(request);
    	String accessDeniedURI = getAccessDeniedURI(appName);
        
        boolean pathInfoIgnored = getConfigurationBoolean(
                IFilterConfigurationConstants.CONFIG_IGNORE_PATH_INFO,
                IFilterConfigurationConstants.DEFAULT_IGNORE_PATH_INFO);
        String requestURL = null;
        if (pathInfoIgnored) {
            requestURL = StringUtils.removePathInfo(request);
        } else {
            AmFilterRequestContext ctx = getRequestContext(request);
            requestURL = ctx.getPolicyDestinationURL();
        }
        if (isLogMessageEnabled()) {
            logMessage("AmIdentityAsserter.isNotenforcedRequest() - " +
                    "requested URL=> " + requestURL);
        }
        
        return (getNotEnforcedListURIHelper().isNotEnforced(
                requestURL, accessDeniedURI) ||
                getNotEnforcedListIPHelper().isNotenforced(
                getClientIPAddress(request)));
    }

    /*
     * create RequestContext just to get absolute requested URL.
     */
    private AmFilterRequestContext getRequestContext(
            HttpServletRequest request) {

        AmFilterRequestContext requestContext = new AmFilterRequestContext(
                request, null, null, null,
                null, false, null,
                null, null,
                getAgentHost(request), getAgentPort(request),
                getAgentProtocol(request));

        return requestContext;
    }

    private String getAgentHost(HttpServletRequest request) {
         String agentHost = getConfigurationString(
                IFilterConfigurationConstants.CONFIG_AGENT_HOST);
        if (agentHost == null) {
            agentHost = request.getServerName();
        }
         return agentHost;
    }
    
    private String getAgentProtocol(HttpServletRequest request) {
        String agentProtocol = getConfigurationString(
                IFilterConfigurationConstants.CONFIG_AGENT_PROTOCOL);
        if (agentProtocol == null) {
            agentProtocol = request.getScheme();
        }
        return agentProtocol;
    }

    private int getAgentPort(HttpServletRequest request) {
        int agentPort = getConfigurationInt(
                IFilterConfigurationConstants.CONFIG_AGENT_PORT);
        if (agentPort <= 0) {
            agentPort = request.getServerPort();
        }
        return agentPort;
    }
    
    private String getApplicationName(HttpServletRequest request) {
        String appName = null;
        String contextPath = request.getContextPath();

        if (contextPath.trim().length() == 0 ||
            contextPath.trim().equals("/")) {
            appName = AgentConfiguration.DEFAULT_WEB_APPLICATION_NAME;
        } else {
            appName = contextPath.substring(1);
        }
        
        return appName;
    }
    
    private String getAccessDeniedURI(String applicationName) {
        return getManager().getApplicationConfigurationString(
        		IFilterConfigurationConstants.CONFIG_ACCESS_DENIED_URI, 
        		applicationName); 
    }
    
    private String getClientIPAddress(HttpServletRequest request) {
        return getSSOTokenValidator().getClientIPAddress(request);
    }
    
    private void setAmFilter(IAmFilter amFilter) {
        _amFilter = amFilter;
    }
    
    private IAmFilter getAmFilter() {
        return _amFilter;
    }
    
    private void setAmRealm(IAmRealm amRealm) {
        _amRealm = amRealm;
    }
    
    private IAmRealm getAmRealm() {
        return _amRealm;
    }
    
    private void setSSOTokenValidator(ISSOTokenValidator validator) {
        _ssoTokenValidator = validator;
    }
    
    private ISSOTokenValidator getSSOTokenValidator() {
        return _ssoTokenValidator;
    }
    
    private void setNotEnforcedListIPHelper(INotenforcedIPHelper helper) {
        _notEnforcedIPHelper = helper;
    }
    
    private INotenforcedIPHelper getNotEnforcedListIPHelper() {
        return _notEnforcedIPHelper;
    }
    
    private void setNotEnforcedListURIHelper(INotenforcedURIHelper helper) {
        _notEnforcedListURIHelper = helper;
    }
    
    private INotenforcedURIHelper getNotEnforcedListURIHelper() {
        return _notEnforcedListURIHelper;
    }
    
    private void setActiveFlag(boolean flag) {
        _isActive = flag;
        if (isLogMessageEnabled()) {
            logMessage("AmIdentityAsserter: is active = " + _isActive);
        }
    }
    
    private boolean isActive() {
        return _isActive;
    }
    
    private INotenforcedURIHelper _notEnforcedListURIHelper;
    private INotenforcedIPHelper _notEnforcedIPHelper;
    private ISSOTokenValidator _ssoTokenValidator;
    private IAmFilter _amFilter;
    private IAmRealm _amRealm;
    private boolean _isActive;
}
