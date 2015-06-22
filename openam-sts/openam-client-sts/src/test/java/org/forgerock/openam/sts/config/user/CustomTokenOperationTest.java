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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.sts.config.user;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;


public class CustomTokenOperationTest {
    private static final String CUSTOM_TOKEN_NAME = "BOBO_TOKEN";
    private static final String CUSTOM_TOKEN_OPERATION_IMPL = "com.company.section.BoboTokenValidator";
    private static final String SMS_STRING = CUSTOM_TOKEN_NAME + "|" + CUSTOM_TOKEN_OPERATION_IMPL;
    private static final String FAULTY_SMS_STRING = "FOO";

    @Test
    public void testEquals() {
        CustomTokenOperation customTokenOperation = new CustomTokenOperation(CUSTOM_TOKEN_NAME, CUSTOM_TOKEN_OPERATION_IMPL);
        CustomTokenOperation customTokenOperation2 = new CustomTokenOperation(CUSTOM_TOKEN_NAME, CUSTOM_TOKEN_OPERATION_IMPL);
        assertEquals(customTokenOperation, customTokenOperation2);
        assertNotEquals(customTokenOperation, new CustomTokenOperation(CUSTOM_TOKEN_NAME, "foo"));
    }

    @Test
    public void testJsonMarshalling() {
        CustomTokenOperation customTokenOperation = new CustomTokenOperation(CUSTOM_TOKEN_NAME, CUSTOM_TOKEN_OPERATION_IMPL);
        assertEquals(customTokenOperation, CustomTokenOperation.fromJson(customTokenOperation.toJson()));
    }

    @Test
    public void testSMSStringMarshalling() {
        assertEquals(new CustomTokenOperation(CUSTOM_TOKEN_NAME, CUSTOM_TOKEN_OPERATION_IMPL), CustomTokenOperation.fromSMSString(SMS_STRING));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidSMSStringMarshalling() {
        CustomTokenOperation.fromSMSString(FAULTY_SMS_STRING);
    }
}
