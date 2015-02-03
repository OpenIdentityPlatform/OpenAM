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

package org.forgerock.openam.sts.publish.config;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.sun.identity.sm.ServiceListener;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.DeploymentPathNormalization;
import org.forgerock.openam.sts.DeploymentPathNormalizationImpl;
import org.forgerock.openam.sts.InstanceConfigMarshaller;
import org.forgerock.openam.sts.RestSTSInstanceConfigMarshaller;
import org.forgerock.openam.sts.SoapSTSInstanceConfigMarshaller;
import org.forgerock.openam.sts.publish.STSInstanceConfigStore;
import org.forgerock.openam.sts.publish.rest.RestSTSInstanceConfigStore;
import org.forgerock.openam.sts.publish.rest.RestSTSInstancePublisher;
import org.forgerock.openam.sts.publish.rest.RestSTSInstancePublisherImpl;
import org.forgerock.openam.sts.publish.rest.RestSTSPublishServiceListener;
import org.forgerock.openam.sts.publish.soap.SoapSTSInstanceConfigStore;
import org.forgerock.openam.sts.publish.soap.SoapSTSInstancePublisher;
import org.forgerock.openam.sts.publish.soap.SoapSTSInstancePublisherImpl;
import org.forgerock.openam.sts.rest.ServiceListenerRegistration;
import org.forgerock.openam.sts.rest.ServiceListenerRegistrationImpl;
import org.forgerock.openam.sts.rest.config.RestSTSInjectorHolder;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class STSPublishModule extends AbstractModule {
    public static final String REST_STS_PUBLISH_LISTENER = "rest_sts_publish_listener";

    @Override
    protected void configure() {
        bind(RestSTSInstancePublisher.class).to(RestSTSInstancePublisherImpl.class).in(Scopes.SINGLETON);
        bind(SoapSTSInstancePublisher.class).to(SoapSTSInstancePublisherImpl.class).in(Scopes.SINGLETON);
        /*
        A binding for the concern of marshalling RestSTSInstanceConfig and SoapSTSInstanceconfig instances to and from an attribute map representation,
        which is necessary for SMS persistence.
         */
        bind(new TypeLiteral<InstanceConfigMarshaller<RestSTSInstanceConfig>>(){}).to(RestSTSInstanceConfigMarshaller.class).in(Scopes.SINGLETON);
        bind(new TypeLiteral<STSInstanceConfigStore<RestSTSInstanceConfig>>(){}).to(RestSTSInstanceConfigStore.class).in(Scopes.SINGLETON);
        bind(new TypeLiteral<InstanceConfigMarshaller<SoapSTSInstanceConfig>>(){}).to(SoapSTSInstanceConfigMarshaller.class).in(Scopes.SINGLETON);
        bind(new TypeLiteral<STSInstanceConfigStore<SoapSTSInstanceConfig>>(){}).to(SoapSTSInstanceConfigStore.class).in(Scopes.SINGLETON);

        /*
        Bind the class which encapsulates ServiceListener registration so that the RestSTSInstancePublisherImpl can
        invoke it to register a ServiceListener to add rest-sts-instances, published at another server in a site deployment,
        the CREST rest-sts-instance router.
         */
        bind(ServiceListenerRegistration.class).to(ServiceListenerRegistrationImpl.class).in(Scopes.SINGLETON);

        /*
        Bind the ServiceListener injected into the RestSTSInstancePublisher, which will be registered with the
        ServiceListenerRegistration by the RestSTSInstancePublisher so that it can hang rest-sts instances published to
        another server in a site deployment to the CREST rest-sts-instance router.
         */
        bind(ServiceListener.class).annotatedWith(Names.named(REST_STS_PUBLISH_LISTENER))
                .to(RestSTSPublishServiceListener.class).in(Scopes.SINGLETON);

        /*
        The RestSTSPublishServiceRequestHandler uses the RestRealmValidator to insure that invocation-specified realms do exist
         */
        bind(RestRealmValidator.class);

        /*
        The Rest/Soap-STSInstancePublisherImpl classes use this class to normalize the sts instance's deployment path
        prior to sms persistence.
         */
        bind(DeploymentPathNormalization.class).to(DeploymentPathNormalizationImpl.class);
    }

    /*
        Provides the router corresponding to the rest sts so that published mutations can affect the crest router. In other
        words, the RestSTSInstancePublisherImpl will must mutate a CREST router corresponding to the rest-sts. The router
        instance is created in the RestSTSModule, and used as the basis for the ConnectionFactory serving request to published
        rest-sts instances. This same router instance must be used by the publish service to expose newly-created rest-sts instances.
     */
    @Provides
    Router getRouter() {
        return RestSTSInjectorHolder.getInstance(Key.get(Router.class));
    }

    @Provides
    Logger getSlf4jLogger() {
        return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
    }

}
