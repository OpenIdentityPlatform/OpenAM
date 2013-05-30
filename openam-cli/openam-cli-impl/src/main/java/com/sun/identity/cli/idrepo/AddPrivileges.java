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
 * $Id: AddPrivileges.java,v 1.9 2009/12/23 21:36:21 veiming Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
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
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command adds privileges to an identity.
 */
public class AddPrivileges extends IdentityCommand {

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
        List privileges = (List)rc.getOption(IArgument.PRIVILEGES);
        IdType idType = convert2IdType(type);
        String[] params = {realm, type, idName};

        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_IDREPO_ADD_PRIVILEGES", params);

            DelegationManager mgr = new DelegationManager(
                adminSSOToken, realm);
            Set privilegeObjects = mgr.getPrivileges();
            AMIdentity amid;
            if (idType.equals(IdType.ROLE) && idName.equalsIgnoreCase(ALL_AUTHENTICATED_USERS)) {
                //realm needs to be /, see DelegationPolicyImpl#privilegeToPolicy implementation
                amid = new AMIdentity(adminSSOToken, idName, idType, "/", null);
                //do not check the existense of all authenticated users role as it would fail
            } else {
                amid = new AMIdentity(adminSSOToken, idName, idType, realm, null);
                if (!amid.isExists()) {
                    Object[] p = {idName, type};
                    throw new CLIException(MessageFormat.format(
                            getResourceString("idrepo-add-privileges-do-not-exist"), p),
                            ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
            }
            String uid = amid.getUniversalId();

            DelegationPrivilege newDp = null;
            for (Iterator i = privileges.iterator(); i.hasNext(); ){
                String name = (String)i.next();
                DelegationPrivilege dp = getDelegationPrivilege(
                    name, privilegeObjects);
                if (dp != null) {
                    Set subjects = dp.getSubjects();
                    if (!subjects.contains(uid)) {
                        subjects.add(uid);
                        newDp = new DelegationPrivilege(name, subjects, realm);
                        mgr.addPrivilege(newDp);
                    } else {
                        String[] args = {idName, name};
                        String msg = MessageFormat.format(getResourceString(
                            "delegation-already-has-privilege"), 
                            (Object[])args);
                        throw new CLIException(msg,
                            ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                    }
                } else {
                    Set subjects = new HashSet(2);
                    subjects.add(uid);
                    newDp = new DelegationPrivilege(name, subjects, realm);
                    mgr.addPrivilege(newDp);
                }
            }

            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("idrepo-add-privileges-succeed"), 
                    (Object[])params));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_IDREPO_ADD_PRIVILEGES", params);
        } catch (IdRepoException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("AddPrivileges.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_ADD_PRIVILEGES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (DelegationException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("AddPrivileges.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_ADD_PRIVILEGES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("AddPrivileges.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_ADD_PRIVILEGES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
