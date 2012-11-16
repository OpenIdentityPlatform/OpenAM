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
 * $Id: CreateRealm.java,v 1.5 2008/06/25 05:42:15 qcheng Exp $
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Creates realm command.
 */
public class CreateRealm extends AuthenticatedCommand {
    
    /**
     * Creates a sub realm.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }
        String parentRealm = RealmUtils.getParentRealm(realm);
        String childRealm = RealmUtils.getChildRealm(realm);

        String[] params = {realm};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO, "ATTEMPT_CREATE_REALM",
            params);

        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, parentRealm);
            Map defaultValues = new HashMap(2);
            Map map = new HashMap(2);
            Set values = new HashSet(2);
            values.add("Active");
            map.put(IdConstants.ORGANIZATION_STATUS_ATTR, values);
            defaultValues.put(IdConstants.REPO_SERVICE, map);
            ocm.createSubOrganization(childRealm, defaultValues);
            getOutputWriter().printlnMessage(getResourceString(
                "create-realm-succeed"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "SUCCEED_CREATE_REALM",
                params);
        } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
            debugError("CreateRealm.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_REALM",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
