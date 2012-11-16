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
 * $Id: UpdateAuthConfigurationEntries.java,v 1.4 2008/12/16 06:47:01 veiming Exp $
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
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class UpdateAuthConfigurationEntries extends AuthenticatedCommand {
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
        
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List listEntries = rc.getOption(AuthOptions.AUTH_CONFIG_ENTRIES);

        if ((datafile == null) && (listEntries == null)) {
            throw new CLIException(
                getResourceString(
                    "authentication-set-auth-config-entries-missing-data"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }
        
        String[] params = {realm, configName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SET_AUTH_CONFIG_ENTRIES", params);
        
        try {
            List entries = parse(datafile, listEntries);
            validateEntries(realm, adminSSOToken, entries, params);
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
                "authentication-set-auth-config-entries-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_SET_AUTH_CONFIG_ENTRIES", params);
        } catch (AMConfigurationException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            debugError("GetAuthConfigurationEntries.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void validateEntries(
        String realm, 
        SSOToken adminSSOToken,
        List entries,
        String[] params
    ) throws CLIException {
        if ((entries != null) && !entries.isEmpty()) {
            Set instanceNames = getInstanceNames(realm, adminSSOToken, params);

            for (Iterator i = entries.iterator(); i.hasNext();) {
                AuthConfigurationEntry token = (AuthConfigurationEntry)i.next();
                String instanceName = token.getLoginModuleName();
                if (!instanceNames.contains(instanceName)) {
                    Object[] p = {instanceName};
                    throw new CLIException(MessageFormat.format(
                        getResourceString(
                   "authentication-set-auth-config-entries-instance-not-found"),
                        p),
                        ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                }
            }
        }
    }
    
    private Set getInstanceNames(
        String realm, 
        SSOToken adminSSOToken, 
        String[] params
    ) throws CLIException {
        Set names = new HashSet();

        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                adminSSOToken, realm);
            Set instances = mgr.getAuthenticationInstances();

            for (Iterator i = instances.iterator(); i.hasNext();) {
                AMAuthenticationInstance instance =
                    (AMAuthenticationInstance) i.next();
                names.add(instance.getName());
            }
        } catch (AMConfigurationException e) {
            debugError("ListAuthInstances.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_AUTH_CONFIG_ENTRIES", params);
            throw new CLIException(e,
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        return names;
    }
    
    private List parse(String fileName, List listEntries)
        throws CLIException, AMConfigurationException {
        CommandManager mgr = getCommandManager();
        
        List results = null;
        if (fileName != null) {
            results = parse(mgr, fileName);
        }

        if ((listEntries != null) && !listEntries.isEmpty()) {
            if (results != null) {
                results.addAll(parse(mgr, listEntries));
            } else {
                results = parse(mgr, listEntries);
            }
        }

        return (results == null) ? new ArrayList() : results;
    }
    
    private List parse(CommandManager mgr, String fileName)
        throws CLIException, AMConfigurationException {
        BufferedReader in = null;
        List entries = new ArrayList();

        try {
            in = new BufferedReader(new FileReader(fileName));
            String line = in.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0) {
                    entries.add(getAuthConfigurationEntry(mgr, line));
                }
                line = in.readLine();
            }
        } catch (IOException e) {
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } finally {
            if (in !=null ) {
                try {
                    in.close();
                } catch (IOException e) {
                    //ignore cannot close input stream
                }
            }
        }
        return entries;
    }
    
    private List parse(CommandManager mgr, List list) 
        throws CLIException, AMConfigurationException {
        List entries = new ArrayList();

        if ((list != null) && !list.isEmpty()) {
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                String s = ((String)i.next()).trim();
                if (s.length() > 0) {
                    entries.add(getAuthConfigurationEntry(mgr, s));
                }
            }
        }
        return entries;
    }
    
    private AuthConfigurationEntry getAuthConfigurationEntry(
        CommandManager mgr,
        String str
    ) throws CLIException, AMConfigurationException {
        System.out.println(str);
        StringTokenizer st = new StringTokenizer(str, "|");
        if (st.countTokens() < 2) {
            throw AttributeValues.createIncorrectFormatException(
                mgr, str);
        }
        
        String name = st.nextToken();
        String flag = st.nextToken();
        String options = (st.countTokens() > 0) ?
            st.nextToken() : null;
        return new AuthConfigurationEntry(name, flag, options);
    }
}
