/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CreateXACML.java,v 1.3 2010/01/11 01:21:01 dillidorai Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.entitlement;


import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.xacml3.XACMLImportExport;

import javax.security.auth.Subject;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Creates policy in a realm by importing the XACML provided, converting to the
 * Entitlement Framework Privileges and then importing.
 */
public class CreateXACML extends AuthenticatedCommand {

    private final XACMLImportExport xacmlImportExport = new XACMLImportExport();

    /**
     * Services the command line request to import XACML.
     *
     * Required Arguments:
     * Realm - Defines the realm the Policies will be imported into.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc)
            throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        SSOToken adminSSOToken = getAdminSSOToken();
        Subject adminSubject = SubjectUtils.createSubject(adminSSOToken);
        String realm = getStringOptionValue(IArgument.REALM_NAME);

        // FIXME: change to use entitlementService.xacmlPrivilegEnabled()
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        if(!ec.migratedToEntitlementService()) {
            String[] args = {realm, "ANY", 
                    "create-xacml not supported in  legacy policy mode"};
            debugError("CreateXACML.handleRequest(): "
                    + "create-xacml not supported in  legacy policy mode");
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_CREATE_POLICY_IN_REALM", 
                args);
            throw new CLIException(
                getResourceString( 
                    "create-xacml-not-supported-in-legacy-policy-mode"), 
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED,
                "create-xacml");
        }

        String datafile = getStringOptionValue(IArgument.XML_FILE);
        IOutput outputWriter = getOutputWriter();

        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();
        String[] params = {realm};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_CREATE_POLICY_IN_REALM", params);

        InputStream xacmlSource;
        if ((url != null) && (url.length() > 0)) {
            xacmlSource = new ByteArrayInputStream(datafile.getBytes());
        } else {
            try {
                xacmlSource = new FileInputStream(datafile);
            } catch (FileNotFoundException e) {
                String[] args = {realm, e.getMessage()};
                debugError("CreateXACML.handleRequest", e);
                writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_POLICY_IN_REALM", args);
                throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }

        try {
            if (xacmlImportExport.importXacml(realm, xacmlSource, adminSubject)) {
                writeLog(LogWriter.LOG_ACCESS, Level.INFO, "SUCCEED_CREATE_POLICY_IN_REALM", params);
                outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("create-policy-in-realm-succeed"),
                        (Object[])params));
            } else {
                String[] args = {realm, "ANY", "create-xacml input poliy set is null"};
                writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_POLICY_IN_REALM", args);
                outputWriter.printlnMessage("policy set is null");
            }
        } catch (EntitlementException e) {
            String[] args = {realm, e.getMessage()};
            debugError("CreateXACML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
