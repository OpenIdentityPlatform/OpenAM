/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AddAuthConfigurationEntry.java,v 1.3 2008/12/16 06:46:59 veiming Exp $
 *
 */

package com.sun.identity.cli.authentication;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddAuthConfigurationEntry extends AuthenticatedCommand {
    private static Set POSSIBLE_CRITERIA = new HashSet();
    static {
        POSSIBLE_CRITERIA.add("REQUIRED");
        POSSIBLE_CRITERIA.add("OPTIONAL");
        POSSIBLE_CRITERIA.add("SUFFICIENT");
        POSSIBLE_CRITERIA.add("REQUISITE");
    }

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
        String moduleName = getStringOptionValue(
            AuthOptions.AUTH_CONFIG_MODULE_NAME);
        String criteria = getStringOptionValue(
            AuthOptions.AUTH_CONFIG_CRITERIA);
        String options = getStringOptionValue(AuthOptions.AUTH_CONFIG_OPTIONS);
        String[] params = {realm, configName, moduleName};

        if (!POSSIBLE_CRITERIA.contains(criteria)) {
            throw new CLIException(getResourceString(
                "authentication-add-auth-config-entry-criteria.invalid"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        int pos = getPosition();
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_ADD_AUTH_CONFIG_ENTRY", params);
        
        try {
            AuthConfigurationEntry ae = new AuthConfigurationEntry(
                moduleName, criteria, options);
            Set instanceNames = getInstanceNames(realm, adminSSOToken);
            String instanceName = ae.getLoginModuleName();
            if (!instanceNames.contains(instanceName)) {
                Object[] p = {instanceName};
                throw new CLIException(MessageFormat.format(getResourceString(
                    "authentication-add-auth-config-entry-not-found"),
                    p), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            List entries = getConfigEntries(realm, configName, adminSSOToken);
            if (entries == null) {
                entries = new ArrayList();
            }

            if ((pos == -1) || (pos >= entries.size())) {
                entries.add(ae);
            } else {
                entries.add(pos, ae);
            }

            Map configData = new HashMap(2);
            Set tmp = new HashSet(2);
            String xml = AMAuthConfigUtils.authConfigurationEntryToXMLString(
                entries);
            tmp.add(xml);
            configData.put(AuthOptions.AUTH_CONFIG_ATTR, tmp);
            IOutput outputWriter = getOutputWriter();
            AMAuthConfigUtils.replaceNamedConfig(configName, 0, configData,
                realm, adminSSOToken);
            outputWriter.printlnMessage(getResourceString(
                "authentication-add-auth-config-entry-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_ADD_AUTH_CONFIG_ENTRY", params);
        } catch (AMConfigurationException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            String[] p = {realm, configName, moduleName, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_AUTH_CONFIG_ENTRY", p);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            String[] p = {realm, configName, moduleName, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_AUTH_CONFIG_ENTRY", p);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            String[] p = {realm, configName, moduleName, e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_AUTH_CONFIG_ENTRY", p);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private int getPosition()
        throws CLIException {
        int pos = 0;
        String position =getStringOptionValue(AuthOptions.AUTH_CONFIG_POSITION);
        if ((position == null) || (position.trim().length() == 0)) {
            pos = -1;
        } else {
            try {
                pos = Integer.parseInt(position);
                if (pos < 0) {
                    throw new CLIException(getResourceString(
                        "authentication-add-auth-config-entry-position.invalid"),
                        ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
            } catch (NumberFormatException e) {
                throw new CLIException(getResourceString(
                    "authentication-add-auth-config-entry-position.invalid"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
        return pos;
    }
    
    private Set getInstanceNames(String realm, SSOToken adminSSOToken)
        throws AMConfigurationException {
        Set names = new HashSet();
        AMAuthenticationManager mgr = new AMAuthenticationManager(
            adminSSOToken, realm);
        Set instances = mgr.getAuthenticationInstances();

        for (Iterator i = instances.iterator(); i.hasNext();) {
            AMAuthenticationInstance instance =
                (AMAuthenticationInstance) i.next();
            names.add(instance.getName());
        }

        return names;
    }

    private List getConfigEntries(
        String realm,
        String configName,
        SSOToken adminSSOToken
    ) throws SMSException, SSOException, AMConfigurationException {
        List entries = null;
        Map configData = AMAuthConfigUtils.getNamedConfig(
            configName, realm, adminSSOToken);
        if ((configData != null) && !configData.isEmpty()) {
            Set tmp = (Set)configData.get(AuthOptions.AUTH_CONFIG_ATTR);

            if ((tmp != null) && !tmp.isEmpty()) {
                String xml = (String)tmp.iterator().next();
                entries = new ArrayList(
                    AMAuthConfigUtils.xmlToAuthConfigurationEntry(xml));
            }
        }
        return entries;
    }
}
