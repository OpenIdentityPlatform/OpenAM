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
package org.forgerock.openam.entitlement.rest;

import static org.fest.assertions.Fail.fail;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;

import com.sun.identity.entitlement.EntitlementException;
import org.assertj.core.api.Condition;
import org.fest.assertions.Assertions;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.promise.Promise;

/**
 * Test utils class to assist with Entitlements testing.
 *
 * @since 13.0.0
 */
public class EntitlementTestUtils {

    protected static void assertQueryPromiseFailedWithCodes(Promise<QueryResponse, ResourceException> promise,
            int resourceErrorCode, int entitlementErrorCode) {
        try {
            promise.getOrThrowUninterruptibly();
            fail("Should throw ResourceException");
        } catch (ResourceException e) {
            Assertions.assertThat(e.getCode()).isEqualTo(resourceErrorCode);
            Assertions.assertThat(e.getCause()).isInstanceOf(EntitlementException.class);
            Assertions.assertThat(((EntitlementException) e.getCause()).getErrorCode()).isEqualTo(entitlementErrorCode);
        }
    }

    protected static void assertResourcePromiseFailedWithCodes(Promise<ResourceResponse, ResourceException> promise,
            int resourceErrorCode, int entitlementErrorCode) {

        assertThat(promise).failedWithResourceException().withCode(resourceErrorCode);

        assertThat(promise).failedWithResourceException().withCause().isInstanceOf(EntitlementException.class)
                .has(entitlementErrorCode(entitlementErrorCode));
    }

    protected static Condition<Throwable> entitlementErrorCode(final int errorCode) {
        return new Condition<Throwable>() {
            @Override
            public boolean matches(Throwable throwable) {
                EntitlementException ee = (EntitlementException) throwable;
                return ee.getErrorCode() == errorCode;
            }
        };
    }
}
