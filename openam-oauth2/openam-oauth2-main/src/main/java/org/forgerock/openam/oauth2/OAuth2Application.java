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
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2;

import java.net.URI;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.internal.UserIdentityVerifier;
import org.forgerock.openam.oauth2.provider.impl.ClientVerifierImpl;
import org.forgerock.openam.oauth2.store.impl.DefaultOAuthTokenStoreImpl;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.provider.*;
import org.forgerock.restlet.ext.openam.OpenAMParameters;
import org.forgerock.restlet.ext.openam.internal.OpenAMServerAuthorizer;
import org.forgerock.restlet.ext.openam.server.OpenAMServletAuthenticator;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.routing.Router;
import org.restlet.security.Verifier;

/**
 * Sets up the OAuth 2 provider end points and their handlers
 */
public class OAuth2Application extends Application {

    private URI redirectURI = null;

    public OAuth2Application(){
        getMetadataService().setEnabled(true);
        getMetadataService().setDefaultMediaType(MediaType.APPLICATION_JSON);
        setStatusService(new OAuth2StatusService());
    }

    @Override
    public Restlet createInboundRoot() {
        Router root = new Router(getContext());

        //default route goes to the flows
        root.attachDefault(activate());

        // Add TokenInfo Resource
        OAuth2Utils.setTokenStore(getTokenStore(), getContext());

        //go to token info endpoint
        root.attach(OAuth2Utils.getTokenInfoPath(getContext()), ValidationServerResource.class);

        //go to register client endpoint
        root.attach("/register_client", RegisterClient.class);

        return root;
    }

    /**
     * Setups OAuth2 paths and handlers
     * 
     * @return a Restlet of the endpoints and their handlers
     */
    public Restlet activate() {
        Context childContext = getContext().createChildContext();
        Router root = new Router(childContext);
        
        OpenAMParameters parameters = new OpenAMParameters();
        OpenAMServletAuthenticator authenticator =
                new OpenAMServletAuthenticator(childContext, parameters);
        // This endpoint protected by OpenAM Filter
        root.attach(OAuth2Utils.getAuthorizePath(childContext), authenticator);

        OpenAMServerAuthorizer authorizer = new OpenAMServerAuthorizer();
        authenticator.setNext(authorizer);

        // Define Authorization Endpoint
        OAuth2FlowFinder finder =
                new OAuth2FlowFinder(childContext, OAuth2Constants.EndpointType.AUTHORIZATION_ENDPOINT)
                        .supportAuthorizationCode().supportClientCredentials().supportImplicit()
                        .supportPassword();
        authorizer.setNext(finder);

        //TODO client authentication needs to be done in the grant code
        ClientAuthenticationFilter filter = new ClientAuthenticationFilter(childContext);
        // Try to authenticate the client The verifier MUST set
        filter.setVerifier(getClientVerifier());
        root.attach(OAuth2Utils.getAccessTokenPath(childContext), filter);

        // Define Token Endpoint
        finder =
                new OAuth2FlowFinder(childContext, OAuth2Constants.EndpointType.TOKEN_ENDPOINT)
                        .supportAuthorizationCode().supportClientCredentials().supportImplicit()
                        .supportPassword().supportSAML20();
        filter.setNext(finder);

        // Configure context
        childContext.setDefaultVerifier(getUserVerifier());
        OAuth2Utils.setClientVerifier(getClientVerifier(), childContext);
        OAuth2Utils.setTokenStore(getTokenStore(), childContext);
        OAuth2Utils.setContextRealm("/", childContext);

        return root;
    }

    /**
     * Creates a new client verifier
     * 
     * @return ClientVerifierImpl
     *              A client verifier
     */
    public org.forgerock.openam.oauth2.provider.ClientVerifier getClientVerifier() {
        return new ClientVerifierImpl();
    }

    /**
     * Creates a new user verifier
     * 
     * @return UserIdentityVerifier
     *              A new UserVerifier
     */
    public Verifier getUserVerifier() {
        return new UserIdentityVerifier();
    }

    /**
     * Gets the current token store or creates a new one if it doesn't exist
     * 
     * @return OAuthTokenStore
     *              A new token store.
     */
    public org.forgerock.openam.oauth2.provider.OAuth2TokenStore getTokenStore() {
        return new DefaultOAuthTokenStoreImpl();
    }

}
