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
 * $Id: IdRepoTest.java,v 1.5 2008/06/25 05:44:18 qcheng Exp $
 *
 */

package com.sun.identity.cli.idrepo;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.DevNullOutputWriter;
import com.sun.identity.cli.IArgument;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPrivilege;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.test.common.TestBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This is to test the idrepo related sub commands.
 */
public class IdRepoTest extends TestBase{
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();
    
    private static final String TOP_LEVEL_ADMIN_GROUP = "Top-Level Admin Group";
    private static final String DUMMY_GROUP = "IdRepoDevUnitTestGroup";

    /**
     * Creates a new instance of <code>IdRepoTest</code>
     */
    public IdRepoTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     */
    @BeforeTest(groups = {"cli-idrepo"})
    public void suiteSetup()
        throws CLIException {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "amadm");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.AccessManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }
    
    @Parameters ({"realm", "uid", "attrs"})
    @Test(groups = {"cli-idrepo", "create-identity"})
    public void createIdentity(String realm, String uid, String attrs)
        throws CLIException {
        String[] param = {realm, uid, attrs};
        entering("createIdentity", param);

        List<String> arguments = new ArrayList<String>();
        arguments.add("create-identity");
        arguments.add(CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME);
        arguments.add(realm);
        arguments.add(CLIConstants.PREFIX_ARGUMENT_LONG +
            IdentityCommand.ARGUMENT_ID_TYPE);
        arguments.add("User");
        arguments.add(CLIConstants.PREFIX_ARGUMENT_LONG +
            IdentityCommand.ARGUMENT_ID_NAME);
        arguments.add(uid);
        arguments.add(CLIConstants.PREFIX_ARGUMENT_LONG +
            IArgument.ATTRIBUTE_VALUES);

        StringTokenizer st = new StringTokenizer(attrs);
        while (st.hasMoreTokens()) {
            arguments.add(st.nextToken());
        }

        String[] args = new String[arguments.size()];
        int i = 0;
        for (String s : arguments) {
            args[i++] = s;
        }

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        AMIdentity amid = new AMIdentity(adminSSOToken, uid, IdType.USER,
            realm, null);
        assert (amid != null);
        exiting("createIdentity");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-idrepo", "show-identity-types"})
    public void getIdentityTypes(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("getIdentityTypes", param);
        String[] args = {
            "show-identity-types",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getIdentityTypes");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-idrepo", "show-identity-ops"})
    public void getIdentityOperations(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("getIdentityOperations", param);
        String[] args = {
            "show-identity-ops",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User"};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getIdentityOperations");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "list-identities"},
        dependsOnMethods = {"createIdentity"})
    public void listIdentities(String realm, String uid)
        throws CLIException {
        String[] param = {realm, uid};
        entering("listIdentities", param);
        String[] args = {
            "list-identities",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.FILTER,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listIdentities");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "get-identity"},
        dependsOnMethods = {"createIdentity"})
    public void getAttributes(String realm, String uid)
        throws CLIException {
        String[] param = {realm, uid};
        entering("getAttributes", param);
        String[] args = {
            "get-identity",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAttributes");
    }

//    Duplicate of getIdentityServices?
//    @Parameters ({"realm", "uid"})
//    @Test(groups = {"cli-idrepo", "get-identity-svcs"},
//        dependsOnMethods = {"createIdentity"})
//    public void getServices(String realm, String uid)
//        throws CLIException {
//        String[] param = {realm, uid};
//        entering("getServices", param);
//        String[] args = {
//            "get-identity-svcs",
//            CLIConstants.PREFIX_ARGUMENT_LONG +
//                IdentityCommand.ARGUMENT_ID_TYPE,
//            "User",
//            CLIConstants.PREFIX_ARGUMENT_LONG +
//                IdentityCommand.ARGUMENT_ID_NAME,
//            uid,
//            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
//            realm};
//
//        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
//        cmdManager.addToRequestQueue(req);
//        cmdManager.serviceRequestQueue();
//        exiting("getServices");
//    }
    
    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "list-identity-assignable-svcs"},
        dependsOnMethods = {"createIdentity"})
    public void getAssignableServices(String realm, String uid)
        throws CLIException {
        String[] param = {realm, uid};
        entering("getAssignableServices", param);
        String[] args = {
            "list-identity-assignable-svcs",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAssignableServices");
    }
    
    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "add-svc-identity"},
        dependsOnMethods = {"createIdentity"})
    public void assignServices(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("assignServices", param);
        String[] args = {
            "add-svc-identity",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "iPlanetAMSessionService",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "iplanet-am-session-quota-limit=10"};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue(); 
        
        AMIdentity amid = new AMIdentity(
            adminSSOToken, uid, IdType.USER, realm, null);
        Set services = amid.getAssignedServices();
        assert services.contains("iPlanetAMSessionService");
        exiting("assignServices");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "remove-svc-identity"},
        dependsOnMethods = {"setServiceAttributes", "getServiceAttributes",
            "getAttributes"})
    public void unassignServices(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("unassignServices", param);
        String[] args = {
            "remove-svc-identity",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "iPlanetAMSessionService"};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue(); 
        
        AMIdentity amid = new AMIdentity(
            adminSSOToken, uid, IdType.USER, realm, null);
        Set services = amid.getAssignedServices();
        for (Iterator i = services.iterator(); i.hasNext(); ) {
            String svc = (String)i.next();
            assert (!svc.equals("iPlanetAMSessionService"));
        }
        exiting("unassignServices");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "show-identity-svc-attrs"},
        dependsOnMethods = {"assignServices"})
    public void getServiceAttributes(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("getServiceAttributes", param);
        String[] args = {
            "show-identity-svc-attrs",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "iPlanetAMSessionService"};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue(); 
        exiting("getServiceAttributes");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "set-identity-svc-attrs"},
        dependsOnMethods = {"assignServices"})
    public void setServiceAttributes(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("setServiceAttributes", param);
        String[] args = {
            "set-identity-svc-attrs",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "iPlanetAMSessionService",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "iplanet-am-session-quota-limit=1"
        };

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue(); 
        
        AMIdentity user = new AMIdentity(
            adminSSOToken, uid, IdType.USER, realm, null);
        Map attrValues = user.getServiceAttributes("iPlanetAMSessionService");
        Set values = (Set)attrValues.get("iplanet-am-session-quota-limit");
        String val = (String)values.iterator().next();
        assert val.equals("1");
        exiting("setServiceAttributes");
    }
    
    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "set-svc-attrs"},
        dependsOnMethods = {"assignServices"})
    public void setAttributes(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("setAttributes", param);
        String[] args = {
            "set-identity-attrs",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "cn=commonname"
        };

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue(); 
        
        AMIdentity user = new AMIdentity(
            adminSSOToken, uid, IdType.USER, realm, null);
        Map attrValues = user.getServiceAttributes("iPlanetAMUserService");
        Set values = (Set)attrValues.get("cn");
        String val = (String)values.iterator().next();
        assert val.equals("commonname");
        exiting("setAttributes");
    }
 
    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "get-identity-svcs"},
        dependsOnMethods = {"createIdentity"})
    public void getIdentityServices(String realm, String uid)
        throws CLIException {
        String[] param = {realm, uid};
        entering("getIdentityServices", param);
        String[] args = {
            "get-identity-svcs",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getIdentityServices");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "show-members"},
        dependsOnMethods = {"createIdentity"})
    public void getMembers(String realm, String uid)
        throws CLIException {
        String[] param = {realm, uid};
        entering("getMembers", param);
        String[] args = {
            "show-members",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "Group",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            DUMMY_GROUP,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                GetMembers.ARGUMENT_MEMBERSHIP_IDTYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getMembers");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "show-memberships"},
        dependsOnMethods = {"createIdentity"})
    public void getMemberships(String realm, String uid)
        throws CLIException {
        String[] param = {realm, uid};
        entering("getMemberships", param);
        String[] args = {
            "show-memberships",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                GetMemberships.ARGUMENT_MEMBERSHIP_IDTYPE,
            "Group",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getMemberships");
    }

    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "add-members"},
        dependsOnMethods = {"createIdentity"})
    public void addMember(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("addMember", param);
        createDummyGroup(realm);
        String[] args = {
            "add-member",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "Group",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            DUMMY_GROUP,
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                IdentityCommand.ARGUMENT_MEMBER_IDNAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_MEMBER_IDTYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        AMIdentity group = new AMIdentity(
            adminSSOToken, DUMMY_GROUP, IdType.GROUP, realm, null);
        AMIdentity user = new AMIdentity(
            adminSSOToken, uid, IdType.USER, realm, null);
        Set members = group.getMembers(IdType.USER);
        assert (members.contains(user));
        exiting("addMember");
    }
    
    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "add-members"},
        dependsOnMethods = {"addMember"})
    public void removeMember(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("removeMember", param);
        String[] args = {
            "remove-member",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "Group",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            DUMMY_GROUP,
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                IdentityCommand.ARGUMENT_MEMBER_IDNAME,
            uid,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_MEMBER_IDTYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        AMIdentity group = new AMIdentity(
            adminSSOToken, DUMMY_GROUP, IdType.GROUP, realm, null);
        Set members = group.getMembers(IdType.USER);
        for (Iterator i = members.iterator(); i.hasNext(); ) {
            AMIdentity x = (AMIdentity)i.next();
            assert (!x.getName().equals(uid));
        }
        deleteDummyGroup(realm);
        exiting("removeMember");
    }
    
    @Parameters ({"realm", "uid"})
    @Test(groups = {"cli-idrepo", "show-privileges"},
        dependsOnMethods = {"createIdentity"})
    public void getPrivileges(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("getPrivileges", param);
        String[] args = {
            "show-privileges",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "Group",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            TOP_LEVEL_ADMIN_GROUP,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getPrivileges");
    }
    /*
    @Parameters ({"realm"})
    @Test(groups = {"cli-idrepo", "add-privileges", "issue245"},
        dependsOnMethods = {"createIdentity"})
    public void addPrivileges(String realm)
        throws CLIException, IdRepoException, DelegationException, SSOException{
        createDummyGroup(realm);
        String[] param = {realm};
        entering("addPrivileges", param);
        String[] args = {
            "add-privileges",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "Group",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            DUMMY_Group,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.PRIVILEGES,
            "PolicyAdmin",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        DelegationManager mgr = new DelegationManager(
            adminSSOToken, realm);
        AMIdentity amid = new AMIdentity(
            adminSSOToken, DUMMY_GROUP, IdType.GROUP, realm, null);
        Set results = mgr.getPrivileges(amid.getUniversalId());
        boolean found = false;
        for (Iterator i = results.iterator(); i.hasNext() && !found; ) {
            DelegationPrivilege p = (DelegationPrivilege)i.next();
            found = p.getName().equals("PolicyAdmin");
        }
        assert found;
        exiting("addPrivileges");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-idrepo", "remove-privileges"},
        dependsOnMethods = {"addPrivileges"})
    public void removePrivileges(String realm)
        throws CLIException, IdRepoException, DelegationException, SSOException{
        String[] param = {realm};
        entering("removePrivileges", param);
        String[] args = {
            "remove-privileges",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "Group",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAME,
            DUMMY_GROUP,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.PRIVILEGES,
            "PolicyAdmin",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        DelegationManager mgr = new DelegationManager(
            adminSSOToken, realm);
        AMIdentity amid = new AMIdentity(
            adminSSOToken, DUMMY_GROUP, IdType.GROUP, realm, null);
        Set results = mgr.getPrivileges(amid.getUniversalId());
        boolean found = false;
        for (Iterator i = results.iterator(); i.hasNext() && !found; ) {
            DelegationPrivilege p = (DelegationPrivilege)i.next();
            found = p.getName().equals("PolicyAdmin");
        }
        deleteDummyGroup(realm);
        assert !found;
        exiting("removePrivileges");
    }
    */
    
    private AMIdentity createDummyGroup(String realm)
        throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
            getAdminSSOToken(), realm);
        return amir.createIdentity(IdType.GROUP, DUMMY_GROUP, 
            Collections.EMPTY_MAP);
    }
    
    private void deleteDummyGroup(String realm)
        throws IdRepoException, SSOException {
        SSOToken adminSSOToken = getAdminSSOToken();
        AMIdentityRepository amir = new AMIdentityRepository(
            adminSSOToken, realm);
        AMIdentity amid = new AMIdentity(
            adminSSOToken, DUMMY_GROUP, IdType.GROUP, realm, null);
        Set<AMIdentity> set = new HashSet<AMIdentity>(2);
        set.add(amid);
        amir.deleteIdentities(set);
    }

    
    @Parameters ({"realm", "uid"})
    @AfterTest(groups = {"cli-idrepo", "delete-identities"})
    public void deleteIdentity(String realm, String uid)
        throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, uid};
        entering("deleteRealm", param);
        String[] args = {
            "delete-identities",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_TYPE,
            "User",
            CLIConstants.PREFIX_ARGUMENT_LONG +
                IdentityCommand.ARGUMENT_ID_NAMES,
            uid};

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        AMIdentityRepository amir = new AMIdentityRepository(
            adminSSOToken, realm);
        IdSearchControl isCtl = new IdSearchControl();
        IdSearchResults isr = amir.searchIdentities(IdType.USER, uid, isCtl);
        Set results = isr.getSearchResults();
        assert (results.isEmpty());
        exiting("deleteIdentities");
    }
}
