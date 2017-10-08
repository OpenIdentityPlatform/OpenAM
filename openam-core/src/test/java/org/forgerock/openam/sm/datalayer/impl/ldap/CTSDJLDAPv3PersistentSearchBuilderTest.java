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
package org.forgerock.openam.sm.datalayer.impl.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.SearchScope;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CTSDJLDAPv3PersistentSearchBuilderTest {

    ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
    CTSDJLDAPv3PersistentSearchBuilder builder;

    @BeforeMethod
    public void theSetup() { //you need this
        builder = new CTSDJLDAPv3PersistentSearchBuilder(mockConnectionFactory);
    }

    public void shouldBuildWIthValidConfig() {
        //given
        builder.withSearchBaseDN(DN.rootDN())
                .withSearchFilter(Filter.alwaysTrue())
                .withRetry(1)
                .withSearchScope(SearchScope.WHOLE_SUBTREE);

        //when
        CTSDJLDAPv3PersistentSearch product = builder.build();

        //then
        assertThat(product).isNotNull();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldFailOnInvalidConnectionFactory() {
        //given

        //when
        builder = new CTSDJLDAPv3PersistentSearchBuilder(null);

        //then
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldFailOnInvalidTimeout() {
        //given
        builder.withSearchBaseDN(DN.rootDN())
                .withSearchFilter(Filter.alwaysTrue())
                .withRetry(0)
                .withSearchScope(SearchScope.WHOLE_SUBTREE);

        //when
        builder.build();

        //then
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldFailOnInvalidSearchScope() {
        //given
        builder.withSearchBaseDN(DN.rootDN())
                .withSearchFilter(Filter.alwaysTrue())
                .withRetry(1)
                .withSearchScope(null);

        //when
        builder.build();

        //then
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldFailOnInvalidFilter() {
        //given
        builder.withSearchBaseDN(DN.rootDN())
                .withSearchFilter(null)
                .withRetry(1)
                .withSearchScope(SearchScope.WHOLE_SUBTREE);

        //when
        builder.build();

        //then

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldFailOnInvalidSearchBase() {
        //given
        builder.withSearchBaseDN(null)
                .withSearchFilter(Filter.alwaysTrue())
                .withRetry(1)
                .withSearchScope(SearchScope.WHOLE_SUBTREE);

        //when
        builder.build();

        //then

    }

}
