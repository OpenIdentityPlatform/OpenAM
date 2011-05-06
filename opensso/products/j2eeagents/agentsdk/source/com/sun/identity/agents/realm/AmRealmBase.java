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
 * $Id: AmRealmBase.java,v 1.4 2008/06/25 05:51:58 qcheng Exp $
 *
 */

package com.sun.identity.agents.realm;

import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.AuditLogMode;
import com.sun.identity.agents.arch.LocalizedMessage;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.ServiceFactory;
import com.sun.identity.agents.common.SSOValidationResult;
import com.sun.identity.agents.log.AmAgentLogManager;
import com.sun.identity.agents.log.IAmAgentLog;


/**
 * The base class for agent realm implementation. 
 * This class is initialized at agent start up and also on an agent 
 * configuration reload.
 */
public abstract class AmRealmBase extends AgentBase implements IAmRealm {
    
    public AmRealmBase(Manager manager) {
        super(manager);
    }
    
    /**
     * @see IAmRealm.getMemberships method
     *
     * @return null if getRealmMembershipCacheFlag = false 
     *         and if getRealmMembershipCacheFlag = true then returns the set of
     *         memberships for the userName.
     */
    public Set getMemberships(String userName) {
            return getMembershipCache().getMembershipFromCache(userName);
    }
    
    protected void processAuthenticationResult(String userName,
            AmRealmAuthenticationResult result,
            SSOValidationResult ssoValidationResult) 
                    throws AgentException
    {
        SSOToken ssoToken = null;
        String applicationName = null;
        if (result.isValid()) {
            ssoToken = ssoValidationResult.getSSOToken();
            applicationName = ssoValidationResult.getApplicationName();
            cacheMembership(userName, result.getAttributes(), 
                    ssoValidationResult);
            if (logAllowEnabled()) {
                logAllow(userName, ssoToken, applicationName);
            }
        } else {
            if (logDenyEnabled()) {
                logDeny(userName, ssoToken, applicationName);
            }
        }
    }

    public void initialize() throws AgentException {
        initMembershipCache();
        initAmAgentLog(); 
        setAuditLogMode(AgentConfiguration.getAuditLogMode().getIntValue());
    }
    
    private void cacheMembership(String userName, Set membershipSet, 
            SSOValidationResult ssoValidationResult) throws AgentException
    {
        try {
            getMembershipCache().addMembershipCacheEntry(userName, 
                    membershipSet, ssoValidationResult);
        } catch (Exception ex) {
            logError("AmRealmBase.cacheMembership: Exception caught while"
                     + " trying to cache memberships for " + userName, ex);
        }
    }
    
    private boolean logAllowEnabled() {
        boolean result = false;
        switch(getAuditLogMode()) {
                    case AuditLogMode.INT_MODE_ALLOW:
                    case AuditLogMode.INT_MODE_BOTH:
                        result = true;
                            break;
        }
        return result;
    }
    
    private boolean logDenyEnabled() {
        boolean result = false;
        switch(getAuditLogMode()) {
                case AuditLogMode.INT_MODE_DENY:
                case AuditLogMode.INT_MODE_BOTH:
                    result = true;
                        break;
        }
        return result;
    }
    
    private void logAllow(String userName, SSOToken ssoToken, String appName)
            throws AgentException 
    {
        LocalizedMessage message = getModule().makeLocalizableString(
                IAmRealmModuleConstants.MSG_AM_REALM_AUTH_SUCCESS,
                new Object[] { userName, appName });
        getAmAgentLog().log(ssoToken, message);
    }

    private void logDeny(String userName, SSOToken ssoToken, String appName)
            throws AgentException 
    {
        LocalizedMessage message = getModule().makeLocalizableString(
                IAmRealmModuleConstants.MSG_AM_REALM_AUTH_FAILURE,
                new Object[] { userName, appName });
        getAmAgentLog().log(ssoToken, message);
    }       

    private void initMembershipCache() throws AgentException {
        setMembershipCache(
                new AmRealmMembershipCache(getManager()));
    } 
         
    private AmRealmMembershipCache getMembershipCache() {
        return _membershipCache;
    }

    private void setMembershipCache(AmRealmMembershipCache cache) {
        _membershipCache = cache;
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
    
    private void setAuditLogMode (int auditLogMode) {
        _auditLogMode = auditLogMode;
    }
    
    private int getAuditLogMode() {
        return _auditLogMode;
    }    
        
   
    private AmRealmMembershipCache _membershipCache;
    private IAmAgentLog _amAgentLog;
    private int _auditLogMode;
}
