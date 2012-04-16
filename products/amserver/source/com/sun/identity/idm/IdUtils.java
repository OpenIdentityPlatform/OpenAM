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
 * $Id: IdUtils.java,v 1.34 2009/11/20 23:52:54 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.idm;

import com.iplanet.am.sdk.AMDirectoryAccessFactory;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMOrganization;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.DNUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.OrgConfigViaAMSDK;

import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;


/**
 * The class defines some static utilities used by other components like policy
 * and auth
 *
 * @supported.api
 */
public final class IdUtils {
    private static Debug debug = AMIdentityRepository.debug;

    private static Map mapSupportedTypes = new CaseInsensitiveHashMap(10);

    public static Set supportedTypes = new HashSet();

    private static Map mapTypesToServiceNames = new CaseInsensitiveHashMap();

    protected static Map typesCanBeMemberOf = new CaseInsensitiveHashMap();

    protected static Map typesCanHaveMembers = new CaseInsensitiveHashMap();

    protected static Map typesCanAddMembers = new CaseInsensitiveHashMap();

    // Static map to cache "orgIdentifier" and organization DN
    private static Map orgIdentifierToOrgName = Collections.synchronizedMap(
        new CaseInsensitiveHashMap());
    private static Map orgStatusCache = Collections.synchronizedMap(
        new CaseInsensitiveHashMap());

    // ServiceConfigManager for sunidentityrepository service
    private static String notificationId;

    private static ServiceConfigManager serviceConfigManager;
    
    // User naming attribute for AMSDK
    private static String USER_NAMING_ATTR;
    
    // Organization naming attribute for AMSDK
    private static String ORG_NAMING_ATTR;
    
    // SMS Root Suffix
    private static String ROOT_SUFFIX;
    
    // DN pointing to the services node
    private static String SERVICES_SUFFIX;
    
    // Special Users
    private static Set specialUsers = new HashSet();
    
    static {
        initialize();
    }

    protected static void initialize() {
        if (ServiceManager.isConfigMigratedTo70()) {
            // IdRepo service schema exists. Read the supported
            // entities from there
            try {
                SSOToken adminToken = (SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance());
                serviceConfigManager = new ServiceConfigManager(adminToken,
                        IdConstants.REPO_SERVICE, "1.0");
                ServiceConfig ss = serviceConfigManager.getGlobalConfig(null);
                Set typeSchemaNames = ss.getSubConfigNames("*",
                        IdConstants.SUPPORTED_TYPES);
                if (typeSchemaNames == null || typeSchemaNames.isEmpty()) {
                    loadDefaultTypes();
                } else {
                    Iterator it = typeSchemaNames.iterator();
                    while (it.hasNext()) {
                        String typeSchema = (String) it.next();
                        IdType idType = new IdType(typeSchema);
                        supportedTypes.add(idType);
                        mapSupportedTypes.put(idType.getName(), idType);
                        ServiceConfig tsc = ss.getSubConfig(typeSchema);
                        Map attributes = tsc.getAttributes();
                        Set serviceNameSet = (Set) attributes
                                .get(IdConstants.SERVICE_NAME);
                        Set canBeMembersOf = (Set) attributes
                                .get(IdConstants.ATTR_MEMBER_OF);
                        Set canHaveMembers = (Set) attributes
                                .get(IdConstants.ATTR_HAVE_MEMBERS);
                        Set canAddMembers = (Set) attributes
                                .get(IdConstants.ATTR_ADD_MEMBERS);
                        if (serviceNameSet != null && 
                                !serviceNameSet.isEmpty()) {
                            mapTypesToServiceNames.put(typeSchema,
                                    (String) serviceNameSet.iterator().next());
                        }
                        if (canBeMembersOf != null && 
                                !canBeMembersOf.isEmpty()) {
                            Set memberOfSet = getMemberSet(canBeMembersOf);
                            typesCanBeMemberOf.put(typeSchema, memberOfSet);
                        }
                        if (canHaveMembers != null && 
                                !canHaveMembers.isEmpty()) {
                            Set memberSet = getMemberSet(canHaveMembers);
                            typesCanHaveMembers.put(typeSchema, memberSet);
                        }
                        if (canAddMembers != null && 
                                !canAddMembers.isEmpty()) 
                        {
                            Set memberSet = getMemberSet(canAddMembers);
                            typesCanAddMembers.put(typeSchema, memberSet);
                        }
                    }
                }
            } catch (SMSException e) {
                String installTime = SystemProperties.get(
                    Constants.SYS_PROPERTY_INSTALL_TIME, "false");
                if (!installTime.equals("true")) {
                    debug.error(
                        "IdUtils.initialize: Loading default types.", e);
                }
                loadDefaultTypes();
            } catch  (SSOException ssoe) {
                debug.error("dUtils.initialize: Loading default types", ssoe);
                loadDefaultTypes();
            }
        } else {
            loadDefaultTypes();
        }

        // Register for SMS notifications to root realm
        if (notificationId == null) {
            try {
                SSOToken adminToken = (SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance());
                if (serviceConfigManager == null) {
                    serviceConfigManager = new ServiceConfigManager(adminToken,
                        IdConstants.REPO_SERVICE, "1.0");
                }
                notificationId = serviceConfigManager.addListener(
                    new IdUtilsListener());
            } catch (SMSException e) {
                String installTime = SystemProperties.get(
                    Constants.SYS_PROPERTY_INSTALL_TIME, "false");
                if (!installTime.equals("true")) {
                    debug.error(
                        "IdUtils.initialize: Register notification", e);
                }
            } catch (SSOException ssoe) {
                String installTime = SystemProperties.get(
                    Constants.SYS_PROPERTY_INSTALL_TIME, "false");
                if (!installTime.equals("true")) {
                    debug.error(
                        "IdUtils.initialize: Register notification", ssoe);
                }
            }
        }
    }

    /**
     * @supported.api 
     * Returns a handle of the Identity object based on
     * the SSO Token passed in (<code>AMIdentity</code> object of the user
     * who is authenticated).
     * 
     * @param token    Single sign on token of user.
     * @return Identity object.
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException if user's single sign on token is invalid.
     */
    public static AMIdentity getIdentity(SSOToken token)
        throws IdRepoException, SSOException {
        String principal = token.getProperty(Constants.UNIVERSAL_IDENTIFIER);
        if (principal == null) {
            // This could happen during co-existence with AM 6.x
            // and SSOToken created by AM 6.x server. In this case
            // the principal name would be the DN
            principal = token.getPrincipal().getName();
        }
        return (getIdentity(token, principal));
    }

    /**
     * @supported.api
     * 
     * Returns a string which uniquely represents this identity object.
     * 
     * @param id
     *            <code>AMIdentity</code> object whose string represenation is
     *            needed.
     * @return universal identifier of <code>id</code>.
     */
    public static String getUniversalId(AMIdentity id) {
        return id.getUniversalId();
    }

    /**
     * @supported.api 
     * 
     * Returns an <code>AMIdentity</code> object, if provided with a string 
     * identifier for the object.
     * 
     * @param token SSOToken of the administrator
     * @param univId String represenation of the identity.
     * @return Identity object
     * @throws IdRepoException if the identifier provided is wrong.
     */
    public static AMIdentity getIdentity(SSOToken token, String univId)
            throws IdRepoException {
        return (getIdentity(token, univId, null));
    }

    /**
     * Returns an <code>AMIdentity</code> object, given the
     * DN of an authenticated identity, realm name and identity type.
     * This interface is mainly for authentication component to get
     * back the identity of the user.
     * 
     * @param token SSOToken of the administrator
     * @param amsdkdn DN of the authenticated user
     * @param realm  realm name where the user was authenticated
     * @return Identity object or <code>null</code> 
     * @throws IdRepoException if the underly components throws
     * exception while obtaining the identity object
     */
    public static AMIdentity getIdentity(SSOToken token, String amsdkdn,
        String realm) throws IdRepoException {
        DN amsdkdnObject = null;
        if (amsdkdn == null || !(amsdkdnObject = new DN(amsdkdn)).isDN()) {
            Object[] args = { amsdkdn };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                "215", args));
        }

        // Try constructing the identity object
        if (amsdkdn.toLowerCase().startsWith("id=")) {
            try {
                return (new AMIdentity(amsdkdnObject, token));
            } catch (IdRepoException ide) {
                // this could be a AMSDK DN. Follow the AMSDK rules
                if (debug.messageEnabled()) {
                    debug.message("IdUtils:getIdentity(token, " +
                        amsdkdn + ") got exception: " + ide.getMessage() +
                        "\n\tContinuing with AMSDK DN check");
                }
            }
        }

        // Check for Special Users
        initializeSpecialUsers();
        if (specialUsers.contains(DNUtils.normalizeDN(amsdkdnObject))) {
            return (new AMIdentity(amsdkdnObject, token, LDAPDN.explodeDN(
                amsdkdnObject, true)[0], IdType.USER, ROOT_SUFFIX));
        }

        // Since "amsdkdn" is not a UUID, check if realm has AMSDK configured
        // This change is to avoid the issue of IdUtils always checking the 
        // users in AMSDK as IdUtils does not check if AMSDK is configured in 
        // any of the realms. 
        try {
            if (!ServiceManager.isAMSDKEnabled() || ((realm != null) && 
                !OrgConfigViaAMSDK.isAMSDKConfigured(realm)) ||
                    (!ServiceManager.isAMSDKConfigured())) { 
                // Not configured for AMSDK, return
                return (null);
            }
        } catch (SMSException smse) {
            // Ignore the exception and continue
        } 

        // Initialize root realm suffix, org and user naming attributes
        initializeForGetIdentity();

        // Determine if the amsdkdn is valid. Obtain name & type
        String name = null;
        IdType type = null;
        try {
            // Since we would using AMSDK, get AMDirectoryManager preload
            // all the attributes and check if it exists
            IDirectoryServices dsServices = 
                AMDirectoryAccessFactory.getDirectoryServices();
            // Preload/cache all the attributes assuming it is a user
            // Mainly for performance reasons, since getObjectType would
            // force multiple another directory lookup
            try {
                if (amsdkdn.startsWith(USER_NAMING_ATTR)) {
                    dsServices.getAttributes(token, amsdkdn, AMObject.USER);
                }
            } catch (Exception e) {
                // Ignore the exception and continue since this for cache
            }

            // Getting object type would use the cached attributes
            int sdkType = dsServices.getObjectType(token, amsdkdn);

            // Convert the sdkType to IdRepo type
            type = getType(AMStoreConnection.getObjectName(sdkType));
            name = AMConstants.CONTAINER_DEFAULT_TEMPLATE_ROLE;
            if (!type.equals(IdType.REALM)) {
                name = LDAPDN.explodeDN(amsdkdnObject, true)[0];
            }
        } catch (AMException ame) {
            // Debug the message and return null
            if (debug.messageEnabled()) {
                debug.message("IdUtils.getIdentity: Unable to resolve " +
                    "AMSDK DN: " + amsdkdn, ame);
            }
            return (null);
        } catch (SSOException ssoe) {
            // Debug the message and return null
            if (debug.messageEnabled()) {
                debug.message("IdUtils.getIdentity: Unable to resolve " +
                    "AMSDK DN. Got SSOException", ssoe);
            }
            return (null);
        }

        // Need to determine realm for amsdkdn
        String srealm = ROOT_SUFFIX;
        if (!amsdkdn.equals(ROOT_SUFFIX) &&
            !amsdkdn.equals(SERVICES_SUFFIX)) {
            // Need to get the object type and walk up the tree
            int index = amsdkdn.indexOf(ORG_NAMING_ATTR);
            if (index == 0) {
                srealm = OrgConfigViaAMSDK.getRealmForAMSDK(amsdkdn, realm);
            } else if (index > 0) {
                srealm = OrgConfigViaAMSDK.getRealmForAMSDK(
                    amsdkdn.substring(index), realm);
            }
            if (debug.messageEnabled()) {
                debug.message("IdUtils.getIdentity:: amsdkdn=" +
                    amsdkdn + " maps to realm=" + srealm);
            }
        } else if (amsdkdn.equals(SERVICES_SUFFIX)) {
            // Since amsdkdn points to services node,
            // it should be reset to root suffix
            amsdkdn = ROOT_SUFFIX;
        }

        return (new AMIdentity(amsdkdnObject, token, name, type, srealm));
    }

    /**
     * Returns the name of service which defines the profile information for
     * this type. Returns null, if nothing is defined.
     * 
     * @param type IdType whose service name is needed.
     * @return Name of the service.
     */
    public static String getServiceName(IdType type) {
        return (String) mapTypesToServiceNames.get(type.getName());
    }

    /**
     * Returns corresponding <code>IdType</code> object given a type.
     * 
     * @param type of object to return.
     * @return Idtype of type.
     * @throws IdRepoException if there are no corresponding types.
     */
    public static IdType getType(String type) throws IdRepoException {
        if (type.equalsIgnoreCase("managedrole")) {
            type = "role";
        } else if (type.equalsIgnoreCase("organization")
                || type.equalsIgnoreCase("organizationalunit")) {
            type = "realm";
        }

        IdType returnType = (IdType) mapSupportedTypes.get(type);
        if (returnType == null) {
            Object args[] = { type };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "217", args);
        }
        return returnType;
    }

    /**
     * Returns the matching DN from the AM SDK for this entry. This utility is
     * required by auth.
     * 
     * @param id  <code>AMIdentity</code> object.
     * @return <code>DN</code> of the object, as represented in the datastore.
     */
    public static String getDN(AMIdentity id) {
        if (id.getDN() != null) {
            return id.getDN();
        } else {
            return id.getUniversalId();
        }
    }

    /**
     * Returns an organization which maps to the identifier used by application
     * 
     * @param orgIdentifier  Organization identifier
     * @return Organization mapping to that identifier.
     */
    public static String getOrganization(SSOToken token, String orgIdentifier)
            throws IdRepoException, SSOException {
        // Check in cache first
        String id = null;
        if ((id = (String) orgIdentifierToOrgName.get(orgIdentifier)) != null) {
            return (id);
        }

        // Compute the organization name
        if (debug.messageEnabled()) {
            debug.message("IdUtils:getOrganization Input orgname: "
                    + orgIdentifier);
        }
        if (orgIdentifier == null || orgIdentifier.length() == 0
                || orgIdentifier.equals("/")) {
            // Return base DN
            id = DNMapper.orgNameToDN("/");
        } else if (orgIdentifier.startsWith("/")) {
            // If orgIdentifier is in "/" format covert to DN and return
            id = DNMapper.orgNameToDN(orgIdentifier);
        } else if (DN.isDN(orgIdentifier)) {
            id = orgIdentifier;
            try {
                // Search for realms with orgIdentifier name
                OrganizationConfigManager ocm = 
                    new OrganizationConfigManager(token, orgIdentifier);
            } catch (SMSException smse) {
                // debug message here.
                if (debug.messageEnabled()) {
                    debug.message("IdUtils.getOrganization Exception in "
                            + "getting org name from SMS", smse);
                }
                Object[] args = { orgIdentifier };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "401", 
                        args);            	
            }
        } else if (ServiceManager.isCoexistenceMode()) {
            // Return the org DN as determined by AMStoreConnection
            if (debug.messageEnabled()) {
                debug.message("IdUtils.getOrganization: getting from AMSDK");
            }
            try {
                AMStoreConnection amsc = new AMStoreConnection(token);
                id = amsc.getOrganizationDN(orgIdentifier, null);
            } catch (AMException ame) {
                if (debug.messageEnabled()) {
                    debug.message("IdUtils.getOrganization Exception in "
                            + "getting org name from AMSDK", ame);
                }
                throw convertAMException(ame);
            }
        } else {
            // Get the realm name from SMS
            if (debug.messageEnabled()) {
                debug.message("IdUtils.getOrganization: getting from " +
                    "SMS realms");
            }
            try {
                boolean foundOrg = false;
                ServiceManager sm = new ServiceManager(token);
                // First search for realms with orgIdentifier name
                OrganizationConfigManager ocm = sm
                        .getOrganizationConfigManager("/");
                Set subOrgNames = ocm.getSubOrganizationNames(orgIdentifier,
                        true);
                if (subOrgNames != null && !subOrgNames.isEmpty()) {
                    if (subOrgNames.size() == 1) {
                        id = DNMapper.orgNameToDN((String) subOrgNames
                                .iterator().next());
                        foundOrg = true;
                    } else {
                        for (Iterator items = subOrgNames.iterator();
                            items.hasNext();) {
                            // check for orgIdentifier
                            String subRealmName = (String) items.next();
                            StringTokenizer st = new StringTokenizer(
                                    subRealmName, "/");
                            // Need to handle the scenario where multiple
                            // sub-realm with the same name should not be
                            // allowed
                            while (st.hasMoreTokens()) {
                                if (st.nextToken().equalsIgnoreCase(
                                        orgIdentifier)) {
                                    if (!foundOrg) {
                                        id = DNMapper.orgNameToDN(subRealmName);
                                        foundOrg = true;
                                    } else {
                                        Object[] args = {orgIdentifier};
                                        throw new IdRepoException(IdRepoBundle
                                            .BUNDLE_NAME, "404", args);
                                    }
                                }
                            }
                        }
                    }
                }

                // Check if organization name has been determined
                if (debug.messageEnabled()) {
                    debug.message("IdUtils.getOrganization: getting from " +
                        "SMS realms aliases");
                }
                // perform organization alias search
                Set vals = new HashSet();
                vals.add(orgIdentifier);
                Set orgAliases = sm.searchOrganizationNames(
                    IdConstants.REPO_SERVICE,
                    IdConstants.ORGANIZATION_ALIAS_ATTR, vals);
                if (!foundOrg &&
                    ((orgAliases == null) || orgAliases.isEmpty())) {
                    if (debug.warningEnabled()) {
                        debug.warning("IdUtils.getOrganization Unable" +
                            " to find Org name for: " + orgIdentifier);
                    }
                    Object[] args = {orgIdentifier};
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                        "401", args);
                } else if ((orgAliases != null)  && (orgAliases.size() > 0) &&
                    (foundOrg || orgAliases.size() > 1)) {
                    // Multiple realms should not have the same alias
                    if (debug.warningEnabled()) {
                        debug.warning("IdUtils.getOrganization Multiple " +
                            " matching Orgs found for: " + orgIdentifier);
                    }
                    Object[] args = {orgIdentifier};
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                        "404", args);
                }
                if (!foundOrg) {
                    String tmpS = (String) orgAliases.iterator().next();
                    id = DNMapper.orgNameToDN(tmpS);
                }
            } catch (SMSException smse) {
                // debug message here.
                if (debug.messageEnabled()) {
                    debug.message("IdUtils.getOrganization Exception in "
                            + "getting org name from SMS", smse);
                }
                Object[] args = { orgIdentifier };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "401", 
                        args);
            }
        }

        if (debug.messageEnabled()) {
            debug.message("IdUtils:getOrganization Search for OrgIdentifier:" +
                orgIdentifier + " returning realm DN: " + id);
        }

        // Add to cache and return id
        orgIdentifierToOrgName.put(orgIdentifier, id);
        return id;
    }

    /**
     * Clears the cache containing orgIdentifiers to organization names
     */
    protected static void clearOrganizationNamesCache() {
        orgIdentifierToOrgName.clear();
        orgStatusCache.clear();
        if (debug.messageEnabled()) {
            debug.message("IdUtils.clearOrganizationNamesCache called");
        }
    }

    /**
     * Returs true or false, depending on if this organization is enabled or
     * not. The organization string passed to this method should be an
     * identifier returned from the method
     * <code> IdUtils.getOrganization </code>. In the default mode, where
     * relams are enabled but backward comaptibility is required, this checks
     * for organization status in the AM enabled Sun DS. Otherwise, it checks
     * for organization status from the realms tree.
     * 
     * @param token token SSOToken a valid SSOToken.
     * @param org name of the organization of interest.
     * @return <code>true</code> if org is active; 
     *    otherwise <code>false</code>
     * @throws IdRepoException if there are repository related error conditions.
     * @throws SSOException If user's single sign on token is invalid.
     */
    public static boolean isOrganizationActive(SSOToken token, String org)
            throws IdRepoException, SSOException {
        // Check the cache
        if (orgStatusCache.containsKey(org)) {
            return (((Boolean) orgStatusCache.get(org)).booleanValue());
        }
        boolean isActive = true;
        // Need to initialize ServiceManager by creating the constructor
        if (!ServiceManager.isCoexistenceMode()) {
            // Pick it up from the realms tree.
            try {
                OrganizationConfigManager ocm = new OrganizationConfigManager(
                        token, org);
                if (ocm == null) {
                    Object[] args = { org };
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "401",
                            args);
                }
                Map attributes = ocm.getAttributes(IdConstants.REPO_SERVICE);
                Set vals = (Set) attributes
                        .get(IdConstants.ORGANIZATION_STATUS_ATTR);
                if (vals == null || vals.isEmpty()) {
                    isActive = true;
                } else {
                    String stringActive = (String) vals.iterator().next();
                    isActive = stringActive.equalsIgnoreCase("Active");
                }
            } catch (SMSException smse) {
                Object args[] = { org };
                throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "401", 
                        args);
            }
        } else if (ServiceManager.isAMSDKEnabled()) {
            // Return the org DN as determined by AMStoreConnection.
            try {
                AMStoreConnection amsc = new AMStoreConnection(token);
                AMOrganization orgObj = amsc.getOrganization(org);
                isActive = orgObj.isActivated();
            } catch (AMException ame) {
                throw convertAMException(ame);
            }
        }
        // Add to cache
        orgStatusCache.put(org, isActive);
        return isActive;
    }

    private static void initializeForGetIdentity() {
        // Initialize root realm, if not already initalized
        if (ROOT_SUFFIX == null) {
            ROOT_SUFFIX = SMSEntry.getRootSuffix();
            StringBuilder sb = new StringBuilder(100);
            sb.append(SMSEntry.SERVICES_RDN)
                .append(SMSEntry.COMMA).append(ROOT_SUFFIX);
            SERVICES_SUFFIX = DNUtils.normalizeDN(sb.toString());
        }

        // Initialize organization and user naming attributes
        if ((ORG_NAMING_ATTR == null) || (USER_NAMING_ATTR == null)) {
            try {
                ORG_NAMING_ATTR = AMStoreConnection.getNamingAttribute(
                    AMObject.ORGANIZATION).toLowerCase() + "=";
                USER_NAMING_ATTR = AMStoreConnection.getNamingAttribute(
                    AMObject.USER).toLowerCase() + "=";
            } catch (AMException ame) {
                if (debug.warningEnabled()) {
                    debug.warning("IdUtils: unable to get naming " +
                        "attribute for org/user. Using \"o\"/\"uid\"");
                }
                ORG_NAMING_ATTR = "o=";
                USER_NAMING_ATTR = "uid=";
            }
        }
    }

    private static void initializeSpecialUsers() {
        // Populate special users
        if (specialUsers.isEmpty()) {
            String susers = SystemProperties.get(
                Constants.AUTHENTICATION_SPECIAL_USERS, "");
            StringTokenizer st = new StringTokenizer(
                susers, "|");
            while (st.hasMoreTokens()) {
                specialUsers.add(DNUtils.normalizeDN(st.nextToken()));
            }
            susers = SystemProperties.get(
                "com.sun.identity.authentication.super.user", "");
            specialUsers.add(DNUtils.normalizeDN(susers));
        }
    }
    
    /**
     * Returns an IdRepoException based on an <code>AMException</code>
     * 
     * @param ame
     * @return IdRepoException based on ame.
     */
    public static IdRepoException convertAMException(AMException ame) {
        Object[] args = ame.getMessageArgs();
        String eCode = ame.getErrorCode();
        IdRepoException ide = null;
        if (args == null) {
            ide = new IdRepoException("amProfile", eCode, null);
        } else { 
            ide = new IdRepoException("amProfile", ame.getErrorCode(), args);
        }
        ide.setLDAPErrorCode(ame.getLDAPErrorCode());
        return ide;
    }

    private static void loadDefaultTypes() {
        supportedTypes.add(IdType.REALM);
        supportedTypes.add(IdType.AGENT);
        supportedTypes.add(IdType.USER);
        supportedTypes.add(IdType.ROLE);
        supportedTypes.add(IdType.GROUP);
        supportedTypes.add(IdType.FILTEREDROLE);
        mapSupportedTypes.put(IdType.REALM.getName(), IdType.REALM);        
        mapSupportedTypes.put(IdType.USER.getName(), IdType.USER);
        mapSupportedTypes.put(IdType.ROLE.getName(), IdType.ROLE);
        mapSupportedTypes.put(IdType.FILTEREDROLE.getName(),
                IdType.FILTEREDROLE);
        mapSupportedTypes.put(IdType.AGENT.getName(), IdType.AGENT);
        mapSupportedTypes.put(IdType.GROUP.getName(), IdType.GROUP);
        Set memberSet = new HashSet();
        memberSet.add(IdType.ROLE);
        memberSet.add(IdType.GROUP);
        memberSet.add(IdType.FILTEREDROLE);
        typesCanBeMemberOf.put(IdType.USER.getName(), memberSet);
        Set memberShipSet = new HashSet();
        memberShipSet.add(IdType.USER);
        typesCanHaveMembers.put(IdType.ROLE.getName(), memberShipSet);
        typesCanHaveMembers.put(IdType.GROUP.getName(), memberShipSet);
        typesCanHaveMembers.put(IdType.FILTEREDROLE.getName(), memberShipSet);
        typesCanAddMembers.put(IdType.GROUP.getName(), memberShipSet);
        typesCanAddMembers.put(IdType.ROLE.getName(), memberShipSet);
    }

    private static Set getMemberSet(Set members) {
        Set memberSet = new HashSet(members.size() * 2);
        for (Iterator iter = members.iterator(); iter.hasNext();) {
            String currType = (String) iter.next();
            memberSet.add(new IdType(currType));
        }
        return memberSet;
    }

    /**
     * Returns the user name extracted from the uuid
     * if the orgName supplied in the parameter is 
     * not same realm name in uuid then <code>IdRepoException</code>
     * is thrown
     * 
     * @param uuid uuid of the user
     * @param orgName the org user is trying to login to
     * @return user name
     * @throws IdRepoException 
     */
    public static String getIdentityName(String uuid, String orgName)
        throws IdRepoException {
        String username = uuid;
        // Check uuid
        if ((uuid != null) && uuid.toLowerCase().startsWith("id=")) {
            // Could be universal id, get the identity object
            AMIdentity id = new AMIdentity(null, uuid);
            username = id.getName();
            // Check the realm names
            String realm = DNUtils.normalizeDN(id.getRealm());
            if (!DNUtils.normalizeDN(orgName).equals(realm)) {
                Object[] args = {uuid, orgName};
		throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "403", args);
            }
        }
        return (username);
    } 
    
    // SMS service listener to reinitialize if IdRepo service changes
    static class IdUtilsListener implements com.sun.identity.sm.ServiceListener 
    {
        public void schemaChanged(String serviceName, String version) {
            initialize();
        }

        public void globalConfigChanged(String serviceName, String version,
            String groupName, String serviceComponent, int type) {
            initialize();
            clearOrganizationNamesCache();
        }

        public void organizationConfigChanged(String serviceName,
            String version, String orgName, String groupName,
            String serviceComponent, int type) {
            clearOrganizationNamesCache();
        }
    }
}
