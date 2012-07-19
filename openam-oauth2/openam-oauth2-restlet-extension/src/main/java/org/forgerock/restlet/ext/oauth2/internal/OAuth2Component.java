/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.restlet.ext.oauth2.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.forgerock.restlet.ext.oauth2.OAuth2;
import org.forgerock.restlet.ext.oauth2.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.provider.ClientAuthenticationFilter;
import org.forgerock.restlet.ext.oauth2.provider.ClientVerifier;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2FlowFinder;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2Provider;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2TokenStore;
import org.forgerock.restlet.ext.oauth2.representation.ClassDirectoryServerResource;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;

/**
 * This class can initialise the OAuth2 Endpoint. IT can be a Spring Bean or an
 * OSGi component
 * 
 * @author $author$
 * @version $Revision$ $Date$
 */
public class OAuth2Component {

    private OAuth2Provider provider = null;

    private Map<String, Object> configuration = null;

    private ClientVerifier clientVerifier;

    private Verifier userVerifier;

    protected OAuth2TokenStore tokenStore = null;

    private String realm = null;

    private Logger logger = null;

    public OAuth2Provider getProvider() {
        return provider;
    }

    public void setProvider(OAuth2Provider provider) {
        this.provider = provider;
    }

    public Map<String, Object> getConfiguration() {
        if (null == configuration) {
            configuration = new HashMap<String, Object>();
        }
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public ClientVerifier getClientVerifier() {
        return clientVerifier;
    }

    public void setClientVerifier(ClientVerifier clientVerifier) {
        this.clientVerifier = clientVerifier;
    }

    public Verifier getUserVerifier() {
        return userVerifier;
    }

    public void setUserVerifier(Verifier userVerifier) {
        this.userVerifier = userVerifier;
    }

    public OAuth2TokenStore getTokenStore() {
        return tokenStore;
    }

    public void setTokenStore(OAuth2TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public Restlet activate() {
        logger = provider.getContext().getLogger();
        Context childContext = getProvider().getContext().createChildContext();
        Router root = new Router(childContext);

        // Define Resources directory
        Directory directory = new Directory(root.getContext(), "clap:///resources");
        directory.setTargetClass(ClassDirectoryServerResource.class);
        root.attach("/resources", directory);

        // Define Authorization Endpoint
        OAuth2FlowFinder finder =
                new OAuth2FlowFinder(childContext, OAuth2.EndpointType.AUTHORIZATION_ENDPOINT)
                        .supportAuthorizationCode().supportClientCredentials().supportImplicit()
                        .supportPassword();
        ChallengeAuthenticator au =
                new ChallengeAuthenticator(childContext, ChallengeScheme.HTTP_BASIC, "realm");
        au.setVerifier(getUserVerifier());
        au.setNext(finder);

        // This endpoint protected by OpenAM Filter
        root.attach(OAuth2Utils.getAuthorizePath(childContext), au);

        // Define Token Endpoint
        finder =
                new OAuth2FlowFinder(childContext, OAuth2.EndpointType.TOKEN_ENDPOINT)
                        .supportAuthorizationCode().supportClientCredentials().supportImplicit()
                        .supportPassword();
        // Try to authenticate the client The verifier MUST set
        ClientAuthenticationFilter filter = new ClientAuthenticationFilter(childContext);
        filter.setVerifier(clientVerifier);
        filter.setNext(finder);
        root.attach(OAuth2Utils.getAccessTokenPath(childContext), filter);

        if (getConfiguration().get(OAuth2.Custom.REALM) instanceof String) {
            realm = (String) getConfiguration().get(OAuth2.Custom.REALM);
            realm = OAuth2Utils.isNotBlank(realm) ? realm : null;
        }

        // Configure context
        childContext.setDefaultVerifier(userVerifier);
        OAuth2Utils.setClientVerifier(clientVerifier, childContext);
        OAuth2Utils.setTokenStore(tokenStore, childContext);
        OAuth2Utils.setContextRealm(realm, childContext);

        if (null != realm ? provider.attachRealm(realm, root) : provider.attachDefaultRealm(root)) {
            logger.fine("Realm attached");
        }
        return root;
    }

    public void deactivate() {
        if (null != realm) {
            provider.detachRealm(realm);
        } else {
            provider.detachDefaultRealm();
        }
        logger.fine("Realm detached");
    }

    // Null-Safe logger example
    /*
     * protected Logger getLogger(Context context) { Handler handler = new
     * Handler(context.getLogger()); Class[] interfacesArray = new
     * Class[]{Logger.class}; return (Logger)
     * Proxy.newProxyInstance(org.restlet.
     * engine.Engine.getInstance().getClassLoader(), interfacesArray, handler);
     * }
     * 
     * class Handler implements InvocationHandler { public Logger logger;
     * 
     * public Handler(Logger sum) { this.logger = sum; }
     * 
     * public Object invoke(Object proxy, Method method, Object[] args) throws
     * Throwable { if (null != logger) { return method.invoke(logger, args); }
     * else { return null; } } }
     */
}
