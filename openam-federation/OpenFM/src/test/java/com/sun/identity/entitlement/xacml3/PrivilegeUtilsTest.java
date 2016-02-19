/*
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
 * $Id: PrivilegeUtilsTest.java,v 1.1 2009/08/19 05:41:02 veiming Exp $
 *
 * Portions copyright 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement.xacml3;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.entitlement.AndCondition;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.OrCondition;
import com.sun.identity.entitlement.OrSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.StaticAttributes;
import com.sun.identity.entitlement.UserAttributes;
import com.sun.identity.entitlement.UserSubject;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.unittest.UnittestLog;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 *
 * @author dorai
 */
public class PrivilegeUtilsTest {

    private static String SERVICE_NAME = "iPlanetAMWebAgentService";
    private static String PRIVILEGE_NAME = "TestPrivilege";

    @BeforeClass
    public void setup() {
    }

    @AfterClass
    public void cleanup() {
    }

    @Test
    public void testPrivilegeToXACMLPolicy() throws Exception {
        try {
        UnittestLog.logMessage("PrivilegeUtils.testPrivilegeToXACMLPolicy():" +
                " entered");
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        actionValues.put("POST", Boolean.FALSE);
        // The port is required for passing equals  test
        // opensso policy would add default port if port not specified
        String resourceName = "http://www.sun.com:80";
        Entitlement entitlement = new Entitlement(SERVICE_NAME,
                resourceName, actionValues);
        entitlement.setName("ent1");

        String user11 = "id=user11,ou=user," + ServiceManager.getBaseDN();
        String user12 = "id=user12,ou=user," + ServiceManager.getBaseDN();
        UserSubject ua1 = new OpenSSOUserSubject();
        ua1.setID(user11);
        UserSubject ua2 = new OpenSSOUserSubject();
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
        AndCondition ac = new AndCondition(conditions);

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

        Privilege privilege = Privilege.getNewInstance();
        privilege.setName(PRIVILEGE_NAME);
        privilege.setEntitlement(entitlement);
        privilege.setSubject(ua1);
        privilege.setCondition(ipc);
        privilege.setResourceAttributes(ra);
        privilege.setCreatedBy("amadmin");
        privilege.setLastModifiedBy("amadmin");
        privilege.setCreationDate(currentTimeMillis());
        privilege.setLastModifiedDate(currentTimeMillis());

        UnittestLog.logMessage("PrivilegeUtils.testPrivilegeToXACMLPolicy():"
                + "Privilege=" + privilege.toString());
        UnittestLog.logMessage("PrivilegeUtils.testPrivilegeToXACMLPolicy():"
                + "converting to xacml policy");
        // TODO(jtb): not compiling
        String xacmlString = XACMLPrivilegeUtils.toXACML(privilege);
        UnittestLog.logMessage("xacml policy=" + xacmlString);
        } catch (Throwable t) {
            UnittestLog.logError("Throwable:",  t);
            UnittestLog.logMessage("Throwable:" +  t.getMessage());
            t.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PrivilegeUtilsTest");
        PrivilegeUtilsTest put = new PrivilegeUtilsTest();
        put.testPrivilegeToXACMLPolicy();
    }
}
