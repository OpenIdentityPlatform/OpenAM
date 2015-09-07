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
 */

package org.forgerock.openam.core.rest.sms;

/**
 * Constants for the REST SMS schema representations.
 */
public class SmsJsonSchema {

    static final String OBJECT_TYPE = "object";
    static final String TYPE = "type";
    static final String FORMAT = "format";
    static final String STRING_TYPE = "string";
    static final String PASSWORD_TYPE = "password";
    static final String PATTERN_PROPERTIES = "patternProperties";
    static final String ARRAY_TYPE = "array";
    static final String ITEMS = "items";
    static final String PROPERTIES = "properties";
    static final String TITLE = "title";
    static final String PROPERTY_ORDER = "propertyOrder";
    static final String DESCRIPTION = "description";
    static final String REQUIRED = "required";
    static final String ENUM = "enum";
    static final String NUMBER_TYPE = "number";
    static final String BOOLEAN_TYPE = "boolean";
    static final String READONLY = "readonly";

    private SmsJsonSchema() {}
}
