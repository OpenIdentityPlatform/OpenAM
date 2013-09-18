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
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.builders.JwsHeaderBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.forgerockrest.authn.core.AuthIndexType;
import org.forgerock.openam.forgerockrest.authn.core.LoginConfiguration;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.AuthContextLocalWrapper;
import org.forgerock.openam.forgerockrest.authn.core.wrappers.CoreServicesWrapper;
import org.forgerock.openam.forgerockrest.authn.exceptions.RestAuthException;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.security.Key;
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
    private JwtBuilderFactory jwtBuilderFactory;

    private JwsHeaderBuilder jwsHeaderBuilder;
    private JwtClaimsSetBuilder claimsSetBuilder;

    @BeforeMethod
    public void setUp() {

        coreServicesWrapper = mock(CoreServicesWrapper.class);
        amKeyProvider = mock(AMKeyProvider.class);
        jwtBuilderFactory = mock(JwtBuilderFactory.class);

        authIdHelper = new AuthIdHelper(coreServicesWrapper, amKeyProvider, jwtBuilderFactory);

        jwsHeaderBuilder = mock(JwsHeaderBuilder.class);
        claimsSetBuilder = mock(JwtClaimsSetBuilder.class);
        JwtClaimsSet claimsSet = mock(JwtClaimsSet.class);
        SignedJwtBuilderImpl signedJwtBuilder = mock(SignedJwtBuilderImpl.class);

        given(jwtBuilderFactory.claims()).willReturn(claimsSetBuilder);
        given(claimsSetBuilder.claim(anyString(), anyObject())).willReturn(claimsSetBuilder);
        given(claimsSetBuilder.claims(anyMap())).willReturn(claimsSetBuilder);
        given(claimsSetBuilder.build()).willReturn(claimsSet);


        given(jwtBuilderFactory.jws(Matchers.<Key>anyObject())).willReturn(signedJwtBuilder);
        given(signedJwtBuilder.headers()).willReturn(jwsHeaderBuilder);
        given(jwsHeaderBuilder.alg(Matchers.<Algorithm>anyObject())).willReturn(jwsHeaderBuilder);
        given(jwsHeaderBuilder.done()).willReturn(signedJwtBuilder);
        given(signedJwtBuilder.claims(claimsSet)).willReturn(signedJwtBuilder);

        given(signedJwtBuilder.build()).willReturn("JWT_STRING");
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
        verify(jwsHeaderBuilder).alg(JwsAlgorithm.HS256);
        verify(claimsSetBuilder).claim(eq("otk"), anyString());
        ArgumentCaptor<Map> contentArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(claimsSetBuilder).claims(contentArgumentCaptor.capture());
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
        verify(jwsHeaderBuilder).alg(JwsAlgorithm.HS256);
        verify(claimsSetBuilder).claim(eq("otk"), anyString());
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(claimsSetBuilder).claims(argumentCaptor.capture());
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
        verify(jwtBuilderFactory).reconstruct("AUTH_ID", SignedJwt.class);
    }

    @Test
    public void shouldThrowRestAuthExceptionWhenReconstructingAuthIdFails() {

        //Given
        given(jwtBuilderFactory.reconstruct("AUTH_ID", SignedJwt.class)).willThrow(JwtRuntimeException.class);

        //When
        RestAuthException exception = null;
        boolean exceptionCaught = false;
        try {
            authIdHelper.reconstructAuthId("AUTH_ID");
            fail();
        } catch (RestAuthException e) {
            exception = e;
            exceptionCaught = true;
        }

        //Then
        assertTrue(exceptionCaught);
        Response response = exception.getResponse();
        assertEquals(response.getStatus(), 400);
        JsonValue jsonValue = JsonValueBuilder.toJsonValue((String) response.getEntity());
        assertTrue(jsonValue.isDefined("errorMessage"));
    }

    @Test
    public void shouldVerifyAuthId() throws SignatureException, SSOException, SMSException {

        //Given
        SignedJwt signedJwt = mock(SignedJwt.class);
        PrivateKey privateKey = mock(PrivateKey.class);

        given(jwtBuilderFactory.reconstruct("AUTH_ID", SignedJwt.class)).willReturn(signedJwt);
        given(signedJwt.verify(privateKey)).willReturn(true);
        given(amKeyProvider.getPrivateKey("KEY_ALIAS")).willReturn(privateKey);

        mockGetKeyAliasMethod("REALM_DN", false);

        //When
        authIdHelper.verifyAuthId("REALM_DN", "AUTH_ID");

        //Then
        verify(jwtBuilderFactory).reconstruct("AUTH_ID", SignedJwt.class);
        verify(signedJwt).verify(privateKey);
    }

    @Test
    public void shouldVerifyAuthIdAndFail() throws SignatureException, SSOException, SMSException {

        //Given
        SignedJwt signedJwt = mock(SignedJwt.class);
        PrivateKey privateKey = mock(PrivateKey.class);

        given(jwtBuilderFactory.reconstruct("AUTH_ID", SignedJwt.class)).willReturn(signedJwt);
        given(signedJwt.verify(privateKey)).willReturn(false);
        given(amKeyProvider.getPrivateKey("KEY_ALIAS")).willReturn(privateKey);

        mockGetKeyAliasMethod("REALM_DN", false);

        //When
        boolean exceptionCaught = false;
        try {
            authIdHelper.verifyAuthId("REALM_DN", "AUTH_ID");
            fail();
        } catch (RestAuthException e) {
            exceptionCaught = true;
        }

        //Then
        verify(jwtBuilderFactory).reconstruct("AUTH_ID", SignedJwt.class);
        verify(signedJwt).verify(privateKey);
        assertTrue(exceptionCaught);
    }

    @Test
    public void shouldVerifyAuthIdAndFailWhenReconstructingJwt() throws SignatureException, SSOException, SMSException {

        //Given
        given(jwtBuilderFactory.reconstruct("AUTH_ID", SignedJwt.class)).willThrow(JwtRuntimeException.class);

        mockGetKeyAliasMethod("REALM_DN", false);

        //When
        boolean exceptionCaught = false;
        RestAuthException exception = null;
        try {
            authIdHelper.verifyAuthId("REALM_DN", "AUTH_ID");
            fail();
        } catch (RestAuthException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        Response response = exception.getResponse();
        assertEquals(response.getStatus(), 400);
        JsonValue jsonValue = JsonValueBuilder.toJsonValue((String) response.getEntity());
        assertTrue(jsonValue.isDefined("errorMessage"));
    }
}
