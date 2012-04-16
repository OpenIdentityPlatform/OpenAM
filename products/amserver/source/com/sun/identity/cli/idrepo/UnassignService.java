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
 * $Id: UnassignService.java,v 1.2 2008/06/25 05:42:15 qcheng Exp $
 *
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
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * This command unassigns service from an identity.
 */
public class UnassignService extends IdentityCommand {
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
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        IdType idType = convert2IdType(type);

        String[] params = {realm, type, idName, serviceName};

        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_IDREPO_UNASSIGN_SERVICE", params);
            AMIdentity amid = new AMIdentity(
                adminSSOToken, idName, idType, realm, null); 
            amid.unassignService(serviceName);

            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("idrepo-unassign-service-succeed"), 
                    (Object[])params));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_IDREPO_UNASSIGN_SERVICE", params);
        } catch (IdRepoException e) {
            String[] args = {realm, type, idName, serviceName, e.getMessage()};
            debugError("UnassignService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_UNASSIGN_SERVICE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, idName, serviceName, e.getMessage()};
            debugError("UnassignService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_IDREPO_UNASSIGN_SERVICE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
