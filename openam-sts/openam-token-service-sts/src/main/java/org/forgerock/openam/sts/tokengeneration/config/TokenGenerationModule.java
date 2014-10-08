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
import com.google.inject.name.Names;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshaller;
import org.forgerock.openam.sts.XMLUtilities;
import org.forgerock.openam.sts.XMLUtilitiesImpl;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.forgerock.openam.sts.rest.ServiceListenerRegistrationImpl;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.marshal.RestSTSInstanceConfigMapMarshaller;
import org.forgerock.openam.sts.rest.publish.RestSTSInstanceConfigStore;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceState;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceStateFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceStateFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.RestSTSInstanceStateServiceListener;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGeneration;
import org.forgerock.openam.sts.tokengeneration.saml2.SAML2TokenGenerationImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.SSOTokenIdentity;
import org.forgerock.openam.sts.tokengeneration.saml2.SSOTokenIdentityImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.STSInstanceStateProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.StatementProvider;
import org.forgerock.openam.sts.tokengeneration.saml2.StatementProviderImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.KeyInfoFactoryImpl;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProviderFactory;
import org.forgerock.openam.sts.tokengeneration.saml2.xmlsig.STSKeyProviderFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the bindings which specify the functionality of the TokenGenerationService.
 */
public class TokenGenerationModule extends AbstractModule {
    public static final String REST_STS_INSTANCE_STATE_LISTENER = "rest_sts_instance_state_listener";
    @Override
    protected void configure() {
        bind(SAML2TokenGeneration.class).to(SAML2TokenGenerationImpl.class);
        bind(StatementProvider.class).to(StatementProviderImpl.class);
        bind(new TypeLiteral<STSInstanceStateProvider<RestSTSInstanceState>>(){}).to(RestSTSInstanceStateProvider.class)
                .in(Scopes.SINGLETON);
        /*
        Once the TokenGenerationService gets called by the SOAP STS, I will need to bind a
        STSInstanceConfigPersister<SoapSTSInstanceConfig> class.
         */
        bind(new TypeLiteral<STSInstanceConfigStore<RestSTSInstanceConfig>>(){}).to(RestSTSInstanceConfigStore.class)
                .in(Scopes.SINGLETON);
        bind(new TypeLiteral<MapMarshaller<RestSTSInstanceConfig>>() {}).to(RestSTSInstanceConfigMapMarshaller.class);

        bind(RestSTSInstanceStateFactory.class).to(RestSTSInstanceStateFactoryImpl.class);
        bind(STSKeyProviderFactory.class).to(STSKeyProviderFactoryImpl.class);
        bind(KeyInfoFactory.class).to(KeyInfoFactoryImpl.class);
        bind(XMLUtilities.class).to(XMLUtilitiesImpl.class);
        bind(SSOTokenIdentity.class).to(SSOTokenIdentityImpl.class);

        /*
        Bind the class which encapsulates ServiceListener registration so that the RestSTSInstanceStateProvider can
        invoke it to register a ServiceListener to remove cached RestSTSInstanceState instances.
         */
        bind(ServiceListenerRegistration.class).to(ServiceListenerRegistrationImpl.class).in(Scopes.SINGLETON);

        /*
        Bind the ServiceListener injected into the RestSTSInstanceStateProvider, which will be registered with the
        ServiceListenerRegistration by the RestSTSInstanceStateProvider so that its cache of RestSTSInstanceState entries
        can be invalidated when the service is changed. The RestSTSInstanceStateServiceListener class will be injected
        with the RestSTSInstanceConfigPersister singleton, and use the STSInstanceConfigPersister interface to invalidate
        the appropriate cache entries.
         */
        bind(ServiceListener.class).annotatedWith(Names.named(REST_STS_INSTANCE_STATE_LISTENER))
                .to(RestSTSInstanceStateServiceListener.class).in(Scopes.SINGLETON);
    }

    @Provides
    Logger getSlf4jLogger() {
        return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
    }

}
