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
 * $Id: ListPolicyNamesTest.java,v 1.1 2009/12/18 07:13:28 dillidorai Exp $
 */

package com.sun.identity.cli.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.realm.RealmGetPolicy;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;

import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;

import com.sun.identity.security.AdminTokenAction;

import com.sun.identity.sm.ServiceManager;

import java.security.AccessController;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class ListPolicyNamesTest extends CLITestImpl {

    private static String APPL_NAME = "iPlanetAMWebAgentService";

    private static final String POLICY_NAME1 = "ListPolicyNamesTest1";
    private static final String POLICY_NAME2 = "ListPolicyNamesTest2";

    private PrivilegeManager pm;

    public void beforeClass() throws EntitlementException {
        pm = PrivilegeManager.getInstance("/",
                SubjectUtils.createSubject(adminToken));
        createPrivilege(POLICY_NAME1);
        createPrivilege(POLICY_NAME2);
    }

    @AfterClass
    public void cleanup() throws EntitlementException {
        pm.removePrivilege(POLICY_NAME1);
        pm.removePrivilege(POLICY_NAME2);
    }

    @Test
    public void listPolicyNames() throws CLIException {
        String[] args = new String[6];
        args[0] = "list-policies";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            RealmGetPolicy.ARGUMENT_POLICY_NAMES;
        args[4] = "*";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.NAMES_ONLY;

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }

    @Test
    public void listXACMLPolicyNames() throws CLIException {
        String[] args = new String[6];
        args[0] = "list-xacml";
        args[1] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.REALM_NAME;
        args[2] = "/";
        args[3] = CLIConstants.PREFIX_ARGUMENT_LONG +
            RealmGetPolicy.ARGUMENT_POLICY_NAMES;
        args[4] = "*";
        args[5] = CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.NAMES_ONLY;

        CLIRequest req = new CLIRequest(null, args, adminToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
    }

    private void createPrivilege(String name) throws EntitlementException {
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        actionValues.put("POST", Boolean.FALSE);
        String resourceName = "http://www.listpolicynamestest.com:80";
        Entitlement entitlement = new Entitlement(APPL_NAME,
                resourceName, actionValues);
        entitlement.setName("ent1");

        String user = "id=demo,ou=user," + ServiceManager.getBaseDN();
        OpenSSOUserSubject usersubj = new OpenSSOUserSubject();
        usersubj.setID(user);

        Privilege priv = Privilege.getNewInstance();
        priv.setName(name);
        priv.setEntitlement(entitlement);
        priv.setSubject(usersubj);
        pm.addPrivilege(priv);
    }

}
