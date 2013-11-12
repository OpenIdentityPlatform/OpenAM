/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of company]"
 */

package org.forgerock.openam.oauth2demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.consumer.AccessTokenValidator;
import org.forgerock.restlet.ext.oauth2.consumer.BearerOAuth2Proxy;
import org.forgerock.openam.oauth2.model.BearerToken;
import org.forgerock.restlet.ext.oauth2.consumer.BearerTokenVerifier;
import org.forgerock.restlet.ext.oauth2.consumer.OAuth2Authenticator;
import org.forgerock.restlet.ext.oauth2.internal.DefaultScopeEnroler;
import org.forgerock.restlet.ext.oauth2.provider.ValidationServerResource;
import org.forgerock.restlet.ext.openam.OpenAMParameters;
import org.forgerock.restlet.ext.openam.server.OpenAMServletAuthenticator;
import org.restlet.Application;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.security.Role;
import org.restlet.security.RoleAuthorizer;

import com.iplanet.am.util.SystemProperties;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

/**
 * Sets up the OAuth2DemoApplication
 */
public class OAuth2DemoApplication extends Application {

    public static final String OAUTH2_ENDPOINT_AUTHORIZE =
            "org.forgerock.openam.oauth2.endpoint.authorize";
    public static final String OAUTH2_ENDPOINT_ACCESS_TOKEN =
            "org.forgerock.openam.oauth2.endpoint.access_token";
    public static final String OAUTH2_ENDPOINT_TOKENINFO =
            "org.forgerock.openam.oauth2.endpoint.tokeninfo";
    public static final String OAUTH2_CLIENT_ID = "org.forgerock.openam.oauth2.client_id";
    public static final String OAUTH2_CLIENT_SECRET = "org.forgerock.openam.oauth2.client_secret";
    public static final String OAUTH2_USERNAME = "org.forgerock.openam.oauth2.username";
    public static final String OAUTH2_PASSWORD = "org.forgerock.openam.oauth2.password";
    public static final String OAUTH2_ENDPOINT_REDIRECTION =
            "org.forgerock.openam.oauth2.endpoint.redirection";
    /**
     * The Freemarker's configuration.
     */
    private Configuration configuration;

    @Override
    public Restlet createInboundRoot() {
        Router root = new Router(getContext());

        String authorizeEndpoint = SystemProperties.get(OAUTH2_ENDPOINT_AUTHORIZE);
        if (OAuth2Utils.isBlank(authorizeEndpoint)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_ENDPOINT_AUTHORIZE);
        }
        String redirectionEndpoint = SystemProperties.get(OAUTH2_ENDPOINT_REDIRECTION);
        if (OAuth2Utils.isBlank(redirectionEndpoint)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_ENDPOINT_REDIRECTION);
        }
        String accessTokenEndpoint = SystemProperties.get(OAUTH2_ENDPOINT_ACCESS_TOKEN);
        if (OAuth2Utils.isBlank(accessTokenEndpoint)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_ENDPOINT_ACCESS_TOKEN);
        }
        String tokenInfoEndpoint = SystemProperties.get(OAUTH2_ENDPOINT_TOKENINFO);
        if (OAuth2Utils.isBlank(tokenInfoEndpoint)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_ENDPOINT_TOKENINFO);
        }

        String clientId = SystemProperties.get(OAUTH2_CLIENT_ID);
        if (OAuth2Utils.isBlank(clientId)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_CLIENT_ID);
        }
        String clientSecret = SystemProperties.get(OAUTH2_CLIENT_SECRET);
        if (OAuth2Utils.isBlank(clientSecret)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_CLIENT_SECRET);
        }
        String username = SystemProperties.get(OAUTH2_USERNAME);
        if (OAuth2Utils.isBlank(username)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_USERNAME);
        }
        String password = SystemProperties.get(OAUTH2_PASSWORD);
        if (OAuth2Utils.isBlank(password)) {
            throw new ResourceException(Status.SERVER_ERROR_INTERNAL, "Missing required AMConfig:"
                    + OAUTH2_PASSWORD);
        }
        String scope = SystemProperties.get("org.forgerock.openam.oauth2.scope");

        String templateDir = "clap:///templates";

        configuration = new Configuration();
        try {
            configuration.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:20, soft:250");
        } catch (TemplateException e) {
            /* ignored */
        }
        configuration.setTemplateLoader(new ContextTemplateLoader(getContext(), templateDir));

        URI current = getCurrentURI();
        getContext().getAttributes().put("org.forgerock.openam.oauth2demo", current);

        BearerOAuth2Proxy auth2Proxy = new BearerOAuth2Proxy(getContext(), null);
        auth2Proxy.pushOAuth2Proxy(getContext());
        auth2Proxy.setAuthorizationEndpoint(new Reference(authorizeEndpoint));
        auth2Proxy.setTokenEndpoint(new Reference(accessTokenEndpoint));
        auth2Proxy.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC, clientId,
                clientSecret.toCharArray()));
        auth2Proxy.setClientCredentials(clientId, clientSecret);
        auth2Proxy.setRedirectionEndpoint(new Reference(URI.create(redirectionEndpoint)));
        auth2Proxy.setResourceOwnerCredentials(username, password);
        auth2Proxy.setScope(OAuth2Utils.split(scope, ","));

        RedirectResource redirectResource =
                new RedirectResource(getContext(), current.resolve("../index.html").toString(),
                        Redirector.MODE_CLIENT_FOUND);
        root.attach("/redirect", redirectResource);

        // Validation Resource
        Reference validationServerRef = new Reference(tokenInfoEndpoint);
        AccessTokenValidator<BearerToken> validator =
                new ValidationServerResource(getContext(), validationServerRef);

        // Use CHALLENGERESPONSE
        BearerTokenVerifier tokenVerifier = new BearerTokenVerifier(validator);
        OAuth2Authenticator authenticator =
                new OAuth2Authenticator(getContext(), null,
                        OAuth2Utils.ParameterLocation.HTTP_HEADER, tokenVerifier);
        authenticator.setEnroler(new DefaultScopeEnroler());
        RoleAuthorizer authorizer = new RoleAuthorizer("RoleAuthorizer1");
        authorizer.setAuthorizedRoles(getAuthorizedRoles());
        authenticator.setNext(authorizer);
        authorizer.setNext(OAuth2TokenResource.class);
        root.attach("/protected/mode1", authenticator);

        // Use PARAMETER
        authenticator =
                new OAuth2Authenticator(getContext(), null,
                        OAuth2Utils.ParameterLocation.HTTP_QUERY, tokenVerifier);
        authenticator.setEnroler(new DefaultScopeEnroler());
        authorizer = new RoleAuthorizer("RoleAuthorizer2");
        authorizer.setAuthorizedRoles(getAuthorizedRoles());
        authenticator.setNext(authorizer);
        authorizer.setNext(OAuth2TokenResource.class);
        root.attach("/protected/mode2", authenticator);

        OpenAMParameters parameters = new OpenAMParameters();
        OpenAMServletAuthenticator amauthenticator =
                new OpenAMServletAuthenticator(getContext(), parameters);
        amauthenticator.setNext(DemoResource.class);
        root.attach("/demo", amauthenticator);
        root.attach("/opendemo", DemoResource.class);

        return root;
    }

    /**
     * Parses the current servlet request and creates a URI
     * 
     * @return URI representing the current servlet request
     */
    protected URI getCurrentURI() {
        Object o = getContext().getAttributes().get(OAuth2DemoApplication.class.getName());
        URI root = null;

        if (o instanceof String) {
            String path = (String) o;
            root = URI.create(path.endsWith("/") ? path : path + "/");
        } else {
            Request request = Request.getCurrent();
            if (null != request) {
                HttpServletRequest servletRequest = ServletUtils.getRequest(request);
                String scheme = servletRequest.getScheme(); // http
                String serverName = servletRequest.getServerName(); // localhost
                int serverPort = servletRequest.getServerPort(); // 8080
                String contextPath = servletRequest.getContextPath(); // /openam
                String servletPath = servletRequest.getServletPath(); // /oauth2demo

                try {
                    root =
                            new URI(scheme, null, serverName, serverPort, contextPath + servletPath
                                    + "/", null, null);

                    // TODO Find a proper solution
                    Client client = new Client(scheme);
                    if (client.isAvailable()) {
                        getConnectorService().getClientProtocols().addAll(client.getProtocols());
                    } else {
                        throw new RuntimeException("Client connector is not available");
                    }

                } catch (URISyntaxException e) {
                    // Should not happen
                }
            }
        }
        if (null == root) {
            throw new RuntimeException("OAuth2DemoApplication can not detect current context");
        }
        return root;
    }

    /**
     * Returns the modifiable list of authorized roles.
     * 
     * @return The modifiable list of authorized roles.
     */
    private List<Role> getAuthorizedRoles() {
        List<Role> authorizedRoles = new ArrayList<Role>(1);
        authorizedRoles.add(new Role("read", ""));
        return authorizedRoles;
    }

    /**
     * Returns the Freemarker's configuration.
     * 
     * @return The Freemarker's configuration.
     */
    public Configuration getConfiguration() {
        return configuration;
    }
}
