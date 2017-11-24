/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: STSExportPolicyModelImpl.java,v 1.1 2009/12/19 00:14:56 asyhuang Exp $
 *
 */
package com.sun.identity.console.service.model;

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

public class STSExportPolicyModelImpl extends AMModelBase implements STSExportPolicyModel {
    
    public STSExportPolicyModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    public Map getAttributeValues()
            throws AMConsoleException {
        try {
            Map values = new HashMap();
            values.put("policyAttributeValues", getPolicyAttributeValues());
            values.put("inputPolicyAttributeValues", getInputPolicyAttributeValues());
            values.put("outputPolicyAttributeValues", getOutputPolicyAttributeValues());
            return values;
        } catch (AMConsoleException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    public String getPolicyAttributeValues()
            throws AMConsoleException {
        try {
            WSSPolicyManager policMmanager = WSSPolicyManager.getInstance();
            String values = policMmanager.getSTSPolicy();
            return values;       
        } catch (WSSPolicyException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    public String getInputPolicyAttributeValues()
            throws AMConsoleException {
        try {
            WSSPolicyManager policMmanager = WSSPolicyManager.getInstance();
            String values = policMmanager.getSTSInputPolicy();
            return values;      
        } catch (WSSPolicyException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }

    public String getOutputPolicyAttributeValues()
            throws AMConsoleException {
        try {
            WSSPolicyManager policMmanager = WSSPolicyManager.getInstance();
            String values = policMmanager.getSTSOutputPolicy();
            return values;       
        } catch (WSSPolicyException ex) {
            throw new AMConsoleException(ex.getMessage());
        }
    }
   
}
