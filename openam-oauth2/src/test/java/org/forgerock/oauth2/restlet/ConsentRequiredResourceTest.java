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
 * Copyright 2026 3A Systems LLC.
 */

package org.forgerock.oauth2.restlet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.UserInfoClaims;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.testng.annotations.Test;

/**
 * Tests the raw consent model served to API clients. The distinguishing property against
 * {@code getDataModel()} is that nothing here is HTML-encoded.
 */
public class ConsentRequiredResourceTest {

    private static ResourceOwnerConsentRequired consentRequired(Map<String, String> scopeDescriptions,
            Map<String, String> claimDescriptions, Map<String, Object> claimValues,
            Map<String, List<String>> compositeScopes) {
        return new ResourceOwnerConsentRequired("client", "description", scopeDescriptions, claimDescriptions,
                new UserInfoClaims(claimValues, compositeScopes), "Demo User", true);
    }

    private static Map<String, Object> values(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(key, value);
        return map;
    }

    @Test
    public void shouldExposeScopeKeyAndDescription() {
        ResourceOwnerConsentRequired consent = consentRequired(
                Collections.singletonMap("profile", "Your personal information"),
                Collections.<String, String>emptyMap(), Collections.<String, Object>emptyMap(),
                Collections.<String, List<String>>emptyMap());

        JsonValue scopes = ConsentRequiredResource.rawScopesAndClaims(consent).get("scopes");

        assertThat(scopes.size()).isEqualTo(1);
        assertThat(scopes.get(0).get("name").asString()).isEqualTo("profile");
        assertThat(scopes.get(0).get("description").asString()).isEqualTo("Your personal information");
    }

    @Test
    public void shouldNestClaimsUnderTheirScope() {
        ResourceOwnerConsentRequired consent = consentRequired(
                Collections.singletonMap("profile", "Your personal information"),
                Collections.singletonMap("given_name", "First name"),
                values("given_name", "Demo"),
                Collections.singletonMap("profile", Collections.singletonList("given_name")));

        JsonValue model = ConsentRequiredResource.rawScopesAndClaims(consent);
        JsonValue claims = model.get("scopes").get(0).get("claims");

        assertThat(claims.size()).isEqualTo(1);
        assertThat(claims.get(0).get("name").asString()).isEqualTo("given_name");
        assertThat(claims.get(0).get("description").asString()).isEqualTo("First name");
        assertThat(claims.get(0).get("value").asString()).isEqualTo("Demo");
        // Consumed by the scope, so it must not also appear at the top level.
        assertThat(model.get("claims").size()).isEqualTo(0);
    }

    @Test
    public void shouldNotHtmlEncodeValues() {
        ResourceOwnerConsentRequired consent = consentRequired(
                Collections.singletonMap("profile", "Fish & Chips <b>"),
                Collections.singletonMap("given_name", "First & last"),
                values("given_name", "O'Brien & Sons"),
                Collections.singletonMap("profile", Collections.singletonList("given_name")));

        JsonValue scope = ConsentRequiredResource.rawScopesAndClaims(consent).get("scopes").get(0);

        assertThat(scope.get("description").asString()).isEqualTo("Fish & Chips <b>");
        assertThat(scope.get("claims").get(0).get("description").asString()).isEqualTo("First & last");
        assertThat(scope.get("claims").get(0).get("value").asString()).isEqualTo("O'Brien & Sons");
    }

    @Test
    public void shouldSkipScopeClaimsWithoutAValue() {
        ResourceOwnerConsentRequired consent = consentRequired(
                Collections.singletonMap("profile", "Your personal information"),
                Collections.singletonMap("given_name", "First name"),
                Collections.<String, Object>emptyMap(),
                Collections.singletonMap("profile", Collections.singletonList("given_name")));

        JsonValue scope = ConsentRequiredResource.rawScopesAndClaims(consent).get("scopes").get(0);

        assertThat(scope.get("claims").size()).isEqualTo(0);
    }

    @Test
    public void shouldUseNullDescriptionWhenClaimHasNone() {
        ResourceOwnerConsentRequired consent = consentRequired(
                Collections.singletonMap("profile", "Your personal information"),
                Collections.<String, String>emptyMap(),
                values("given_name", "Demo"),
                Collections.singletonMap("profile", Collections.singletonList("given_name")));

        JsonValue claim = ConsentRequiredResource.rawScopesAndClaims(consent).get("scopes").get(0)
                .get("claims").get(0);

        assertThat(claim.get("name").asString()).isEqualTo("given_name");
        assertThat(claim.get("description").getObject()).isNull();
    }

    @Test
    public void shouldListClaimsNotCoveredByAnyScope() {
        ResourceOwnerConsentRequired consent = consentRequired(
                Collections.singletonMap("profile", "Your personal information"),
                Collections.singletonMap("email", "Email address"),
                values("email", "demo@example.com"),
                Collections.<String, List<String>>emptyMap());

        JsonValue model = ConsentRequiredResource.rawScopesAndClaims(consent);

        assertThat(model.get("scopes").get(0).get("claims").size()).isEqualTo(0);
        assertThat(model.get("claims").size()).isEqualTo(1);
        assertThat(model.get("claims").get(0).get("name").asString()).isEqualTo("email");
        assertThat(model.get("claims").get(0).get("description").asString()).isEqualTo("Email address");
        assertThat(model.get("claims").get(0).get("value").asString()).isEqualTo("demo@example.com");
    }

    @Test
    public void shouldRenderNonStringClaimValuesAsText() {
        ResourceOwnerConsentRequired consent = consentRequired(
                Collections.<String, String>emptyMap(), Collections.<String, String>emptyMap(),
                values("email_verified", Boolean.TRUE),
                Collections.<String, List<String>>emptyMap());

        JsonValue model = ConsentRequiredResource.rawScopesAndClaims(consent);

        assertThat(model.get("claims").get(0).get("value").asString()).isEqualTo("true");
    }
}
