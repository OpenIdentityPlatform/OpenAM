/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GetAuthInstance.java,v 1.4 2008/12/16 06:47:01 veiming Exp $
 *
 */

package com.sun.identity.cli.authentication;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMAuthenticationSchema;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIUtil;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;
import java.util.Map;

public class GetAuthInstance extends AuthenticatedCommand {
    /**
     * Handles request.
     *
     * @param rc Request Context.
     * @throws CLIException if request cannot be processed.
     */
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String instanceName = getStringOptionValue(
            AuthOptions.AUTH_INSTANCE_NAME);
        
        String[] params = {realm, instanceName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_AUTH_INSTANCE", params);
        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                adminSSOToken, realm);
            AMAuthenticationInstance ai = mgr.getAuthenticationInstance(
                instanceName);
            if (ai != null) {
                IOutput outputWriter = getOutputWriter();
                Map attributeValues = ai.getAttributeValues();
                
                if ((attributeValues != null) && !attributeValues.isEmpty()) {

                    AMAuthenticationSchema schema =
                        mgr.getAuthenticationSchema(ai.getType());
                    String serviceName = schema.getServiceName();

                    outputWriter.printlnMessage(getResourceString(
                        "authentication-get-auth-instance-succeeded"));
                    outputWriter.printlnMessage(
                        FormatUtils.printAttributeValues(getResourceString(
                            "authentication-get-auth-instance-result"),
                        attributeValues,
                        CLIUtil.getPasswordFields(serviceName)));
                } else {
                    outputWriter.printlnMessage(getResourceString(
                        "authentication-get-auth-instance-no-values"));
                }                
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_GET_AUTH_INSTANCE", params);
            } else {
                writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_GET_AUTH_INSTANCE", params);
                throw new CLIException(
                    getResourceString(
                        "authentication-get-auth-instance-not-found"), 
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (SSOException e) {
            debugError("GetAuthInstance.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_AUTH_INSTANCE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            debugError("GetAuthInstance.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_AUTH_INSTANCE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (AMConfigurationException e) {
            debugError("GetAuthInstance.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_AUTH_INSTANCE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
