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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.sts.soap.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.DefaultHttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.soap.publish.PublishServiceAccessTokenProvider;
import org.forgerock.openam.sts.soap.publish.PublishServiceAccessTokenProviderImpl;
import org.forgerock.openam.sts.soap.publish.PublishServiceConsumer;
import org.forgerock.openam.sts.soap.publish.PublishServiceConsumerImpl;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstancePublisher;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstancePublisherImpl;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstanceLifecycleManager;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstanceLifecycleManagerImpl;
import org.forgerock.openam.sts.soap.publish.SoapSTSPublishPoller;
import org.forgerock.openam.sts.soap.publish.SoapSTSPublishPollerImpl;
import org.forgerock.openam.sts.token.AMTokenParser;
import org.forgerock.openam.sts.token.AMTokenParserImpl;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.forgerock.openam.sts.token.UrlConstituentCatenatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Defines the guice bindings common to all soap-sts instances.
 */
public class SoapSTSModule extends AbstractModule {
    /*
    Note that the correspondence between these values and the properties defined in config.properties must be maintained,
    as the Names.bindProperties(binder(), properties) call will automatically bind the property values in the properties
    file with @Named annotations corresponding to the keys - and the values defined below are used in the @Named annotations
    governing these bindings.
     */
    public static final String OPENAM_HOME_SERVER_PROPERTY_KEY = "openam_home_server";
    public static final String PUBLISH_SERVICE_POLL_INTERVAL_PROPERTY_KEY = "sts_publish_poll_interval";
    public static final String AM_SESSION_COOKIE_NAME_PROPERTY_KEY = "am_session_cookie_name";
    public static final String PUBLISH_SERVICE_CONSUMER_USERNAME_PROPERTY_KEY = "publish_service_consumer_username";
    public static final String PUBLISH_SERVICE_CONSUMER_PASSWORD_PROPERTY_KEY = "publish_service_consumer_password";

    @Override
    protected void configure() {
        bind(SoapSTSPublishPoller.class).to(SoapSTSPublishPollerImpl.class);
        bind(SoapSTSInstanceLifecycleManager.class).to(SoapSTSInstanceLifecycleManagerImpl.class);
        bind(SoapSTSInstancePublisher.class).to(SoapSTSInstancePublisherImpl.class);
        /*
        Bind the class responsible for producing HttpURLConnectionWrapper instances, and the HttpURLConnectionFactory it consumes
         */
        bind(HttpURLConnectionFactory.class).to(DefaultHttpURLConnectionFactory.class).in(Scopes.SINGLETON);
        bind(HttpURLConnectionWrapperFactory.class).in(Scopes.SINGLETON);
        bind(PublishServiceConsumer.class).to(PublishServiceConsumerImpl.class).in(Scopes.SINGLETON);
        bind(AMTokenParser.class).to(AMTokenParserImpl.class).in(Scopes.SINGLETON);
        bind(PublishServiceAccessTokenProvider.class).to(PublishServiceAccessTokenProviderImpl.class).in(Scopes.SINGLETON);
        bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);

        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(getClass().getResourceAsStream("/config.properties")));
        } catch (Exception e) {
            throw new IllegalStateException("Could not load the /config.properties for the soap-sts: " + e, e);
        }
        Names.bindProperties(binder(), properties);
    }

    @Provides
    @Singleton
    ScheduledExecutorService getScheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    /*
        The following 6 methods provide the String constants corresponding to relatively static values relating to
        consumption of the OpenAM rest context. This information is necessary for the STS instances to consume this
        context, and is the single point where these values need to be changed.
     */
    @Provides
    @Singleton
    @Named(AMSTSConstants.REST_AUTHN_URI_ELEMENT)
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
    @Named(AMSTSConstants.SOAP_STS_PUBLISH_SERVICE_URI_ELEMENT)
    String soapSTSPublishUriElement() {
        return "sts-publish/soap";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
    String getJsonRoot() {
        return "/json";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE)
    String getSessionServiceVersion() {
        return "protocol=1.0, resource=1.1";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)
    String getAuthNServiceVersion() {
        return "protocol=1.0, resource=2.0";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE)
    String getTokenGenServiceVersion() {
        return "protocol=1.0, resource=1.0";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)
    String getUsersServiceVersion() {
        return "protocol=1.0, resource=2.0";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_SOAP_STS_PUBLISH_SERVICE)
    String getSoapPublishServiceVersion() {
        return "protocol=1.0, resource=1.0";
    }

    @Provides
    @Singleton
    Logger getSlf4jLogger() {
        return LoggerFactory.getLogger(AMSTSConstants.SOAP_STS_DEBUG_ID);
    }


}
