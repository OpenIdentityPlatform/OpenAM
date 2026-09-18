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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2026 3A Systems, LLC.
 */

package org.forgerock.openam.oauth2;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.openam.oauth2.OAuth2Constants.OAuth2Client.*;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.jaspi.modules.openid.resolvers.SharedSecretOpenIdResolverImpl;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.json.jose.builders.JwsHeaderBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.util.encode.Base64url;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.encode.Base64;

public class OpenAMClientRegistrationTest {

    private static final String REDIRECT_URI = "http://redirection.uri";
    private static final String POST_LOGOUT_URI = "http://post.logout.uri";
    private static final String RESPONSE_TYPE = "response type";
    private static final String ANOTHER_RESPONSE_TYPE = "another response type";

    @Mock
    private AMIdentity amIdentity;
    private OpenAMClientRegistration clientRegistration;
    @Mock
    private OpenIdResolverService resolver;
    @Mock
    private OAuth2ProviderSettings providerSettings;

    private PublicKey publicEncryptionKey;

    @BeforeClass
    public void generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        publicEncryptionKey = keyPairGenerator.generateKeyPair().getPublic();
    }

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        ClientAuthenticationFailureFactory failureFactory = mock(ClientAuthenticationFailureFactory.class);
        clientRegistration = new OpenAMClientRegistration(amIdentity, new PEMDecoder(), resolver, providerSettings,
                failureFactory);
    }

    @Test
    public void canGetRedirectUris() throws IdRepoException, SSOException {
        final String redirectUri = OAuth2Constants.OAuth2Client.REDIRECT_URI + "=" + REDIRECT_URI;
        setUpAgentWithAttribute(OAuth2Constants.OAuth2Client.REDIRECT_URI, redirectUri);

        assertThat(clientRegistration.getRedirectUris())
                .isEqualTo(new HashSet<>(Collections.singletonList(URI.create(REDIRECT_URI))));
    }

    @Test(expectedExceptions = OAuthProblemException.class)
    public void throwExceptionIfCannotGetRedirectUris() throws IdRepoException, SSOException {
        setUpAgentToThrowExceptionForAttribute(OAuth2Constants.OAuth2Client.REDIRECT_URI);

        clientRegistration.getRedirectUris();
    }

    @Test
    public void canGetPostLogoutRedirectUris() throws IdRepoException, SSOException {
        final String logoutUri = OAuth2Constants.OAuth2Client.POST_LOGOUT_URI + "=" + POST_LOGOUT_URI;
        setUpAgentWithAttribute(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI, logoutUri);

        assertThat(clientRegistration.getPostLogoutRedirectUris())
                .isEqualTo(new HashSet<>(Collections.singletonList(URI.create(POST_LOGOUT_URI))));
    }

    @Test(expectedExceptions = OAuthProblemException.class)
    public void throwExceptionIfCannotGetPostLogoutRedirectUris() throws IdRepoException, SSOException {
        setUpAgentToThrowExceptionForAttribute(OAuth2Constants.OAuth2Client.POST_LOGOUT_URI);

        clientRegistration.getPostLogoutRedirectUris();
    }

    @Test
    public void canGetAllowedResponseTypes() throws IdRepoException, SSOException {
        final String responseType = OAuth2Constants.OAuth2Client.RESPONSE_TYPES + "=" + RESPONSE_TYPE;
        final String anotherResponseType = OAuth2Constants.OAuth2Client.RESPONSE_TYPES + "=" + ANOTHER_RESPONSE_TYPE;
        setUpAgentWithAttribute(OAuth2Constants.OAuth2Client.RESPONSE_TYPES, responseType, anotherResponseType);

        assertThat(clientRegistration.getAllowedResponseTypes())
                .isEqualTo(new HashSet<>(Arrays.asList(RESPONSE_TYPE, ANOTHER_RESPONSE_TYPE)));
    }

    @Test(expectedExceptions = OAuthProblemException.class)
    public void throwExceptionIfCannotGetAllowedResponseTypes() throws IdRepoException, SSOException {
        setUpAgentToThrowExceptionForAttribute(OAuth2Constants.OAuth2Client.RESPONSE_TYPES);

        clientRegistration.getAllowedResponseTypes();
    }

    @Test
    public void canGetClientSecret() throws IdRepoException, SSOException {
        final String SECRET = "client secret";
        setUpAgentWithAttribute(OAuth2Constants.OAuth2Client.USERPASSWORD, SECRET);

        assertThat(clientRegistration.getClientSecret()).isEqualTo(SECRET);
    }

    @Test
    public void canGetAccessTokenType() {
        final String accessTokenType = clientRegistration.getAccessTokenType();

        assertThat(accessTokenType).isEqualTo("Bearer");
    }

    @Test
    public void testGetDisplayName() throws Exception {
        // Given
        when(amIdentity.getAttribute(NAME)).thenReturn(asSet("[0]=Name1", "[2]=en|Name2", "[1]=en_GB|Name3"));

        // When
        String name = clientRegistration.getDisplayName(Locale.UK);

        // Then
        assertThat(name).isEqualTo("Name3");
    }

    @Test
    public void testGetDisplayDescription() throws Exception {
        // Given
        when(amIdentity.getAttribute(DESCRIPTION)).thenReturn(asSet("[0]=Desc1", "[1]=en|Desc2", "[2]=en_GB|Desc3"));

        // When
        String desc = clientRegistration.getDisplayDescription(Locale.UK);

        // Then
        assertThat(desc).isEqualTo("Desc3");
    }

    @DataProvider(name = "languageStrings")
    public Object[][] languageStrings() {
        return new Object[][] {
                { asSet("1"), Locale.ENGLISH, "1" },
                { asSet("1", "en|2", "en_US|3", "en_GB|4"), Locale.ENGLISH, "2" },
                { asSet("1", "en|2", "en_US|3", "en_GB|4"), Locale.UK, "4" },
                { asSet("en_US|3"), new Locale("en", "US", "WIN"), "3" }
        };
    }

    @Test(dataProvider = "languageStrings")
    public void testFindLocaleSpecificString(Set<String> strings, Locale locale, String expected) throws Exception {
        // Given
        List<String[]> splitStrings = clientRegistration.splitPipeDelimited(strings, "").get("");

        // When
        String result = clientRegistration.findLocaleSpecificString(splitStrings, locale);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    @DataProvider(name = "locales")
    public Object[][] localesData() {
        return new Object[][] {
                { new Locale("en"), asList("en") },
                { new Locale("de", "DE"), asList("de_DE", "de") },
                { new Locale("", "GB"), asList("_GB") },
                { new Locale("de", "", "POSIX"), asList("de__POSIX", "de") }
        };
    }

    @Test(dataProvider = "locales")
    public void testLanguageStrings(Locale locale, List<String> expected) throws Exception {
        assertThat(clientRegistration.languageStrings(locale)).isEqualTo(expected);
    }

    @Test
    public void testGetScopeDescriptions() throws Exception {
        // Given
        when(amIdentity.getAttribute(DEFAULT_SCOPES)).thenReturn(asSet(
                "[0]=scope1",
                "[1]=scope2|Default",
                "[2]=scope2|fr|En Français",
                "[3]=scope3|en|Default English",
                "[4]=scope3|en_GB|British, innit",
                "[5]=scope3|en_US|American y'all",
                "[6]=scope4",
                "[7]=scope4|en|English only",
                "[8]=scope5|Default with overridden French exclusion",
                "[9]=scope5|fr|",
                "[10]=scope6|en|Included in English",
                "[11]=scope6|"
        ));

        when(providerSettings.getSupportedScopesWithTranslations()).thenReturn(asSet(
                "scope1",
                "scope2",
                "scope2|fr_FR|Pas en Français",
                "scope3",
                "scope4",
                "scope5",
                "scope6",
                "scope7",
                "scope8",
                "scope8|fr|Aussi en Français",
                "scope8|en|In English"
        ));

        // When
        Map<String, String> french = clientRegistration.getScopeDescriptions(Locale.FRANCE);
        Map<String, String> english = clientRegistration.getScopeDescriptions(Locale.UK);

        // Then
        assertThat(french).containsOnly(
                entry("scope1", "scope1"),
                entry("scope2", "En Français"),
                entry("scope3", "scope3"),
                entry("scope4", "scope4"),
                entry("scope7", "scope7"),
                entry("scope8", "Aussi en Français"));
        assertThat(english).containsOnly(
                entry("scope1", "scope1"),
                entry("scope2", "Default"),
                entry("scope3", "British, innit"),
                entry("scope4", "English only"),
                entry("scope5", "Default with overridden French exclusion"),
                entry("scope6", "Included in English"),
                entry("scope7", "scope7"),
                entry("scope8", "In English")
        );
    }

    @Test(dataProvider = "encryptionAlgorithms")
    public void shouldReturnCorrectEncryptionKey(JweAlgorithm jweAlgorithm, Key key) throws Exception {
        when(amIdentity.getAttribute("idTokenEncryptionAlgorithm")).thenReturn(singleton(jweAlgorithm.toString()));
        when(amIdentity.getAttribute("idTokenPublicEncryptionKey")).thenReturn(singleton(pem(publicEncryptionKey)));
        when(amIdentity.getAttribute("userpassword")).thenReturn(singleton("password"));
        when(amIdentity.getAttribute(OAuth2Constants.OAuth2Client.CLIENT_TYPE)).thenReturn(singleton("CONFIDENTIAL"));
        when(amIdentity.getAttribute("idTokenEncryptionMethod")).thenReturn(singleton("A256CBC-HS512"));

        Key encryptionKey = clientRegistration.getIDTokenEncryptionKey();

        assertThat(encryptionKey).isEqualTo(key);
    }

    @Test(expectedExceptions = OAuthProblemException.class)
    public void shouldDisallowSymmetricEncryptionForPublicClients() throws Exception {
        when(amIdentity.getAttribute("idTokenEncryptionAlgorithm")).thenReturn(singleton("dir"));
        when(amIdentity.getAttribute("idTokenEncryptionMethod")).thenReturn(singleton("A256CBC-HS512"));
        when(amIdentity.getAttribute(OAuth2Constants.OAuth2Client.CLIENT_TYPE)).thenReturn(singleton("PUBLIC"));

        clientRegistration.getIDTokenEncryptionKey();
    }

    private String pem(PublicKey pk) {
        return "-----BEGIN PUBLIC KEY-----" + Base64.encode(pk.getEncoded()) + "-----END PUBLIC KEY-----";
    }

    @DataProvider
    public Object[][] encryptionAlgorithms() throws Exception {
        return new Object[][] {
                { JweAlgorithm.RSAES_PKCS1_V1_5, publicEncryptionKey },
                { JweAlgorithm.RSA_OAEP, publicEncryptionKey },
                { JweAlgorithm.RSA_OAEP_256, publicEncryptionKey },
                { JweAlgorithm.DIRECT, hashedKey("SHA-512", "password", 64)},
                { JweAlgorithm.A128KW, hashedKey("SHA-256", "password", 16)},
                { JweAlgorithm.A192KW, hashedKey("SHA-256", "password", 24)},
                { JweAlgorithm.A256KW, hashedKey("SHA-256", "password", 32)}
        };
    }

    private Key hashedKey(String hashAlgorithm, String password, int size) throws Exception {
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
        return new SecretKeySpec(Arrays.copyOfRange(md.digest(password.getBytes(StandardCharsets.UTF_8)), 0, size),
                "AES");
    }

    private void setUpAgentWithAttribute(String attributeName, String... attributeValues)
            throws IdRepoException, SSOException {
        given(amIdentity.getAttribute(attributeName))
                .willReturn(new HashSet<>(Arrays.asList(attributeValues)));
    }

    private void setUpAgentToThrowExceptionForAttribute(String attributeName) throws IdRepoException, SSOException {
        given(amIdentity.getAttribute(attributeName))
                .willThrow(new SSOException("exception!"));
    }

    // --- #1130: id_token_signed_response_alg default and client-assertion dispatch ----------

    /**
     * AgentsRepo reads agent attributes without schema defaults, so a client created without
     * the attribute (realm-config PUT, ssoadm) has no value persisted; the token endpoint then
     * NPEs. The schema, the console and dynamic registration all default to HS256.
     */
    @Test
    public void idTokenSignedResponseAlgorithmDefaultsToHs256WhenUnset() throws Exception {
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(Collections.<String>emptySet());
        assertThat(clientRegistration.getIDTokenSignedResponseAlgorithm()).isEqualTo("HS256");

        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(null);
        assertThat(clientRegistration.getIDTokenSignedResponseAlgorithm()).isEqualTo("HS256");

        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("RS256"));
        assertThat(clientRegistration.getIDTokenSignedResponseAlgorithm()).isEqualTo("RS256");
    }

    /**
     * A client_secret_jwt assertion is verified with the client secret whatever algorithm the
     * client asked for its (outgoing) ID tokens.
     */
    @Test
    public void verifyJwtIdentityUsesClientSecretForHmacAssertionRegardlessOfIdTokenAlg() throws Exception {
        String clientId = "client1";
        String secret = "a-client-secret-of-sufficient-length";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton(secret));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("RS256"));

        SigningHandler signer = new SigningManager().newHmacSigningHandler(secret.getBytes(StandardCharsets.UTF_8));
        OAuth2Jwt assertion = assertion(clientId, signer, JwsAlgorithm.HS256, null);

        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isTrue();

        SigningHandler wrongSigner = new SigningManager().newHmacSigningHandler("wrong".getBytes(StandardCharsets.UTF_8));
        assertThat(clientRegistration.verifyJwtIdentity(assertion(clientId, wrongSigner, JwsAlgorithm.HS256, null)))
                .isFalse();
    }

    /**
     * A private_key_jwt assertion is verified with the client's registered public keys whatever
     * algorithm the client asked for its (outgoing) ID tokens.
     */
    @Test
    public void verifyJwtIdentityUsesPublicKeysForAsymmetricAssertionRegardlessOfIdTokenAlg() throws Exception {
        String clientId = "client1";
        KeyPair clientKeys = generateRsaKeyPair();
        String kid = UUID.randomUUID().toString();
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("unused-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("jwks"));
        given(amIdentity.getAttribute(JWKS)).willReturn(singleton(jwks((RSAPublicKey) clientKeys.getPublic(), kid)));

        SigningHandler signer = new SigningManager().newRsaSigningHandler((RSAPrivateKey) clientKeys.getPrivate());
        assertThat(clientRegistration.verifyJwtIdentity(assertion(clientId, signer, JwsAlgorithm.RS256, kid))).isTrue();

        KeyPair otherKeys = generateRsaKeyPair();
        SigningHandler otherSigner = new SigningManager().newRsaSigningHandler((RSAPrivateKey) otherKeys.getPrivate());
        assertThat(clientRegistration.verifyJwtIdentity(assertion(clientId, otherSigner, JwsAlgorithm.RS256, kid)))
                .isFalse();
    }

    /**
     * {@code JwsHeader.getAlgorithm()} is {@code JwsAlgorithm.valueOf(alg)}: the RFC spelling
     * {@code "none"} (or anything outside the enum) throws rather than returning {@code NONE}, so
     * the assertion is built as a raw compact serialisation, not through the builder, which would
     * emit the enum name {@code "NONE"} that never occurs on the wire.
     */
    @Test
    public void verifyJwtIdentityRejectsWireAlgNone() throws Exception {
        String clientId = "client1";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));

        assertThat(clientRegistration.verifyJwtIdentity(rawAssertion(clientId, "none"))).isFalse();
    }

    @Test
    public void verifyJwtIdentityRejectsUnknownAlg() throws Exception {
        String clientId = "client1";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));

        assertThat(clientRegistration.verifyJwtIdentity(rawAssertion(clientId, "hs256"))).isFalse();
    }

    /** A public client has no userpassword at all; an HMAC assertion is invalid_client, not an NPE. */
    @Test
    public void verifyJwtIdentityReturnsFalseForHmacAssertionWhenClientHasNoSecret() throws Exception {
        String clientId = "client1";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        SigningHandler signer = new SigningManager().newHmacSigningHandler("any".getBytes(StandardCharsets.UTF_8));
        OAuth2Jwt assertion = assertion(clientId, signer, JwsAlgorithm.HS256, null);

        given(amIdentity.getAttribute("userpassword")).willReturn(null);
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();

        given(amIdentity.getAttribute("userpassword")).willReturn(Collections.<String>emptySet());
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();
    }

    /**
     * AgentsRepo does not apply the schema default of publicKeyLocation either; a client with no
     * registered key location cannot verify an asymmetric assertion, which is invalid_client rather
     * than a server_error.
     */
    @Test
    public void verifyJwtIdentityReturnsFalseForAsymmetricAssertionWithoutPublicKeyLocation() throws Exception {
        String clientId = "client1";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        KeyPair clientKeys = generateRsaKeyPair();
        SigningHandler signer = new SigningManager().newRsaSigningHandler((RSAPrivateKey) clientKeys.getPrivate());
        OAuth2Jwt assertion = assertion(clientId, signer, JwsAlgorithm.RS256, "kid");

        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(null);
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();

        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(Collections.<String>emptySet());
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();

        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("not-a-selector"));
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();
    }

    /**
     * A symmetric (oct) JWK can never verify an RS/ES-signed assertion; it must not be tried as an
     * HMAC key. (JWKLookup keys an oct JWK's {@code alg} by the JCA name, hence {@code HmacSHA256}.)
     */
    @Test
    public void verifyJwtIdentityRejectsAsymmetricAssertionAgainstSymmetricJwk() throws Exception {
        String clientId = "client1";
        String kid = UUID.randomUUID().toString();
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("jwks"));
        given(amIdentity.getAttribute(JWKS)).willReturn(singleton("{\"keys\":[{\"kty\":\"oct\",\"alg\":\"HmacSHA256\",\"kid\":\"" + kid
                + "\",\"k\":\"" + Base64url.encode("a-shared-key-of-sufficient-length".getBytes(StandardCharsets.UTF_8))
                + "\"}]}"));
        KeyPair clientKeys = generateRsaKeyPair();
        SigningHandler signer = new SigningManager().newRsaSigningHandler((RSAPrivateKey) clientKeys.getPrivate());

        assertThat(clientRegistration.verifyJwtIdentity(assertion(clientId, signer, JwsAlgorithm.RS256, kid))).isFalse();
    }

    /**
     * An ID token this server issued carries id_token_signed_response_alg in its header; one signed
     * with any other algorithm is not ours, whatever key it would otherwise verify with.
     */
    @Test
    public void verifyIdTokenIdentityRejectsAlgorithmOtherThanConfigured() throws Exception {
        String clientId = "client1";
        String secret = "a-client-secret-of-sufficient-length";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton(secret));
        SigningHandler signer = new SigningManager().newHmacSigningHandler(secret.getBytes(StandardCharsets.UTF_8));
        OAuth2Jwt idToken = assertion(clientId, signer, JwsAlgorithm.HS256, null);

        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        assertThat(clientRegistration.verifyIdTokenIdentity(idToken)).isTrue();

        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("RS256"));
        assertThat(clientRegistration.verifyIdTokenIdentity(idToken)).isFalse();
    }

    @Test
    public void verifyIdTokenIdentityRejectsWireAlgNone() throws Exception {
        String clientId = "client1";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));

        assertThat(clientRegistration.verifyIdTokenIdentity(rawAssertion(clientId, "none"))).isFalse();
    }

    /**
     * AgentConfiguration.createAgent persists the schema defaults, so a client created through the
     * console, ssoadm or /json/agents carries publicKeyLocation=jwks_uri and no jwks_uri. Nothing
     * registered behind the selector is invalid_client, not server_error with a logged stack trace.
     */
    @Test
    public void verifyJwtIdentityReturnsFalseWhenRegisteredKeyLocationHasNoMaterial() throws Exception {
        String clientId = "client1";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        KeyPair clientKeys = generateRsaKeyPair();
        SigningHandler signer = new SigningManager().newRsaSigningHandler((RSAPrivateKey) clientKeys.getPrivate());
        OAuth2Jwt assertion = assertion(clientId, signer, JwsAlgorithm.RS256, "kid");

        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("jwks_uri"));
        given(amIdentity.getAttribute(JWKS_URI)).willReturn(null);
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();
        given(amIdentity.getAttribute(JWKS_URI)).willReturn(Collections.<String>emptySet());
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();
        given(amIdentity.getAttribute(JWKS_URI)).willReturn(singleton(""));
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();

        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("x509"));
        given(amIdentity.getAttribute(CLIENT_JWT_PUBLIC_KEY)).willReturn(null);
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();
        given(amIdentity.getAttribute(CLIENT_JWT_PUBLIC_KEY)).willReturn(singleton(""));
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();

        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("jwks"));
        given(amIdentity.getAttribute(JWKS)).willReturn(null);
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();
        given(amIdentity.getAttribute(JWKS)).willReturn(singleton(""));
        assertThat(clientRegistration.verifyJwtIdentity(assertion)).isFalse();
    }

    /** A registered key that cannot verify the header's algorithm (RSA key, ES256 header) is invalid_client. */
    @Test
    public void verifyJwtIdentityReturnsFalseWhenRegisteredKeyCannotVerifyAlgorithm() throws Exception {
        String clientId = "client1";
        String kid = UUID.randomUUID().toString();
        KeyPair rsaKeys = generateRsaKeyPair();
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("jwks"));
        given(amIdentity.getAttribute(JWKS)).willReturn(singleton(jwks((RSAPublicKey) rsaKeys.getPublic(), kid)));

        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        SigningHandler ecSigner = new SigningManager().newEcdsaSigningHandler((ECPrivateKey) gen.generateKeyPair().getPrivate());

        assertThat(clientRegistration.verifyJwtIdentity(assertion(clientId, ecSigner, JwsAlgorithm.ES256, kid))).isFalse();
    }

    /**
     * RSASigningHandler wraps the JDK's "Bad signature length" in JwsVerifyingException, a sibling of
     * JwsSigningException: a client that rotated to a longer key, or any sender choosing the
     * signature length, must be invalid_client rather than server_error with a logged stack trace.
     */
    @Test
    public void verifyJwtIdentityReturnsFalseForRsaSignatureOfWrongLength() throws Exception {
        String clientId = "client1";
        String kid = UUID.randomUUID().toString();
        KeyPair registeredKeys = generateRsaKeyPair(2048);
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("jwks"));
        given(amIdentity.getAttribute(JWKS)).willReturn(singleton(jwks((RSAPublicKey) registeredKeys.getPublic(), kid)));

        KeyPair rotatedKeys = generateRsaKeyPair(3072);
        SigningHandler signer = new SigningManager().newRsaSigningHandler((RSAPrivateKey) rotatedKeys.getPrivate());

        assertThat(clientRegistration.verifyJwtIdentity(assertion(clientId, signer, JwsAlgorithm.RS256, kid))).isFalse();
    }

    /**
     * A symmetric key served at jwks_uri reaches an HMAC handler inside the commons resolver, which
     * cannot verify an RS256 assertion; that is invalid_client, not server_error. The resolver is
     * seeded through the cache so the test needs no HTTP server.
     */
    @Test
    public void verifyJwtIdentityReturnsFalseForSymmetricKeyBehindJwksUri() throws Exception {
        String clientId = "client1";
        String url = "https://jwks.invalid/" + UUID.randomUUID();
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));
        given(amIdentity.getAttribute(PUBLIC_KEY_SELECTOR)).willReturn(singleton("jwks_uri"));
        given(amIdentity.getAttribute(JWKS_URI)).willReturn(singleton(url));
        KeyPair clientKeys = generateRsaKeyPair();
        SigningHandler signer = new SigningManager().newRsaSigningHandler((RSAPrivateKey) clientKeys.getPrivate());
        ClientJwksResolverCache.putIfAbsent(clientId + "|" + url,
                new SharedSecretOpenIdResolverImpl(clientId, "a-shared-key-of-sufficient-length"));
        try {
            assertThat(clientRegistration.verifyJwtIdentity(assertion(clientId, signer, JwsAlgorithm.RS256, "kid")))
                    .isFalse();
        } finally {
            ClientJwksResolverCache.resetForTest();
        }
    }

    /**
     * A header without alg is what JwsHeader.getAlgorithm() maps to NONE, and the enum name "NONE"
     * is the other spelling that reaches that arm; neither is a signed assertion.
     */
    @Test
    public void verifyJwtIdentityRejectsAssertionWithoutAlgOrWithEnumSpelledNone() throws Exception {
        String clientId = "client1";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton("a-client-secret"));
        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("HS256"));

        assertThat(clientRegistration.verifyJwtIdentity(rawAssertion(clientId, null))).isFalse();
        assertThat(clientRegistration.verifyJwtIdentity(rawAssertion(clientId, "NONE"))).isFalse();
    }

    /**
     * idTokenSignedResponseAlg is a free-text attribute; StatefulTokenStore upper-cases it when
     * issuing, so the verifier must accept the same spelling, and an unknown value is not ours.
     */
    @Test
    public void verifyIdTokenIdentityNormalisesConfiguredAlgorithmAndRejectsUnknown() throws Exception {
        String clientId = "client1";
        String secret = "a-client-secret-of-sufficient-length";
        given(amIdentity.getName()).willReturn(clientId);
        given(amIdentity.getAttribute("userpassword")).willReturn(singleton(secret));
        SigningHandler signer = new SigningManager().newHmacSigningHandler(secret.getBytes(StandardCharsets.UTF_8));
        OAuth2Jwt idToken = assertion(clientId, signer, JwsAlgorithm.HS256, null);

        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("hs256"));
        assertThat(clientRegistration.verifyIdTokenIdentity(idToken)).isTrue();

        given(amIdentity.getAttribute(IDTOKEN_SIGNED_RESPONSE_ALG)).willReturn(singleton("not-an-alg"));
        assertThat(clientRegistration.verifyIdTokenIdentity(idToken)).isFalse();
    }

    /**
     * Compact serialisation with the given literal {@code alg} ({@code null}: no alg member) and an
     * empty signature part.
     */
    private static OAuth2Jwt rawAssertion(String clientId, String alg) {
        JwtClaimsSet claims = new JwtBuilderFactory().claims()
                .iss(clientId)
                .sub(clientId)
                .aud(Collections.singletonList(clientId))
                .exp(new Date(System.currentTimeMillis() + 60_000L))
                .iat(new Date())
                .build();
        String headerJson = alg == null ? "{\"typ\":\"JWT\"}" : "{\"typ\":\"JWT\",\"alg\":\"" + alg + "\"}";
        String header = Base64url.encode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = Base64url.encode(claims.build().getBytes(StandardCharsets.UTF_8));
        return OAuth2Jwt.create(header + "." + payload + ".");
    }

    private static OAuth2Jwt assertion(String clientId, SigningHandler signer, JwsAlgorithm alg, String kid) {
        JwtClaimsSet claims = new JwtBuilderFactory().claims()
                .iss(clientId)
                .sub(clientId)
                .aud(Collections.singletonList(clientId))
                .exp(new Date(System.currentTimeMillis() + 60_000L))
                .iat(new Date())
                .build();
        JwsHeaderBuilder headers = new JwtBuilderFactory().jws(signer).headers().alg(alg);
        if (kid != null) {
            headers = headers.kid(kid);
        }
        return OAuth2Jwt.create(headers.done().claims(claims).build());
    }

    private static KeyPair generateRsaKeyPair() throws Exception {
        return generateRsaKeyPair(2048);
    }

    private static KeyPair generateRsaKeyPair(int bits) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(bits);
        return gen.generateKeyPair();
    }

    private static String jwks(RSAPublicKey pk, String kid) {
        return "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + kid + "\","
                + "\"n\":\"" + base64UrlUnsigned(pk.getModulus()) + "\","
                + "\"e\":\"" + base64UrlUnsigned(pk.getPublicExponent()) + "\"}]}";
    }

    private static String base64UrlUnsigned(java.math.BigInteger bi) {
        byte[] full = bi.toByteArray();
        if (full.length > 1 && full[0] == 0) {
            full = Arrays.copyOfRange(full, 1, full.length);
        }
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(full);
    }
}