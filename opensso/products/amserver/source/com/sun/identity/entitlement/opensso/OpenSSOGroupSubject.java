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
 * $Id: OpenSSOGroupSubject.java,v 1.2 2009/08/21 21:52:01 hengming Exp $
 */
package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.GroupSubject;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.SubjectAttributesCollector;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * This class represents group identity for membership check
 */
public class OpenSSOGroupSubject extends GroupSubject {
    /**
     * Constructor.
     */
    public OpenSSOGroupSubject() {
        super();
    }

    /**
     * Constructor.
     *
     * @param group the uuid of the group who is member of the 
     *        EntitlementSubject.
     */
    public OpenSSOGroupSubject(String group) {
        super(group);
    }

    /**
     * Constructs GroupSubject
     *
     * @param group the uuid of the group who is member of the
     *        EntitlementSubject
     * @param pSubjectName subject name as used in OpenSSO policy,
     *        this is releavant only when GroupSubject was created from
     *        OpenSSO policy Subject
     */
    public OpenSSOGroupSubject(String group, String pSubjectName) {
        super(group, pSubjectName);
    }

    /**
     * Returns <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementSubject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     * @throws com.sun.identity.entitlement,  EntitlementException in case
     * of any error
     */
    @Override
    public SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException {

        boolean satified = false;
        if (mgr.isGroupMembershipSearchIndexEnabled()) {
            Set publicCreds = subject.getPublicCredentials();
            if ((publicCreds != null) && !publicCreds.isEmpty()) {
                Map<String, Set<String>> attributes = (Map<String, Set<String>>)
                    publicCreds.iterator().next();
                Set<String> values = attributes.get(
                    SubjectAttributesCollector.NAMESPACE_MEMBERSHIP +
                    IdType.GROUP.getName());
                String grpID = getID();
                if (values != null) {
                    if (values.contains(grpID)) {
                        satified = true;
                    } else {
                        try {
                            SSOToken adminToken = (SSOToken)AccessController.
                                doPrivileged(AdminTokenAction.getInstance());
                            AMIdentity idGroup = IdUtils.getIdentity(adminToken,
                                grpID);
                            for(String value: values) {
                                AMIdentity amgrp = IdUtils.getIdentity(
                                    adminToken, value);
                                if (idGroup.equals(amgrp)) {
                                    satified = true;
                                    break;
                                }
                            }
                        } catch (IdRepoException e) {
                            PrivilegeManager.debug.error(
                                "GroupSubject.evaluate", e);
                        }
                    }
                }
            }
        } else {
            try {
                SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                AMIdentity idGroup = IdUtils.getIdentity(adminToken, getID());
                Set<IdType> supportedType = IdType.GROUP.canHaveMembers();
                for (IdType type : supportedType) {
                    if (isMember(subject, type, idGroup)) {
                        satified = true;
                        break;
                    }            
                }
            } catch (IdRepoException e) {
                PrivilegeManager.debug.error("GroupSubject.evaluate", e);
            } catch (SSOException e) {
                PrivilegeManager.debug.error("GroupSubject.evaluate", e);
            }
        }
        return new SubjectDecision(satified, Collections.EMPTY_MAP);
    }
    
    /**
     * Returns search index attributes.
     *
     * @return search index attributes.
     */
    @Override
    public Map<String, Set<String>> getSearchIndexAttributes() {
        SubjectAttributesManager sam = getSubjectAttributesManager();
        if (sam != null) {
            Map<String, Set<String>> map =
                new HashMap<String, Set<String>>(4);
            if (sam.isGroupMembershipSearchIndexEnabled()) {
                Set<String> set = new HashSet<String>();
                String uuid = getID();
                SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());
                try {
                    AMIdentity amid = IdUtils.getIdentity(adminToken, uuid);
                    set.add(OpenSSOSubjectAttributesCollector.
                        getIDWithoutOrgName(amid));
                } catch (IdRepoException ex) {
                    if (PrivilegeManager.debug.messageEnabled()) {
                        PrivilegeManager.debug.message(
                            "OpenSSOGroupSubject.getSearchIndexAttributes", ex);
                    }
                    set.add(uuid);
                }
                map.put(SubjectAttributesCollector.NAMESPACE_MEMBERSHIP +
                    IdType.GROUP.getName(), set);
            } else {
                Set<String> set = new HashSet<String>();
                set.add(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
                map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
            }
            return map;
        } else {
            return super.getSearchIndexAttributes();
        }
    }

    /**
     * Returns required attribute names.
     * 
     * @return required attribute names.
     */
    @Override
    public Set<String> getRequiredAttributeNames() {
        SubjectAttributesManager sam = getSubjectAttributesManager();
        if (sam != null) {
            if (sam.isGroupMembershipSearchIndexEnabled()) {
                Set<String> set = new HashSet<String>(2);
                set.add(SubjectAttributesCollector.NAMESPACE_MEMBERSHIP +
                    IdType.GROUP.getName());
                return set;
            } else {
                return(Collections.EMPTY_SET);
            }
        } else {
            return super.getRequiredAttributeNames();
        }
    }

    private boolean isMember(
        Subject subject,
        IdType type, 
        AMIdentity idGroup
    ) throws IdRepoException, SSOException {
        Set<Principal> userPrincipals = subject.getPrincipals();
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        for (Principal p : userPrincipals) {
            AMIdentity amid = IdUtils.getIdentity(adminToken, p.getName());
            Set<AMIdentity> memberships = amid.getMemberships(IdType.GROUP);
            for(AMIdentity amgrp: memberships) {
                if (amgrp.equals(idGroup)) {
                    return true;
                }
            }
        }
        return false;
    }

    private SubjectAttributesManager getSubjectAttributesManager() {
        String uuid = getID();
        if (uuid == null) {
            return null;
        }

        try {
            AMIdentity amid = new AMIdentity(null, uuid);
            String realm = amid.getRealm();
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            return SubjectAttributesManager.getInstance(
                SubjectUtils.createSubject(adminToken), realm);
        } catch (IdRepoException idex) {
            if (PrivilegeManager.debug.messageEnabled()) {
                PrivilegeManager.debug.message(
                    "OpenSSOGroupSubject.getSubjectAttributesManager:", idex);
            }
        }
        return null;
    }
}
