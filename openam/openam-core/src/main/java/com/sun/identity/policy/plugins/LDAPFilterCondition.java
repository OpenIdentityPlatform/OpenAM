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
 * $Id: LDAPFilterCondition.java,v 1.8 2009/11/20 23:52:55 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.policy.plugins;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.LDAPSearchResults;
import com.sun.identity.shared.ldap.LDAPBindRequest;
import com.sun.identity.shared.ldap.LDAPRequestParser;
import com.sun.identity.shared.ldap.LDAPSearchRequest;

import com.sun.identity.common.LDAPConnectionPool;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyConfig;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyEvaluator;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.ResBundleUtils;
import com.sun.identity.policy.SubjectEvaluationCache;
import com.sun.identity.policy.Syntax;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.ldap.LDAPEntry;
import com.sun.identity.shared.ldap.LDAPReferralException;


/**
 * The class <code>LDAPFilterCondition</code> is a plugin 
 * implementation of <code>Condition</code> interface.
 * This condition checks whether the ldap entry of the 
 * user identified by sso token, in the directory specified 
 * in policy configuration service, satisfiies the ldap filter
 * specified in the condition
 */

public class LDAPFilterCondition implements Condition {

    static final String LDAP_SCOPE_BASE = "SCOPE_BASE";
    static final String LDAP_SCOPE_ONE = "SCOPE_ONE";

    private static final Debug debug 
        = Debug.getInstance(PolicyManager.POLICY_DEBUG_NAME);

    private static List propertyNames = new ArrayList(1);
    private static final String AMPERSAND = "&";
    private static final String OPEN_PARENTHESIS = "(";
    private static final String CLOSE_PARENTHESIS = ")";


    static {
        propertyNames.add(LDAP_FILTER);
    }

    private Map properties;

    private String ldapConditionFilter;
    private long policyConfigExpiresAt;

    private String authid;
    private String authpw;
    private String baseDN;
    private String userSearchFilter;
    private int userSearchScope = LDAPv2.SCOPE_SUB;
    private String userRDNAttrName;
    private int timeLimit;
    private int maxResults;
    private boolean sslEnabled = false;
    private int minPoolSize;
    private int maxPoolSize;
    private String orgName;
    private LDAPConnectionPool connPool;
    private String ldapServer;
    private boolean aliasEnabled;

    /** 
     * No argument constructor 
     */
    public LDAPFilterCondition() {
    }

     /**
      * Returns the <code>List</code> of property names for the condition.
      *
      * @return <code>List</code> of property names
      */
     public List getPropertyNames() {
         return Collections.unmodifiableList(propertyNames);
     }
 
     /**
      * Returns the syntax for a property name
      * @see com.sun.identity.policy.Syntax
      *
      * @param property name of property for which to get <code>Syntax</code>
      *
      * @return <code>Syntax<code> for the property name
      */
     public Syntax getPropertySyntax(String property) {
         return (Syntax.ANY);
     }
      
     /**
      * Returns the display name for the property name.
      * The <code>locale</code> variable could be used by the
      * plugin to customize the display name for the given locale.
      * The <code>locale</code> variable could be <code>null</code>, in which 
      * case the plugin must use the default locale.
      *
      * @param property name of property for which to get the display name
      * @param locale locale for which to get the display name
      * @return display name for the property name
      */
     public String getDisplayName(String property, Locale locale) 
           throws PolicyException {
         return property;
     }
 
     /**
      * Returns the set of valid values given the property name. This method
      * is called if the property Syntax is either the SINGLE_CHOICE or 
      * MULTIPLE_CHOICE.
      *
      * @param property name of property for which to find valid values
      * @return <code>Set</code> of valid values for the property.
      * @throws exception PolicyException if unable to get the Syntax.
      */
     public Set getValidValues(String property) throws PolicyException { 
         return (Collections.EMPTY_SET);
     }


    /** 
     *  Sets the properties of the condition.
     *  Evaluation of <code>ConditionDecision</code> is influenced by these 
     *  properties.
     *  @param properties the properties of the condition that governs
     *         whether a policy applies. The keys in properties should
     *         be String objects.   Value corresponding to each key should 
     *         be a <code>Set</code> of String(s). Please note that properties 
     *         is not cloned by the method.
     *         This condition requires value for key <code>LDAP_FILTER</code>
     *         to be defined.  The value corresponding to the key should be a 
     *         Set with only one element. The element should be a  String.
     *
     *  @see #LDAP_FILTER
     *
     *  @throws PolicyException if properties is null or empty or does not
     *                  contain value for key LDAP_FILTER or contains values 
     *                  for other keys
     */

    public void setProperties(Map properties) throws PolicyException {
        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition."
                    + "setProperties():"
                    + "properties=" + properties);  
        }
        validateProperties(properties);
        this.properties = properties;
        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition."
                    + "setProperties():"
                    + "ldapConditionFilter=" + ldapConditionFilter);  
        }
    }


    /** 
     * Returns properties of this condition.
     * @return properties of the condition as an unmodifiable <code>Map</code>.
     */
    public Map getProperties() {
        return Collections.unmodifiableMap(properties);
    } 


    /**
     * Returns the decision computed by this condition object.
     *
     * @param token single sign on token of the user
     *
     * @param env request specific environment map of key/value pairs.
     *
     * @return the condition decision. The <code>ConditionDecision</code>
     *         encapsulates whether a policy applies for the request. 
     *         The condition decision would imply <code>true</code>, if 
     *         the  ldap entry of the user, in the directory specified by
     *         policy configuration service, satifies the ldap filter, 
     *         specified by <code>LDAP_FILTER</code> property of this condition.
     *         Otherwise, it would imply <code>false</code>
     *
     * Policy framework continues evaluating a policy only if it 
     * applies to the request as indicated by the CondtionDecision. 
     * Otherwise, further evaluation of the policy is skipped. 
     *
     * @throws SSOException if the token is invalid
     */

    public ConditionDecision getConditionDecision(SSOToken token, Map env) 
            throws PolicyException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition."
                    + "getConditionDecision():entering:"
                    + "principalDN=" + token.getPrincipal().getName()  
                    + ":ldapConditionFilter=" + ldapConditionFilter);  
        }
        boolean allowed = true;

        resetPolicyConfig(env);

        allowed = isMember(token);

        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.getConditionDecision():"
                    + "allowed= " + allowed);
        }

        return new ConditionDecision(allowed);
    }


    /**
     * Determines if the user statisfies the <code>ldapConditionFilter</code>
     * defined for this condition.
     *
     * @param token Single Sign On token of the user
     *
     * @return <code>true</code> if the user satisifes the <code>
     * ldapConditionFilter</code>
     *
     * @throws exception SSOException if Single Sign On token is not valid
     * @throws exception PolicyException if an error occured 
     */
    private boolean isMember(SSOToken token)
        throws SSOException, PolicyException {

        boolean member = false;
        boolean listenerAdded = false;
        String userLocalDN = token.getPrincipal().getName();

        String tokenID = token.getTokenID().toString();
        if (debug.messageEnabled()) {
            debug.message(
             "LDAPFilterCondition.isMember(): userLocalDN from ssoToken is: " 
                    + userLocalDN);

        }

        Boolean matchFound = null;
        if ((matchFound = SubjectEvaluationCache.isMember(
                tokenID, ldapServer, ldapConditionFilter)) != null) {
            if (debug.messageEnabled()) {
                debug.message("LDAPFilterCondition.isMember():"
                    + "Got membership "
                    +"from cache userLocalDN: " + userLocalDN 
                    + ", ldapConditionFilter: " + ldapConditionFilter
                    + " , member:" +matchFound.booleanValue());
            }
            boolean result = matchFound.booleanValue();
            if (result) {
                return result;
            }
        }

        // got here so entry not in subject evaluation cache
        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition:isMember():"
                    + " ldapConditionFilter:" + ldapConditionFilter
                    + " not in subject evaluation cache, "
                    + " fetching from directory server.");
        }

        // construct searchFilter for user
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
        String userMappingFilter = PolicyUtils.constructUserFilter(
                token, userRDNAttrName, userName, aliasEnabled);
        boolean multipleFilters = false;
        String searchFilter = null;
        if ((userSearchFilter != null) && !(userSearchFilter.equals(""))) {
            searchFilter =  trimAndParenthesise(userSearchFilter) 
                    + trimAndParenthesise(userMappingFilter);
                multipleFilters = true;
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.isMember(): "
                    + " user search filter is: " + userSearchFilter);
            debug.message("LDAPFilterCondition.isMember(): "
                    + " user mapping filter is: " + userMappingFilter);
            debug.message("LDAPFilterCondition.isMember(): "
                    + " condition ldapConditionFilter is: " 
                    + ldapConditionFilter);
        }

        //combine condition ldapConditionFilter and user search filter
        if ((ldapConditionFilter != null) 
                && (ldapConditionFilter.length() != 0)) {
            multipleFilters = true;
            searchFilter =  searchFilter + 
                 trimAndParenthesise(ldapConditionFilter);
        } 

        if (multipleFilters) {
            searchFilter = trimAndParenthesise(AMPERSAND + searchFilter);
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.isMember(): "
                    + " combined filter : " + searchFilter);
        }

        member = searchFilterSatisfied(searchFilter);

        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition:isMember():"
                    + " caching result, searchFilter:" + searchFilter
                    + ", member:" + member);
        }

        SubjectEvaluationCache.addEntry(tokenID, ldapServer, 
            ldapConditionFilter, member);
        if (!listenerAdded) {
            if (!PolicyEvaluator.ssoListenerRegistry.containsKey(
                tokenID)) 
            {
                token.addSSOTokenListener(PolicyEvaluator.ssoListener);
                PolicyEvaluator.ssoListenerRegistry.put(
                    tokenID, PolicyEvaluator.ssoListener);
                if (debug.messageEnabled()) {
                    debug.message("LDAPFilterCondition.isMember():"
                            + " sso listener added .\n");
                }
                listenerAdded = true;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.isMember():" 
                    + "member=" + member);
        }
        return member;
    }

    /**
     * returns a boolean result indicating if the specified
     * <code>searchFilter</code> is satisfied by 
     * making a directory search using the filter.
     */
    private boolean searchFilterSatisfied(String searchFilter) 
            throws SSOException, PolicyException {

        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.searchFilterSatified():"
                    + "entering, searchFitler=" + searchFilter); 
        }
        boolean filterSatisfied = false;
        String[] attrs = { userRDNAttrName };      

        // search the remote ldap         
        LDAPConnection ld = null;
        try {
            LDAPSearchResults res = null;
            int ldapVersion = 3;
            LDAPBindRequest bindRequest = LDAPRequestParser.parseBindRequest(
                ldapVersion, authid, authpw);
            LDAPSearchRequest searchRequest =
                LDAPRequestParser.parseSearchRequest(baseDN, userSearchScope,
                searchFilter, attrs, false, timeLimit,
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
            if (res.hasMoreElements()) {
                try {
                    LDAPEntry entry = res.next();
                    if (entry != null) {
                        String dn = entry.getDN();
                        if (dn != null && dn.length() != 0) {
                            if (debug.messageEnabled()) {
                                debug.message("LDAPFilterCondition."
                                        + "searchFilterSatified(): dn=" + dn);
                            }
                            filterSatisfied = true;
                        }
                    }
                } catch (LDAPReferralException lre) {
                    debug.warning("LDAPFilterCondition.searchFilterSatified()"
                            + ": Partial results have been received, status code 9."
                            + " The message provided by the LDAP server is: \n"
                            + lre.getLDAPErrorMessage());
                } catch (LDAPException le) {
                    int resultCode = le.getLDAPResultCode();
                    if (resultCode == le.SIZE_LIMIT_EXCEEDED) {
                        debug.warning(
                        "LDAPFilterCondition.searchFilterSatified(): "
                                + "exceeded the size limit");
                    } else if (resultCode == le.TIME_LIMIT_EXCEEDED) {
                        debug.warning(
                        "LDAPFilterCondition.searchFilterSatified(): "
                                + "exceeded the time limit");
                    } else {
                       throw le;
                    }
                }
                
            }
        } catch (LDAPException lde) {
            int ldapErrorCode = lde.getLDAPResultCode();
            if (ldapErrorCode == LDAPException.INVALID_CREDENTIALS) {
                throw (new PolicyException(ResBundleUtils.rbName,
                    "ldap_invalid_password", null, null));
            } else if (ldapErrorCode == LDAPException.NO_SUCH_OBJECT) {
                String[] objs = { baseDN };
                throw (new PolicyException(ResBundleUtils.rbName,
                    "no_such_ldap_users_base_dn", objs, null));
            } 
            String errorMsg = lde.getLDAPErrorMessage(); 
            String additionalMsg = lde.errorCodeToString();
            if (additionalMsg != null) {
                throw (new PolicyException(
                         errorMsg + ": " + additionalMsg));
            } else { 
                throw (new PolicyException(errorMsg));
            }
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.searchFilterSatified():"
                    + "returning, filterSatisfied=" + filterSatisfied); 
        }
        return filterSatisfied;
            
    }

    /**
     * This condition resets its policy configuration information,
     * perodically. The time period is based on the SUBJECTS_RESULT_TTL
     * defined in the policy config service.
     * @see #com.sun.identity.policy.PolicyConfig.SUBJECTS_RESULT_TTL
     */

    private void resetPolicyConfig(Map env) throws PolicyException {
        if (System.currentTimeMillis() > policyConfigExpiresAt) {
            Map policyConfigParams 
                    = (Map)env.get(PolicyEvaluator.SUN_AM_POLICY_CONFIG);
            setPolicyConfig(policyConfigParams);
        }
    }

    /**
     * Sets the policy configration parameters used by this
     * condition.
     */
    synchronized private void setPolicyConfig(Map configParams) 
            throws PolicyException {
        if (System.currentTimeMillis() < policyConfigExpiresAt) {
            return;
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.setPolicyConfig():"
                    + "policy config expired, resetting");
        }
        if (configParams == null) {
            debug.error("LDAPFilterCondition.setPolicyConfig():"
                    + "configParams is null");
            throw (new PolicyException(ResBundleUtils.rbName, 
                "ldapfiltercondition_setpolicyconfig_null_policy_config", 
                null, null));
        }

        String configuredLdapServer = 
            (String)configParams.get(PolicyConfig.LDAP_SERVER);
        if (configuredLdapServer == null) {
            debug.error("LDAPFilterCondition.initialize(): failed to get LDAP "
              + "server name. If you enter more than one server name "
              + "in the policy config service's Primary LDAP Server "
              + "field, please make sure the ldap server name is preceded " 
              + "with the local server name.");
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_ldap_server_host", null, null));
        }
        ldapServer = configuredLdapServer.toLowerCase();

        aliasEnabled = Boolean.valueOf((String) configParams.get(
            PolicyConfig.USER_ALIAS_ENABLED)).booleanValue();

        authid = (String) configParams.get(PolicyConfig.LDAP_BIND_DN);
        authpw = (String) configParams.get(PolicyConfig.LDAP_BIND_PASSWORD);
        if (authpw != null) {
            authpw = PolicyUtils.decrypt(authpw);
        }
        baseDN = (String) configParams.get(PolicyConfig.LDAP_USERS_BASE_DN);

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

        String ssl = (String) configParams.get(PolicyConfig.LDAP_SSL_ENABLED);
        if (ssl.equalsIgnoreCase("true")) {
            sslEnabled = true;
        } else {
            sslEnabled = false;
        }

        // get the organization name
        Set orgNameSet 
                = (Set) configParams.get(PolicyManager.ORGANIZATION_NAME);
        if ((orgNameSet != null) && (!orgNameSet.isEmpty())) {
            Iterator items = orgNameSet.iterator();
            orgName = (String) items.next();
        }


        if (debug.messageEnabled()) {
            debug.message("LDAPFilterCondition.setPolicyConfig(): "
                           + "getting params" 
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

        policyConfigExpiresAt = System.currentTimeMillis() 
                + PolicyConfig.getSubjectsResultTtl(configParams);
    }

    /**
     * Validates the <code>properties</code> set using the
     * setProperties public method. Checks for null,
     * presence of expected LDAP_FILTER property and no
     * other invalid property.
     * @see #LDAP_FILTER
     */
    private boolean validateProperties(Map properties) 
            throws PolicyException {
        if ( (properties == null) || ( properties.keySet() == null) ) {
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "properties_can_not_be_null_or_empty", null, null);
        }

        Set keySet = properties.keySet();
        //Check if the required key(s) are defined
        if ( !keySet.contains(LDAP_FILTER) ) {
            String args[] = { LDAP_FILTER };
            throw new PolicyException(
                    ResBundleUtils.rbName,"property_value_not_defined", 
                    args, null);
        }

        //Check if all the keys are valid 
        Iterator keys = keySet.iterator();
        while ( keys.hasNext()) {
            String key = (String) keys.next();
            if ( !LDAP_FILTER.equals(key) ) {
                String args[] = {key};
                throw new PolicyException(
                        ResBundleUtils.rbName,
                        "attempt_to_set_invalid_property", 
                        args, null);
            }
        }

        //validate LDAP_FILTER
        Set ldapFilterSet = (Set) properties.get(LDAP_FILTER);
        if ( ldapFilterSet != null ) {
            validateLdapFilterSet(ldapFilterSet);
        }

        return true;

    }
    /**
     * Validates the <code>Set</code> to contain
     * LDAP_FILTER property, checks  for null,
     * mutiple values ( not allowed).
     * @see #LDAP_FILTER
     */

    private boolean validateLdapFilterSet(Set ldapFilterSet) 
            throws PolicyException {
        if ( ldapFilterSet.isEmpty() ) {
            String args[] = { LDAP_FILTER };
            throw new PolicyException(
                    ResBundleUtils.rbName,
                    "property_does_not_allow_empty_values", 
                    args, null);
        }
        if ( ldapFilterSet.size() > 1) {
            String args[] = { LDAP_FILTER };
            throw new PolicyException(
                   ResBundleUtils.rbName,
                "property_does_not_allow_multiple_values", 
                args, null);
        }

        try {
            ldapConditionFilter = (String) (ldapFilterSet.iterator().next());
        } catch (ClassCastException e) {
            String args[] = { LDAP_FILTER };
            throw new PolicyException(
                ResBundleUtils.rbName,"property_is_not_a_String", 
                args, null);
        }

        return true;
    }

    /**
     * Utility method to trim and put parenthessis in the
     * expected ldap filter format for a String representing
     * the ldap filter.
     */

    private String trimAndParenthesise(String str) {
        String parenthesisedString = str;
        if (str != null)  {
            str = str.trim();
            if (!str.startsWith(OPEN_PARENTHESIS)) {
                parenthesisedString = OPEN_PARENTHESIS + str + 
                    CLOSE_PARENTHESIS;
            }
        } else {
            parenthesisedString = OPEN_PARENTHESIS + CLOSE_PARENTHESIS;
        }
        return parenthesisedString;
    }

    /**
     * Returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        LDAPFilterCondition theClone = null;
        try {
            theClone = (LDAPFilterCondition)super.clone();
        } catch (CloneNotSupportedException e) {
            //this should never happen
            throw new InternalError();
        }

        if (properties != null) {
            theClone.properties = new HashMap();
            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                Object o = it.next();
                Set values = new HashSet();
                values.addAll((Set) properties.get(o));
                theClone.properties.put(o, values);
            }
        }
        return theClone;
    }
}
