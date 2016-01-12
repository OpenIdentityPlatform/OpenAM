/*
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
 * $Id: LDAPUsers.java,v 1.7 2009/11/20 23:52:55 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation 
 */
package com.sun.identity.policy.plugins;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.SubjectEvaluationCache;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.shared.debug.Debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.DereferenceAliasesPolicy;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

/**
 * This class respresents a group of LDAP users
 */
public class LDAPUsers implements Subject {

    static final String LDAP_SCOPE_BASE = "SCOPE_BASE";
    static final String LDAP_SCOPE_ONE = "SCOPE_ONE";
    static final String LDAP_SCOPE_SUB = "SCOPE_SUB";

    // Variables
    private boolean initialized = false;
    private Set<String> selectedUserDNs = Collections.emptySet();
    private Set<String> selectedRFCUserDNs = Collections.emptySet();
    private String baseDN;
    private String userSearchFilter;
    private SearchScope userSearchScope = SearchScope.WHOLE_SUBTREE;
    private String userRDNAttrName;
    private int timeLimit;
    private int maxResults;
    private String orgName;
    private ConnectionFactory connPool;
    private boolean localDS;
    private String ldapServer;
    private boolean aliasEnabled;

    // Debug
    static Debug debug = Debug.getInstance(
                          PolicyManager.POLICY_DEBUG_NAME);

    /**
     * Constructor with no parameter
     */
    public LDAPUsers() {
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
                "ldapusers_initialization_failed", null, null));
        }

        String configuredLdapServer = 
            (String)configParams.get(PolicyConfig.LDAP_SERVER);
        if (configuredLdapServer == null) {
            debug.error("LDAPUsers.initialize(): failed to get LDAP "
              + "server name. If you enter more than one server name "
              + "in the policy config service's Primary LDAP Server "
              + "field, please make sure the ldap server name is preceded " 
              + "with the local server name.");
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_ldap_server_host", null, null));
        }
        ldapServer = configuredLdapServer.toLowerCase();
        localDS = PolicyUtils.isLocalDS(ldapServer);

        aliasEnabled = Boolean.valueOf((String) configParams.get(PolicyConfig.USER_ALIAS_ENABLED));

        String authid = (String) configParams.get(PolicyConfig.LDAP_BIND_DN);
        String authpw = (String) configParams.get(PolicyConfig.LDAP_BIND_PASSWORD);
        if (authpw != null) {
            authpw = PolicyUtils.decrypt(authpw);
        }
        baseDN = (String) configParams.get(PolicyConfig.LDAP_USERS_BASE_DN);

        userSearchFilter = (String) configParams.get(
                              PolicyConfig.LDAP_USERS_SEARCH_FILTER);
        String scope = (String) configParams.get(
                              PolicyConfig.LDAP_USERS_SEARCH_SCOPE);
        if (scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            userSearchScope = SearchScope.BASE_OBJECT;
        } else if (scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            userSearchScope = SearchScope.SINGLE_LEVEL;
        } else {
            userSearchScope = SearchScope.WHOLE_SUBTREE;
        }

        userRDNAttrName = (String) configParams.get(
                              PolicyConfig.LDAP_USER_SEARCH_ATTRIBUTE);
        int minPoolSize;
        int maxPoolSize;
        try {
            timeLimit = Integer.parseInt(
                 (String) configParams.get(PolicyConfig.LDAP_SEARCH_TIME_OUT));
            maxResults = Integer.parseInt(
                 (String) configParams.get(PolicyConfig.LDAP_SEARCH_LIMIT));
            minPoolSize = Integer.parseInt(
                 (String) configParams.get(
                            PolicyConfig.LDAP_CONNECTION_POOL_MIN_SIZE));
            maxPoolSize = Integer.parseInt((String) configParams.get(
                            PolicyConfig.LDAP_CONNECTION_POOL_MAX_SIZE));
        } catch (NumberFormatException nfe) {
            throw (new PolicyException(nfe));
        }

        boolean sslEnabled = Boolean.valueOf((String) configParams.get(PolicyConfig.LDAP_SSL_ENABLED));

        // get the organization name
        Set orgNameSet = (Set) configParams.get(
            PolicyManager.ORGANIZATION_NAME);
        if ((orgNameSet != null) && (!orgNameSet.isEmpty())) {
            Iterator items = orgNameSet.iterator();
            orgName = (String) items.next();
        }


        if (debug.messageEnabled()) {
            debug.message("LDAPUsers.initialize(): getting params" 
                           + "\nldapServer: " + ldapServer 
                           + "\nauthid: " + authid
                           + "\nbaseDN: " + baseDN
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
     * Returns the syntax of the values the <code>LDAPUsers</code> 
     * @see com.sun.identity.policy.Syntax
     * @param token the <code>SSOToken</code> that will be used
     * to determine the syntax
     * @return set of of valid names for the user collection.
     * @exception SSOException if <code>SSOToken</code> is not valid
     */
    public Syntax getValueSyntax(SSOToken token) throws SSOException {
        return (Syntax.MULTIPLE_CHOICE);
    }

    /**
     * Returns a list of possible values for the <code>LDAPUsers
     * </code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */

    public ValidValues getValidValues(SSOToken token) throws
        SSOException, PolicyException {
        return (getValidValues(token, "*"));
    }

    /**
     * Returns a list of possible values for the <code>LDAPUsers
     * </code> that satisfy the given <code>pattern</code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     * @param pattern search pattern that will be used to narrow
     * the list of valid names.
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public ValidValues getValidValues(SSOToken token, String pattern)
        throws SSOException, PolicyException 
    {
        if (!initialized) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "ldapusers_subject_not_yet_initialized", null, null));
        }

        String searchFilter = getSearchFilter(pattern);
        Set<String> validUserDNs = new HashSet<>();
        int status = ValidValues.SUCCESS;

        try (Connection ld = connPool.getConnection()){
            ConnectionEntryReader res = search(searchFilter, ld, userRDNAttrName);
            while (res.hasNext()) {
                try {
                    if (res.isEntry()) {
                        SearchResultEntry entry = res.readEntry();
                        String name = entry.getName().toString();
                        validUserDNs.add(name);
                        debug.message("LDAPUsers.getValidValues(): found user name={}", name);
                    } else {
                        // ignore referrals
                        debug.message("LDAPUsers.getValidValues(): Ignoring reference: {}", res.readReference());
                    }
                } catch (LdapException e) {
                    ResultCode resultCode = e.getResult().getResultCode();
                    if (resultCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                        debug.warning("LDAPUsers.getValidValues(): exceeded the size limit");
                        status = ValidValues.SIZE_LIMIT_EXCEEDED;
                    } else if (resultCode.equals(ResultCode.TIME_LIMIT_EXCEEDED)) {
                        debug.warning("LDAPUsers.getValidValues(): exceeded the time limit");
                        status = ValidValues.TIME_LIMIT_EXCEEDED;
                    } else {
                        throw new PolicyException(e);
                    }
                } catch (SearchResultReferenceIOException e) {
                    // ignore referrals
                }
            }
        } catch (LdapException e) {
            throw handleResultException(e);
        }
        return new ValidValues(status, validUserDNs);
    }

    /**
     * Returns a set of possible values that satisfy the <code>pattern</code>.
     * The returned <code>ValidValues</code> object contains a set of
     * map of user DN to a map of user's attribute name to a string array of
     * attribute values.
     *
     * @param token Single Sign On token for fetching the possible values.
     * @param pattern Search pattern of which possible values are matched to.
     * @param attributeNames Array of attribute names to be to returned.
     * @return a set of possible values that satify the <code>pattern</code>.
     * @throws SSOException if <code>SSOToken</code> is invalid.
     * @throws PolicyException if there are problems getting these values.
     */
    public ValidValues getValidEntries(
        SSOToken token,
        String pattern,
        String[] attributeNames
    ) throws SSOException, PolicyException
    {
        if (!initialized) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "ldapusers_subject_not_yet_initialized", null, null));
        }

        Set<Map<String, Map<String, String[]>>> results = new HashSet<>();
        String searchFilter = getSearchFilter(pattern);
        int status = ValidValues.SUCCESS;

        try (Connection ld = connPool.getConnection()) {
            ConnectionEntryReader res = search(searchFilter, ld, attributeNames);
            Map<String, Map<String, String[]>> map = new HashMap<>();
            results.add(map);

            while (res.hasNext()) {
                try {
                    SearchResultEntry entry = res.readEntry();
        
                    if (entry != null) {
                        String userDN = entry.getName().toString();
                        map.put(userDN, getUserAttributeValues(entry, attributeNames));
                    }
                } catch (SearchResultReferenceIOException lre) {
                    // ignore referrals
                    continue;
                } catch (LdapException e) {
                    ResultCode resultCode = e.getResult().getResultCode();
                    if (resultCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                        debug.warning("LDAPUsers.getValidEntries(): exceeded the size limit");
                        status = ValidValues.SIZE_LIMIT_EXCEEDED;
                    } else if (resultCode.equals(ResultCode.TIME_LIMIT_EXCEEDED)) {
                        debug.warning(
                        "LDAPUsers.getValidEntries(): exceeded the time limit");
                        status = ValidValues.TIME_LIMIT_EXCEEDED;
                    } else {
                        throw new PolicyException(e);
                    }
                }
            }
        } catch (LdapException e) {
            throw handleResultException(e);
        } catch (Exception e) {
            throw new PolicyException(e);
        }
        return new ValidValues(status, results);
    }

    protected ConnectionEntryReader search(String searchFilter, Connection ld, String... attributeNames) {
        SearchRequest request = LDAPRequests.newSearchRequest(baseDN, userSearchScope, searchFilter, attributeNames)
                .setDereferenceAliasesPolicy(DereferenceAliasesPolicy.NEVER)
                .setTimeLimit(timeLimit);
        if (maxResults > 0) {
            request.setSizeLimit(maxResults);
        }
        return ld.search(request);
    }

    /**
     * Given an <code>LDAPEntry</code> object reads it and
     * returns a <code>Map</code> of attribute name ( String)
     * to an Array of <code>String</code> objects representing
     * values for the attribute name.
     */

    private Map<String, String[]> getUserAttributeValues(SearchResultEntry entry, String[] attributeNames) {
        Map<String, String[]> map = new HashMap<>();

        if ((attributeNames != null) && (attributeNames.length > 0)) {
            for (String attrName : attributeNames) {

                Attribute lAttr = entry.getAttribute(attrName);

                if (lAttr != null) {
                    map.put(attrName, toStringArray(lAttr));
                }
            }
        }

        return map;
    }

    private String[] toStringArray(Attribute lAttr) {
        String[] values = new String[lAttr.size()];
        int j = 0;
        for (ByteString value : lAttr) {
            values[j++] = value.toString();
        }
        return values;
    }

    /**
     * Given the search pattern ( String), creates an LDAP
     * search filter in the expected LDAP filter format.
     */

    private String getSearchFilter(String pattern) {
        String searchFilter = null;

        if ((pattern != null) && (pattern.trim().length() > 0)) {
            searchFilter = "(&" + userSearchFilter + "(" + userRDNAttrName +
                "=" + pattern + "))";
        } else {
           searchFilter = userSearchFilter;
        }

        if (debug.messageEnabled()) {
            debug.message(
                "LDAPUsers.getSearchFilter(): user search filter is: " +
                    searchFilter);
        }

        return searchFilter;
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
     * @return values that have been set for the user collection
     */

    public Set getValues() {
        if (debug.messageEnabled()) {
            debug.message("LDAPUsers.getValues() gets called");
        }
        return selectedUserDNs;
    }


    /**
     * Sets the names for the instance of the <code>LDAPUsers</code>
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
            debug.error("LDAPUsers.setValues(): Invalid names");
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "ldapusers_subject_invalid_user_names", null, "null", 
                PolicyException.USER_COLLECTION));
        }
        selectedUserDNs = new HashSet<>();
        selectedUserDNs.addAll(names);
        if (debug.messageEnabled()) {
            debug.message("LDAPUsers.setValues(): selected user names=" + 
                selectedUserDNs);
        }
        selectedRFCUserDNs = new HashSet<>();
        // add to the RFC Set now
        for (Object name : names) {
            selectedRFCUserDNs.add(DN.valueOf((String) name).toString().toLowerCase());
        }
    }


    /**
     * Determines if the user belongs to this instance
     * of the <code>LDAPUsers</code> object.
     *
     * @param token single-sign-on token of the user
     *
     * @return <code>true</code> if the user is memeber of the
     * given subject; <code>false</code> otherwise.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if an error occured while
     * checking if the user is a member of this subject
     */

    public boolean isMember(SSOToken token)
        throws SSOException, PolicyException {

        boolean userMatch = false;
        boolean listenerAdded = false;
        DN userDN = null;
        String userLocalDN = token.getPrincipal().getName();

        String tokenID = token.getTokenID().toString();
        if (debug.messageEnabled()) {
            debug.message(
             "LDAPUsers.isMember(): user local DN is " + userLocalDN);
        }

        for (String valueDN : selectedRFCUserDNs) {
            Boolean matchFound = SubjectEvaluationCache.isMember(tokenID, ldapServer, valueDN);
            if (matchFound != null) {
                debug.message("LDAPUsers.isMember():Got membership from cache of {} in subject user {} : {}",
                        userLocalDN, valueDN, matchFound);
                if (matchFound) {
                    return true;
                } else {
                    continue;
                }
            }
            // got here so entry not in subject evalauation cache
            if (debug.messageEnabled()) {
                debug.message("LDAPUsers:isMember():entry for " + valueDN
                        + " not in subject evaluation cache, fetching from "
                        + "directory server.");
            }
            if (userDN == null) {
                userDN = getUserDN(token);
                if (userDN == null) {
                    if (debug.messageEnabled()) {
                        debug.message("LDAPUsers.isMember(): User {} is not found in the directory",
                                token.getPrincipal().getName());
                    }
                    return false;
                }
            }
            if (userDN.equals(DN.valueOf(valueDN))) {
                userMatch = true;
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPUsers.isMember:adding entry "
                        + tokenID + " " + ldapServer + " " + valueDN + " " + userMatch
                        + " in subject evaluation cache.");
            }
            SubjectEvaluationCache.addEntry(tokenID, ldapServer, valueDN, userMatch);
            if (!listenerAdded && !PolicyEvaluator.ssoListenerRegistry.containsKey(tokenID)) {
                token.addSSOTokenListener(PolicyEvaluator.ssoListener);
                PolicyEvaluator.ssoListenerRegistry.put(tokenID, PolicyEvaluator.ssoListener);
                debug.message("LDAPUsers.isMember(): sso listener added");
                listenerAdded = true;
            }
            if (userMatch) {
                break;
            }
        }
        if (!userMatch) {
            debug.message("LDAPUsers.isMember(): User {} is not a member of this LDAPUsers object", userLocalDN);
        } else {
            debug.message("LDAPUsers.isMember(): User {} is a member of this LDAPUsers object", userLocalDN);
        }
        return userMatch;
    }


   /** 
    * Return a hash code for this <code>LDAPUsers</code>.
    *
    * @return a hash code for this <code>LDAPUsers</code> object.
    */

    public int hashCode() {
        return selectedUserDNs.hashCode();
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
        if (o instanceof LDAPUsers) {
            LDAPUsers g = (LDAPUsers) o;
            if ((selectedUserDNs != null) 
                 && (g.selectedUserDNs != null) 
                 && (selectedUserDNs.equals(g.selectedUserDNs))) {
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
        LDAPUsers theClone = null;
        try {
            theClone = (LDAPUsers) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (selectedRFCUserDNs != null) {
            theClone.selectedRFCUserDNs = new HashSet();
            theClone.selectedRFCUserDNs.addAll(selectedRFCUserDNs);
        }
        return theClone;
    }
    /**
     * Gets the DN for a user identified 
     * by the token. If the Directory server is locally installed to speed
     * up the search, no directoty search is performed and the DN obtained
     * from the token is returned. If the directory is remote
     * a LDAP search is performed to get the user DN.
     */

    private DN getUserDN(SSOToken token) throws SSOException, PolicyException {
        Set<String> qualifiedUserDNs = new HashSet<>();
        String userLocalDN = token.getPrincipal().getName();
        DN userDN = null;
        if (localDS && !PolicyUtils.principalNameEqualsUuid( token)) {
            userDN = DN.valueOf(userLocalDN);
        } else {
            // try to figure out the user name from the local user DN
            int beginIndex = userLocalDN.indexOf("=");
            int endIndex = userLocalDN.indexOf(",");
            if ((beginIndex <= 0) || (endIndex <= 0) ||
                (beginIndex >= endIndex)) {
                throw (new PolicyException(ResBundleUtils.rbName,
                    "ldapusers_subject_invalid_local_user_dn", null, 
                        null));
            }
            String userName = userLocalDN.substring(beginIndex+1, 
                endIndex);
            String searchFilter = null;
            if ((userSearchFilter != null) && !(userSearchFilter.length() == 0))
            {
                searchFilter = "(&" + userSearchFilter +
                    PolicyUtils.constructUserFilter(
                    token, userRDNAttrName, userName, 
                    aliasEnabled) + ")";
            } else {
                searchFilter = PolicyUtils.constructUserFilter(
                    token, userRDNAttrName, userName, aliasEnabled);
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPUsers.getUserDN(): search filter is: "
                + searchFilter);
            }
               
            String[] attrs = { userRDNAttrName };     
            // search the remote ldap and find out the user DN
            try (Connection ld = connPool.getConnection()){
                ConnectionEntryReader res = search(searchFilter, ld, attrs);
                while (res.hasNext()) {
                    try {
                        SearchResultEntry entry = res.readEntry();
                        qualifiedUserDNs.add(entry.getName().toString());
                    } catch (SearchResultReferenceIOException e) {
                        // ignore referrals
                        continue;
                    } catch (LdapException e) {
                        String objs[] = { orgName };
                        ResultCode resultCode = e.getResult().getResultCode();
                        if (resultCode.equals(ResultCode.SIZE_LIMIT_EXCEEDED)) {
                            debug.warning("LDAPUsers.getUserDN(): exceeded the size limit");
                            throw new PolicyException(ResBundleUtils.rbName, "ldap_search_exceed_size_limit", objs,
                                null);
                        } else if (resultCode.equals(ResultCode.TIME_LIMIT_EXCEEDED)) {
                            debug.warning("LDAPUsers.getUserDN(): exceeded the time limit");
                            throw new PolicyException(ResBundleUtils.rbName, "ldap_search_exceed_time_limit", objs,
                                null);
                        } else {
                            throw new PolicyException(e);
                        }
                    }
                }
            } catch (LdapException e) {
                throw handleResultException(e);
            } catch (Exception e) {
                throw new PolicyException(e);
            }

            // check if the user belongs to any of the selected users
            if (qualifiedUserDNs.size() > 0) {
                debug.message("LDAPUsers.getUserDN(): qualified users={}", qualifiedUserDNs);
                Iterator<String> iter = qualifiedUserDNs.iterator();
                // we only take the first qualified DN
                userDN =  DN.valueOf(iter.next());
            }
        }
        return userDN;
    }

    private PolicyException handleResultException(LdapException e) {
        ResultCode ldapErrorCode = e.getResult().getResultCode();
        if (ldapErrorCode.equals(ResultCode.INVALID_CREDENTIALS)) {
            return new PolicyException(ResBundleUtils.rbName, "ldap_invalid_password", null, null);
        } else if (ldapErrorCode.equals(ResultCode.NO_SUCH_OBJECT)) {
            String[] objs = { baseDN };
            return new PolicyException(ResBundleUtils.rbName, "no_such_ldap_users_base_dn", objs, null);
        }
        String errorMsg = e.getResult().getDiagnosticMessage();
        String additionalMsg = e.getMessage();
        if (additionalMsg != null) {
            return new PolicyException(errorMsg + ": " + additionalMsg);
        } else {
            return new PolicyException(errorMsg);
        }
    }
}
