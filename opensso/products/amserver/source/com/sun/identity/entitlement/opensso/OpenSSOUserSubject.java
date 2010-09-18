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
 * $Id: OpenSSOUserSubject.java,v 1.2 2009/08/21 21:52:01 hengming Exp $
 */
package com.sun.identity.entitlement.opensso;


import java.security.AccessController;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.SubjectAttributesCollector;
import com.sun.identity.entitlement.UserSubject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;

/**
 * OpenSSOUserSubject to represent user identity for membership check
 * @author dorai
 */
public class OpenSSOUserSubject extends UserSubject {
    /**
     * Constructs an OpenSSOUserSubject
     */
    public OpenSSOUserSubject() {
        super();
    }

    /**
     * Constructs OpenSSOUserSubject
     * @param user the uuid of the user who is member of the OpenSSOUserSubject
     */
    public OpenSSOUserSubject(String user) {
        super(user);
    }

    /**
     * Constructs OpenSSOUserSubject
     * @param user the uuid of the user who is member of the OpenSSOUserSubject
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when OpenSSOUserSubject was created from
     * OpenSSO policy Subject
     */
    public OpenSSOUserSubject(String user, String pSubjectName) {
        super(user, pSubjectName);
    }

    /**
     * Returns search index attributes.
     *
     * @return search index attributes.
     */
    @Override
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        String uuid = getID();
        try {
            AMIdentity amid = new AMIdentity(adminToken, uuid);
            set.add(OpenSSOSubjectAttributesCollector.getIDWithoutOrgName(
                amid));
        } catch (IdRepoException ex) {
            if (PrivilegeManager.debug.messageEnabled()) {
                PrivilegeManager.debug.message(
                    "OpenSSOUserSubject.getSearchIndexAttributes", ex);
            }
            set.add(uuid);
        }
        map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
        return map;
    }

    @Override
    protected boolean hasPrincipal(Subject subject, String uuid) {
        Set<Principal> userPrincipals = subject.getPrincipals();
        for (Principal p : userPrincipals) {
            String puuid = p.getName();
            if (puuid.equals(uuid)) {
                return true;
            }

            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                AMIdentity pamid = new AMIdentity(adminToken, puuid);
                AMIdentity amid = new AMIdentity(adminToken, uuid);
                if (pamid.equals(amid)) {
                    return true;
                }
            } catch (IdRepoException ex) {
                if (PrivilegeManager.debug.messageEnabled()) {
                    PrivilegeManager.debug.message(
                        "EntitlementSubjectImpl.hasPrincipal", ex);
                }
            }
        }
        return false;
    }
}
