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

package com.sun.identity.entitlement;

import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;

public class JwtClaimSubjectTest {
    private static final String CLAIM = "testClaim";
    private static final String VALUE = "testValue";

    private JwtClaimSubject testSubject;

    @BeforeMethod
    public void createSubjectCondition() {
        testSubject = new JwtClaimSubject();
        testSubject.setClaimName(CLAIM);
        testSubject.setClaimValue(VALUE);
    }

    @Test
    public void shouldSaveCorrectly() {
        // Given
        final JwtClaimSubject newSubject = new JwtClaimSubject();

        // When
        newSubject.setState(testSubject.getState());

        // Then
        assertThat(newSubject.getClaimName()).isEqualTo(testSubject.getClaimName());
        assertThat(newSubject.getClaimValue()).isEqualTo(testSubject.getClaimValue());
    }

    @Test
    public void shouldDenyIfJwtPrincipalNotPresent() throws Exception {
        // Given
        final Subject subject = new Subject();

        // When
        final SubjectDecision result = testSubject.evaluate(null, null, subject, null, null);

        // Then
        assertThat(result.isSatisfied()).isFalse();
    }

    @Test
    public void shouldDenyIfClaimIsMissing() throws Exception {
        // Given
        final Subject subject = getTestSubject("wibble", "badger");

        // When
        final SubjectDecision result = testSubject.evaluate(null, null, subject, null, null);

        // Then
        assertThat(result.isSatisfied()).isFalse();
    }

    @Test
    public void shouldDenyIfClaimDoesNotMatch() throws Exception {
        // Given
        final Subject subject = getTestSubject(CLAIM, "badger");

        // When
        final SubjectDecision result = testSubject.evaluate(null, null, subject, null, null);

        // Then
        assertThat(result.isSatisfied()).isFalse();
    }

    @Test
    public void shouldAllowIfClaimDoesMatch() throws Exception {
        // Given
        final Subject subject = getTestSubject(CLAIM, VALUE);

        // When
        final SubjectDecision result = testSubject.evaluate(null, null, subject, null, null);

        // Then
        assertThat(result.isSatisfied()).isTrue();
    }

    private Subject getTestSubject(final String claimName, final String claimValue) {
        final JwtPrincipal principal = new JwtPrincipal(json(object(
                field("sub", "test"),
                field(claimName, claimValue))));

        return new Subject(false, CollectionUtils.asSet(principal), Collections.emptySet(), Collections.emptySet());
    }
}