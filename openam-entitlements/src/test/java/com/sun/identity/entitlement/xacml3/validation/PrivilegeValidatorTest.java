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
 * Copyright 2014 ForgeRock AS.
 */
package com.sun.identity.entitlement.xacml3.validation;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ReferralPrivilege;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.fail;

public class PrivilegeValidatorTest {

    private RealmValidator mockValidator;
    private PrivilegeValidator validator;

    @BeforeMethod
    public void setUp() throws Exception {
        mockValidator = mock(RealmValidator.class);
        validator = new PrivilegeValidator(mockValidator);
    }

    @Test
    public void shouldErrorIfRealmWasNotValid() throws EntitlementException {
        doThrow(new EntitlementException(EntitlementException.INVALID_SEARCH_FILTER))
                .when(mockValidator).validateRealms(anyListOf(String.class));
        try {
            validator.validateReferralPrivilege(createReferral("Badger"));
            fail();
        } catch (EntitlementException e) {
        }
    }

    private static ReferralPrivilege createReferral(String... realms) throws EntitlementException {
        return new ReferralPrivilege(
                "Name",
                Collections.<String, Set<String>>emptyMap(),
                new HashSet<String>(Arrays.asList(realms)));
    }
}