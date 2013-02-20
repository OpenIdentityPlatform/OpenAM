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
 * $Id: DataStore.java,v 1.13 2010/01/20 17:01:35 veiming Exp $
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.IPrivilege;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferredApplicationManager;
import com.sun.identity.entitlement.ResourceSaveIndexes;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.util.NetworkMonitor;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.BufferedIterator;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.stats.Stats;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSDataEntry;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class *talks* to SMS to get the configuration information.
 */
public class DataStore {
    private static DataStore instance = new DataStore();
    public static final String POLICY_STORE = "default";
    public static final String REFERRAL_STORE = "referrals";

    private static final String SERVICE_NAME = "sunEntitlementIndexes";
    private static final String INDEX_COUNT = "indexCount";
    private static final String REFERRAL_INDEX_COUNT = "referralIndexCount";
    private static final String REALM_DN_TEMPLATE =
         "ou={0},ou=default,ou=OrganizationConfig,ou=1.0,ou=" + SERVICE_NAME +
         ",ou=services,{1}";
    private static final String SUBJECT_INDEX_KEY = "subjectindex";
    private static final String HOST_INDEX_KEY = "hostindex";
    private static final String PATH_INDEX_KEY = "pathindex";
    private static final String PATH_PARENT_INDEX_KEY = "pathparentindex";
    private static final String SERIALIZABLE_INDEX_KEY = "serializable";

    public static final String REFERRAL_REALMS = "referralrealms";
    public static final String REFERRAL_APPLS = "referralappls";

    private static final String SUBJECT_FILTER_TEMPLATE =
        "(" + SMSEntry.ATTR_XML_KEYVAL + "=" + SUBJECT_INDEX_KEY + "={0})";
    private static final String HOST_FILTER_TEMPLATE =
        "(" + SMSEntry.ATTR_XML_KEYVAL + "=" + HOST_INDEX_KEY + "={0})";
    private static final String PATH_FILTER_TEMPLATE =
        "(" + SMSEntry.ATTR_XML_KEYVAL + "=" + PATH_INDEX_KEY + "={0})";
    private static final String PATH_PARENT_FILTER_TEMPLATE =
        "(" + SMSEntry.ATTR_XML_KEYVAL + "=" + PATH_PARENT_INDEX_KEY + "={0})";

    private static final NetworkMonitor DB_MONITOR_PRIVILEGE =
        NetworkMonitor.getInstance("dbLookupPrivileges");
    private static final NetworkMonitor DB_MONITOR_REFERRAL =
        NetworkMonitor.getInstance("dbLookupReferrals");
    private static final String HIDDEN_REALM_DN =
        "o=sunamhiddenrealmdelegationservicepermissions,ou=services,";
    
    // count of number of policies per realm
    private static ReadWriteLock countRWLock = new ReentrantReadWriteLock();
    private static Map<String, Integer> policiesPerRealm =
        new HashMap<String, Integer>();
    private static Map<String, Integer> referralsPerRealm =
        new HashMap<String, Integer>();
    private static SSOToken adminToken = (SSOToken)
        AccessController.doPrivileged(AdminTokenAction.getInstance());

    static {
        // Initialize statistics collection
        Stats stats = Stats.getInstance("Entitlements");
        EntitlementsStats es = new EntitlementsStats(stats);
        stats.addStatsListener(es);
    }

    private DataStore() {
    }

    public static DataStore getInstance() {
        return instance;
    }
    /**
     * Returns distingished name of a privilege.
     *
     * @param name Privilege name.
     * @param realm Realm name.
     * @param indexName Index name.
     * @return the distingished name of a privilege.
     */
    public static String getPrivilegeDistinguishedName(
        String name,
        String realm,
        String indexName) {
        return "ou=" + name + "," + getSearchBaseDN(realm, indexName);
    }

    /**
     * Returns the base search DN.
     *
     * @param realm Realm name.
     * @param indexName Index name.
     * @return the base search DN.
     */
    public static String getSearchBaseDN(String realm, String indexName) {
        if (indexName == null) {
            indexName = POLICY_STORE;
        }
        String dn = (DN.isDN(realm)) ? realm : DNMapper.orgNameToDN(realm);
        Object[] args = {indexName, dn};
        return MessageFormat.format(REALM_DN_TEMPLATE, args);
    }

    private String createDefaultSubConfig(
        SSOToken adminToken,
        String realm,
        String indexName)
        throws SMSException, SSOException {
        if (indexName == null) {
            indexName = POLICY_STORE;
        }
        ServiceConfig orgConf = getOrgConfig(adminToken, realm);

        Set<String> subConfigNames = orgConf.getSubConfigNames();
        if (!subConfigNames.contains(indexName)) {
            orgConf.addSubConfig(indexName, "type", 0,
                Collections.EMPTY_MAP);
        }
        ServiceConfig defSubConfig = orgConf.getSubConfig(indexName);
        return defSubConfig.getDN();
    }

    private ServiceConfig getOrgConfig(SSOToken adminToken, String realm)
        throws SMSException, SSOException {
        ServiceConfigManager mgr = new ServiceConfigManager(
            SERVICE_NAME, adminToken);
        ServiceConfig orgConf = mgr.getOrganizationConfig(realm, null);
        if (orgConf == null) {
            mgr.createOrganizationConfig(realm, null);
        }
        return orgConf;
    }

    void clearIndexCount(String realm, boolean referral) {
        countRWLock.writeLock().lock();
        try {
            if (referral) {
                referralsPerRealm.remove(DNMapper.orgNameToDN(realm));
            } else {
                policiesPerRealm.remove(DNMapper.orgNameToDN(realm));
            }
        } finally {
            countRWLock.writeLock().unlock();
        }
    }

    private void updateIndexCount(String realm, int num, boolean referral) {
        countRWLock.writeLock().lock();

        try {
            String key = (referral) ? REFERRAL_INDEX_COUNT : INDEX_COUNT;

            ServiceConfig orgConf = getOrgConfig(adminToken, realm);
            Map<String, Set<String>> map = orgConf.getAttributes();
            Set<String> set = map.get(key);
            int count = num;

            if ((set != null) && !set.isEmpty()) {
                String strCount = (String) set.iterator().next();
                count += Integer.parseInt(strCount);
                set.clear();
            } else {
                set = new HashSet<String>();
                map.put(key, set);
            }

            set.add(Integer.toString(count));
            orgConf.setAttributes(map);

            if (referral) {
                referralsPerRealm.put(DNMapper.orgNameToDN(realm), count);
            } else {
                policiesPerRealm.put(DNMapper.orgNameToDN(realm), count);
            }
        } catch (NumberFormatException ex) {
            PrivilegeManager.debug.error("DataStore.updateIndexCount", ex);
        } catch (SMSException ex) {
            PrivilegeManager.debug.error("DataStore.updateIndexCount", ex);
        } catch (SSOException ex) {
            PrivilegeManager.debug.error("DataStore.updateIndexCount", ex);
        } finally {
            countRWLock.writeLock().unlock();
        }
    }

    private static int getIndexCount(String realm, boolean referral) {
        int count = 0;
        if (adminToken != null) {
            try {
                ServiceConfigManager mgr = new ServiceConfigManager(
                    SERVICE_NAME, adminToken);
                ServiceConfig orgConf = mgr.getOrganizationConfig(realm, null);
                if (orgConf != null) {
                    Map<String, Set<String>> map = orgConf.getAttributes();
                    Set<String> set = (referral) ?
                        map.get(REFERRAL_INDEX_COUNT) : map.get(INDEX_COUNT);
                    if ((set != null) && !set.isEmpty()) {
                        String strCount = (String) set.iterator().next();
                        count = Integer.parseInt(strCount);
                    }
                }
            } catch (NumberFormatException ex) {
                PrivilegeManager.debug.error("DataStore.getIndexCount", ex);
            } catch (SMSException ex) {
                PrivilegeManager.debug.error("DataStore.getIndexCount", ex);
            } catch (SSOException ex) {
                PrivilegeManager.debug.error("DataStore.getIndexCount", ex);
            }
        }
        return count;
    }

    public static int getNumberOfPolicies() {
        return getCountInMap(policiesPerRealm);
    }

    public static int getNumberOfReferrals() {
        return getCountInMap(referralsPerRealm);
    }

    private static int getCountInMap(Map<String, Integer> map) {
        countRWLock.readLock().lock();
        try {
            int total = 0;
            for (Integer cnt : map.values()) {
                total += cnt;
            }
            return total;
        } finally {
            countRWLock.readLock().unlock();
        }
    }

    public static int getNumberOfPolicies(String realm) {
        countRWLock.readLock().lock();
        try {
            int totalPolicies = 0;
            String dnRealm = DNMapper.orgNameToDN(realm);
            Integer tp = policiesPerRealm.get(dnRealm);
            if (tp == null) {
                totalPolicies = getIndexCount(realm, false);
                policiesPerRealm.put(dnRealm, totalPolicies);
            } else {
                totalPolicies = tp.intValue();
            }
            return (totalPolicies);
        } finally {
            countRWLock.readLock().unlock();
        }
    }

    public static int getNumberOfReferrals(String realm) {
        countRWLock.readLock().lock();
        try {
            int referralCnt = 0;
            String dnRealm = DNMapper.orgNameToDN(realm);
            Integer tp = referralsPerRealm.get(dnRealm);
            if (tp == null) {
                referralCnt = getIndexCount(realm, true);
                referralsPerRealm.put(dnRealm, referralCnt);
            } else {
                referralCnt = tp.intValue();
            }

            return (referralCnt);
        } finally {
            countRWLock.readLock().unlock();
        }
    }

    /**
     * Adds a privilege.
     *
     * @param adminSubject Admin Subject who has the rights to write to
     *        datastore.
     * @param realm Realm name.
     * @param p Privilege object.
     * @return the DN of added privilege.
     * @throws com.sun.identity.entitlement.EntitlementException if privilege
     * cannot be added.
     */
    public String add(Subject adminSubject, String realm, Privilege p)
        throws EntitlementException {

        ResourceSaveIndexes indexes =
            p.getEntitlement().getResourceSaveIndexes(adminSubject, realm);
        Set<String> subjectIndexes =
            SubjectAttributesManager.getSubjectSearchIndexes(p);

        String dn = null;
        try {
            createDefaultSubConfig(adminToken, realm, null);
            dn = getPrivilegeDistinguishedName(p.getName(), realm, null);

            SMSEntry s = new SMSEntry(adminToken, dn);
            Map<String, Set<String>> map = new HashMap<String, Set<String>>();

            Set<String> searchable = new HashSet<String>();
            map.put(SMSEntry.ATTR_XML_KEYVAL, searchable);

            for (String i : indexes.getHostIndexes()) {
                searchable.add(HOST_INDEX_KEY + "=" + i);
            }
            for (String i : indexes.getPathIndexes()) {
                searchable.add(PATH_INDEX_KEY + "=" + i);
            }
            for (String i : indexes.getParentPathIndexes()) {
                searchable.add(PATH_PARENT_INDEX_KEY + "=" + i);
            }
            for (String i : subjectIndexes) {
                searchable.add(SUBJECT_INDEX_KEY + "=" + i);
            }

            Set<String> setServiceID = new HashSet<String>(2);
            map.put(SMSEntry.ATTR_SERVICE_ID, setServiceID);
            setServiceID.add("indexes");

            Set<String> set = new HashSet<String>(2);
            map.put(SMSEntry.ATTR_KEYVAL, set);
            set.add(SERIALIZABLE_INDEX_KEY + "=" + p.toJSONObject().toString());

            Set<String> setObjectClass = new HashSet<String>(4);
            map.put(SMSEntry.ATTR_OBJECTCLASS, setObjectClass);
            setObjectClass.add(SMSEntry.OC_TOP);
            setObjectClass.add(SMSEntry.OC_SERVICE_COMP);

            Set<String> info = new HashSet<String>(8);

            String privilegeName = p.getName();
            if (privilegeName != null) {
                info.add(Privilege.NAME_ATTRIBUTE + "=" + privilegeName);
            }

            String privilegeDesc = p.getDescription();
            if (privilegeDesc != null) {
                info.add(Privilege.DESCRIPTION_ATTRIBUTE + "=" + privilegeDesc);
            }

            String createdBy = p.getCreatedBy();
            if (createdBy != null) {
                info.add(Privilege.CREATED_BY_ATTRIBUTE + "=" + createdBy);
            }

            String lastModifiedBy = p.getLastModifiedBy();
            if (lastModifiedBy != null) {
                info.add(Privilege.LAST_MODIFIED_BY_ATTRIBUTE + "=" +
                    lastModifiedBy);
            }

            long creationDate = p.getCreationDate();
            if (creationDate > 0) {
                String data = Long.toString(creationDate) + "=" +
                    Privilege.CREATION_DATE_ATTRIBUTE;
                info.add(data);
                info.add("|" + data);
            }

            long lastModifiedDate = p.getLastModifiedDate();
            if (lastModifiedDate > 0) {
                String data = Long.toString(lastModifiedDate) + "=" +
                    Privilege.LAST_MODIFIED_DATE_ATTRIBUTE;
                info.add(data);
                info.add("|" + data);
            }

            Entitlement ent = p.getEntitlement();
            info.add(Privilege.APPLICATION_ATTRIBUTE + "=" +
                ent.getApplicationName());
            for (String a : p.getApplicationIndexes()) {
                info.add(Privilege.APPLICATION_ATTRIBUTE + "=" + a);
            }
            map.put("ou", info);

            s.setAttributes(map);
            s.save();

            Map<String, String> params = new HashMap<String, String>();
            params.put(NotificationServlet.ATTR_NAME, privilegeName);
            params.put(NotificationServlet.ATTR_REALM_NAME, realm);
            Notifier.submit(NotificationServlet.PRIVILEGE_ADDED,
                params);
            updateIndexCount(realm, 1, false);
        } catch (JSONException e) {
            throw new EntitlementException(210, e);
        } catch (SSOException e) {
            throw new EntitlementException(210, e);
        } catch (SMSException e) {
            throw new EntitlementException(210, e);
        }
        return dn;
    }
    /**
     * Adds a referral.
     *
     * @param adminSubject Admin Subject who has the rights to write to
     *        datastore.
     * @param realm Realm name.
     * @param referral Referral Privilege object.
     * @return the DN of added privilege.
     * @throws EntitlementException if privilege cannot be added.
     */
    public String addReferral(
        Subject adminSubject,
        String realm,
        ReferralPrivilege referral
    ) throws EntitlementException {
        ResourceSaveIndexes indexes = referral.getResourceSaveIndexes(
            adminSubject, realm);
        SSOToken token = getSSOToken(adminSubject);
        String dn = null;
        try {
            createDefaultSubConfig(token, realm, REFERRAL_STORE);
            dn = getPrivilegeDistinguishedName(referral.getName(), realm,
                REFERRAL_STORE);

            SMSEntry s = new SMSEntry(token, dn);
            Map<String, Set<String>> map = new HashMap<String, Set<String>>();

            Set<String> searchable = new HashSet<String>();
            map.put(SMSEntry.ATTR_XML_KEYVAL, searchable);

            if (indexes != null) {
                for (String i : indexes.getHostIndexes()) {
                    searchable.add(HOST_INDEX_KEY + "=" + i);
                }
                for (String i : indexes.getPathIndexes()) {
                    searchable.add(PATH_INDEX_KEY + "=" + i);
                }
                for (String i : indexes.getParentPathIndexes()) {
                    searchable.add(PATH_PARENT_INDEX_KEY + "=" + i);
                }
            }

            Set<String> setServiceID = new HashSet<String>(2);
            map.put(SMSEntry.ATTR_SERVICE_ID, setServiceID);
            setServiceID.add("indexes");

            Set<String> set = new HashSet<String>(2);
            map.put(SMSEntry.ATTR_KEYVAL, set);
            set.add(SERIALIZABLE_INDEX_KEY + "=" + referral.toJSON());

            Set<String> setObjectClass = new HashSet<String>(4);
            map.put(SMSEntry.ATTR_OBJECTCLASS, setObjectClass);
            setObjectClass.add(SMSEntry.OC_TOP);
            setObjectClass.add(SMSEntry.OC_SERVICE_COMP);

            Set<String> info = new HashSet<String>(8);

            String privilegeName = referral.getName();
            if (privilegeName != null) {
                info.add(Privilege.NAME_ATTRIBUTE + "=" + privilegeName);
            }

            String privilegeDesc = referral.getDescription();
            if (privilegeDesc != null) {
                info.add(Privilege.DESCRIPTION_ATTRIBUTE + "=" + privilegeDesc);
            }

            String createdBy = referral.getCreatedBy();
            if (createdBy != null) {
                info.add(Privilege.CREATED_BY_ATTRIBUTE + "=" + createdBy);
            }

            String lastModifiedBy = referral.getLastModifiedBy();
            if (lastModifiedBy != null) {
                info.add(Privilege.LAST_MODIFIED_BY_ATTRIBUTE + "=" +
                    lastModifiedBy);
            }

            long creationDate = referral.getCreationDate();
            if (creationDate > 0) {
                String data = Long.toString(creationDate) + "=" +
                    Privilege.CREATION_DATE_ATTRIBUTE;
                info.add(data);
                info.add("|" + data);
            }

            long lastModifiedDate = referral.getLastModifiedDate();
            if (lastModifiedDate > 0) {
                String data = Long.toString(lastModifiedDate) + "=" +
                    Privilege.LAST_MODIFIED_DATE_ATTRIBUTE;
                info.add(data);
                info.add("|" + data);
            }

            for (String rlm : referral.getRealms()) {
                info.add(REFERRAL_REALMS + "=" + rlm);
            }
            for (String n : referral.getApplicationTypeNames(adminSubject,
                realm)) {
                info.add(REFERRAL_APPLS + "=" + n);
            }
            for (String n : referral.getMapApplNameToResources().keySet()) {
                info.add(Privilege.APPLICATION_ATTRIBUTE + "=" + n);
            }
            map.put("ou", info);

            s.setAttributes(map);
            s.save();

            Map<String, String> params = new HashMap<String, String>();
            params.put(NotificationServlet.ATTR_NAME, privilegeName);
            params.put(NotificationServlet.ATTR_REALM_NAME, realm);
            Notifier.submit(NotificationServlet.REFERRAL_ADDED,
                params);
            updateIndexCount(realm, 1, true);
        } catch (SSOException e) {
            throw new EntitlementException(270, e);
        } catch (SMSException e) {
            throw new EntitlementException(270, e);
        }
        return dn;
    }

    /**
     * Removes privilege.
     *
     * @param adminSubject Admin Subject who has the rights to write to
     *        datastore.
     * @param realm Realm name.
     * @param name Privilege name.
     * @throws com.sun.identity.entitlement.EntitlementException if privilege
     * cannot be removed.
     */
    public void remove(
        Subject adminSubject,
        String realm,
        String name
    ) throws EntitlementException {
        SSOToken token = getSSOToken(adminSubject);

        if (token == null) {
            Object[] arg = {name};
            throw new EntitlementException(55, arg);
        }

        String dn = null;
        try {
            dn = getPrivilegeDistinguishedName(name, realm, null);

            if (SMSEntry.checkIfEntryExists(dn, token)) {
                SMSEntry s = new SMSEntry(token, dn);
                s.delete();
                updateIndexCount(realm, -1, false);

                Map<String, String> params = new HashMap<String, String>();
                params.put(NotificationServlet.ATTR_NAME, name);
                params.put(NotificationServlet.ATTR_REALM_NAME, realm);
                Notifier.submit(NotificationServlet.PRIVILEGE_DELETED,
                    params);
            }
        } catch (SMSException e) {
            Object[] arg = {dn};
            throw new EntitlementException(51, arg, e);
        } catch (SSOException e) {
            throw new EntitlementException(10, null, e);
        }

    }

    /**
     * Removes referral privilege.
     *
     * @param adminSubject Admin Subject who has the rights to write to
     *        datastore.
     * @param realm Realm name.
     * @param name Referral privilege name.
     * @throws EntitlementException if privilege cannot be removed.
     */
    public void removeReferral(
        Subject adminSubject,
        String realm,
        String name
    ) throws EntitlementException {
        SSOToken token = getSSOToken(adminSubject);

        if (token == null) {
            Object[] arg = {name};
            throw new EntitlementException(55, arg);
        }

        String dn = null;
        try {
            dn = getPrivilegeDistinguishedName(name, realm, REFERRAL_STORE);

            if (SMSEntry.checkIfEntryExists(dn, token)) {
                SMSEntry s = new SMSEntry(token, dn);
                s.delete();
                updateIndexCount(realm, -1, true);

                Map<String, String> params = new HashMap<String, String>();
                params.put(NotificationServlet.ATTR_NAME, name);
                params.put(NotificationServlet.ATTR_REALM_NAME, realm);

                ReferredApplicationManager.getInstance().clearCache();
                Notifier.submit(NotificationServlet.REFERRAL_DELETED,
                    params);
            }
        } catch (SMSException e) {
            Object[] arg = {dn};
            throw new EntitlementException(51, arg, e);
        } catch (SSOException e) {
            throw new EntitlementException(10, null, e);
        }
    }

    /**
     * Returns a set of privilege names that satifies a search filter.
     *
     * @param adminSubject Subject who has the rights to read datastore.
     * @param realm Realm name
     * @param filter Search filter.
     * @param numOfEntries Number of max entries.
     * @param sortResults <code>true</code> to have result sorted.
     * @param ascendingOrder <code>true</code> to have result sorted in
     * ascending order.
     * @return a set of privilege names that satifies a search filter.
     * @throws EntitlementException if search failed.
     */
    public Set<String> search(
        Subject adminSubject,
        String realm,
        String filter,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws EntitlementException {
        Set<String> results = new HashSet<String>();

        try {
            SSOToken token = getSSOToken(adminSubject);

            if (token == null) {
                throw new EntitlementException(216);
            }

            String baseDN = getSearchBaseDN(realm, null);

            if (SMSEntry.checkIfEntryExists(baseDN, token)) {
                Set<String> dns = SMSEntry.search(token, baseDN, filter,
                    numOfEntries, 0, sortResults, ascendingOrder);
                for (String dn : dns) {
                    if (!areDNIdentical(baseDN, dn)) {
                        String rdns[] = LDAPDN.explodeDN(dn, true);
                        if ((rdns != null) && rdns.length > 0) {
                            results.add(rdns[0]);
                        }
                    }
                }
            } else {
                return Collections.EMPTY_SET;
            }
        } catch (SMSException ex) {
            throw new EntitlementException(215, ex);
        }
        return results;
    }
    
    /**
     * Returns a set of referral privilege names that satifies a search filter.
     *
     * @param adminSubject Subject who has the rights to read datastore.
     * @param realm Realm name
     * @param filter Search filter.
     * @param numOfEntries Number of max entries.
     * @param sortResults <code>true</code> to have result sorted.
     * @param ascendingOrder <code>true</code> to have result sorted in
     * ascending order.
     * @return a set of privilege names that satifies a search filter.
     * @throws EntityExistsException if search failed.
     */
    public Set<String> searchReferral(
        Subject adminSubject,
        String realm,
        String filter,
        int numOfEntries,
        boolean sortResults,
        boolean ascendingOrder
    ) throws EntitlementException {
        Set<String> results = new HashSet<String>();

        try {
            SSOToken token = getSSOToken(adminSubject);

            if (token == null) {
                throw new EntitlementException(216);
            }

            String baseDN = getSearchBaseDN(realm, REFERRAL_STORE);

            if (SMSEntry.checkIfEntryExists(baseDN, token)) {
                Set<String> dns = SMSEntry.search(token, baseDN, filter,
                    numOfEntries, 0, sortResults, ascendingOrder);
                for (String dn : dns) {
                    if (!areDNIdentical(baseDN, dn)) {
                        String rdns[] = LDAPDN.explodeDN(dn, true);
                        if ((rdns != null) && rdns.length > 0) {
                            results.add(rdns[0]);
                        }
                    }
                }
            } else {
                return Collections.EMPTY_SET;
            }
        } catch (SMSException ex) {
            throw new EntitlementException(215, ex);
        }
        return results;
    }

    private static boolean areDNIdentical(String dn1, String dn2) {
        DN dnObj1 = new DN(dn1);
        DN dnObj2 = new DN(dn2);
        return dnObj1.equals(dnObj2);
    }

     public boolean hasPrivilgesWithApplication(
        Subject adminSubject,
        String realm,
        String applName
    ) throws EntitlementException {
        SSOToken token = getSSOToken(adminSubject);

         //Search privilege
         String filter = "(ou=" + Privilege.APPLICATION_ATTRIBUTE + "=" +
             applName + ")";
         String baseDN = getSearchBaseDN(realm, null);
         if (hasEntries(token, baseDN, filter)) {
             return true;
         }

         //Search referral privilege
         baseDN = getSearchBaseDN(realm, REFERRAL_STORE);
         if (hasEntries(token, baseDN, filter)) {
             return true;
         }
         
         //Search delegation privilege
         baseDN = getSearchBaseDN(getHiddenRealmDN(), null);
         if (hasEntries(token, baseDN, filter)) {
             return true;
         }

         return false;
    }

     private static String getHiddenRealmDN() {
        return HIDDEN_REALM_DN + SMSEntry.getRootSuffix();
    }

    private boolean hasEntries(SSOToken token, String baseDN, String filter)
        throws EntitlementException {
         if (SMSEntry.checkIfEntryExists(baseDN, token)) {
             try {
                 Set<String> dns = SMSEntry.search(token, baseDN, filter,
                     0, 0, false, false);
                 if ((dns != null) && !dns.isEmpty()) {
                     return true;
                 }
             } catch (SMSException e) {
                 Object[] arg = {baseDN};
                 throw new EntitlementException(52, arg, e);
             }
         }
         return false;
    }


    /**
     * Returns a set of privilege that satifies the resource and subject
     * indexes.
     *
     * @param adminSubject Subject who has the rights to read datastore.
     * @param realm Realm name
     * @param iterator Buffered iterator to have the result fed to it.
     * @param indexes Resource search indexes.
     * @param subjectIndexes Subject search indexes.
     * @param bSubTree <code>true</code> to do sub tree search
     * @param excludeDNs Set of DN to be excluded from the search results.
     * @return a set of privilege that satifies the resource and subject
     * indexes.
     */
    public Set<IPrivilege> search(
        Subject adminSubject,
        String realm,
        BufferedIterator iterator,
        ResourceSearchIndexes indexes,
        Set<String> subjectIndexes,
        boolean bSubTree,
        Set<String> excludeDNs
    ) throws EntitlementException {
        SSOToken token = getSSOToken(adminSubject);
        Set<IPrivilege> results = searchPrivileges(realm,
            iterator, indexes, subjectIndexes, bSubTree, excludeDNs);
        // Get referrals only if count is greater than 0
        int countInt = getNumberOfReferrals(realm);
        if (countInt > 0) {
            results.addAll(searchReferral(token, realm, iterator,
                indexes, bSubTree, excludeDNs));
        }
        return results;
    }

    private Set<IPrivilege> searchPrivileges(
        String realm,
        BufferedIterator iterator,
        ResourceSearchIndexes indexes,
        Set<String> subjectIndexes,
        boolean bSubTree,
        Set<String> excludeDNs
    ) throws EntitlementException {
        Set<IPrivilege> results = new HashSet<IPrivilege>();
        String filter = getFilter(indexes, subjectIndexes, bSubTree);
        String baseDN = getSearchBaseDN(realm, null);

        if (PrivilegeManager.debug.messageEnabled()) {
            PrivilegeManager.debug.message(
                "[PolicyEval] DataStore.searchPrivileges", null);
            PrivilegeManager.debug.message(
                "[PolicyEval] search filter: " + filter, null);
            PrivilegeManager.debug.message(
                "[PolicyEval] search DN: " + baseDN, null);
        }

        if (filter != null) {
            SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());

            long start = DB_MONITOR_PRIVILEGE.start();
            
            if (SMSEntry.checkIfEntryExists(baseDN, token)) {
                try {
                    Iterator i = SMSEntry.search(
                        token, baseDN, filter, 0, 0, false, false, excludeDNs);
                    while (i.hasNext()) {
                        SMSDataEntry e = (SMSDataEntry) i.next();
                        Privilege privilege = Privilege.getInstance(
                            new JSONObject(e.getAttributeValue(
                            SERIALIZABLE_INDEX_KEY)));
                        iterator.add(privilege);
                        results.add(privilege);
                    }
                } catch (JSONException e) {
                    Object[] arg = {baseDN};
                    throw new EntitlementException(52, arg, e);
                } catch (SMSException e) {
                    Object[] arg = {baseDN};
                    throw new EntitlementException(52, arg, e);
                }
            }

            DB_MONITOR_PRIVILEGE.end(start);
        }
        return results;
    }

    /**
     * Returns a set of referral privilege that satifies the resource and
     * subject indexes.
     *
     * @param adminToken Subject who has the rights to read datastore.
     * @param realm Realm name
     * @param iterator Buffered iterator to have the result fed to it.
     * @param indexes Resource search indexes.
     * @param bSubTree <code>true</code> to do sub tree search
     * @param excludeDNs Set of DN to be excluded from the search results.
     * @return a set of privilege that satifies the resource and subject
     * indexes.
     */
    public Set<ReferralPrivilege> searchReferral(
        SSOToken adminToken,
        String realm,
        BufferedIterator iterator,
        ResourceSearchIndexes indexes,
        boolean bSubTree,
        Set<String> excludeDNs
    ) throws EntitlementException {
        Set<ReferralPrivilege> results = new HashSet<ReferralPrivilege>();
        String filter = getFilter(indexes, null, bSubTree);
        String baseDN = getSearchBaseDN(realm, REFERRAL_STORE);

        if (PrivilegeManager.debug.messageEnabled()) {
            PrivilegeManager.debug.message(
                "[PolicyEval] DataStore.searchReferral", null);
            PrivilegeManager.debug.message(
                "[PolicyEval] search filter: " + filter, null);
            PrivilegeManager.debug.message(
                "[PolicyEval] search DN: " + baseDN, null);
        }

        if (filter != null) {
            SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            long start = DB_MONITOR_REFERRAL.start();

            if (SMSEntry.checkIfEntryExists(baseDN, token)) {
                try {
                    Iterator i = SMSEntry.search(
                        token, baseDN, filter, 0, 0, false, false, excludeDNs);
                    while (i.hasNext()) {
                        SMSDataEntry e = (SMSDataEntry) i.next();
                        ReferralPrivilege referral = ReferralPrivilege.
                            getInstance(
                            new JSONObject(e.getAttributeValue(
                            SERIALIZABLE_INDEX_KEY)));
                        iterator.add(referral);
                        results.add(referral);
                    }
                    iterator.isDone();
                } catch (JSONException e) {
                    Object[] arg = {baseDN};
                    throw new EntitlementException(52, arg, e);
                } catch (SMSException e) {
                    Object[] arg = {baseDN};
                    throw new EntitlementException(52, arg, e);
                }
            }

            DB_MONITOR_REFERRAL.end(start);
        }
        return results;
    }

    static String getFilter(
        ResourceSearchIndexes indexes,
        Set<String> subjectIndexes,
        boolean bSubTree
    ) {
        StringBuilder filter = new StringBuilder();

        StringBuilder subjectBuffer = new StringBuilder();
        if ((subjectIndexes != null) && !subjectIndexes.isEmpty()) {
            for (String i : subjectIndexes) {
                Object[] o = {i};
                subjectBuffer.append(
                    MessageFormat.format(SUBJECT_FILTER_TEMPLATE, o));
            }
        }
        if (subjectBuffer.length() > 0) {
            filter.append("(|").append(subjectBuffer.toString()).append(")");
        }

        Set<String> hostIndexes = indexes.getHostIndexes();
        StringBuilder hostBuffer = new StringBuilder();
        if ((hostIndexes != null) && !hostIndexes.isEmpty()) {
            for (String h : indexes.getHostIndexes()) {
                Object[] o = {h};
                hostBuffer.append(MessageFormat.format(
                    HOST_FILTER_TEMPLATE, o));
            }
        }
        if (hostBuffer.length() > 0) {
            filter.append("(|").append(hostBuffer.toString()).append(")");
        }

        StringBuilder pathBuffer = new StringBuilder();
        Set<String> pathIndexes = indexes.getPathIndexes();

        if ((pathIndexes != null) && !pathIndexes.isEmpty()) {
            for (String p : pathIndexes) {
                Object[] o = {p};
                pathBuffer.append(MessageFormat.format(
                    PATH_FILTER_TEMPLATE, o));
            }
        }

        if (bSubTree) {
            Set<String> parentPathIndexes = indexes.getParentPathIndexes();
            if ((parentPathIndexes != null) && !parentPathIndexes.isEmpty()) {
                for (String p : parentPathIndexes) {
                    Object[] o = {p};
                    pathBuffer.append(MessageFormat.format(
                        PATH_PARENT_FILTER_TEMPLATE, o));
                }
            }
        }
        if (pathBuffer.length() > 0) {
            filter.append("(|").append(pathBuffer.toString()).append(")");
        }

        String result = filter.toString();
        return (result.length() > 0) ? "(&" + result + ")" : null;
    }


    public Set<ReferralPrivilege> searchReferrals(
        SSOToken adminToken,
        String realm,
        String filter
    ) throws EntitlementException {
        Set<ReferralPrivilege> results = new HashSet<ReferralPrivilege>();
        String baseDN = getSearchBaseDN(realm, REFERRAL_STORE);

        if (SMSEntry.checkIfEntryExists(baseDN, adminToken)) {
            try {
                Iterator i = SMSEntry.search(
                    adminToken, baseDN, filter, 0, 0, false, false,
                    Collections.EMPTY_SET);
                while (i.hasNext()) {
                    SMSDataEntry e = (SMSDataEntry) i.next();
                    ReferralPrivilege referral = ReferralPrivilege.getInstance(
                        new JSONObject(e.getAttributeValue(
                        SERIALIZABLE_INDEX_KEY)));
                    results.add(referral);
                }
            } catch (JSONException e) {
                Object[] arg = {baseDN};
                throw new EntitlementException(52, arg, e);
            } catch (SMSException e) {
                Object[] arg = {baseDN};
                throw new EntitlementException(52, arg, e);
            }
        }
        return results;
    }

    private SSOToken getSSOToken(Subject subject) {
        if (subject == PrivilegeManager.superAdminSubject) {
            return adminToken;
        }
        return SubjectUtils.getSSOToken(subject);
    }

    static Set<String> getReferralNames(String realm, String referredRealm)
        throws EntitlementException {
        try {
            Set<String> results = new HashSet<String>();
            String filter = "(ou=" + REFERRAL_REALMS + "=" + 
                DNMapper.orgNameToRealmName(referredRealm) + ")";
            String baseDN = getSearchBaseDN(realm, REFERRAL_STORE);

            if (SMSEntry.checkIfEntryExists(baseDN, adminToken)) {
                Set<String> dns = SMSEntry.search(adminToken, baseDN, filter,
                    0, 0, false, false);
                for (String dn : dns) {
                    if (!areDNIdentical(baseDN, dn)) {
                        String rdns[] = LDAPDN.explodeDN(dn, true);
                        if ((rdns != null) && rdns.length > 0) {
                            results.add(rdns[0]);
                        }
                    }
                }
            }
            return results;
        } catch (SMSException ex) {
            throw new EntitlementException(215, ex);
        }
    }
}
