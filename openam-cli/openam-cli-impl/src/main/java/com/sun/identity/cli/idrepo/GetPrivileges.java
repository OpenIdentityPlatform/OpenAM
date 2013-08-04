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
 * $Id: GetPrivileges.java,v 1.3 2008/06/25 05:42:15 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.cli.idrepo;


import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets the privilege of an identity.
 */
public class GetPrivileges extends IdentityCommand {

    private static final String ALL_AUTHENTICATED_USERS = "All Authenticated Users";

    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);

        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String idName = getStringOptionValue(ARGUMENT_ID_NAME);
        String type = getStringOptionValue(ARGUMENT_ID_TYPE);
        IdType idType = convert2IdType(type);
        String[] params = {realm, type, idName};

        try {
            DelegationManager mgr = new DelegationManager(
                adminSSOToken, realm);
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_IDREPO_GET_PRIVILEGES", params);
            AMIdentity amid;
            if (idType.equals(IdType.ROLE) && idName.equalsIgnoreCase(ALL_AUTHENTICATED_USERS)) {
                //realm needs to be /, see DelegationPolicyImpl#privilegeToPolicy implementation
                amid = new AMIdentity(adminSSOToken, idName, idType, "/", null);
                //do not check the existense of all authenticated users role as it would fail
            } else {
                amid = new AMIdentity(adminSSOToken, idName, idType, realm, null);
                if (!amid.isExists()) {
                    Object[] p = {idName, type};
                    throw new CLIException(MessageFormat.format(getResourceString("identity-does-not-exist"), p),
                            ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
            }
            Set results = mgr.getPrivileges(amid.getUniversalId());

            if ((results != null) && !results.isEmpty()) {
                String[] param = {""};
                String msg = getResourceString("privilege-result");

                for (Iterator i = results.iterator(); i.hasNext(); ) {
                    DelegationPrivilege p = (DelegationPrivilege)i.next();
                    param[0] = p.getName();
                    outputWriter.printlnMessage(MessageFormat.format(
                        msg, (Object[])param));
                }
            } else {
                outputWriter.printlnMessage(getResourceString("no-privileges"));
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_IDREPO_GET_PRIVILEGES", params);
        } catch (DelegationException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetPrivileges.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_PRIVILEGES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IdRepoException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetPrivileges.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_PRIVILEGES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("GetPrivileges.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_GET_PRIVILEGES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
