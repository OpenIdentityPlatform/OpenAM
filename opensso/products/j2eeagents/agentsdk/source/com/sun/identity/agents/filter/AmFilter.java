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
 * $Id: AmFilter.java,v 1.8 2009/05/26 22:47:58 leiming Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.AgentServerErrorException;
import com.sun.identity.agents.arch.AgentSSOException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IURLFailoverHelper;
import com.sun.identity.agents.common.IPatternMatcher;
import com.sun.identity.agents.util.IUtilConstants;
import com.sun.identity.agents.util.RequestDebugUtils;


/**
 * The <code>AmFilter</code> is the service class for the filter module.
 * This class provides the necessary functions needed to enforce J2EE as
 * well as URL policies for various resources in the protected application.
 */
public class AmFilter extends AgentBase 
    implements IAmFilter, IFilterConfigurationConstants 
{

    /**
     * Constructs an <code>AmFilter</code> instance with the filter mode set
     * to the specified filter mode.
     *
     * @param manager the <code>Manager</code> of the containing subsystem
     * @param filterMode the filter mode identifier in which this instance will
     * operate in
     * @throws AgentException if the instance fails to initialze
     */
    public AmFilter(Manager manager) {
        super(manager);
    }
    
    public void initialize(AmFilterMode filterMode) throws AgentException {
        initFilterMode(filterMode);
        initFormLoginList();
        initAgentServerDetails();
        setRedirectParameterName(getConfigurationString(
                CONFIG_REDIRECT_PARAM_NAME, DEFAULT_REDIRECT_PARAM_NAME));
        
        CommonFactory cf = new CommonFactory(getModule());
        setMatcher(cf.newURLPatternMatcher(getFormLoginList()));
        initLoginURLFailoverHelper(cf);
        initLogoutURLFailoverHelper(cf);
        initSSOContext(cf);

        // Initialize various handlers
        initInboundTaskHandlers();
        initSelfRedirectTaskHandlers();
        initResultHandlers();

        if (isLogMessageEnabled()) {
            logMessage("AmFilter: The following inbound task handlers are "
                     + "active" + IUtilConstants.NEW_LINE
                     + getHandlerDebugString(getInboundTaskHandlers()));
            logMessage("AmFilter: The following self-redriect task handlers are"
                     + "active" + IUtilConstants.NEW_LINE
                     + getHandlerDebugString(getSelfRedirectTaskHandlers()));
            logMessage("AmFilter: The following result handlers are active"
                     + IUtilConstants.NEW_LINE
                     + getHandlerDebugString(getResultHandlers()));

            logMessage("AmFilter initialized");
        }
    }

    /**
     * Determines if access to the requested resource should be allowed or any
     * other corrective action needs to be taken in case the request is a 
     * special request such as a notification, or lacks the necessary 
     * credentials. The return value of <code>AmFilterResult</code> carries 
     * the necessary information regarding what action must be taken for this 
     * request including any ncessary redirects or error codes that must be 
     * sent to the client from where this request originated.
     *
     * @param request the incoming <code>HttpServletRequest</code>
     * @param response the incoming <code>HttpServletResponse</code>
     *
     * @return an <code>AmFilterResult</code> instance which indicates what
     * specific action must be taken in order to fulfill this request.
     */
    public AmFilterResult isAccessAllowed(HttpServletRequest request,
            HttpServletResponse response)
    {
        if(isLogMessageEnabled()) {
            logMessage("AmFilter: incoming request => "
                       + RequestDebugUtils.getDebugString(request));
        }

        String appName = null;
        String contextPath = request.getContextPath();

        if (contextPath.trim().length() == 0 ||
            contextPath.trim().equals("/")) {
            appName = AgentConfiguration.DEFAULT_WEB_APPLICATION_NAME;
        } else {
            appName = contextPath.substring(1);
        }

        AmFilterRequestContext ctx = new AmFilterRequestContext(request,
            response, getRedirectParameterName(), getLoginURLFailoverHelper(),
            getLogoutURLFailoverHelper(),
            isFormLoginRequest(request), getAccessDeniedURI(appName), this,
            getFilterMode(), getAgentHost(request), getAgentPort(request), 
            getAgentProtocol(request));

        // First: Process the task handlers
        AmFilterResult result = null;
        try {
            if (getFilterMode().equals(AmFilterMode.MODE_NONE)) {
                result = new AmFilterResult(
                    AmFilterResultStatus.STATUS_CONTINUE);
            } else {
                result = processTaskHandlers(ctx);
            }
        } catch(Throwable th) {
            logError("AmFilter: An error occurred while processing request. "
                     + "Access will be denied.", th);
            result = ctx.getBlockAccessResult();
        }

        // Now: Process the result task handlers
        result = processResultHandlers(ctx, result);

        if (isLogMessageEnabled()) {
            logMessage("AmFilter: result => " 
                    + IUtilConstants.NEW_LINE + result);
        }
        
        // If verified session associate with request, propagate it down
        if (ctx.isAuthenticated()) {
            result.setSSOValidationResult(ctx.getSSOValidationResult());
        }
        return result;
    }

    private AmFilterResult processTaskHandlers(AmFilterRequestContext ctx)
        throws AgentException
    {
        IAmFilterTaskHandler[] handler = getInboundTaskHandlers();

        int index = 0;
        int count = handler.length;
        AmFilterResult result = null;
        while (index < count && result == null) {
            try {
                if (isLogMessageEnabled()) {
                    logMessage("AmFilter: now processing: "
                              + handler[index].getHandlerName());
                }
                result = handler[index].process(ctx);
                index++;
            } catch (AgentServerErrorException ase) {
                logError("AmFilter: a server error occurred.", ase);
                result = ctx.getServerErrorResult();
            } catch (AgentSSOException assoe) {
                if (isLogMessageEnabled()) {
                    logMessage("AmFilter: user SSO Token is invalid. "
                            + assoe.getMessage()
                            + ". Redirect to authentication page.");
                }
                result = ctx.getAuthRedirectResult();
            } catch (Exception ex) {
                logError("AmFilter: Error while delegating to inbound"
                       + " handler: " + handler[index].getHandlerName()
                       + ", access will be denied", ex);
                result = ctx.getBlockAccessResult();
            }
        }

        if (result == null) {
            result = ctx.getContinueResult();
        }

        return result;
    }

    private AmFilterResult processResultHandlers(AmFilterRequestContext ctx,
        AmFilterResult result)
    {
        IAmFilterResultHandler[] resultHandler = getResultHandlers();
        int index = 0;
        int count = resultHandler.length;
        while (index < count) {
            try {
                if (isLogMessageEnabled()) {
                    logMessage("AmFilter: now processing: "
                               + resultHandler[index].getHandlerName());
                }
                result = resultHandler[index].process(ctx, result);
                if (result == null)  {
                    throw new AgentException("Result handler invocation " +
                        "failed." );
                }
                index++;
            } catch (Exception ex) {
                logError("AmFilter: Error while delegating to result handler: "
                         + resultHandler[index].getHandlerName()
                         + ", access will be denied", ex);
                result = ctx.getBlockAccessResult(true);
            }
        }

        // Final sanity check
        if (result == null) {
            logError("AmFilter: Failed to process request: no result available "
                   + "access will be denied.");
            result = ctx.getBlockAccessResult(true);
        }

        return result;
    }

    /**
     * This method constructs and returns an instance of
     * <code>AmFilterResult</code> which can be used to redirect the request to
     * its original destination.
     *
     * @param ctx the <code>AmFilterRequestContext</code> which carries the
     * information pertaining to the request that is currently being processed.
     *
     * @return an <code>AmFilterResult</code> that can be used to redirect the
     * request back to its destination thereby making a roundtrip before passing
     * the request to the downstream application.
     */
    public AmFilterResult redirectToSelf(AmFilterRequestContext ctx)
    {
        AmFilterResult result = null;

        IAmFilterTaskHandler[] handler = getSelfRedirectTaskHandlers();
        int index = 0;
        while (index < handler.length && result == null) {
            try {
                result = handler[index].process(ctx);
                index++;
            } catch (Exception ex) {
                logError("Error while delegating to self-redirect handler: "
                         + handler[index].getHandlerName()
                         + ", access will be denied", ex);
                result = ctx.getBlockAccessResult();
            }
        }

        if (result == null) {
            result = new AmFilterResult(AmFilterResultStatus.STATUS_REDIRECT,
                                    ctx.getDestinationURL());
        }

        return result;
    }

    private boolean isFormLoginRequest(HttpServletRequest request) {
       return getMatcher().match(request.getRequestURL().toString());
    }

    private void initResultHandlers() throws AgentException {
        ArrayList handlers = ServiceFactory.getFilterResultHandlers(
              getManager(), getSSOContext(), getFilterMode(), isCDSSOEnabled());
        
        if (isLogMessageEnabled())  {
            logMessage("AmFilter: Applicable result handlers are: "
                    + handlers);
        }
        Iterator it = handlers.iterator();
        while(it.hasNext()) {
            IAmFilterResultHandler handler = (IAmFilterResultHandler) it.next();
            if (isLogMessageEnabled()) {
                logMessage("AmFilter: " + handler.getHandlerName() + ": "
                           + (handler.isActive()?"enabled":"disabled"));
            }
            if (!handler.isActive()) {
                it.remove();
            }
        }
        IAmFilterResultHandler[] resultHandlers = 
            new IAmFilterResultHandler[handlers.size()];
        for (int i=0; i<handlers.size();i++) {
            resultHandlers[i] = (IAmFilterResultHandler) handlers.get(i);
        }
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: For the current configuration, there are "
                       + resultHandlers.length
                       + " active self-redirect task handlers");
        }
        setResultHandlers(resultHandlers);
    }

    private void initSelfRedirectTaskHandlers() throws AgentException {
        ArrayList handlers = ServiceFactory.getFilterSelfRedirectTaskHandlers(
              getManager(), getSSOContext(), getFilterMode(), isCDSSOEnabled());
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: Applicable self-redirect handlers: "
                    + handlers);
        }

        Iterator it = handlers.iterator();
        while (it.hasNext()) {
            IAmFilterTaskHandler handler = (IAmFilterTaskHandler) it.next();
            if (isLogMessageEnabled()) {
                logMessage("AmFilter: " + handler.getHandlerName() + ": "
                           + (handler.isActive()?"enabled":"disabled"));
            }
            if (!handler.isActive()) {
                it.remove();
            }
        }

        IAmFilterTaskHandler[] selfRedirectHandlers = 
            new IAmFilterTaskHandler[handlers.size()];
        for (int i=0; i<handlers.size();i++) {
            selfRedirectHandlers[i] = (IAmFilterTaskHandler) handlers.get(i);
        }

        if (isLogMessageEnabled()) {
            logMessage("AmFilter: For the current configuration, there are "
                       + selfRedirectHandlers.length
                       + " active self-redirect task handlers");
        }

        setSelfRedirectTaskHandlers(selfRedirectHandlers);
    }

    private void initInboundTaskHandlers() throws AgentException {
        ArrayList handlers = ServiceFactory.getFilterInboundTaskHandlers(
             getManager(), getSSOContext(), getFilterMode(), isCDSSOEnabled());
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: Applicable handlers are: " + handlers);
        }
        Iterator it = handlers.iterator();
        while (it.hasNext()) {
            IAmFilterTaskHandler handler = (IAmFilterTaskHandler) it.next();
            if (isLogMessageEnabled()) {
                logMessage("AmFilter: " + handler.getHandlerName() + ": "
                           + (handler.isActive()?"enabled":"disabled"));
            }
            if (!handler.isActive()) {
                it.remove();
            }
        }

        IAmFilterTaskHandler[] inboundTaskHandler = 
            new IAmFilterTaskHandler[handlers.size()];
        for (int i=0; i<handlers.size();i++) {
            inboundTaskHandler[i] = (IAmFilterTaskHandler) handlers.get(i);
        }

        if (isLogMessageEnabled()) {
            logMessage("AmFilter: For the current configuration, there are "
                 + inboundTaskHandler.length + " active inbound task handlers");
        }

        setInboundTaskHandlers(inboundTaskHandler);
    }

    private IAmFilterTaskHandler[] getSelfRedirectTaskHandlers() {
        return _selfRedirectTaskHandler;
    }

    private void setSelfRedirectTaskHandlers(IAmFilterTaskHandler[] handlers) {
        _selfRedirectTaskHandler = handlers;
    }

    private IAmFilterTaskHandler[] getInboundTaskHandlers() {
        return _inboundTaskHandler;
    }

    private void setInboundTaskHandlers(IAmFilterTaskHandler[] handlers) {
        _inboundTaskHandler = handlers;
    }    

    private String getHandlerDebugString(IAmFilterHandler[] handler) {
        StringBuffer buff = new StringBuffer(IUtilConstants.NEW_LINE);
        if (handler.length > 0) {
            for(int i=0; i<handler.length; i++) {
                buff.append("\t[").append(i).append("]: ");
                buff.append(handler[i].getHandlerName()).append(
                        IUtilConstants.NEW_LINE);
            }
        } else {
            buff.append("\t*** No Handlers Available ***").append(
                    IUtilConstants.NEW_LINE);
        }

        return buff.toString();
    }

    private void setResultHandlers(IAmFilterResultHandler[] resultHandler) {
        _resultHandler = resultHandler;
    }

    private IAmFilterResultHandler[] getResultHandlers() {
        return _resultHandler;
    }

    private void initSSOContext(CommonFactory cf) throws AgentException {
        setCDSSOEnabledFlag(getConfigurationBoolean(CONFIG_CDSSO_ENABLED));
        ISSOContext ssoContext = null;
        if (isCDSSOEnabled()) {
            ssoContext =
                ServiceFactory.getCDSSOContext(getManager(), getFilterMode());
        } else {
            ssoContext = 
                ServiceFactory.getSSOContext(getManager(), getFilterMode());
        }
        
        setSSOContext(ssoContext);
    }
    
    protected boolean isCDSSOEnabled() {
        return _cdssoEnabledFlag;
    }

    private void setCDSSOEnabledFlag(boolean flag) {
        _cdssoEnabledFlag = flag;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: CDSSO enabled: " + _cdssoEnabledFlag);
        }
    }    
    
    private ISSOContext getSSOContext() {
        return _ssoContext;
    }
    
    private void setSSOContext(ISSOContext ssoContext) {
        _ssoContext = ssoContext;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: sso context is: " + _ssoContext);
        }
    }
    
    private void initLoginURLFailoverHelper(CommonFactory cf) 
    throws AgentException 
    {
        String[] loginURLs = getConfigurationStrings(CONFIG_LOGIN_URL);
        boolean isPrioritized = getConfigurationBoolean(
                CONFIG_LOGIN_URL_PRIORITIZED);
        boolean probeEnabled = getConfigurationBoolean(
                CONFIG_LOGIN_URL_PROBE_ENABLED, true);
        long    timeout = getConfigurationLong(
                CONFIG_LOGIN_URL_PROBE_TIMEOUT, 2000);
        setLoginURLFailoverHelper(cf.newURLFailoverHelper(
                probeEnabled,
                isPrioritized, 
                timeout,
                loginURLs,
                getParsedConditionalUrls(CONFIG_CONDITIONAL_LOGIN_URL)));
    }
    
    private void initLogoutURLFailoverHelper(CommonFactory cf) 
    throws AgentException 
    {
        String[] logoutURLs = getConfigurationStrings(CONFIG_LOGOUT_URL);
        boolean isPrioritized = getConfigurationBoolean(
                CONFIG_LOGOUT_URL_PRIORITIZED);
        boolean probeEnabled = getConfigurationBoolean(
                CONFIG_LOGOUT_URL_PROBE_ENABLED, true);
        long    timeout = getConfigurationLong(
                CONFIG_LOGOUT_URL_PROBE_TIMEOUT, 2000);
        setLogoutURLFailoverHelper(cf.newURLFailoverHelper(
                probeEnabled,
                isPrioritized, 
                timeout,
                logoutURLs,
                getParsedConditionalUrls(CONFIG_CONDITIONAL_LOGOUT_URL)));
    }
    
    private void setLoginURLFailoverHelper(IURLFailoverHelper helper) {
        _loginURLFailoverHelper = helper;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: login url failover helper: " 
                    + _loginURLFailoverHelper);
        }
    }
    
    private void setLogoutURLFailoverHelper(IURLFailoverHelper helper) {
        _logoutURLFailoverHelper = helper;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: logout url failover helper: " 
                    + _logoutURLFailoverHelper);
        }
    }
    
    private IURLFailoverHelper getLoginURLFailoverHelper() {
        return _loginURLFailoverHelper;
    }
    
    private IURLFailoverHelper getLogoutURLFailoverHelper() {
        return _logoutURLFailoverHelper;
    }
    
    private String getRedirectParameterName() {
        return _redirectParameterName;
    }

    private void setRedirectParameterName(String redirectParameterName) {
        _redirectParameterName = redirectParameterName;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: redirect parameter name is set to: " 
                    + redirectParameterName);
        }
    }    
    
    private void initFormLoginList() {
        String[] formList = getConfigurationStrings(CONFIG_FORM_LOGIN_LIST);
        setFormLoginList(formList);
    }
    
    private String[] getFormLoginList() {
        return _formLoginList;
    }
    
    private void setFormLoginList(String[] list) {
        _formLoginList = list;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: form list is set to: " + _formLoginList);
        }
    }
    
    private String getAccessDeniedURI(String applicationName) {
        return getManager().getApplicationConfigurationString(
                CONFIG_ACCESS_DENIED_URI, applicationName); 
    }

    private void initFilterMode(AmFilterMode mode) throws AgentException {
        if (mode == null) {
            String strMode = getConfigurationString(CONFIG_FILTER_MODE, 
                    AmFilterMode.STR_MODE_ALL);
            
            mode = AmFilterMode.get(strMode);
            if (mode == null) {
                throw new AgentException("Unknown filter mode: " + strMode);
            }
        }
        
        setFilterMode(mode);
    }
    
    private AmFilterMode getFilterMode() {
        return _filterMode;
    }
    
    private void setFilterMode(AmFilterMode mode) {
        _filterMode = mode;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: filter mode set to: " + _filterMode);
        }
    }
    
    private String getAgentHost(HttpServletRequest request) {
        String result = _agentHost;
        if (result == null) {
            result = request.getServerName();
        }
        return result;
    }
    
    private String getAgentProtocol(HttpServletRequest request) {
        String result = _agentProtocol;
        if (result == null) {
            result = request.getScheme();
        }
        return result;
    }
    
    private int getAgentPort(HttpServletRequest request) {
        int result = _agentPort;
        if (result == 0) {
            result = request.getServerPort();
        }
        return result;
    }
    
    private void initAgentServerDetails() throws AgentException {
        String agentHost = getConfigurationString(CONFIG_AGENT_HOST);
        if (agentHost != null && agentHost.trim().length() >0) {
            setAgentHost(agentHost);
        }
        
        String agentProtocol = getConfigurationString(CONFIG_AGENT_PROTOCOL);
        if (agentProtocol != null && agentProtocol.trim().length() > 0) {
            
            if (agentProtocol.equalsIgnoreCase(STR_HTTP)) {
                setAgentProtocol(STR_HTTP);
            } else if (agentProtocol.equalsIgnoreCase(STR_HTTPS)) {
                setAgentProtocol(STR_HTTPS);
            } else {
                if (isLogWarningEnabled()) {
                    logWarning("AmFilter: invalid agent protocol: " 
                            + agentProtocol);
                }
            }
        }
        
        int agentPort = getConfigurationInt(CONFIG_AGENT_PORT);
        if (agentPort > 0) {
            if (agentPort < 65535) {
                setAgentPort(agentPort);
            } else {
                if (isLogWarningEnabled()) {
                    logWarning("AmFilter: invalid agent port: " + agentPort);
                }
            }
        }
    }
    
    private void setAgentHost(String host) {
        _agentHost = host;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: agent host set to: " + host);
        }
    }
    
    private void setAgentPort(int port) {
        _agentPort = port;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: agent port set to: " + port);
        }
    }
    
    private void setAgentProtocol(String protocol) {
        _agentProtocol = protocol;
        if (isLogMessageEnabled()) {
            logMessage("AmFilter: agent protocol set to: " + protocol);
        }
    }
    
    private IPatternMatcher getMatcher() {
            return _matcher;
    }

    private void setMatcher(IPatternMatcher matcher) {
        _matcher = matcher;
    }
    
    private IAmFilterTaskHandler[] _inboundTaskHandler;
    private IAmFilterTaskHandler[] _selfRedirectTaskHandler;
    private IAmFilterResultHandler[] _resultHandler;
    private boolean _defaultRefererInitialized;
    private boolean _cdssoEnabledFlag;
    private AmFilterMode _filterMode = AmFilterMode.MODE_ALL;
    private String[] _formLoginList;
    private String _redirectParameterName;
    private IURLFailoverHelper _loginURLFailoverHelper;
    private IURLFailoverHelper _logoutURLFailoverHelper;
    private ISSOContext _ssoContext;
    private String _agentHost;
    private int _agentPort;
    private String _agentProtocol;
    private IPatternMatcher _matcher;
}
