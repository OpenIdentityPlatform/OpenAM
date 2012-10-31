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
 * $Id: AgentExportPolicyModelImpl.java,v 1.1 2009/12/19 00:08:14 asyhuang Exp $
 *
 */
package com.sun.identity.console.agentconfig.model;

import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.wss.policy.WSSPolicyException;
import com.sun.identity.wss.policy.WSSPolicyManager;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.ProviderException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class AgentExportPolicyModelImpl extends AMModelBase implements AgentExportPolicyModel {

    // private static SSOToken adminSSOToken = AMAdminUtils.getSuperAdminSSOToken();
    public AgentExportPolicyModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    public Map getAttributeValues(String agentName, String agentType)
            throws AMConsoleException {
        try {
            Map values = new HashMap();
            values.put("policyAttributeValues", getPolicyAttributeValues(agentName, agentType));
            values.put("inputPolicyAttributeValues", getInputPolicyAttributeValues(agentName, agentType));
            values.put("outputPolicyAttributeValues", getOutputPolicyAttributeValues(agentName, agentType));
            return values;
        } catch (AMConsoleException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    public String getPolicyAttributeValues(String agentName, String agentType)
            throws AMConsoleException {
        try {
            WSSPolicyManager policMmanager = WSSPolicyManager.getInstance();
            String values = policMmanager.getPolicy(ProviderConfig.getProvider(agentName, agentType));
            return values;
        } catch (ProviderException ex) {
            throw new AMConsoleException(ex.getMessage());
        } catch (WSSPolicyException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    public String getInputPolicyAttributeValues(String agentName, String agentType)
            throws AMConsoleException {
        try {
            WSSPolicyManager policMmanager = WSSPolicyManager.getInstance();
            String values = policMmanager.getInputPolicy(ProviderConfig.getProvider(agentName, agentType));
            return values;
        } catch (ProviderException ex) {
            throw new AMConsoleException(ex.getMessage());
        } catch (WSSPolicyException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    public String getOutputPolicyAttributeValues(String agentName, String agentType)
            throws AMConsoleException {
        try {
            WSSPolicyManager policMmanager = WSSPolicyManager.getInstance();
            String values = policMmanager.getOutputPolicy(ProviderConfig.getProvider(agentName, agentType));
            return values;
        } catch (ProviderException ex) {
            throw new AMConsoleException(ex.getMessage());
        } catch (WSSPolicyException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    public String getDisplayName(String universalId) throws AMConsoleException {
        try {
            AMIdentity amid =
                    IdUtils.getIdentity(getUserSSOToken(), universalId);
            return amid.getName();
        } catch (IdRepoException e) {
            throw new AMConsoleException(getErrorString(e));
        }       
    }

}
