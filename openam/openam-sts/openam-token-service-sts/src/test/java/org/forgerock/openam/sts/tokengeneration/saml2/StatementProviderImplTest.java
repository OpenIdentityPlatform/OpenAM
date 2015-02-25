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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.saml2;

import static org.testng.Assert.assertTrue;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.config.user.SAML2Config;
import org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultConditionsProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactoryImpl;
import org.testng.annotations.Test;

public class StatementProviderImplTest {
    class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(KeyInfoFactory.class).to(KeyInfoFactoryImpl.class);
            bind(StatementProvider.class).to(StatementProviderImpl.class);
            bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
        }
    }

    @Test
    public void testCustomProvider() throws TokenCreationException {
        SAML2Config config = SAML2Config.builder()
                            .customConditionsProviderClassName("org.forgerock.openam.sts.tokengeneration.saml2.statements.DefaultConditionsProvider")
                            .spEntityId("http://host.com/sp/entity/id")
                            .build();
        StatementProvider statementProvider = Guice.createInjector(new MyModule()).getInstance(StatementProvider.class);
        assertTrue(statementProvider.getConditionsProvider(config) instanceof DefaultConditionsProvider);
    }

    @Test(expectedExceptions = TokenCreationException.class)
    public void testSpeciousProvider() throws TokenCreationException {
        SAML2Config config = SAML2Config.builder()
                .customConditionsProviderClassName("faux_class_name")
                .spEntityId("http://host.com/sp/entity/id")
                .build();
        StatementProvider statementProvider = Guice.createInjector(new MyModule()).getInstance(StatementProvider.class);
        statementProvider.getConditionsProvider(config);
    }
}