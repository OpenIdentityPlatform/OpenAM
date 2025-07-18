/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

package com.sun.identity.entitlement.util;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.forgerock.util.Reject;

/**
 * A representation of a policy attribute that can be searched for. Policy framework attributes are stored
 * in LDAP attributes, so have an LDAP attribute name as well as a policy attribute name.
 */
public class SearchAttribute {

    private final String attributeName;
    private final String ldapAttribute;

    /**
     * Create a new searchable attribute.
     * @param attributeName The name of the policy framework attribute.
     * @param ldapAttribute The name of the LDAP attribute that attribute will be stored in.
     */
    public SearchAttribute(String attributeName, String ldapAttribute) {
        Reject.ifNull(attributeName, ldapAttribute);
        this.attributeName = attributeName;
        this.ldapAttribute = ldapAttribute;
    }

    /**
     * Get the LDAP attribute name.
     */
    public String getLdapAttribute() {
        return ldapAttribute;
    }

    /**
     * Get the policy framework attribute name.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Join the LDAP attribute and policy attribute together with an operator.
     */
    public String toFilter(String operator) {
        return ldapAttribute + operator + attributeName;
    }

    @Override
    public int hashCode() {
        int result = attributeName.hashCode();
        result = 31 * result + ldapAttribute.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object that) {
        return this == that || (that instanceof SearchAttribute &&
                new EqualsBuilder()
                        .append(this.attributeName, ((SearchAttribute) that).attributeName)
                        .append(this.ldapAttribute, ((SearchAttribute) that).ldapAttribute)
                        .isEquals()
        );
    }
}
