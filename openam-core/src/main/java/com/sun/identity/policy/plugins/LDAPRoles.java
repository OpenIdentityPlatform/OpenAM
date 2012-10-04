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
 * $Id: LDAPRoles.java,v 1.8 2009/11/20 23:52:55 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation 
 */
package com.sun.identity.policy.plugins;

import java.util.*;
import com.sun.identity.shared.ldap.*;
import com.sun.identity.shared.ldap.util.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPBindRequest;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPSearchRequest;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.SubjectEvaluationCache;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.interfaces.Subject;

/**
 * This class represents a group of LDAP roles
 */
public class LDAPRoles implements Subject {

    static final String LDAP_OBJECT_CLASS = "objectclass";
    static final String LDAP_ROLE_ATTR = "nsroledefinition";
    static final String LDAP_USER_ROLE_ATTR = "nsrole";
    static final String LDAP_SCOPE_BASE = "SCOPE_BASE";
    static final String LDAP_SCOPE_ONE = "SCOPE_ONE";
    static final String LDAP_SCOPE_SUB = "SCOPE_SUB";

    /*
    * Cache for user's LDAPRoles based on the
    * external directory server where the
    * LDAPRoles are retrieved from.
    * key for this cache would be token ID and 
    * value would be Map of ldap server to an array of 2 Objects, 
    * one being the timeToLive
    * 2 being the Set of all the user roles
    * tokenID ----> ldapServer1 --> [timeToLive, set of rolesDNS]
    *               ldapServer2 --> [timeToLive, set of rolesDNS]
    *               ....
    *               ....
    */                     
    public static Map userLDAPRoleCache = 
              Collections.synchronizedMap(new HashMap());

    // Variables
    private boolean initialized = false;
    private Set selectedRoleDNs = Collections.EMPTY_SET;
    private Set selectedRFCRoleDNs = Collections.EMPTY_SET;
    private String authid;
    private String authpw;
    private String baseDN;
    private String roleSearchFilter;
    private int roleSearchScope = LDAPv2.SCOPE_SUB;
    private String userSearchFilter;
    private int userSearchScope = LDAPv2.SCOPE_SUB;
    private String roleRDNAttrName;
    private String userRDNAttrName;
    private int timeLimit;
    private int maxResults;
    private boolean sslEnabled = false;
    private int minPoolSize;
    private int maxPoolSize;
    private String orgName;
    private LDAPConnectionPool connPool;
    private boolean localDS;
    private boolean aliasEnabled;
    private String ldapServer;

    // Debug
    static Debug debug = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);
    /**
     * Constructor with no parameter
     */
    public LDAPRoles() {
        // do nothing
    }


    /** 
     * Initialize the LDAPGroup object by using the configuration
     * information passed by the Policy Framework.
     * @param configParams the configuration information
     * @exception PolicyException if an error occured during 
     * initialization of the instance
     */
    public void initialize(Map configParams) throws PolicyException {
 
        if (configParams == null) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "ldaproles_initialization_failed", null, null));
        }
        String configuredLdapServer = 
            (String)configParams.get(PolicyConfig.LDAP_SERVER);
        if (configuredLdapServer == null) {
            debug.error("LDAPRoles.initialize(): failed to get LDAP "
              + "server name. If you enter more than one server name "
              + "in the policy config service's Primary LDAP Server "
              + "field, please make sure the ldap server name is preceded " 
              + "with the local server name.");
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_ldap_server_host", null, null));
        }
        ldapServer = configuredLdapServer.toLowerCase(); 
        localDS = PolicyUtils.isLocalDS(ldapServer);

        aliasEnabled = Boolean.valueOf((String) configParams.get(
            PolicyConfig.USER_ALIAS_ENABLED)).booleanValue();
 
        authid = (String) configParams.get(PolicyConfig.LDAP_BIND_DN);
        authpw = (String) configParams.get(PolicyConfig.LDAP_BIND_PASSWORD);
        if (authpw != null) {
            authpw = PolicyUtils.decrypt(authpw);
        }
        baseDN = (String) configParams.get(PolicyConfig.LDAP_BASE_DN);
        roleSearchFilter = (String) configParams.get(
            PolicyConfig.LDAP_ROLES_SEARCH_FILTER);
        String scope = (String) configParams.get(
            PolicyConfig.LDAP_ROLES_SEARCH_SCOPE);
        if (scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            roleSearchScope = LDAPv2.SCOPE_BASE;
        } else if (scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            roleSearchScope = LDAPv2.SCOPE_ONE;
        } else {
            roleSearchScope = LDAPv2.SCOPE_SUB;
        }

        roleRDNAttrName = (String) configParams.get(
            PolicyConfig.LDAP_ROLES_SEARCH_ATTRIBUTE);
        userSearchFilter = (String) configParams.get(
            PolicyConfig.LDAP_USERS_SEARCH_FILTER);
        scope = (String) configParams.get(PolicyConfig.LDAP_USERS_SEARCH_SCOPE);
        if (scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            userSearchScope = LDAPv2.SCOPE_BASE;
        } else if (scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            userSearchScope = LDAPv2.SCOPE_ONE;
        } else {
            userSearchScope = LDAPv2.SCOPE_SUB;
        }

        userRDNAttrName = (String) configParams.get(
            PolicyConfig.LDAP_USER_SEARCH_ATTRIBUTE);
        try {
            timeLimit = Integer.parseInt(
                (String) configParams.get(PolicyConfig.LDAP_SEARCH_TIME_OUT));
            maxResults = Integer.parseInt(
                (String) configParams.get(PolicyConfig.LDAP_SEARCH_LIMIT));
            minPoolSize = Integer.parseInt(
                (String) configParams.get(
                PolicyConfig.LDAP_CONNECTION_POOL_MIN_SIZE));
            maxPoolSize = Integer.parseInt(
                (String) configParams.get(
                PolicyConfig.LDAP_CONNECTION_POOL_MAX_SIZE));
        } catch (NumberFormatException nfe) {
            throw (new PolicyException(nfe));
        }

        String ssl = (String) configParams.get(PolicyConfig.LDAP_SSL_ENABLED);
        if (ssl.equalsIgnoreCase("true")) {
            sslEnabled = true;
        } else {
            sslEnabled = false;
        }

        // get the organization name
        Set orgNameSet = (Set) configParams.get(
            PolicyManager.ORGANIZATION_NAME);
        if ((orgNameSet != null) && (!orgNameSet.isEmpty())) {
            Iterator items = orgNameSet.iterator();
            orgName = (String) items.next();
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPRoles.initialize(): getting params" 
                           + "\nldapServer: " + ldapServer 
                           + "\nauthid: " + authid
                           + "\nbaseDN: " + baseDN
                           + "\nroleSearchFilter: " + roleSearchFilter
                           + "\nroleRDNAttrName: " + roleRDNAttrName
                           + "\nuserSearchFilter: " + userSearchFilter
                           + "\nuserRDNAttrName: " + userRDNAttrName
                           + "\ntimeLimit: " + timeLimit
                           + "\nmaxResults: " + maxResults
                           + "\nminPoolSize: " + minPoolSize
                           + "\nmaxPoolSize: " + maxPoolSize
                           + "\nSSLEnabled: " + sslEnabled
                           + "\nOrgName: " + orgName);
        }

        // initialize the connection pool for the ldap server 
        LDAPConnectionPools.initConnectionPool(ldapServer, 
                authid, authpw, sslEnabled, minPoolSize, maxPoolSize);
        connPool = LDAPConnectionPools.getConnectionPool(ldapServer);
        initialized = true;
    }


    /**
     * Returns the syntax of the values the <code>LDAPRoles</code> 
     * @see com.sun.identity.policy.Syntax
     * @param token the <code>SSOToken</code> that will be used
     * to determine the syntax
     * @return set of of valid names for the user collection.
     * @exception SSOException if <code>SSOToken></code> is not valid
     */
    public Syntax getValueSyntax(SSOToken token) throws SSOException {
        return (Syntax.MULTIPLE_CHOICE);
    }

    /**
     * Returns a list of possible values for the <code>LDAPRoles
     * </code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if <code>SSOToken></code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */

    public ValidValues getValidValues(SSOToken token) throws
        SSOException, PolicyException {
        return (getValidValues(token, "*"));
    }

    /**
     * Returns a list of possible values for the <code>LDAPRoles
     * </code> that satisfy the given <code>pattern</code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     * @param pattern search pattern that will be used to narrow
     * the list of valid names.
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if <code>SSOToken></code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */

    public ValidValues getValidValues(SSOToken token, String pattern)
        throws SSOException, PolicyException {
        if (!initialized) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "ldaproles_subject_not_yet_initialized",null, null));
        }

        String searchFilter = null;
        if ((pattern != null) && !(pattern.trim().length() == 0)) {
            searchFilter = "(&" + roleSearchFilter + "(" + roleRDNAttrName + 
            "=" + pattern + "))";
        } else {
           searchFilter = roleSearchFilter;
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPRoles.getValidValues(): role search filter is: " 
                + searchFilter);
        }
        String[] attrs = { roleRDNAttrName };
        LDAPConnection ld = null;
        Set validRoleDNs = new HashSet();
        int status = ValidValues.SUCCESS;
        try {
            LDAPSearchResults res = null;
            LDAPBindRequest bindRequest = LDAPRequestParser.parseBindRequest(
                3, authid, authpw);
            LDAPSearchRequest searchRequest =
                LDAPRequestParser.parseSearchRequest(baseDN, roleSearchScope,
                searchFilter, attrs, false, timeLimit,
                LDAPRequestParser.DEFAULT_DEREFERENCE, maxResults);
            try {
                ld = connPool.getConnection();
                // connect to the server to authenticate
                try {
                    ld.authenticate(bindRequest);
                } catch (LDAPException connEx) {
                    // fallback to ldap v2 if v3 is not supported
                    if (connEx.getLDAPResultCode() ==
                        LDAPException.PROTOCOL_ERROR)
                    {
                        if (debug.messageEnabled()) {
                            debug.message("LDAPRoles.getValidValues(): "+
                            "Bind with LDAPv3 failed, retrying with v2");
                        }
                        bindRequest = LDAPRequestParser.parseBindRequest(
                                2, authid, authpw);
                        ld.authenticate(bindRequest);
                    } else {
                        throw connEx;
                    }
                }
                res = ld.search(searchRequest);
            } finally {
                if (ld != null) {
                    connPool.close(ld);
                }
            }
            while (res.hasMoreElements()) {
                try {
                    LDAPEntry entry = res.next();
                    if (entry != null) {
                        validRoleDNs.add(entry.getDN());
                        if (debug.messageEnabled()) {
                            debug.message("LDAPRoles.getValidValues(): "+
                                "found role name=" + entry.getDN());
                        }
                    }
                } catch (LDAPReferralException lre) {
                    // ignore referrals
                    continue;
                } catch (LDAPException le) {
                    String objs[] = { orgName };
                    int resultCode = le.getLDAPResultCode();
                    if (resultCode == le.SIZE_LIMIT_EXCEEDED) {
                        debug.warning("LDAPRoles.getValidValues(): "+
                             "exceeded the size limit");
                        status = ValidValues.SIZE_LIMIT_EXCEEDED;
                    } else if (resultCode == le.TIME_LIMIT_EXCEEDED) {
                        debug.warning("LDAPRoles.getValidValues(): "+
                            "exceeded the time limit");
                        status = ValidValues.TIME_LIMIT_EXCEEDED;
                    } else {
                        throw (new PolicyException(le));
                    }
                }
            }
        } catch (LDAPException lde) {
            int ldapErrorCode = lde.getLDAPResultCode();
            if (ldapErrorCode == LDAPException.INVALID_CREDENTIALS) {
                throw (new PolicyException(ResBundleUtils.rbName,
                    "ldap_invalid_password", null, null));
            } else if (ldapErrorCode == LDAPException.NO_SUCH_OBJECT) {
                String objs[] = { baseDN };
                throw (new PolicyException(ResBundleUtils.rbName,
                    "no_such_ldap_base_dn", objs, null));
            } 
            String errorMsg = lde.getLDAPErrorMessage(); 
            String additionalMsg = lde.errorCodeToString(); 
            if (additionalMsg != null) {
                throw (new PolicyException(
                         errorMsg + ": " + additionalMsg));
            } else { 
                throw (new PolicyException(errorMsg));
            }
        } catch (Exception e) {
            throw (new PolicyException(e));
        }
        return(new ValidValues(status, validRoleDNs));
    }


    /**
     * Returns the display name for the value for the given locale.
     * For all the valid values obtained through the methods
     * <code>getValidValues</code> this method must be called
     * by GUI and CLI to get the corresponding display name.
     * The <code>locale</code> variable could be used by the
     * plugin to customize the display name for the given locale.
     * The <code>locale</code> variable could be <code>null</code>, 
     * in which case the plugin must use the default locale (most probabily 
     * en_US).
     * Alternatively, if the plugin does not have to localize
     * the value, it can just return the <code>value</code> as is.
     *
     * @param value one of the valid value for the plugin
     * @param locale locale for which the display name must be customized
     *
     * @exception NameNotFoundException if the given <code>value</code>
     * is not one of the valid values for the plugin
     */
    public String getDisplayNameForValue(String value, Locale locale)
        throws NameNotFoundException {
        return PolicyUtils.getDNDisplayString(value);
    }


    /**
     * Returns the values that was set using the
     * method <code>setValues</code>.
     *
     * @return <code>Set</code> of values that have been set for the 
     * user collection.
     */

    public Set getValues() {
        if (debug.messageEnabled()) {
            debug.message("LDAPRoles.getValues() gets called");
        }
        return selectedRoleDNs;
    }


    /**
     * Sets the names for the instance of the <code>LDAPRoles</code>
     * object. The names are obtained from the policy object,
     * usually configured when a policy is created. 
     *
     * @param names names selected for the instance of
     * the user collection object.
     *
     * @exception InvalidNameException if the given names are not valid
     */

    public void setValues(Set names) throws InvalidNameException {
        if (names == null) {
            debug.error("LDAPRoles.setValues() Invalid names");
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "ldaproles_subject_invalid_group_names", null, 
                null, PolicyException.USER_COLLECTION));
        }
        selectedRoleDNs = new HashSet();
        selectedRoleDNs.addAll(names);
        if (debug.messageEnabled()) {
            debug.message("LDAPRoles.setValues(): selected role names=" + 
                selectedRoleDNs);
        }
        selectedRFCRoleDNs = new HashSet();
        // add to the RFC Set now
        Iterator it = names.iterator();
        while (it.hasNext()) {
            selectedRFCRoleDNs.add(new DN((String)it.next()).toRFCString().
                toLowerCase());
        }
    }


    /**
     * Determines if the user identified by the token,
     * belongs to this instance of the <code>LDAPRoles</code> object.
     *
     * @param token single-sign-on token of the user
     *
     * @return <code>true</code> if the user is member of the
     * given subject; <code>false</code> otherwise.
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception PolicyException if an error occured while
     * checking if the user is a member of this subject
     */

    public boolean isMember(SSOToken token)
        throws SSOException, PolicyException {

        boolean roleMatch = false;
        String userLocalDN = token.getPrincipal().getName();
        if (selectedRFCRoleDNs.size() > 0) {
            LDAPEntry userEntry = null;
            Set userRoles = null;
            boolean listenerAdded = false;
            String tokenID = token.getTokenID().toString();
            Iterator items = selectedRFCRoleDNs.iterator();
            while ( items.hasNext()) {
                String roleName = (String)items.next();
                Boolean matchFound = null;
                if ((matchFound = SubjectEvaluationCache.isMember(
                        tokenID, ldapServer,roleName)) != null) {
                    if (debug.messageEnabled()) {
                        debug.message("LDAPRoles.isMember():Got membership "
                            +"from cache of " +userLocalDN+" in LDAP role "
                            +roleName+ " :" +matchFound.booleanValue());
                    }
                    boolean result = matchFound.booleanValue();
                    if (result) {
                        return result;
                    } else {
                        continue;
                    }
                }
                // came here so no entry in subject eval cache
                if (debug.messageEnabled()) {
                    debug.message("LDAPRoles.isMember():did not find entry "
                        +" for " +roleName+ " in SubjectEvaluation "
                        + "cache,  getting from LDAPRole cache");
                }
                if (userEntry == null) {
                    userEntry = getUserEntry(token);
                    if (userEntry == null) {
                        if (debug.messageEnabled()) {
                            debug.message("LDAPRoles.isMember(): User " 
                                + userLocalDN
                                + " is not found in the directory"); 
                        }
                        return false;
                    }
                }
                if (userRoles == null) {
                    userRoles = getUserRoles(token,userEntry);        
                }
                if (!listenerAdded) {
                    if (!PolicyEvaluator.ssoListenerRegistry.containsKey(
                        tokenID)) 
                    {
                        token.addSSOTokenListener(PolicyEvaluator.ssoListener);
                        PolicyEvaluator.ssoListenerRegistry.put(tokenID, 
                            PolicyEvaluator.ssoListener);
                        if (debug.messageEnabled()) {
                           debug.message("LDAPRoles.isMember(): sso listener "
                                + "added .\n");
                        }
                        listenerAdded = true;
                    }
                }
                if ((userRoles != null) && userRoles.size() > 0) {
                    // check if the user belongs to any of the selected roles
                    if (userRoles.contains(roleName)) {
                       roleMatch = true;
                    }
                }
                if (debug.messageEnabled()) {
                    String member = (roleMatch) ? "is member of":
                        "is not a member of";
                    debug.message("LDAPRoles.isMember(): User " 
                        + userLocalDN + " "+member+ " the LDAPRole "+roleName
                            +", adding to Subject eval cache");
                }
                SubjectEvaluationCache.addEntry(tokenID, ldapServer,
                    roleName, roleMatch);
                if (roleMatch) {
                    break;
                }
            }
        }
        if (debug.messageEnabled()) {
            if (!roleMatch) { 
                debug.message("LDAPRoles.isMember(): User " + userLocalDN 
                  + " is not a member of this LDAPRoles object"); 
            } else {
                debug.message("LDAPRoles.isMember(): User " + userLocalDN 
                  + " is a member of this LDAPRoles object"); 
            }
        }
        return roleMatch;
    }


   /** 
    * Return a hash code for this <code>LDAPRoles</code>.
    *
    * @return a hash code for this <code>LDAPRoles</code> object.
    */

    public int hashCode() {
        return selectedRoleDNs.hashCode();
    }


    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o another object that will be compared with this one
     *
     * @return <code>true</code> if eqaul; <code>false</code>
     * otherwise
     */

    public boolean equals(Object o) {
        if (o instanceof LDAPRoles) {
            LDAPRoles roles = (LDAPRoles) o;
            if ((selectedRoleDNs != null) 
                 && (roles.selectedRoleDNs != null) 
                 && (selectedRoleDNs.equals(roles.selectedRoleDNs))) {
                return true;
            }
        }
        return false;
    }


    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */

    public Object clone() {
        LDAPRoles theClone = null;
        try {
            theClone = (LDAPRoles) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (selectedRoleDNs != null) {
            theClone.selectedRoleDNs = new HashSet();
            theClone.selectedRoleDNs.addAll(selectedRoleDNs);
        }
        if (selectedRFCRoleDNs != null) {
            theClone.selectedRFCRoleDNs = new HashSet();
            theClone.selectedRFCRoleDNs.addAll(selectedRFCRoleDNs);
        }
        return theClone;
    }

    /**
     *  returns user's roles from userLDAPRoleCache if found, 
     *  else gets users roles from the directory
     *  @return <code>Set</code> of Role DNs.
     */
    private Set getUserRoles(SSOToken token, LDAPEntry userEntry) throws
        SSOException, PolicyException 
    {
        if (token == null) {
            return null;
        }
        String tokenIDStr = token.getTokenID().toString();
        Map serverRoleMap = null;
        if ((serverRoleMap = (Map)userLDAPRoleCache.get(tokenIDStr)) != null) {
            Object[] element = (Object[])serverRoleMap.get(ldapServer);
            if (element != null) {
                long timeToLive = (element[0] == null) ? 
                    0:((Long)element[0]).longValue();
                long currentTime = System.currentTimeMillis();
                if (timeToLive > currentTime) {
                    if (debug.messageEnabled()) {
                        debug.message("LDAPRoles.getUserRoles():"
                                  + " get the nsrole values from cache.\n");
                    }
                    return (Set)element[1];
                }
            }
        }
        // add or update the cache entry.
        // we come here either the token is not registered with the
        // cache or the cache element is out of date. 
        // get the user DN from the directory server.
        Set roles = new HashSet();
        if (userEntry != null) {
            LDAPAttribute attribute
                = userEntry.getAttribute(LDAP_USER_ROLE_ATTR);
            if (attribute != null) {
                Enumeration enumVals = attribute.getStringValues();
                while (enumVals.hasMoreElements()) {
                    roles.add(new DN((String)enumVals.nextElement()).
                        toRFCString().toLowerCase());
                }
            }
            // If the cache is enabled
            if (SubjectEvaluationCache.getSubjectEvalTTL() > 0) {
                Object[] elem = new Object[2];
                elem[0] = new Long(System.currentTimeMillis()
                                + SubjectEvaluationCache.getSubjectEvalTTL());
                elem[1] = roles;
                serverRoleMap = null;
                if ((serverRoleMap = (Map)userLDAPRoleCache.get(tokenIDStr))
                    == null)
                {
                    serverRoleMap = Collections.synchronizedMap(new HashMap());
                    serverRoleMap.put(ldapServer,elem);
                    userLDAPRoleCache.put(tokenIDStr, serverRoleMap);
                } else {
                    serverRoleMap.put(ldapServer,elem);
                }
            }
        }
        return roles;
    }

    /**
     * Gets the <code>LDAPEntry</code> for a user identified 
     * by the token. The base DN used to perform the user search
     * is the DN of the user if the user is local to speed
     * up the search, but if user is not local then the base DN as
     * configured in the policy config service is used.
     */
    private LDAPEntry getUserEntry(SSOToken token) throws SSOException,
        PolicyException 
    {
        Set qualifiedUsers = new HashSet();
        String userLocalDN = token.getPrincipal().getName();
    
        if (debug.messageEnabled()) {
            debug.message("LDAPRoles.getUserEntry(): user local DN is " 
                    +  userLocalDN);
        }

        String searchBaseDN = baseDN;
        if (localDS && !PolicyUtils.principalNameEqualsUuid( token)) {
           // if it is local, then we search the user entry only
           searchBaseDN = (new DN(userLocalDN)).toString();
           if (debug.messageEnabled()) {
               debug.message("LDAPRoles.getUserEntry(): search user " 
                             + searchBaseDN + " only as it is local.");
           }
        } 

        // try to figure out the user name from the local user DN
        int beginIndex = userLocalDN.indexOf("=");
        int endIndex = userLocalDN.indexOf(",");
        if ((beginIndex <= 0) || (endIndex <= 0) ||
            (beginIndex >= endIndex)) {
            throw (new PolicyException(ResBundleUtils.rbName,
              "ldaproles_subject_invalid_local_user_dn", null, null));
        }
        String userName = userLocalDN.substring(
                                     beginIndex+1, endIndex);
        String searchFilter = null;
        if ((userSearchFilter != null) 
            && !(userSearchFilter.length() == 0)) {
            searchFilter = "(&" + userSearchFilter +
                PolicyUtils.constructUserFilter(
                    token, userRDNAttrName, userName, aliasEnabled) + ")";
        } else {
            searchFilter = PolicyUtils.constructUserFilter(
                token, userRDNAttrName, userName, aliasEnabled);
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPRoles.getUserEntry(): search filter is: " 
                          + searchFilter);
        }
        
        // search the remote ldap and find out the user DN        
        LDAPConnection ld = null;
        String[] myAttrs = { LDAP_USER_ROLE_ATTR };
        try {
            LDAPSearchResults res = null;
            LDAPBindRequest bindRequest = LDAPRequestParser.parseBindRequest(
                authid, authpw);
            LDAPSearchRequest searchRequest =
                LDAPRequestParser.parseSearchRequest(searchBaseDN,
                userSearchScope, searchFilter, myAttrs, false, timeLimit,
                LDAPRequestParser.DEFAULT_DEREFERENCE, maxResults);
            try {
                ld = connPool.getConnection();
                // connect to the server to authenticate
                ld.authenticate(bindRequest);
                res = ld.search(searchRequest);
            } finally {
                if (ld != null) {
                    connPool.close(ld);
                }
            }
            while (res.hasMoreElements()) {
                try {
                    LDAPEntry entry = res.next();
                    if (entry != null) {
                        qualifiedUsers.add(entry);
                    }
                } catch (LDAPReferralException lre) {
                    // ignore referrals
                    continue;
                } catch (LDAPException le) {
                    String objs[] = { orgName };
                    int resultCode = le.getLDAPResultCode();
                    if (resultCode == le.SIZE_LIMIT_EXCEEDED) {
                        debug.warning(
                     "LDAPRoles.isMember(): exceeded the size limit");
                        throw (new PolicyException(ResBundleUtils.rbName,
                        "ldap_search_exceed_size_limit", objs, null));
                    } else if (resultCode == le.TIME_LIMIT_EXCEEDED) {
                        debug.warning(
                     "LDAPRoles.isMember(): exceeded the time limit");
                        throw (new PolicyException(ResBundleUtils.rbName,
                        "ldap_search_exceed_time_limit", objs, null));
                    } else {
                        throw (new PolicyException(le));
                    }
                }
            }
        } catch (LDAPException lde) {
            int ldapErrorCode = lde.getLDAPResultCode();
            if (ldapErrorCode == LDAPException.INVALID_CREDENTIALS) {
                throw (new PolicyException(ResBundleUtils.rbName,
                    "ldap_invalid_password", null, null));
            } else if (ldapErrorCode == LDAPException.NO_SUCH_OBJECT) {
                String objs[] = { baseDN };
                throw (new PolicyException(ResBundleUtils.rbName,
                    "no_such_ldap_base_dn", objs, null));
            } 
            String errorMsg = lde.getLDAPErrorMessage();
            String additionalMsg = lde.errorCodeToString(); 
            if (additionalMsg != null) {
                throw (new PolicyException(
                          errorMsg + ": " + additionalMsg));
            } else {
                throw (new PolicyException(errorMsg));
            }
        } catch (Exception e) {
            throw (new PolicyException(e));
        }
        if (qualifiedUsers.size() > 0) {
            Iterator iter = qualifiedUsers.iterator();
            // we only take the first qualified DN
            return (LDAPEntry)iter.next();
        }
        return null;
    }
}
