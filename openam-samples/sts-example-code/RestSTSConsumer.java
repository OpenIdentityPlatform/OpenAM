/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file at legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.forgerock.openam.functionaltest.sts.frmwk.rest;
 
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCancellationException;
import org.forgerock.openam.sts.TokenCreationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.TokenTypeId;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.user.invocation.OpenAMTokenState;
import org.forgerock.openam.sts.user.invocation.OpenIdConnectTokenCreationState;
import org.forgerock.openam.sts.user.invocation.OpenIdConnectTokenState;
import org.forgerock.openam.sts.user.invocation.ProofTokenState;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenCancellationInvocationState;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenTranslationInvocationState;
import org.forgerock.openam.sts.user.invocation.RestSTSTokenValidationInvocationState;
import org.forgerock.openam.sts.user.invocation.SAML2TokenCreationState;
import org.forgerock.openam.sts.user.invocation.SAML2TokenState;
import org.forgerock.openam.sts.user.invocation.UsernameTokenState;
import org.forgerock.openam.sts.user.invocation.X509TokenState;
import org.forgerock.openam.sts.token.SAML2SubjectConfirmation;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.util.encode.Base64;
 
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
 
/**
 * This class demonstrates consumption of rest-sts token transformations.
 */
public class RestSTSConsumer {
    private final URL restSTSInstanceTranslateUrl;
    private final URL restSTSInstanceValidateUrl;
    private final URL restSTSInstanceCancelUrl;
    private final Logger logger;
 
    /**
     *
     * @param restSTSInstanceTranslateUrl The full url of the rest-sts instance, with the translate action specfied.
     *                                    For example, for a rest-sts instance with a deployment url of instanceId, published to the
     *                                    root realm, the url would be: http://amhost.com:8080/openam/rest-sts/instanceId?_action=translate
     *                                    A rest-sts instance published to realm fred with a deployment url of instanceId2, the
     *                                    url would be: http://amhost.com:8080/openam/rest-sts/fred/instanceId2?_action=translate
     *                                    A rest-sts instance published to realm bobo, a sub-realm of fred, with a deployment url of
     *                                    instanceId3, the url would be: http://amhost.com:8080/openam/rest-sts/fred/bobo/instanceId3?_action=translate
     * @param restSTSInstanceValidateUrl The full url of the rest-sts instance, with the translate action specfied.
     *                                    For example, for a rest-sts instance with a deployment url of instanceId, published to the
     *                                    root realm, the url would be: http://amhost.com:8080/openam/rest-sts/instanceId?_action=validate
     *                                    A rest-sts instance published to realm fred with a deployment url of instanceId2, the
     *                                    url would be: http://amhost.com:8080/openam/rest-sts/fred/instanceId2?_action=validate
     *                                    A rest-sts instance published to realm bobo, a sub-realm of fred, with a deployment url of
     *                                    instanceId3, the url would be: http://amhost.com:8080/openam/rest-sts/fred/bobo/instanceId3?_action=validate
     * @throws MalformedURLException In case the specified restSTSInstanceTranslateUrl is mal-formed.
     */
    @Inject
    public RestSTSConsumer(String restSTSInstanceTranslateUrl, String restSTSInstanceValidateUrl,
                           String restSTSInstanceCancelUrl, Logger logger) throws MalformedURLException {
        this.restSTSInstanceTranslateUrl = new URL(restSTSInstanceTranslateUrl);
        this.restSTSInstanceValidateUrl = new URL(restSTSInstanceValidateUrl);
        this.restSTSInstanceCancelUrl = new URL(restSTSInstanceCancelUrl);
        this.logger = logger;
        this.logger.log(Level.FINE, "RestSTSConsumer will consume the REST STS at url: " + this.restSTSInstanceTranslateUrl.toString());
    }
 
    /**
     * Invokes a UsernameToken->SAML2 token transformation.
     *
     * Sample json posted at the rest-sts instance in this method:
     *
     { "input_token_state": { "token_type": "USERNAME", "username": "unt_user1767572069", "password": "password" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "BEARER" } }
 
     { "input_token_state": { "token_type": "USERNAME", "username": "unt_user1683257432", "password": "password" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "HOLDER_OF_KEY", "proof_token_state": { "base64EncodedCertificate": "MIICQDCCAakCBEeNB0...wWigmrW0Y0Q==" } } }
 
     { "input_token_state": { "token_type": "USERNAME", "username": "unt_user1683257432", "password": "password" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "SENDER_VOUCHES" } }
     *
     * @param username the username in the UsernameToken
     * @param password the password in the UsernameToken
     * @param subjectConfirmation The SAML2 SubjectConfirmation. For HoK, the certificate in the file /cert.jks on the
     *                            classpath will be included.
     * @return The string representation of the SAML2 Assertion
     * @throws Exception If transformation fails
     */
    public String transformUntToSAML2(String username, String password,
                                      SAML2SubjectConfirmation subjectConfirmation, X509Certificate hokProofCert)
                                        throws IOException {
        UsernameTokenState untState = UsernameTokenState.builder()
                .username(username.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .password(password.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
 
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(untState.toJson())
                .outputTokenState(buildSAML2TokenCreationState(subjectConfirmation, hokProofCert).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    public String transformCustomTokenToOIDC(JsonValue customTokenInput) throws IOException {
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(customTokenInput)
                .outputTokenState(buildOpenIdConnectTokenCreationState("faux_nonce").toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    public String transformCustomTokenToCustomToken(JsonValue customTokenInput, JsonValue customTokenOutput) throws IOException {
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(customTokenInput)
                .outputTokenState(customTokenOutput)
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    public String transformUntToCustomToken(String username, String password, JsonValue customTokenOutput) throws IOException {
        UsernameTokenState untState = UsernameTokenState.builder()
                .username(username.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .password(password.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(untState.toJson())
                .outputTokenState(customTokenOutput)
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    /*
    example invocation state:
    { "input_token_state": { "token_type": "USERNAME", "username": "unt_user1767572069", "password": "password" }, "output_token_state": { "token_type": "OPENIDCONNECT", "nonce": "521828576", "allow_access": true } }
    */
    public String transformUntToOIDC(String username, String password,
                                      String nonce) throws IOException {
        UsernameTokenState untState = UsernameTokenState.builder()
                .username(username.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .password(password.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                .build();
 
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(untState.toJson())
                .outputTokenState(buildOpenIdConnectTokenCreationState(nonce).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    /*
    Transforming oidc->oidc does make sense - an oidc token issued by e.g. google can be transformed into an oidc token
    intended for another audience. In this way, OpenAM can function as a 'meta' IdP - it can accept tokens from other
    IdPs (google), authenticate them, and act as an idp creating a oidc token for another sp.
 
    { "input_token_state": { "token_type": "OPENIDCONNECT", "oidc_id_token": "eyAidHlwIjogIkpXVCIsICJhbGciOiAiSFMyNTYiIH0.eyAidG9rZW5OYW1AxNDQ4MDUzNjkz...pZW50IiBdIH0.YJFTbrlyAoZ3JP--zfcr8TwG_B0q6bPeUt0bBrx7bEw" }, "output_token_state": { "token_type": "OPENIDCONNECT", "nonce": "247885108", "allow_access": true } }
     */
    public String transformOIDCToOIDC(String oidcTokenValue, String nonce) throws IOException {
        OpenIdConnectTokenState oidcTokenState = OpenIdConnectTokenState.builder().tokenValue(oidcTokenValue).build();
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(oidcTokenState.toJson())
                .outputTokenState(buildOpenIdConnectTokenCreationState(nonce).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
    /**
     * Invokes a OpenAMToken->SAML2 token transformation.
     *
     * Sample json posted at the rest-sts instance in this method:
     * { "input_token_state": { "token_type": "OPENAM", "session_id": "AQIC5wM...1MjYyAAJTMQAA*" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "BEARER" } }
     *
     * { "input_token_state": { "token_type": "OPENAM", "session_id": "AQIC5...TQ1MjYyAAJTMQAA*" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "HOLDER_OF_KEY", "proof_token_state": { "base64EncodedCertificate": "MIICQDCCAakCB...wWigmrW0Y0Q==" } } }
 
     * { "input_token_state": { "token_type": "OPENAM", "session_id": "AQIC5...TMQAA*" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "SENDER_VOUCHES" } }
     *
     * @param sessionId the OpenAM session ID. Corresponds to the iPlanetDirectoryPro (or equivalent) cookie.
     * @param subjectConfirmation The SAML2 SubjectConfirmation. For HoK, the certificate in the file /cert.jks on the
     *                            classpath will be included.
     * @return The string representation of the SAML2 Assertion
     * @throws Exception If transformation fails
     */
    public String transformOpenAMToSAML2(String sessionId,
                                         SAML2SubjectConfirmation subjectConfirmation, X509Certificate hokProofCert)
                                            throws IOException {
        OpenAMTokenState sessionState = OpenAMTokenState.builder().sessionId(sessionId).build();
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(sessionState.toJson())
                .outputTokenState(buildSAML2TokenCreationState(subjectConfirmation, hokProofCert).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    /*
    Example invocation state:
    { "input_token_state": { "token_type": "OPENAM", "session_id": "AQIC5wM2...TMQAA*" }, "output_token_state": { "token_type": "OPENIDCONNECT", "nonce": "471564333", "allow_access": true } }
    */
    public String transformOpenAMToOIDC(String sessionId,
                                         String nonce) throws IOException {
        OpenAMTokenState sessionState = OpenAMTokenState.builder().sessionId(sessionId).build();
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(sessionState.toJson())
                .outputTokenState(buildOpenIdConnectTokenCreationState(nonce).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    /**
     * Invokes a OIDCToken->SAML2 token transformation
     * Sample json posted at the rest-sts instance in this method (HoK SubjectConfirmation, with token elements truncated):
     { "input_token_state": { "token_type": "OPENIDCONNECT", "oidc_id_token": "eyAiYWxQ.euTNnNDExNTkyMjEyIH0.kuNlKwyvZJqaC8EYpDyPJMiEcII" },"output_token_state": { "token_type": "SAML2", "subject_confirmation": "HOLDER_OF_KEY", "proof_token_state": { "base64EncodedCertificate": "MIMbFAAOBjQAwgYkCgYEArSQ...c/U75GB2AtKhbGS5pimrW0Y0Q==" } } }
 
     { "input_token_state": { "token_type": "OPENIDCONNECT", "oidc_id_token": "eyAidHlwIjogIkpXVCIsICJhbGciOiAiSFMyNTYiIH0.eyAidG9rZW5OYW1lIjogImlkX3...gMTQ0ODA1MzY52xpZW50IiBdIH0.yKVp4kInTR-6TZGL3cjvA-adhbIfLqjf8E7ZQWHCm9c" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "BEARER" } }
 
     { "input_token_state": { "token_type": "OPENIDCONNECT", "oidc_id_token": "eyAidHlwIjogIkpXVCIsICJhbGciOiAiSFMyNTYiIH0.eyAidG9rZW5O...Q2xpZW50IiBdIH0.yKVp4kInTR-6TZGL3cjvA-adhbIfLqjf8E7ZQWHCm9c" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "SENDER_VOUCHES" } }
 
     To set up oauth2, you have to:
     1. configure the OAuth2 provider using the common tasks
     2. then create a client using the agents tab - reflecting the client id and the redirect uri defined in my
     integration test. Also add the openid scope
     2.5. Make sure you specify a hmac-based based signing in the OAuth2 client.
     3. then create the oidc module - the issuer should be the url of the openam deployment, with oauth2 appended (e.g.
     http://macbook.dirk.internal.forgerock.com:8080/openam/oauth2), the type should be
     client-secret, and the secret itself should be that configured for the oauth2 client - we are authN-ing a HMAC-signed jwt.
     4. if the OpenAM provider is issuing the oidc token, then the azp and aud in the oidc module should be set to
     the name of the oauth2 client
     * @param oidcTokenValue the OpenIdConnect ID token. Note that the targeted rest-sts instance has to be deployed with
     *                       a AuthTargetMapping which references an instance of the OIDC module with configuration state
     *                       necessary to validate an OIDC token from a specific issuer.
     * @param subjectConfirmation The SAML2 SubjectConfirmation. For HoK, the certificate in the file /cert.jks on the
     *                            classpath will be included.
     * @param hokProofCert The X509Certificate used as the HolderOfKey proof cert. Null if non-HolderOfKey SubjectConfirmation
     *                     is specified.
     * @return The string representation of the SAML2 Assertion
     * @throws IOException If transformation fails
     */
    public String transformOpenIdConnectToSAML2(SAML2SubjectConfirmation subjectConfirmation, String oidcTokenValue,
                                                X509Certificate hokProofCert)
            throws IOException {
        if (oidcTokenValue == null) {
            throw new IOException("OIDC token is null!");
        }
        OpenIdConnectTokenState tokenState = OpenIdConnectTokenState.builder().tokenValue(oidcTokenValue).build();
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(tokenState.toJson())
                .outputTokenState(buildSAML2TokenCreationState(subjectConfirmation, hokProofCert).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString());
    }
 
    /**
     * Invokes a X509->SAML2 token transformation
     *
     * Sample json posted at the rest-sts instance in this method:
     * { "input_token_state": { "token_type": "X509" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "SENDER_VOUCHES" } }
     *
     * { "input_token_state": { "token_type": "X509" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "BEARER" } }
     *
       { "input_token_state": { "token_type": "X509" }, "output_token_state": { "token_type": "SAML2", "subject_confirmation": "HOLDER_OF_KEY", "proof_token_state": { "base64EncodedCertificate": "MIICQDCCAakCBEeNB0swDQYJKoZIhvcNAQEEB...Fax0JDC/FfwWigmrW0Y0Q==" } } }
     *
     * Note that the caller's X509 token must be specified either 1. in a header specified in the publshed rest-sts instance and invoked
     * from one of the trusted hosts, again specified in the published rest-sts instance, or 2. via two-way TLS.
     * Note that the targeted rest-sts module has to be deployed with an AuthTargetMapping which reference an instance
     * of the Certificate module configured to reference the client's certificate from the header specified in the
     * AuthTargetMapping, and configured to trust the local OpenAM instance. In addition, the
     * 'Certificate Field Used to Access User Profile' should be set to subject CN. The CN
     * used in the test cert deployed with OpenAM, and used in this integration test, is 'test', so a subject with a uid of
     * 'test' has to be created for account mapping to work.
     * Likewise the published rest-sts instance
     * must also be configured to trust the host running this test, and must be configured to reference the client's certificate
     * in the header specified by stsClientCertHeaderName (unless the rest-sts is being consumed via two-way-tls, in which
     * case the stsClientCertHeaderName is irrelevant, as the rest-sts will reference the client's certificate via the
     * javax.servlet.request.X509Certificate ServletRequest attribute.
     * @param subjectConfirmation The SAML2 SubjectConfirmation. For HoK, the certificate in the file /cert.jks on the
     *                            classpath will be included.
     * @param stsClientCertHeaderName The header name specification of where the published sts expects to find the client
     *                                cert (must correspond to published sts state)
     * @param clientCertificate the X509 Certificate that will be authenticated to establish the caller's identity
     * @param hokProofCert For SAML2 assertions with HoK SubjectConfirmation, the holder's x509 certificate. Null for non-HoK
     *                     SubjectConfirmations
     * @return The string representation of the SAML2 Assertion
     * @throws IOException If transformation fails
     */
    public String transformX509ToSAML2(SAML2SubjectConfirmation subjectConfirmation, String stsClientCertHeaderName,
                                       X509Certificate clientCertificate, X509Certificate hokProofCert)
            throws IOException {
        X509TokenState tokenState = new X509TokenState();
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(tokenState.toJson())
                .outputTokenState(buildSAML2TokenCreationState(subjectConfirmation, hokProofCert).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString(), stsClientCertHeaderName, clientCertificate);
    }
 
    /*
    Example invocation state:
     * Note that the caller's X509 token must be specified either 1. in a header specified in the publshed rest-sts instance and invoked
     * from one of the trusted hosts, again specified in the published rest-sts instance, or 2. via two-way TLS.
    { "input_token_state": { "token_type": "X509" }, "output_token_state": { "token_type": "OPENIDCONNECT", "nonce": "2003851386", "allow_access": true } }
 
     */
    public String transformX509ToOIDC(String nonce, String stsClientCertHeaderName, X509Certificate clientCertificate)
            throws IOException {
        X509TokenState tokenState = new X509TokenState();
        RestSTSTokenTranslationInvocationState invocationState = RestSTSTokenTranslationInvocationState.builder()
                .inputTokenState(tokenState.toJson())
                .outputTokenState(buildOpenIdConnectTokenCreationState(nonce).toJson())
                .build();
        return invokeTokenTranslation(invocationState.toJson().toString(), stsClientCertHeaderName, clientCertificate);
    }
 
    private SAML2TokenCreationState buildSAML2TokenCreationState(SAML2SubjectConfirmation subjectConfirmation,
                                                                 X509Certificate hokProofCert) throws IOException {
        if (SAML2SubjectConfirmation.HOLDER_OF_KEY.equals(subjectConfirmation)) {
            ProofTokenState proofTokenState = ProofTokenState.builder().x509Certificate(hokProofCert).build();
            return SAML2TokenCreationState.builder()
                    .saml2SubjectConfirmation(subjectConfirmation)
                    .proofTokenState(proofTokenState)
                    .build();
 
        } else {
            return SAML2TokenCreationState.builder()
                    .saml2SubjectConfirmation(subjectConfirmation)
                    .build();
        }
    }
    /*
    Example invocation state:
    Note: full token, as returned by transform operation, must be included in the invocation. Token details shortened below.
    { "validated_token_state": { "token_type": "OPENIDCONNECT", "oidc_id_token": "eyAidHlwIjogIkpXVCIsICJhbGciOiAiSFMyNTYiIH0.eyAiYXV0...LWE0ZmUtNDQ2YmZhODIzNGEyIiB9.TjhBCB9K64dHoDiBh7I5RWkJ6_y_EEjInuCBiD3F3tc" } }
 
    { "validated_token_state": { "token_type": "SAML2", "saml2_token": "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ...</saml:AuthnContext></saml:AuthnStatement></saml:Assertion>" } }
 
     */
    public boolean isTokenValid(TokenTypeId tokenType, String validatedToken) throws IOException {
        RestSTSTokenValidationInvocationState invocationState;
        if (TokenType.SAML2.getId().equals(tokenType.getId())) {
            invocationState = RestSTSTokenValidationInvocationState.builder()
                    .validatedTokenState(SAML2TokenState.builder().tokenValue(validatedToken).build().toJson())
                    .build();
        } else if (TokenType.OPENIDCONNECT.getId().equals(tokenType.getId())) {
            invocationState = RestSTSTokenValidationInvocationState.builder()
                    .validatedTokenState(OpenIdConnectTokenState.builder().tokenValue(validatedToken).build().toJson())
                    .build();
        } else {
            throw new TokenValidationException(ResourceException.BAD_REQUEST, "Invalid integration test invocation: " +
                    "cannot validate token of type: " + tokenType.getId());
        }
        final String response = invokeTokenValidation(invocationState.toJson().toString());
        return parseTokenValidationResponse(response);
    }
 
    /*
    Example invocation state:
    Note: full token, as returned by transform operation, must be included in the invocation. Token details shortened below.
    { "cancelled_token_state": { "token_type": "OPENIDCONNECT", "oidc_id_token": "eyAidHlwIjogIkpXVCIsICJhbGciOiAiSFMyNTYiIH0.eyAiYXV0...LWE0ZmUtNDQ2YmZhODIzNGEyIiB9.TjhBCB9K64dHoDiBh7I5RWkJ6_y_EEjInuCBiD3F3tc" } }
 
    { "cancelled_token_state": { "token_type": "SAML2", "saml2_token": ""<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ...</saml:AuthnContext></saml:AuthnStatement></saml:Assertion>" } }
 
     */
    public void cancelToken(TokenTypeId tokenType, String cancelledToken) throws IOException {
        RestSTSTokenCancellationInvocationState invocationState;
        if (TokenType.SAML2.getId().equals(tokenType.getId())) {
            invocationState = RestSTSTokenCancellationInvocationState.builder()
                    .cancelledTokenState(SAML2TokenState.builder().tokenValue(cancelledToken).build().toJson())
                    .build();
        } else if (TokenType.OPENIDCONNECT.getId().equals(tokenType.getId())) {
            invocationState = RestSTSTokenCancellationInvocationState.builder()
                    .cancelledTokenState(OpenIdConnectTokenState.builder().tokenValue(cancelledToken).build().toJson())
                    .build();
        } else {
            throw new TokenCancellationException(ResourceException.BAD_REQUEST, "Invalid integration test invocation: " +
                    "cannot validate token of type: " + tokenType.getId());
        }
        invokeTokenCancellation(invocationState.toJson().toString());
    }
 
    private OpenIdConnectTokenCreationState buildOpenIdConnectTokenCreationState(String nonce) {
        return OpenIdConnectTokenCreationState.builder().nonce(nonce).allowAccess(true).build();
    }
 
    private String invokeTokenTranslation(String invocationPayload)
            throws IOException {
        return invokeTokenTranslation(invocationPayload, null, null);
 
    }
 
    private String invokeTokenTranslation(String invocationPayload, String stsClientCertHeaderName, X509Certificate userCertificate)
            throws IOException {
        logger.log(Level.FINE, "Invoking token translation with the following payload: " + invocationPayload);
        HttpURLConnection connection = (HttpURLConnection) restSTSInstanceTranslateUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
 
        if (stsClientCertHeaderName != null) {
            try {
                connection.setRequestProperty(stsClientCertHeaderName, Base64.encode(userCertificate.getEncoded()));
            } catch (CertificateEncodingException e) {
                throw new IOException("Exception encoding user certificate: " + e.getMessage(), e);
            }
        }
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();
 
        final int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return parseTokenTranslationResponse(getSuccessMessage(connection));
        } else {
            throw new TokenCreationException(responseCode, getErrorMessage(connection));
        }
    }
 
    private String invokeTokenValidation(String invocationPayload)
            throws IOException {
        logger.log(Level.FINE, "Invoking token validation on url " + restSTSInstanceValidateUrl + " with payload: " + invocationPayload);
        HttpURLConnection connection = (HttpURLConnection) restSTSInstanceValidateUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
 
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();
 
        final int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return getSuccessMessage(connection);
        } else {
            throw new TokenValidationException(responseCode, getErrorMessage(connection));
        }
    }
 
    private String invokeTokenCancellation(String invocationPayload) throws IOException {
        logger.log(Level.FINE, "Invoking token cancellation on url " + restSTSInstanceCancelUrl + " with payload: " + invocationPayload);
        HttpURLConnection connection = (HttpURLConnection) restSTSInstanceCancelUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(SharedSTSConstants.CONTENT_TYPE, SharedSTSConstants.APPLICATION_JSON);
 
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(invocationPayload);
        writer.close();
        final int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return getSuccessMessage(connection);
        } else {
            throw new TokenCancellationException(responseCode, getErrorMessage(connection));
        }
    }
 
    private String getSuccessMessage(HttpURLConnection connection) throws IOException {
        return readInputStream(connection.getInputStream());
    }
 
    private String getErrorMessage(HttpURLConnection connection) throws IOException {
        if (connection.getErrorStream() != null) {
            return readInputStream(connection.getErrorStream());
        } else {
            return readInputStream(connection.getInputStream());
        }
    }
 
    private String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "Empty error stream";
        } else {
            return IOUtils.readStream(inputStream);
        }
    }
 
    private String parseTokenTranslationResponse(String response) throws TokenCreationException {
        Object responseContent;
        try {
            JsonParser parser =
                    new ObjectMapper().getFactory().createParser(response);
            responseContent = parser.readValueAs(Object.class);
        } catch (IOException e) {
            throw new TokenCreationException(500, "Could not map the response from the rest sts instance at url "  + restSTSInstanceTranslateUrl +
                    " to a json object. The response: " + response + "; The exception: " + e);
        }
        JsonValue assertionJson = new JsonValue(responseContent).get(AMSTSConstants.ISSUED_TOKEN);
        if (assertionJson.isNull() || !assertionJson.isString()) {
            throw new TokenCreationException(500, "The json response returned from the rest-sts instance at url " + restSTSInstanceTranslateUrl +
                    " did not have a non-null string element for the " + AMSTSConstants.ISSUED_TOKEN + " key. The json: "
                            + responseContent.toString());
        }
        return assertionJson.asString();
    }
 
    private boolean parseTokenValidationResponse(String response) throws TokenValidationException {
        Object responseContent;
        try {
            JsonParser parser =
                    new ObjectMapper().getFactory().createParser(response);
            responseContent = parser.readValueAs(Object.class);
        } catch (IOException e) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "Could not map the response from the rest sts instance at url "  + restSTSInstanceValidateUrl +
                    " to a json object. The response: " + response + "; The exception: " + e);
        }
        JsonValue assertionJson = new JsonValue(responseContent).get(AMSTSConstants.TOKEN_VALID);
        if (assertionJson.isNull() || !assertionJson.isBoolean()) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "The json response returned from the rest-sts instance at url " + restSTSInstanceValidateUrl +
                    " did not have a non-null string element for the " + AMSTSConstants.TOKEN_VALID + " key. The json: "
                    + responseContent.toString());
        }
        return assertionJson.asBoolean();
    }
}