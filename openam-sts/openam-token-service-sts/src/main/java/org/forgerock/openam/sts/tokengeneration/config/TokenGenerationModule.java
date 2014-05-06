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

package org.forgerock.openam.sts.tokengeneration.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.publish.RestSTSInstanceConfigPersister;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceStateFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceStateFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGenerationImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.StatementProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.StatementProviderImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2AssertionSigner;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.SAML2AssertionSignerImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProviderFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProviderFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the bindings which specify the functionality of the TokenGenerationService.
 */
public class TokenGenerationModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SAML2TokenGeneration.class).to(SAML2TokenGenerationImpl.class);
        bind(StatementProvider.class).to(StatementProviderImpl.class);
        bind(new TypeLiteral<STSInstanceStateProvider<RestSTSInstanceState>>(){}).to(RestSTSInstanceStateProvider.class)
                .in(Scopes.SINGLETON);
        bind(SAML2AssertionSigner.class).to(SAML2AssertionSignerImpl.class);
        /*
        Once the TokenGenerationService gets called by the SOAP STS, I will need to bind a
        STSInstanceConfigPersister<SoapSTSInstanceConfig> class.
         */
        bind(new TypeLiteral<STSInstanceConfigPersister<RestSTSInstanceConfig>>(){}).to(RestSTSInstanceConfigPersister.class)
                .in(Scopes.SINGLETON);
        bind(RestSTSInstanceStateFactory.class).to(RestSTSInstanceStateFactoryImpl.class);
        bind(STSKeyProviderFactory.class).to(STSKeyProviderFactoryImpl.class);
    }

    @Provides
    Logger getSlf4jLogger() {
        return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
    }

}
