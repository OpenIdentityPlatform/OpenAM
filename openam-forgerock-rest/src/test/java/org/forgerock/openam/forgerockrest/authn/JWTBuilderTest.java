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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.shared.encode.Base64;
import org.forgerock.openam.forgerockrest.authn.exceptions.JWTBuilderException;
import org.forgerock.openam.utils.SignatureUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertEquals;

public class JWTBuilderTest {

    private JWTBuilder jwtBuilder;

    private SignatureUtil signatureUtil;

    @BeforeClass
    public void setUp() {
        signatureUtil = mock(SignatureUtil.class);
    }

    @BeforeMethod
    public void setUpMethod() {
        jwtBuilder = new JWTBuilder(signatureUtil);
    }

    @Test
    public void shouldBuildPlainTextJWT() throws JSONException {

        //Given
        Map<String, Object> claimPairs = new LinkedHashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");

        //When
        String jwt = jwtBuilder.addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .build();

        //Then
        JSONObject header = new JSONObject()
                .put("typ", "JWT")
                .put("alg", "none");

        JSONObject body = new JSONObject()
                .put("NAME1", true)
                .put("NAME2", 1)
                .put("N AME3", "VAL UE3");

        String expectedJwt = Base64.encode(header.toString().getBytes())
                + "." + Base64.encode(body.toString().getBytes())
                + ".";
        assertEquals(expectedJwt, jwt);
    }

    @Test
    public void shouldBuildSignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);

        //When
        String jwt = jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .sign(privateKey)
                .build();

        //When
        JSONObject header = new JSONObject()
                .put("typ", "JWS")
                .put("alg", "ALGORITHM");

        JSONObject body = new JSONObject()
                .put("NAME1", true)
                .put("NAME2", 1)
                .put("N AME3", "VAL UE3");

        String expectedJwt = Base64.encode(header.toString().getBytes())
                + "." + Base64.encode(body.toString().getBytes())
                + "." + Base64.encode(signature);
        assertEquals(expectedJwt, jwt);
    }

    //TODO
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldBuildEncryptedJWT() throws JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");

        //When
        String jwt = jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .encrypt()
                .build();

        //When
        fail();
    }

    //TODO
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldBuildSignedThenEncryptedJWT() throws SignatureException {


        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);


        //When
        fail();
    }

    //TODO
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldBuilderEncryptedThenSignedJWT() throws SignatureException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);

        //When
        String jwt = jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .encrypt()
                .sign(privateKey)
                .build();

        //When
        fail();
    }

    @Test (expectedExceptions = JWTBuilderException.class)
    public void shouldFailToSetAlgorithmOnSignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.sign(privateKey);

        //When
        jwtBuilder.setAlgorithm("ALOGIRTHM");

        //When
        fail();
    }

    @Test (expectedExceptions = JWTBuilderException.class)
    public void shouldFailToAddValuePairOnSignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.sign(privateKey);

        //When
        jwtBuilder.addValuePair("NAME1", true);

        //When
        fail();
    }

    @Test (expectedExceptions = JWTBuilderException.class)
    public void shouldFailToAddValuePairsOnSignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.sign(privateKey);

        //When
        jwtBuilder.addValuePairs(claimPairs);

        //When
        fail();
    }

    //TODO (expectedExceptions = JWTBuilderException.class)
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldFailToSetAlgorithmOnEncryptedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");

        jwtBuilder.encrypt();

        //When
        jwtBuilder.setAlgorithm("ALOGIRTHM");

        //When
        fail();
    }

    //TODO (expectedExceptions = JWTBuilderException.class)
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldFailToAddValuePairOnEncryptedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");

        jwtBuilder.encrypt();

        //When
        jwtBuilder.addValuePair("NAME1", true);

        //When
        fail();
    }

    //TODO (expectedExceptions = JWTBuilderException.class)
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldFailToAddValuePairsOnEncryptedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");

        jwtBuilder.encrypt();

        //When
        jwtBuilder.addValuePairs(claimPairs);

        //When
        fail();
    }

    @Test (expectedExceptions = JWTBuilderException.class)
    public void shouldFailToSignASignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .sign(privateKey);

        //When
        jwtBuilder.sign(privateKey);

        //When
        fail();
    }

    //TODO (expectedExceptions = JWTBuilderException.class)
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldFailToSignASignedThenEncryptedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .sign(privateKey)
                .encrypt();

        //When
        jwtBuilder.sign(privateKey);

        //When
        fail();
    }

    //TODO (expectedExceptions = JWTBuilderException.class)
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldFailToSignAnEncryptedThenSignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .encrypt()
                .sign(privateKey);

        //When
        jwtBuilder.sign(privateKey);

        //When
        fail();
    }

    //TODO (expectedExceptions = JWTBuilderException.class)
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldFailToEncryptASignedThenEncryptedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .sign(privateKey)
                .encrypt();

        //When
        jwtBuilder.encrypt();

        //When
        fail();
    }

    //TODO (expectedExceptions = JWTBuilderException.class)
    @Test (expectedExceptions = UnsupportedOperationException.class)
    public void shouldFailToEncryptAnEncryptedThenSignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);
        jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .encrypt()
                .sign(privateKey);

        //When
        jwtBuilder.encrypt();

        //When
        fail();
    }

    @Test
    public void shouldVerifySignedJWT() throws SignatureException, JSONException {

        //Given
        Map<String, Object> claimPairs = new HashMap<String, Object>();
        claimPairs.put("NAME2", 1);
        claimPairs.put("N AME3", "VAL UE3");
        PrivateKey privateKey = mock(PrivateKey.class);
        byte[] signature = "SIGNATURE".getBytes();
        X509Certificate certificate = mock(X509Certificate.class);

        given(signatureUtil.sign(eq(privateKey), anyString())).willReturn(signature);

        String jwt = jwtBuilder.setAlgorithm("ALGORITHM")
                .addValuePair("NAME1", true)
                .addValuePairs(claimPairs)
                .sign(privateKey)
                .build();

        JSONObject header = new JSONObject()
                .put("typ", "JWS")
                .put("alg", "ALGORITHM");

        JSONObject body = new JSONObject()
                .put("NAME1", true)
                .put("NAME2", 1)
                .put("N AME3", "VAL UE3");

        String encodedHeaderAndBody = Base64.encode(header.toString().getBytes())
                + Base64.encode(body.toString().getBytes());

        given(signatureUtil.verify(certificate, encodedHeaderAndBody,
                Base64.encode(signature).getBytes())).willReturn(true);

        //When
        jwtBuilder.verify(jwt, certificate);

        //Then
        //All Good
    }
}
