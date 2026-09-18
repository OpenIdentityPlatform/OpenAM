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
 * Copyright 2026 3A Systems, LLC.
 */
package org.openidentityplatform.openam.federation.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;

import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class RealmGotoUrlValidatorTest {

    private static final String REALM = "/";

    @AfterMethod
    public void restoreTheRealValidator() {
        RealmGotoUrlValidator.setValidator(null);
    }

    private static void listOf(final Collection<String> patterns) {
        RealmGotoUrlValidator.setValidator(new RedirectUrlValidator<String>(new ValidDomainExtractor<String>() {
            @Override
            public Collection<String> extractValidDomains(String realm) {
                assertThat(realm).isEqualTo(REALM);
                return patterns;
            }
        }));
    }

    @Test
    public void refusesAnAbsoluteUrlOutsideTheRealmList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(RealmGotoUrlValidator.isValid("https://evil.example/", REALM)).isFalse();
    }

    @Test
    public void acceptsAUrlInTheRealmList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(RealmGotoUrlValidator.isValid("https://app.example.com/after/login", REALM)).isTrue();
    }

    @Test
    public void acceptsARelativeUrlWhateverTheList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(RealmGotoUrlValidator.isValid("/openam/console", REALM)).isTrue();
    }

    @Test
    public void refusesASchemeRelativeUrlOutsideTheList() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(RealmGotoUrlValidator.isValid("//evil.example/", REALM)).isFalse();
    }

    @Test
    public void refusesANonHttpScheme() {
        listOf(null);

        assertThat(RealmGotoUrlValidator.isValid("javascript:alert(1)", REALM)).isFalse();
    }

    /** The same rule as for the login goto: a realm that configures no list restricts nothing. */
    @Test
    public void permitsAnyHttpUrlWhenTheRealmConfiguresNoList() {
        listOf(null);

        assertThat(RealmGotoUrlValidator.isValid("https://anywhere.example/", REALM)).isTrue();
    }

    @Test
    public void refusesAnEmptyTarget() {
        listOf(null);

        assertThat(RealmGotoUrlValidator.isValid("", REALM)).isFalse();
        assertThat(RealmGotoUrlValidator.isValid(null, REALM)).isFalse();
    }

    @Test
    public void treatsAMissingRealmAsTheRoot() {
        listOf(Collections.singleton("https://app.example.com/*"));

        assertThat(RealmGotoUrlValidator.isValid("https://app.example.com/x", null)).isTrue();
    }
}
