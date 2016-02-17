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
 * $Id: XACMLExportTest.java,v 1.1 2009/11/25 18:54:08 dillidorai Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;

import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.xacml3.XACMLPrivilegeUtils;
import com.sun.identity.entitlement.xacml3.core.PolicySet;

import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;

import java.io.ByteArrayInputStream;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

/**
 *
 * @author dillidorai
 */
public class XACMLExportTest {

    private static final String APPLICATION_NAME = "iPlanetAMWebAgentService";

    private static final String PRIVILEGE_NAME = "XACMLExportTest";
    private static final String PRIVILEGE_DESC = "Test Description";

    private static final String RESOURCE = "http://www.xacmlexportest.com/*";

    private static final String startIp = "100.100.100.100";
    private static final String endIp = "200.200.200.200";

    private Privilege privilege;
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private UserSubject ua;
    private boolean migrated = true;

    private PrivilegeManager pm;
    private Privilege privilege1;
    private Privilege privilege2;

    private String policySetXML;

    @BeforeClass
    public void setup()
            throws SSOException, IdRepoException, EntitlementException,
            SMSException, InstantiationException, IllegalAccessException {
        //UnittestLog.logMessage("XACMLExportTest.addPrivilege(), setup");

        if (!migrated) {
            throw new RuntimeException("Server not in entitlement mode");
        }
        pm = PrivilegeManager.getInstance("/",
                SubjectUtils.createSubject(adminToken));
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        actionValues.put("POST", Boolean.FALSE);
        Entitlement entitlement = new Entitlement(APPLICATION_NAME,
                RESOURCE, actionValues);
        entitlement.setName("ent1");

        String user11 = "id=user11,ou=user," + ServiceManager.getBaseDN();
        UserSubject ua1 = new OpenSSOUserSubject();
        ua1.setID(user11);
        UserSubject ua2 = new OpenSSOUserSubject();
        String user12 = "id=user12,ou=user," + ServiceManager.getBaseDN();
        ua2.setID(user12);
        Set<EntitlementSubject> subjects = new HashSet<EntitlementSubject>();
        subjects.add(ua1);
        subjects.add(ua2);
        OrSubject os = new OrSubject(subjects);

        Set<EntitlementCondition> conditions = new HashSet<EntitlementCondition>();
        String startIp = "100.100.100.100";
        String endIp = "200.200.200.200";
        IPv4Condition ipc = new IPv4Condition();
        ipc.setStartIpAndEndIp(startIp, endIp);
        conditions.add(ipc);
        OrCondition oc = new OrCondition(conditions);

        /*
        StaticAttributes sa1 = new StaticAttributes();
        Set<String> aValues = new HashSet<String>();
        aValues.add("a10");
        aValues.add("a20");
        sa1.setPropertyName("a");
        sa1.setPropertyValues(aValues);
        sa1.setPResponseProviderName("sa");

        StaticAttributes sa2 = new StaticAttributes();
        Set<String> bValues = new HashSet<String>();
        bValues.add("b10");
        bValues.add("b20");
        sa2.setPropertyName("b");
        sa2.setPropertyValues(bValues);
        sa2.setPResponseProviderName("sa");

        UserAttributes uat1 = new UserAttributes();
        uat1.setPropertyName("email");
        uat1.setPResponseProviderName("ua");

        UserAttributes uat2 = new UserAttributes();
        uat2.setPropertyName("uid");
        uat2.setPResponseProviderName("ua");

        Set<ResourceAttribute> ra = new HashSet<ResourceAttribute>();
        ra.add(sa1);
        ra.add(sa2);
        ra.add(uat1);
        ra.add(uat2);
        */

        privilege1 = Privilege.getNewInstance();
        privilege1.setName(PRIVILEGE_NAME);
        privilege1.setEntitlement(entitlement);
        privilege1.setSubject(ua1);
        privilege1.setCondition(ipc);
        //privilege.setResourceAttributes(ra);

        pm.add(privilege1);

        //UnittestLog.logMessage("XACMLExportTest.setup(), added privilege:" +
                //"Privilege1=" + privilege1.toString());
    }

    @AfterClass
    public void deletePrivilege()
            throws SSOException, IdRepoException, EntitlementException,
            SMSException, InstantiationException, IllegalAccessException {
        //UnittestLog.logMessage("XACMLExportTest.deletePrivilege(), cleanup");
        pm.remove(PRIVILEGE_NAME);
    }

    @Test
    public void testListXACML() throws EntitlementException {
        //UnittestLog.logMessage("XACMLExportTest.testListXACML()");
        Set<Privilege> privileges = new HashSet<Privilege>();
        Privilege privilege = pm.findByName(PRIVILEGE_NAME, adminSubject);
        privileges.add(privilege);
        PolicySet policySet = XACMLPrivilegeUtils.privilegesToPolicySet("/", privileges);
        policySetXML = XACMLPrivilegeUtils.toXML(policySet);
    }

    @Test(dependsOnMethods={"testListXACML"})
    public void testDeleteXACML()
            throws SSOException, IdRepoException, EntitlementException,
            SMSException, InstantiationException, IllegalAccessException {
        //UnittestLog.logMessage("XACMLExportTest.testDeleteXACML()");
        pm.remove(PRIVILEGE_NAME);
    }

    @Test(dependsOnMethods={"testDeleteXACML"})
    public void testCreateXACML() throws Exception {
        //UnittestLog.logMessage("XACMLExportTest.testCreateXACML()");
        //UnittestLog.logMessage("XACMLExportTest.testCreateXML(): policySetXML:"
                //+ policySetXML);
        PolicySet policySet = XACMLPrivilegeUtils.streamToPolicySet(
                new ByteArrayInputStream(policySetXML.getBytes("UTF-8")));
        Set<Privilege> privileges = XACMLPrivilegeUtils.policySetToPrivileges(policySet);
        if (privileges == null | privileges.isEmpty()) {
            throw new Exception("privielges is null");
        }
        Privilege privilege = privileges.iterator().next();
        //UnittestLog.logMessage("XACMLExportTest.testCreateXML(): original priivilege:"
                //+ privilege1.toString());
        //UnittestLog.logMessage("XACMLExportTest.testCreateXML(): recreated priivilege:"
                //+ privilege.toString());
        if (privilege == null) {
            throw new Exception("privielge is null");
        }
        assert privilege.equals(privilege1);
        pm.add(privilege);
    }
}
