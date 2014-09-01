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
 * $Id: AMFilteredRoleImpl.java,v 1.5 2008/06/25 05:41:20 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.SearchControl;

/**
 * The <code>AMFilteredRoleImpl</code> implements interface AMFilteredRole
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMFilteredRoleImpl extends AMRoleImpl implements AMFilteredRole {
    
    public AMFilteredRoleImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, FILTERED_ROLE);
    }

    public void addUsers(Set users) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes users from the role.
     * 
     * @param users
     *            The set of user distinguished names to be removed from the
     *            role.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeUsers(Set users) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Searches for users in this filtered role using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that distinguished
     * names of users with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching users.
     * @param level
     *            the search level that needs to be used. 
     *            (<code>AMConstants.SCOPE_ONE</code> or 
     *             <code>AMConstansts.SCOPE_SUB</code>)
     * @return Set distinguished name of users matching the search.
     * @throws AMException
     *             if there is an internal error in the access management store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchUsers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        if ((level != AMConstants.SCOPE_ONE)
                && (level != AMConstants.SCOPE_SUB)) {
            throw new AMException(AMSDKBundle.getString("123", super.locale),
                    "123");
        }

        if ((wildcard == null) || (wildcard.length() == 0)) {
            throw new AMException(AMSDKBundle.getString("122", super.locale),
                    "122");
        }

        String userFilter = "(&(" + AMNamingAttrManager.getNamingAttr(USER)
                + "=" + wildcard + ")" + getFilter() + ")";

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

        return dsServices.search(token, getOrganizationDN(), filter, level);
    }

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specifed so that distinguished name
     * of users with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching users.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a set distinguished
     *         name of users matching the search.
     * @throws AMException
     *             if there is an internal error in the access management Store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        int level = searchControl.getSearchScope();

        if ((level != AMConstants.SCOPE_ONE)
                && (level != AMConstants.SCOPE_SUB)) {
            throw new AMException(AMSDKBundle.getString("123", super.locale),
                    "123");
        }

        if ((wildcard == null) || (wildcard.length() == 0)) {
            throw new AMException(AMSDKBundle.getString("122", super.locale),
                    "122");
        }

        String userFilter = "(&(" + AMNamingAttrManager.getNamingAttr(USER)
                + "=" + wildcard + ")" + getFilter() + ")";
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
     * search, attribute-value pairs can be specifed so that distinguished name
     * of users with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching users.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a set distinguished
     *         name of users matching the search.
     * @throws AMException
     *             if there is an internal error in the access management Store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchUsers(AMSearchControl searchControl,
            String avFilter) throws AMException, SSOException {
        int level = searchControl.getSearchScope();

        if ((level != AMConstants.SCOPE_ONE)
                && (level != AMConstants.SCOPE_SUB)) {
            throw new AMException(AMSDKBundle.getString("123", super.locale),
                    "123");
        }

        String filter = "(&" + getFilter() + avFilter + ")";

        if (debug.messageEnabled()) {
            debug.message("AMFilteredRoleImpl.searchUsers: " + filter);
        }

        searchControl.setSearchScope(AMConstants.SCOPE_SUB);
        SearchControl sc = searchControl.getSearchControl();
        String returnAttrs[] = searchControl.getReturnAttributes();
        return dsServices.search(super.token, getOrganizationDN(), filter, sc,
                returnAttrs);
    }

    /**
     * Returns the filter for the filtered role.
     * 
     * @return The filter for the filtered role.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public String getFilter() throws AMException, SSOException {
        return getStringAttribute(FILTER_ATTR_NAME);
    }

    /**
     * Returns the filter for the filtered role.
     *
     * @param filter filter for the filtered role.
     * @throws AMException if an error is encountered when trying to 
     *         access/retrieve data from the data store.
     * @throws SSOException if the single sign on token is no longer valid.
     */
    public void setFilter(String filter) throws AMException, SSOException {
        setStringAttribute(FILTER_ATTR_NAME, filter);
        store();
    }
}
