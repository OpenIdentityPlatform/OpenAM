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
 * $Id: UpdateAuthInstance.java,v 1.3 2008/12/16 06:47:01 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2014 Forgerock AS.
 */

package com.sun.identity.cli.authentication;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIUtil;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Level;
import com.sun.identity.sm.SMSException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpdateAuthInstance extends AuthenticatedCommand {
    private static final String FILE_REFERENCE_SUFFIX = "-file";
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
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        @SuppressWarnings("unchecked")
        Map<String, Set<String>> attributeValues =
                AttributeValues.parse(getCommandManager(), datafile, attrValues);

        attributeValues = processFileAttributes(attributeValues);
        
        String[] params = {realm, instanceName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_UPDATE_AUTH_INSTANCE", params);
        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                adminSSOToken, realm);
            AMAuthenticationInstance ai = mgr.getAuthenticationInstance(
                instanceName);
            if (ai != null) {
                ai.setAttributeValues(attributeValues);
                getOutputWriter().printlnMessage(
                    getResourceString(
                    "authentication-update-auth-instance-succeeded"));
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_UPDATE_AUTH_INSTANCE", params);
            } else {
                writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_UPDATE_AUTH_INSTANCE", params);
                throw new CLIException(
                    getResourceString(
                        "authentication-update-auth-instance-not-found"), 
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (AMConfigurationException e) {
            debugError("UpdateAuthInstance.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_AUTH_INSTANCE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            debugError("UpdateAuthInstance.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_AUTH_INSTANCE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            debugError("UpdateAuthInstance.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_AUTH_INSTANCE", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    /**
     * Post-process any attributes specified for the module instance (either via data file or on the command line) to
     * resolve any file references. Any attribute can be specified using a -file suffix on the attribute name. This
     * will cause the value to be treated as a file name, and the associated file to be read in (in the platform
     * default encoding) and used as the attribute value. The attribute will be renamed to remove the -file suffix
     * during this process.
     *
     * @param attrs the raw attributes read from the command line and/or data file.
     * @return the processed attributes with all file references resolved.
     * @throws CLIException if a referenced file cannot be read or if an attribute is specified both normally and using
     * a -file reference.
     */
    private Map<String, Set<String>> processFileAttributes(Map<String, Set<String>> attrs) throws CLIException {
        Map<String, Set<String>> result = attrs;
        if (attrs != null) {
            result = new LinkedHashMap<String, Set<String>>(attrs.size());

            for (Map.Entry<String, Set<String>> attr : attrs.entrySet()) {
                String key = attr.getKey();
                Set<String> values = attr.getValue();

                if (key != null && key.endsWith(FILE_REFERENCE_SUFFIX)) {
                    key = key.substring(0, key.length() - FILE_REFERENCE_SUFFIX.length());

                    if (attrs.containsKey(key)) {
                        throw new CLIException("Cannot specify both normal and " + FILE_REFERENCE_SUFFIX
                                + " attribute: " + key, ExitCodes.DUPLICATED_OPTION);
                    }

                    if (values != null) {
                        Set<String> newValues = new LinkedHashSet<String>(values.size());
                        for (String value : values) {
                            newValues.add(CLIUtil.getFileContent(getCommandManager(), value));
                        }
                        values = newValues;
                    }
                }

                result.put(key, values);
            }

        }
        return result;
    }
}
