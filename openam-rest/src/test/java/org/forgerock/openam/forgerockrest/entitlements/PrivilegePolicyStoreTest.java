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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.util.SearchFilter;
import com.sun.identity.shared.DateUtils;
import org.forgerock.json.resource.QueryFilter;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.AttributeType;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryAttribute;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singleton;
import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PrivilegePolicyStoreTest {

    private static final String STRING_ATTRIBUTE = "stringAttribute";
    private static final String DATE_ATTRIBUTE = "dateAttribute";
    private static final String NUMERIC_ATTRIBUTE = "numberAttribute";

    private PrivilegeManager mockManager;
    private PrivilegePolicyStore testStore;

    @BeforeClass
    public void mockPrivilegeClass() {
        System.setProperty(Privilege.PRIVILEGE_CLASS_PROPERTY, StubPrivilege.class.getName());
    }

    @AfterClass
    public void unmockPrivilegeClass() {
        System.clearProperty(Privilege.PRIVILEGE_CLASS_PROPERTY);
    }

    @BeforeMethod
    public void setupMocks() {
        mockManager = mock(PrivilegeManager.class);

        Map<String, QueryAttribute> queryAttributes = new HashMap<String, QueryAttribute>();
        queryAttributes.put(STRING_ATTRIBUTE, new QueryAttribute(AttributeType.STRING, STRING_ATTRIBUTE));
        queryAttributes.put(NUMERIC_ATTRIBUTE, new QueryAttribute(AttributeType.NUMBER, NUMERIC_ATTRIBUTE));
        queryAttributes.put(DATE_ATTRIBUTE, new QueryAttribute(AttributeType.TIMESTAMP, DATE_ATTRIBUTE));

        testStore = new PrivilegePolicyStore(mockManager, queryAttributes);
    }

    @Test
    public void shouldDelegateReadsToPrivilegeManager() throws Exception {
        // Given
        String id = "testPolicy";
        Privilege policy = new StubPrivilege();
        given(mockManager.findByName(id)).willReturn(policy);

        // When
        Privilege response = testStore.read(id);

        // Then
        verify(mockManager).findByName(id);
        assertThat(response).isSameAs(policy);
    }

    @Test
    public void shouldAddPoliciesToPrivilegeManager() throws Exception {
        // Given
        Privilege policy = new StubPrivilege();

        // When
        Privilege response = testStore.create(policy);

        // Then
        verify(mockManager).add(policy);
        assertThat(response).isSameAs(policy);
    }

    @Test
    public void shouldDelegateUpdatesToPrivilegeManager() throws Exception {
        // Given
        String name = "test";
        Privilege policy = new StubPrivilege();

        // When
        Privilege response = testStore.update(name, policy);

        // Then
        verify(mockManager).modify(name, policy);
        assertThat(response).isSameAs(policy);
    }


    @Test
    public void shouldDelegateDeletesToPrivilegeManager() throws Exception {
        // Given
        String id = "testPolicy";

        // When
        testStore.delete(id);

        // Then
        verify(mockManager).remove(id);
    }

    @Test
    public void shouldTranslateAlwaysTrueQueryFilterToEmptySearchFilters() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.alwaysTrue());

        // When
        testStore.query(request);

        // Then
        verify(mockManager).search(Collections.<SearchFilter>emptySet());
    }

    @Test
    public void shouldSendAllMatchingPoliciesToQueryHandler() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.alwaysTrue());
        List<Privilege> policies = Arrays.<Privilege>asList(
                new StubPrivilege("one"), new StubPrivilege("two"), new StubPrivilege("three"));
        given(mockManager.search(anySetOf(SearchFilter.class))).willReturn(policies);

        // When
        List<Privilege> result = testStore.query(request);

        // Then
        assertThat(result).isEqualTo(policies);
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = ".*'false' not supported.*")
    public void shouldRejectAlwaysFalseQueryFilters() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.alwaysFalse());

        // When
        testStore.query(request);

        // Then - exception
    }

    @Test
    public void shouldHandleStringEquality() throws Exception {
        // Given
        String value = "testValue";
        QueryRequest request = mockQueryRequest(QueryFilter.equalTo(STRING_ATTRIBUTE, value));

        // When
        testStore.query(request);

        // Then
        verify(mockManager).search(singleton(new SearchFilter(STRING_ATTRIBUTE, value)));
    }

    @DataProvider(name = "SupportedQueryOperators")
    public static Object[][] supportedQueryOperators() {
        return new Object[][] {
                { "eq", SearchFilter.Operator.EQUAL_OPERATOR },
                // Treat >= and > as both greater-than-or-equals as that is all the search filters support
                { "gt", SearchFilter.Operator.GREATER_THAN_OPERATOR },
                { "ge", SearchFilter.Operator.GREATER_THAN_OPERATOR },
                // Same for <= and <.
                { "lt", SearchFilter.Operator.LESSER_THAN_OPERATOR },
                { "le", SearchFilter.Operator.LESSER_THAN_OPERATOR }
        };
    }

    @Test(dataProvider = "SupportedQueryOperators")
    public void shouldTranslateSupportedOperators(String queryOperator, SearchFilter.Operator expectedOperator)
    throws Exception {
        // Given
        long value = 123l;
        QueryRequest request = mockQueryRequest(QueryFilter.comparisonFilter(NUMERIC_ATTRIBUTE, queryOperator, value));

        // When
        testStore.query(request);

        // Then
        verify(mockManager).search(singleton(
                new SearchFilter(NUMERIC_ATTRIBUTE, value, expectedOperator)));
    }

    @DataProvider(name = "UnsupportedOperators")
    public static Object[][] unsupportedQueryOperators() {
        // We do not support starts-with, contains or any extended operators
        return new Object[][] {{ "sw" }, { "co" }, { "someExtendedOperator" }};
    }

    @Test(dataProvider = "UnsupportedOperators", expectedExceptions = EntitlementException.class,
        expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectUnsupportedQueryOperators(String queryOperator) throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.comparisonFilter(NUMERIC_ATTRIBUTE, queryOperator, 123l));

        // When
        testStore.query(request);

        // Then - exception
    }

    @Test(expectedExceptions = EntitlementException.class, expectedExceptionsMessageRegExp = ".*Unknown query field.*")
    public void shouldRejectUnknownAttributes() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.equalTo("unknown", "a value"));

        // When
        testStore.query(request);

        // Then - exception
    }

    @Test(dataProvider = "SupportedQueryOperators")
    public void shouldSupportDateQueries(String queryOperator, SearchFilter.Operator expectedOperator)
            throws Exception {
        // Given
        Date value = new Date(123456789000l); // Note: only second accuracy supported in timestamp format
        QueryRequest request = mockQueryRequest(QueryFilter.comparisonFilter(DATE_ATTRIBUTE, queryOperator,
                DateUtils.toUTCDateFormat(value)));

        // When
        testStore.query(request);

        // Then
        // Date should be converted into a time-stamp long value
        verify(mockManager).search(
                singleton(new SearchFilter(DATE_ATTRIBUTE, value.getTime(), expectedOperator)));
    }

    @Test(expectedExceptions = EntitlementException.class, expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectPresenceQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.present(STRING_ATTRIBUTE));

        // When
        testStore.query(request);

        // Then - exception
    }

    @Test
    public void shouldHandleAndQueries() throws Exception {
        // Given
        String value1 = "value1";
        String value2 = "value2";
        QueryRequest request = mockQueryRequest(
                QueryFilter.and(QueryFilter.equalTo(STRING_ATTRIBUTE, value1),
                        QueryFilter.equalTo(STRING_ATTRIBUTE, value2)));

        // When
        testStore.query(request);

        // Then
        verify(mockManager).search(
                asSet(new SearchFilter(STRING_ATTRIBUTE, value1),
                        new SearchFilter(STRING_ATTRIBUTE, value2)));
    }

    @Test(expectedExceptions = EntitlementException.class, expectedExceptionsMessageRegExp = ".*'Or' not supported.*")
    public void shouldRejectOrQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.or(QueryFilter.alwaysTrue(), QueryFilter.alwaysTrue()));

        // When
        testStore.query(request);

        // Then - exception
    }

    @Test(expectedExceptions = EntitlementException.class, expectedExceptionsMessageRegExp = ".*not supported.*")
    public void shouldRejectNotQueries() throws Exception {
        // Given
        QueryRequest request = mockQueryRequest(QueryFilter.not(QueryFilter.alwaysTrue()));

        // When
        testStore.query(request);

        // Then - exception
    }

    private QueryRequest mockQueryRequest(QueryFilter filter) {
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(filter);
        return request;
    }
}
