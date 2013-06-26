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

import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.json.jwt.JwsAlgorithm;
import org.forgerock.json.jwt.JwtBuilder;
import org.forgerock.json.jwt.PlaintextJwt;
import org.forgerock.json.jwt.SignedJwt;
import org.forgerock.openam.forgerockrest.authn.core.AuthIndexType;
import org.forgerock.openam.forgerockrest.authn.core.LoginConfiguration;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.AMKeyProvider;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.*;

public class AuthIdHelperTest {

    private AuthIdHelper authIdHelper;

    private CoreServicesWrapper coreServicesWrapper;
    private AMKeyProvider amKeyProvider;
    private JwtBuilder jwtBuilder;

    private PlaintextJwt plaintextJwt;
    private SignedJwt signedJwt;

    @BeforeMethod
    public void setUp() {

        coreServicesWrapper = mock(CoreServicesWrapper.class);
        amKeyProvider = mock(AMKeyProvider.class);
        jwtBuilder = mock(JwtBuilder.class);

        authIdHelper = new AuthIdHelper(coreServicesWrapper, amKeyProvider, jwtBuilder);

        plaintextJwt = mock(PlaintextJwt.class);
        signedJwt = mock(SignedJwt.class);
        given(jwtBuilder.jwt()).willReturn(plaintextJwt);
        given(plaintextJwt.header(anyString(), anyString())).willReturn(plaintextJwt);
        given(plaintextJwt.content(anyString(), anyString())).willReturn(plaintextJwt);
        given(plaintextJwt.content(anyMap())).willReturn(plaintextJwt);
        given(plaintextJwt.sign(eq(JwsAlgorithm.HS256), (PrivateKey) anyObject())).willReturn(signedJwt);
        given(signedJwt.build()).willReturn("JWT_STRING");
    }

    private void mockGetKeyAliasMethod(String orgName, boolean nullKeyAlias) throws SMSException, SSOException {
        SSOToken adminToken = mock(SSOToken.class);
        ServiceConfigManager serviceConfigManager = mock(ServiceConfigManager.class);
        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        Map<String, Set<String>> orgConfigAttributes = new HashMap<String, Set<String>>();
        Set<String> orgConfigSet = new HashSet<String>();
        orgConfigSet.add("");
        orgConfigSet.add(null);
        if (!nullKeyAlias) {
            orgConfigSet.add("KEY_ALIAS");
        }
        orgConfigAttributes.put("iplanet-am-auth-key-alias", orgConfigSet);
        given(coreServicesWrapper.getAdminToken()).willReturn(adminToken);
        given(coreServicesWrapper.getServiceConfigManager("iPlanetAMAuthService", adminToken))
                .willReturn(serviceConfigManager);
        given(serviceConfigManager.getOrganizationConfig(orgName, null)).willReturn(serviceConfig);
        given(serviceConfig.getAttributes()).willReturn(orgConfigAttributes);
    }

    @Test
    public void shouldCreateAuthId() throws SignatureException, SMSException, SSOException {

        //Given
        LoginConfiguration loginConfiguration = mock(LoginConfiguration.class);
        AuthContextLocalWrapper authContext = mock(AuthContextLocalWrapper.class);

        given(authContext.getOrgDN()).willReturn("ORG_DN");
        given(authContext.getSessionID()).willReturn(new SessionID("SESSION_ID"));
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.NONE);
        given(loginConfiguration.getIndexValue()).willReturn(null);

        mockGetKeyAliasMethod("ORG_DN", false);

        PrivateKey privateKey = mock(PrivateKey.class);
        given(amKeyProvider.getPrivateKey("KEY_ALIAS")).willReturn(privateKey);

        //When
        String authId = authIdHelper.createAuthId(loginConfiguration, authContext);

        //Then
        assertNotNull(authId);
        verify(plaintextJwt).header("alg", JwsAlgorithm.HS256.toString());
        verify(plaintextJwt).content(eq("otk"), anyString());
        ArgumentCaptor<Map> contentArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(plaintextJwt).content(contentArgumentCaptor.capture());
        Map jwtContent = contentArgumentCaptor.getValue();
        assertTrue(jwtContent.containsKey("realm"));
        assertTrue(jwtContent.containsValue("ORG_DN"));
        assertTrue(jwtContent.containsKey("sessionId"));
        assertTrue(jwtContent.containsValue("SESSION_ID"));
        assertFalse(jwtContent.containsKey("authIndexType"));
        assertFalse(jwtContent.containsKey("authIndexValue"));
    }

    @Test
    public void shouldCreateAuthIdIncludingAuthIndexTypeAndValue() throws SignatureException, SMSException,
            SSOException {

        //Given
        LoginConfiguration loginConfiguration = mock(LoginConfiguration.class);
        AuthContextLocalWrapper authContext = mock(AuthContextLocalWrapper.class);

        given(authContext.getOrgDN()).willReturn("ORG_DN");
        given(authContext.getSessionID()).willReturn(new SessionID("SESSION_ID"));
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.SERVICE);
        given(loginConfiguration.getIndexValue()).willReturn("INDEX_VALUE");

        mockGetKeyAliasMethod("ORG_DN", false);

        PrivateKey privateKey = mock(PrivateKey.class);
        given(amKeyProvider.getPrivateKey("KEY_ALIAS")).willReturn(privateKey);

        //When
        String authId = authIdHelper.createAuthId(loginConfiguration, authContext);

        //Then
        assertNotNull(authId);
        verify(plaintextJwt).header("alg", JwsAlgorithm.HS256.toString());
        verify(plaintextJwt).content(eq("otk"), anyString());
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(plaintextJwt).content(argumentCaptor.capture());
        Map jwtValues = argumentCaptor.getValue();
        assertTrue(jwtValues.containsKey("realm"));
        assertTrue(jwtValues.containsValue("ORG_DN"));
        assertTrue(jwtValues.containsKey("sessionId"));
        assertTrue(jwtValues.containsValue("SESSION_ID"));
        assertTrue(jwtValues.containsKey("authIndexType"));
        assertTrue(jwtValues.containsValue(AuthIndexType.SERVICE.getIndexType().toString()));
        assertTrue(jwtValues.containsKey("authIndexValue"));
        assertTrue(jwtValues.containsValue("INDEX_VALUE"));
    }

    @Test
    public void shouldThrowExceptionWhenGeneratingAuthIdAndKeyAliasIsNull() throws SSOException, SMSException,
            SignatureException {

        //Given
        LoginConfiguration loginConfiguration = mock(LoginConfiguration.class);
        AuthContextLocalWrapper authContext = mock(AuthContextLocalWrapper.class);

        given(authContext.getOrgDN()).willReturn("ORG_DN");
        given(authContext.getSessionID()).willReturn(new SessionID("SESSION_ID"));
        given(loginConfiguration.getIndexType()).willReturn(AuthIndexType.NONE);
        given(loginConfiguration.getIndexValue()).willReturn(null);

        mockGetKeyAliasMethod("ORG_DN", true);

        //When
        boolean exceptionCaught = false;
        try {
            authIdHelper.createAuthId(loginConfiguration, authContext);
            fail();
        } catch (RestAuthException e) {
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
        verify(amKeyProvider, never()).getPrivateKey(anyString());
    }

    @Test
    public void shouldThrowSMSExceptionWhenFailToGetOrgConfig() throws SSOException, SMSException,
            SignatureException {

        //Given
        LoginConfiguration loginConfiguration = mock(LoginConfiguration.class);
        AuthContextLocalWrapper authContext = mock(AuthContextLocalWrapper.class);

        given(coreServicesWrapper.getServiceConfigManager("iPlanetAMAuthService", null)).willThrow(SMSException.class);

        //When
        boolean exceptionCaught = false;
        RestAuthException exception = null;
        try {
            authIdHelper.createAuthId(loginConfiguration, authContext);
            fail();
        } catch (RestAuthException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getResponse().getStatus(), 500);
        verify(amKeyProvider, never()).getPrivateKey(anyString());
    }

    @Test
    public void shouldThrowSSOExceptionWhenFailToGetOrgConfig() throws SSOException, SMSException,
            SignatureException {

        //Given
        LoginConfiguration loginConfiguration = mock(LoginConfiguration.class);
        AuthContextLocalWrapper authContext = mock(AuthContextLocalWrapper.class);

        given(coreServicesWrapper.getServiceConfigManager("iPlanetAMAuthService", null)).willThrow(SSOException.class);

        //When
        boolean exceptionCaught = false;
        RestAuthException exception = null;
        try {
            authIdHelper.createAuthId(loginConfiguration, authContext);
            fail();
        } catch (RestAuthException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getResponse().getStatus(), 500);
        verify(amKeyProvider, never()).getPrivateKey(anyString());
    }

    @Test
    public void shouldReconstructAuthId() {

        //Given

        //When
        authIdHelper.reconstructAuthId("AUTH_ID");

        //Then
        verify(jwtBuilder).recontructJwt("AUTH_ID");
    }

    @Test
    public void shouldVerifyAuthId() throws SignatureException, SSOException, SMSException {

        //Given
        SignedJwt signedJwt = mock(SignedJwt.class);
        PrivateKey privateKey = mock(PrivateKey.class);
        X509Certificate certificate = mock(X509Certificate.class);

        given(jwtBuilder.recontructJwt("AUTH_ID")).willReturn(signedJwt);
        given(signedJwt.verify(privateKey, certificate)).willReturn(true);
        given(amKeyProvider.getPrivateKey("KEY_ALIAS")).willReturn(privateKey);
        given(amKeyProvider.getX509Certificate("KEY_ALIAS")).willReturn(certificate);

        mockGetKeyAliasMethod("REALM_DN", false);

        //When
        authIdHelper.verifyAuthId("REALM_DN", "AUTH_ID");

        //Then
        verify(jwtBuilder).recontructJwt("AUTH_ID");
        verify(signedJwt).verify(privateKey, certificate);
    }

    @Test
    public void shouldVerifyAuthIdAndFail() throws SignatureException, SSOException, SMSException {

        //Given
        SignedJwt signedJwt = mock(SignedJwt.class);
        PrivateKey privateKey = mock(PrivateKey.class);
        X509Certificate certificate = mock(X509Certificate.class);

        given(jwtBuilder.recontructJwt("AUTH_ID")).willReturn(signedJwt);
        given(signedJwt.verify(privateKey, certificate)).willReturn(false);
        given(amKeyProvider.getPrivateKey("KEY_ALIAS")).willReturn(privateKey);
        given(amKeyProvider.getX509Certificate("KEY_ALIAS")).willReturn(certificate);

        mockGetKeyAliasMethod("REALM_DN", false);

        //When
        boolean exceptionCaught = false;
        try {
            authIdHelper.verifyAuthId("REALM_DN", "AUTH_ID");
            fail();
        } catch (SignatureException e) {
            exceptionCaught = true;
        }

        //Then
        verify(jwtBuilder).recontructJwt("AUTH_ID");
        verify(signedJwt).verify(privateKey, certificate);
        assertTrue(exceptionCaught);
    }
}
