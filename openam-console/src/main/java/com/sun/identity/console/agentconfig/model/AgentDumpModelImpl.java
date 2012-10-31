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
 * $Id: AgentDumpModelImpl.java,v 1.1 2008/12/10 18:25:14 farble1670 Exp $
 *
 */

package com.sun.identity.console.agentconfig.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.SMSException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

public class AgentDumpModelImpl extends AMModelBase implements AgentDumpModel
{

    private static SSOToken adminSSOToken = AMAdminUtils.getSuperAdminSSOToken();

    public AgentDumpModelImpl(HttpServletRequest req, Map map) {
        super(req, map);
    }

    public Map getAttributeValues(String universalId)
            throws AMConsoleException {
        try {
            AMIdentity amid = IdUtils.getIdentity(adminSSOToken, universalId);
            Map values = AgentConfiguration.getAgentAttributes(amid, true);
            return values;
        } catch (IdRepoException re) {
            throw new AMConsoleException(re.getMessage());
        } catch (SMSException se) {
            throw new AMConsoleException(se.getMessage());
        } catch (SSOException ssoe) {
            throw new AMConsoleException(ssoe.getMessage());
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
