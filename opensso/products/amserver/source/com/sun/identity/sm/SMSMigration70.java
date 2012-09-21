/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SMSMigration70.java,v 1.5 2008/07/11 01:46:20 arviranga Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPrivilege;

public class SMSMigration70 {

    private static String orgNamingAttr;

    public static void main(String args[]) {
    }

    public static void migrate63To70(SSOToken token, String entryDN) {
        try {

            if (ServiceManager.isRealmEnabled()) {
                System.out.println(
                        "\n\n\nSMSMigration70::main() : Realms enabled. " +
                        "\n\nService Management Migration to "
                                + "7.0 cannot be done. ");
                throw (new Exception(
                        "\n\n\nSMSMigration70::main() : Realms enabled. " +
                        "\n\nService Management Migration to " +
                        "7.0 cannot be done."));
            }

            // Add AMSDK plugin to root realm before migration of sub-realms
            // This is similar to SMSEntry.baseDN.
            entryDN = entryDN.toLowerCase();
            addIdRepoAMSDKPlugin(token, entryDN);

            // Add these organization attributes from root org to root realm.
            Map addMap = getOrgAttributes(token, entryDN);
            String rootRealmDN = SMSEntry.SERVICES_RDN + SMSEntry.COMMA
                    + entryDN;
            OrganizationConfigManager ocmAddAttr =
                new OrganizationConfigManager(token, rootRealmDN);
            ocmAddAttr.setAttributes("sunidentityrepositoryservice", addMap);

            // Migrate sub-orgs to realms
            migrateToRealms(token, entryDN);

            // After migration of config data, Set the realmEnabled/realmMode
            // flag to true in the Global Schema of the identity repository
            // service.
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                ServiceManager.REALM_SERVICE, token);
            ServiceSchema gss = ssm.getGlobalSchema();
            if (gss != null) {
                Map attrSet = new HashMap(2);
                Set realmValue = new HashSet(2);
                realmValue.add("true");
                attrSet.put(ServiceManager.REALM_ATTR_NAME, realmValue);
                Set coExistValue = new HashSet(2);
                coExistValue.add("false");
                attrSet.put(ServiceManager.COEXISTENCE_ATTR_NAME, coExistValue);
                gss.setAttributeDefaults(attrSet);
            }

            // After migration of config data, Set the realmEnabled/realmMode
            // flag to true
            ServiceConfigManager scm = new ServiceConfigManager(
                    ServiceManager.REALM_SERVICE, token);
            ServiceConfig sc = scm.getGlobalConfig(null);
            if (sc != null) {
                Map map = new HashMap(2);
                Set realmSet = new HashSet(2);
                realmSet.add("true");
                map.put(ServiceManager.REALM_ATTR_NAME, realmSet);
                Set coExistSet = new HashSet(2);
                coExistSet.add("false");
                map.put(ServiceManager.COEXISTENCE_ATTR_NAME, coExistSet);
                sc.setAttributes(map);
            }
            ServiceManager.checkFlags(token);
            System.out.println("migrateToRealms.REALM is "
                    + ServiceManager.isRealmEnabled());
            System.out.println("migrateToRealms.COEXISTENCE is "
                    + ServiceManager.isCoexistenceMode());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void migrateToRealms(SSOToken token, String entryDN) {
        try {

            OrganizationConfigManager ocm = new OrganizationConfigManager(
                    token, entryDN);
            Set subOrgNames = ocm.getSubOrganizationNames("*", false);
            if (orgNamingAttr == null) {
                orgNamingAttr = ocm.getNamingAttrForOrg();
            }
            System.out.println("Organization naming attr is " + orgNamingAttr);

            Iterator subOrgs = subOrgNames.iterator();
            System.out.println("SIZE is " + subOrgNames.size());

            while (subOrgs.hasNext()) {
                String org = orgNamingAttr + SMSEntry.EQUALS
                        + (String) subOrgs.next() + SMSEntry.COMMA + entryDN;
                System.out.println("Organization is " + org);

                // Add AMSDK plugin before migrating the config data
                addIdRepoAMSDKPlugin(token, org);

                // Create corresponding realms and migrate the config data
                migrateOrganization(token, org);

                // Migrate the delegation policies before migrating config data
                migrateDelegationPolicies(token, org);

                // Look for suborgs too and copy/create subrealms.
                migrateToRealms(token, org);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds IdRepo AMSDK plugin to the given organization name
     */
    protected static void addIdRepoAMSDKPlugin(SSOToken token, String orgName)
            throws SMSException, SSOException {
        ServiceConfigManager scm = new ServiceConfigManager(
                ServiceManager.REALM_SERVICE, token);
        ServiceConfig sc = scm.getOrganizationConfig(orgName, null);
        Map attributes = new HashMap();
        Set values = new HashSet();
        values.add("com.iplanet.am.sdk.AMSDKRepo");
        attributes.put("sunIdRepoClass", values);
        values = new HashSet();
        values
                .add(DNMapper.realmNameToAMSDKName(DNMapper
                        .orgNameToDN(orgName)));
        attributes.put("amSDKOrgName", values);
        sc.addSubConfig("amsdk1", "amSDK", 0, attributes);
    }

    /**
     * Migrate delegation policies to have correct policy name, resource name
     * and subjects
     */
    protected static void migrateDelegationPolicies(SSOToken token,
            String orgName) throws SSOException {

        System.out.println("Migrating delegation policies for org: " + orgName);

        try {
            DelegationManager dm = new DelegationManager(token, orgName);
            Set privileges = dm.getPrivileges();
            Set newPrivileges = new HashSet();
            for (Iterator items = privileges.iterator(); items.hasNext();) {
                DelegationPrivilege dp = (DelegationPrivilege) items.next();
                String name = dp.getName();
                // remove the privilege
                dm.removePrivilege(name);

                Set permissions = dp.getPermissions();
                DelegationPermission perm = null;
                int index = -1;
                for (Iterator perms = permissions.iterator(); perms.hasNext();)
                {
                    perm = (DelegationPermission) perms.next();

                    // change the resource name
                    String resource = perm.getOrganizationName();
                    index = resource.toLowerCase().indexOf(
                            "," + SMSEntry.getRootSuffix());
                    if (index != -1) {
                        resource = resource.substring(0, index)
                                + ","
                                + DNMapper.serviceDN
                                + resource
                                        .substring(index
                                                + SMSEntry.getRootSuffix()
                                                        .length() + 1);
                        perm.setOrganizationName(resource);
                    }
                }

                // change the subject name
                Set subjects = dp.getSubjects();
                Set newSubjects = new HashSet();
                for (Iterator ss = subjects.iterator(); ss.hasNext();) {
                    String subject = (String) ss.next();
                    index = subject.toLowerCase().indexOf(
                            "," + SMSEntry.getRootSuffix());
                    if (index != -1) {
                        subject = subject.substring(0, index)
                                + ","
                                + DNMapper.serviceDN
                                + subject
                                        .substring(index
                                                + SMSEntry.getRootSuffix()
                                                        .length() + 1);
                    }
                    newSubjects.add(subject);
                }
                dp.setSubjects(newSubjects);
                newPrivileges.add(dp);
            }

            // Normalized orgname to realm name
            int index = orgName.toLowerCase().indexOf(
                    "," + SMSEntry.getRootSuffix());
            if (index != -1) {
                orgName = orgName.substring(0, index)
                        + ","
                        + DNMapper.serviceDN
                        + orgName.substring(index + 1
                                + SMSEntry.getRootSuffix().length());
            }
            dm = new DelegationManager(token, orgName);

            // Add the modified privileges
            for (Iterator items = newPrivileges.iterator(); items.hasNext();) {
                DelegationPrivilege dp = (DelegationPrivilege) items.next();
                dm.addPrivilege(dp);
            }
            System.out.println("Delegation Policies for org: " + orgName + "\n"
                    + privileges);
        } catch (DelegationException de) {
            System.out.println("   " + de.getMessage());
        }
    }

    /**
     * Adds these two organization attributes to realm.
     * "sunOrganizationStatus=inetDomainStatus"
     * 
     * From AM point of view, all these 3 attributes in AMSDK Organization serve
     * the purpose of identifying the realm give the alias names.
     * 
     * "sunOrganizationAliases=sunOrganizationAlias"
     * "sunOrganizationAliases=sunPreferredDomain"
     * "sunOrganizationAliases=associatedDomain"
     */
    protected static Map getOrgAttributes(SSOToken token, String org) {
        Map map = new HashMap();
        try {
            OrgConfigViaAMSDK amsdk = new OrgConfigViaAMSDK(token, org, org);
            Set orgStatus = amsdk.getSDKAttributeValue("inetDomainStatus");
            Set orgPrefDom = amsdk.getSDKAttributeValue("sunPreferredDomain");
            Set orgAssocDom = amsdk.getSDKAttributeValue("associatedDomain");
            Set orgAlias = amsdk.getSDKAttributeValue("sunOrganizationAlias");

            orgAlias.addAll(orgPrefDom);
            orgAlias.addAll(orgAssocDom);

            map.put("sunOrganizationStatus", orgStatus);
            map.put("sunOrganizationAliases", orgAlias);

            System.out.println("\n addIdRepoAMSDKPlugin.Org Status &  ");
            System.out.println("addIdRepoAMSDKPlugin.Org Alias. ");

            Iterator its = map.keySet().iterator();
            while (its.hasNext()) {
                String st = (String) its.next();
                System.out.println(st + "=" + map.get(st));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void migrateOrganization(SSOToken token, String org) {

        try {
            String orglc = org.toLowerCase();
            int sdn = orglc.indexOf(SMSEntry.baseDN);
            if (sdn > 0) {
                System.out.println("\n migrateOrganization.Org Name: " + org);

                Map map = getOrgAttributes(token, org);
                String realm = org;
                if (!(orgNamingAttr.equalsIgnoreCase(
                        SMSEntry.ORGANIZATION_RDN))) {
                    String tmp = org.substring(0, sdn);
                    realm = DNMapper.replaceString(tmp, orgNamingAttr
                            + SMSEntry.EQUALS, SMSEntry.ORG_PLACEHOLDER_RDN)
                            + org.substring(sdn);
                }
                System.out.println("\nmigrateOrganization.realm: " + realm);

                String orgDN = SMSEntry.SERVICES_RDN + SMSEntry.COMMA + org;
                System.out
                        .println("\nmigrateOrganization.orgDN Name: " + orgDN);
                CachedSubEntries cse = CachedSubEntries.getInstance(token,
                        orgDN);
                Set subEntries = cse.getSubEntries(token);
                sdn = realm.toLowerCase().indexOf(SMSEntry.baseDN);
                String realmDN = realm.substring(0, sdn)
                        + SMSEntry.SERVICES_RDN + SMSEntry.COMMA
                        + realm.substring(sdn);
                System.out.println("\nmigrateOrganization.RealmDN Name: "
                        + realmDN);
                CreateServiceConfig.createOrganization(token, realmDN);
                // After creation of the realm, Set the DNMapper.migration
                // flag to true to avoid removal of 'ou=services' from the
                // newly formed realm DN.
                DNMapper.migration = true;

                OrganizationConfigManager ocmAddAttrs = 
                    new OrganizationConfigManager(token, realmDN);
                ocmAddAttrs.setAttributes("sunidentityrepositoryservice", map);

                Iterator iter = subEntries.iterator();
                while (iter.hasNext()) {
                    String serviceName = (String) iter.next();
                    System.out.println("\nmigrateOrganization.ServiceName: "
                            + serviceName);
                    // Migrate service config data
                    migrateConfigData(token, realmDN, serviceName, org);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void migrateConfigData(SSOToken token, String realmDN,
            String serviceName, String org) {
        try {
            System.out.println("Initial value. migrateConfigData.REALM is "
                    + ServiceManager.isRealmEnabled());
            System.out.println("initial value. migrateConfigData.COEXISTENCE "
                    + "is " + ServiceManager.isCoexistenceMode());

            OrganizationConfigManager ocmAdd = new OrganizationConfigManager(
                    token, realmDN);
            ServiceConfigManager scmGet = new ServiceConfigManager(serviceName,
                    token);

            System.out.println("\nMigrating Organization Config data");
            ServiceConfig orgServiceConfig = scmGet.getOrganizationConfig(org,
                    null);
            migrateConfigs(token, orgServiceConfig, serviceName, ocmAdd);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void migrateConfigs(SSOToken token, ServiceConfig scGet,
            String serviceName, OrganizationConfigManager ocmAdd) {

        try {
            Map attrResults = scGet.getAttributes();
            Iterator it = attrResults.keySet().iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                System.out.println(s + "=" + attrResults.get(s));
            }
            // create sub-config node
            ServiceConfig newServiceConfig = ocmAdd.addServiceConfig(
                    serviceName, attrResults);
            Set subConfigNames = scGet.getSubConfigNames();
            Iterator itr = subConfigNames.iterator();
            for (int j = 0; itr.hasNext(); j++) {
                String subConfigName = (String) itr.next();
                ServiceConfig oldSubConfig = scGet.getSubConfig(subConfigName);
                String scID = oldSubConfig.getSchemaID();
                if (scID == null || scID.length() == 0) {
                    scID = subConfigName;
                }
                System.out.println("Sub Config Name " + subConfigName);
                System.out.println("Sub ConfigID " + scID);
                Map subConfigMap = oldSubConfig.getAttributes();
                newServiceConfig.addSubConfig(subConfigName, scID, scGet
                        .getPriority(), subConfigMap);
                migrateSubEntries(token, newServiceConfig, oldSubConfig,
                        subConfigName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void migrateSubEntries(SSOToken token,
            ServiceConfig newServiceConfig, ServiceConfig oldSubConfig,
            String subConfigName) {

        try {
            Set subEntryNames = oldSubConfig.getSubConfigNames();
            if (subEntryNames != null && !subEntryNames.isEmpty()) {
                Iterator iter = subEntryNames.iterator();
                for (int k = 0; iter.hasNext(); k++) {
                    String subEntryName = (String) iter.next();
                    System.out.println("Sub Config Name1 " + subEntryName);

                    ServiceConfig subEntryConfig = oldSubConfig
                            .getSubConfig(subEntryName);
                    Map subEntryConfigMap = subEntryConfig.getAttributes();
                    Iterator it1 = subEntryConfigMap.keySet().iterator();
                    while (it1.hasNext()) {
                        String s1 = (String) it1.next();
                        System.out
                                .println(s1 + "=" + subEntryConfigMap.get(s1));
                    }
                    String serviceID = subEntryConfig.getSchemaID();
                    if (serviceID.length() == 0) {
                        serviceID = subEntryName;
                    }
                    System.out.println("serviceID " + serviceID);
                    StringBuilder sb = new StringBuilder(8);

                    String subConfigDN = "ou=" + subEntryName + SMSEntry.COMMA
                            + "ou=" + subConfigName + SMSEntry.COMMA
                            + newServiceConfig.getDN();

                    SMSEntry newsubConfigSMSEntry = new SMSEntry(token,
                            subConfigDN);
                    SMSUtils.setAttributeValuePairs(newsubConfigSMSEntry,
                            subEntryConfigMap, Collections.EMPTY_SET);
                    newsubConfigSMSEntry.addAttribute(SMSEntry.ATTR_SERVICE_ID,
                            serviceID);
                    newsubConfigSMSEntry.addAttribute(SMSEntry.ATTR_PRIORITY,
                            sb.append(oldSubConfig.getPriority()).toString());
                    newsubConfigSMSEntry
                            .addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                                    SMSEntry.OC_SERVICE_COMP);
                    newsubConfigSMSEntry.addAttribute(
                            SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);

                    newsubConfigSMSEntry.save(token);
                    CachedSMSEntry cachedE = CachedSMSEntry.getInstance(token,
                            newsubConfigSMSEntry.getDN());
                    if (cachedE.isDirty()) {
                        cachedE.refresh();
                    }
                    cachedE.refresh(newsubConfigSMSEntry);
                    // oldSubConfig = subEntryConfig;
                    migrateSubEntries(token, newServiceConfig, subEntryConfig,
                            subEntryName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
