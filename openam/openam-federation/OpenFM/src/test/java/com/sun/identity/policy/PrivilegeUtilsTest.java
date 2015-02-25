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
 * $Id: PrivilegeUtilsTest.java,v 1.1 2009/08/19 05:41:03 veiming Exp $
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.policy;

import com.sun.identity.entitlement.opensso.PrivilegeUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.AndCondition;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.OrSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.StaticAttributes;
import com.sun.identity.entitlement.UserAttributes;
import com.sun.identity.entitlement.UserSubject;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ServiceManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.testng.annotations.Test;

/**
 *
 * @author dillidorai
 */
public class PrivilegeUtilsTest {

    @Test
    public void testPrivilegeToPolicy() throws Exception {
        String BASE_DN = Constants.DEFAULT_ROOT_SUFFIX;
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        actionValues.put("POST", Boolean.TRUE);
        String resourceName = "http://www.sun.com";
        Entitlement entitlement = new Entitlement("iPlanetAMWebAgentService",
                resourceName, actionValues);
        entitlement.setName("ent1");
        String user11 = "id=user11,ou=user," + BASE_DN;
        String user12 = "id=user12,ou=user," + BASE_DN;
        UserSubject us1 = new OpenSSOUserSubject();
        us1.setID(user11);
        UserSubject us2 = new OpenSSOUserSubject();
        us2.setID(user12);
        Set<EntitlementSubject> subjects = new HashSet<EntitlementSubject>();
        subjects.add(us1);
        subjects.add(us2);
        OrSubject os = new OrSubject(subjects);
        IPv4Condition ipc = new IPv4Condition();
        ipc.setStartIpAndEndIp("100.100.100.100", "200.200.200.200");

        Set<EntitlementCondition> setConditions = new
            HashSet<EntitlementCondition>();
        setConditions.add(ipc);
        AndCondition andCondition = new AndCondition();
        andCondition.setEConditions(setConditions);

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
        privilege.setName("PrivilegeUtilsTest");
        privilege.setEntitlement(entitlement);
        privilege.setSubject(os); //orSubject
        privilege.setCondition(andCondition);
        privilege.setResourceAttributes(ra);

        Policy policy = PrivilegeUtils.privilegeToPolicy("/", privilege);
        Set<IPrivilege> ps = PrivilegeUtils.policyToPrivileges(policy);

        if ((ps == null) || ps.isEmpty()) {
            throw new Exception(
                "PrivilegeUtilsTest.testPrivilegeToPolicy failed.");
        }
    }

    private Rule createRule(String ruleName) throws PolicyException {
        Map<String, Set<String>> actionMap = new HashMap<String, Set<String>>();
        Set<String> getValues = new HashSet<String>();
        getValues.add("allow");
        actionMap.put("GET", getValues);
        Set<String> postValues = new HashSet<String>();
        postValues.add("deny");
        actionMap.put("POST", postValues);
        return new Rule(ruleName,
                "iPlanetAMWebAgentService",
                "http://sample.com/" + ruleName,
                actionMap);
    }

    private Subject createUsersSubject(
            PolicyManager pm,
            String... userNames) throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> values = new HashSet<String>();
        for (String value : userNames) {
            String uuid = "id=" + value + ",ou=user," + ServiceManager.getBaseDN();
            values.add(uuid);
        }
        subject.setValues(values);
        return subject;
    }

    private void createUsers(
            SSOToken adminToken,
            String... names)
            throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
                adminToken, "/");
        for (String name : names) {
            Map attrMap = new HashMap();

            Set cnVals = new HashSet();
            cnVals.add(name);
            attrMap.put("cn", cnVals);

            Set snVals = new HashSet();
            snVals.add(name);
            attrMap.put("sn", snVals);

            Set nameVals = new HashSet();
            nameVals.add(name);
            attrMap.put("givenname", nameVals);

            Set passworVals = new HashSet();
            passworVals.add(name);
            attrMap.put("userpassword", passworVals);

            amir.createIdentity(IdType.USER, name, attrMap);
        }
    }

    private void deleteUsers(SSOToken adminToken,
            String... names)
            throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
                adminToken, "/");
        Set identities = new HashSet();
        for (String name : names) {
            String uuid = "id=" + name + ",ou=user," + ServiceManager.getBaseDN();
            identities.add(IdUtils.getIdentity(adminToken, uuid));
        }
        amir.deleteIdentities(identities);
    }
}
