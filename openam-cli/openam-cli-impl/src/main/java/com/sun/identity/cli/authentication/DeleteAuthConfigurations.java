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
 * $Id: DeleteAuthConfigurations.java,v 1.3 2008/12/16 06:47:00 veiming Exp $
 *
 */

package com.sun.identity.cli.authentication;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;
import java.util.Iterator;
import java.util.List;

public class DeleteAuthConfigurations extends AuthenticatedCommand {
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
        List names = (List)rc.getOption(AuthOptions.AUTH_CONFIG_NAMES);
        String[] params = {realm, names.toString()};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_DELETE_AUTH_CONFIGURATIONS", params);
        
        try {
            for (Iterator i = names.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                AMAuthConfigUtils.removeNamedConfig(name, realm, adminSSOToken);
            }
            
            if (names.size() == 1) {
                getOutputWriter().printlnMessage(
                    getResourceString(
                    "authentication-delete-auth-configuration-succeeded"));
            } else {
                getOutputWriter().printlnMessage(
                    getResourceString(
                    "authentication-delete-auth-configurations-succeeded"));
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_DELETE_AUTH_CONFIGURATIONS", params);
        } catch (AMConfigurationException e) {
            debugError("DeleteAuthConfiguraton.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_AUTH_CONFIGURATIONS", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            debugError("DeleteAuthConfiguraton.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_AUTH_CONFIGURATIONS", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            debugError("DeleteAuthConfiguraton.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_AUTH_CONFIGURATIONS", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
