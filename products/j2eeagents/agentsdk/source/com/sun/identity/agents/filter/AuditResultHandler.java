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
 * $Id: AuditResultHandler.java,v 1.3 2008/06/25 05:51:44 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.AuditLogMode;
import com.sun.identity.agents.arch.LocalizedMessage;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.log.AmAgentLogManager;
import com.sun.identity.agents.log.IAmAgentLog;

/**
 * <p>
 * This result handler provides the necessary functionality to log the result
 * of filter processing on incoming requests.
 * </p>
 */
public class AuditResultHandler extends AmFilterResultHandler 
implements IAuditResultHandler {
    
    public AuditResultHandler(Manager manager) {
        super(manager);
    }

    public void initialize(ISSOContext context, AmFilterMode mode) 
    throws AgentException {
        super.initialize(context, mode);
        initAmAgentLog();
        setAuditLogMode(AgentConfiguration.getAuditLogMode());       
    }

    /**
     * Logs the given result based on its type.
     *
     * @param ctx the filter request context which provides access to the
     * underlying <code>HttpServletRequest</code>,
     * <code>HttpServletResponse</code> and other data that
     * may be needed by this handler for facilitating its processing.
     *
     * @param result the <code>AmFilterResult</code> obtained by the
     * <code>AmFilter</code> by processing the incoming request.
     *
     * @return <code>AmFilterResult</code> if the processing resulted in a
     * particular action to be taken for the incoming request. <b>If no processing
     * is applicable to the given result instance, the same instance is returned
     * by this method.</b>
     *
     * @throws AgentException if the processing resulted in an unrecoverable
     * error condition
     * an unexpected error condition
     */
    public AmFilterResult process(AmFilterRequestContext ctx,
                                  AmFilterResult result)
        throws AgentException
    {
        String requestURL = ctx.getDestinationURL();
        if (isActive()) {
            if (result.isBlocked() && logDenyEnabled()) 
            {
                logDeny(requestURL, ctx);
            } else if (result.isAllowed() && !result.isNotEnforced() 
                                    && logAllowEnabled()) 
            {
                logAllow(requestURL, ctx);
            }
        }
        return result;
    }
    
    private boolean logAllowEnabled() {
        boolean result = false;
        switch(getAuditLogMode().getIntValue()) {
                    case AuditLogMode.INT_MODE_ALLOW:
                    case AuditLogMode.INT_MODE_BOTH:
                        result = true;
                            break;
        }
        return result;
    }
    
    private boolean logDenyEnabled() {
        boolean result = false;
        switch(getAuditLogMode().getIntValue()) {
                case AuditLogMode.INT_MODE_DENY:
                case AuditLogMode.INT_MODE_BOTH:
                    result = true;
                        break;
        }
        return result;
    }

    /**
     * Returns a boolean value indicating if this result handler is enabled or not.
     * @return true if the result handler is enabled, false otherwise
     */
    public boolean isActive() {
        return !getAuditLogMode().equals(AuditLogMode.MODE_NONE);
    }

    /**
     * Returns a String that can be used to identify this result handler
     * @return the name of this task handler
     */
    public String getHandlerName() {
        return AM_FILTER_AUDIT_RESULT_HANDLER_NAME;
    }
    
    protected LocalizedMessage getAllowMessage(
                    String userName, String requestURL) {
            return getModule().makeLocalizableString(
                IAmFilterModuleConstants.MSG_AM_FILTER_ACCESS_ALLOWED,
                new Object[] { requestURL, userName });
    }
    
    protected LocalizedMessage getDenyMessage(
                    String userName, String requestURL) 
    {
            return getModule().makeLocalizableString(
                IAmFilterModuleConstants.MSG_AM_FILTER_ACCESS_DENIED,
                new Object[] { requestURL, userName });
    }

    private void logAllow(String requestURL, AmFilterRequestContext ctx)  
        throws AgentException 
    {
        SSOValidationResult result = ctx.getSSOValidationResult();
        String userName = null;
        SSOToken ssoToken = null;
        if (result != null) {
            ssoToken = result.getSSOToken();
            userName = result.getUserPrincipal();
        }
        LocalizedMessage message = getAllowMessage(userName, requestURL);
        getAmAgentLog().log(ssoToken, message);
    }

    private void logDeny(String requestURL, AmFilterRequestContext ctx)
        throws AgentException 
    {
        SSOValidationResult result = ctx.getSSOValidationResult();
        String userName = null;
        SSOToken ssoToken = null;
        if (result != null) {
            ssoToken = result.getSSOToken();
            userName = result.getUserPrincipal();
        }
        LocalizedMessage message = getDenyMessage(userName, requestURL);
        getAmAgentLog().log(ssoToken, message);
    }

    private void initAmAgentLog() throws AgentException {
        setAmAgentLog(AmAgentLogManager.getAmAgentLogInstance());
    }

    private void setAmAgentLog(IAmAgentLog amAgentLog) {
        _amAgentLog = amAgentLog;
    }

    private IAmAgentLog getAmAgentLog() {
        return _amAgentLog;
    }
    
    private void setAuditLogMode (AuditLogMode auditLogMode) {
        _auditLogMode = auditLogMode;
    }
    
    private AuditLogMode getAuditLogMode() {
        return _auditLogMode;
    }
    
    private AuditLogMode _auditLogMode = AuditLogMode.MODE_BOTH;
    private IAmAgentLog _amAgentLog;
}
