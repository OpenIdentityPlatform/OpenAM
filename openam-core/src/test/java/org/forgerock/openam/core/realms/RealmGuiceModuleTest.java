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

package org.forgerock.openam.core.realms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.security.PrivilegedAction;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.OrganizationConfigManagerFactory;
import org.forgerock.openam.core.CoreWrapper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class RealmGuiceModuleTest {

    @AfterMethod
    public void tearDown() throws Exception {
        // RealmGuiceModule injects the Realm class, so put back the test doubles the other realm tests expect
        RealmTestHelper realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldProvideOneRealmDnsAliasesInstance() {
        Injector injector = Guice.createInjector(new RealmGuiceModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(SSOToken.class).annotatedWith(Names.named("AdminToken")).toInstance(mock(SSOToken.class));
                bind(new TypeLiteral<PrivilegedAction<SSOToken>>() { }).toInstance(mock(PrivilegedAction.class));
                bind(OrganizationConfigManagerFactory.class).toInstance(mock(OrganizationConfigManagerFactory.class));
                bind(CoreWrapper.class).toInstance(mock(CoreWrapper.class));
            }
        });

        assertThat(injector.getInstance(RealmDnsAliases.class))
                .isNotNull()
                .isSameAs(injector.getInstance(RealmDnsAliases.class));
    }
}
