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
package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.util.SearchFilter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.testng.AssertJUnit.fail;

public class SearchFilterFactoryTest {

    private SearchFilterFactory factory;

    @BeforeMethod
    public void setUp() {
        factory = new SearchFilterFactory();
    }

    @Test
    public void shouldParseEquals() throws EntitlementException {
        String filter = "badger=mammal";
        SearchFilter result = factory.getFilter(filter);
        assertThat(result.getOperator()).isEqualTo(SearchFilter.Operator.EQUAL_OPERATOR);
    }

    @Test
    public void shouldParseNumericOperator() throws EntitlementException {
        String filter = Privilege.LAST_MODIFIED_DATE_ATTRIBUTE + ">1235";
        SearchFilter result = factory.getFilter(filter);
        assertThat(result.getOperator()).isEqualTo(SearchFilter.Operator.GREATER_THAN_OPERATOR);
    }

    @Test
    public void shouldThrowExceptionIfNoOperatorProvided() {
        String filter = "badgerbadgerbadgermushroom";
        try {
            factory.getFilter(filter);
            fail();
        } catch (EntitlementException e) {
            assertThat(e.getErrorCode()).isEqualTo(EntitlementException.INVALID_SEARCH_FILTER);
        }
    }

    @Test
    public void shouldCatchInvalidNumericFormat() {
        String filter = Privilege.LAST_MODIFIED_DATE_ATTRIBUTE + ">ferret";
        try {
            factory.getFilter(filter);
            fail();
        } catch (EntitlementException e) {
            assertThat(e.getErrorCode()).isEqualTo(EntitlementException.INVALID_SEARCH_FILTER);
        }
    }

    @Test
    public void shouldConsiderWhiteSpaceOK() throws EntitlementException {
        String filter = "badger = another badger";
        SearchFilter result = factory.getFilter(filter);
        assertThat(result.getName()).isEqualTo("badger");
    }
}