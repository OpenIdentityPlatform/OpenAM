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
 * $Id: DSAMERole.java,v 1.4 2009/01/28 05:35:01 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.policy.plugins;

import com.iplanet.am.sdk.*;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.policy.*;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.util.DN;
import java.util.*;

/**
 * DSAME Role plugin lets policy admins specify the DSAME roles as a subject.
 * The plugin validates a user belonging to a DSAME role specified with this 
 * plugin.
 */

public class DSAMERole implements Subject {

    private boolean initialized;
    private String organizationDN;
    private Set subjectRoles = Collections.EMPTY_SET;
    private int timeLimit;
    private int maxResults;
    private int roleSearchScope;
    private String ldapServer;

    private static final String LDAP_SCOPE_BASE = "SCOPE_BASE";
    private static final String LDAP_SCOPE_ONE = "SCOPE_ONE";

    // Debug 
    static Debug debug = Debug.getInstance(
        PolicyManager.POLICY_DEBUG_NAME);

    public DSAMERole() {
        // do nothing
    }

    /**
     * This method initializes the DSAME Role plugin with the organization 
     * DN, search configuration, ldap server name,  in which this plugin 
     * is specified for a <code>Policy</code>.
     *
     * @param configParams configuration parameters as a map.
     * The values in the map is <code>java.util.Set</code>,
     * which contains one or more configuration paramaters.
     *
     * @exception PolicyException if an error occured during
     * initialization of <code>Subject</code> instance
     */
    public void initialize(Map configParams) throws PolicyException {
        
        String configuredLdapServer = 
            (String)configParams.get(PolicyConfig.LDAP_SERVER);
        if (configuredLdapServer == null) {
            debug.error("DSAMERole.initialize(): failed to get LDAP "
              + "server name. If you enter more than one server name "
              + "in the policy config service's Primary LDAP Server "
              + "field, please make sure the ldap server name is preceded " 
              + "with the local server name.");
            throw (new PolicyException(ResBundleUtils.rbName,
                "invalid_ldap_server_host", null, null));
        }
        ldapServer = configuredLdapServer.toLowerCase();
        
        organizationDN = (String) configParams.get(
            PolicyConfig.IS_ROLES_BASE_DN);
        String scope = (String) configParams.get(
            PolicyConfig.IS_ROLES_SEARCH_SCOPE);
        if (scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            roleSearchScope = AMConstants.SCOPE_BASE;
        } else if (scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            roleSearchScope = AMConstants.SCOPE_ONE;
        } else {
            roleSearchScope = AMConstants.SCOPE_SUB;
        }
        try {
            timeLimit = Integer.parseInt((String) configParams.get(
                                        PolicyConfig.LDAP_SEARCH_TIME_OUT));
            maxResults = Integer.parseInt((String) configParams.get(
                                        PolicyConfig.LDAP_SEARCH_LIMIT));
        } catch (NumberFormatException nfe) {
            debug.error("Can not parse search parameters in DSAMERole", nfe);
            timeLimit = 5;
            maxResults = 100;
        }
        initialized = true;
    }

    /**
     * Returns the syntax of the values this <code>Subject</code> 
     * implementation can have.
     * @see com.sun.identity.policy.Syntax
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the syntax
     *
     * @return set of of valid names for the user collection.
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     *
     * @return syntax of the values for the <code>Subject</code>
     */
    public Syntax getValueSyntax(SSOToken token) throws SSOException {
        return (Syntax.MULTIPLE_CHOICE);
    }

    /**
     * Returns a list of possible values for the <code>Subject</code>.
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public ValidValues getValidValues(SSOToken token) throws
        SSOException, PolicyException {
        return (getValidValues(token, "*"));
    }

    /**
     * Returns a list of possible values for the <code>Subject
     * </code> that matches the pattern. 
     *
     * @param token the <code>SSOToken</code> that will be used
     * to determine the possible values
     *
     * @return <code>ValidValues</code> object
     *
     * @exception SSOException if SSO token is not valid
     * @exception PolicyException if unable to get the list of valid
     * names.
     */
    public ValidValues getValidValues(SSOToken token, String pattern)
        throws SSOException, PolicyException {
        if (!initialized) {
            throw (new PolicyException(ResBundleUtils.rbName,
                "role_subject_not_yet_initialized", null, null));
        }

        try {
            AMStoreConnection amConnection = new AMStoreConnection(token);
            AMOrganization orgObject = amConnection.getOrganization(
                organizationDN);
            AMSearchControl sc = new AMSearchControl();
            sc.setMaxResults(maxResults);
            sc.setTimeOut(timeLimit);
            sc.setSearchScope(roleSearchScope);
            AMSearchResults results = orgObject.searchAllRoles(pattern, sc);
            int status;
            switch(results.getErrorCode()) {
                case AMSearchResults.SUCCESS:
                    status = ValidValues.SUCCESS;
                    break;
                case AMSearchResults.SIZE_LIMIT_EXCEEDED:
                    status = ValidValues.SIZE_LIMIT_EXCEEDED;
                    break;
                case AMSearchResults.TIME_LIMIT_EXCEEDED:
                    status = ValidValues.TIME_LIMIT_EXCEEDED;
                    break;
                default:
                    status = ValidValues.SUCCESS;
            }
            return(new ValidValues(status, results.getSearchResults()));
        } catch (AMException e) {
            LDAPException lde = e.getLDAPException();
            if (lde != null) {
                int ldapErrorCode = lde.getLDAPResultCode();
                if (ldapErrorCode == LDAPException.INVALID_CREDENTIALS) {
                    throw (new PolicyException(ResBundleUtils.rbName,
                        "ldap_invalid_password", null, null));
                } else if (ldapErrorCode == LDAPException.NO_SUCH_OBJECT) {
                    String[] objs = { organizationDN };
                    throw (new PolicyException(ResBundleUtils.rbName,
                        "no_such_am_roles_base_dn", objs, null));
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
            throw (new PolicyException(e));
        }
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
        if (subjectRoles == null) {
            return (Collections.EMPTY_SET);
        }
        return (subjectRoles);
    }

    /**
     * Sets the names for the instance of the <code>Subject</code>
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
            throw (new InvalidNameException(ResBundleUtils.rbName,
                "role_subject_invalid_role_names", null, null,
                PolicyException.USER_COLLECTION));
        }

        if (names.isEmpty()) {
            subjectRoles = names;
        } else {
            subjectRoles = new HashSet();
            Iterator iter = names.iterator();
            while (iter.hasNext()) {
                String role = (String) iter.next();
                if (role != null) {
                    subjectRoles.add((new DN(role)).toRFCString().
                        toLowerCase());
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Set subjectRoles to: " + subjectRoles);
        }
    }

    /**
     * Determines if the user belongs to this instance of the 
     * <code>Subject</code> object.
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
        String tokenID = token.getTokenID().toString();
        String userDN = token.getPrincipal().getName();
        boolean listenerAdded = false;
        boolean roleMatch = false;
        Set roleSet = null;
        if (subjectRoles.size() > 0) {
            Iterator roleIter = subjectRoles.iterator();
            while (roleIter.hasNext()) {
                Boolean matchFound = null;
                String valueDN = (String)roleIter.next();
                if ((matchFound = SubjectEvaluationCache.isMember(
                tokenID, ldapServer,valueDN)) != null) {
                    if (debug.messageEnabled()) {
                        debug.message("DSAMERole.isMember():Got membership "
                            +"from cache of " +token.getPrincipal().getName()
                            +" in DSAME role "+valueDN
                            + " :"+matchFound.booleanValue());
                    }
                    boolean result = matchFound.booleanValue();
                    if (result) {
                        return result;
                    } else {
                        continue;
                    }
                }
                // got here so entry not in subject evalauation cache
                if (!listenerAdded) {
                    if (!PolicyEvaluator.ssoListenerRegistry.containsKey(
                            tokenID)) 
                    {
                        token.addSSOTokenListener(PolicyEvaluator.ssoListener);
                        PolicyEvaluator.ssoListenerRegistry.put(
                            tokenID, PolicyEvaluator.ssoListener);
                        if (debug.messageEnabled()) {
                            debug.message("DSAMERole.isMember():"
                                + " sso listener added .\n");
                        }
                        listenerAdded = true;
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("DSAMERole:isMember():entry for "
                        +valueDN+" not in subject evaluation cache, fetching "
                        +"from NS User Cache.");
                    }
                if (roleSet == null) {
                    roleSet = PolicyEvaluator.getUserNSRoleValues(token);
                }
                if ((roleSet != null) && !roleSet.isEmpty()) {
                    if (debug.messageEnabled()) {
                        debug.message("DSAMERole.isMember():" +
                        "\n  user roles: " + roleSet +
                        "\n  subject roles: " + subjectRoles);
                    }
                    if (roleSet.contains(valueDN)) { 
                        roleMatch = true;
                    }
                }
                if (debug.messageEnabled()) {
                        debug.message("DSAMERole.isMember:adding entry "
                          +tokenID+" "+ldapServer+" "+valueDN+" "+roleMatch
                        +" in subject evaluation cache.");
                }
                SubjectEvaluationCache.addEntry(tokenID, ldapServer, 
                      valueDN, roleMatch);
                if (roleMatch) {
                    break;
                }
            }
        }
        if (debug.messageEnabled()) {
            if (!roleMatch) { 
                debug.message("DSAMERole.isMember(): User " + userDN 
                  + " is not a member of this DSAMERole object"); 
            } else {
                debug.message("DSAMERole.isMember(): User " + userDN 
                  + " is a member of this DSAMERole object"); 
            }
        }
        return roleMatch;
    }


   /** 
    * Return a hash code for this <code>DSAMERole</code>.
    *
    * @return a hash code for this <code>DSAMERole</code> object.
    */

    public int hashCode() {
        return subjectRoles.hashCode();
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
        if (o instanceof DSAMERole) {
            DSAMERole role = (DSAMERole) o;
            return(subjectRoles.equals(role.subjectRoles));
        }
        return (false);
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public Object clone() {
        DSAMERole theClone = null;
        try {
            theClone = (DSAMERole) super.clone();
        } catch (CloneNotSupportedException e) {
            // this should never happen
            throw new InternalError();
        }
        if (subjectRoles != null) {
            theClone.subjectRoles = new HashSet();
            theClone.subjectRoles.addAll(subjectRoles);
        }
        return theClone;
    }
}
