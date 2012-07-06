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
 * $Id: GetAuthConfigurationEntries.java,v 1.3 2008/12/16 06:47:01 veiming Exp $
 *
 */

package com.sun.identity.cli.authentication;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.config.AuthConfigurationEntry;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetAuthConfigurationEntries extends AuthenticatedCommand {
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
        String configName = getStringOptionValue(AuthOptions.AUTH_CONFIG_NAME);
        
        String[] params = {realm, configName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_AUTH_CONFIG_ENTRIES", params);
        
        try {
            Map configData = AMAuthConfigUtils.getNamedConfig(
                configName, realm, adminSSOToken);
            IOutput outputWriter = getOutputWriter();
            boolean hasData = false;
            
            if ((configData != null) && !configData.isEmpty()) {
                Set tmp = (Set)configData.get(AuthOptions.AUTH_CONFIG_ATTR);

                if ((tmp != null) && !tmp.isEmpty()) {
                    hasData = true;
                    String xml = (String)tmp.iterator().next();
                    List entryList = new ArrayList(
                        AMAuthConfigUtils.xmlToAuthConfigurationEntry(xml));

                    outputWriter.printlnMessage(getResourceString(
                        "authentication-get-auth-config-entries-succeeded"));

                    for (Iterator i = entryList.iterator(); i.hasNext(); ) {
                        AuthConfigurationEntry e = (AuthConfigurationEntry)
                            i.next();
                        String options = e.getOptions();
                        if (options == null) {
                            options = "";
                        }
                        Object[] args = {e.getLoginModuleName(), 
                            e.getControlFlag(), options};
                        outputWriter.printlnMessage(MessageFormat.format(
                            getResourceString(
                                "authentication-get-auth-config-entries-entry"),
                            args));
                    }
                }
            } else {
                writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_GET_AUTH_CONFIG_ENTRIES", params);
                throw new CLIException(
                    getResourceString(
                        "authentication-get-auth-config-entries-not-found"), 
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            if (!hasData) {
                outputWriter.printlnMessage(getResourceString(
                    "authentication-get-auth-config-entries-no-values"));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_GET_AUTH_CONFIG_ENTRIES", params);
        } catch (AMConfigurationException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
