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
 * $Id: RealmTest.java,v 1.8 2008/06/25 05:44:18 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.cli.realm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.DevNullOutputWriter;
import com.sun.identity.cli.IArgument;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.shared.test.CollectionUtils;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.test.common.TestBase;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This is to test the realm related sub commands.
 */
public class RealmTest extends TestBase{
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();

    /**
     * Creates a new instance of <code>RealmTest</code>
     */
    public RealmTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     */
    @BeforeTest(groups = {"cli-realm"})
    public void suiteSetup()
        throws CLIException {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "amadm");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.AccessManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }
    
    @Parameters ({"realm"})
    @BeforeTest(groups = {"cli-realm", "create-realm"})
    public void createRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("createRealm", param);
        String[] args = {
            "create-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        String parentRealm = RealmUtils.getParentRealm(realm);
        String realmName = RealmUtils.getChildRealm(realm);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), parentRealm);
        Set names = ocm.getSubOrganizationNames(realmName, true);
        assert (names.size() == 1);
        String name = (String)names.iterator().next();
        assert name.equals(realmName);
        exiting("createRealm");
    }

    @Parameters ({"parent-realm"})
    @Test(groups = {"cli-realm", "list-realms"})
    public void listRealms(String parentRealm)
        throws CLIException, SMSException {
        String[] param = {parentRealm};
        entering("listRealms", param);
        String[] args = {
            "list-realms",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.FILTER,
            "*",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            parentRealm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listRealms");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-realm", "list-realm-assignable-svcs"})
    public void getAssignableServicesInRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("getAssignableServicesInRealm", param);
        String[] args = {
            "list-realm-assignable-svcs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAssignableServicesInRealm");
    }
 
    @Parameters ({"realm"})
    @Test(groups = {"cli-realm", "show-realm-svcs"})
    public void getAssignedServicesInRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("getAssignedServicesInRealm", param);
        String[] args = {
            "show-realm-svcs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAssignedServicesInRealm");
    }
    
    @Parameters ({"realm", "service-name", "attribute-value"})
    @Test(groups = {"cli-realm", "add-svc-realm"})
    public void assignedServiceToRealm(
        String realm,
        String serviceName,
        String attributeValue
    ) throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, serviceName, attributeValue};
        entering("assignedServiceToRealm", param);
        String[] args = {
            "add-svc-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            attributeValue
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        AMIdentityRepository amir = new AMIdentityRepository(
            getAdminSSOToken(), realm);
        AMIdentity ai = amir.getRealmIdentity();
        ai.getServiceAttributes(serviceName);
        exiting("assignedServiceToRealm");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-realm", "get-realm"})
    public void getRealmAttributeValues(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("getRealmAttributeValues", param);
        String[] args = {
            "get-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "sunIdentityRepositoryService"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getRealmAttributeValues");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-realm", "set-realm-attrs"},
        dependsOnMethods = {"removeRealmAttribute"})
    public void setRealmAttributeValues(String realm)
        throws CLIException, SMSException, SSOException {
        String[] param = {realm};
        entering("setRealmAttributeValues", param);
        String[] args = {
            "set-realm-attrs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "sunIdentityRepositoryService",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "sunOrganizationStatus=Active"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), realm);
        Map values = ocm.getAttributes("sunIdentityRepositoryService");
        Set attrValues = (Set)values.get("sunOrganizationStatus");
        assert attrValues.contains("Active");
        exiting("setRealmAttributeValues");
    }  
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-realm", "delete-realm-attr"})
    public void removeRealmAttribute(String realm)
        throws CLIException, SMSException, SSOException {
        String[] param = {realm};
        entering("removeRealmAttribute", param);
        String[] args = {
            "delete-realm-attr",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "sunIdentityRepositoryService",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_NAME,
            "sunOrganizationStatus"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), realm);
        Map values = ocm.getAttributes("sunIdentityRepositoryService");
        Set attrValues = (Set)values.get("sunOrganizationStatus");
        assert (attrValues == null);
        exiting("removeRealmAttribute");
    }  
    
    @Test(groups = {"cli-realm", "create-policies"})
    public void createPolicy()
        throws CLIException, PolicyException, SSOException {
        entering("createPolicy", null);
        String[] args = {
            "create-policies",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.XML_FILE,
            "mock/cli/createpolicy.xml"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        PolicyManager pm = new PolicyManager(getAdminSSOToken(), "/");
        Policy p = pm.getPolicy("clipolicy");
        assert (p != null);
        exiting("createPolicy");    
    }
    
    @Test(groups = {"cli-realm", "list-policies"},
        dependsOnMethods = {"createPolicy"})
    public void getPolicy()
        throws CLIException, PolicyException, SSOException {
        entering("getPolicy", null);
        String[] args = {
            "list-policies",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getPolicy");        
    }

    @Test(groups = {"cli-realm", "delete-policies"},
        dependsOnMethods = {"getPolicy"})
    public void deletePolicy()
        throws CLIException, PolicyException, SSOException {
        entering("deletePolicy", null);
        String[] args = {
            "delete-policies",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/",
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                RealmDeletePolicy.ARGUMENT_POLICY_NAMES,
            "clipolicy"
        };

        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        PolicyManager pm = new PolicyManager(adminSSOToken, "/");
        try {
            Policy p = pm.getPolicy("clipolicy");
            assert (p == null);
        } catch (NameNotFoundException e) {
            // do nothing
        }

        exiting("deletePolicy");        
    }
    
    @Parameters ({"realm", "service-name"})
    @Test(groups = {"cli-realm", "services", "show-realm-svc-attrs"}, 
        dependsOnMethods = {"assignedServiceToRealm"})
    public void getServiceAttribute(String realm, String serviceName)
        throws CLIException {
        String[] param = {realm};
        entering("getServiceAttribute", param);
        String[] args = {
            "get-realm-svc-attrs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getServiceAttribute");
    }

    
    @Parameters ({"realm", "service-name", "modify-attribute-value"})
    @Test(groups = {"cli-realm", "services", "set-svc-attrs"}, 
        dependsOnMethods = {"assignedServiceToRealm"})
    public void setServiceAttribute(
        String realm,
        String serviceName,
        String attributeValue
    ) throws CLIException, IdRepoException, SSOException {        
        String[] param = {realm};
        entering("setServiceAttribute", param);
        String[] args = {
            "set-svc-attrs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            attributeValue
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        AMIdentityRepository amir = new AMIdentityRepository(
            getAdminSSOToken(), realm);
        AMIdentity ai = amir.getRealmIdentity();
        Map map = ai.getServiceAttributes(serviceName);
        Map<String, Set<String>> orig = 
            CollectionUtils.parseStringToMap(attributeValue);
        
        String key = orig.keySet().iterator().next();
        String value = orig.get(key).iterator().next();
        
        Set resultSet = (Set)map.get(key);
        String result = (String)resultSet.iterator().next();
        
        assert (result.equals(value));
        exiting("setServiceAttribute");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-realm", "add-realm-attrs"}, 
        dependsOnMethods = {"assignedServiceToRealm"})
    public void addRealmAttribute(String realm)
        throws CLIException, IdRepoException, SMSException, SSOException {        
        String[] param = {realm};
        entering("addRealmAttribute", param);
        String[] args = {
            "set-realm-attrs",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "sunIdentityRepositoryService",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "sunOrganizationAliases=dummy"
        };
        
        SSOToken adminSSOToken = getAdminSSOToken();
        CLIRequest req = new CLIRequest(null, args, adminSSOToken);
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        OrganizationConfigManager orgMgr = new OrganizationConfigManager(
            adminSSOToken, realm);
        Map map = orgMgr.getAttributes("sunIdentityRepositoryService");
        Set values = (Set)map.get("sunOrganizationAliases");
        assert(values.contains("dummy"));
        values.remove("dummy");
        orgMgr.setAttributes("sunIdentityRepositoryService", map);
        exiting("addRealmAttribute");
    }

    @Parameters ({"realm", "service-name", "attribute-value"})
    @Test(groups = {"cli-realm", "remove-svc-realm"},
        dependsOnGroups = {"services"})
    public void unassignServiceFromRealm(
        String realm,
        String serviceName,
        String attributeValue
    ) throws CLIException, IdRepoException, SSOException {
        String[] param = {realm};
        entering("unassignServiceFromRealm", param);
        String[] args = {
            "remove-svc-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        AMIdentityRepository amir = new AMIdentityRepository(
            getAdminSSOToken(), realm);
        AMIdentity ai = amir.getRealmIdentity();
        Map map = ai.getServiceAttributes(serviceName);
        Map orig = CollectionUtils.parseStringToMap(attributeValue);
        assert !map.equals(orig);
        exiting("unassignServiceFromRealm");
    }
    
    @Parameters ({"realm"})
    @AfterTest(groups = {"cli-realm", "delete-realm"})
    public void deleteRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("deleteRealm", param);
        String[] args = {
            "delete-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        String parentRealm = RealmUtils.getParentRealm(realm);
        String realmName = RealmUtils.getChildRealm(realm);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), parentRealm);
        Set results = ocm.getSubOrganizationNames(realmName, false);
        assert (results.isEmpty());
        exiting("deleteRealm");
    }
}
