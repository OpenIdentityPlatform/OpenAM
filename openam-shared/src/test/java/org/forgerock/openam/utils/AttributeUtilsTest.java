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
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.utils;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AttributeUtilsTest {

    public static String ATTRIBUTE_NAME = "attributeName";
    public static String BINARY_ATTRIBUTE_NAME = ATTRIBUTE_NAME + AttributeUtils.BINARY_FLAG;
    public static String STATIC_ATTRIBUTE_NAME = AttributeUtils.STATIC_QUOTE + ATTRIBUTE_NAME + AttributeUtils.STATIC_QUOTE;

    @Test
    public void testIsBinaryAttribute() throws Exception {
        assertTrue(AttributeUtils.isBinaryAttribute(BINARY_ATTRIBUTE_NAME));
    }

    @Test
    public void testRemoveBinaryAttributeFlag() throws Exception {
        assertEquals(AttributeUtils.removeBinaryAttributeFlag(BINARY_ATTRIBUTE_NAME), ATTRIBUTE_NAME);
    }

    @Test
    public void testIsStaticAttribute() throws Exception {
        assertTrue(AttributeUtils.isStaticAttribute(STATIC_ATTRIBUTE_NAME));
    }

    @Test
    public void testRemoveStaticAttributeFlag() throws Exception {
        assertEquals(AttributeUtils.removeStaticAttributeFlag(STATIC_ATTRIBUTE_NAME), ATTRIBUTE_NAME);
    }
}