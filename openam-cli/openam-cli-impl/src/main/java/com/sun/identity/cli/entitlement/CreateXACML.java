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
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */

package com.sun.identity.cli.entitlement;

import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.xacml3.SearchFilterFactory;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.entitlement.xacml3.XACMLExportImport.ImportStep;
import com.sun.identity.entitlement.xacml3.XACMLReaderWriter;
import com.sun.identity.entitlement.xacml3.validation.PrivilegeValidator;
import com.sun.identity.entitlement.xacml3.validation.RealmValidator;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import org.forgerock.openam.utils.IOUtils;

import javax.security.auth.Subject;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.List;

import static com.sun.identity.cli.LogWriter.LOG_ACCESS;
import static com.sun.identity.cli.LogWriter.LOG_ERROR;
import static java.util.logging.Level.INFO;

/**
 * Converts access policies read from XACML XML into Entitlement Framework Privileges
 * and then imports these into the specified realm.
 */
public class CreateXACML extends AuthenticatedCommand {

    /**
     * Services the command line request to import XACML.
     *
     * Required Arguments:
     * realm - Defines the realm the Policies will be imported into.
     * xmlfile - References the XACML file from which the Policies should be read.
     *
     * Optional Arguments:
     * dryrun - Optional flag indicates that, rather than carrying out the import,
     *          a report of anticipated affects should be generated.
     * outfile - Optional reference to a file for dryrun report to be written, if not provided
     *         the dryrun report is written directly to stdout.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        SSOToken adminSSOToken = getAdminSSOToken();
        Subject adminSubject = SubjectUtils.createSubject(adminSSOToken);
        String realm = getStringOptionValue(IArgument.REALM_NAME);

        ensureEntitlementServiceActive(adminSubject, realm);

        InputStream xacmlInputStream = getXacmlInputStream(realm);

        logStart(realm);

        List<ImportStep> importSteps;
        try {
            PrivilegeValidator privilegeValidator = new PrivilegeValidator(
                    new RealmValidator(new OrganizationConfigManager(adminSSOToken, "/")));
            XACMLExportImport xacmlExportImport = new XACMLExportImport(
                    new XACMLExportImport.PrivilegeManagerFactory(),
                    new XACMLExportImport.ReferralPrivilegeManagerFactory(),
                    new XACMLReaderWriter(),
                    privilegeValidator,
                    new SearchFilterFactory(),
                    PrivilegeManager.debug);

            importSteps = xacmlExportImport.importXacml(realm, xacmlInputStream, adminSubject, isDryRun());
        } catch (EntitlementException e) {
            debugError("CreateXACML.handleRequest", e);
            logException(realm, e);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            debugError("CreateXACML.handleRequest", e);
            logException(realm, e);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if (importSteps.isEmpty()) {

            String message = getResourceString("no-policies-provided");
            logNothingToImport(realm, message);
            getOutputWriter().printlnMessage(message);

        } else {

            logSuccess(realm);
            if (isDryRun()) {
                outputDryRunResults(importSteps);
            } else {
                getOutputWriter().printlnMessage(MessageFormat.format(
                        getResourceString("create-policy-in-realm-succeed"), realm));
            }

        }
    }

    private void logStart(String realm) throws CLIException {
        if (isDryRun()) {
            writeLog(LOG_ACCESS, INFO, "ATTEMPT_TO_GET_POLICY_NAMES_IN_REALM", new String[]{realm});
        } else {
            writeLog(LOG_ACCESS, INFO, "ATTEMPT_CREATE_POLICY_IN_REALM", new String[]{realm});
        }
    }

    private void logException(String realm, Exception e) throws CLIException {
        if (isDryRun()) {
            writeLog(LOG_ERROR, INFO, "FAILED_GET_POLICY_NAMES_IN_REALM", new String[]{realm});
        }  else {
            writeLog(LOG_ERROR, INFO, "FAILED_CREATE_POLICY_IN_REALM", new String[]{realm, e.getMessage()});
        }
    }

    private void logNothingToImport(String realm, String message) throws CLIException {
        writeLog(LOG_ERROR, INFO, "FAILED_CREATE_POLICY_IN_REALM", new String[]{realm, message});
    }

    private void logSuccess(String realm) throws CLIException {
        if (isDryRun()) {
            writeLog(LOG_ACCESS, INFO, "GOT_POLICY_NAMES_IN_REALM", new String[]{realm});
        } else {
            writeLog(LOG_ACCESS, INFO, "SUCCEED_CREATE_POLICY_IN_REALM", new String[]{realm});
        }
    }

    private void ensureEntitlementServiceActive(Subject adminSubject, String realm) throws CLIException {
        // FIXME: change to use entitlementService.xacmlPrivilegEnabled()
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(adminSubject, "/");
        if (!ec.migratedToEntitlementService()) {
            String[] args = {realm, "ANY", "create-xacml not supported in  legacy policy mode"};
            debugError("CreateXACML.handleRequest(): create-xacml not supported in  legacy policy mode");
            writeLog(LOG_ERROR, INFO, "FAILED_CREATE_POLICY_IN_REALM", args);
            throw new CLIException(getResourceString("create-xacml-not-supported-in-legacy-policy-mode"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED,
                    "create-xacml");
        }
    }

    private InputStream getXacmlInputStream(String realm) throws CLIException {
        InputStream inputStream;

        String datafile = getStringOptionValue(IArgument.XML_FILE);
        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();

        if ((url != null) && (url.length() > 0)) {
            inputStream = new ByteArrayInputStream(datafile.getBytes());
        } else {
            try {
                inputStream = new FileInputStream(datafile);
            } catch (FileNotFoundException e) {
                debugError("CreateXACML.handleRequest", e);
                logException(realm, e);
                throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }

        return inputStream;
    }

    private void outputDryRunResults(List<ImportStep> importSteps) throws CLIException {

        StringBuffer sb = new StringBuffer();
        for (ImportStep step : importSteps) {
            sb.append(MessageFormat.format(
                    "{0} {1}\n", step.getDiffStatus().getCode(), step.getPrivilege().getName()));
        }

        if (isOutfileSet()) {
            writeToOutputFile(sb.toString());
        } else {
            getOutputWriter().printlnMessage(sb.toString());
        }
    }

    private void writeToOutputFile(String string) throws CLIException {
        FileOutputStream fout = null;
        PrintWriter pwout = null;

        try {
            fout = new FileOutputStream(getOutfileName(), true); // appending to be consistent with ListXACML
            pwout = new PrintWriter(fout, true);
        } catch (FileNotFoundException e) {
            debugError("CreateXACML.writeToOutputFile", e);
            IOUtils.closeIfNotNull(fout);
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } catch (SecurityException e) {
            debugError("CreateXACML.writeToOutputFile", e);
            IOUtils.closeIfNotNull(fout);
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        }

        pwout.write(string);

        IOUtils.closeIfNotNull(pwout);
        IOUtils.closeIfNotNull(fout);
    }

    private boolean isDryRun() {
        return isOptionSet(IArgument.DRY_RUN);
    }

    private boolean isOutfileSet() {
        return isOptionSet(IArgument.OUTPUT_FILE);
    }

    private String getOutfileName() {
        return getStringOptionValue(IArgument.OUTPUT_FILE);
    }
}
