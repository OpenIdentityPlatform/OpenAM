/*
 * Copyright 2014 ForgeRock, AS.
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

package org.forgerock.openam.policy.plugins;

import com.sun.identity.policy.ConditionDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.Syntax;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for {@link OAuth2ScopeCondition}.
 *
 * @since 12.0.0
 */
public class OAuth2ScopeConditionTest {
    private static final Set<String> SCOPES = new LinkedHashSet<String>(Arrays.asList("a", "b", "c"));
    private static final Map<String, Set<String>> VALID_PROPERTIES =
            Collections.singletonMap(OAuth2ScopeCondition.OAUTH2_SCOPE_PROPERTY,
                Collections.singleton(StringUtils.join(SCOPES, ' ')));

    private OAuth2ScopeCondition condition;

    @BeforeMethod
    public void setup() {
        condition = new OAuth2ScopeCondition();
    }

    @Test
    public void shouldHaveADefaultConstructor() {
        // Required by the Condition plugin framework
        new OAuth2ScopeCondition();
    }

    @Test
    public void shouldDefineASingleProperty() {
        // Given

        // When
        List propertyNames = condition.getPropertyNames();

        // Then
        assertEquals(propertyNames, Collections.singletonList(OAuth2ScopeCondition.OAUTH2_SCOPE_PROPERTY));
    }

    @Test
    public void shouldUseFreeFormStringSyntax() {
        // Given

        // When
        Syntax syntax = condition.getPropertySyntax(OAuth2ScopeCondition.OAUTH2_SCOPE_PROPERTY);

        // Then
        assertEquals(syntax, Syntax.ANY);
    }

    @Test
    public void shouldDefineDisplayLabel() throws Exception {
        // Given

        // When
        String message = condition.getDisplayName(OAuth2ScopeCondition.OAUTH2_SCOPE_PROPERTY, Locale.ENGLISH);

        // Then
        assertNotNull(message);
        assertFalse(message.isEmpty());
    }

    @Test(expectedExceptions = PolicyException.class)
    public void shouldRejectNullProperties() throws Exception {
        condition.setProperties(null);
    }

    @Test(expectedExceptions = PolicyException.class)
    public void shouldRejectEmptyProperties() throws Exception {
        condition.setProperties(Collections.<String, Set<String>> emptyMap());
    }

    @Test(expectedExceptions = PolicyException.class)
    public void shouldRejectUnknownProperties() throws Exception {
        // Given
        Map<String, Set<String>> properties = new HashMap<String, Set<String>>(VALID_PROPERTIES);
        properties.put("unknown", Collections.<String>emptySet());

        // When
        condition.setProperties(properties);

        // Then - exception
    }

    @Test(expectedExceptions = PolicyException.class)
    public void shouldRejectEmptyScopes() throws Exception {
        // Given
        Map<String, Set<String>> properties = Collections.singletonMap(OAuth2ScopeCondition.OAUTH2_SCOPE_PROPERTY,
                Collections.<String>emptySet());

        // When
        condition.setProperties(properties);

        // Then - exception
    }

    @Test(expectedExceptions = PolicyException.class)
    public void shouldRejectInvalidScopeSyntax() throws Exception {
        // Given
        String invalid = "scope\\"; // Backslash is not allowed in a scope name
        Map<String, Set<String>> properties = Collections.singletonMap(OAuth2ScopeCondition.OAUTH2_SCOPE_PROPERTY,
                Collections.singleton(invalid));

        // When
        condition.setProperties(properties);

        // Then - exception
    }

    @Test
    public void shouldAcceptValidScopes() throws Exception {
        // Given

        // When
        condition.setProperties(VALID_PROPERTIES);

        // Then
        assertEquals(condition.getProperties(), VALID_PROPERTIES);
    }

    @Test
    public void shouldDenyRequestWithoutScopes() throws Exception {
        // Given
        condition.setProperties(VALID_PROPERTIES);

        // When
        ConditionDecision decision = condition.getConditionDecision(null, null);

        // Then
        assertFalse(decision.isAllowed());
    }

    @Test
    public void shouldDenyRequestWithoutAllRequiredScopes() throws Exception {
        // Given
        condition.setProperties(VALID_PROPERTIES);
        Map<String, Set<String>> requestScopes = Collections.singletonMap(OAuth2ScopeCondition.REQUEST_SCOPE_ATTRIBUTE,
                Collections.singleton("a"));

        // When
        ConditionDecision decision = condition.getConditionDecision(null, requestScopes);

        // Then
        assertFalse(decision.isAllowed());
    }

    @Test
    public void shouldAllowRequestWithExactlyMatchingScopes() throws Exception {
        // Given
        condition.setProperties(VALID_PROPERTIES);
        Map<String, Set<String>> requestScopes = Collections.singletonMap(OAuth2ScopeCondition.REQUEST_SCOPE_ATTRIBUTE,
                Collections.singleton(StringUtils.join(SCOPES, ' ')));

        // When
        ConditionDecision decision = condition.getConditionDecision(null, requestScopes);

        // Then
        assertTrue(decision.isAllowed());
    }

    @Test
    public void shouldAllowRequestWithAdditionalScopes() throws Exception {
        // Given
        condition.setProperties(VALID_PROPERTIES);
        Map<String, Set<String>> requestScopes = Collections.singletonMap(OAuth2ScopeCondition.REQUEST_SCOPE_ATTRIBUTE,
                Collections.singleton(StringUtils.join(SCOPES, ' ') + " otherScope"));

        // When
        ConditionDecision decision = condition.getConditionDecision(null, requestScopes);

        // Then
        assertTrue(decision.isAllowed());
    }
}
