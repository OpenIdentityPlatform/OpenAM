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
 * $Id: SubjectAttributesCollector.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Interface class for obtaining attributes for users. Implementations of
 * this class would be called during authorization to obtain users'
 * attributes and memberships.
 */
public interface SubjectAttributesCollector {
    /**
     * Attribute Namespace.
     */
    String NAMESPACE_ATTR = "attribute:";

    /**
     * Membership Namespace.
     */
    String NAMESPACE_MEMBERSHIP = "membership:";

    /**
     * Identity Namespace
     */
    String NAMESPACE_IDENTITY = "identity:";

    /**
     * All Entities.
     */
    String ATTR_NAME_ALL_ENTITIES = "all";

    /**
     * Initializes this object with specified parameters.
     *
     * @param realm the realm
     * @param configMap configuration map
     */
    public void init(String realm, Map<String, Set<String>> configMap);

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
    Map<String, Set<String>> getAttributes(
        Subject subject,
        Set<String> attrNames
    ) throws EntitlementException;

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
    boolean hasAttribute(
        Subject subject,
        String attrName,
        String attrValue) throws EntitlementException;

    /**
     * Returns available subject attribute names.
     *
     * @return a set of available subject attribute names or null if not found
     */
    public Set<String> getAvailableSubjectAttributeNames()
        throws EntitlementException;

    /**
     * Returns true if group membership search index is enabled or false
     * otherwise.
     *
     * @return true if group membership search index is enabled or false
     * otherwise.
     */
    public boolean isGroupMembershipSearchIndexEnabled();

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
    Map<String, Set<String>> getUserAttributes(
        Subject subject,
        Set<String> attrNames
    ) throws EntitlementException;
}
