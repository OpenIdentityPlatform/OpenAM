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
 * Copyright 2014-2015 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.SearchFilter;
import static com.sun.identity.entitlement.util.SearchFilter.Operator.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

import static com.sun.identity.entitlement.Application.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.*;

/**
 * @since 12.0.0
 */
public class ApplicationManagerTest {

    private final boolean MATCH = true;
    private final boolean NO_MATCH = false;

    @DataProvider(name = "testMatchData")
    public Object[][] testMatchData() throws IllegalAccessException, InstantiationException {

        Application dummyApplication = application("Application");
        dummyApplication.setDescription("An example application");
        dummyApplication.setCreatedBy("amadmin");
        dummyApplication.setCreationDate(10);
        dummyApplication.setLastModifiedBy("bjensen");
        dummyApplication.setLastModifiedDate(100);

        return new Object[][] {

                // Name
                { asSet(nameFilter("Application")), dummyApplication, MATCH },
                { asSet(nameFilter("*")), dummyApplication, MATCH },
                { asSet(nameFilter("A*t*o*")), dummyApplication, MATCH },
                { asSet(nameFilter("iPlanetAMWebAgentService")), dummyApplication, NO_MATCH },

                // Description
                { asSet(descriptionFilter("An example application")), dummyApplication, MATCH },
                { asSet(descriptionFilter("*")), dummyApplication, MATCH },
                { asSet(descriptionFilter("Now is the winter of our discontent...")), dummyApplication, NO_MATCH },

                // Created By
                { asSet(createdByFilter("amadmin")), dummyApplication, MATCH },
                { asSet(createdByFilter("*")), dummyApplication, MATCH },
                { asSet(createdByFilter("rumpelstiltskin")), dummyApplication, NO_MATCH },

                // Last Modified By
                { asSet(lastModifiedByFilter("bjensen")), dummyApplication, MATCH },
                { asSet(lastModifiedByFilter("*")), dummyApplication, MATCH },
                { asSet(lastModifiedByFilter("rumpelstiltskin")), dummyApplication, NO_MATCH },

                // Creation Date
                { asSet(creationDateFilter(11, LESS_THAN_OPERATOR)), dummyApplication, MATCH },
                { asSet(creationDateFilter(10, LESS_THAN_OPERATOR)), dummyApplication, NO_MATCH },
                { asSet(creationDateFilter(10, LESS_THAN_OR_EQUAL_OPERATOR)), dummyApplication, MATCH },
                { asSet(creationDateFilter(10, EQUALS_OPERATOR)), dummyApplication, MATCH },
                { asSet(creationDateFilter(10, GREATER_THAN_OR_EQUAL_OPERATOR)), dummyApplication, MATCH },
                { asSet(creationDateFilter(10, GREATER_THAN_OPERATOR)), dummyApplication, NO_MATCH },
                { asSet(creationDateFilter(9, GREATER_THAN_OPERATOR)), dummyApplication, MATCH },

                // Last Modified Date
                { asSet(lastModifiedDateFilter(101, LESS_THAN_OPERATOR)), dummyApplication, MATCH },
                { asSet(lastModifiedDateFilter(100, LESS_THAN_OPERATOR)), dummyApplication, NO_MATCH },
                { asSet(lastModifiedDateFilter(100, LESS_THAN_OR_EQUAL_OPERATOR)), dummyApplication, MATCH },
                { asSet(lastModifiedDateFilter(100, EQUALS_OPERATOR)), dummyApplication, MATCH },
                { asSet(lastModifiedDateFilter(100, GREATER_THAN_OR_EQUAL_OPERATOR)), dummyApplication, MATCH },
                { asSet(lastModifiedDateFilter(100, GREATER_THAN_OPERATOR)), dummyApplication, NO_MATCH },
                { asSet(lastModifiedDateFilter(99, GREATER_THAN_OPERATOR)), dummyApplication, MATCH },

        };
    }

    @Test(dataProvider = "testMatchData")
    public void testMatch(Set<SearchFilter> searchFilters, Application application, boolean expectedMatchResult) {
        assertThat(ApplicationManager.match(searchFilters, application)).isEqualTo(expectedMatchResult);
    }

    private Application application(String name) throws IllegalAccessException, InstantiationException {
        final ApplicationType dummyType =
                new ApplicationType("DummyType", Collections.<String, Boolean>emptyMap(), null, null, null);

        return new Application("/", name, dummyType);
    }

    private SearchFilter nameFilter(String name) {
        return new SearchFilter(NAME_SEARCH_ATTRIBUTE, name);
    }

    private SearchFilter descriptionFilter(String description) {
        return new SearchFilter(DESCRIPTION_SEARCH_ATTRIBUTE, description);
    }

    private SearchFilter createdByFilter(String description) {
        return new SearchFilter(CREATED_BY_SEARCH_ATTRIBUTE, description);
    }

    private SearchFilter creationDateFilter(long date, SearchFilter.Operator filterOperator) {
        return new SearchFilter(CREATION_DATE_SEARCH_ATTRIBUTE, date, filterOperator);
    }

    private SearchFilter lastModifiedByFilter(String description) {
        return new SearchFilter(LAST_MODIFIED_BY_SEARCH_ATTRIBUTE, description);
    }

    private SearchFilter lastModifiedDateFilter(long date, SearchFilter.Operator filterOperator) {
        return new SearchFilter(LAST_MODIFIED_DATE_SEARCH_ATTRIBUTE, date, filterOperator);
    }

}
