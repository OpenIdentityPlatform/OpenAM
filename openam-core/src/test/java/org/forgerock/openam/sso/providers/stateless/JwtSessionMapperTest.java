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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.openam.sso.providers.stateless;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.SecretKeySpec;

import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.share.SessionInfo;

/**
 * @since 13.0.0
 */
public class JwtSessionMapperTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectConfigurationWithNoEncryptionOrSigning() throws IOException {
        JwtSessionMapperBuilder jwtSessionBuilder = new JwtSessionMapperBuilder(){
            @Override
            boolean isConfigured() { return true; }
        };
        jwtSessionBuilder.build();
    }

    @Test
    public void canRoundtripSessionInfoAsSignedPlaintextJwt() throws IOException {
        // Given
        SessionInfo inputSessionInfo = newExampleSessionInfo();
        JwtSessionMapper jwtSessionMapper = new JwtSessionMapperBuilder().signedUsingHS256("SHARED_SECRET").build();

        // When
        String jwtString = jwtSessionMapper.asJwt(inputSessionInfo);
        SessionInfo outputSessionInfo = jwtSessionMapper.fromJwt(jwtString);

        // Then
        assertEquals(inputSessionInfo, outputSessionInfo);
    }

    @Test
    public void canRoundtripSessionInfoAsSignedEncryptedJwtUsingMatchingAlgs() throws Exception {
        // Given
        SessionInfo inputSessionInfo = newExampleSessionInfo();
        KeyPair keyPair = newKeyPair();
        JwtSessionMapper jwtSessionMapper =
                new JwtSessionMapperBuilder().signedUsingRS256(keyPair).encryptedUsingKeyPair(keyPair).build();

        // When
        String jwtString = jwtSessionMapper.asJwt(inputSessionInfo);
        SessionInfo outputSessionInfo = jwtSessionMapper.fromJwt(jwtString);

        // Then
        assertEquals(inputSessionInfo, outputSessionInfo);
    }

    @Test(dataProvider = "encryptionAlgorithms")
    public void canRoundtripSessionInfoAsSignedEncryptedJwtUsingDifferentAlgs(JweAlgorithm jweAlgorithm)
            throws Exception {
        // Given
        SessionInfo inputSessionInfo = newExampleSessionInfo();
        KeyPair keyPair = newKeyPair();
        Key key;
        JwtSessionMapperBuilder builder = new JwtSessionMapperBuilder().signedUsingHS256("SHARED_SECRET");
        switch (jweAlgorithm.getAlgorithmType()) {
            case RSA:
                builder.encryptedUsingKeyPair(keyPair);
                break;
            case DIRECT:
                key = new SecretKeySpec(new byte[32], "AES");
                builder.encryptedUsingDirectKey(key);
                break;
            case AES_KEYWRAP:
                key = new SecretKeySpec(new byte[16], "AES");
                builder.encryptedUsingKeyWrap(key);
                break;
        }

        // When
        String jwtString = builder.build().asJwt(inputSessionInfo);
        SessionInfo outputSessionInfo = builder.build().fromJwt(jwtString);

        // Then
        assertEquals(inputSessionInfo, outputSessionInfo);
    }

    @Test(expectedExceptions = InvalidJwtException.class)
    public void throwsInvalidJwtExceptionWhenAttemptingToReadStandardOpenAMTokenID() {
        // Given
        String tokenId = "AQIC5wM2LY4SfcwpSGrFolnz48kscZqlRuiOh2f1LQDHWEM.*AAJTSQACMDIAAlNLABQtNzg4MzM3MzY5MDUyOTA5MTE4MQACUzEAAjAz*";
        JwtSessionMapper jwtSessionMapper = new JwtSessionMapperBuilder().signedUsingHS256("SHARED_SECRET").build();

        // When
        jwtSessionMapper.fromJwt(tokenId);

        // Then
        // expect InvalidJwtException
    }

    @Test(expectedExceptions = JwtRuntimeException.class)
    public void throwsExceptionIfSignatureVerificationOfPlaintextJwtFails() {
        // Given
        SessionInfo inputSessionInfo = newExampleSessionInfo();
        JwtSessionMapperBuilder builder = new JwtSessionMapperBuilder().signedUsingHS256("SHARED_SECRET");
        builder.verificationHandler = new SigningManager().newHmacSigningHandler(
                "INVALID_KEY".getBytes(StandardCharsets.UTF_8));
        JwtSessionMapper jwtSessionMapper = builder.build();

        // When
        String plaintextJwt = jwtSessionMapper.asJwt(inputSessionInfo);
        jwtSessionMapper.fromJwt(plaintextJwt);

        // Then
        // expect InvalidJwtException

    }

    @Test(expectedExceptions = JwtRuntimeException.class)
    public void throwsExceptionIfSignatureVerificationOfEncryptedJwtFails() throws Exception {

        // Given
        SessionInfo inputSessionInfo = newExampleSessionInfo();
        JwtSessionMapperBuilder builder = new JwtSessionMapperBuilder().signedUsingHS256("SHARED_SECRET")
                                                                       .encryptedUsingKeyPair(newKeyPair());
        builder.verificationHandler = new SigningManager().newHmacSigningHandler(
                "INVALID_KEY".getBytes(StandardCharsets.UTF_8));
        JwtSessionMapper jwtSessionMapper = builder.build();

        // When
        String plaintextJwt = jwtSessionMapper.asJwt(inputSessionInfo);
        jwtSessionMapper.fromJwt(plaintextJwt);

        // Then
        // expect InvalidJwtException
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void throwsExceptionIfConfigSpecifiesHmacSigningButDoesntProvideSharedSecret() throws Exception {
        new JwtSessionMapperBuilder().signedUsingHS256("").build();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void throwsExceptionIfConfigSpecifiesRsaSigningButDoesntProvideKeyPair() throws Exception {
        new JwtSessionMapperBuilder().signedUsingRS256(null).build();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void throwsExceptionIfConfigSpecifiesRsaEncryptionButDoesntProvideKeyPair() throws Exception {
        new JwtSessionMapperBuilder().encryptedUsingKeyPair(null).build();
    }

    @DataProvider
    public Object[][] encryptionAlgorithms() {
        // Only test algorithms that do not require JCE Unlimited Strength
        return new Object[][] {
                {JweAlgorithm.RSAES_PKCS1_V1_5},
                {JweAlgorithm.RSA_OAEP},
                {JweAlgorithm.RSA_OAEP_256},
                {JweAlgorithm.DIRECT},
                {JweAlgorithm.A128KW}
        };
    }

    private SessionInfo newExampleSessionInfo() {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionID(null);
        sessionInfo.setSessionType("user");
        sessionInfo.setClientID("id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org");
        sessionInfo.setClientDomain("dc=openam,dc=openidentityplatform,dc=org");
        sessionInfo.setMaxTime(120);
        sessionInfo.setMaxIdle(30);
        sessionInfo.setMaxCaching(3);
        sessionInfo.setTimeIdle(11);
        sessionInfo.setTimeLeft(7189);
//        sessionInfo.expiry="9223372036854775807";
        sessionInfo.setState("valid");
        sessionInfo.getProperties().put("CharSet", "UTF-8");
        sessionInfo.getProperties().put("UserId", "amadmin");
        sessionInfo.getProperties().put("FullLoginURL", "/openam/UI/Login?realm=%2F");
        sessionInfo.getProperties().put("successURL", "/openam/console");
        sessionInfo.getProperties().put("cookieSupport", "true");
        sessionInfo.getProperties().put("AuthLevel", "0");
        sessionInfo.getProperties().put("UserToken", "amadmin");
        sessionInfo.getProperties().put("loginURL", "/openam/UI/Login");
        sessionInfo.getProperties().put("Principals", "amadmin");
        sessionInfo.getProperties().put("Service", "ldapService");
        sessionInfo.getProperties().put("sun.am.UniversalIdentifier", "id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org");
        sessionInfo.getProperties().put("amlbcookie", "01");
        sessionInfo.getProperties().put("Organization", "dc=openam,dc=openidentityplatform,dc=org");
        sessionInfo.getProperties().put("Locale", "en_GB");
        sessionInfo.getProperties().put("HostName", "127.0.0.1");
        sessionInfo.getProperties().put("AuthType", "DataStore");
        sessionInfo.getProperties().put("Host", "127.0.0.1");
        sessionInfo.getProperties().put("UserProfile", "Required");
        sessionInfo.getProperties().put("AMCtxId", "13452a66dc3c54bc01");
        sessionInfo.getProperties().put("clientType", "genericHTML");
        sessionInfo.getProperties().put("authInstant", "2015-01-14T12:00:44Z");
        sessionInfo.getProperties().put("Principal", "id=amadmin,ou=user,dc=openam,dc=openidentityplatform,dc=org");
        return sessionInfo;
    }

    /**
     * Generate a random RSA public-private key pair.
     *
     * @return The public-private KeyPair.
     */
    static KeyPair newKeyPair() throws NoSuchAlgorithmException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        return keyPairGenerator.generateKeyPair();

    }
}