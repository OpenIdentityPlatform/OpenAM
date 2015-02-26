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
 * $Id: FQDNTaskHandler.java,v 1.2 2008/06/25 05:51:44 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IFQDNHelper;

/**
 * <p>
 * This task handler provides the necessary functionality to process incoming
 * requests for FQDN compliance.
 * </p>
 */
public class FQDNTaskHandler extends AmFilterTaskHandler 
implements IFQDNTaskHandler {

    /**
     * The constructor that takes a <code>Manager</code> intance in order
     * to gain access to the infrastructure services such as configuration
     * and log access.
     *
     * @param manager the <code>Manager</code> for the <code>filter</code>
     * subsystem.
     * @throws AgentException in case this task handler fails to initiaze
     */
    public FQDNTaskHandler(Manager manager) {
        super(manager);
    }
    
    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        initFQDNEnabledFlag();
        initFQDNHelper();
    }

    /**
     * Checks to see if the incoming request has an appropriate FQDN host name
     * and suggests any action needed to handle requests that do not comply.
     *
     * @param ctx the <code>AmFilterRequestContext</code> that carries information
     * about the incoming request and response objects.
     *
     * @return <code>null</code> if no action is necessary, or
     * <code>AmFilterResult</code> object indicating the necessary action in
     * order to handle notifications.
     * @throws AgentException in case this request processing results in an
     * unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx)
        throws AgentException
    {
        AmFilterResult result = null;
        HttpServletRequest request = ctx.getHttpServletRequest();

        String validServerName =
            getFQDNHelper().getValidFQDNResource(request.getServerName());

        if(validServerName != null) {
            StringBuffer buff        = new StringBuffer();
            String       requestURI  = request.getRequestURI();
            String       protocol    = request.getScheme();
            int          portNumber  = request.getServerPort();
            String       queryString = request.getQueryString();

            buff.append(protocol);
            buff.append("://");
            buff.append(validServerName);
            buff.append(":");
            buff.append(portNumber);
            buff.append(requestURI);

            if(queryString != null) {
                buff.append("?");
                buff.append(queryString);
            }

            if(isLogWarningEnabled()) {
                logWarning("FQDNTaskHandler: FQDN check resulted in redirect: "
                           + request.getServerName() + ", redirecting to => "
                           + buff.toString());
            }
            result = ctx.getCustomRedirectResult(buff.toString());
        }

        return result;
    }

    /**
     * Returns a boolean value indicating if this task handler is enabled or not.
     * @return true if this task handler is enabled, false otherwise
     */
    public boolean isActive() {
        return  isModeSSOOnlyActive() && isFQDNCheckEnabled();
    }

    /**
     * Returns a String that can be used to identify this task handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_FQDN_TASK_HANDLER_NAME;
    }

    private boolean isFQDNCheckEnabled() {
        return _isFQDNCheckEnabled;
    }

    private IFQDNHelper getFQDNHelper() {
        return _fqdnHelper;
    }
    private void setFQDNHelper(IFQDNHelper helper) {
        _fqdnHelper = helper;
    }
    
    private void initFQDNEnabledFlag() {
        _isFQDNCheckEnabled = getConfigurationBoolean(CONFIG_FQDN_ENABLE_FLAG, 
                DEFAULT_FQDN_ENABLE_FLAG);
        if (isLogMessageEnabled()) {
            logMessage("FQDNTaskHandler: enabled: " + _isFQDNCheckEnabled);
        }
    }

    private void initFQDNHelper() throws AgentException {
        CommonFactory cf = new CommonFactory(getModule());
        String defaultFQDN = getConfigurationString(CONFIG_DEFAULT_FQDN);
        if (defaultFQDN == null || defaultFQDN.trim().length() == 0) {
            throw new AgentException("Invalid default fqdn specified");
        }
        
        Map fqdnMap = getConfigurationMap(CONFIG_FQDN_MAP);
        setFQDNHelper(cf.newFQDNHelper(defaultFQDN, fqdnMap));
    }

    private IFQDNHelper _fqdnHelper;
    private boolean _isFQDNCheckEnabled;
}
