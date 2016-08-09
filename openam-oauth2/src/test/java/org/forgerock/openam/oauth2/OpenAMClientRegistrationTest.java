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
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
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
}