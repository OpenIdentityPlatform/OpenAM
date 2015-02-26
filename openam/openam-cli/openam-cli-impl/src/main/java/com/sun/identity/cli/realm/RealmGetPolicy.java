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
 * $Id: RealmGetPolicy.java,v 1.6 2009/12/18 07:13:27 dillidorai Exp $
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Gets policies in a realm.
 */
public class RealmGetPolicy extends AuthenticatedCommand {
    public static final String ARGUMENT_POLICY_NAMES = "policynames";
    private SSOToken adminSSOToken;
    private String realm;
    private List filters;
    private String outfile;
    private boolean getPolicyNamesOnly;
    private IOutput outputWriter;
    
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
        adminSSOToken = getAdminSSOToken();
        realm = getStringOptionValue(IArgument.REALM_NAME);
        filters = (List)rc.getOption(ARGUMENT_POLICY_NAMES);
        outfile = getStringOptionValue(IArgument.OUTPUT_FILE);
        getPolicyNamesOnly = isOptionSet("namesonly");
        outputWriter = getOutputWriter();
        if (getPolicyNamesOnly) {
            getPolicyNames();
        } else {
            getPolicies();
        }

    }

    private void getPolicyNames() throws CLIException {
        try {
            PolicyManager pm = new PolicyManager(adminSSOToken, realm);

            String currentPolicyName;
            String[] parameters = new String[1];
            parameters[0] = realm;

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_TO_GET_POLICY_NAMES_IN_REALM", parameters);
            
            Set policyNames = null;

            if ((filters == null) || filters.isEmpty()) {
                policyNames = pm.getPolicyNames();
            } else {
                policyNames = new HashSet();
                for (Iterator i = filters.iterator(); i.hasNext(); ) {
                    policyNames.addAll(
                        pm.getPolicyNames((String)i.next()));
                }
            }

            if ((policyNames != null) && !policyNames.isEmpty()) {
                StringBuilder buff = new StringBuilder();
                for (Object s : policyNames) {
                    String str = (String)s;
                    buff.append(str).append("\n");
                }
                FileOutputStream fout = null;
                PrintWriter pwout = null;
                
                if (outfile != null) {
                    try {
                        fout = new FileOutputStream(outfile, true);
                        pwout = new PrintWriter(fout, true);
                    } catch (FileNotFoundException e) {
                        debugError("RealmGetPolicy.handleRequest", e);
                        try {
                            if (fout != null) {
                                fout.close();
                            }
                        } catch (IOException ex) {
                            //do nothing
                        }
                        throw new CLIException(e, ExitCodes.IO_EXCEPTION);
                    } catch (SecurityException e) {
                        debugError("RealmGetPolicy.handleRequest", e);
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
                if (pwout != null) {
                    pwout.write(buff.toString());
                    try {
                        pwout.close();
                        fout.close();
                    } catch (IOException e) {
                        //do nothing
                    }
                } else {
                    outputWriter.printlnMessage(buff.toString());
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
        } catch (PolicyException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmGetPolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_POLICY_NAMES_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmGetPolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_POLICY_NAMES_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    
    }
    private void getPolicies() throws CLIException {
        try {
            PolicyManager pm = new PolicyManager(adminSSOToken, realm);

            String currentPolicyName;
            String[] params = new String[2];
            params[0] = realm;

            Set policyNames = null;

            if ((filters == null) || filters.isEmpty()) {
                policyNames = pm.getPolicyNames();
            } else {
                policyNames = new HashSet();
                for (Iterator i = filters.iterator(); i.hasNext(); ) {
                    policyNames.addAll(
                        pm.getPolicyNames((String)i.next()));
                }
            }

            if ((policyNames != null) && !policyNames.isEmpty()) {
                StringBuilder buff = new StringBuilder();
                buff.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n")
                    .append("<!DOCTYPE Policies \n")
                    .append("PUBLIC \"-//OpenSSO Policy Administration DTD//EN\"\n")
                    .append("\"jar://com/sun/identity/policy/policyAdmin.dtd\">\n\n");

                buff.append("<!-- extracted from realm, ")
                    .append(realm)
                    .append(" -->\n<Policies>\n");

                for (Iterator i = policyNames.iterator(); i.hasNext(); ) {
                    currentPolicyName = (String)i.next();
                    params[1] = currentPolicyName;
                    writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                        "ATTEMPT_GET_POLICY_IN_REALM", params);
                    Policy policy = pm.getPolicy(currentPolicyName);
                    buff.append(policy.toXML(false));
                    writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                        "SUCCEED_GET_POLICY_IN_REALM", params);
                }

                buff.append("\n</Policies>\n");

                FileOutputStream fout = null;
                PrintWriter pwout = null;
                
                if (outfile != null) {
                    try {
                        fout = new FileOutputStream(outfile, true);
                        pwout = new PrintWriter(fout, true);
                    } catch (FileNotFoundException e) {
                        debugError("RealmGetPolicy.handleRequest", e);
                        try {
                            if (fout != null) {
                                fout.close();
                            }
                        } catch (IOException ex) {
                            //do nothing
                        }
                        throw new CLIException(e, ExitCodes.IO_EXCEPTION);
                    } catch (SecurityException e) {
                        debugError("RealmGetPolicy.handleRequest", e);
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

                String[] arg = {realm};
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-policy-in-realm-succeed"), 
                    (Object[])arg));

                if (pwout != null) {
                    pwout.write(buff.toString());
                    try {
                        pwout.close();
                        fout.close();
                    } catch (IOException e) {
                        //do nothing
                    }
                } else {
                    outputWriter.printlnMessage(buff.toString());
                }
            } else {
                String[] arg = {realm};
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-policy-in-realm-no-policies"),
                    (Object[])arg));
            }
        } catch (PolicyException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmGetPolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_POLICY_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmGetPolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_POLICY_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    
    }

    
}
