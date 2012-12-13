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
 * $Id: GetAllowedIdOperations.java,v 1.4 2008/06/25 05:42:15 qcheng Exp $
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
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets the allowed operations of a identity type.
 */
public class GetAllowedIdOperations extends IdentityCommand {
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
        String type = getStringOptionValue(ARGUMENT_ID_TYPE);
        String[] params = {realm, type};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_ALLOWED_OPS", params);

        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            IdType idType = convert2IdType(type);
            Set ops = amir.getAllowedIdOperations(idType);
            String msg = getResourceString("allowed-ops-result");
            String[] arg = {""};

            if ((ops != null) && !ops.isEmpty()) {
                for (Iterator i = ops.iterator(); i.hasNext(); ) {
                    arg[0] = ((IdOperation)i.next()).getName();
                    outputWriter.printlnMessage(
                        MessageFormat.format(msg, (Object[])arg));
                }
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-allowed-ops-no-ops"),
                        (Object[])params));
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_GET_ALLOWED_OPS", params);
        } catch (IdRepoException e) {
            String[] args = {realm, type, e.getMessage()};
            debugError("GetAllowedIdOperations.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_GET_ALLOWED_OPS",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, e.getMessage()};
            debugError("GetAllowedIdOperations.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_GET_ALLOWED_OPS",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
