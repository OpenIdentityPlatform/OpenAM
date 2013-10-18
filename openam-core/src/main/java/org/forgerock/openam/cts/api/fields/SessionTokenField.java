/*
 * Copyright 2013 ForgeRock AS.
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
 * Responsible for describing specific fields of interest in Session Tokens.
 *
 * @author robert.wapshott@forgerock.com
 */
public enum SessionTokenField {
    LATEST_ACCESS_TIME("latestAccessTime", CoreTokenField.STRING_ONE),
    SESSION_ID("sessionID", CoreTokenField.STRING_TWO);

    private final String sessionFieldName;
    private final CoreTokenField field;

    SessionTokenField(String fieldName, CoreTokenField field) {
        sessionFieldName = fieldName;
        this.field = field;
    }

    /**
     * The CoreTokenField that this Session Field is mapped to.
     * @return Non null CoreTokenField.
     */
    public CoreTokenField getField() {
        return field;
    }

    /**
     * The InternalSession class field name.
     * @return Non null, used for InternalSession mapping purposes.
     */
    public String getInternalSessionFieldName() {
        return sessionFieldName;
    }
}
