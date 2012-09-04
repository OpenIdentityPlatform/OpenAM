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
 * $Id: OrgConfigViaAMSDK.java,v 1.14 2009/11/20 23:52:56 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMNamingAttrManager;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.common.DNUtils;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

// This class provides support for OrganizationConfigManager
// in coexistence mode. This class interfaces with AMSDK
// to manage organization names and organization attributes.
public class OrgConfigViaAMSDK {

    // Instance variables
    private SSOToken token;

    private String parentOrgName;

    private String smsOrgName;

    private AMOrganization parentOrg;

    private AMOrganization parentOrgWithAdminToken;

    private ServiceConfig serviceConfig;
    
    private int objType;

    // permissions for the user token
    boolean hasReadPermissionOnly;

    // Cache of organization names to ServiceConfig that
    // contains the attribute mappings
    static Map attributeMappings = new CaseInsensitiveHashMap();

    static Map reverseAttributeMappings = new CaseInsensitiveHashMap();

    static Map attributeMappingServiceConfigs = new HashMap();

    static final String IDREPO_SERVICE = "sunidentityrepositoryservice";

    static final String MAPPING_ATTR_NAME = "sunCoexistenceAttributeMapping";
    
    // Cache of AMSDK organization names to SMS relam dn
    static Map amsdkdn2realmname = new CaseInsensitiveHashMap();
    
    static Map amsdkConfiguredRealms = new CaseInsensitiveHashMap();

    // Debug & Locale
    static Debug debug = SMSEntry.debug;

    ResourceBundle bundle = SMSEntry.bundle;

    // When DIT not migrated to AM 7.0 we need to use static mapping
    static Map notMigratedAttributeMappings;

    static Map notMigratedReverseAttributeMappings;
    
    static {
        if (!ServiceManager.isConfigMigratedTo70()) {
            notMigratedAttributeMappings = new CaseInsensitiveHashMap();
            notMigratedAttributeMappings.put("sunPreferredDomain",
                    "sunPreferredDomain");
            notMigratedAttributeMappings.put("sunOrganizationStatus",
                    "inetDomainStatus");
            notMigratedAttributeMappings.put("sunOrganizationAliases",
                    "sunOrganizationAlias");
            notMigratedAttributeMappings.put("sunDNSAliases",
                    "associatedDomain");
            notMigratedReverseAttributeMappings = new CaseInsensitiveHashMap();
            notMigratedReverseAttributeMappings.put("sunPreferredDomain",
                    "sunPreferredDomain");
            notMigratedReverseAttributeMappings.put("inetDomainStatus",
                    "sunOrganizationStatus");
            notMigratedReverseAttributeMappings.put("sunOrganizationAlias",
                    "sunOrganizationAliases");
            notMigratedReverseAttributeMappings.put("associatedDomain",
                    "sunDNSAliases");
        }
    }

    /**
     * Constructor for Realm management via AMSDK The parameter
     * <code>orgName</code> must be LDAP organization name
     */
    OrgConfigViaAMSDK(SSOToken token, String orgName, String smsOrgName)
            throws SMSException {
        this.token = token;
        parentOrgName = orgName;
        this.smsOrgName = smsOrgName;
        
        // Get admin SSOToken for operations to bypass ACIs and delegation
        SSOToken adminToken = (SSOToken) AccessController
            .doPrivileged(AdminTokenAction.getInstance());

        try {
            // Check if the user has realm privileges, if yes use
            // admin SSOToken to bypass directory ACIs.
            // Look if the incoming request is from client or server.
            // If client,(SMSJAXRPCObjectFlg=true), and since it is a JAXRPC
            // call, the permission checking would be done at the server.
            // So client need not have this check.(checkRealmPermission)
            if (!SMSEntry.SMSJAXRPCObjectFlg) {
                if (checkRealmPermission(token, smsOrgName,
                    SMSEntry.modifyActionSet)) {
                    token = adminToken;
                } else if (checkRealmPermission(token, smsOrgName,
                    SMSEntry.readActionSet)) {
                    hasReadPermissionOnly = true;
                }
            }
            AMStoreConnection amcom = new AMStoreConnection(token);
            parentOrg = amcom.getOrganization(orgName);

            if (hasReadPermissionOnly) {
                // Construct parent org with admin token for reads
                amcom = new AMStoreConnection(adminToken);
                parentOrgWithAdminToken = amcom.getOrganization(orgName);
            }

            // Get the Realm <---> LDAP Org attribute mappings.
            // To get the service config of idrepo service.
            String newOrg = orgName;
            if (!SMSEntry.getRootSuffix().equalsIgnoreCase(
                SMSEntry.getAMSdkBaseDN())) {
                newOrg = smsOrgName;
            }

            if (ServiceManager.isConfigMigratedTo70() &&
                (serviceConfig = (ServiceConfig) attributeMappingServiceConfigs
                .get(orgName)) == null) {
                ServiceConfigManager scm = new ServiceConfigManager(
                    IDREPO_SERVICE, adminToken);
                // Do we need to use internal token?
                serviceConfig = scm.getOrganizationConfig(newOrg, null);
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK::constructor"
                        + ": serviceConfig" + serviceConfig);
                }
                attributeMappingServiceConfigs.put(orgName, serviceConfig);
            }
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Create a suborganization using AMSDK. The code checks if the DIT has been
     * migrated to AM 7.0 to add the objectclass "sunRelamService".
     */
    void createSubOrganization(String subOrgName) throws SMSException {
        // Check if suborg exists
        if (!getSubOrganizationNames(subOrgName, false).isEmpty()
                || subOrgName.startsWith(SMSEntry.SUN_INTERNAL_REALM_NAME)) {
            // Sub-org already exists or it is a hidden realm
            return;
        }

        // Create the organization
        try {
            if (ServiceManager.isConfigMigratedTo70()) {
                Map attrs = new HashMap();
                Set attrValues = new HashSet();
                attrValues.add(SMSEntry.OC_REALM_SERVICE);
                attrs.put(SMSEntry.ATTR_OBJECTCLASS, attrValues);
                Map subOrgs = new HashMap();
                subOrgs.put(subOrgName, attrs);
                parentOrg.createSubOrganizations(subOrgs);
            } else {
                Set subOrgs = new HashSet();
                subOrgs.add(subOrgName);
                parentOrg.createSubOrganizations(subOrgs);
            }
        } catch (AMException ame) {
            // Ignore if it is Organization already exists
            if (!ame.getErrorCode().equals("474")) {
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK::createSubOrganization"
                            + ": failed with AMException", ame);
                }
                throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame
                        .getMessage(), ame, ame.getMessage()));
            }
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Returns the set of assigned services for the organization
     */
    Set getAssignedServices() throws SMSException {
        try {
            if (hasReadPermissionOnly) {
                return (parentOrgWithAdminToken.getRegisteredServiceNames());
            } else {
                return (parentOrg.getRegisteredServiceNames());
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK::getAssignedServices"
                        + ": failed with AMException", ame);
            }
            throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame.getMessage(),
                    ame, ame.getMessage()));
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Assigns the service to the organization
     */
    void assignService(String serviceName) throws SMSException {
        try {
            // Check if it is a hidden realm
            if (ServiceManager.isCoexistenceMode() &&
                (parentOrgName.startsWith(SMSEntry.SUN_INTERNAL_REALM_PREFIX)))
            {
                return;
            }
            // Check if service is already assigned
            if (!getAssignedServices().contains(serviceName)) {
                parentOrg.registerService(serviceName, false, false);
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK::assignService"
                        + ": failed with AMException", ame);
            }
            throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame.getMessage(),
                    ame, ame.getMessage()));
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Unassigns the service from the organization
     */
    void unassignService(String serviceName) throws SMSException {
        try {
            // Check if service is already unassigned
            if (getAssignedServices().contains(serviceName)) {
                parentOrg.unregisterService(serviceName);
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK::unassignService"
                        + ": failed with AMException", ame);
            }
            throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame.getMessage(),
                    ame, ame.getMessage()));
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Returns sub-organization names using AMSKK APIs. The returned names are
     * in "/" separated format and are normailized using DNMapper.
     */
    Set getSubOrganizationNames(String pattern, boolean recursive)
            throws SMSException {
        try {
            // Search for sub-organization names
            Set subOrgDNs;
            if (hasReadPermissionOnly) {
                subOrgDNs = parentOrgWithAdminToken.searchSubOrganizations(
                        pattern, recursive ? AMConstants.SCOPE_SUB
                                : AMConstants.SCOPE_ONE);
            } else {
                subOrgDNs = parentOrg.searchSubOrganizations(pattern,
                        recursive ? AMConstants.SCOPE_SUB
                                : AMConstants.SCOPE_ONE);
            }
            // Convert DNs to "/" seperated relam names
            if (subOrgDNs != null && !subOrgDNs.isEmpty()) {
                Set subOrgs = new HashSet();
                for (Iterator items = subOrgDNs.iterator(); items.hasNext();) {
                    subOrgs.add(DNMapper.orgNameToDN((String) items.next()));
                }
                return SMSEntry.parseResult(subOrgs, smsOrgName);
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK::getSubOrganizationNames"
                        + ": failed with AMException", ame);
            }
            throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame.getMessage(),
                    ame, ame.getMessage()));
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
        return (Collections.EMPTY_SET);
    }

    /**
     * Deletes sub-organiation using AMSDK. If recursive flag is set, then all
     * sub-entries are also removed. Else if sub-entries are present this will
     * throw an exception.
     */
    void deleteSubOrganization(String subOrgName) throws SMSException {
        try {
            // Check if subOrgName is empty or null
            if (subOrgName == null || subOrgName.trim().length() == 0) {
                if (parentOrg.isExists()) {
                    parentOrg.delete(true);
                }
                return;
            }

            // Check if it is a hidden realm
            if (subOrgName.startsWith(SMSEntry.SUN_INTERNAL_REALM_NAME)) {
                return;
            }

            // Get the suborg DN
            Set subOrgDNs = parentOrg.searchSubOrganizations(subOrgName,
                    AMConstants.SCOPE_ONE);
            if (subOrgDNs != null && !subOrgDNs.isEmpty()) {
                for (Iterator items = subOrgDNs.iterator(); items.hasNext();) {
                    String dn = (String) items.next();
                    AMOrganization subOrg = parentOrg.getSubOrganization(dn);
                    if (subOrg != null) {
                        subOrg.delete(true);
                    }
                }
            } else {
                AMOrganization subOrg = parentOrg
                        .getSubOrganization(subOrgName);
                if (subOrg != null) {
                    subOrg.delete(true);
                }
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK::deleteSubOrganization"
                        + ": failed with AMException", ame);
            }
            throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame.getMessage(),
                    ame, ame.getMessage()));
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Returns the AMSDK Organization attributes. The return attributes are
     * defined in the IdRepo service and can be configured per organization.
     */
    Map getAttributes() throws SMSException {
        Map answer = null;
        try {
            // Get the list of attribute names
            Map attrMapping = getReverseAttributeMapping();
            Set attrNames = attrMapping.keySet();
            if (!attrNames.isEmpty()) {
                // Perform AMSDK search
                Map attributes;
                if (hasReadPermissionOnly) {
                    attributes = parentOrgWithAdminToken
                            .getAttributes(attrNames);
                } else {
                    attributes = parentOrg.getAttributes(attrNames);
                }
                if (attributes != null && !attributes.isEmpty()) {
                    // Do reverse name mapping, and copy to answer
                    for (Iterator items = attributes.keySet().iterator(); items
                            .hasNext();) {
                        String key = (String) items.next();
                        Set values = (Set) attributes.get(key);
                        if (values != null && !values.isEmpty()) {
                            if (answer == null) {
                                answer = new HashMap();
                            }
                            answer.put(attrMapping.get(key), values);
                        }
                    }
                }
            }
        } catch (AMException ame) {
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK::getAttributes"
                        + ": failed with AMException", ame);
            }
            throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame.getMessage(),
                    ame, ame.getMessage()));
        } catch (SSOException ssoe) {
            throw (new SMSException(bundle.getString("sms-INVALID_SSO_TOKEN"),
                    ssoe, "sms-INVALID_SSO_TOKEN"));
        }
        return (answer == null ? Collections.EMPTY_MAP : answer);
    }

    /**
     * Adds attributes to AMSDK Organization. The organziation attribute names
     * are defined in the IdRepo service.
     */
    void addAttributeValues(String attrName, Set values) throws SMSException {
        // Get the attribute values, add the new values
        // and set the attribute
        if (attrName != null && values != null && !values.isEmpty()) {
            // First get the attribute values, remove the
            // specified valued and then set the attributes
            Map attrs = getAttributes();
            Set origValues = (Set) attrs.get(attrName);
            Set newValues = new HashSet(values);
            if (origValues != null && !origValues.isEmpty()) {
                newValues.addAll(origValues);
            }
            Map newAttrs = new HashMap();
            newAttrs.put(attrName, newValues);
            setAttributes(newAttrs);
        }
    }

    /**
     * Sets attributes to AMSDK Organization. The organziation attribute names
     * are defined in the IdRepo service.
     */
    void setAttributes(Map attributes) throws SMSException {
        Map amsdkAttrs = null;
        // Need to get attributes such as domain name, alias names
        // and org status from attributes and set them.
        // These attributes must be defined in ../idm/xml/idRepoService.xml
        if (attributes != null && !attributes.isEmpty()) {
            Map smsIdRepoAttrs = new CaseInsensitiveHashMap(attributes);
            // Iterate through the attribute mappings
            Map attrs = getAttributeMapping();
            Map existingAttributes = getAttributes();
            if (attrs != null && !attrs.isEmpty()) {
                for (Iterator items = attrs.keySet().iterator(); items
                        .hasNext();) {
                    String key = (String) items.next();
                    Set value = (Set) smsIdRepoAttrs.get(key);
                    if (value != null) {
                        if (amsdkAttrs == null) {
                            amsdkAttrs = new HashMap();
                        }
                        boolean notEmptyFlg = false;
                        if (!value.isEmpty()) {
                            for (Iterator iter = value.iterator(); iter
                                    .hasNext();) {
                                String val = (String) iter.next();
                                // Avoid empty string storage.
                                if (val.length() > 0) {
                                    notEmptyFlg = true;
                                }
                            }
                            if (notEmptyFlg) {
                                amsdkAttrs.put(attrs.get(key), value);
                            }
                        } else {
                            Set existingValues = (Set) existingAttributes
                                    .get(key);
                            if (existingValues != null
                                    && !existingValues.isEmpty()) {
                                amsdkAttrs.put(attrs.get(key), value);
                            }
                        }
                    }
                }
            }
        }

        // Update the organization entry
        if (amsdkAttrs != null) {
            try {
                parentOrg.setAttributes(amsdkAttrs);
                parentOrg.store();
            } catch (AMException ame) {
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK::createSub"
                            + "Organization: failed with AMException", ame);
                }
                throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame
                        .getMessage(), ame, ame.getMessage()));
            } catch (SSOException ssoe) {
                throw (new SMSException(bundle
                        .getString("sms-INVALID_SSO_TOKEN"), ssoe,
                        "sms-INVALID_SSO_TOKEN"));
            }
        }
    }

    /**
     * Removes the specified attribute from AMSDK organization. The organziation
     * attribute names are defined in the IdRepo service.
     */
    void removeAttribute(String attrName) throws SMSException {
        if (attrName == null) {
            return;
        }

        // Get the attribute mapping and removed specified attribute
        Map attrMap = getAttributeMapping();
        String amsdkAttrName = (String) attrMap.get(attrName);
        if (amsdkAttrName != null) {
            HashSet set = new HashSet();
            set.add(amsdkAttrName);
            try {
                parentOrg.removeAttributes(set);
                parentOrg.store();
            } catch (AMException ame) {
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK::removeAttribute"
                            + ": failed with AMException", ame);
                }
                throw (new SMSException(AMSDKBundle.BUNDLE_NAME, ame
                        .getMessage(), ame, ame.getMessage()));
            } catch (SSOException ssoe) {
                throw (new SMSException(bundle
                        .getString("sms-INVALID_SSO_TOKEN"), ssoe,
                        "sms-INVALID_SSO_TOKEN"));
            }
        }
    }

    /**
     * Removes the specified attribute values from AMSDK organization. The
     * organziation attribute names are defined in the IdRepo service.
     */
    void removeAttributeValues(String attrName, Set values) throws SMSException 
    {
        if (attrName != null) {
            // First get the attribute values, remove the
            // specified valued and then set the attributes
            Map attrs = getAttributes();
            Set origValues = (Set) attrs.get(attrName);
            if (origValues != null && !origValues.isEmpty()) {
                Set newValues = new HashSet(origValues);
                newValues.removeAll(values);
                if (newValues.isEmpty()) {
                    removeAttribute(attrName);
                } else {
                    Map newAttrs = new HashMap();
                    newAttrs.put(attrName, newValues);
                    setAttributes(newAttrs);
                }
            }
        }
    }

    /**
     * Returns the SMS attribute name to AMSDK attribute name mappings for the
     * organization
     */
    private Map getAttributeMapping() throws SMSException {
        if (!ServiceManager.isConfigMigratedTo70()) {
            return (notMigratedAttributeMappings);
        }
        // Check the cache
        Map answer = (Map) attributeMappings.get(parentOrgName);
        if (answer != null)
            return (answer);

        // Construct the attribute mappings
        Map attrs = serviceConfig.getAttributes();
        if (attrs != null && !attrs.isEmpty()) {
            Set mapAttrs = (Set) attrs.get(MAPPING_ATTR_NAME);
            if (mapAttrs != null && !mapAttrs.isEmpty()) {
                for (Iterator items = mapAttrs.iterator(); items.hasNext();) {
                    String attrMapping = (String) items.next();
                    String[] maps = DNMapper.splitString(attrMapping);
                    if (answer == null) {
                        answer = new CaseInsensitiveHashMap();
                    }
                    answer.put(maps[0], maps[1]);
                }
            }
        }
        if (answer == null) {
            answer = Collections.EMPTY_MAP;
        }
        // Add to cache
        attributeMappings.put(parentOrgName, answer);
        return (answer);
    }

    /**
     * Returns the AMSDK attribute name to SMS attribute name mappings for the
     * organization
     */
    private Map getReverseAttributeMapping() throws SMSException {
        if (!ServiceManager.isConfigMigratedTo70()) {
            return (notMigratedReverseAttributeMappings);
        }
        // Check the cache
        Map answer = (Map) reverseAttributeMappings.get(parentOrgName);
        if (answer != null)
            return (answer);

        // Get the attribute mapping and reverse it
        Map attrMaps = getAttributeMapping();
        for (Iterator items = attrMaps.entrySet().iterator(); items.hasNext();) 
        {
            Map.Entry entry = (Map.Entry) items.next();
            if (answer == null) {
                answer = new CaseInsensitiveHashMap();
            }
            answer.put(entry.getValue(), entry.getKey().toString());
        }
        if (answer == null) {
            answer = Collections.EMPTY_MAP;
        }
        reverseAttributeMappings.put(parentOrgName, answer);
        return (answer);
    }

    // Check to see if the user has realm permissions
    private boolean checkRealmPermission(SSOToken token, String realm,
        Set action) {
        boolean answer = false;
        if (token != null) {
            try {
                DelegationEvaluator de = new DelegationEvaluator();
                DelegationPermission dp = new DelegationPermission(realm,
                    com.sun.identity.sm.SMSEntry.REALM_SERVICE, "1.0", "*",
                    "*", action, Collections.EMPTY_MAP);
                answer = de.isAllowed(token, dp, null);
            } catch (DelegationException dex) {
                debug.error("OrgConfigViaAMSDK.checkRealmPermission: "
                    + "Got Delegation Exception: ", dex);
            } catch (SSOException ssoe) {
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK.checkRealmPermission: "
                        + "Invalid SSOToken: ", ssoe);
                }
            }
        }
        return (answer);
    }
    
    static String getNamingAttrForOrg() {
        return (ServiceManager.isAMSDKEnabled()) ?
            AMNamingAttrManager.getNamingAttr(AMObject.ORGANIZATION) :
            SMSEntry.ORG_PLACEHOLDER_RDN;
    }
    
    static String getNamingAttrForOrgUnit() {
        return AMNamingAttrManager.getNamingAttr(AMObject.ORGANIZATIONAL_UNIT);
    }
    
    public Set getSDKAttributeValue(String key) {
        Set attrSet = new HashSet();
        try {
            attrSet = parentOrg.getAttribute(key);
        } catch (AMException ame) {
            if (debug.warningEnabled()) {
                debug.warning("OrgConfigViaAMSDK::getSDKAttributeValue"
                    + ": failed with AMException", ame);
            }
        } catch (SSOException ssoe) {
            if (debug.warningEnabled()) {
                debug.warning("OrgConfigViaAMSDK::getSDKAttributeValue"
                    + ": failed with SSOException", ssoe);
            }
        }
        return (attrSet);
    }
    
    /**
     * Clears the cache
     */
    protected static void clearCache() {
        attributeMappings = new CaseInsensitiveHashMap();
        reverseAttributeMappings = new CaseInsensitiveHashMap();
        amsdkdn2realmname = new CaseInsensitiveHashMap();
        amsdkConfiguredRealms = new CaseInsensitiveHashMap();
    }
    
    protected static void updateAMSDKConfiguredRealms(
        String realm, boolean configured) {
        if (!amsdkConfiguredRealms.keySet().contains(realm)) {
            amsdkConfiguredRealms.put(realm, Boolean.valueOf(configured));
        }
    }
    
    /**
     * Returns the true if AMSDK plugin is configured for the realm,
     * else returns false.
     */
    public static boolean isAMSDKConfigured(String realm) {
        if (ServiceManager.isCoexistenceMode()) {
            return (true);
        }
        // Check the cache
        realm = DNUtils.normalizeDN(realm);
        Boolean answer = (Boolean) amsdkConfiguredRealms.get(realm);
        if (answer == null) {
            try {
                SSOToken token = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                OrganizationConfigManagerImpl ocm =
                    OrganizationConfigManagerImpl.getInstance(token, realm);
                String orgname = getAmsdkdn(token, ocm);
                answer = Boolean.valueOf(orgname != null);
            } catch (SSOException ssoe) {
                answer = Boolean.FALSE;
            } catch (SMSException smse) {
                answer = Boolean.FALSE;
            }
            // Update cache
            amsdkConfiguredRealms.put(realm, answer);
        }
        return (answer.booleanValue());
    }
    
    /**
     * Returns the realm name that contains the AMSDK plugin with the
     * given organization dn. The function optionally takes "inrealm"
     * the realm, where the initial search would be done
     * If not found, returns null.
     */
    public static String getRealmForAMSDK(String amsdkdn,
        String inrealm) {
        // If in legacy mode, return amsdkdn
        if (ServiceManager.isCoexistenceMode()) {
            return (amsdkdn);
        }
        String realm = inrealm;
        // Check the cache
        amsdkdn = DNUtils.normalizeDN(amsdkdn);
        // if amsdk was not in DN format then normalizeDN will return null
        if(amsdkdn == null) {
            return null;
        }
        String orgname = (String) amsdkdn2realmname.get(amsdkdn);
        if (orgname != null) {
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK:getRealmForAMSDK " +
                    "from cache: orgdn=" + amsdkdn + " realm=" + orgname);
            }
            return (orgname);
        }
        
        // First check with "inrealm" and then with "amsdkdn"
        OrganizationConfigManagerImpl ocm = null;
        SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            // Check inrealm first
            if (inrealm != null) {
                ocm = OrganizationConfigManagerImpl.getInstance(
                    token, inrealm);
                orgname = getAmsdkdn(token, ocm);
            }
            
            // Need to check for the following conditions before
            // using amsdkdn as the realm name to determine the
            // AMSDK plugin organization name
            // i) "inrealm" is null  (realm name is not provided)
            // ii) orgname != null && !orgname.equals(realm)
            //   (since orgname is not null, AMSDK has been configured
            //   configured for the realm, but it does not match the
            //   provided "amsdkdn", hence need to check for amsdkdn realm
            // iii) !inrealm.equals(amsdkdn)
            //   If same, the check has been done. No need to repeat
            // iv) If the dn starts with ou then the realm for the orgUnit
            //     is hidden. So first replace values of all ou's in the
            //     amsdkdn and then find the realm for it.
            if ((inrealm == null) ||
                ((orgname != null) && !orgname.equals(realm)) ||
                ((orgname != null) &&
                !amsdkdn.equals(DNUtils.normalizeDN(inrealm)))) {
                String dn = hideOrgUnits(amsdkdn);
                ocm = OrganizationConfigManagerImpl.getInstance(
                    token, dn);
                orgname = getAmsdkdn(token, ocm);
                if ((orgname != null) && orgname.equals(amsdkdn)) {
                    realm = ocm.getOrgDN();
                }
            }
        } catch (SMSException sme) {
            // Ignore the exception, since the realm is not present
            // and an explicit search would be done below
        } catch (SSOException ssoe) {
            // Ignore the exception, since the realm is not present
            // and an explicit search would be done below
        }
        
        if (realm != null) {
            amsdkdn2realmname.put(amsdkdn, realm);
            if (debug.messageEnabled()) {
                debug.message("OrgConfigViaAMSDK:getRealmForAMSDK " +
                    "first realm lookup: orgdn=" + amsdkdn +
                    " realm=" + realm);
            }
        } else {
            // If realm is still null, need to search the realm tree
            try {
                ocm = OrganizationConfigManagerImpl.getInstance(token, "/");
                updateAmsdk2RealmNameCache(token, ocm, amsdkdn);
                realm = (String) amsdkdn2realmname.get(amsdkdn);
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK:getRealmForAMSDK " +
                        "full search orgdn=" + amsdkdn + " realm=" + realm);
                }
            } catch (SMSException e) {
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK:getRealmForAMSDK" +
                        " Exception: ", e);
                }
            } catch (SSOException ssoe) {
                if (debug.messageEnabled()) {
                    debug.message("OrgConfigViaAMSDK:getRealmForAMSDK" +
                        " SSException: ", ssoe);
                }
            }
        }
        return (realm);
    }
    
    /**
     * This method checks if the dn starts with org unit naming attr.
     * If yes, then it replaces values of all ou's by prefixing
     * SMSEntry.SUN_INTERNAL_REALM_NAME because all realms mapping to
     * orgUnits are hidden.
     * If the dn does not start with org unit naming attr then it is
     * returned as-is.
     * For example,
     *      ou=X,ou=Y,o=DevSample,dc=red,dc=iplanet,dc=com
     *      is replaced with
     *      ou=sunamhiddenrealmX,ou=sunamhiddenrealmY,o=DevSample,dc=red,dc=iplanet,dc=com
     *
     * @param orgUnitDN String can not be null
     */
    private static String hideOrgUnits(String orgUnitDN) {
        String ou = getNamingAttrForOrgUnit();
        if(!orgUnitDN.startsWith(ou)) {
            return orgUnitDN;
        }
        DN result = new DN();
        result.setDNType(DN.RFC);
        DN dn = new DN(orgUnitDN);
        List rdns = dn.getRDNs();
        for(Iterator iter = rdns.iterator();iter.hasNext();) {
            String relDN = ((RDN)iter.next()).toString();
            if(relDN.startsWith(ou)) {
                relDN = relDN.replaceFirst(ou + SMSEntry.EQUALS,
                    ou + SMSEntry.EQUALS + SMSEntry.SUN_INTERNAL_REALM_NAME);
            }
            result.addRDNToBack(new RDN(relDN));
        }
        return result.toRFCString();
    }
    
    private static boolean updateAmsdk2RealmNameCache(SSOToken token,
        OrganizationConfigManagerImpl ocm, String amsdkdn)
        throws SMSException, SSOException {
        boolean foundEntry = false;
        // Get the AMSDK DN configured for the realm, update cache
        String orgname = getAmsdkdn(token, ocm);
        if (orgname != null) {
            amsdkdn2realmname.put(orgname, ocm.getOrgDN());
            if (orgname.equals(amsdkdn)) {
                foundEntry = true;
            }
        }
        
        // Walk down the realm tree if entry is not found
        if (!foundEntry) {
            Set subRealmNames = ocm.getSubOrganizationNames(token);
            if ((subRealmNames != null) && !subRealmNames.isEmpty()) {
                for (Iterator realms = subRealmNames.iterator();
                realms.hasNext();) {
                    OrganizationConfigManagerImpl socm =
                        OrganizationConfigManagerImpl.getInstance(
                        token, "o=" + realms.next() + "," +
                        ocm.getOrgDN());
                    if ((foundEntry = updateAmsdk2RealmNameCache(
                        token, socm, amsdkdn))) {
                        break;
                    }
                }
            }
        }
        return (foundEntry);
    }
    
    public static String getAmsdkdn(SSOToken token,
        OrganizationConfigManagerImpl ocm)
        throws SMSException, SSOException {
        if (ServiceManager.isCoexistenceMode()) {
            return ocm.getOrgDN();
        }
        String orgdn = null;
        // Get idrepo plugins and check for amsdkdn plugin
        ServiceConfigManagerImpl sci = ServiceConfigManagerImpl
            .getInstance(token, ServiceManager.REALM_SERVICE, "1.0");
        if (sci != null) {
            ServiceConfigImpl sc = sci.getOrganizationConfig(
                token, ocm.getOrgDN(), null);
            if (sc != null) {
                Set plugins = sc.getSubConfigNames(token);
                if (plugins != null && !plugins.isEmpty()) {
                    for (Iterator items = plugins.iterator();
                    items.hasNext();) {
                        ServiceConfigImpl ssc = sc.getSubConfig(
                            token, (String) items.next());
                        if (ssc.getSchemaID().equalsIgnoreCase(
                            IdConstants.AMSDK_PLUGIN_NAME)) {
                            Map cMap = ssc.getAttributesForRead();
                            if ((cMap != null) && !cMap.isEmpty()) {
                                Set orgs = (Set) cMap.get("amSDKOrgName");
                                if ((orgs != null) && !orgs.isEmpty()) {
                                    orgdn = DNUtils.normalizeDN(
                                        (String) orgs.iterator().next());
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        return (orgdn);
    }
    
    // Returns the organization type for AMSDK DN.
    private int getObjectType() {
        if (objType == 0) {
            try {
                AMStoreConnection amcom = new AMStoreConnection(
                    (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance()));
                objType = amcom.getAMObjectType(parentOrgName);
            } catch(AMException ame) {
                // set as organizational unit
                objType = AMObject.ORGANIZATIONAL_UNIT;
                debug.error("OrgConfigViaAMSDK: Unable to determine type");
            } catch (SSOException ssoe) {
                // set as organizational unit
                objType = AMObject.ORGANIZATIONAL_UNIT;
            }
        }
        return (objType);
    }
}
