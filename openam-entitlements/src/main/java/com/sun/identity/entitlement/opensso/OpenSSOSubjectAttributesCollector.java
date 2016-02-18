/*
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
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */

package com.sun.identity.entitlement.opensso;

import static java.util.Collections.singletonMap;
import static org.forgerock.openam.utils.CollectionUtils.isEmpty;

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
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    @Override
    public Map<String, Set<String>> getAttributes(Subject subject, Set<String> attributeNames)
            throws EntitlementException {
        String uuid = SubjectUtils.getPrincipalId(subject);

        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMIdentity identity = new AMIdentity(adminToken, uuid);

            Map<String, Set<String>> attributeKeyValues = new HashMap<>();
            attributeKeyValues.putAll(getIdentityUniversalIds(attributeNames, identity));
            attributeKeyValues.putAll(getIdentityAttributes(attributeNames, identity));

            return attributeKeyValues;
        } catch (SSOException | IdRepoException e) {
            throw new EntitlementException(EntitlementException.UNABLE_TO_RETRIEVE_SUBJECT_ATTRIBUTE, e, uuid);
        }
    }

    private Map<String, Set<String>> getIdentityUniversalIds(Set<String> attributeNames,
            AMIdentity identity) throws IdRepoException, SSOException {

        Set<String> identityTypes = filterSet(attributeNames, NAMESPACE_IDENTITY);

        if (isEmpty(identityTypes)) {
            return Collections.emptyMap();
        }

        Set<String> values = new HashSet<>();

        for (String identityType : identityTypes) {
            values.addAll(getUniversalIdForIdentityType(identityType, identity));
        }

        if (isEmpty(values)) {
            return Collections.emptyMap();
        }

        return singletonMap(NAMESPACE_IDENTITY, values);
    }

    private Set<String> getUniversalIdForIdentityType(String identityType,
            AMIdentity identity) throws IdRepoException, SSOException {

        if (identityType.equalsIgnoreCase(IdType.USER.getName())) {
            return Collections.singleton(identity.getUniversalId());
        }

        return checkTypeForMembership(identityType, identity);
    }

    private Set<String> checkTypeForMembership(String identityType,
            AMIdentity identity) throws IdRepoException, SSOException {

        IdType membershipType = IdUtils.getType(identityType);

        if (membershipType == null) {
            return Collections.emptySet();
        }

        @SuppressWarnings("unchecked")
        Set<AMIdentity> membershipIdentities = identity.getMemberships(membershipType);

        if (isEmpty(membershipIdentities)) {
            return Collections.emptySet();
        }

        Set<String> membershipUniversalIds = new HashSet<>();

        for (AMIdentity membership : membershipIdentities) {
            membershipUniversalIds.add(membership.getUniversalId());
        }

        return membershipUniversalIds;
    }

    private Map<String, Set<String>> getIdentityAttributes(Set<String> attributeNames,
            AMIdentity identity) throws IdRepoException, SSOException {

        Set<String> identityAttributes = filterSet(attributeNames, NAMESPACE_ATTR);

        if (isEmpty(identityAttributes)) {
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked")
        Map<String, Set<String>> attributeKeyValues = identity.getAttributes(identityAttributes);
        Map<String, Set<String>> attributePrefixKeyValues = new HashMap<>();

        for (Map.Entry<String, Set<String>> attributeKeyValue : attributeKeyValues.entrySet()) {
            String prefixKey = NAMESPACE_ATTR + attributeKeyValue.getKey();
            attributePrefixKeyValues.put(prefixKey, attributeKeyValue.getValue());
        }

        return attributePrefixKeyValues;
    }

    private Set<String> filterSet(Set<String> set, String prefix) {
        Set<String> filteredSet = new HashSet<>();
        int prefixLength = prefix.length();

        for (String value : set) {
            if (!value.startsWith(prefix)) {
                continue;
            }

            filteredSet.add(value.substring(prefixLength));
        }

        return filteredSet;
    }

    public Set<String> getAvailableSubjectAttributeNames() throws EntitlementException {
        CaseInsensitiveHashSet result = new CaseInsensitiveHashSet();
        try {
            ServiceConfig sc = idRepoServiceConfigManager.getOrganizationConfig(realm, null);
            if (sc != null) {
                Set<String> subConfigNames = sc.getSubConfigNames();
                if (subConfigNames != null)  {
                    for (String idRepoName : subConfigNames) {
                        ServiceConfig reposc = sc.getSubConfig(idRepoName);
                        Map<String, Set<String>> attrMap = reposc.getAttributesForRead();
                        Set<String> userAttrs = attrMap.get(LDAPv3Config_USER_ATTR);
                        if ((userAttrs != null) && !userAttrs.isEmpty()) {
                            result.addAll(userAttrs);
                        }
                    }
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

}
