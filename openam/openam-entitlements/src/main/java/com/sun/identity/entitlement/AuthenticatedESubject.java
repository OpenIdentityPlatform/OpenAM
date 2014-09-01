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
 * $Id: AuthenticatedESubject.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

public class AuthenticatedESubject extends VirtualSubject {
    public static final String ID = "_authenticated";

    public VirtualId getVirtualId() {
        return VirtualId.AUTHENTICATED;
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        //DO NOTHING
    }

    /**
     * Returns state of the object
     * @return state of the object encoded as string
     */
    public String getState() {
        return "";
    }

    /**
     * Returns <code>SubjectDecision</code> of
     * <code>ESubject</code> evaluation
     * @param subject ESubject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>SubjectDecision</code> of
     * <code>ESubject</code> evaluation
     * @throws com.sun.identity.entitlement,  EntitlementException in case
     * of any error
     */
    public SubjectDecision evaluate(
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException {
        return null;
    }

    public void setName(String name) {
    }

    public String getName() {
        return null;
    }

    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
        map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
        return map;
    }

    public Set<String> getRequiredAttributeNames() {
        return Collections.EMPTY_SET;
    }

    public SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        return new SubjectDecision(true, Collections.EMPTY_MAP); //TOFIX
    }

    public boolean isIdentity() {
        return true;
    }

}
