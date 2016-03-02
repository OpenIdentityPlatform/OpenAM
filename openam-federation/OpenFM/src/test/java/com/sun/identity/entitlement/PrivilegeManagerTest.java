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
 * $Id: PrivilegeManagerTest.java,v 1.3 2010/01/26 20:10:16 dillidorai Exp $
 *
 * Portions copyright 2014-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.opensso.OpenSSOUserSubject;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.unittest.UnittestLog;
import org.forgerock.openam.entitlement.conditions.environment.IPv4Condition;
import org.forgerock.openam.entitlement.conditions.environment.SimpleTimeCondition;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dillidorai
 */
public class PrivilegeManagerTest {
    private static final String APPL_NAME = "PrivilegeManagerTestAppl";
    private static final String REFERRAL_PRIVILEGE_NAME = "PrivilegeManagerTestReferral";
    private static final String PRIVILEGE_NAME = "PrivilegeManagerTest";
    private static final String PRIVILEGE_NAME1 = "PrivilegeManagerTest1";
    private static final String PRIVILEGE_NAME2 = "PrivilegeManagerTest2";
    private static final String PRIVILEGE_DESC = "Test Description";
    private static final String startIp = "100.100.100.100";
    private static final String endIp = "200.200.200.200";
    private static final String SUB_REALM = "/PrivilegeManagerTestsub";
    private static final String RESOURCE = "http://www.privilegemanagertest.*";

    private Privilege privilege;
    private SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
    private Subject adminSubject = SubjectUtils.createSubject(adminToken);
    private UserSubject ua1;
    private UserSubject ua2;
    private boolean migrated = true;

    @BeforeClass
    public void setup()
        throws SSOException, IdRepoException, EntitlementException,
        SMSException, InstantiationException, IllegalAccessException {

        if (!migrated) {
            return;
        }

        createApplication("/");
        OrganizationConfigManager orgMgr = new OrganizationConfigManager(
            adminToken, "/");
        String subRealm = SUB_REALM.substring(1);
        orgMgr.createSubOrganization(subRealm, Collections.EMPTY_MAP);
    }

    private void createApplication(String realm) throws EntitlementException,
        InstantiationException, IllegalAccessException {

        if (!migrated) {
            return;
        }

        Application appl = new Application(APPL_NAME,
            ApplicationTypeManager.getAppplicationType(adminSubject,
            ApplicationTypeManager.URL_APPLICATION_TYPE_NAME));

        // Test disabled, unable to fix model change
        // Set<String> appResources = new HashSet<String>();
        // appResources.add(RESOURCE);
        // appl.addResources(appResources);
        appl.setEntitlementCombiner(DenyOverride.class);
        ApplicationServiceTestHelper.saveApplication(adminSubject, realm, appl);
    }

    @AfterClass
    public void cleanup() throws Exception {
        if (!migrated) {
            return;
        }

        //Sub Realm
        PrivilegeManager prmSubReam = PrivilegeManager.getInstance(SUB_REALM, SubjectUtils.createSubject(adminToken));
        prmSubReam.remove(PRIVILEGE_NAME);

        PrivilegeManager prm = PrivilegeManager.getInstance("/", SubjectUtils.createSubject(adminToken));
        prm.remove(PRIVILEGE_NAME);
        ApplicationServiceTestHelper.deleteApplication(adminSubject, "/", APPL_NAME);
        ApplicationServiceTestHelper.deleteApplication(adminSubject, SUB_REALM, APPL_NAME);

        OrganizationConfigManager orgMgr = new OrganizationConfigManager(adminToken, "/");
        orgMgr.deleteSubOrganization(SUB_REALM, true);
    }

    @Test
    public void testNoSubjectInPrivilege() throws Exception {
        if (!migrated) {
            return;
        }
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        actionValues.put("POST", Boolean.FALSE);
        String resourceName = "http://www.privilegemanagertest.com:80";
        Entitlement entitlement = new Entitlement(APPL_NAME,
                resourceName, actionValues);
        entitlement.setName("ent1");

        Set<EntitlementSubject> eSubjects = new HashSet<EntitlementSubject>();
        eSubjects.add(new AndSubject(null));
        OrSubject os = new OrSubject(eSubjects);

        try {
            Privilege p = Privilege.getNewInstance();
            p.setName(PRIVILEGE_NAME1);
            p.setEntitlement(entitlement);
            p.setSubject(os);
        } catch (EntitlementException e) {
            if (e.getErrorCode() != 310)  {
                throw e;
            } else {
                return;
            }
        }

        throw new Exception("PrivilegeManagerTest.testNoSubjectInPrivilege failed");
    }

    private Privilege createPrivilege() throws EntitlementException {
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        actionValues.put("GET", Boolean.TRUE);
        actionValues.put("POST", Boolean.FALSE);
        String resourceName = "http://www.privilegemanagertest.com:80";
        Entitlement entitlement = new Entitlement(APPL_NAME, resourceName, actionValues);
        entitlement.setName("ent1");

        String user11 = "id=user11,ou=user," + ServiceManager.getBaseDN();
        String user12 = "id=user12,ou=user," + ServiceManager.getBaseDN();
        ua1 = new OpenSSOUserSubject();
        ua1.setID(user11);
        ua2 = new OpenSSOUserSubject();
        ua2.setID(user12);
        Set<EntitlementSubject> subjects = new HashSet<EntitlementSubject>();
        subjects.add(ua1);
        subjects.add(ua2);
        OrSubject os = new OrSubject(subjects);

        IPv4Condition ipc = new IPv4Condition();
        ipc.setStartIpAndEndIp(startIp, endIp);
        SimpleTimeCondition tc = new SimpleTimeCondition();
        tc.setStartTime("08:00");
        tc.setEndTime("16:00");
        tc.setStartDay("mon");
        tc.setEndDay("fri");
        Set<EntitlementCondition> conditions = new HashSet<EntitlementCondition>();
        conditions.add(tc);

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

        Privilege priv = Privilege.getNewInstance();
        priv.setName(PRIVILEGE_NAME);
        priv.setEntitlement(entitlement);
        priv.setSubject(os);
        priv.setCondition(ipc);
        priv.setResourceAttributes(ra);
        priv.setDescription(PRIVILEGE_DESC);
        return priv;
    }

    @Test(dependsOnMethods = {"testAddPrivilege"})
    public void subRealmTest() throws Exception {
        if (!migrated) {
            return;
        }
        PrivilegeManager prm = PrivilegeManager.getInstance(SUB_REALM,
            SubjectUtils.createSubject(adminToken));

        try {
            Privilege p = prm.findByName(PRIVILEGE_NAME);
        } catch (EntitlementException e){
            //ok
        }

        Set<String> realms = new HashSet<String>();
        realms.add(SUB_REALM);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> referResources = new HashSet<String>();
        referResources.add(RESOURCE);
        map.put(APPL_NAME, referResources);
        ReferralPrivilege referral = new ReferralPrivilege(
            REFERRAL_PRIVILEGE_NAME, map, realms);

        if (!referral.getMapApplNameToResources().get(
            APPL_NAME).contains(RESOURCE)) {
            throw new Exception("PrivilegeManagerTest.subRealmTest, resource is likely to be canonicalized");
        }

        prm.add(privilege);
        Thread.sleep(1000);
        Privilege p = prm.findByName(PRIVILEGE_NAME);
    }

    @Test
    public void testAddPrivilege() throws Exception {
        if (!migrated) {
            return;
        }
        privilege = createPrivilege();
        PrivilegeManager prm = PrivilegeManager.getInstance("/", SubjectUtils.createSubject(adminToken));
        prm.add(privilege);
        Thread.sleep(1000);

        Privilege p = prm.findByName(PRIVILEGE_NAME);

        IPv4Condition ipc1 = (IPv4Condition) p.getCondition();
        if (!ipc1.getStartIp().equals(startIp)) {
            throw new Exception(
                "PrivilegeManagerTest.testAddPrivilege():"
                + "READ startIp "
                + " does not equal set startIp");
        }
        if (!ipc1.getEndIp().equals(endIp)) {
            throw new Exception(
                "PrivilegeManagerTest.testAddPrivilege():"
                + "READ endIp "
                + " does not equal set endIp");
        }
        if (!privilege.equals(p)) {
            throw new Exception("PrivilegeManagerTest.testAddPrivilege():"
                + "read privilege not"
                + "equal to saved privilege");
        }

        {
            EntitlementSubject subjectCollections = privilege.getSubject();
            if (subjectCollections instanceof OrSubject) {
                OrSubject orSbj = (OrSubject)subjectCollections;
                Set<EntitlementSubject> subjs = orSbj.getESubjects();
                for (EntitlementSubject sbj : subjs) {
                    if (!sbj.equals(ua1) && !sbj.equals(ua2)) {
                        throw new Exception("PrivilegeManagerTest.testAddPrivilege: Subject does not matched.");
                    }
                }
            }
        }
    }

    @Test(dependsOnMethods = {"testAddPrivilege"})
    public void testAddPrivilege2() throws Exception {
        if (!migrated) {
            return;
        }
        privilege = createPrivilege();
        privilege.setName(PRIVILEGE_NAME2);
        PrivilegeManager prm = PrivilegeManager.getInstance("/", SubjectUtils.createSubject(adminToken));
        prm.add(privilege);
        Thread.sleep(1000);

        Privilege p = prm.findByName(PRIVILEGE_NAME2);

        IPv4Condition ipc1 = (IPv4Condition) p.getCondition();
        if (!ipc1.getStartIp().equals(startIp)) {
            throw new Exception(
                "PrivilegeManagerTest.testAddPrivilege():"
                + "READ startIp "
                + " does not equal set startIp");
        }
        if (!ipc1.getEndIp().equals(endIp)) {
            throw new Exception(
                "PrivilegeManagerTest.testAddPrivilege():"
                + "READ endIp "
                + " does not equal set endIp");
        }
        if (!privilege.equals(p)) {
            throw new Exception("PrivilegeManagerTest.testAddPrivilege():"
                + "read privilege not"
                + "equal to saved privilege");
        }

        {
            EntitlementSubject subjectCollections = privilege.getSubject();
            if (subjectCollections instanceof OrSubject) {
                OrSubject orSbj = (OrSubject)subjectCollections;
                Set<EntitlementSubject> subjs = orSbj.getESubjects();
                for (EntitlementSubject sbj : subjs) {
                    if (!sbj.equals(ua1) && !sbj.equals(ua2)) {
                        throw new Exception("PrivilegeManagerTest.testAddPrivilege: Subject does not matched.");
                    }
                }
            }
        }
    }

    @Test(dependsOnMethods = {"testAddPrivilege"})
    public void testSerializePrivilege() throws Exception {
        if (!migrated) {
            return;
        }
        String serialized = privilege.toJSONObject().toString();
        Privilege p = Privilege.getInstance(new JSONObject(serialized));
        if (!p.equals(privilege)) {
            throw new Exception("PrivilegeManagerTest.testSerializePrivilege: failed");
        }
    }

    @Test(dependsOnMethods = {"testAddPrivilege"})
    public void testListPrivilegeNames() throws Exception {
        if (!migrated) {
            return;
        }
        PrivilegeManager prm = PrivilegeManager.getInstance("/",
            SubjectUtils.createSubject(adminToken));

        Set<SearchFilter> psf = new HashSet<SearchFilter>();
        psf.add(new SearchFilter(Privilege.NAME_SEARCH_ATTRIBUTE, "*"));
        Set privilegeNames = prm.searchNames(psf);
        if (!privilegeNames.contains(PRIVILEGE_NAME)) {
              throw new Exception(
                "PrivilegeManagerTest.testListPrivilegeNames():"
                + "got privilege names does not contain saved privilege");
        }

        psf = new HashSet<SearchFilter>();
        psf.add(new SearchFilter(Privilege.DESCRIPTION_SEARCH_ATTRIBUTE,
            PRIVILEGE_DESC));
        privilegeNames = prm.searchNames(psf);
        if (!privilegeNames.contains(PRIVILEGE_NAME)) {
              throw new Exception(
                "PrivilegeManagerTest.testListPrivilegeNames():"
                + "got privilege names does not contain saved privilege");
        }
    }

    @Test(dependsOnMethods = {"testAddPrivilege"})
    public void testGetPrivilege() throws Exception {
        if (!migrated) {
            return;
        }
        PrivilegeManager prm = PrivilegeManager.getInstance("/",
            SubjectUtils.createSubject(adminToken));
        Privilege p = prm.findByName(PRIVILEGE_NAME);

        if (p == null) {
            throw new Exception("PrivilegeManagerTest.testGetPrivilege: "
                    + "failed to get privilege.");
        }

        if (!p.getDescription().equals(PRIVILEGE_DESC)) {
            throw new Exception("PrivilegeManagerTest.testGetPrivilege: "
                    + "failed to get privilege description.");
        }

    }

    @Test(dependsOnMethods = {"testAddPrivilege"})
    public void testLastModifiedDate() throws Exception {
        if (!migrated) {
            return;
        }
        PrivilegeManager prm = PrivilegeManager.getInstance("/", SubjectUtils.createSubject(adminToken));
        prm.modify(privilege);
        Long creationDate = privilege.getCreationDate();
        Long lastModifiedDate = privilege.getLastModifiedDate();
        if (creationDate.equals(lastModifiedDate)) {
            throw new Exception("PrivilegeManagerTest.testLastModifiedDate: "
                    + "creation and last modified date are the same.");
        }
    }
}
