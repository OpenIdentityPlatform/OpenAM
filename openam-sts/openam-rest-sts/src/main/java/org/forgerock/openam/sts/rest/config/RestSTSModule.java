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

package org.forgerock.openam.sts.rest.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.MapMarshaller;
import org.forgerock.openam.sts.publish.STSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;
import org.forgerock.openam.sts.rest.marshal.RestSTSInstanceConfigMapMarshaller;
import org.forgerock.openam.sts.rest.publish.RestSTSInstanceConfigPersister;
import org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisher;
import org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisherImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * This module defines bindings which are common to all STS instances. This includes, for example, state related to
 * publishing Rest STS instances.
 */
public class RestSTSModule extends AbstractModule {
    private final Router router;
    public RestSTSModule() {
        router = new Router();
    }

    @Override
    protected void configure() {
        bind(RestSTSInstancePublisher.class).to(RestSTSInstancePublisherImpl.class).in(Scopes.SINGLETON);
        /*
        A binding for the concern of marshalling RestSTSInstanceConfig instances to and from an attribute map representation,
        which is necessary for SMS persistence.
         */
        bind(new TypeLiteral<MapMarshaller<RestSTSInstanceConfig>>(){}).to(RestSTSInstanceConfigMapMarshaller.class);
        bind(new TypeLiteral<STSInstanceConfigPersister<RestSTSInstanceConfig>>(){}).to(RestSTSInstanceConfigPersister.class).in(Scopes.SINGLETON);

    }

    /*
    Might want to return a dynamic router like the RealmRouterConnectionFactory. For now, take the more static approach.
     */
    @Provides
    @Named(AMSTSConstants.REST_STS_CONNECTION_FACTORY_NAME)
    @Singleton
    ConnectionFactory getConnectionFactory() {
        return Resources.newInternalConnectionFactory(router);
    }

    /*
    Used to obtain the router to publish new Rest STS instances.
     */
    @Provides
    Router getRouter() {
        return router;
    }

    @Provides
    Logger getSlf4jLogger() {
        return LoggerFactory.getLogger(AMSTSConstants.REST_STS_DEBUG_ID);
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.REST_STS_PUBLISH_SERVICE_URI_ELEMENT)
    String getRestSTSPublishServiceUriElement() {
        return "/rest-sts-publish/publish";
    }
    /*
        The following 6 methods provide the String constants corresponding to relatively static values relating to
        consumption of the OpenAM rest context. This information is necessary for the STS instances to consume this
        context, and is the single point where these values need to be changed.
     */
    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_AUTHN_URI_ELEMENT)
    String restAuthnUriElement() {
        return "/authenticate";
    }

    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_LOGOUT_URI_ELEMENT)
    String restLogoutUriElement() {
        return "/sessions/?_action=logout";
    }

    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)
    String restAMTokenValidationUriElement() {
        return "/users/?_action=idFromSession";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)
    String tokenGenerationServiceUriElement() {
        return "/sts-tokengen/issue?_action=issue";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME)
    String getAMSessionCookieName() {
        return SystemPropertiesManager.get(Constants.AM_COOKIE_NAME);
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
    String getJsonRoot() {
        return "/json";
    }
}
