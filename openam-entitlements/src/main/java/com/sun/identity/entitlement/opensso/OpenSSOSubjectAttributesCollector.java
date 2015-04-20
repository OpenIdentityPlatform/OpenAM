/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OpenSSOSubjectAttributesCollector.java,v 1.3 2009/09/24 22:38:21 hengming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.SubjectAttributesCollector;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public class OpenSSOSubjectAttributesCollector
    implements SubjectAttributesCollector {

    static ServiceConfigManager idRepoServiceConfigManager;
    private static final String GROUP_MEMBERSHIP_SEARCH_INDEX_ENABLED_ATTR = 
        "groupMembershipSearchIndexEnabled";
    private static final String LDAPv3Config_USER_ATTR = 
        "sun-idrepo-ldapv3-config-user-attributes";
    private String realm;
    private boolean groupMembershipSearchIndexEnabled = false;

    static {
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            idRepoServiceConfigManager = new ServiceConfigManager(adminToken,
                IdConstants.REPO_SERVICE, "1.0");
        } catch (SSOException ssoex) {
            PrivilegeManager.debug.error(
                "OpenSSOSubjectAttributesCollector.static:", ssoex);
        } catch (SMSException smsex) {
            PrivilegeManager.debug.error(
                "OpenSSOSubjectAttributesCollector.static:", smsex);
        }
    }

    /**
     * Initializes this object with specified parameters.
     *
     * @param realm the realm
     * @param configMap configuration map
     */
    public void init(String realm, Map<String, Set<String>> configMap) {
        this.realm = realm;
        if (configMap != null) { //TODO do not know if this is right fix
            Set<String> values =
                configMap.get(GROUP_MEMBERSHIP_SEARCH_INDEX_ENABLED_ATTR);
            if ((values != null) && (!values.isEmpty())) {
                groupMembershipSearchIndexEnabled = Boolean.valueOf(
                    values.iterator().next()).booleanValue();
            }
        }
    }

    /**
     * Returns the attribute values of the given user represented by
     * <class>Subject</class> object.
     *
     * @param subject identity of the user
     * @param attrNames requested attribute names
     * @return a map of attribute names and their values
     * @throws com.sun.identity.entitlement.EntitlementException if this
     * operation failed.
     */
    public Map<String, Set<String>> getAttributes(
        Subject subject,
        Set<String> attrNames
    ) throws EntitlementException {
        String uuid = SubjectUtils.getPrincipalId(subject);
        try {
            Map<String, Set<String>> results = new
                HashMap<String, Set<String>>();
            Map<String, Set<String>> pubCreds = new
                HashMap<String, Set<String>>();

            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            AMIdentity amid = new AMIdentity(adminToken, uuid);

            Set<String> set = new HashSet<String>(2);
            set.add(getIDWithoutOrgName(amid));
            results.put(NAMESPACE_IDENTITY, set);
            set = new HashSet<String>(2);
            set.add(uuid);
            pubCreds.put(NAMESPACE_IDENTITY, set);

            Set<String> primitiveAttrNames = getAttributeNames(attrNames,
                NAMESPACE_ATTR);
            if (!primitiveAttrNames.isEmpty()) {
                Map<String, Set<String>> primitiveAttrValues =
                    amid.getAttributes(primitiveAttrNames);
                for (String name : primitiveAttrValues.keySet()) {
                    Set<String> values = primitiveAttrValues.get(name);
                    if (values != null) {
                        results.put(NAMESPACE_ATTR + name, values);
                        pubCreds.put(NAMESPACE_ATTR + name, values);
                    }
                }
            }

            Set<String> membershipAttrNames = getAttributeNames(attrNames,
                NAMESPACE_MEMBERSHIP);
            if (!membershipAttrNames.isEmpty()) {
                for (String m : membershipAttrNames) {
                    IdType type = IdUtils.getType(m);

                    if (type != null) {
                        Set<AMIdentity> memberships = amid.getMemberships(type);

                        if (memberships != null) {
                            Set<String> setMemberships = new HashSet<String>();
                            Set<String> membershipsCred = new HashSet<String>();
                            for (AMIdentity a : memberships) {
                                setMemberships.add(getIDWithoutOrgName(a));
                                membershipsCred.add(a.getUniversalId());
                            }
                            results.put(NAMESPACE_MEMBERSHIP + m,
                                setMemberships);
                            pubCreds.put(NAMESPACE_MEMBERSHIP + m,
                                membershipsCred);
                        }
                    }
                }
            }

            Set<Object> publicCreds = subject.getPublicCredentials();
            publicCreds.add(pubCreds);
            return results;
        } catch (SSOException e) {
            Object[] params = {uuid};
            throw new EntitlementException(600, params, e);
        } catch (IdRepoException e) {
            Object[] params = {uuid};
            throw new EntitlementException(600, params, e);
        }
    }

    private Set<String> getAttributeNames(Set<String> attrNames, String ns) {
        Set<String> results = new HashSet<String>();
        int len = ns.length();
        for (String s : attrNames) {
            if (s.startsWith(ns)) {
                results.add(s.substring(len));
            }
        }
        return results;
    }

    /**
     * Returns <code>true</code> if attribute value for the given user
     * represented by <class>Subject</class> object is present.
     *
     * @param subject identity of the user
     * @param attrName attribute name to check
     * @param attrValue attribute value to check
     * @return <code>true</code> if attribute value for the given user
     * represented by <class>Subject</class> object is present.
     * @throws com.sun.identity.entitlement.EntitlementException if this
     * operation failed.
     */
    public boolean hasAttribute(
        Subject subject,
        String attrName,
        String attrValue
    ) throws EntitlementException {
        String uuid = SubjectUtils.getPrincipalId(subject);
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            AMIdentity amid = new AMIdentity(adminToken, uuid);
            if (attrName.startsWith(NAMESPACE_ATTR)) {
                Set<String> values = amid.getAttribute(attrName.substring(
                    NAMESPACE_ATTR.length()));
                return (values != null) ? values.contains(attrValue) : false;
            } else if (attrName.startsWith(NAMESPACE_MEMBERSHIP)) {
                IdType type = IdUtils.getType(attrName.substring(
                    NAMESPACE_MEMBERSHIP.length()));
                if (type != null) {
                    AMIdentity parent = new AMIdentity(adminToken,
                        attrValue);
                    if (parent.getType().equals(type)) {
                        Set<String> members = parent.getMembers(IdType.USER);
                        return members.contains(amid.getUniversalId());
                    }
                }
            }
            return false;
        } catch (IdRepoException e) {
            Object[] params = {uuid};
            throw new EntitlementException(601, params, e);
        } catch (SSOException e) {
            Object[] params = {uuid};
            throw new EntitlementException(601, params, e);
        }
    }

    /**
     * Returns available subject attribute names.
     *
     * @return a set of available subject attribute names or null if not found
     */
    public Set<String> getAvailableSubjectAttributeNames()
        throws EntitlementException {

        try {
            ServiceConfig sc = idRepoServiceConfigManager.getOrganizationConfig(
                realm, null);
            if (sc == null) {
                return null;
            }
            Set subConfigNames = sc.getSubConfigNames();
            if ((subConfigNames == null) || (subConfigNames.isEmpty())) {
                return null;
            }

            CaseInsensitiveHashSet result = null;

            for(Iterator iter = subConfigNames.iterator(); iter.hasNext();) {
                String idRepoName = (String) iter.next();
                ServiceConfig reposc = sc.getSubConfig(idRepoName);
                Map attrMap = reposc.getAttributesForRead();
                Set userAttrs = (Set)attrMap.get(LDAPv3Config_USER_ATTR);
                if ((userAttrs != null) && (!userAttrs.isEmpty())) {
                    if (result == null) {
                        result = new CaseInsensitiveHashSet();
                    }
                    result.addAll(userAttrs);
                }
            }

            return result;
        } catch (SMSException e) {
            throw new EntitlementException(602, e);
        } catch (SSOException e) {
            throw new EntitlementException(602, e);
        }
    }

    /**
     * Returns true if group membership search index is enabled or false
     * otherwise.
     *
     * @return true if group membership search index is enabled or false
     * otherwise.
     */
    public boolean isGroupMembershipSearchIndexEnabled() {
        return groupMembershipSearchIndexEnabled;
    }

    /**
     * Returns the attribute values of the given user represented by
     * <class>Subject</class> object.
     *
     * @param subject identity of the user.
     * @param attrNames requested attribute names.
     * @return a map of attribute names and their values
     * @throws com.sun.identity.entitlement.EntitlementException if this
     * operation failed.
     */
    public Map<String, Set<String>> getUserAttributes(
        Subject subject,
        Set<String> attrNames
    ) throws EntitlementException {
        String uuid = SubjectUtils.getPrincipalId(subject);
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            AMIdentity amid = new AMIdentity(adminToken, uuid);
            return amid.getAttributes(attrNames);
        } catch (IdRepoException e) {
            Object[] params = {uuid};
            throw new EntitlementException(601, params, e);
        } catch (SSOException e) {
            Object[] params = {uuid};
            throw new EntitlementException(601, params, e);
        }
    }

    /**
     * Returns the universal identifier of this object without organization
     * name.
     * 
     * @return String representing the universal identifier of this object
     *     without organization name.
     */
    protected static String getIDWithoutOrgName(AMIdentity amidentity) {
        return ("id=" + LDAPDN.escapeValue(amidentity.getName()) + ",ou=" +
            amidentity.getType().getName());
    }
}
