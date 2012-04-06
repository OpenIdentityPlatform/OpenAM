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
 * $Id: AMRoleImpl.java,v 1.6 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.SearchControl;

/**
 * The <code>AMRole</code> interface provides methods to manage role
 *
 ** @deprecated  As of Sun Java System Access Manager 7.1.
 */

class AMRoleImpl extends AMObjectImpl implements AMRole {
    
    static String roleTypeAN = "iplanet-am-role-type";

    static String roleDNsAN = "nsroledn";

    // define a Integer object for getPolicyTemplate
    private static final Integer POLICY_TEMPLATE_INTEGER = new Integer(
            AMTemplate.POLICY_TEMPLATE);

    public AMRoleImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, ROLE);
    }

    AMRoleImpl(SSOToken ssoToken, String DN, int type) {
        super(ssoToken, DN, type);
    }

    /**
     * Gets the type of the role.
     * 
     * @return The type of the role.
     */
    public int getRoleType() throws AMException, SSOException {
        return getIntegerAttribute(roleTypeAN);
    }

    /**
     * Sets the type of the role.
     * 
     * @param roleType
     *            The type of the role.
     */
    public void setRoleType(int roleType) throws AMException, SSOException {
        setIntegerAttribute(roleTypeAN, roleType);
        store();
    }

    /**
     * Adds users to the role.
     * 
     * @param users
     *            The set of user DN's to be added to the role.
     */
    public void addUsers(Set users) throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        dsServices.modifyMemberShip(super.token, users, super.entryDN, ROLE,
                ADD_MEMBER);
    }

    /**
     * Removes users from the role.
     * 
     * @param users
     *            The set of user DN's to be removed from the role.
     */
    public void removeUsers(Set users) throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        dsServices.modifyMemberShip(super.token, users, super.entryDN, ROLE,
                REMOVE_MEMBER);
    }

    /**
     * Gets number of users in the role.
     * 
     * @return Number of users in the role.
     */
    public long getNumberOfUsers() throws AMException, SSOException {
        return getUserDNs().size();
    }

    /**
     * Gets the names of users in the role.
     * 
     * @return The names of users in the role.
     */
    public Set getUserDNs() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        return dsServices.getMembers(super.token, super.entryDN, profileType);
    }

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of Users matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     * 
     */
    public Set searchUsers(String wildcard, int level) throws AMException,
            SSOException {
        return searchUsers(wildcard, null, level);
    }

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set DNs of Users matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     * 
     */
    public AMSearchResults searchUsers(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchUsers(wildcard, null, searchControl);
    }

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specifed so that DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of Users matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchUsers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        if ((level != AMConstants.SCOPE_ONE)
                && (level != AMConstants.SCOPE_SUB))
            throw new AMException(AMSDKBundle.getString("123", super.locale),
                    "123");

        if ((wildcard == null) || (wildcard.length() == 0))
            throw new AMException(AMSDKBundle.getString("122", super.locale),
                    "122");

        String userFilter = "(&(" + AMNamingAttrManager.getNamingAttr(USER)
                + "=" + wildcard + ")" + getSearchFilter(AMObject.USER) + "("
                + roleDNsAN + "=" + super.entryDN + "))";

        String filter = null;
        if (avPairs == null) {
            filter = userFilter;
        } else {
            if (avPairs.isEmpty()) {
                filter = userFilter;
            } else {
                StringBuilder filterSB = new StringBuilder();

                filterSB.append("(&").append(userFilter).append("(|");
                Iterator iter = avPairs.keySet().iterator();

                while (iter.hasNext()) {
                    String attributeName = (String) (iter.next());
                    Iterator iter2 = ((Set) (avPairs.get(attributeName)))
                            .iterator();
                    while (iter2.hasNext()) {
                        String attributeValue = (String) iter2.next();
                        filterSB.append("(").append(attributeName).append("=")
                                .append(attributeValue).append(")");
                    }
                }
                filterSB.append("))");
                filter = filterSB.toString();
            }
        }

        return dsServices.search(super.token, getOrganizationDN(), filter,
                level);
    }

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specifed so that DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set DNs of Users matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        int level = searchControl.getSearchScope();
        if ((level != AMConstants.SCOPE_ONE)
                && (level != AMConstants.SCOPE_SUB))
            throw new AMException(AMSDKBundle.getString("123", super.locale),
                    "123");

        if ((wildcard == null) || (wildcard.length() == 0))
            throw new AMException(AMSDKBundle.getString("122", super.locale),
                    "122");

        String userFilter = "(&(" + AMNamingAttrManager.getNamingAttr(USER)
                + "=" + wildcard + ")" + getSearchFilter(AMObject.USER) + "("
                + roleDNsAN + "=" + super.entryDN + "))";

        String filter = null;
        if (avPairs == null) {
            filter = userFilter;
        } else {
            if (avPairs.isEmpty()) {
                filter = userFilter;
            } else {
                StringBuilder filterSB = new StringBuilder();

                filterSB.append("(&").append(userFilter).append("(|");
                Iterator iter = avPairs.keySet().iterator();

                while (iter.hasNext()) {
                    String attributeName = (String) (iter.next());
                    Iterator iter2 = ((Set) (avPairs.get(attributeName)))
                            .iterator();
                    while (iter2.hasNext()) {
                        String attributeValue = (String) iter2.next();
                        filterSB.append("(").append(attributeName).append("=")
                                .append(attributeValue).append(")");
                    }
                }
                filterSB.append("))");
                filter = filterSB.toString();
            }
        }

        SearchControl sc = searchControl.getSearchControl();
        String returnAttrs[] = searchControl.getReturnAttributes();
        return dsServices.search(super.token, getOrganizationDN(), filter, sc,
                returnAttrs);
    }

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specifed so that DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set DNs of Users matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchUsers(AMSearchControl searchControl,
            String avFilter) throws AMException, SSOException {
        int level = searchControl.getSearchScope();
        if ((level != AMConstants.SCOPE_ONE)
                && (level != AMConstants.SCOPE_SUB))
            throw new AMException(AMSDKBundle.getString("123", super.locale),
                    "123");

        String filter = "(&" + getSearchFilter(AMObject.USER) + "(" + roleDNsAN
                + "=" + super.entryDN + ")" + avFilter + ")";

        if (debug.messageEnabled()) {
            debug.message("AMRoleImpl.searchUsers: " + filter);
        }
        searchControl.setSearchScope(AMConstants.SCOPE_SUB);
        SearchControl sc = searchControl.getSearchControl();
        String returnAttrs[] = searchControl.getReturnAttributes();
        return dsServices.search(super.token, getOrganizationDN(), filter, sc,
                returnAttrs);
    }

    /**
     * Get requested templates defined for this role
     * 
     * @param templateReqs
     *            a Map of services names and template types. The key in the Map
     *            entry is the service name as a String, and the value of the
     *            Map entry is a java.lang.Integer whose int value is one of
     *            AMTemplate.DYNAMIC_TEMPLATE AMTemplate.POLICY_TEMPLATE
     *            AMTemplate.ALL_TEMPLATES
     * @return Set a Set of AMTemplate objects representing the templates
     *         requested. If the <code>templateReqs</code> argument is null or
     *         empty, the returned set will contain the AMTemplates for each
     *         registered service which has a template defined. If there is no
     *         template defined for any registered services for this role, an
     *         empty Set will be returned.
     * @throws AMException
     *             if there is an internal problem with AM Store
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public Set getTemplates(Map templateReqs) throws AMException, SSOException {
        return getTemplates(templateReqs, false);
    }

    /**
     * this method will retrieved required templates from DS,
     * 
     * @param policyOnly
     *            get policy templates only, this will apply only if
     *            templateReqs is null or empty
     */
    private Set getTemplates(Map templateReqs, boolean policyOnly)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);

        if (debug.messageEnabled()) {
            debug.message("AMRoleImpl.getTemplate: Map = [" + templateReqs
                    + "] DN=" + super.entryDN);
        }

        // issue search to find out all templates for this role
        // base for search is role's parent which is an Org
        Set set = dsServices.search(super.token, getParentDN(),
                "(&(objectclass=costemplate)(cn=\"" + super.entryDN + "\"))",
                AMConstants.SCOPE_SUB);
        // return empty set if there is no templates found at all
        if (set == null || set.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        // check if we need to get all templates
        boolean getAll = false;
        if (templateReqs == null || templateReqs.isEmpty()) {
            getAll = true;
        }
        Set retSet = new HashSet();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            // get the DN of the template entry
            DN dn = new DN((String) it.next());
            if (debug.messageEnabled()) {
                debug.message("AMRoleImpl.getTemplate: DN=" + dn);
            }
            // get the parent name of the template, which is a COS deinfition
            String serviceName = (((RDN) dn.getParent().getRDNs().get(0))
                    .getValues())[0];

            // check template type & service name
            int templateType = AMTemplate.DYNAMIC_TEMPLATE;
            if (getAll) {
                // get all templates, need to check if we want to get policy
                // templates only
                if (!policyOnly) {
                    retSet.add(new AMTemplateImpl(super.token,
                            dn.toRFCString(), serviceName, templateType));
                } else if (templateType == AMTemplate.POLICY_TEMPLATE) {
                    retSet.add(new AMTemplateImpl(super.token,
                            dn.toRFCString(), serviceName, templateType));
                }
            } else {
                // get specified type of templates
                Integer type = (Integer) templateReqs.get(serviceName);
                if (type == null) {
                    continue;
                }
                if (type.intValue() == AMTemplate.ALL_TEMPLATES
                        || type.intValue() == templateType) {
                    retSet.add(new AMTemplateImpl(super.token,
                            dn.toRFCString(), serviceName, templateType));
                }
            }
        }
        return retSet;
    }

    /**
     * Get requested policy templates defined for this role.
     * 
     * @param serviceNames
     *            a Set of services names, each specified as a java.lang.String.
     * @return Set a Set of AMTemplate objects representing the policy templates
     *         requested. If the <code>serviceNames</code> argument is null or
     *         empty, the returned set will contain the AMTemplates for each
     *         registered service which has a policy template defined. If there
     *         is no policy template defined for any registered services for
     *         this role, an empty Set will be returned.
     * @throws AMException
     *             if there is an internal problem with AM Store
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public Set getPolicyTemplates(Set serviceNames) throws AMException,
            SSOException {
        // check if serviceName is null or empty
        if (serviceNames == null || serviceNames.isEmpty()) {
            // get all policy template only
            return getTemplates(null, true);
        }

        // construct a map from the set
        Map map = new HashMap();
        Iterator it = serviceNames.iterator();
        while (it.hasNext()) {
            map.put((String) it.next(), POLICY_TEMPLATE_INTEGER);
        }
        return getTemplates(map, false);
    }

    /**
     * Gets all the assigned policies for this role
     * 
     * @return Set a set of assigned policy DNs
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public Set getAssignedPolicyDNs() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        throw new UnsupportedOperationException();
    }
}
