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
 * $Id: GroupSubject.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * This class represents group identity for membership check
 */
public class GroupSubject extends EntitlementSubjectImpl {
    public static final String GROUP_NAME = "group";

    /**
     * Constructor.
     */
    public GroupSubject() {
        super();
    }

    /**
     * Constructor.
     *
     * @param group the uuid of the group who is member of the 
     *        EntitlementSubject.
     */
    public GroupSubject(String group) {
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
    public GroupSubject(String group, String pSubjectName) {
        super(group, pSubjectName);
    }

    /**
     * Returns <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation.
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

        boolean satified = false;
        Set publicCreds = subject.getPublicCredentials();
        if ((publicCreds != null) && !publicCreds.isEmpty()) {
            Map<String, Set<String>> attributes = (Map<String, Set<String>>)
                publicCreds.iterator().next();
            Set<String> values = attributes.get(
                SubjectAttributesCollector.NAMESPACE_MEMBERSHIP + GROUP_NAME);
            satified = (values != null) ? values.contains(getID()) : false;
        }
        satified = satified ^ isExclusive();
        return new SubjectDecision(satified, Collections.EMPTY_MAP);
    }

    /**
     * Returns search index attributes.
     *
     * @return search index attributes.
     */
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>(4);
        {
            Set<String> set = new HashSet<String>();
            set.add(getID());
            map.put(SubjectAttributesCollector.NAMESPACE_MEMBERSHIP +
                GROUP_NAME, set);
        }
        {
            Set<String> set = new HashSet<String>();
            set.add(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
            map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
        }

        return map;
    }

    /**
     * Returns required attribute names.
     * 
     * @return required attribute names.
     */
    public Set<String> getRequiredAttributeNames() {
        Set<String> set = new HashSet<String>(2);
        set.add(SubjectAttributesCollector.NAMESPACE_MEMBERSHIP + GROUP_NAME);
        return set;
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
