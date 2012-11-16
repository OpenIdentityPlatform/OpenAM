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
 * $Id: AddResourceBundle.java,v 1.5 2008/06/25 05:42:07 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ISResourceBundle;
import com.sun.identity.sm.SMSException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Adds resource bundle to data store. So that the resource cannot be
 * made available to different hosts.
 */
public class AddResourceBundle extends AuthenticatedCommand {
    static final String ARGUMENT_RESOURCE_BUNDLE_FILE_NAME =
        "bundlefilename";

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

        String bundleName = getStringOptionValue(
            IArgument.RESOURCE_BUNDLE_NAME);
        String fileName = getStringOptionValue(
            ARGUMENT_RESOURCE_BUNDLE_FILE_NAME);
        String localeName = getStringOptionValue(
            IArgument.RESOURCE_BUNDLE_LOCALE);
        
        try {
            String[] params = {bundleName, fileName, localeName};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_ADD_RESOURCE_BUNDLE", params);

            CommandManager mgr = getCommandManager();
            String url = mgr.getWebEnabledURL();
            
            if (url != null) {
                params[1] = CLIConstants.WEB_INPUT;
            }
            
            Map mapStrings = (url != null) ? 
                getResourceStringsMap(new StringReader(fileName)) :
                getResourceStringsMap(new FileReader(fileName));
            
            ISResourceBundle.storeResourceBundle(adminSSOToken,
                bundleName, localeName, mapStrings);

            getOutputWriter().printlnMessage(getResourceString(
                "resourcebundle-added"));
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_ADD_RESOURCE_BUNDLE", params);
        } catch (SSOException e) {
            String[] args = {bundleName, fileName, localeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_ADD_RESOURCE_BUNDLE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            String[] args = {bundleName, fileName, localeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_ADD_RESOURCE_BUNDLE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {bundleName, fileName, localeName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_ADD_RESOURCE_BUNDLE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private Map getResourceStringsMap(Reader reader)
        throws CLIException
    {
        BufferedReader in = null;
        Map resourceStrings = new HashMap();
    
        try {
            boolean commented = false;
            in = new BufferedReader(reader);
            String line = in.readLine();

            /*
             * reading a file, and scan for format like <key>=<value>
             * and map the key to value in a map.
             */
            while (line != null) {
                line = line.trim();

                if (line.startsWith("/*")) { // mark the start of comment block
                    commented = true;
                } else if (line.endsWith("*/")) { // mark the end of comment
                    commented = false;
                } else if (line.startsWith("#")) {
                    // ignore this line
                } else if (!commented) {
                    int idx = line.indexOf('=');
                    if (idx != -1) {
                        String key = line.substring(0, idx).trim();
                        if (key.length() > 0) {
                            Set tmp = new HashSet(2);
                            String value = line.substring(idx+1).trim();
                            tmp.add(value);
                            resourceStrings.put(key, tmp);
                        }
                    }
                }
                line = in.readLine();
            }
        } catch(IOException e){
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } finally {
            if (in !=null ) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new CLIException(e, ExitCodes.IO_EXCEPTION);
                }
            }
        }
        return resourceStrings;
    }
}
