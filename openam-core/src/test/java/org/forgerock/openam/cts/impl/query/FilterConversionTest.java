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
package org.forgerock.openam.cts.impl.query;

import org.forgerock.openam.cts.api.filter.TokenFilter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class FilterConversionTest {

    private QueryFactory mockFactory;
    private QueryFilter mockQueryFilter;
    private QueryFilter.QueryFilterBuilder mockAndBuilder;
    private QueryFilter.QueryFilterBuilder mockOrBuilder;

    @BeforeMethod
    public void setup() {
        mockFactory = mock(QueryFactory.class);
        mockQueryFilter = mock(QueryFilter.class);
        given(mockFactory.createFilter()).willReturn(mockQueryFilter);

        mockAndBuilder = mock(QueryFilter.QueryFilterBuilder.class);
        mockOrBuilder = mock(QueryFilter.QueryFilterBuilder.class);
        given(mockQueryFilter.and()).willReturn(mockAndBuilder);
        given(mockQueryFilter.or()).willReturn(mockOrBuilder);
    }

    @Test
    public void shouldCreateAndFilter() {
        new FilterConversion(mockFactory).convert(new TokenFilterBuilder().and().build());
        verify(mockAndBuilder).build();
    }

    @Test
    public void shouldCreateOrFilter() {
        new FilterConversion(mockFactory).convert(new TokenFilterBuilder().or().build());
        verify(mockOrBuilder).build();
    }

    @Test
    public void shouldIncludeAttributes() {
        // Given
        String value = "badger";
        CoreTokenField field = CoreTokenField.TOKEN_ID;
        TokenFilter tokenFilter = new TokenFilterBuilder().and()
                .withAttribute(field, value).build();

        // When
        new FilterConversion(mockFactory).convert(tokenFilter);

        // Then
        verify(mockAndBuilder).attribute(eq(field), eq(value));
    }
}