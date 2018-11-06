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

package org.forgerock.openam.session.service.access.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.QueryFilters;
import org.forgerock.openam.cts.api.fields.SessionTokenField;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder.FilterAttributeBuilder;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.util.query.QueryFilter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.IdType;

public class SessionQueryFilterVisitorTest {

    private static final String MOCK_UUID = "id=demo,ou=people,dc=openam,dc=openidentityplatform,dc=org";
    @Mock
    private IdentityUtils identityUtils;
    private SessionQueryFilterVisitor visitor;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        visitor = new SessionQueryFilterVisitor(identityUtils);
    }

    @Test
    public void shouldConstructFilterWithUsernameAndRealm() {
        // Given
        final QueryFilter<JsonPointer> filter = QueryFilters.parse("username eq \"demo\" and realm eq \"/\"");
        final FilterAttributeBuilder builder = new TokenFilterBuilder().and();
        given(identityUtils.getUniversalId(anyString(), any(IdType.class), anyString())).willReturn(MOCK_UUID);

        // When
        filter.accept(visitor, builder);

        // Then
        assertThat(builder.build().getQuery()).isEqualTo(QueryFilter.and(
                QueryFilter.equalTo(CoreTokenField.USER_ID, MOCK_UUID),
                QueryFilter.equalTo(SessionTokenField.REALM.getField(), "/")));
    }

    @Test
    public void shouldConstructFilterWithRealmOnly() {
        // Given
        final QueryFilter<JsonPointer> filter = QueryFilters.parse("realm eq \"/\"");
        final FilterAttributeBuilder builder = new TokenFilterBuilder().and();
        given(identityUtils.getUniversalId(anyString(), any(IdType.class), anyString())).willReturn(MOCK_UUID);

        // When
        filter.accept(visitor, builder);

        // Then
        assertThat(builder.build().getQuery()).isEqualTo(QueryFilter.and(
                QueryFilter.equalTo(SessionTokenField.REALM.getField(), "/")));
    }

    @Test
    public void shouldIgnoreUsernameWithoutRealm() {
        // Given
        final QueryFilter<JsonPointer> filter = QueryFilters.parse("username eq \"demo\"");
        final FilterAttributeBuilder builder = new TokenFilterBuilder().and();
        given(identityUtils.getUniversalId(anyString(), any(IdType.class), anyString())).willReturn(MOCK_UUID);

        // When
        filter.accept(visitor, builder);

        // Then
        assertThat(builder.build().getQuery()).isEqualTo(QueryFilter.and());
    }
}
