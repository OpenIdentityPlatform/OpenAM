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
 * $Id: CreateIdentity.java,v 1.4 2008/06/25 05:42:15 qcheng Exp $
 *
 */

package com.sun.identity.cli.idrepo;


import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command creates identity.
 */
public class CreateIdentity extends IdentityCommand {
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
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String idName = getStringOptionValue(ARGUMENT_ID_NAME);
        String type = getStringOptionValue(ARGUMENT_ID_TYPE);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);
        Map attributeValues = Collections.EMPTY_MAP;
        
        if ((datafile != null) || (attrValues != null)) {
            attributeValues = AttributeValues.parse(getCommandManager(),
                datafile, attrValues);        
        }

        String[] params = {realm, type, idName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO, "ATTEMPT_CREATE_IDENTITY",
            params);
        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            IdType idType = convert2IdType(type);
            Set set = amir.getAllowedIdOperations(idType);
            if (!set.contains(IdOperation.CREATE)) {
                String[] args = {realm, type};
                throw new CLIException(MessageFormat.format(
                    getResourceString("does-not-support-creation"),
                    (Object[])args),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            amir.createIdentity(idType, idName, attributeValues);
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("create-identity-succeed"),
                (Object[])params));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_CREATE_IDENTITY", params);
        } catch (IdRepoException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("CreateIdentity.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_IDENTITY",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, type, idName, e.getMessage()};
            debugError("CreateIdentity.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_IDENTITY",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
