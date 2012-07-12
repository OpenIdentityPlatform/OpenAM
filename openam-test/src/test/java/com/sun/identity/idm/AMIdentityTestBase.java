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
 * $Id: AMIdentityTestBase.java,v 1.5 2008/06/25 05:44:19 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.idm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.test.CollectionUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.test.common.FileHelper;
import com.sun.identity.test.common.TestBase;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This class tests the <code>com.sun.identity.idm.AMIdentity</code> class.
 */
public class AMIdentityTestBase extends TestBase {
    
    public AMIdentityTestBase() {
        super("IDM");
    }
    
    /**
     * Creates realm before the test suites are executed.
     *
     * @throws SMSException if realm cannot be created.
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid.
     */
    @Parameters({"parent-realms"})
    @BeforeSuite(groups = {"api"})
    public void suiteSetup(String realms)
        throws SSOException, SMSException {
        Object[] params = {realms};
        entering("suiteSetup", params);
        
        StringTokenizer st = new StringTokenizer(realms, ",");
        while (st.hasMoreElements()) {
            String realm = st.nextToken().trim();
            createSubRealm(getAdminSSOToken(), realm);
        }
        
        exiting("suiteSetup");
    }

    /**
     * Creates realm and <code>AMIdenity</code> object before the testcases are
     * executed.
     *
     * @throws Exception if <code>AMIdenity</code> object cannot be created.
     */
    @Parameters({"parent-realm", "entity-type", "entity-name",
        "entity-creation-attributes"})
    @BeforeTest(groups = {"api"})
    public void setup(
        String parentRealm,
        String idType,
        String entityName,
        String createAttributes
    ) throws Exception {
        Object[] params = {parentRealm, idType, entityName, createAttributes};
        entering("setup", params);
        try {
            IdType type = IdUtils.getType(idType);
            Map values = CollectionUtils.parseStringToMap(createAttributes);

            AMIdentity amid = createIdentity(parentRealm, type, entityName,
                values);
            assert amid.getName().equals(entityName);
            assert amid.getType().equals(type);
            String amidRealm = DNMapper.orgNameToRealmName(amid.getRealm());
            if (amidRealm.charAt(0) != '/') {
                amidRealm = "/" + amidRealm;
            }
            assert amidRealm.equals(parentRealm);
            if (type.equals(IdType.AGENT) || type.equals(IdType.USER)) {
                assert amid.isActive();
            }
            assert amid.isExists();
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("setup");
    }
    
    /**
     * Assigning and deassigning services to <code>AMIdentity</code> object.
     *
     * @throws Exception if cannot access to <code>AMIdentity</code> object.
     */
    @Parameters({"parent-realm", "entity-type", "entity-name",
        "entity-modify-service1-name", "entity-modify-service1-attributes"})
    @Test(groups = {"api", "service"})
    public void assignUnassignService(
        String parentRealm,
        String idType,
        String entityName,
        String strServiceNames,
        String svcModificationAttrs
    ) throws Exception {
        Object[] params = {parentRealm, idType, entityName, strServiceNames,
            svcModificationAttrs};
        entering("assignUnassignService", params);
        try {
            
            AMIdentity amid = getIdentity(parentRealm,
                IdUtils.getType(idType), entityName);

            Set<String> serviceNames = CollectionUtils.parseStringToSet(
                    strServiceNames);
            if ((serviceNames != null) && !serviceNames.isEmpty()) {
                String serviceName = serviceNames.iterator().next();
                amid.assignService(serviceName, Collections.EMPTY_MAP);
                Map<String, Set<String>> values = 
                    CollectionUtils.parseStringToMap(svcModificationAttrs);
                amid.modifyService(serviceName, values);
                Map<String, Set<String>> verification = amid.getServiceAttributes(serviceName);
                for (String key : verification.keySet()) {
                    if (values.keySet().contains(key)) {
                        assert values.get(key).equals(verification.get(key));
                    }
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        }

        exiting("assignUnassignService");
    }

    /**
     * Modifies attributes
     *
     * @throws Exception if cannot access to <code>AMIdentity</code> object.
     */
    @Parameters({"parent-realm", "entity-type", "entity-name",
        "entity-modify-attributes"})
    @Test(groups = {"api"})
    public void modifyAttributes(
        String parentRealm,
        String idType,
        String entityName,
        String modificationAttributes
    ) throws Exception {
        Object[] params = {parentRealm, idType, entityName, 
            modificationAttributes};
        entering("modifyAttributes", params);

        Map<String, Set<String>> values = CollectionUtils.parseStringToMap(
            modificationAttributes);
        if (!values.isEmpty()) {
            try {
                AMIdentity amid = getIdentity(parentRealm,
                    IdUtils.getType(idType), entityName);
                modifyIdentity(amid, values);
                Map verification = amid.getAttributes(
                    values.keySet());
                assert verification.equals(values);
            } catch (Exception e) {
                log(Level.SEVERE, "modifyAttributes", e.getMessage(), 
                    params);
                e.printStackTrace();
                throw e;
            }
        }
        
        exiting("modifyAttributes");
    }

    /**
     * Sets and gets binary attributes
     *
     * @throws Exception if cannot access to <code>AMIdentity</code> object.
     **/
    @Parameters({"parent-realm", "entity-type", "entity-name",
        "entity-binary-attributes"})
    @Test(groups = {"api", "user-base"})
    public void setGetBinaryAttributes(
        String parentRealm,
        String idType,
        String entityName,
        String fileName
    ) throws Exception {
        // Issue 77
        Object[] params = {parentRealm, idType, entityName, fileName};
        entering("setGetBinaryAttributes", params);
        try {
            byte[] content = FileHelper.getBinary(fileName);
            AMIdentity amid = getIdentity(parentRealm,
                IdUtils.getType(idType), entityName);
            Map<String, byte[][]> map = new HashMap<String, byte[][]>();
            byte[][] values = new byte[1][];
            map.put("telephonenumber", values);
            values[0] = content;
            amid.setBinaryAttributes(map);
            amid.store();
            
            Set<String> set = new HashSet<String>();
            set.add("telephonenumber");
            Map verify = amid.getBinaryAttributes(set);
            assert !verify.values().isEmpty();
            byte[][] verifyArr = (byte[][]) verify.values().iterator().next();

            assert Arrays.deepEquals(values, verifyArr);
         } catch (Exception e) {
            log(Level.SEVERE, "setGetBinaryAttributes", 
                e.getMessage(), params);
            e.printStackTrace();
            throw e;
        }
       
        exiting("setGetBinaryAttributes");
    }
        
    /**
     * Passes an null (Map) to the modify attribute API.
     *
     * @throws Exception if cannot access to <code>AMIdentity</code> object.
     */
    @Parameters({"parent-realm", "entity-type", "entity-name"})
    @Test(groups = {"api"})
    public void modifyWithNullValues(
        String parentRealm,
        String idType,
        String entityName
    ) throws Exception {
        Object[] params = {parentRealm, idType, entityName};
        entering("modifyWithNullValues", params);
        
        try {
            
            AMIdentity amid = getIdentity(parentRealm,
                IdUtils.getType(idType), entityName);
            modifyIdentity(amid, null);
        } catch (Exception e) {
            log(Level.SEVERE, "modifyWithNullValues", e.getMessage(),
                params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("modifyWithNullValues");
    }

    @Parameters({"parent-realm", "entity-type", "entity-name"})
    @Test(groups = {"api", "memberships"}, expectedExceptions = {IdRepoException.class})
    public void assignMemberTwice(String parentRealm, String idType, String entityName) throws IdRepoException, SSOException {
        Object[] params = {parentRealm, idType, entityName};
        entering("assignMemberTwice", params);
        try {
            AMIdentity amid1 = createDummyUser(parentRealm, entityName, "1");
            AMIdentity amid = getIdentity(parentRealm,
                    IdUtils.getType(idType), entityName);

            amid.addMember(amid1);
            assert amid1.isMember(amid);
            // add twice
            amid.addMember(amid1);

        } catch (SSOException e) {
            log(Level.SEVERE, "assignMemberTwice", e.getMessage(),
                    params);
            e.printStackTrace();
            throw e;
        } finally {
            deleteIdentity(parentRealm, IdType.USER, entityName + "1");
        }
        exiting("assignMemberTwice");
    }

    /**
     * Adds and removes members from the <code>AMIdentity</code> object.
     *
     * @throws Exception if cannot access to <code>AMIdentity</code>
     */
    @Parameters({"parent-realm", "entity-type", "entity-name"})
    @Test(groups = {"api", "memberships"})
    public void assignUnassignMembers(
        String parentRealm,
        String idType,
        String entityName
    ) throws Exception {
        Object[] params = {parentRealm, idType, entityName};
        entering("assignUnassignMembers", params);
        try {
            
            AMIdentity amid1 = createDummyUser(parentRealm, entityName, "1");
            AMIdentity amid2 = createDummyUser(parentRealm, entityName, "2");
            AMIdentity amid3 = createDummyUser(parentRealm, entityName, "3");
            AMIdentity amid = getIdentity(parentRealm,
                IdUtils.getType(idType), entityName);
            
            amid.addMember(amid1);

            assert amid1.isMember(amid);
            amid.addMember(amid2);
            assert amid1.isMember(amid);
            assert amid2.isMember(amid);
            amid.addMember(amid3);
            assert amid1.isMember(amid);
            assert amid2.isMember(amid);
            assert amid3.isMember(amid);

            Set<AMIdentity> set = new HashSet<AMIdentity>();
            set.add(amid2);
            set.add(amid3);

            amid.removeMember(amid1);
            assert !amid1.isMember(amid);

            Set members = amid.getMembers(IdType.USER);
            assert members.equals(set);

            amid.removeMembers(set);
            assert !amid1.isMember(amid);
            assert !amid2.isMember(amid);
            assert !amid3.isMember(amid);
            deleteIdentity(parentRealm, IdType.USER, entityName + "1");
            deleteIdentity(parentRealm, IdType.USER, entityName + "2");
            deleteIdentity(parentRealm, IdType.USER, entityName + "3");
        } catch (Exception e) {
            log(Level.SEVERE, "assignUnassignMembers", e.getMessage(), 
                params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("assignUnassignMembers");
    }
    

    /**
     * Creates <code>AMIdentity</code> twice.
     *
     * @throws IdRepoException if <code>AMIdentity</code> object cannot be
     *         created.
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid. 
     */
    @Parameters({"parent-realm", "entity-type", "entity-name",
        "entity-creation-attributes"})
    @Test(groups = {"api"}, expectedExceptions={IdRepoException.class})
    public void createIdentityTwice(
        String parentRealm,
        String idType,
        String entityName,
        String createAttributes
    ) throws IdRepoException, SSOException {
        Object[] params = {parentRealm, idType, entityName, createAttributes};
        entering("createIdentityTwice", params);
        
        try {
                
            IdType type = IdUtils.getType(idType);
            Map values = CollectionUtils.parseStringToMap(createAttributes);
            createIdentity(parentRealm, type, entityName, values);
        } catch (SSOException e) {
            log(Level.SEVERE, "createIdentityTwice", e.getMessage(), 
                params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("createIdentityTwice");
    }

    /**
     * Creates <code>AMIdentity</code> twice with long name.
     *
     * @throws IdRepoException if <code>AMIdentity</code> object cannot be
     *         created.
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid. 
     */
    @Parameters({"parent-realm", "entity-type", "entity-name",
        "entity-creation-attributes"})
    @Test(groups = {"api"})
    public void createIdentityWithLongName(
            String parentRealm,
            String idType,
            String entityName,
            String createAttributes
    ) throws IdRepoException, SSOException {
        Object[] params = {parentRealm, idType, entityName, createAttributes};
        entering("createIdentityWithLongName", params);
        try {
            
            String name = entityName;
            for (int i = 0; i < 100; i++) {
                name += entityName;
            }
            IdType type = IdUtils.getType(idType);
            Map values = CollectionUtils.parseStringToMap(createAttributes);
            createIdentity(parentRealm, type, name, values);
            deleteIdentity(parentRealm, type, name);
        } catch (SSOException e) {
            log(Level.SEVERE, "createIdentityWithLongName", e.getMessage(),
                params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("createIdentityWithLongName");
    }


    /**
     * Creates <code>AMIdentity</code> twice with no name.
     *
     * @throws IdRepoException if <code>AMIdentity</code> object cannot be
     *         created.
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid. 
     */
    @Parameters({"parent-realm", "entity-type", "entity-creation-attributes"})
    @Test(groups = {"api"}, expectedExceptions={IdRepoException.class})
    public void createIdenityWithNoName(
        String parentRealm,
        String idType,
        String createAttributes
    ) throws IdRepoException, SSOException {
        Object[] params = {parentRealm, idType, createAttributes};
        entering("createIdenityWithNoName", params);
        try {
            
            IdType type = IdUtils.getType(idType);
            Map values = CollectionUtils.parseStringToMap(createAttributes);
            createIdentity(parentRealm, type, "", values);
        } catch (SSOException e) {
            log(Level.SEVERE, "createIdenityWithNoName", e.getMessage(), 
                params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("createIdenityWithNoName");
    }


    /**
     * Set required values of required attributes in <code>AMIdentity</code>
     * to null.
     *
     * @throws IdRepoException if <code>AMIdentity</code> object cannot be
     *         modified.
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid. 
     */
    @Parameters({"parent-realm", "entity-type", "entity-name",
        "entity-required-attributes"})
    @Test(groups = {"api", "ldap"}, expectedExceptions={IdRepoException.class})
    public void nullifyRequiredAttribute(
        String parentRealm,
        String idType,
        String entityName,
        String requiredAttributes
    ) throws IdRepoException, SSOException {
        Object[] params = {parentRealm, idType, entityName, requiredAttributes};
        entering("nullifyRequiredAttribute", params);
        try {
            
            Set<String> setRequiredAttributes =
                CollectionUtils.parseStringToSet(requiredAttributes);
            if (!setRequiredAttributes.isEmpty()) {
                Map<String, Set<String>> emptyValues =
                    CollectionUtils.getEmptyValuesMap(setRequiredAttributes);
                AMIdentity amid = getIdentity(parentRealm,
                    IdUtils.getType(idType), entityName);
                amid.setAttributes(emptyValues);
                amid.store();
            }
        } catch (SSOException e) {
            log(Level.SEVERE, "nullifyRequiredAttribute", e.getMessage(), 
                params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("nullifyRequiredAttribute");
    }

    /**
     * Removes membership of <code>AMIdentity</code> itself from it.
     *
     * @throws IdRepoException if membership removal failed.
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid. 
     */
    @Parameters({"parent-realm", "entity-type", "entity-name"})
    @Test(groups = {"api", "memberships"}, 
        expectedExceptions={IdRepoException.class}) 
    public void addItselfAsMember(
        String parentRealm,
        String idType,
        String entityName
    ) throws IdRepoException, SSOException {
        Object[] params = {parentRealm, idType, entityName};
        entering("addItselfAsMember", params);
        
        try {
            AMIdentity amid = getIdentity(parentRealm,
                IdUtils.getType(idType), entityName);
            amid.removeMember(amid);
        } catch (SSOException e) {
            log(Level.SEVERE, "addItselfAsMember", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("addItselfAsMember");
    }
    
    
    /**
     * Tests <code>isExists</code> method.
     *
     * @throws Exception if cannot access to <code>AMIdentity</code> object.
     */
    @Parameters({"parent-realm", "entity-type", "entity-name", 
        "entity-creation-attributes"})
    @Test(groups = {"api"})
    public void verifyExistence(
        String parentRealm,
        String idType,
        String entityName,
        String createAttributes
    ) throws Exception {
        Object[] params = {parentRealm, idType, entityName, createAttributes};
        entering("verifyExistence", params);
        try {
            Map values = CollectionUtils.parseStringToMap(createAttributes);
            IdType type = IdUtils.getType(idType);
            AMIdentity a = createIdentity(parentRealm, type, entityName +
                "exist", values);
            assert a.isExists();
            AMIdentityRepository repo = new AMIdentityRepository(
                getAdminSSOToken(), parentRealm);
            IdSearchResults results = repo.searchIdentities(type,
                entityName + "exist", new IdSearchControl());
            Set resultSets = results.getSearchResults();
            assert resultSets.size() == 1;
            deleteIdentity(parentRealm, type, entityName + "exist");
            resultSets = repo.searchIdentities(type, entityName + "exist", new IdSearchControl()).getSearchResults();
            assert resultSets.isEmpty();
        } catch (Exception e) {
            log(Level.SEVERE, "verifyExistence", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        } finally {
            exiting("verifyExistence");
        }
    }

    /**
     * Removes <code>AMIdentity</code> object after suite test is
     * done.
     * 
     * @throws Exception if <code>AMIdenity</code> object cannot be deleted.
     */
    @Parameters({"parent-realm", "entity-type", "entity-name"})
    @AfterTest(groups = {"api"})
    public void tearDown(
        String parentRealm,
        String idType,
        String entityName
    ) throws Exception {
        Object[] params = {parentRealm, idType, entityName};
        entering("tearDown", params);
        try {
            
            deleteIdentity(parentRealm, IdUtils.getType(idType), entityName);
        } catch (Exception e) {
            log(Level.SEVERE, "tearDown", e.getMessage(), params);
            e.printStackTrace();
            throw e;
        }
        
        exiting("tearDown");
    }
    
    /**
     * Removes realm after suite test is done.
     * 
     * @throws SMSException if realm cannot be deleted.
     * @throws SSOException if the super administrator Single Sign On is 
     *         invalid.
     */
    @Parameters({"parent-realms"})
    @AfterSuite(groups = {"api"})
    public void suiteTearDown(String realms)
        throws SSOException, SMSException {
        Object[] params = {realms};
        entering("suiteTearDown", params);
        
        StringTokenizer st = new StringTokenizer(realms, ",");
        while (st.hasMoreElements()) {
            String realm = st.nextToken().trim();
            deleteRealm(getAdminSSOToken(), realm);
        }

        exiting("suiteTearDown");
    }

    private AMIdentity createIdentity(
        String parentRealm,
        IdType idType,
        String entityName,
        Map values
    ) throws IdRepoException, SSOException {
        SSOToken ssoToken = getAdminSSOToken();
        AMIdentityRepository repo = new AMIdentityRepository(
            ssoToken, parentRealm);
        AMIdentity amid = repo.createIdentity(idType, entityName, values);
        return amid;
    }

    private AMIdentity getIdentity(
        String parentRealm,
        IdType idType,
        String entityName
    ) throws IdRepoException, SSOException {
        SSOToken ssoToken = getAdminSSOToken();
        AMIdentityRepository repo = new AMIdentityRepository(
            ssoToken, parentRealm);
        return new AMIdentity(ssoToken, entityName, idType, parentRealm, null);
    }

    private void modifyIdentity(AMIdentity amid, Map values)
        throws IdRepoException, SSOException {
        amid.setAttributes(values);
        amid.store();
    }

    private void deleteIdentity(
        String parentRealm,
        IdType idType,
        String entityName
    ) throws IdRepoException, SSOException {
        SSOToken ssoToken = getAdminSSOToken();
        AMIdentityRepository repo = new AMIdentityRepository(
            ssoToken, parentRealm);
        repo.deleteIdentities(getAMIdentity(
            ssoToken, entityName, idType, parentRealm));
        IdSearchResults results = repo.searchIdentities(idType, entityName,
            new IdSearchControl());
        Set resultSets = results.getSearchResults();
        assert resultSets.isEmpty();
    }

    private Set<AMIdentity> getAMIdentity(
        SSOToken ssoToken,
        String name,
        IdType idType,
        String realm
    ) {
        Set<AMIdentity> set = new HashSet<AMIdentity>();
        set.add(new AMIdentity(ssoToken, name, idType, realm, null));
        return set;
    }
    
    private AMIdentity createDummyUser(
        String parentRealm, 
        String entityName, 
        String suffix
    ) throws IdRepoException, SSOException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        CollectionUtils.putSetIntoMap("sn", map, "sn" + suffix);
        CollectionUtils.putSetIntoMap("cn", map, "cn" + suffix);
        CollectionUtils.putSetIntoMap("userpassword", map, "password" + suffix);
        CollectionUtils.putSetIntoMap("inetuserstatus", map, "Active");
        return createIdentity(parentRealm, IdType.USER, entityName + suffix, 
            map);
    }
   
    private String getParentRealm(String realm) {
        int idx = realm.lastIndexOf("/");
        if (idx == -1) {
            throw new RuntimeException("Incorrect Realm, " + realm);
        }
        return (idx == 0) ? "/" : realm.substring(0, idx);
    }
    
    private void createSubRealm(SSOToken ssoToken, String realm)
        throws SSOException, SMSException
    {
        if ((realm != null) && !realm.equals("/")) {
            String parentRealm = getParentRealm(realm);
            createSubRealm(ssoToken, parentRealm);
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                ssoToken, parentRealm);
            int idx = realm.lastIndexOf("/");
            try {
                orgMgr.createSubOrganization(realm.substring(idx + 1), null);
            } catch (SMSException e) {
                //ignore if the sub organization already exists.
            }
        }
    }

    private void deleteRealm(SSOToken ssoToken, String realm)
        throws SSOException
    {
        if ((realm != null) && !realm.equals("/")) {
            String parentRealm = getParentRealm(realm);
            try {
                OrganizationConfigManager orgMgr = new
                    OrganizationConfigManager(ssoToken, parentRealm);
                int idx = realm.lastIndexOf("/");
                orgMgr.deleteSubOrganization(realm.substring(idx+1), true);
            } catch (SMSException e) {
                //ignore if the sub organization already exists.
            }
            deleteRealm(ssoToken, parentRealm);
        }
    }
}

