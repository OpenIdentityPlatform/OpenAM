/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.cts.api.fields;

/**
 * SAMLTokenField defines a mapping between additional fields needed to service the requirements of
 * the SAML token. These fields are mapped to existing CoreTokenField fields.
 *
 * @author robert.wapshott@forgerock.com
 */
public enum SAMLTokenField {
    OBJECT_CLASS(CoreTokenField.STRING_ONE),
    SECONDARY_KEY(CoreTokenField.STRING_TWO);

    private final CoreTokenField field;

    /**
     * @param field CoreTokenField that this field is mapped to.
     */
    private SAMLTokenField(CoreTokenField field) {
        this.field = field;
    }

    /**
     * @return The mapped CoreTokenField.
     */
    public CoreTokenField getField() {
        return field;
    }

    /**
     * @return The name of the LDAP attribute.
     */
    @Override
    public String toString() {
        return getField().toString();
    }
}
