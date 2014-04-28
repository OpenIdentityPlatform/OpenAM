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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.server;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.ScopeValidatorImpl;
import org.forgerock.oauth2.core.AuthorizationCodeResponseTypeHandler;
import org.forgerock.oauth2.core.TokenResponseTypeHandler;
import org.forgerock.openidconnect.IdTokenResponseTypeHandlerImpl;

import javax.inject.Singleton;

import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * @since 12.0.0
 */
@Singleton
public class ConfigurationResource implements SingletonResourceProvider {

    public static ConnectionFactory getConnectionFactory() {
        final Router router = new Router();
        router.addRoute("/configuration", InjectorHolder.getInstance(ConfigurationResource.class));
        return Resources.newInternalConnectionFactory(router);
    }

    public JsonValue configuration = json(object(
            field("allowedResponseTypes", array(
                    object(field("responseType", "code"), field("handler", AuthorizationCodeResponseTypeHandler.class.getCanonicalName())),
                    object(field("responseType", "id_token"), field("handler", IdTokenResponseTypeHandlerImpl.class.getCanonicalName())),
                    object(field("responseType", "token"), field("handler", TokenResponseTypeHandler.class.getCanonicalName()))
            )),
            field("authorizationCodeLifetime", 60L),
            field("accessTokenLifetime", 60L),
            field("openIdTokenLifetime", 60L),
            field("refreshTokenLifetime", 60L),
            field("supportedOpenIdTokenSigningAlgorithms", array("HS256")),
            field("jwksUri", null),
            field("supportedSubjectTypes", array()),
            field("supportedClaims", array("phone", "email", "address", "openid", "profile")),
            field("issueRefreshTokens", false),
            field("keystore", object(
                    field("keystorePath", "/Users/Phill/ForgeRockDev/servers/apache-tomcat-7.0.32/webapps/oauth2/WEB-INF/classes/keystore.jks"),
                    field("keystoreType", "JKS"),
                    field("keystorePassword", "password"),
                    field("keyAlias", "jwt-test-ks"),
                    field("keyPassword", "password")
            )),
            field("scopeValidator", ScopeValidatorImpl.class.getName())
    )) ;

    public synchronized JsonValue getConfiguration() {
        return configuration;
    }

    public synchronized void setConfiguration(final JsonValue configuration) {
        this.configuration = configuration;
    }

    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceException.getException(ResourceException.NOT_SUPPORTED));
    }

    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        final JsonValue config = getConfiguration();
        handler.handleResult(new Resource("configuration", config.hashCode() + "", config));
    }

    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        final JsonValue config = request.getContent();
        setConfiguration(config);
        handler.handleResult(new Resource("configuration", config.hashCode() + "", config));
    }
}
