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
 * $Id: LDAPGroups.java,v 1.8 2009/11/20 23:52:55 ww203982 Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation 
 */

package com.sun.identity.policy.plugins;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.REQUEST_TIMEOUT;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LDAPUrl;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.SearchRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;
import org.forgerock.util.time.Duration;

/**
 * This class respresents a group of LDAP groups
 */
public class LDAPGroups implements Subject {

    static final String STATIC_GROUP_MEMBER_ATTR = "uniqueMember";
    static final String STATIC_GROUP_MEMBER_ALT_ATTR = "member";
    static final String DYNAMIC_GROUP_MEMBER_URL = "memberUrl";
    static final String LDAP_SCOPE_BASE = "SCOPE_BASE";
    static final String LDAP_SCOPE_ONE = "SCOPE_ONE";
    static final String LDAP_SCOPE_SUB = "SCOPE_SUB";

    // Variables
    private boolean initialized = false;
    private Set<String> selectedGroupDNs = Collections.emptySet();
    private Set<String> selectedRFCGroupDNs = Collections.emptySet();
    private String authid;
    private String authpw;
    private String baseDN;
    private String groupSearchFilter;
    private SearchScope groupSearchScope = SearchScope.WHOLE_SUBTREE;
    private String userSearchFilter;
    private SearchScope userSearchScope = SearchScope.WHOLE_SUBTREE;
    private String groupRDNAttrName;
    private String userRDNAttrName;
    private int timeLimit;
    private int maxResults;
    private boolean sslEnabled = false;
    private int minPoolSize;
    private int maxPoolSize;
    private String orgName;
    private ConnectionFactory connPool;
    private boolean localDS;
    private boolean aliasEnabled;
    private String ldapServer;

    // Debug 
    static Debug debug = Debug.getInstance(
                              PolicyManager.POLICY_DEBUG_NAME);

    /**
     * no argument constructor.
     */
    public LDAPGroups() {
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
                "ldapgroups_initialization_failed", null, null));
        }

        String configuredLdapServer = 
            (String)configParams.get(PolicyConfig.LDAP_SERVER);
        if (configuredLdapServer == null) {
            debug.error("LDAPGroups.initialize(): failed to get LDAP "
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
        groupSearchFilter = (String) configParams.get(
                               PolicyConfig.LDAP_GROUP_SEARCH_FILTER);
        String scope = (String) configParams.get(
                                PolicyConfig.LDAP_GROUP_SEARCH_SCOPE);
        if (scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            groupSearchScope = SearchScope.BASE_OBJECT;
        } else if (scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            groupSearchScope = SearchScope.SINGLE_LEVEL;
        } else {
            groupSearchScope = SearchScope.WHOLE_SUBTREE;
        }

        groupRDNAttrName = (String) configParams.get(
                           PolicyConfig.LDAP_GROUP_SEARCH_ATTRIBUTE);
        userSearchFilter = (String) configParams.get(
                           PolicyConfig.LDAP_USERS_SEARCH_FILTER);
        scope = (String) configParams.get(
                           PolicyConfig.LDAP_USERS_SEARCH_SCOPE);
        userSearchScope = LDAPUtils.getSearchScope(scope, SearchScope.WHOLE_SUBTREE);

        userRDNAttrName = (String) configParams.get(
                           PolicyConfig.LDAP_USER_SEARCH_ATTRIBUTE);
        try {
            timeLimit = Integer.parseInt((String) configParams.get(
                           PolicyConfig.LDAP_SEARCH_TIME_OUT));
            maxResults = Integer.parseInt((String) configParams.get(
                           PolicyConfig.LDAP_SEARCH_LIMIT));
            minPoolSize = Integer.parseInt((String) configParams.get(
                          PolicyConfig.LDAP_CONNECTION_POOL_MIN_SIZE));
            maxPoolSize = Integer.parseInt((String) configParams.get(
                          PolicyConfig.LDAP_CONNECTION_POOL_MAX_SIZE));
        } catch (NumberFormatException nfe) {
            throw (new PolicyException(nfe));
        }

        String ssl = (String) configParams.get(
                                       PolicyConfig.LDAP_SSL_ENABLED);
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
            debug.message("LDAPGroups.initialize(): getting params" 
                           + "\nldapServer: " + ldapServer 
                           + "\nauthid: " + authid
                           + "\nbaseDN: " + baseDN
                           + "\ngroupSearchFilter: " + groupSearchFilter
                           + "\ngroupRDNAttrName: " + groupRDNAttrName
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
        Options options = Options.defaultOptions()
                .set(REQUEST_TIMEOUT, new Duration((long) timeLimit, TimeUnit.SECONDS));

        LDAPConnectionPools.initConnectionPool(ldapServer, authid, authpw, sslEnabled, minPoolSize, maxPoolSize, options);
        connPool = LDAPConnectionPools.getConnectionPool(ldapServer);
        initialized = true;
    }


    /**
     * Returns the syntax of the values the <code>LDAPGroups</code> 
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
     * Returns a list of possible values for the <code>LDAPGroups
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
     * Returns a list of possible values for the <code>LDAPGroups
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
        throws SSOException, PolicyException {
        if (!initialized) {
            throw new PolicyException(ResBundleUtils.rbName, "ldapgroups_subject_not_yet_initialized", null, null);
        }

        Set<String> validGroupDNs = new HashSet<>();
        String searchFilter;
        if (pattern != null && !pattern.trim().isEmpty()) {
            searchFilter = "(&" + groupSearchFilter + "(" + groupRDNAttrName + "=" + pattern + "))";
        } else {
           searchFilter = groupSearchFilter;
        }
        debug.message("LDAPGroups.getValidValues(): group search filter is: {}", searchFilter);

        String[] attrs = { groupRDNAttrName };
        Connection ld = null;
        int status = ValidValues.SUCCESS;
        try (Connection conn = connPool.getConnection()) {

            SearchRequest searchRequest = LDAPRequests.newSearchRequest(baseDN, groupSearchScope, searchFilter, attrs);
            ConnectionEntryReader reader = conn.search(searchRequest);

            while (reader.hasNext()) {
                if (reader.isReference()) {
                    //Ignore
                    reader.readReference();
                } else {
                    SearchResultEntry entry = reader.readEntry();
                    if (entry != null) {
                        validGroupDNs.add(entry.getName().toString());
                        debug.message("LDAPGroups.getValidValues(): found group name={}", entry.getName().toString());
                    }
                }
            }
        } catch (LdapException lde) {
            ResultCode resultCode = lde.getResult().getResultCode();
            if (ResultCode.SIZE_LIMIT_EXCEEDED.equals(resultCode)) {
                debug.warning("LDAPGroups.getValidValues(): exceeded the size limit");
                return new ValidValues(ValidValues.SIZE_LIMIT_EXCEEDED, validGroupDNs);
            } else if (ResultCode.TIME_LIMIT_EXCEEDED.equals(resultCode)) {
                debug.warning("LDAPGroups.getValidValues(): exceeded the time limit");
                return new ValidValues(ValidValues.TIME_LIMIT_EXCEEDED, validGroupDNs);
            } else if (ResultCode.INVALID_CREDENTIALS.equals(resultCode)) {
                throw new PolicyException(ResBundleUtils.rbName, "ldap_invalid_password", null, null);
            } else if (ResultCode.NO_SUCH_OBJECT.equals(resultCode)) {
                String[] objs = { baseDN };
                throw new PolicyException(ResBundleUtils.rbName, "no_such_ldap_base_dn", objs, null);
            } 
            String errorMsg = lde.getMessage();
            String additionalMsg = lde.getResult().getDiagnosticMessage();
            if (additionalMsg != null) {
                throw new PolicyException(errorMsg + ": " + additionalMsg);
            } else { 
                throw new PolicyException(errorMsg);
            }
        } catch (Exception e) {
            throw new PolicyException(e);
        }
        return new ValidValues(status, validGroupDNs);
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
            debug.message("LDAPGroups.getValues() gets called");
        }
        return selectedGroupDNs;
    }


    /**
     * Sets the names for the instance of the <code>LDAPGroups</code>
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
            debug.error("LDAPGroups.setValues(): Invalid names");
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "ldapgroups_subject_invalid_group_names", null, 
                null, PolicyException.USER_COLLECTION));
        }
        selectedGroupDNs = new HashSet();
        selectedGroupDNs.addAll(names);
        if (debug.messageEnabled()) {
            debug.message(
                   "LDAPGroups.setValues(): selected group names=" 
                   + selectedGroupDNs);
        }
        // add to the RFC Set now
        selectedRFCGroupDNs = new HashSet();
        Iterator it = names.iterator();
        while (it.hasNext()) {
            selectedRFCGroupDNs.add(DN.valueOf((String) it.next()).toString().toLowerCase());
        }
    }


    /**
     * Determines if the user belongs to this instance
     * of the <code>LDAPGroups</code> object.
     *
     * @param token single-sign-on token of the user
     *
     * @return <code>true</code> if the user is memeber of the
     * given subject; <code>false</code> otherwise.
     *
     * @exception SSOException if <code>SSOToken>/code> is not valid
     * @exception PolicyException if an error occured while
     * checking if the user is a member of this subject
     */

    public boolean isMember(SSOToken token)
        throws SSOException, PolicyException {

        if (token == null) {
            return false;
        }
        boolean listenerAdded = false;
        String tokenID = token.getTokenID().toString();
        String userLocalDN = token.getPrincipal().getName();
        DN userDN = null;
        if (debug.messageEnabled()) {
            debug.message("LDAPGroups.isMember(): user local DN is "
                          + userLocalDN);
        }
        if (selectedRFCGroupDNs.size() > 0) {
            Iterator<String> groupsIter = selectedRFCGroupDNs.iterator();
            String userRDN = null;
            while (groupsIter.hasNext()) {
                Boolean matchFound;
                String groupDN = groupsIter.next();
                if ((matchFound = SubjectEvaluationCache.isMember(tokenID, ldapServer,groupDN)) != null) {
                    debug.message("LDAPGroups.isMember():Got membership from cache of {} in group {} :{}", userLocalDN,
                            groupDN, matchFound);
                    boolean result = matchFound;
                    if (result) {
                        return true;
                    } else {
                        continue;
                    }
                }
                // got here so entry not in subject evalauation cache
                if (debug.messageEnabled()) {
                    debug.message("LDAPGroups:isMember():entry for {} not in subject evaluation cache,fetching from "
                            + "directory server.", groupDN);
                }
                if (userDN == null) {
                    int beginIndex = userLocalDN.indexOf("=");
                    int endIndex = userLocalDN.indexOf(",");
                    if (beginIndex <= 0 || endIndex <= 0 || beginIndex >= endIndex) {
                        throw new PolicyException(ResBundleUtils.rbName, "ldapgroups_subject_invalid_local_user_dn",
                                null, null);
                    }
                    String userName = userLocalDN.substring(beginIndex+1, endIndex);
                    userRDN = PolicyUtils.constructUserFilter(token, userRDNAttrName, userName, aliasEnabled);
        
                    if (localDS && !PolicyUtils.principalNameEqualsUuid(token)) {
                        userDN = DN.valueOf(userLocalDN);
                     } else {
                        // try to figure out the user name from the local 
                        // user DN
                        userDN = getUserDN(userRDN);
                    }
                    if (userDN == null) {
                        debug.message("LDAPGroups.isMember(): User {} is not found in the directory", userLocalDN);
                        return false;
                    }
                }
                if (!listenerAdded) {
                    if (!PolicyEvaluator.ssoListenerRegistry.containsKey(tokenID)) {
                        token.addSSOTokenListener(PolicyEvaluator.ssoListener);
                        PolicyEvaluator.ssoListenerRegistry.put(tokenID, PolicyEvaluator.ssoListener);
                        debug.message("LDAPGroups.isMember(): sso listener added .\n");
                        listenerAdded = true;
                    }
                }
                if (isMemberOfGroup(groupDN, userDN, userRDN, token)) {
                    debug.message("LDAPGroups.isMember(): User {} is a member of this LDAPGroups.", userDN.toString());
                    return  true;
                }
            }
        }
        debug.message("LDAPGroups.isMember(): User {} is not a member of this LDAPGroups.", userLocalDN);
        return false;
    }


    /**
     * Find out if a user belongs to a particular group
     * @param groupName the ldap DN of the group
     * @param userDN the ldap DN of the user
     * @return <code>true</code> if the user is member of the group;
     * <code>false</code> otherwise.
     */

    private boolean isMemberOfGroup(String groupName, DN userDN,
                                        String userRDN, SSOToken token)
        throws SSOException, PolicyException {

        if (debug.messageEnabled()) {
            debug.message("LDAPGroups.isMemberOfGroup():"
                    + " entering with groupName = " + groupName
                    + ",userDN = " + userDN);
        }

        if ((groupName == null) || (groupName.length() == 0) || 
             (userDN == null)) {
            return false;
        }
        String tokenID = token.getTokenID().toString();
        boolean groupMatch = false;

        SearchResultEntry entry;
        try (Connection conn = connPool.getConnection()) {
            entry = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(groupName));
        } catch (Exception e) {
            debug.warning("LDAPGroups: invalid group name {} specified in the policy definition.", groupName);
            return false;
        }

        debug.message("LDAPGroups.isMemberOfGroup(): get {} group attribute", STATIC_GROUP_MEMBER_ATTR);
        Attribute attribute = entry.getAttribute(STATIC_GROUP_MEMBER_ATTR);
        if (attribute != null) {
            for (ByteString memberDNStr : attribute) {
                debug.message("LDAPGroups.isMemberOfGroup(): memberDNStr = ", memberDNStr);
                DN memberDN = DN.valueOf(memberDNStr.toString());
                if (userDN.equals(memberDN)) {
                    groupMatch = true;
                    break;
                }
            }
        }

        if (!groupMatch) {
            debug.message("LDAPGroups.isMemberOfGroup(): get {} group attribute", STATIC_GROUP_MEMBER_ALT_ATTR);
            attribute = entry.getAttribute(STATIC_GROUP_MEMBER_ALT_ATTR);
            if (attribute != null) {
                for (ByteString memberDNStr : attribute) {
                    debug.message("LDAPGroups.isMemberOfGroup(): memberDNStr = ", memberDNStr);
                    DN memberDN = DN.valueOf(memberDNStr.toString());
                    if (userDN.equals(memberDN)) {
                        groupMatch = true;
                        break;
                    }
                }
            }
        }

        if (!groupMatch) {
            attribute = entry.getAttribute(DYNAMIC_GROUP_MEMBER_URL);
            if (attribute != null) {
                for (ByteString memberUrl : attribute) {
                    try {
                        LDAPUrl ldapUrl = LDAPUrl.valueOf(memberUrl.toString());
                        Set members = findDynamicGroupMembersByUrl(ldapUrl, userRDN);
                        Iterator iter = members.iterator();
                        while (iter.hasNext()) {
                            String memberDNStr = (String)iter.next();
                            DN memberDN = DN.valueOf(memberDNStr);
                            if (userDN.equals(memberDN)) {
                                groupMatch = true;
                                break;
                            }
                        }
                    } catch (LocalizedIllegalArgumentException e) {
                        throw new PolicyException(e);
                    }
                }
            }
        }
        debug.message("LDAPGroups.isMemberOfGroup():adding entry {} {} {} {} in subject evaluation cache.", tokenID,
                ldapServer, groupName, groupMatch);
        SubjectEvaluationCache.addEntry(tokenID, ldapServer, groupName, groupMatch);
        return groupMatch;
    }

    /**
     * Finds the dynamic group member DNs 
     * @param url the url to be used for the group member search
     * @return the set of group member DNs satisfied the search url
     */         
    private Set findDynamicGroupMembersByUrl(LDAPUrl url, String userRDN)
        throws PolicyException {

        Connection ld = null;
        Set<String> groupMemberDNs = new HashSet<>();
        try (Connection conn = connPool.getConnection()) {
            // Need to pass the user dn in the filter
            StringBuilder filter = new StringBuilder(25);
            filter.append("(&").append(userRDN);
            String groupFilter = url.getFilter().toString();
            int index = groupFilter.indexOf("(");
            if (index != 0) {
                filter.append("(").append(groupFilter).append("))"); 
            } else {
                filter.append(groupFilter).append(")");
            }
            debug.message("search filter in LDAPGroups : {}", filter);
            String[] attrs = { userRDNAttrName };

            SearchRequest searchRequest = LDAPRequests.newSearchRequest(url.getName(), url.getScope(),
                    Filter.valueOf(filter.toString()), attrs);
            ConnectionEntryReader reader = conn.search(searchRequest);

            while (reader.hasNext()) {
                if (reader.isReference()) {
                    //Ignore
                    reader.readReference();
                } else {
                    SearchResultEntry entry = reader.readEntry();
                    if (entry != null) {
                        groupMemberDNs.add(entry.getName().toString());
                    }
                }
            }
        } catch (LdapException le) {
            String objs[] = { orgName };
            ResultCode resultCode = le.getResult().getResultCode();
            if (ResultCode.SIZE_LIMIT_EXCEEDED.equals(resultCode)) {
                debug.warning("LDAPGroups.findDynamicGroupMembersByUrl(): exceeded the size limit");
                throw new PolicyException(ResBundleUtils.rbName, "ldap_search_exceed_size_limit", objs, null);
            } else if (ResultCode.TIME_LIMIT_EXCEEDED.equals(resultCode)) {
                debug.warning("LDAPGroups.findDynamicGroupMembersByUrl(): exceeded the time limit");
                throw new PolicyException(ResBundleUtils.rbName, "ldap_search_exceed_time_limit", objs, null);
            } else {
                throw new PolicyException(le);
            }
        } catch (Exception e) {
            throw new PolicyException(e);
        }
        return groupMemberDNs;
    }

   /** 
    * Return a hash code for this <code>LDAPGroups</code>.
    *
    * @return a hash code for this <code>LDAPGroups</code> object.
    */
    public int hashCode() {
        return selectedGroupDNs.hashCode();
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
        if (o instanceof LDAPGroups) {
            LDAPGroups g = (LDAPGroups) o;
            if ((selectedGroupDNs != null) 
                 && (g.selectedGroupDNs != null) 
                 && (selectedGroupDNs.equals(g.selectedGroupDNs))) {
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
        LDAPGroups theClone = null;
        try {
            theClone = (LDAPGroups) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (selectedGroupDNs != null) {
            theClone.selectedGroupDNs = new HashSet();
            theClone.selectedGroupDNs.addAll(selectedGroupDNs);
        }
        if (selectedRFCGroupDNs != null) {
            theClone.selectedRFCGroupDNs = new HashSet();
            theClone.selectedRFCGroupDNs.addAll(selectedRFCGroupDNs);
        }
        return theClone;
    }

    /**
     * Get the full DN for the user using the RDN against the
     * LDAP server configured in the policy config service.
     */
    private DN getUserDN(String userRDN ) throws SSOException, PolicyException {
        DN userDN = null;
        if (userRDN != null) {
            Set<String> qualifiedUserDNs = new HashSet<>();
            String searchFilter = null;
            if ((userSearchFilter != null) 
                && !(userSearchFilter.length() == 0)) {
                searchFilter = "(&" + userSearchFilter + userRDN + ")";
            } else {
                searchFilter = userRDN;
            }
            if (debug.messageEnabled()) {
                debug.message(
                    "LDAPGroups.getUserDN(): search filter is: " 
                    + searchFilter);
            }
            
            String[] attrs = { userRDNAttrName }; 
            try (Connection conn = connPool.getConnection()) {
                SearchRequest searchRequest = LDAPRequests.newSearchRequest(baseDN, userSearchScope, searchFilter,
                        attrs);
                ConnectionEntryReader reader = conn.search(searchRequest);

                while (reader.hasNext()) {
                    if (reader.isReference()) {
                        //Ignore
                        reader.readReference();
                    } else {
                        SearchResultEntry entry = reader.readEntry();
                        if (entry != null) {
                            qualifiedUserDNs.add(entry.getName().toString());
                        }
                    }
                }
            } catch (LdapException le) {
                ResultCode resultCode = le.getResult().getResultCode();
                if (ResultCode.SIZE_LIMIT_EXCEEDED.equals(resultCode)) {
                    String objs[] = { orgName };
                    debug.warning("LDAPGroups.isMember(): exceeded the size limit");
                    throw new PolicyException(ResBundleUtils.rbName, "ldap_search_exceed_size_limit", objs, null);
                } else if (ResultCode.TIME_LIMIT_EXCEEDED.equals(resultCode)) {
                    String objs[] = { orgName };
                    debug.warning("LDAPGroups.isMember(): exceeded the time limit");
                    throw new PolicyException(ResBundleUtils.rbName, "ldap_search_exceed_time_limit", objs, null);
                } else if (ResultCode.INVALID_CREDENTIALS.equals(resultCode)) {
                    throw new PolicyException(ResBundleUtils.rbName, "ldap_invalid_password", null, null);
                } else if (ResultCode.NO_SUCH_OBJECT.equals(resultCode)) {
                    String[] objs = { baseDN };
                    throw new PolicyException(ResBundleUtils.rbName, "no_such_ldap_base_dn", objs, null);
                } 
                String errorMsg = le.getMessage();
                String additionalMsg = le.getResult().getDiagnosticMessage();
                if (additionalMsg != null) {
                    throw new PolicyException(errorMsg + ": " + additionalMsg);
                } else {
                    throw new PolicyException(errorMsg);
                }
            } catch (Exception e) {
                throw new PolicyException(e);
            }
            // check if the user belongs to any of the selected groups
            if (qualifiedUserDNs.size() > 0) {
                debug.message("LDAPGroups.getUserDN(): qualified users={}", qualifiedUserDNs);
                Iterator<String> iter = qualifiedUserDNs.iterator();
                // we only take the first qualified DN if the DN
                userDN = DN.valueOf(iter.next());
            }
        }
        return userDN;
    }
}
