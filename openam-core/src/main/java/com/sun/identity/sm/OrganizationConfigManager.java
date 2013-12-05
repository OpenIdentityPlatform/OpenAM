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
 * $Id: OrganizationConfigManager.java,v 1.31 2010/01/20 17:01:36 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationUtils;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.plugins.internal.AgentsRepo;
import com.sun.identity.shared.Constants;

/**
 * The class <code>OrganizationConfigManager</code> provides interfaces to
 * manage an organization's configuration data. It provides interfaces to create
 * and delete organizations, service attributes for organizations and service
 * configuration parameters.
 * <p>
 * The organization configuration can be managed in a hierarchical manner, and a
 * forward slash "/" will be used to separate the name hierarchy. Hence the root
 * of the organization hierarchy will be represented by a single forward slash
 * "/", and sub-organizations will be separated by "/". For example "/a/b/c"
 * would represent a "c" sub-organization within "b" which would be a
 * sub-organization of "a".
 *
 * @supported.all.api
 */
public class OrganizationConfigManager {
    // Instance variables
    private SSOToken token;

    private String orgName;

    private String orgDN;

    private OrgConfigViaAMSDK amsdk;

    private OrganizationConfigManagerImpl orgConfigImpl;

    static String orgNamingAttrInLegacyMode;

    static Pattern baseDNpattern = Pattern.compile(SMSEntry.getRootSuffix());

    protected static final String SERVICES_NODE = SMSEntry.SERVICES_RDN
            + SMSEntry.COMMA + SMSEntry.getRootSuffix();

    // set the special characters which are not in realm names.
    static String specialCharsString = "*|(|)|!|/|=";

    private static String SEPERATOR = "|";

    private String CONF_ENABLED =
        "sun-idrepo-amSDK-config-copyconfig-enabled";

    private boolean copyOrgInitialized;

    private boolean copyOrgEnabled;

    private String amSDKOrgDN;

    // sunOrganizationAlias in org DIT.
    public static final String SUNORG_ALIAS = "sunOrganizationAliases";

    // associatedDomain in org DIT.
    private String SUNDNS_ALIAS = "sunDNSAliases";

    // sunPreferredDomain in org DIT.
    private String SUNPREF_DOMAIN = "sunPreferredDomain";

    // inetDomainStatus in org DIT.
    private String SUNORG_STATUS = "sunOrganizationStatus";

    static {
        initializeFlags();
    }

    /**
     * Constructor to obtain an instance of
     * <code>OrganizationConfigManager
     * </code> for an organization by providing
     * an authenticated identity of the user. The organization name would be "/"
     * seperated to represent organization hierarchy.
     * 
     * @param token
     *            single sign on token of authenticated user identity.
     * @param orgName
     *            name of the organization. The value of <code>null
     * </code> or
     *            "/" would represent the root organization.
     * 
     * @throws SMSException
     *             if an error has occurred while getting the instance of
     *             <code>OrganizationConfigManager
     *                      </code>.
     */
    public OrganizationConfigManager(SSOToken token, String orgName)
            throws SMSException {
        // Copy instance variables
        this.token = token;
        this.orgName = orgName;

        // Instantiate and validate
        validateConfigImpl();
        orgDN = orgConfigImpl.getOrgDN();
        try {
            if (migratedTo70 && !registeredForConfigNotifications) {
                ServiceConfigManager scmr = new ServiceConfigManager(
                        ServiceManager.REALM_SERVICE, token);
                scmr.addListener(new OrganizationConfigManagerListener());
                registeredForConfigNotifications = true;
            }
        } catch (SMSException s) {
            String installTime = SystemProperties.get(
                Constants.SYS_PROPERTY_INSTALL_TIME, "false");
            if (!installTime.equals("true")) {
                SMSEntry.debug.warning("OrganizationConfigManager: "
                    + "constructor. Unable to "
                    + "construct ServiceConfigManager for idRepoService ", s);
            }
            throw s;
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("OrganizationConfigManager:Constructor", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }

        if (coexistMode) {
            amsdk = new OrgConfigViaAMSDK(token, DNMapper
                    .realmNameToAMSDKName(orgDN), orgDN);
            if (orgNamingAttrInLegacyMode == null) {
                orgNamingAttrInLegacyMode = getNamingAttrForOrg();
            }
        }
    }

    /**
     * Returns the fully qualified name of the
     * organization from the root
     * 
     * @return the name of the organization
     */
    public String getOrganizationName() {
        return (orgName);
    }

    /**
     * Returns the services configured for the organization.
     * 
     * @return service names configured for the organization.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             configured services.
     * 
     * @deprecated This method has been deprecated, use <code>
     * getAssignedServices()</code>
     *             instead.
     */
    public Set getConfiguredServices() throws SMSException {
        return (getAssignedServices());
    }

    /**
     * Returns a set of service schemas to be used for
     * creation of an organization. The service schemas contain a list of
     * attributes and their schema, and will be provided as
     * <code>ServiceSchema</code>.
     * 
     * @return Set of <code>ServiceSchema</code> to be used for creation of an
     *         organization.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             service schemas.
     */
    public Set getServiceSchemas() throws SMSException {
        // Loop through the services and determine the
        // organization creation schemas
        Set serviceSchemaSet = null;
        try {
            Set serviceNames = getServiceNames(token);
            serviceSchemaSet = new HashSet(serviceNames.size() * 2);
            for (Iterator names = serviceNames.iterator(); names.hasNext();) {
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                    (String) names.next(), token);
                ServiceSchema ss = ssm.getOrganizationCreationSchema();
                if (ss != null) {
                    serviceSchemaSet.add(ss);
                }
            }
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("OrganizationConfigManager:getServiceSchemas"
                    + " unable to get service schema", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"), ssoe,
                    "sms-INVALID_SSO_TOKEN"));
        }
        return (serviceSchemaSet);
    }

    /**
     * Creates a sub-organization under the current
     * organization and sets the specified attributes. The sub-organization
     * created can be only one level below the current organization. For
     * multiple levels this method must be called recursively with the
     * corresponding <code>OrganizationConfigManager
     * </code>. The organization
     * name must not have forward slash ("/"). For eg., the actual organization
     * name 'iplanet' cannot be 'iplan/et' because we are using '/' as the
     * seperator here. The attributes for the organization can be <code>
     * null</code>;
     * else would contain service name as the key and another <code>Map</code>
     * as the value that would contain the key-values pair for the services.
     * 
     * @param subOrgName
     *            the name of the sub-organization.
     * @param attributes
     *            Map of attributes for the organization per service. The
     *            parameter Map attributes contains another Map as its value,
     *            which then has attribute names and values. The way it is
     *            arranged is: Map::attributes --> Key: String::ServiceName
     *            Value: Map::svcAttributes Map::svcAttributes --> Key:
     *            String::AttributeName Value: Set::AttributeValues
     * 
     * @return organization config manager of the newly created
     *         sub-organization.
     * @throws SMSException
     *             if creation of sub-organization failed, or if creation of
     *             sub-organization is attempted when configuration is not
     *             migrated to realms.
     */
    public OrganizationConfigManager createSubOrganization(String subOrgName,
            Map attributes) throws SMSException {
        validateConfigImpl();
        /*
         * Since the "Map attributes" can contain more than one service name,
         * creation of the sub organization is be achieved in 2 steps. i) create
         * the sub-organization without the attributes ii) for the service names
         * in the Map call setAttributes(...)
         */
        boolean orgExists = false;
        String subOrgDN = normalizeDN(subOrgName, orgDN);
        try {
            // Check if realm exists, this throws SMSException
            // if realm does not exist
            // This is to avoid duplicate creation of realms.
            new OrganizationConfigManager(token, subOrgDN);
            SMSEntry.debug.error("OrganizationConfigManager::"
                    + "createSubOrganization() " + "Realm Already Exists.. "
                    + subOrgDN);
            orgExists = true;
        } catch (SMSException smse) {
            // Realm does not exist, create it
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("OrganizationConfigManager::"
                        + "createSubOrganization() "
                        + "New Realm, creating realm: " + subOrgName + "-"
                        + smse);
            }
        }
        Object args[] = { subOrgName };
        if (orgExists) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-organization_already_exists1",
                    args));
        }
        StringTokenizer st =
            new StringTokenizer(specialCharsString, SEPERATOR);
        while (st.hasMoreTokens()) {
            String obj = (String) st.nextToken();
            if (subOrgName.indexOf(obj) > -1) {
                SMSEntry.debug.error("OrganizationConfigManager::"+
                    "createSubOrganization() : Invalid realm name: "+
                        subOrgName);
                SMSEntry.debug.error("OrganizationConfigManager::"+
                    "createSubOrganization() : Detected invalid chars: "+obj);
                Object args1[] = {subOrgName};
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    SMSEntry.bundle.getString("sms-invalid-org-name"),args1));
            }
        }

        // If in legacy mode or (realm mode and copy org enabled)
        // Create the AMSDK organization first
        if ((coexistMode) || (realmEnabled && isCopyOrgEnabled())) {
            amsdk.createSubOrganization(subOrgName);
        }
        if ((realmEnabled || subOrgDN.toLowerCase().startsWith(
                SMSEntry.SUN_INTERNAL_REALM_PREFIX))
                && getSubOrganizationNames(subOrgName, false).isEmpty()) {
            CreateServiceConfig.createOrganization(token, subOrgDN);
        }
        // Update the attributes
        // If in coexistMode and serviceName is idRepoService
        // the following call sets the attributes to AMSDK organization also.
        OrganizationConfigManager ocm = getSubOrgConfigManager(subOrgName);
        if ((attributes != null) && (!attributes.isEmpty())) {
            for (Iterator svcNames = attributes.keySet().iterator(); svcNames
                    .hasNext();) {
                String serviceName = (String) svcNames.next();
                Map svcAttributes = (Map) attributes.get(serviceName);
                if ((svcAttributes != null) && (!svcAttributes.isEmpty())) {
                    ocm.setAttributes(serviceName, svcAttributes);
                }
            }
        }

        if (realmEnabled) {
            AgentsRepo agentsRepo = new AgentsRepo();
            HashMap config = new HashMap(1);
            HashSet realmName = new HashSet(1);
            realmName.add(subOrgName);
            config.put("agentsRepoRealmName", realmName);
            try {
                agentsRepo.initialize(config);
                agentsRepo.createAgentGroupConfig(token);
            } catch (IdRepoException ide) {
            	SMSEntry.debug.error("OrganizationConfigManager::"+
                        "createSubOrganization:", ide);
            }
        }
        		
        // If in realm mode and not in legacy mode, default services needs
        // to be added.
        if (realmEnabled && !coexistMode) {
            loadDefaultServices(token, ocm);
        }

        // If in realm mode and copy org enabled, default services needs
        // to be registered for the newly created org/suborg and the
        // amSDKOrgName/OpenSSO Organization is updated with the
        // new suborg dn.
        if (realmEnabled && isCopyOrgEnabled()) {
            registerSvcsForOrg(subOrgName, subOrgDN);
            OrganizationConfigManager subOrg =
                getSubOrgConfigManager(subOrgName);
            ServiceConfig s =
                subOrg.getServiceConfig(ServiceManager.REALM_SERVICE);
            if (s != null) {
                try {
                    Iterator items = s.getSubConfigNames().iterator();
                    while (items.hasNext()) {
                        ServiceConfig subConfig =
                            s.getSubConfig((String) items.next());
                        if (subConfig.getSchemaID().equalsIgnoreCase(
                            IdConstants.AMSDK_PLUGIN_NAME)) {
                            Map amsdkConfig = new HashMap();
                            Set vals = new HashSet();
                            vals.add(orgNamingAttrInLegacyMode +
                                SMSEntry.EQUALS +
                                subOrgName + SMSEntry.COMMA + amSDKOrgDN);
                            amsdkConfig.put("amSDKOrgName", vals);
                            subConfig.setAttributes(amsdkConfig);
                        }
                        break;
                    }
                } catch (SSOException ssoe) {
                    SMSEntry.debug.error("OrganizationConfigManager::"+
                        "createSubOrganization:", ssoe);
                    throw (new SMSException(SMSEntry.bundle.getString(
                        "sms-INVALID_SSO_TOKEN"), "sms-INVALID_SSO_TOKEN"));
                }
            }
        }

        if (realmEnabled) {
            try {
                if (coexistMode) {
                    DelegationUtils.createRealmPrivileges(token, orgName);
                } else {
                    OrganizationConfigManager parentOrg =
                        getParentOrgConfigManager();
                    DelegationUtils.copyRealmPrivilegesFromParent(
                        token, parentOrg, ocm);
                }
            } catch (SSOException ssoe) {
                if (SMSEntry.debug.messageEnabled()) {
                	SMSEntry.debug.message("Creating delegation permissions for: " +
                        orgName + " failed", ssoe);
                }
            } catch (SMSException smse) {
                if (SMSEntry.debug.messageEnabled()) {
                	SMSEntry.debug.message("Creating delegation permissions for: " +
                        orgName + " failed", smse);
                }
            } catch (DelegationException de) {
                if (SMSEntry.debug.messageEnabled()) {
                	SMSEntry.debug.message("Creating delegation permissions for: " +
                        orgName + " failed", de);
                }
            }
        } 

        // Return the newly created organization config manager
        return (ocm);
    }

    /**
     * Returns the names of all sub-organizations.
     * 
     * @return set of names of all sub-organizations.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             sub-organization names.
     */

    public Set getSubOrganizationNames() throws SMSException {
        try {
            return (getSubOrganizationNames("*", false));
        } catch (SMSException s) {
            SMSEntry.debug.error("OrganizationConfigManager: "
                    + "getSubOrganizationNames() Unable to "
                    + "get sub organization names ", s);
            throw s;
        }
    }

    /**
     * Returns the names of all peer-organizations.
     * 
     * @return set of names of all peer-organizations.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             peer-organization names.
     */

    public Set getPeerOrganizationNames() throws SMSException {
        Set getPeerSet = Collections.EMPTY_SET;
        if (realmEnabled) {
            try {
                OrganizationConfigManager ocmParent = 
                    getParentOrgConfigManager();
                getPeerSet = ocmParent.getSubOrganizationNames();
            } catch (SMSException s) {
                if (SMSEntry.debug.warningEnabled()) {
                    SMSEntry.debug.warning("OrganizationConfigManager: "
                            + "getPeerOrganizationNames() Unable to "
                            + "get Peer organization names ", s);
                }
                throw s;
            }
        }
        return (getPeerSet);
    }

    /**
     * Returns names of sub-organizations matching the
     * given pattern. If the parameter <code>recursive</code> is set to
     * <code>true</code>, search will be performed for the entire sub-tree.
     * The pattern can contain "*" as the wildcard to represent zero or more
     * characters.
     * 
     * @param pattern
     *            pattern that will be used for searching, where "*" will be the
     *            wildcard.
     * @param recursive
     *            if set to <code>true</code> the entire sub-tree will be
     *            searched for the organization names.
     * @return names of sub-organizations matching the pattern.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             sub-organization names.
     */
    public Set getSubOrganizationNames(String pattern, boolean recursive)
            throws SMSException {
        validateConfigImpl();
        try {
            if (realmEnabled) {
                return (orgConfigImpl.getSubOrganizationNames(token, pattern,
                        recursive));
            } else {
                // Must be in coexistence mode
                return (amsdk.getSubOrganizationNames(pattern, recursive));
            }
        } catch (SMSException s) {
            SMSEntry.debug.error("OrganizationConfigManager: "
                    + "getSubOrganizationNames(String pattern, "
                    + "boolean recursive) Unable to get sub organization "
                    + "names for filter: " + pattern, s);
            throw s;
        }
    }

    /**
     * Deletes the given sub-organization. If the
     * parameter <code>recursive</code> is set to <code>true</code>, then
     * the suborganization and the sub-tree will be deleted.
     * 
     * If the parameter <code>recursive</code> is set to <code>false</code>
     * then the sub-organization shall be deleted provided it is the leaf node.
     * If there are entries beneath the sub-organization and if the parameter
     * <code>recursive</code> is set to <code>false</code>, then an
     * exception is thrown that this sub-organization cannot be deleted.
     * 
     * @param subOrgName
     *            sub-organization name to be deleted.
     * @param recursive
     *            if set to <code>true</code> the entire sub-tree will be
     *            deleted.
     * @throws SMSException
     *             if the sub-organization name cannot be found, or if there are
     *             entries beneath the sub-organization and if the parameter
     *             <code>recursive</code> is set to <code>false</code>.
     */
    public void deleteSubOrganization(String subOrgName, boolean recursive)
            throws SMSException {
        validateConfigImpl();
        // Should not delete the root realm, should throw exception if
        // attempted.
        String subOrgDN = normalizeDN(subOrgName, orgDN);
        if (subOrgDN.equals(SMSEntry.SLASH_STR) ||
            subOrgDN.equalsIgnoreCase(SMSEntry.getRootSuffix()) ||
            subOrgDN.equalsIgnoreCase(SERVICES_NODE)) {
            
            Object parms[] = { orgName };
            SMSEntry.debug.error(
                    "OrganizationConfigManager: deleteSubOrganization(" +
                    "Root realm "+orgName + " cannot be deleted. ");
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-cannot_delete_rootsuffix",parms));
                    
        }
        // Delete the sub-organization
        OrganizationConfigManager subRlmConfigMgr =
            getSubOrgConfigManager(subOrgName);
        //set the filter "*" to be passed for the search.
        Set subRlmSet =
            subRlmConfigMgr.getSubOrganizationNames("*", true);

        if (realmEnabled) {
            try {
                CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token,
                        subOrgDN);
                if (cEntry.isDirty()) {
                    cEntry.refresh();
                }
                SMSEntry entry = cEntry.getClonedSMSEntry();
                if (!recursive) {
                    // Check if there are sub organization entries
                    // and if exist
                    // throw exception that this sub organization cannot be
                    // deleted.
                    if ((subRlmSet !=null) && (!subRlmSet.isEmpty())) {
                        throw (new SMSException(SMSEntry.bundle
                                .getString("sms-entries-exists"),
                                "sms-entries-exists"));
                    }
                }
                // Obtain the SMSEntry for the suborg and
                // sub tree and delete it.
                entry.delete(token);
                cEntry.refresh(entry);
            } catch (SSOException ssoe) {
                SMSEntry.debug.error(
                        "OrganizationConfigManager: deleteSubOrganization(" +
                        "String subOrgName, boolean recursive) Unable to " +
                        "delete sub organization ", ssoe);
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-INVALID_SSO_TOKEN"),
                        "sms-INVALID_SSO_TOKEN"));
            }
        }

        // If in legacy mode or (realm mode and copy org enabled)
        // delete the corresponding organization.
        if ((coexistMode) || (realmEnabled && isCopyOrgEnabled())) {
            String amsdkName = DNMapper.realmNameToAMSDKName(subOrgDN);
            if (!SMSEntry.getRootSuffix().equalsIgnoreCase(
                SMSEntry.getAMSdkBaseDN())) {
                String convOrg = subOrgName;
                if (subOrgName.startsWith("/")) {
                    convOrg = DNMapper.convertToDN(subOrgName).toString();
                }
                amsdkName = convOrg + SMSEntry.COMMA + amSDKOrgDN;
            }
            amsdk.deleteSubOrganization(amsdkName);
        }
    }

    /**
     * Returns the <code>OrganizationConfigManager</code>
     * for the given organization name.
     * 
     * @param subOrgName
     *            the name of the organization.
     * @return the configuration manager for the given organization.
     * 
     * @throws SMSException
     *             if the organization name cannot be found or user doesn't have
     *             access to that organization.
     */
    public OrganizationConfigManager getSubOrgConfigManager(String subOrgName)
            throws SMSException {
        validateConfigImpl();
        // Normalize sub organization name
        return (new OrganizationConfigManager(token, normalizeDN(subOrgName,
                orgDN)));
    }

    /**
     * Returns the organization creation attributes for
     * the service.
     * 
     * @param serviceName
     *            name of the service.
     * @return map of organization creation attribute values for service
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             attributes of the service.
     */
    public Map getAttributes(String serviceName) throws SMSException {
        validateConfigImpl();
        if (serviceName == null) {
            return (Collections.EMPTY_MAP);
        }
        Map attrValues = null;
        // Attributes can be obtained only if DIT is migrated to AM 7.0
        if (migratedTo70) {
            // Lowercase the service name
            serviceName = serviceName.toLowerCase();
            try {
                CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token,
                        orgDN);
                if (cEntry.isDirty() || (coexistMode) ||
                    (realmEnabled && isCopyOrgEnabled())) {
                    // Since AMSDK org notifications will not be
                    // obtained, the entry must be read again
                    cEntry.refresh();
                }
                SMSEntry entry = cEntry.getSMSEntry();
                Map map = SMSUtils.getAttrsFromEntry(entry);
                if ((map != null) && (!map.isEmpty())) {
                    Iterator itr = map.keySet().iterator();
                    while (itr.hasNext()) {
                        String name = (String) itr.next();
                        if ((name.toLowerCase()).startsWith(serviceName)) {
                            Set values = (Set) map.get(name);
                            // Remove the serviceName and '-' and return only
                            // the attribute name,value.
                            String key = name
                                    .substring(serviceName.length() + 1);
                            if (attrValues == null) {
                                attrValues = new HashMap();
                            }
                            attrValues.put(key, values);
                        }
                    }
                }
            } catch (SSOException ssoe) {
                SMSEntry.debug.error("OrganizationConfigManager: "
                        + "getAttributes(String serviceName) Unable to "
                        + "get Attributes", ssoe);
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-INVALID_SSO_TOKEN"),
                        "sms-INVALID_SSO_TOKEN"));
            }
        }

        // If in coexistMode and serviceName is idRepoService
        // get attributes from AMSDK organization
        if ((coexistMode || (realmEnabled && isCopyOrgEnabled()))
                && serviceName
                    .equalsIgnoreCase(OrgConfigViaAMSDK.IDREPO_SERVICE)) {
            Map amsdkMap = amsdk.getAttributes();
            Map mergesdkMap = new HashMap(2);
            if (amsdkMap != null && !amsdkMap.isEmpty()) {
                Set mergeValues = new HashSet(2);
                Iterator itr = amsdkMap.keySet().iterator();
                while (itr.hasNext()) {
                    String key = (String) itr.next();
                    if (key.equalsIgnoreCase(SUNDNS_ALIAS) || 
                        key.equalsIgnoreCase(SUNPREF_DOMAIN) ||
                            key.equalsIgnoreCase(SUNORG_ALIAS)) {
                        buildSet(key, amsdkMap, mergeValues);
                    }
                }
                mergesdkMap.put(SUNORG_ALIAS, mergeValues);
                mergesdkMap.put(SUNORG_STATUS,
                    (Set) amsdkMap.get(SUNORG_STATUS));
            }
            if (attrValues == null) {
                attrValues = mergesdkMap;
            } else {
                attrValues.putAll(mergesdkMap);
            }
        }
        return ((attrValues == null) ? Collections.EMPTY_MAP : attrValues);
    }

    /**
     * Builds and returns the appropriate Set for the attributes to be
     * merged from org and realm if the system is
     * in intrusive mode (Both org DIT and realm DIT are present).
     * This happens when the Copy Config flag is enabled.
     */
    private Set buildSet(String attrName, Map attributes, Set resultSet) {
        Set vals = (Set) attributes.get(attrName);
        if ((vals != null) && !vals.isEmpty()) {
            resultSet.addAll(vals);
        }
        return (resultSet);
    }

    /**
     * Adds organization attributes for the service. If
     * the attribute already exists, the values will be appended to it, provided
     * it is a multi-valued attribute. It will throw exception if we try to add
     * a value to an attribute which has the same value already.
     * 
     * @param serviceName
     *            name of the service.
     * @param attrName
     *            name of the attribute.
     * @param values
     *            values for the attribute.
     * @throws SMSException
     *             if we try to add a value to an attribute which has the same
     *             value already.
     */
    public void addAttributeValues(String serviceName, String attrName,
            Set values) throws SMSException {
        validateConfigImpl();
        if (serviceName == null || attrName == null) {
            return;
        }

        if (migratedTo70) {
            // Lowercase the servicename
            serviceName = serviceName.toLowerCase();
            try {
                CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token,
                        orgDN);
                if (cEntry.isDirty()) {
                    cEntry.refresh();
                }
                SMSEntry e = cEntry.getClonedSMSEntry();
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                        serviceName, token);
                ServiceSchema ss = ssm.getOrganizationCreationSchema();
                if (ss == null) {
                    throw (new SMSException(SMSEntry.bundle
                            .getString("sms-SMSSchema_service_notfound"),
                            "sms-SMSSchema_service_notfound"));
                }

                Map map = new HashMap(2);
                Set newValues = new HashSet(values);
                Map allAttributes = ss.getAttributeDefaults();
                Set existingValues = (Set)allAttributes.get(attrName);
                if ((existingValues != null) && !existingValues.isEmpty()) {
                    newValues.addAll(existingValues);
                }
                map.put(attrName, newValues);
                ss.validateAttributes(map);
                SMSUtils.addAttribute(e, serviceName + "-" + attrName,
                    values, ss.getSearchableAttributeNames());
                e.save(token);
                cEntry.refresh(e);
            } catch (SSOException ssoe) {
                SMSEntry.debug.error("OrganizationConfigManager: Unable "
                        + "to add Attribute Values", ssoe);
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-INVALID_SSO_TOKEN"),
                        "sms-INVALID_SSO_TOKEN"));
            }
        }

        // If in coexistMode and serviceName is idRepoService
        // add the attributes to AMSDK organization
        if (coexistMode
                && serviceName
                        .equalsIgnoreCase(OrgConfigViaAMSDK.IDREPO_SERVICE)) {
            amsdk.addAttributeValues(attrName, values);
        }
    }

    /**
     * Sets/Creates organization attributes for the
     * service. If the attributes already exists, the given attribute values
     * will replace them.
     * 
     * @param serviceName
     *            name of the service.
     * @param attributes
     *            attribute-values pairs.
     * @throws SMSException
     *             if the serviceName cannot be found.
     */
    public void setAttributes(String serviceName, Map attributes)
            throws SMSException {
        validateConfigImpl();
        if (serviceName == null) {
            return;
        }

        if (migratedTo70) {
            // Lowercase the serviceName
            serviceName = serviceName.toLowerCase();
            try {
                CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token,
                        orgDN);
                if (cEntry.isDirty()) {
                    cEntry.refresh();
                }
                SMSEntry e = cEntry.getClonedSMSEntry();
                if ((attributes != null) && (!attributes.isEmpty())) {
                    // Validate the attributes
                    ServiceSchemaManager ssm = new ServiceSchemaManager(
                            serviceName, token);
                    ServiceSchema ss = ssm.getOrganizationCreationSchema();
                    ss.validateAttributes(attributes);

                    // Normalize the attributes with service name
                    Map attrsMap = new HashMap();
                    Iterator itr = attributes.keySet().iterator();
                    while (itr.hasNext()) {
                        String name = (String) itr.next();
                        Set values = (Set) attributes.get(name);
                        /*
                         * To make the attributes qualified by service name we
                         * prefix the attribute names with the service name.
                         */
                        attrsMap.put(serviceName + "-" + name, values);
                    }

                    // Look for old attrs. in the storage and add them too.
                    Map oldAttrs = getAttributes(serviceName);
                    Iterator it = oldAttrs.keySet().iterator();
                    while (it.hasNext()) {
                        String skey = (String) it.next();
                        if (!attributes.containsKey(skey))
                            attrsMap.put(serviceName + "-" + skey, oldAttrs
                                    .get(skey));
                    }

                    // Set the attributes in SMSEntry
                    SMSUtils.setAttributeValuePairs(e, attrsMap, ss
                            .getSearchableAttributeNames());

                    String dataStore = SMSEntry.getDataStore(token);
                    // Add these OCs only for SunOne DS. Do not add the 
                    // OCs for Active Directory.
                    // Will get WILL_NOT_PERFORM in AD.
                    if ((dataStore != null) && !dataStore.equals(
                        SMSEntry.DATASTORE_ACTIVE_DIR)
                    ) {
                        // This is for storing organization attributes
                        // in top/default realm node. eg.,ou=services,o=isp
                        if (e.getDN().equalsIgnoreCase(SERVICES_NODE)) {
                            String[] ocVals = e
                                .getAttributeValues(SMSEntry.ATTR_OBJECTCLASS);
                            boolean exists = false;
                            for (int ic = 0; ocVals != null 
                                && ic < ocVals.length; ic++) 
                            {
                                if (ocVals[ic].startsWith(
                                    SMSEntry.OC_SERVICE_COMP)) {
                                    // OC needs to be added outside the for loop
                                    // else will throw concurrent mod exception
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                e.addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                                    SMSEntry.OC_SERVICE_COMP);
                            }
                        } else if (e.getDN().startsWith(
                            SMSEntry.ORGANIZATION_RDN + SMSEntry.EQUALS)) {
                            // This is for storing organization attributes in
                            // organizations created via sdk through realm 
                            // console.
                            String[] vals = e
                                .getAttributeValues(SMSEntry.ATTR_OBJECTCLASS);
                            boolean rsvcExists = false;
                            for (int n = 0; vals != null && n < vals.length; 
                                n++) {
                                if (vals[n].equalsIgnoreCase(
                                    SMSEntry.OC_REALM_SERVICE)) 
                                {
                                    // OC needs to be added outside the for loop
                                    // else will throw concurrent mod exception
                                    rsvcExists = true;
                                    break;
                                }
                            }
                            if (!rsvcExists) {
                                e.addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                                    SMSEntry.OC_REALM_SERVICE);
                            }
                        }
                    }

                    // Save in backend data store and refresh the cache
                    e.save(token);
                    cEntry.refresh(e);
                }

            } catch (SSOException ssoe) {
                SMSEntry.debug.error("OrganizationConfigManager: Unable "
                        + "to set Attributes", ssoe);
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-INVALID_SSO_TOKEN"),
                        "sms-INVALID_SSO_TOKEN"));
            }
        }

        // If in coexistMode and serviceName is idRepoService
        // set the attributes to AMSDK organization
        if ((coexistMode || (realmEnabled && isCopyOrgEnabled()))
                && serviceName
                        .equalsIgnoreCase(OrgConfigViaAMSDK.IDREPO_SERVICE)) {
            amsdk.setAttributes(attributes);
        }
    }

    /**
     * Removes the given organization creation attribute
     * for the service.
     * 
     * @param serviceName
     *            name of service.
     * @param attrName
     *            name of attribute.
     * @throws SMSException
     *             if the organization attribute for the service to be removed
     *             cannot be found, or if the service name cannot be found.
     */
    public void removeAttribute(String serviceName, String attrName)
            throws SMSException {
        validateConfigImpl();
        if (serviceName == null || attrName == null) {
            return;
        }

        if (migratedTo70) {
            try {
                CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token,
                        orgDN);
                if (cEntry.isDirty()) {
                    cEntry.refresh();
                }
                SMSEntry e = cEntry.getClonedSMSEntry();
                SMSUtils.removeAttribute(e, serviceName.toLowerCase() + "-"
                        + attrName);
                e.save(token);
                cEntry.refresh(e);
            } catch (SSOException ssoe) {
                SMSEntry.debug.error("OrganizationConfigManager: Unable "
                        + "to remove Attribute", ssoe);
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-INVALID_SSO_TOKEN"),
                        "sms-INVALID_SSO_TOKEN"));
            }
        }

        // If in coexistMode and serviceName is idRepoService
        // remove the attributes to AMSDK organization
        if (coexistMode
                && serviceName
                        .equalsIgnoreCase(OrgConfigViaAMSDK.IDREPO_SERVICE)) {
            amsdk.removeAttribute(attrName);
        }
    }

    /**
     * Removes the given organization creation attribute
     * values for the service.
     * 
     * @param serviceName
     *            name of service.
     * @param attrName
     *            name of attribute.
     * @param values
     *            attribute values to be removed.
     * @throws SMSException
     *             if the organization attribute for the service to be removed
     *             cannot be found, or if the service name cannot be found, or
     *             if the value cannot be removed.
     */
    public void removeAttributeValues(String serviceName, String attrName,
            Set values) throws SMSException {
        validateConfigImpl();
        if (serviceName == null || attrName == null) {
            return;
        }
        if (migratedTo70) {
            try {
                CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token,
                        orgDN);
                if (cEntry.isDirty()) {
                    cEntry.refresh();
                }
                SMSEntry e = cEntry.getClonedSMSEntry();
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                        serviceName, token);
                ServiceSchema ss = ssm.getOrganizationCreationSchema();
                Map map = new HashMap(2);
                map.put(attrName, values);
                ss.validateAttributes(map);
                SMSUtils.removeAttributeValues(e, serviceName.toLowerCase()
                        + "-" + attrName, values, ss
                        .getSearchableAttributeNames());
                e.save(token);
                cEntry.refresh(e);

            } catch (SSOException ssoe) {
                SMSEntry.debug.error("OrganizationConfigManager: Unable "
                        + "to remove Attribute Values", ssoe);
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-INVALID_SSO_TOKEN"),
                        "sms-INVALID_SSO_TOKEN"));
            }
        }

        // If in coexistMode and serviceName is idRepoService
        // remove the attributes to AMSDK organization
        if (coexistMode
                && serviceName
                        .equalsIgnoreCase(OrgConfigViaAMSDK.IDREPO_SERVICE)) {
            amsdk.removeAttributeValues(attrName, values);
        }
    }

    /**
     * Returns the service configuration object for the
     * given service name.
     * 
     * @param serviceName
     *            name of a service.
     * @return service configuration object for the service.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             service configuration, or if the service name cannot be
     *             found.
     */
    public ServiceConfig getServiceConfig(String serviceName)
            throws SMSException {
        try {
            ServiceConfigManager scmgr = new ServiceConfigManager(serviceName,
                    token);
            ServiceConfig scg = scmgr.getOrganizationConfig(orgName, null);
            return (scg);
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("OrganizationConfigManager: Unable to "
                    + "get Service Config", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Adds a service configuration object for the given
     * service name for this organization. If the service has been already added
     * a <code>SMSException</code> will be thrown.
     * 
     * @param serviceName
     *            name of the service.
     * @param attributes
     *            service configuration attributes.
     * @return service configuration object.
     * @throws SMSException
     *             if the service configuration has been added already.
     */
    public ServiceConfig addServiceConfig(String serviceName, Map attributes)
            throws SMSException {
        try {
            ServiceConfigManagerImpl scmi = ServiceConfigManagerImpl
                    .getInstance(token, serviceName, 
                    ServiceManager.getVersion(serviceName));
            ServiceConfigImpl sci = scmi.getOrganizationConfig(token, orgName,
                    null);
            if (sci == null || sci.isNewEntry()) {
                ServiceConfigManager scm = new ServiceConfigManager(
                        serviceName, token);
                return (scm.createOrganizationConfig(orgName, attributes));
            } else {
                SMSEntry.debug.error("OrganizationConfigManager: "
                        + "ServiceConfig already exists: " + sci.getDN());
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-service_already_exists1")));
            }
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("OrganizationConfigManager: Unable to "
                    + "add Service Config", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Removes the service configuration object for the
     * given service name for this organization.
     * 
     * @param serviceName
     *            name of the service.
     * @throws SMSException
     *             if the service name cannot be found, or not added to the
     *             organization.
     */
    public void removeServiceConfig(String serviceName) throws SMSException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(serviceName,
                    token);
            scm.deleteOrganizationConfig(orgName);
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("OrganizationConfigManager: Unable to "
                    + "delete Service Config", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Registers for changes to organization's
     * configuration. The object will be called when configuration for this
     * organization is changed.
     * 
     * @param listener
     *            callback object that will be invoked when organization
     *            configuration has changed
     * @return an ID of the registered listener.
     */
    public String addListener(ServiceListener listener) {
        return (orgConfigImpl.addListener(listener));
    }

    /**
     * Removes the listener from the organization for the
     * given listener ID. The ID was issued when the listener was registered.
     * 
     * @param listenerID
     *            the listener ID issued when the listener was registered
     */
    public void removeListener(String listenerID) {
        orgConfigImpl.removeListener(listenerID);
    }

    /**
     * Returns normalized DN for realm model
     */
    private static String normalizeDN(String subOrgName, String orgDN) {
        // Return orgDN if subOrgName is either null or empty
        if (subOrgName == null || subOrgName.length() == 0) {
            return (orgDN);
        }
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("OrganizationConfigManager."
                    + "normalizeDN()-subOrgName " + subOrgName);
        }
        String subOrgDN = null;
        if (DN.isDN(subOrgName) && (!subOrgName.startsWith("///"))) {
            int ndx = subOrgName.lastIndexOf(DNMapper.serviceDN);
            if (ndx == -1) {
                // Check for baseDN
                ndx = subOrgName.lastIndexOf(SMSEntry.getRootSuffix());
            }
            if (ndx > 0) {
                subOrgName = subOrgName.substring(0, ndx - 1);
            }
            subOrgDN = DNMapper.normalizeDN(subOrgName) + orgDN;
        } else if (subOrgName.indexOf('/') != -1) {
            String tmp = DNMapper.convertToDN(subOrgName).toString();
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("OrganizationConfigManager."
                        + "normalizeDN()-slashConvertedString: " + tmp);
            }
            if (tmp != null && tmp.length() > 0) {
                if (tmp.charAt(tmp.length() - 1) == ',') {
                    subOrgDN = tmp + DNMapper.serviceDN;
                } else {
                    int dx = tmp.indexOf(SMSEntry.COMMA);
                    if (dx >= 0) {
                        subOrgDN = tmp + SMSEntry.COMMA + DNMapper.serviceDN;
                    } else {
                        subOrgDN = tmp + SMSEntry.COMMA + orgDN;
                    }
                }
            } else {
                subOrgDN = orgDN;
            }
        } else if (subOrgName.startsWith(SMSEntry.SUN_INTERNAL_REALM_NAME)) {
            subOrgDN = SMSEntry.ORG_PLACEHOLDER_RDN + subOrgName
                    + SMSEntry.COMMA + DNMapper.serviceDN;
        } else {
            if (coexistMode) {
                subOrgDN = orgNamingAttrInLegacyMode + SMSEntry.EQUALS
                        + subOrgName + SMSEntry.COMMA
                        + DNMapper.realmNameToAMSDKName(orgDN);
            } else {
                subOrgDN = SMSEntry.ORG_PLACEHOLDER_RDN + subOrgName
                        + SMSEntry.COMMA + orgDN;
            }
        }
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("OrganizationConfigManager::"
                    + "normalizeDN() suborgdn " + subOrgDN);
        }
        return (subOrgDN);
    }

    /**
     * Returns all service names configured for AM
     */
    static Set getServiceNames(SSOToken token) throws SMSException,
            SSOException {
        // Get the service names from ServiceManager
        CachedSubEntries cse = CachedSubEntries.getInstance(token,
                DNMapper.serviceDN);
        return (cse.getSubEntries(token));
    }

    /**
     * Returns a set of service names that can be assigned
     * to a realm. This set excludes name of services that are already assigned
     * to the realm and services that are required for the existence of a realm.
     * 
     * @return a set of service names that can be assigned to a realm.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             service configuration
     */
    public Set getAssignableServices() throws SMSException {
        // Get all service names, and remove the assigned services
        // Set containing service names that has organization schema
        Set orgSchemaServiceNames = new HashSet();
        try {
            for (Iterator names = getServiceNames(token).iterator(); names
                    .hasNext();) {
                String serviceName = (String) names.next();
                ServiceSchemaManagerImpl ssmi = ServiceSchemaManagerImpl
                        .getInstance(token, serviceName, 
                        ServiceManager.getVersion(serviceName));
                if (ssmi.getSchema(SchemaType.ORGANIZATION) != null) {
                    // Need to check if the user has permission
                    // to add/assign the service
                    StringBuilder d = new StringBuilder(100);
                    // Need to construct
                    // "ou=default,ou=organizationconfig,ou=1.0,ou="
                    d.append(SMSEntry.PLACEHOLDER_RDN).append(SMSEntry.EQUALS)
                            .append(SMSUtils.DEFAULT).append(SMSEntry.COMMA)
                            .append(CreateServiceConfig.ORG_CONFIG_NODE)
                            .append(SMSEntry.PLACEHOLDER_RDN).append(
                                    SMSEntry.EQUALS).append("1.0").append(
                                    SMSEntry.COMMA).append(
                                    SMSEntry.PLACEHOLDER_RDN).append(
                                    SMSEntry.EQUALS);
                    // Append service name, and org name
                    d.append(serviceName);
                    if (!orgDN.equalsIgnoreCase(DNMapper.serviceDN)) {
                        d.append(SMSEntry.COMMA).append(SMSEntry.SERVICES_NODE);
                    }
                    d.append(SMSEntry.COMMA).append(orgDN);
                    try {
                        // The function will throw exception if
                        // user does not have permissions
                        SMSEntry.getDelegationPermission(token, d.toString(),
                                SMSEntry.modifyActionSet);
                        orgSchemaServiceNames.add(serviceName);
                    } catch (SMSException smse) {
                        if (smse.getExceptionCode() != 
                            SMSException.STATUS_NO_PERMISSION) 
                        {
                            throw (smse);
                        }
                    }
                }
            }
            // Need to remove mandatory services
            // %%% TODO. Need to have SMS Service with this information
            // orgSchemaServiceNames.removeAll(getMandatoryServices());
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("OrganizationConfigManager."
                    + "getAssignableServices(): SSOException", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
        // Remove assigned services
        HashSet answer = new HashSet(orgSchemaServiceNames);
        answer.removeAll(getAssignedServices());
        return (answer);
    }

    /**
     * Returns a set of service names that are assigned to
     * a realm.
     * 
     * @return a set of service names that are assigned to a realm.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             service configuration
     */
    public Set getAssignedServices() throws SMSException {
        return (getAssignedServices(true));
    }

    /**
     * Returns a set of service names that are assigned to a realm.
     * 
     * @param includeMandatory
     *            <code>true</code> to include mandatory service names.
     * @return a set of service names that are assigned to a realm.
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             service configuration
     */
    public Set getAssignedServices(boolean includeMandatory)
            throws SMSException {
        validateConfigImpl();
        Set assignedServices = Collections.EMPTY_SET;
        if (coexistMode) {
            // Get assigned services from OrgConfigViaAMSDK
            assignedServices = amsdk.getAssignedServices();
        } else {
            // Get assigned service names from OrganizationConfigManagerImpl
            assignedServices = orgConfigImpl.getAssignedServices(token);
        }
        if (!includeMandatory) {
            // Get services assigned by default
            Set ds = ServiceManager.requiredServices();
            assignedServices.removeAll(ds);
        }
        return (assignedServices);
    }

    /**
     * Assigns the given service to the orgnization with
     * the respective attributes. If the service has been already added a <code>
     * SMSException</code>
     * will be thrown.
     * 
     * @param serviceName
     *            name of the service
     * @param attributes
     *            service configuration attributes
     * @throws SMSException
     *             if the service configuration has been added already.
     */
    public void assignService(String serviceName, Map attributes)
            throws SMSException {
        addServiceConfig(serviceName, attributes);
    }

    /**
     * Returns attributes configured for the service.
     * 
     * @param serviceName
     *            name of the service
     * @return a map of attributes for the service
     * @throws SMSException
     *             if there is an error accessing the data store to read the
     *             service configuration, or if the service name cannot be
     *             found.
     */
    public Map getServiceAttributes(String serviceName) throws SMSException {
        ServiceConfig scg = getServiceConfig(serviceName);
        if (scg == null) {
            Object args[] = { serviceName };
            SMSEntry.debug.error(
                    "OrganizationConfigManager.getServiceAttributes() Unable " +
                    "to get service attributes. ");
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-no-organization-schema",
                    args));

        }
        return (scg.getAttributes());
    }

    /**
     * Unassigns the service from the organization.
     * 
     * @param serviceName
     *            name of the service
     * @throws SMSException
     *             if the service name cannot be found or assigned, or if the
     *             service is a mandatory service.
     */
    public void unassignService(String serviceName) throws SMSException {
        // if (coexistMode) {
        // amsdk.unassignService(serviceName);
        // } else {
        removeServiceConfig(serviceName);
        // }
    }

    /**
     * Sets the attributes related to provided service.
     * The assumption is that the service is already assigned to the
     * organization. The attributes for the service are validated against the
     * service schema.
     * 
     * @param serviceName
     *            name of the service
     * @param attributes
     *            attributes of the service
     * @throws SMSException
     *             if the service name cannot be found or not assigned to the
     *             organization.
     */
    public void modifyService(String serviceName, Map attributes)
            throws SMSException {
        try {
            getServiceConfig(serviceName).setAttributes(attributes);
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("OrganizationConfigManager.modifyService "
                    + "SSOException in modify service ", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    public String getNamingAttrForOrg() {
        return OrgConfigViaAMSDK.getNamingAttrForOrg();
    }

    /**
     * Returns the <code>OrganizationConfigManager</code>
     * of the parent for the given organization name.
     * 
     * @return the configuration manager of the parent for the given
     *         organization.
     * @throws SMSException
     *             if user doesn't have access to that organization.
     */
    public OrganizationConfigManager getParentOrgConfigManager()
            throws SMSException {
        OrganizationConfigManager ocm = null;
        String parentDN = null;
        if (DN.isDN(orgDN)) {
            if (orgDN.equalsIgnoreCase(DNMapper.serviceDN)) {
                return (this);
            }
            parentDN = (new DN(orgDN)).getParent().toString();
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("OrganizationConfigManager."
                        + "getParentOrgConfigManager() parentDN : " + parentDN);
            }
            if (parentDN != null && parentDN.length() > 0) {
                ocm = new OrganizationConfigManager(token, parentDN);
            }
        }
        return ocm;
    }

    /**
     * Loads default services to a newly created realm
     */
    public static void loadDefaultServices(SSOToken token,
            OrganizationConfigManager ocm) throws SMSException {
        // Check if DIT has been migrated to 7.0
        if (!migratedTo70) {
            return;
        }
        Set defaultServices = ServiceManager.servicesAssignedByDefault();
        // Load the default services automatically
        OrganizationConfigManager parentOrg = ocm.getParentOrgConfigManager();
        if (defaultServices == null) {
            // There are no services to be loaded
            return;
        }

        Set assignedServices = new CaseInsensitiveHashSet(
            parentOrg.getAssignedServices());
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("OrganizationConfigManager"
                    + "::loadDefaultServices " + "assignedServices : "
                    + assignedServices);
        }
        boolean doAuthServiceLater = false;
        boolean doAuthHttpBasicLater = false;
        String serviceName = null;

        // Copy service configuration
        Iterator items = defaultServices.iterator();
        while (items.hasNext() || doAuthHttpBasicLater || doAuthServiceLater) {
            if (items.hasNext()) {
                serviceName = (String) items.next();
                if (serviceName.equals(ISAuthConstants.AUTH_SERVICE_NAME)) {
                    doAuthServiceLater = true;
                    continue;
                } else if (serviceName.equals(
                    ISAuthConstants.AUTH_HTTP_BASIC_SERVICE_NAME)) {

                    doAuthHttpBasicLater = true;
                    continue;
                }
            } else if (doAuthHttpBasicLater) {
                serviceName = ISAuthConstants.AUTH_HTTP_BASIC_SERVICE_NAME;
                doAuthHttpBasicLater = false;
            } else if (doAuthServiceLater) {
                serviceName = ISAuthConstants.AUTH_SERVICE_NAME;
                doAuthServiceLater = false;
            }
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("OrganizationConfigManager" +
                    "::loadDefaultServices:ServiceName " + serviceName);
            }
            try {
                ServiceConfig sc = parentOrg.getServiceConfig(serviceName);
                Map attrs = null;
                if (sc != null && assignedServices.contains(serviceName)) {
                    attrs = sc.getAttributesWithoutDefaults();
                    if (SMSEntry.debug.messageEnabled()) {
                        SMSEntry.debug
                                .message("OrganizationConfigManager"
                                        + "::loadDefaultServices "
                                        + "Copying service from parent: "
                                        + serviceName);
                    }
                    ServiceConfig scn = ocm
                            .addServiceConfig(serviceName, attrs);
                    // Copy sub-configurations, if any
                    copySubConfig(sc, scn);
                }
            } catch (SSOException ssoe) {
                if (SMSEntry.debug.messageEnabled()) {
                    SMSEntry.debug.message(
                            "OrganizationConfigManager.loadDefaultServices " +
                            "SSOException in loading default services ",
                                    ssoe);
                }
                throw (new SMSException(SMSEntry.bundle
                        .getString("sms-INVALID_SSO_TOKEN"),
                        "sms-INVALID_SSO_TOKEN"));
            }
        }
    }


    /**
     * Registers default services to newly created suborganizations.
     */
    private void registerSvcsForOrg(String subOrgName, String subOrgDN)
    {
        try {
            Set defaultServices =
                ServiceManager.servicesAssignedByDefault();
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("OrganizationConfigManager::"+
                    "registerSvcsForOrg. "+
                    "defaultServices : " + defaultServices);
            }

            // Register the default services to the newly created orgs,so
            // they will be marked with the OC sunRegisteredServiceName.
            if (defaultServices != null) {
                Set assignedServices = amsdk.getAssignedServices();
                if (SMSEntry.debug.messageEnabled()) {
                    SMSEntry.debug.message("OrganizationConfigManager::" +
                        "registerSvcsForOrg:assignedServices: " +
                            assignedServices);
                }
                Iterator items = defaultServices.iterator();
                String serviceName = null;
                if (SMSEntry.getRootSuffix().equalsIgnoreCase(
                    SMSEntry.getAMSdkBaseDN())) {
                    amsdk = new OrgConfigViaAMSDK(token,
                      orgNamingAttrInLegacyMode + SMSEntry.EQUALS +
                        subOrgName + SMSEntry.COMMA +
                        DNMapper.realmNameToAMSDKName(orgDN), subOrgDN);
                } else {
                    amsdk = new OrgConfigViaAMSDK(token,
                      orgNamingAttrInLegacyMode + SMSEntry.EQUALS +
                        subOrgName + SMSEntry.COMMA + amSDKOrgDN, subOrgDN);
                }
                while (items.hasNext()) {
                    serviceName = (String) items.next();
                    if (assignedServices.contains(serviceName)) {
                        if (SMSEntry.debug.messageEnabled()) {
                            SMSEntry.debug.message(
                                "OrganizationConfigManager::"+
                                "registerSvcsForOrg:ServiceName : " +
                                serviceName);
                        }
                        amsdk.assignService(serviceName);
                    }
                }
            }
        } catch (SMSException smse) {
            // Unable to load default services
            if (SMSEntry.debug.warningEnabled()) {
                SMSEntry.debug.warning("OrganizationConfigManager::" +
                    "registerSvcsForOrg. " +
                    "SMSException in registering services: ", smse);
            }
        }
    }

    /**
     * Copies service configurations recursively from source to destination
     */
    static void copySubConfig(ServiceConfig from, ServiceConfig to)
            throws SMSException, SSOException {
        Set subConfigNames = from.getSubConfigNames();
        for (Iterator items = subConfigNames.iterator(); items.hasNext();) {
            String subConfigName = (String) items.next();
            ServiceConfig scf = from.getSubConfig(subConfigName);
            to.addSubConfig(subConfigName, scf.getSchemaID(),
                    scf.getPriority(), scf.getAttributesWithoutDefaults());
            ServiceConfig sct = to.getSubConfig(subConfigName);
            copySubConfig(scf, sct);
        }
    }


    /**
     * Determines whether an organization ought to be created for each
     * realm in realm only mode of installation based on the boolean flag
     * in amSDK plugin.
     * This requirement is for portal customers.
     */
    protected boolean isCopyOrgEnabled() {
        if (copyOrgInitialized) {
            return (copyOrgEnabled);
        }
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("OrganizationConfigManager: "+
                "in isCopyOrgEnabled() ");
        }
        // Check if AMSDK is configured for the realm
        try {
            ServiceConfig s = getServiceConfig(ServiceManager.REALM_SERVICE);
            if (s != null) {
                Iterator items = s.getSubConfigNames().iterator();
                while (items.hasNext()) {
                    String name = items.next().toString();
                    ServiceConfig subConfig = s.getSubConfig(name);
                    if (subConfig == null) {
                        SMSEntry.debug.error("OrganizationConfigManager.is" +
                            "CopyOrgEnabled. SubConfig is NULL: " +
                            "SC Name: " + name + " For org: " + orgDN);
                        return (false);
                    }
                    if (subConfig.getSchemaID().equalsIgnoreCase(
                        IdConstants.AMSDK_PLUGIN_NAME)) {
                        Map configMap = subConfig.getAttributes();
                        if ((configMap != null) && !configMap.isEmpty()) {
                            // Get the amsdkOrgName from the amSDKRepo to build
                            // OrgConfigViaSDK instance.
                            Set orgs = (Set) configMap.get("amSDKOrgName");
                            if (orgs != null && !orgs.isEmpty()) {
                                amSDKOrgDN = (String) orgs.iterator().next();
                                Set cfgs = (Set) configMap.get(CONF_ENABLED);
                                if ( (cfgs != null) && (!cfgs.isEmpty()) &&
                                    (cfgs.contains("true")) &&
                                        (amSDKOrgDN !=null) ) {
                                    amsdk = new OrgConfigViaAMSDK(token,
                                        amSDKOrgDN, orgDN);
                                    if (orgNamingAttrInLegacyMode == null) {
                                        orgNamingAttrInLegacyMode =
                                        getNamingAttrForOrg();
                                    }
                                    copyOrgEnabled = true;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (SSOException sse) {
            // Use default values i.e., false
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("OrganizationConfigManager:" +
                    "isCopyOrgEnabled() Unable to get service: " +
                    ServiceManager.REALM_SERVICE, sse);
            }
        } catch (SMSException e) {
            // Use default values i.e., false
            if (SMSEntry.debug.messageEnabled()) {
                SMSEntry.debug.message("OrganizationConfigManager:" +
                    "isCopyOrgEnabled() Unable to get service: " +
                    ServiceManager.REALM_SERVICE, e);
            }
        }
        copyOrgInitialized = true;
        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("OrganizationConfigManager: "+
                "copyOrgEnabled == " + copyOrgEnabled);
        }
        return (copyOrgEnabled);
    }

    static void initializeFlags() {
        realmEnabled = ServiceManager.isRealmEnabled();
        coexistMode = ServiceManager.isCoexistenceMode();
        migratedTo70 = ServiceManager.isConfigMigratedTo70();
    }
    
    void validateConfigImpl() throws SMSException {
        // Instantiate the OrgConfigImpl and cache it
        if ((orgConfigImpl == null) || !orgConfigImpl.isValid()) {
            try {
                orgConfigImpl = OrganizationConfigManagerImpl.getInstance(
                    token, orgName);
            } catch (SSOException ssoe) {
                throw (new SMSException(ssoe, "sms-INVALID_SSO_TOKEN"));
            }
        }
    }

    class OrganizationConfigManagerListener implements ServiceListener {
        public void schemaChanged(String serviceName, String version) {
            // Call ServiceManager to notify
            ServiceManager.schemaChanged();
            // If naming service has changed, reload the AM Servers
            if (serviceName.equalsIgnoreCase(ServiceManager.PLATFORM_SERVICE)) {
                ServiceManager.accessManagerServers = null;
            }
        }

        public void globalConfigChanged(String serviceName, String version,
                String groupName, String serviceComponent, int type) {
            if (serviceName.equalsIgnoreCase(ServiceManager.REALM_SERVICE)) {
                try {
                    ServiceManager.checkFlags(token);
                } catch (SSOException ssoe) {
                    SMSEntry.debug.error("OrganizationConfigManager: "
                            + "globalConfigChanged ", ssoe);
                } catch (SMSException smse) {
                    SMSEntry.debug.error("OrganizationConfigManager: "
                            + "globalConfigChanged ", smse);
                }
                realmEnabled = ServiceManager.isRealmEnabled();
                coexistMode = ServiceManager.isCoexistenceMode();
                migratedTo70 = ServiceManager.isConfigMigratedTo70();
            }
        }

        public void organizationConfigChanged(String serviceName,
                String version, String orgName, String groupName,
                String serviceComponent, int type) {
            // Reset the cached configuration in OrgConfigViaAMSDK
            if (serviceName.equalsIgnoreCase(OrgConfigViaAMSDK.IDREPO_SERVICE))
            {
                OrgConfigViaAMSDK.attributeMappings = new HashMap();
                OrgConfigViaAMSDK.reverseAttributeMappings = new HashMap();
            }
        }
    }

    // ******* Static Variables ************
    // To determine if notification object has been registered for config
    // changes
    private static boolean registeredForConfigNotifications;

    // Realm & Co-existence modes
    private static boolean realmEnabled;

    private static boolean coexistMode;

    private static boolean migratedTo70;
}
