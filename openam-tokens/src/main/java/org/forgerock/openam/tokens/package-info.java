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

/**
 * A collection of classes is given here for describing how to store a bean-compliant POJO as a token. An example usage
 * is given below:
 *
 * <pre>
 * &#64;Type(TokenType.MY_TOKEN_TYPE)
 * public class MyBean {
 *     &#64;Field(CoreTokenField.TOKEN_ID`)
 *     private String id;
 *     &#64;Field(CoreTokenField.STRING_ONE)
 *     private String name;
 *     &#64;Field(CoreTokenField.BLOB, converter = MapToJsonBytesConverter.class)
 *     private Map&lt;String,?> complexData;
 *
 *     // getters and setters follow...
 * }
 * </pre>
 */
package org.forgerock.openam.tokens;