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
 * $Id: RealmModifyService.java,v 1.3 2008/06/25 05:42:16 qcheng Exp $
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Modifies service attribute values of a realm.
 */
public class RealmModifyService extends AuthenticatedCommand {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        Map attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);
        try {
            String[] params = {realm, serviceName};

            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, realm);
            Set assignedServices = ocm.getAssignedServices(true);

            AMIdentityRepository repo = new AMIdentityRepository(
                adminSSOToken, realm);
            AMIdentity ai = repo.getRealmIdentity();
            Set servicesFromIdRepo = ai.getAssignedServices();
            boolean modified = false;
            
            if (assignedServices.contains(serviceName)) {
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_MODIFY_SERVICE_REALM", params);
                ocm.modifyService(serviceName, attributeValues);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_MODIFY_SERVICE_REALM", params);
                modified = true;
            }
            
            if (servicesFromIdRepo.contains(serviceName)) {
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_MODIFY_SERVICE_REALM", params);
                ai.modifyService(serviceName, attributeValues);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_MODIFY_SERVICE_REALM", params);
                modified = true;
            }
            
            if (modified) {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("modify-service-of-realm-succeed"),
                    (Object[])params));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("modify-service-of-realm-not-assigned"),
                    (Object[])params));
            }
        } catch (IdRepoException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmModifyService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_MODIFY_SERVICE_REALM", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmModifyService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_MODIFY_SERVICE_REALM", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmModifyService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_MODIFY_SERVICE_REALM", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
