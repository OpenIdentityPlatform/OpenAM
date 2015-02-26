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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.restlet.ext.oauth2.flow;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sun.identity.shared.OAuth2Constants;
import org.forgerock.openam.oauth2.model.*;
import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.consumer.BearerOAuth2Proxy;
import org.forgerock.restlet.ext.oauth2.internal.OAuth2Component;
import org.forgerock.openam.oauth2.provider.ClientVerifier;
import org.forgerock.openam.oauth2.provider.OAuth2Provider;
import org.forgerock.restlet.ext.oauth2.provider.OAuth2RealmRouter;
import org.forgerock.openam.oauth2.provider.OAuth2TokenStore;
import org.forgerock.restlet.ext.oauth2.representation.ClassDirectoryServerResource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.security.MapVerifier;
import org.restlet.security.Verifier;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * @author $author$
 * @version $Revision$ $Date$
 */
@SuppressWarnings(value = { "unchecked" })
public class AbstractFlowTest {
    protected OAuth2Provider pathProvider;
    protected OAuth2Provider queryProvider;
    protected Component component = new Component();
    protected final OAuth2Component realm = new OAuth2Component();

    protected ConcurrentMap<String, AuthorizationCode> authorizationCodeMap =
            new ConcurrentHashMap<String, AuthorizationCode>();
    protected ConcurrentMap<String, CoreToken> accessTokenMap =
            new ConcurrentHashMap<String, CoreToken>();
    protected ConcurrentMap<String, CoreToken> refreshTokenMap =
            new ConcurrentHashMap<String, CoreToken>();

    // protected static String AUTHORIZATION_CODE = "SplxlOBeZQQYbYS6WxSbIA";
    // protected static String ACCESS_TOKEN = "2YotnFZFEjr1zCsicMWpAA";
    // protected static String REFRESH_TOKEN = "tGzv3JOkF0XG5Qx2TlKWIA";

    @BeforeClass
    public void beforeClass() throws Exception {
        component.getClients().add(Protocol.RIAP); // Enable Client connectors
        component.getClients().add(Protocol.FILE); // Enable Client connectors
        component.getClients().add(Protocol.CLAP); // Enable Client connectors
        component.getStatusService().setEnabled(false); // The status service is
                                                        // disabled by default.
        Application application = new Application(component.getContext().createChildContext());
        application.getTunnelService().setQueryTunnel(false); // query string
                                                              // purism

        // Create InboundRoot
        Router root = new Router(application.getContext());
        Directory directory = new Directory(root.getContext(), "clap:///resources");
        directory.setTargetClass(ClassDirectoryServerResource.class);
        root.attach("/resources", directory);

        OAuth2RealmRouter realmRouter = new OAuth2RealmRouter(application.getContext());
        root.attach("/{realm}/oauth2", realmRouter);
        pathProvider = realmRouter;
        realmRouter = new OAuth2RealmRouter(application.getContext());
        root.attach("/oauth2", realmRouter);
        queryProvider = realmRouter;
        application.setInboundRoot(root);

        // Attach to internal routes
        component.getInternalRouter().attach("", application);

        // Activate service
        realm.getConfiguration().put(OAuth2Constants.Custom.REALM, "test");
        realm.setClientVerifier(mock(ClientVerifier.class));
        realm.setTokenStore(mock(OAuth2TokenStore.class));
        realm.setUserVerifier(getUserVerifier());
        realm.setProvider(pathProvider);
        realm.activate();
        realm.setProvider(queryProvider);
        realm.activate();

        // Mock
        // SessionClient sessionClient = mock(SessionClient.class);
        // when(sessionClient.getClientId()).thenReturn("cid");

        ClientApplication client = mock(ClientApplication.class);
        when(client.getClientId()).thenReturn("cid");
        when(client.getAccessTokenType()).thenReturn(OAuth2Constants.Bearer.BEARER);
        when(client.getAllowedGrantScopes()).thenReturn(OAuth2Utils.split("read write", null));
        when(client.getDefaultGrantScopes()).thenReturn(OAuth2Utils.split("read", null));
        Set<URI> redirectionURIs = new HashSet<URI>(1);
        redirectionURIs.add(URI.create("http://localhost:8080/oauth2/cb"));
        when(client.getRedirectionURIs()).thenReturn(redirectionURIs);

        //when(realm.getClientVerifier().verify(anyString(), anyString())).thenReturn(client);
        //when(realm.getClientVerifier().verify(any(ChallengeResponse.class))).thenReturn(client);
        //when(realm.getClientVerifier().findClient(matches("cid"))).thenReturn(client);

        // Mock Token Store

        // Mock createAuthorizationCode
        when(
                realm.getTokenStore().createAuthorizationCode(anySet(), anyString(), anyString(),
                        any(SessionClient.class))).then(new Answer<AuthorizationCode>() {
            @Override
            public AuthorizationCode answer(InvocationOnMock invocation) throws Throwable {
                AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
                String code = UUID.randomUUID().toString();

                String realm = (String) invocation.getArguments()[2];
                if (realm.indexOf("|") > 0) {
                    String[] r = realm.split("|");
                    when(authorizationCode.getRealm()).thenReturn(r[0]);
                    when(authorizationCode.isTokenIssued()).thenReturn(r[1].contains("I"));
                    when(authorizationCode.isExpired()).thenReturn(r[1].contains("E"));
                    if (r.length == 3) {
                        Long life = Long.getLong(r[2]);
                        when(authorizationCode.getExpireTime()).thenReturn(
                                System.currentTimeMillis() + life);
                    } else {
                        when(authorizationCode.getExpireTime()).thenReturn(
                                System.currentTimeMillis() + 600000L);
                    }
                } else {
                    when(authorizationCode.isTokenIssued()).thenReturn(false);
                    when(authorizationCode.getRealm()).thenReturn(realm);
                    when(authorizationCode.getExpireTime()).thenReturn(
                            System.currentTimeMillis() + 600000L);
                    when(authorizationCode.isExpired()).thenReturn(false);
                }
                when(authorizationCode.getScope()).thenReturn(
                        (Set<String>) invocation.getArguments()[0]);
                when(authorizationCode.getUserID()).thenReturn(
                        (String) invocation.getArguments()[1]);
                when(authorizationCode.getClient()).thenReturn(
                        (SessionClient) invocation.getArguments()[3]);
                when(authorizationCode.getTokenID()).thenReturn(code);

                authorizationCodeMap.put(code, authorizationCode);

                return authorizationCode;
            }
        });

        when(realm.getTokenStore().readAuthorizationCode(anyString())).then(
                new Answer<AuthorizationCode>() {

                    /**
                     * @param invocation
                     *            the invocation on the mock.
                     * @return the value to be returned
                     * @throws Throwable
                     *             the throwable to be thrown
                     */
                    @Override
                    public AuthorizationCode answer(InvocationOnMock invocation) throws Throwable {
                        return authorizationCodeMap.get(invocation.getArguments()[0]);
                    }
        });

        when(realm.getTokenStore().readRefreshToken(anyString())).then(new Answer<CoreToken>() {

            /**
             * @param invocation
             *            the invocation on the mock.
             * @return the value to be returned
             * @throws Throwable
             *             the throwable to be thrown
             */
            @Override
            public CoreToken answer(InvocationOnMock invocation) throws Throwable {
                return refreshTokenMap.get(invocation.getArguments()[0]);
            }
        });

        // Mock createRefreshToken
        when(
                realm.getTokenStore().createRefreshToken(anySet(), anyString(), anyString(),
                        anyString(), any(String.class))).then(new Answer<CoreToken>() {
            @Override
            public CoreToken answer(InvocationOnMock invocation) throws Throwable {
                CoreToken token = mock(CoreToken.class);
                String code = UUID.randomUUID().toString();

                when(token.getScope()).thenReturn((Set<String>) invocation.getArguments()[0]);
                when(token.getRealm()).thenReturn((String) invocation.getArguments()[1]);
                when(token.getUserID()).thenReturn((String) invocation.getArguments()[2]);
                // TODO We don't have redirect URI
                SessionClient sc = mock(SessionClient.class);
                when(sc.getClientId()).thenReturn((String) invocation.getArguments()[3]);
                //when(token.get("scope")).thenReturn(sc);
                when(token.getExpireTime()).thenReturn(System.currentTimeMillis() + 600000L);
                when(token.isExpired()).thenReturn(false);
                refreshTokenMap.put(code, token);

                return token;
            }
        });

        // Mock createAccessToken - Refresh Token
        when(
                realm.getTokenStore().createAccessToken(eq(OAuth2Constants.Bearer.BEARER), anySet(),
                        any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class))).then(new Answer<CoreToken>() {
            @Override
            public CoreToken answer(InvocationOnMock invocation) throws Throwable {
                CoreToken refreshToken = (CoreToken) invocation.getArguments()[2];

                CoreToken accessToken = mock(CoreToken.class);
                String token = UUID.randomUUID().toString();

                Map<String, Object> tokenMap = new HashMap<String, Object>();
                tokenMap.put(OAuth2Constants.Params.ACCESS_TOKEN, token);
                tokenMap.put(OAuth2Constants.Params.REFRESH_TOKEN, refreshToken.getParameter(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN));
                tokenMap.put(OAuth2Constants.Params.TOKEN_TYPE, OAuth2Constants.Bearer.BEARER);
                tokenMap.put(OAuth2Constants.Params.EXPIRES_IN, 3600);

                when(accessToken.convertToMap()).thenReturn(tokenMap);
                when(accessToken.getScope()).thenReturn((Set<String>) invocation.getArguments()[1]);
                String value = refreshToken.getRealm();
                when(accessToken.getRealm()).thenReturn(value);
                value = refreshToken.getUserID();
                when(accessToken.getUserID()).thenReturn(value);
                when(accessToken.getTokenID()).thenReturn(token);
                when(accessToken.getExpireTime()).thenReturn(System.currentTimeMillis() + 3600000L);
                when(accessToken.isExpired()).thenReturn(false);
                accessTokenMap.put(token, accessToken);

                return accessToken;
            }
        });

        BearerOAuth2Proxy auth2Proxy = new BearerOAuth2Proxy(component.getContext(), getClient());
        auth2Proxy.pushOAuth2Proxy(application.getContext());
        auth2Proxy
                .setAuthorizationEndpoint(new Reference("riap://component/test/oauth2/authorize"));
        auth2Proxy.setTokenEndpoint(new Reference(new Reference(
                "riap://component/test/oauth2/access_token")));
        auth2Proxy.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "cid",
                "admin".toCharArray()));
        auth2Proxy.setClientCredentials("cid", "admin");
        auth2Proxy.setRedirectionEndpoint(new Reference("http://localhost:8080/oauth2/cb"));
        auth2Proxy.setResourceOwnerCredentials("admin", "admin");
        auth2Proxy.setScope(OAuth2Utils.split("read write", " "));
        auth2Proxy.pushOAuth2Proxy(component.getContext());

    }

    @AfterClass
    public void afterClass() throws Exception {
        realm.setProvider(pathProvider);
        realm.deactivate();
        realm.setProvider(queryProvider);
        realm.deactivate();
    }

    protected Restlet getClient() {
        return component.getContext().getClientDispatcher();
    }

    public Verifier getUserVerifier() {
        MapVerifier mapVerifier = new MapVerifier();
        // Load a single static login/secret pair.
        mapVerifier.getLocalSecrets().put("admin", "admin".toCharArray());
        return mapVerifier;
    }

}
