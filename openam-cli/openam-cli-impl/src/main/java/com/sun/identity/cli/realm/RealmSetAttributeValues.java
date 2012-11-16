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
 * $Id: RealmSetAttributeValues.java,v 1.7 2008/08/26 21:13:15 veiming Exp $
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Set attribute values of a realm.
 */
public class RealmSetAttributeValues extends AuthenticatedCommand {
    private static final String OPT_APPEND = "append";
    
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);
        boolean bAppend = isOptionSet(OPT_APPEND);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        Map attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);

        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, realm);
            if (bAppend) {
                for (Iterator i = attributeValues.keySet().iterator();
                    i.hasNext();
                ) {
                    String attributeName = (String)i.next();
                    String[] params = {realm, serviceName, attributeName};
                    Set values = (Set)attributeValues.get(attributeName);
                    
                    writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                        "ATTEMPT_ADD_ATTR_VALUES_REALM", params);
                    ocm.addAttributeValues(serviceName, attributeName, values);
                    writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                        "SUCCEED_ADD_ATTR_VALUES_REALM", params);
                    outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("add-attribute-values-realm-succeed"),
                        (Object[])params));
                }
            } else {
                String[] params = {realm, serviceName};
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_SET_ATTR_VALUES_REALM", params);
                ocm.setAttributes(serviceName, attributeValues);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_SET_ATTR_VALUES_REALM", params);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("set-attribute-values-realm-succeed"),
                    (Object[])params));
            }
        } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmSetAttributeValues.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_ATTR_VALUES_REALM", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}

