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

import com.google.inject.Exposed;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.DefaultHttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionFactory;
import org.forgerock.openam.sts.HttpURLConnectionWrapperFactory;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAgentConfigAccess;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAgentConfigAccessImpl;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAgentCredentialsAccess;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAgentCredentialsAccessImpl;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSLifecycle;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSLifecycleImpl;
import org.forgerock.openam.sts.soap.publish.PublishServiceConsumer;
import org.forgerock.openam.sts.soap.publish.PublishServiceConsumerImpl;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProvider;
import org.forgerock.openam.sts.soap.bootstrap.SoapSTSAccessTokenProviderImpl;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstancePublisher;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstancePublisherImpl;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstanceLifecycleManager;
import org.forgerock.openam.sts.soap.publish.SoapSTSInstanceLifecycleManagerImpl;
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
 * Defines the guice bindings needed by the soap-sts 'framework' - i.e. the elements need to exposed published soap-sts
 * instances as web-services. Also defines bindings common to all soap-sts instances - i.e. version strings, and rest target
 * endpoints. It is a PrivateModule as the guice Injecotr created for each soap-sts instance will be a child of the Injector
 * created by this module, and only some of the global bindings should be exposed to the child Injector.
 */
public class SoapSTSModule extends PrivateModule {
    /*
    Note that the correspondence between these values and the properties defined in config.properties must be maintained,
    as the Names.bindProperties(binder(), properties) call will automatically bind the property values in the properties
    file with @Named annotations corresponding to the keys - and the values defined below are used in the @Named annotations
    governing these bindings.
     */
    public static final String OPENAM_HOME_SERVER_PROPERTY_KEY = "openam_home_server";
    public static final String AM_SESSION_COOKIE_NAME_PROPERTY_KEY = AMSTSConstants.AM_SESSION_COOKIE_NAME;
    public static final String SOAP_STS_AGENT_USERNAME_PROPERTY_KEY = "soap_sts_agent_username";
    public static final String SOAP_STS_AGENT_PASSWORD_PROPERTY_KEY = "soap_sts_agent_password";
    public static final String SOAP_STS_AGENT_ENCRYPTION_KEY_PROPERTY_KEY = "soap_sts_agent_encryption_key";
    public static final String AGENT_REALM = "agent_realm";

    @Override
    protected void configure() {
        bind(SoapSTSInstanceLifecycleManager.class).to(SoapSTSInstanceLifecycleManagerImpl.class).in(Scopes.SINGLETON);
        bind(SoapSTSInstancePublisher.class).to(SoapSTSInstancePublisherImpl.class).in(Scopes.SINGLETON);
        bind(HttpURLConnectionFactory.class).to(DefaultHttpURLConnectionFactory.class).in(Scopes.SINGLETON);
        bind(HttpURLConnectionWrapperFactory.class).in(Scopes.SINGLETON);
        bind(PublishServiceConsumer.class).to(PublishServiceConsumerImpl.class).in(Scopes.SINGLETON);
        bind(AMTokenParser.class).to(AMTokenParserImpl.class).in(Scopes.SINGLETON);
        bind(SoapSTSAccessTokenProvider.class).to(SoapSTSAccessTokenProviderImpl.class);
        // Expose to injectors creating soap-sts instances, as each require access tokens to consume the TGS.
        expose(SoapSTSAccessTokenProvider.class);

        bind(UrlConstituentCatenator.class).to(UrlConstituentCatenatorImpl.class);
        bind(SoapSTSAgentConfigAccess.class).to(SoapSTSAgentConfigAccessImpl.class).in(Scopes.SINGLETON);
        bind(SoapSTSLifecycle.class).to(SoapSTSLifecycleImpl.class).in(Scopes.SINGLETON);
        // Expose as the STSBroker needs to reference this class to kick off the soap-sts bootstrap process.
        expose(SoapSTSLifecycle.class);
        bind(SoapSTSAgentCredentialsAccess.class).to(SoapSTSAgentCredentialsAccessImpl.class).in(Scopes.SINGLETON);
        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(getClass().getResourceAsStream("/config.properties")));
        } catch (Exception e) {
            throw new IllegalStateException("Could not load the /config.properties for the soap-sts: " + e, e);
        }
        Names.bindProperties(binder(), properties);
        /*
        Expose the session cookie name, as this will be required by the instance module for the various rest authN
        and TGS consumers. Note that these consumers have a dependency on a string @Named AMSTSConstants.AM_SESSION_COOKIE_NAME.
        If the name in the soap sts property file changes - i.e. if the SoapSTSModule.AM_SESSION_COOKIE_PROPERTY_NAME_KEY changes
        from the current value of AMSTSConstants.AM_SESSION_COOKIE_NAME, then the SoapSTSInstanceModule will need a
        @Provides method which will reference SoapSTSModule.AM_SESSION_COOKIE_NAME_PROPERTY via the SoapSTSInjectorHolder
        to provide the dependency on a string @Named AMSTSConstants.AM_SESSION_COOKIE_NAME via the
        SoapSTSModule.AM_SESSION_COOKIE_NAME_PROPERTY_KEY value.
         */
        expose(Key.get(String.class, Names.named(SoapSTSModule.AM_SESSION_COOKIE_NAME_PROPERTY_KEY)));
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
    @Exposed
    String restAuthnUriElement() {
        return "/authenticate";
    }

    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_LOGOUT_URI_ELEMENT)
    @Exposed
    String restLogoutUriElement() {
        return "/sessions/?_action=logout";
    }

    @Provides
    @Singleton
    @Named (AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT)
    @Exposed
    String restAMTokenValidationUriElement() {
        return "/users/?_action=idFromSession";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT)
    @Exposed
    String tokenGenerationServiceUriElement() {
        return "/sts-tokengen/issue?_action=issue";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.SOAP_STS_PUBLISH_SERVICE_URI_ELEMENT)
    @Exposed
    String soapSTSPublishUriElement() {
        return "sts-publish/soap";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT)
    @Exposed
    String getJsonRoot() {
        return "/json";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.AGENTS_PROFILE_SERVICE_URI_ELEMENT)
    @Exposed
    String getAgentsProfileServiceUriElement() {
        return "/agents";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_SESSION_SERVICE)
    @Exposed
    String getSessionServiceVersion() {
        return "protocol=1.0, resource=1.1";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_AUTHN_SERVICE)
    @Exposed
    String getAuthNServiceVersion() {
        return "protocol=1.0, resource=2.0";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_TOKEN_GEN_SERVICE)
    @Exposed
    String getTokenGenServiceVersion() {
        return "protocol=1.0, resource=1.0";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_USERS_SERVICE)
    @Exposed
    String getUsersServiceVersion() {
        return "protocol=1.0, resource=2.0";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_SOAP_STS_PUBLISH_SERVICE)
    @Exposed
    String getSoapPublishServiceVersion() {
        return "protocol=1.0, resource=1.0";
    }

    @Provides
    @Singleton
    @Named(AMSTSConstants.CREST_VERSION_AGENTS_PROFILE_SERVICE)
    @Exposed
    String getAgentsProfileServiceVersion() {
        return "protocol=1.0, resource=2.0";
    }

    @Provides
    @Singleton
    @Exposed
    Logger getSlf4jLogger() {
        return LoggerFactory.getLogger(AMSTSConstants.SOAP_STS_DEBUG_ID);
    }
}
