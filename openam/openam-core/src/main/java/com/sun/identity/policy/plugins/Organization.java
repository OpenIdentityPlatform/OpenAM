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
 * $Id: Organization.java,v 1.9 2009/11/20 23:52:55 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock Inc 
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation 
 */
package com.sun.identity.policy.plugins;

import java.util.*;
import com.sun.identity.shared.ldap.*;
import com.sun.identity.shared.ldap.util.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.ValidValues;
import com.sun.identity.policy.Syntax;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.SubjectEvaluationCache;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.shared.ldap.LDAPBindRequest;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPSearchRequest;

/**
 * This class respresents a group of OpenSSO organizations.
 */
public class Organization implements Subject {

    static final String LDAP_SCOPE_BASE = "SCOPE_BASE";
    static final String LDAP_SCOPE_ONE = "SCOPE_ONE";
    static final String LDAP_SCOPE_SUB = "SCOPE_SUB";

    // Variables
    private boolean initialized = false;
    private Set selectedOrgDNs = Collections.EMPTY_SET;
    private Set selectedRFCOrgDNs = Collections.EMPTY_SET;
    private String authid;
    private String authpw;
    private String baseDN;
    private String userSearchFilter;
    private String orgSearchFilter;
    private int userSearchScope = LDAPv2.SCOPE_SUB;
    private int orgSearchScope = LDAPv2.SCOPE_SUB;
    private String userRDNAttrName;
    private String orgRDNAttrName;
    private int timeLimit;
    private int maxResults;
    private boolean sslEnabled = false;
    private int minPoolSize;
    private int maxPoolSize;
    private LDAPConnectionPool connPool;
    private String orgName;
    private boolean localDS;
    private boolean aliasEnabled;
    private String ldapServer;

    // Debug
    static Debug debug = Debug.getInstance(
                          PolicyManager.POLICY_DEBUG_NAME);

    /**
     * Constructor with no parameter
     */
    public Organization() {
        // do nothing
    }


    /** 
     * Initialize the <code>Organization</code> object by using the 
     * configuration information passed by the Policy Framework.
     * @param configParams the configuration information
     * @exception PolicyException if an error occured during 
     * initialization of the instance
     */
    public void initialize(Map configParams) throws PolicyException {
 
        if (configParams == null) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "org_initialization_failed", null, null));
        }

        String configuredLdapServer = 
            (String)configParams.get(PolicyConfig.LDAP_SERVER);
        if (configuredLdapServer == null) {
            debug.error("Organization.initialize(): failed to get LDAP "
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

        userSearchFilter = (String) configParams.get(
                   PolicyConfig.LDAP_USERS_SEARCH_FILTER);
        String scope = (String) configParams.get(
                   PolicyConfig.LDAP_USERS_SEARCH_SCOPE);
        if (scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            userSearchScope = LDAPv2.SCOPE_BASE;
        } else if (scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            userSearchScope = LDAPv2.SCOPE_ONE;
        } else {
            userSearchScope = LDAPv2.SCOPE_SUB;
        }

        userRDNAttrName = (String) configParams.get(
                  PolicyConfig.LDAP_USER_SEARCH_ATTRIBUTE);
        orgSearchFilter = (String) configParams.get(
                  PolicyConfig.LDAP_ORG_SEARCH_FILTER);
        scope = (String) configParams.get(
                  PolicyConfig.LDAP_ORG_SEARCH_SCOPE);
        if (scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            orgSearchScope = LDAPv2.SCOPE_BASE;
        } else if (scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            orgSearchScope = LDAPv2.SCOPE_ONE;
        } else {
            orgSearchScope = LDAPv2.SCOPE_SUB;
        }

        orgRDNAttrName = (String) configParams.get(
                          PolicyConfig.LDAP_ORG_SEARCH_ATTRIBUTE);
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
            debug.message("Organization.initialize(): getting params" 
                           + "\nldapServer: " + ldapServer 
                           + "\nauthid: " + authid
                           + "\nbaseDN: " + baseDN
                           + "\nuserSearchFilter: " + userSearchFilter
                           + "\nuserRDNAttrName: " + userRDNAttrName
                           + "\norgSearchFilter: " + orgSearchFilter
                           + "\norgRDNAttrName: " + orgRDNAttrName
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
     * Returns the syntax of the values the <code>Organization</code> 
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
     * Returns a list of possible values for the <code>Organization
     * </code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code>
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
     * Returns a list of possible values for the <code>Organization
     * </code> that satisfy the given <code>pattern</code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     * @param pattern search pattern that will be used to narrow
     * the list of valid names.
     *
     * @return <code>ValidValues</code>
     *
     * @exception SSOException if <code>SSOToken</code> is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */

    public ValidValues getValidValues(SSOToken token, String pattern)
        throws SSOException, PolicyException {
        if (!initialized) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "org_subject_not_yet_initialized", null, null));
        }

        String searchFilter = null;
        if ((pattern != null) && !(pattern.trim().length() == 0)) {
            searchFilter = "(&" + orgSearchFilter + "(" 
                           + orgRDNAttrName + "=" + pattern + "))";
        } else {
           searchFilter = orgSearchFilter;
        }
        if (debug.messageEnabled()) {
            debug.message(
            "Organization.getValidValues(): organization search filter is: " 
            + searchFilter);
        }

        String[] attrs = { orgRDNAttrName };
        LDAPConnection ld = null;
        Set validOrgDNs = new HashSet();
        int status = ValidValues.SUCCESS;
        try {
            LDAPSearchResults res = null;
            LDAPBindRequest bindRequest = LDAPRequestParser.parseBindRequest(
                3, authid, authpw);
            LDAPSearchRequest searchRequest =
                LDAPRequestParser.parseSearchRequest(baseDN, orgSearchScope,
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
                            debug.message("Organization.getValidValues(): "+
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
                if ( ld != null) {
                    connPool.close(ld);
                }
            }
            while (res.hasMoreElements()) {
                try {
                    LDAPEntry entry = res.next();
                    if (entry != null) {
                        validOrgDNs.add(entry.getDN());
                        if (debug.messageEnabled()) {
                            debug.message(
                   "Organization.getValidValues(): found org name =" 
                            + entry.getDN());
                        }
                    }
                } catch (LDAPReferralException lre) {
                    // ignore referrals
                    continue;
                } catch (LDAPException le) {
                    String objs[] = { orgName };
                    int resultCode = le.getLDAPResultCode();
                    if (resultCode == le.SIZE_LIMIT_EXCEEDED) {
                        debug.warning(
                      "Organization.getValidValues(): exceeded the size limit");
                        status = ValidValues.SIZE_LIMIT_EXCEEDED;
                    } else if (resultCode == le.TIME_LIMIT_EXCEEDED) {
                        debug.warning(
                      "Organization.getValidValues(): exceeded the time limit");
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
        if (debug.messageEnabled()) {
            debug.message("Organization.getValidValues(): return set=" 
                           + validOrgDNs.toString());
        }
        return(new ValidValues(status, validOrgDNs));
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
            debug.message("Organization.getValues() gets called");
        }
        return selectedOrgDNs;
    }


    /**
     * Sets the names for the instance of the <code>Organization</code>
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
            debug.error("Organization.setValues() Invalid names");
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "org_subject_invalid_user_names", null, 
                "null", PolicyException.USER_COLLECTION));
        }
        selectedOrgDNs = new HashSet();
        selectedOrgDNs.addAll(names);
        if (debug.messageEnabled()) {
            debug.message("Organization.setValues(): selected org names=" 
                          + selectedOrgDNs);
        }
        selectedRFCOrgDNs = new HashSet();
        // add to the RFC Set now
        Iterator it = names.iterator();
        while (it.hasNext()) {
            selectedRFCOrgDNs.add(new DN((String)it.next()).toRFCString().
                toLowerCase());
        }
    }


    /**
     * Determines if the user belongs to this instance
     * of the <code>Organization</code> object.
     *
     * @param token single-sign-on token of the user
     *
     * @return <code>true</code> if the user is member of the
     * given subject; <code>false</code> otherwise.
     *
     * @exception SSOException if <code>token</code> is not valid
     * @exception PolicyException if an error occured while
     * checking if the user is a member of this subject
     */

    public boolean isMember(SSOToken token)
        throws SSOException, PolicyException {

        boolean listenerAdded = false;

        String userLocalDN = token.getPrincipal().getName();
        if (debug.messageEnabled()) {
            debug.message("Organization.isMember(): user local DN is " 
                          + userLocalDN);
        }
        DN userDN = null;
        boolean found = false;
        if (selectedRFCOrgDNs.size() > 0) {
            String tokenID = token.getTokenID().toString();
            Iterator orgIter = selectedRFCOrgDNs.iterator();
            while (orgIter.hasNext()) {
                Boolean matchFound = null;
                String valueDN = (String)orgIter.next();
                if ((matchFound = SubjectEvaluationCache.isMember(
                tokenID, ldapServer,valueDN)) != null) {
                    if (debug.messageEnabled()) {
                        debug.message("Organization.isMember():Got membership "
                            +"from cache of " +userLocalDN+" in org "
                            +valueDN+ " :" +matchFound.booleanValue());
                    }
                    boolean result = matchFound.booleanValue();
                    if (result) {
                        return result;
                    } else {
                        continue;
                    }
                }
                // got here so entry not in subject evaluation cache
                if (debug.messageEnabled()) {
                    debug.message("Organization:isMember():entry for "+valueDN
                        +" not in subject evaluation cache, fetching from "
                        +"directory server.");
                }        

                if (userDN == null) {
                    userDN= getUserDN(token);
                    if (userDN == null) {
                        if (debug.messageEnabled()) {
                            debug.message("Organization.isMember(): User "
                            + userLocalDN + " is not found in the directory");
                        }
                        return false;
                    }
                }
                if (!listenerAdded) {
                        if (!PolicyEvaluator.ssoListenerRegistry.containsKey(
                        tokenID)) 
                    {
                        token.addSSOTokenListener(PolicyEvaluator.ssoListener);
                        PolicyEvaluator.ssoListenerRegistry.put(
                            tokenID, PolicyEvaluator.ssoListener);
                        if (debug.messageEnabled()) {
                            debug.message("Organization.isMember():"
                                    + " sso listener added .\n");
                        }
                        listenerAdded = true;
                    }
                }
                if (isMemberOfOrg(valueDN, userDN, tokenID)) {
                    if (debug.messageEnabled()) {
                        debug.message("Organization.isMember(): User " 
                        + userDN.toRFCString() + " is a member of the "
                        +"Organization object");
                    }
                    found = true;
                    break;
                }
            }
        }
         
        if (debug.messageEnabled()) {
            if (found) {
                debug.message("Organization.isMember(): User " + userLocalDN 
                 + " is a member of this Organization object"); 
            } else {
                debug.message("Organization.isMember(): User " + userLocalDN 
                 + " is not a member of this Organization object"); 
            }
        }
        return found;
    }


    /**
     *  Check if the user identified by the userDN belongs to organization
     * <code>org</code>
     */
    private boolean isMemberOfOrg(String org, DN userDN, String tokenID) {
        DN orgDN = new DN(org);
        boolean orgMatch = userDN.isDescendantOf(orgDN);
        if (debug.messageEnabled()) {
            String member = (orgMatch) ? "is member of":"is not a member of";
            debug.message("Organization.isMemberOfGroup(): User " 
                + userDN.toRFCString()+ " "+member+ " the Organization "+org
                +", adding to Subject eval cache");
        }
        SubjectEvaluationCache.addEntry(tokenID, ldapServer, 
                           org, orgMatch);
        return orgMatch;
    }


   /** 
    * Return a hash code for this <code>Organization</code>.
    *
    * @return a hash code for this <code>Organization</code> object.
    */

    public int hashCode() {
        return selectedOrgDNs.hashCode();
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
        if (o instanceof Organization) {
            Organization org = (Organization) o;
            if ((selectedOrgDNs != null) 
                 && (org.selectedOrgDNs != null) 
                 && (selectedOrgDNs.equals(org.selectedOrgDNs))) {
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
        Organization theClone = null;
        try {
            theClone = (Organization) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (selectedRFCOrgDNs != null) {
            theClone.selectedRFCOrgDNs = new HashSet();
            theClone.selectedRFCOrgDNs.addAll(selectedRFCOrgDNs);
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
    private DN getUserDN(SSOToken token) throws SSOException,
        PolicyException  
    {
        DN userDN = null;
        Set qualifiedUserDNs = new HashSet();
        String userLocalDN = token.getPrincipal().getName();
        if (localDS && !PolicyUtils.principalNameEqualsUuid( token)) {
            userDN = new DN(userLocalDN);
        } else {
            // try to figure out the user name from the local user DN
            int beginIndex = userLocalDN.indexOf("=");
            int endIndex = userLocalDN.indexOf(",");
            if ((beginIndex <= 0) || (endIndex <= 0) ||
                (beginIndex >= endIndex)) 
            { 
                throw (new PolicyException(ResBundleUtils.rbName,
                  "org_subject_invalid_local_user_dn", null, null));
            }
            String userName = userLocalDN.substring(
                                 beginIndex+1, endIndex);
            String searchFilter = null;
            if ((userSearchFilter != null) && 
                !(userSearchFilter.length() == 0)) 
            {
                searchFilter = "(&" + userSearchFilter +
                    PolicyUtils.constructUserFilter(token, 
                        userRDNAttrName, userName, aliasEnabled) + ")";
            } else {
                searchFilter = PolicyUtils.constructUserFilter(
                token, userRDNAttrName, userName, aliasEnabled);
            }
            if (debug.messageEnabled()) {
                debug.message(
                    "Organization.getUserDN(): search filter is: " 
                    + searchFilter);
            }
                    
            String[] attrs = { userRDNAttrName }; 
            // search the remote ldap and find out the user DN          
            LDAPConnection ld = null;
            try {
                LDAPSearchResults res = null;
                LDAPBindRequest bindRequest =
                    LDAPRequestParser.parseBindRequest(authid, authpw);
                LDAPSearchRequest searchRequest =
                    LDAPRequestParser.parseSearchRequest(baseDN,
                    userSearchScope, searchFilter, attrs, false, timeLimit,
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
                            qualifiedUserDNs.add(entry.getDN());
                        }
                    } catch (LDAPReferralException lre) {
                        // ignore referrals
                        continue;
                    } catch (LDAPException le) {
                        String objs[] = { orgName };
                        int resultCode = le.getLDAPResultCode();
                        if (resultCode == le.SIZE_LIMIT_EXCEEDED) {
                            debug.warning("Organization.getUserDN(): exceeded "
                                    +"the size limit");
                            throw (new PolicyException(
                                 ResBundleUtils.rbName,
                                "ldap_search_exceed_size_limit", 
                                objs, null));
                        } else if (resultCode == le.TIME_LIMIT_EXCEEDED) {
                            debug.warning("Organization.getUserDN(): exceeded "
                                    +"the time limit");
                            throw (new PolicyException(
                                 ResBundleUtils.rbName,
                                "ldap_search_exceed_time_limit", 
                                objs, null));
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
            if (qualifiedUserDNs.size() > 0) {
                if (debug.messageEnabled()) {
                    debug.message(
                       "Organization.getUserDN(): qualified users=" 
                       + qualifiedUserDNs);
                }
                Iterator iter = qualifiedUserDNs.iterator();
                // we only take the first qualified DN
                userDN = new DN((String)iter.next());
            }
        }
        return userDN;
    }
}
