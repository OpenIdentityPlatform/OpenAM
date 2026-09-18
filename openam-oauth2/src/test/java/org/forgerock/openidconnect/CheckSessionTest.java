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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openidconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Collections;
import java.util.Date;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.utils.OpenAMSettings;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The check-session endpoint reads an id_token out of the {@code Referer} of the caller's iframe.
 * It used to skip signature verification altogether when the token named no client, and to verify
 * every other token with an HMAC handler built from the client secret - whatever algorithm the
 * client was registered for, and whatever the token header said.
 */
public class CheckSessionTest {

    private static final String CLIENT = "the-clients-own-id";
    private static final String SECRET = "the-clients-own-secret-which-is-long-enough";
    private static final String RSA_CLIENT = "a-client-registered-for-rs256";
    private static final String SESSION_URI = "https://rp.example.com/session";
    private static final String OPS = "a-session-identifier";
    private static final String SSO_TOKEN_ID = "an-sso-token-id";

    private static KeyPair providerRsaKeys;

    private SSOTokenManager ssoTokenManager;
    private OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private CheckSession checkSession;

    @BeforeClass
    public static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        providerRsaKeys = generator.generateKeyPair();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        clientRegistrationStore = mock(OpenIdConnectClientRegistrationStore.class);
        register(CLIENT, JwsAlgorithm.HS256);
        register(RSA_CLIENT, JwsAlgorithm.RS256);

        OpenAMSettings openAMSettings = mock(OpenAMSettings.class);
        when(openAMSettings.getSigningKeyPair(anyString(), eq(JwsAlgorithm.RS256))).thenReturn(providerRsaKeys);

        // The session the id_token points at exists and is valid, so that the answer turns only on
        // whether the token itself is accepted.
        CTSPersistentStore cts = mock(CTSPersistentStore.class);
        Token token = mock(Token.class);
        when(cts.read(OPS)).thenReturn(token);
        @SuppressWarnings("unchecked")
        TokenAdapter<JsonValue> tokenAdapter = mock(TokenAdapter.class);
        // The ops field is an array because CTS attributes are multi-valued: StatefulTokenStore writes
        // field(LEGACY_OPS, array(ops)). Stubbing it as a bare string passes against a shape that never
        // reaches this code in production, and hides the read that used to throw on it.
        when(tokenAdapter.fromToken(token)).thenReturn(json(object(field("ops", array(SSO_TOKEN_ID)))));

        ssoTokenManager = mock(SSOTokenManager.class);
        SSOToken ssoToken = mock(SSOToken.class);
        when(ssoTokenManager.createSSOToken(SSO_TOKEN_ID)).thenReturn(ssoToken);
        when(ssoTokenManager.isValidToken(ssoToken)).thenReturn(true);

        checkSession = new CheckSession(ssoTokenManager, openAMSettings, clientRegistrationStore, cts, tokenAdapter,
                new IdTokenSignatureVerifier());
    }

    private void register(String clientId, JwsAlgorithm algorithm) throws Exception {
        OpenIdConnectClientRegistration client = mock(OpenIdConnectClientRegistration.class);
        when(client.getClientId()).thenReturn(clientId);
        when(client.getClientSecret()).thenReturn(SECRET);
        when(client.getIDTokenSignedResponseAlgorithm()).thenReturn(algorithm.name());
        when(client.getClientSessionURI()).thenReturn(SESSION_URI);
        when(clientRegistrationStore.get(eq(clientId), any(OAuth2Request.class))).thenReturn(client);
    }

    @Test
    public void acceptsAnIdTokenSignedByThisProvider() {

        HttpServletRequest request = requestCarrying(idToken(CLIENT, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getValidSession(request)).isTrue();
    }

    @Test
    public void rejectsAnIdTokenSignedWithAnotherSecret() {

        HttpServletRequest request = requestCarrying(
                idToken(CLIENT, hmac("a-secret-this-provider-never-issued"), JwsAlgorithm.HS256));

        assertThat(checkSession.getValidSession(request)).isFalse();
    }

    /**
     * A token naming no client selected no key, and the guard that skipped verification when there
     * was no client registration let it through unchecked.
     */
    @Test
    public void rejectsAnIdTokenThatNamesNoClient() {

        JwtClaimsSet claims = new JwtBuilderFactory().claims()
                .claim("ops", OPS)
                .exp(new Date(System.currentTimeMillis() + 60_000))
                .build();
        HttpServletRequest request = requestCarrying(sign(claims, hmac("any-secret-at-all"), JwsAlgorithm.HS256));

        assertThat(checkSession.getValidSession(request)).isFalse();
    }

    /** The digest is the registration's to choose, not the sender's. */
    @Test
    public void rejectsAnIdTokenSignedWithAnotherDigestThanTheClientIsRegisteredFor() {

        HttpServletRequest request = requestCarrying(idToken(CLIENT, hmac(SECRET), JwsAlgorithm.HS512));

        assertThat(checkSession.getValidSession(request)).isFalse();
    }

    @Test
    public void acceptsAnRsaIdTokenSignedWithTheProvidersKey() {

        HttpServletRequest request = requestCarrying(idToken(RSA_CLIENT,
                new SigningManager().newRsaSigningHandler(providerRsaKeys.getPrivate()), JwsAlgorithm.RS256));

        assertThat(checkSession.getValidSession(request)).isTrue();
    }

    /**
     * An id_token this provider signs with its own key used to be verified with an HMAC handler
     * built from the client secret, so anyone holding that secret could mint one.
     */
    @Test
    public void rejectsAnRsaClientsIdTokenSignedWithTheClientSecret() {

        HttpServletRequest request = requestCarrying(idToken(RSA_CLIENT, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getValidSession(request)).isFalse();
    }

    @Test
    public void answersWithTheClientSessionUriOfAVerifiedIdToken() throws Exception {

        HttpServletRequest request = requestCarrying(idToken(CLIENT, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getClientSessionURI(request)).isEqualTo(SESSION_URI);
    }

    /** Answering this one at all is what used to throw a NullPointerException. */
    @Test
    public void answersWithNoClientSessionUriForAnIdTokenThatNamesNoClient() throws Exception {

        JwtClaimsSet claims = new JwtBuilderFactory().claims().claim("ops", OPS).build();
        HttpServletRequest request = requestCarrying(sign(claims, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getClientSessionURI(request)).isEmpty();
    }

    @Test
    public void answersWithNoClientSessionUriForAnUnsignedIdToken() throws Exception {

        HttpServletRequest request = requestCarrying("this-is-not-a-jwt");

        assertThat(checkSession.getClientSessionURI(request)).isEmpty();
    }

    /**
     * The client is named by the caller's own token, so an id_token naming one that does not exist
     * is an answer of "", not an exception out of the JSP that serves this iframe.
     */
    @Test
    public void answersWithNoClientSessionUriForAnUnknownClient() throws Exception {

        when(clientRegistrationStore.get(eq("no-such-client"), any(OAuth2Request.class)))
                .thenThrow(mock(InvalidClientException.class));
        HttpServletRequest request = requestCarrying(
                idToken("no-such-client", hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getClientSessionURI(request)).isEmpty();
        assertThat(checkSession.getValidSession(request)).isFalse();
    }

    /** Both endpoints resolve the client the same way, azp first and aud after it. */
    @Test
    public void resolvesTheClientFromAzpAsTheEndSessionPathDoes() {

        JwtClaimsSet claims = new JwtBuilderFactory().claims()
                .claim("azp", CLIENT)
                .claim("realm", "/")
                .claim("ops", OPS)
                .exp(new Date(System.currentTimeMillis() + 60_000))
                .build();

        HttpServletRequest request = requestCarrying(sign(claims, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getValidSession(request)).isTrue();
    }

    /**
     * The Referer is the caller's own page URL and may carry a query segment with no {@code =} in it,
     * which the parser used to hand to {@code substring(0, -1)}. getIDToken is called outside both
     * callers' try blocks, so that left this unauthenticated endpoint as a
     * StringIndexOutOfBoundsException.
     */
    @Test
    public void readsAnIdTokenPastAValuelessQuerySegment() {

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Referer")).thenReturn("https://rp.example.com/page?prompt&id_token="
                + idToken(CLIENT, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getValidSession(request)).isTrue();
    }

    /**
     * A stateless access token is signed with the same provider key an RS256 id_token is, and
     * carries an {@code aud} naming its client, so the signature alone does not say what kind of
     * token it is. The endSession path refuses one; this endpoint used to accept it.
     */
    @Test
    public void refusesAProviderSignedTokenThatIsNotAnIdToken() {

        JwtClaimsSet accessToken = claimsFor(RSA_CLIENT);
        accessToken.setClaim("tokenName", "access_token");
        HttpServletRequest request = requestCarrying(
                sign(accessToken, rsa(), JwsAlgorithm.RS256));

        assertThat(checkSession.getClientSessionURI(request)).isEmpty();
        assertThat(checkSession.getValidSession(request)).isFalse();
    }

    /** An id_token that names itself one is accepted for saying so. */
    @Test
    public void acceptsAnIdTokenThatNamesItselfOne() {

        JwtClaimsSet claims = claimsFor(CLIENT);
        claims.setClaim("tokenName", "id_token");
        HttpServletRequest request = requestCarrying(sign(claims, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getClientSessionURI(request)).isEqualTo(SESSION_URI);
        assertThat(checkSession.getValidSession(request)).isTrue();
    }

    /**
     * The registration's own accessors throw unchecked on a datastore fault, and both
     * {@code getIDTokenSignedResponseAlgorithm()} and {@code getClientSessionURI()} are read after
     * the client has been resolved. They used to sit outside the catch, so a valid id_token took an
     * unchecked exception out of here and out of the JSP that serves this iframe, which has no try
     * of its own. {@code getValidSession()} answers false to the identical failure.
     */
    @Test
    public void answersEmptyWhenTheRegistrationCannotBeRead() throws Exception {

        OpenIdConnectClientRegistration failing = mock(OpenIdConnectClientRegistration.class);
        when(failing.getClientId()).thenReturn(CLIENT);
        when(failing.getClientSecret()).thenReturn(SECRET);
        when(failing.getIDTokenSignedResponseAlgorithm()).thenReturn(JwsAlgorithm.HS256.name());
        when(failing.getClientSessionURI()).thenThrow(new IllegalStateException("the datastore is down"));
        when(clientRegistrationStore.get(eq(CLIENT), any(OAuth2Request.class))).thenReturn(failing);

        HttpServletRequest request = requestCarrying(idToken(CLIENT, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getClientSessionURI(request)).isEmpty();
    }

    /** The same fault, raised while the algorithm the signature is checked with is being read. */
    @Test
    public void answersEmptyWhenTheRegisteredAlgorithmCannotBeRead() throws Exception {

        OpenIdConnectClientRegistration failing = mock(OpenIdConnectClientRegistration.class);
        when(failing.getClientId()).thenReturn(CLIENT);
        when(failing.getIDTokenSignedResponseAlgorithm())
                .thenThrow(new IllegalStateException("the datastore is down"));
        when(clientRegistrationStore.get(eq(CLIENT), any(OAuth2Request.class))).thenReturn(failing);

        HttpServletRequest request = requestCarrying(idToken(CLIENT, hmac(SECRET), JwsAlgorithm.HS256));

        assertThat(checkSession.getClientSessionURI(request)).isEmpty();
        assertThat(checkSession.getValidSession(request)).isFalse();
    }

    private HttpServletRequest requestCarrying(String idToken) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Referer")).thenReturn("https://rp.example.com/page?id_token=" + idToken);
        return request;
    }

    private SigningHandler hmac(String secret) {
        return new SigningManager().newHmacSigningHandler(secret.getBytes(Utils.CHARSET));
    }

    private SigningHandler rsa() {
        return new SigningManager().newRsaSigningHandler(providerRsaKeys.getPrivate());
    }

    private String idToken(String clientId, SigningHandler signingHandler, JwsAlgorithm algorithm) {
        return sign(claimsFor(clientId), signingHandler, algorithm);
    }

    private JwtClaimsSet claimsFor(String clientId) {
        return new JwtBuilderFactory().claims()
                .aud(Collections.singletonList(clientId))
                .claim("azp", clientId)
                .claim("realm", "/")
                .claim("ops", OPS)
                .exp(new Date(System.currentTimeMillis() + 60_000))
                .build();
    }

    private String sign(JwtClaimsSet claims, SigningHandler signingHandler, JwsAlgorithm algorithm) {
        return new JwtBuilderFactory()
                .jws(signingHandler)
                .headers().alg(algorithm).done()
                .claims(claims)
                .build();
    }
}
