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
 * $Id: ListXACML.java,v 1.4 2010/01/10 06:39:42 dillidorai Exp $
 *
 * Portions Copyrighted 2011-2014 ForgeRock AS
 */
package com.sun.identity.cli.entitlement;


import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.entitlement.xacml3.SearchFilterFactory;
import com.sun.identity.entitlement.xacml3.XACMLExportImport;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.entitlement.xacml3.XACMLReaderWriter;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.entitlement.xacml3.validation.PrivilegeValidator;
import com.sun.identity.entitlement.xacml3.validation.RealmValidator;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;

import javax.security.auth.Subject;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Gets policies in a realm.
 * @author dillidorai
 */
public class ListXACML extends AuthenticatedCommand {

    private static final String ARGUMENT_POLICY_NAMES = "policynames";
    private SSOToken adminSSOToken;
    private Subject adminSubject;
    private String realm;
    private boolean getPolicyNamesOnly;
    private List filters;
    private String outfile;
    private IOutput outputWriter;
    private final SearchFilterFactory searchFilterFactory = new SearchFilterFactory();

    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        // FIXME: change to use entitlementService.xacmlPrivilegEnabled()
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        if(!ec.migratedToEntitlementService()) {
            String[] args = {realm, "ANY",
                    "list-xacml not supported in  legacy policy mode"};
            debugError("ListXACML.handleRequest(): "
                    + "list-xacml not supported in  legacy policy mode");
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_POLICY_IN_REALM",
                args);
            throw new CLIException(
                getResourceString(
                    "list-xacml-not-supported-in-legacy-policy-mode"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED,
                "list-xacml");
        }

        adminSSOToken = getAdminSSOToken();
        adminSubject = SubjectUtils.createSubject(adminSSOToken);
        realm = getStringOptionValue(IArgument.REALM_NAME);
        getPolicyNamesOnly = isOptionSet("namesonly");
        filters = (List)rc.getOption(ARGUMENT_POLICY_NAMES);
        outfile = getStringOptionValue(IArgument.OUTPUT_FILE);
        outputWriter = getOutputWriter();

        if (getPolicyNamesOnly) {
            getPolicyNames();
        } else {
            getPolicies();
        }

    }

    /**
     * Convert the String filter parameters into SearchFilter instances.
     *
     * @param filters Non null but possibly empty.
     * @return Empty if no strings provided.
     *
     * @throws EntitlementException If there was an unexpected error parsing the filter strings.
     */
    private Set<SearchFilter> getFilters(List<String> filters)
        throws EntitlementException {
        if ((filters == null) || filters.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        Set<SearchFilter> results = new HashSet<SearchFilter>();
        for (String filter : filters) {
            results.add(searchFilterFactory.getFilter(filter));
        }
        return results;
    }

    /**
     * Indicates the names of the Privileges that match both the Realm and Search Filters
     * provided.
     *
     * @throws CLIException If there was an unexpected error.
     */
    private void getPolicyNames() throws CLIException {
        String currentPrivilegeName = null;
        try {
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, adminSubject);
            ReferralPrivilegeManager rpm = new ReferralPrivilegeManager(realm, adminSubject);

            String[] parameters = new String[1];
            parameters[0] = realm;

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_TO_GET_POLICY_NAMES_IN_REALM", parameters);

            Set<SearchFilter> filterSet = getFilters(filters);
            Set<String> privilegeNames = pm.searchNames(filterSet);

            Set<String> referralPrivilegeNames = rpm.searchNames(
                    filterSet);
            if (referralPrivilegeNames != null & !referralPrivilegeNames.isEmpty()) {
                if (privilegeNames == null) {
                    privilegeNames = new HashSet<String>();
                }
                privilegeNames.addAll(referralPrivilegeNames);
            }

            if ((privilegeNames != null) && !privilegeNames.isEmpty()) {
                FileOutputStream fout = null;
                PrintWriter pwout = null;

                if (outfile != null) {
                    try {
                        fout = new FileOutputStream(outfile, true);
                        pwout = new PrintWriter(fout, true);
                    } catch (FileNotFoundException e) {
                        debugError("ListXACML.handleXACMLPolicyRequest", e);
                        try {
                            if (fout != null) {
                                fout.close();
                            }
                        } catch (IOException ex) {
                            //do nothing
                        }
                        throw new CLIException(e, ExitCodes.IO_EXCEPTION);
                    } catch (SecurityException e) {
                        debugError("ListXACML.handleXACMLPolicyRequest", e);
                        try {
                            if (fout != null) {
                                fout.close();
                            }
                        } catch (IOException ex) {
                            //do nothing
                        }
                        throw new CLIException(e, ExitCodes.IO_EXCEPTION);
                    }
                }

                String[] params = new String[2];
                params[0] = realm;

                StringBuilder buff = new StringBuilder();
                for (Iterator i = privilegeNames.iterator(); i.hasNext(); ) {
                    currentPrivilegeName = (String)i.next();
                    buff.append(currentPrivilegeName).append("\n");
                }
                if (pwout != null) {
                    pwout.write(buff.toString());
                } else {
                    outputWriter.printlnMessage(buff.toString());
                }

                if (pwout != null) {
                    try {
                        pwout.close();
                        fout.close();
                    } catch (IOException e) {
                        //do nothing
                    }
                }
            } else {
                String[] arg = {realm};
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-policy-names-in-realm-no-policies"),
                    (Object[])arg));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "GOT_POLICY_NAMES_IN_REALM", parameters);
            String[] arg = {realm};
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("get-policy-names-in-realm-succeed"),
                (Object[])arg));

        } catch (EntitlementException e) {
            String[] args = {realm, currentPrivilegeName, e.getMessage()};
            debugError("ListXACML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_POLICY_NAMES_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    /**
     * Uses the Realm and Search Filters to identify all Privileges in the Entitlement
     * framework to export.
     *
     * @throws CLIException If there was an unexpected error.
     */
    private void getPolicies() throws CLIException {
        FileOutputStream fout = null;
        PrintWriter pwout = null;

        if (outfile != null) {
            try {
                fout = new FileOutputStream(outfile, true);
                pwout = new PrintWriter(fout, true);
            } catch (FileNotFoundException e) {
                debugError("ListXACML.handleXACMLPolicyRequest", e);
                try {
                    if (fout != null) {
                        fout.close();
                    }
                } catch (IOException ex) {
                    //do nothing
                }
                throw new CLIException(e, ExitCodes.IO_EXCEPTION);
            } catch (SecurityException e) {
                debugError("ListXACML.handleXACMLPolicyRequest", e);
                try {
                    if (fout != null) {
                        fout.close();
                    }
                } catch (IOException ex) {
                    //do nothing
                }
                throw new CLIException(e, ExitCodes.IO_EXCEPTION);
            }
        }

        PolicySet policySet = null;
        try {
            PrivilegeValidator privilegeValidator = new PrivilegeValidator(
                    new RealmValidator(new OrganizationConfigManager(adminSSOToken, "/")));
            XACMLExportImport importExport = new XACMLExportImport(
                    new XACMLExportImport.PrivilegeManagerFactory(),
                    new XACMLExportImport.ReferralPrivilegeManagerFactory(),
                    new XACMLReaderWriter(),
                    privilegeValidator,
                    new SearchFilterFactory(),
                    PrivilegeManager.debug);

            policySet = importExport.exportXACML(realm, adminSubject, filters);
        } catch (EntitlementException e) {
            String[] args = {realm, e.getMessage()};
            debugError("ListXACML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_GET_POLICY_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
            debugError("ListXACML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_GET_POLICY_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if (policySet == null || policySet.getPolicySetOrPolicyOrPolicySetIdReference().isEmpty()) {
            String[] arg = {realm};
            outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-policy-in-realm-no-policies"),
                    (Object[])arg));
        } else {
            try {
                if (pwout != null) {
                    pwout.write(XACMLPrivilegeUtils.toXML(policySet));
                } else {
                    outputWriter.printlnMessage(XACMLPrivilegeUtils.toXML(policySet));
                }
            } catch (EntitlementException e) {
                throw new CLIException(e, ExitCodes.IO_EXCEPTION);
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_GET_POLICY_IN_REALM", new String[]{realm});

            String[] arg = {realm};
            outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-policy-in-realm-succeed"),
                    (Object[])arg));

            if (pwout != null) {
                try {
                    pwout.close();
                    fout.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
    }

}
