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
 * $Id: UserSubject.java,v 1.1 2009/08/19 05:40:34 veiming Exp $
 */
package com.sun.identity.entitlement;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * EntitlementSubject to represent user identity for membership check
 * @author dorai
 */
public class UserSubject extends EntitlementSubjectImpl {
    /**
     * Constructs an UserSubject
     */
    public UserSubject() {
        super();
    }

    /**
     * Constructs UserSubject
     * @param user the uuid of the user who is member of the EntitlementSubject
     */
    public UserSubject(String user) {
        super(user);
    }

    /**
     * Constructs UserSubject
     * @param user the uuid of the user who is member of the EntitlementSubject
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when UserSubject was created from
     * OpenSSO policy Subject
     */
    public UserSubject(String user, String pSubjectName) {
        super(user, pSubjectName);
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
    public SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException {
        boolean satified = hasPrincipal(subject, getID()) ^ isExclusive();
        return new SubjectDecision(satified, Collections.EMPTY_MAP);
    }


    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(getID());
        map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
        return map;
    }

    public Set<String> getRequiredAttributeNames() {
        return(Collections.EMPTY_SET);
    }

    /**
     * Returns <code>true</code> is this subject is an identity object.
     *
     * @return <code>true</code> is this subject is an identity object.
     */
    public boolean isIdentity() {
        return true;
    }
}
