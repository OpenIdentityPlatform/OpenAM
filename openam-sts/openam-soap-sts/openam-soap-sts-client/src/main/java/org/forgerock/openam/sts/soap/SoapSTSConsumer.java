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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 * Portions Copyrighted 2025 3A-Systems LLC.
 */

package org.forgerock.openam.sts.soap;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.TrustException;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.soap.policy.am.OpenAMSessionTokenClientAssertionBuilder;
import org.forgerock.openam.sts.soap.policy.am.OpenAMSessionTokenClientInterceptorProvider;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class wraps the org.apache.cxf.ws.security.trust.STSClient to provide configuration state necessary to consume
 * published soap-sts instances. It is intended to demonstrate how to set the configuration state of the CXF sts client
 * in order to successfully invoke the 'standard' sts types published via OpenAM. It is not an exhaustive enumeration of
 * all configuration possibilities, nor sufficient to consume sts instances with custom SecurityPolicy bindings.
 */
public class SoapSTSConsumer {
    public static class SoapSTSConsumerBuilder {
        private String stsInstanceWsdlUrl;
        private String usernameTokenSupportingTokenUsername;
        private String clientSignatureKeyAlias;
        private String keystoreFile;
        private String keystorePassword;
        private String stsPublicKeyAlias;
        private boolean logMessages;
        private CallbackHandler callbackHander;

        /**
         * @param stsInstanceWsdlUrl the url where the sts wsdl can be obtained
         * @return the builder
         */
        public SoapSTSConsumerBuilder stsInstanceWsdlUrl(String stsInstanceWsdlUrl) {
            this.stsInstanceWsdlUrl = stsInstanceWsdlUrl;
            return this;
        }

        /**
         * @param usernameTokenSupportingTokenUsername The username corresponding to the UsernameToken which will be
         *                                             created to satisfy SecurityPolicy bindings with UsernameToken
         *                                             SupportingTokens. Note that the CallbackHandler will be consulted
         *                                             to provide the UsernameToken password.
         * @return the builder
         */
        public SoapSTSConsumerBuilder usernameTokenSupportingTokenUsername(String usernameTokenSupportingTokenUsername) {
            this.usernameTokenSupportingTokenUsername = usernameTokenSupportingTokenUsername;
            return this;
        }

        /**
         * @param clientSignatureKeyAlias the alias of the keystore entry which will be used to sign client requests in
         *                                the asymmetric binding
         * @return the builder
         */
        public SoapSTSConsumerBuilder clientSignatureKeyAlias(String clientSignatureKeyAlias) {
            this.clientSignatureKeyAlias = clientSignatureKeyAlias;
            return this;
        }

        /**
         * @param keystoreFile the location (on classpath, or filesystem) of the keystore
         * @return the builder
         */
        public SoapSTSConsumerBuilder keystoreFile(String keystoreFile) {
            this.keystoreFile = keystoreFile;
            return this;
        }

        /**
         * @param keystorePassword the keystore password
         * @return the builder
         */
        public SoapSTSConsumerBuilder keystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        /**
         * @param stsPublicKeyAlias the alias of the sts' public key entry. Note that the sts' public key must
         *                          be in the client's keystore for the symmetric binding (for trust), and will be
         *                          used in the asymmetric binding to encrypt messages from client to server.
         * @return the builder
         */
        public SoapSTSConsumerBuilder stsPublicKeyAlias(String stsPublicKeyAlias) {
            this.stsPublicKeyAlias = stsPublicKeyAlias;
            return this;
        }

        /**
         * @see org.forgerock.openam.sts.soap.SoapSTSConsumerCallbackHandler for all of the Callback types the CallbackHandler
         * must handle
         * @param callbackHander to handle the Callback types necessary to satisfy the sts' SecurityPolicy bindings
         * @return the builder
         */
        public SoapSTSConsumerBuilder callbackHander(CallbackHandler callbackHander) {
            this.callbackHander = callbackHander;
            return this;
        }

        /**
         * @param logMessages set to true to log input and output messages
         * @return builder
         */
        public SoapSTSConsumerBuilder logMessages(boolean logMessages) {
            this.logMessages = logMessages;
            return this;
        }

        public SoapSTSConsumer build() {
            return new SoapSTSConsumer(this);
        }
    }
    private static final boolean ALLOW_TOKEN_RENEWAL = true;
    private static final SecurityToken NULL_SECURITY_TOKEN = null;

    protected Bus bus;
    private final String stsInstanceWsdlUrl;
    private final String usernameTokenSupportingTokenUsername;
    private final String clientSignatureKeyAlias;
    private final String keystoreFile;
    private final String keystorePassword;
    private final String stsPublicKeyAlias;
    private final boolean logMessages;
    private final CallbackHandler callbackHander;

    private SoapSTSConsumer(SoapSTSConsumerBuilder builder) {
        this.stsInstanceWsdlUrl = builder.stsInstanceWsdlUrl;
        this.usernameTokenSupportingTokenUsername = builder.usernameTokenSupportingTokenUsername;
        this.callbackHander = builder.callbackHander;
        this.clientSignatureKeyAlias = builder.clientSignatureKeyAlias;
        this.keystoreFile = builder.keystoreFile;
        this.keystorePassword = builder.keystorePassword;
        this.stsPublicKeyAlias = builder.stsPublicKeyAlias;
        this.logMessages = builder.logMessages;
        initializeBus();
    }

    public static SoapSTSConsumerBuilder builder() {
        return new SoapSTSConsumerBuilder();
    }

    private void initializeBus() {
        bus = CXFBusFactory.getDefaultBus();
        addAMSessionTokenSupport();
    }

    private SecurityToken issueTokenInternal(EndpointSpecification endpointSpecification,
                                             TokenSpecification tokenSpecification,
                                             boolean allowRenewing) throws SoapSTSConsumerException {
        try {
            STSClient client = getSTSClient(
                    stsInstanceWsdlUrl,
                    endpointSpecification.serviceQName,
                    endpointSpecification.portQName);
            client.setKeyType(tokenSpecification.keyType);
            client.setTokenType(tokenSpecification.tokenType);
            //for integration with renew tests.
            client.setAllowRenewing(allowRenewing);
            client.setOnBehalfOf(tokenSpecification.onBehalfOf); //will be non-null only for SV assertions
            if (TokenSpecification.PUBLIC_KEY_KEYTYPE.equals(tokenSpecification.keyType)) {
                client.setUseCertificateForConfirmationKeyInfo(true);
                client.setUseKeyCertificate(tokenSpecification.holderOfKeyCertificate);
            }
            return client.requestSecurityToken();
        } catch (Exception e) {
            System.out.println("Exception caught in testIssue for wsdlLocation:  " + stsInstanceWsdlUrl +
                    "\nserviceQName: " + endpointSpecification.serviceQName + "\nendpointName: " +
                    endpointSpecification.portQName + "\nkeyType: " + tokenSpecification.keyType +
                    "\ntokenType: " + tokenSpecification.tokenType + "\nException: " +  e);
            e.printStackTrace(System.out);
            throw new SoapSTSConsumerException(e.getMessage(), e);
        }
    }

    private void validateTokenSuiteInternal(List<EndpointSpecification> endpoints,
                                            List<TokenSpecification> tokenSpecs,
                                            SecurityToken token) throws SoapSTSConsumerException {
        for (EndpointSpecification endpoint : endpoints) {
            for (TokenSpecification tokenSpec : tokenSpecs) {
                SecurityToken localToken = token;
                if (localToken == null) {
                    localToken = issueTokenInternal(
                            endpoint,
                            tokenSpec,
                            ALLOW_TOKEN_RENEWAL);
                }
                validateToken(
                        endpoint,
                        localToken);
            }
        }
    }

    /**
     *
     * @param endpoints the endpoints targeted by the issue operation
     * @param tokenSpecifications the desired tokens, including any additional necessary state
     * @return the list SecurityTokens resulting from the successful invocations
     * @throws SoapSTSConsumerException if any individual issue invocation did not succeed.
     */
    public List<SecurityToken> issueTokenSuite(List<EndpointSpecification> endpoints, List<TokenSpecification> tokenSpecifications) throws SoapSTSConsumerException {
        final List<SecurityToken> tokens = new ArrayList<>(endpoints.size() * tokenSpecifications.size());
        for (EndpointSpecification endpoint : endpoints) {
            for (TokenSpecification tokenSpecification : tokenSpecifications) {
                tokens.add(issueTokenInternal(
                        endpoint,
                        tokenSpecification,
                        ALLOW_TOKEN_RENEWAL));
            }
        }
        return tokens;
    }

    private void validateTokenSuite(List<EndpointSpecification> endpoints, List<TokenSpecification> tokenSpecs) throws SoapSTSConsumerException {
        validateTokenSuiteInternal(endpoints, tokenSpecs, NULL_SECURITY_TOKEN);
    }

    /**
     * Invoke the issue operation to obtain a token specified by the tokenSpecification on the endpoint specified by
     * the endpointSpecification
     * @param endpointSpecification specifies the targeted endpoint
     * @param tokenSpecification specifies the desired token, along with any necessary state
     * @return a SecurityToken encapsulating the issued token
     * @throws SoapSTSConsumerException if an exception occurred when issuing the token
     */
    public SecurityToken issueToken(EndpointSpecification endpointSpecification, TokenSpecification tokenSpecification)
            throws SoapSTSConsumerException {
        return issueTokenInternal(
                endpointSpecification,
                tokenSpecification,
                ALLOW_TOKEN_RENEWAL);
    }

    /**
     * Invokes the soap-sts Validate operation
     * @param endpointSpecification port and service qname of soap-sts instance to be invoked
     * @param toBeValidatedToken the to-be-validated SecurityToken instance returned from the Issue operation
     * @throws SoapSTSConsumerException
     */
    public boolean validateToken(EndpointSpecification endpointSpecification,
                                 SecurityToken toBeValidatedToken) throws SoapSTSConsumerException {
        STSClient client = getSTSClient(
                stsInstanceWsdlUrl,
                endpointSpecification.serviceQName,
                endpointSpecification.portQName);
        client.setTokenType(STSConstants.STATUS);
        try {
            client.validateSecurityToken(toBeValidatedToken);
            return true;
        } catch (TrustException e) {
            return false;
        } catch (Exception e) {
            throw new SoapSTSConsumerException(e.getMessage(), e);
        }
        /*
        No further checks are needed, as the STSClient will throw an exception if the token is not validated successfully.
        Also checking that the type of the SecurityToken returned by the validateSecurityToken call matches that of the
        type passed as a parameter to this call is also not a valid test, as the passed-in token is simply returned if
        the token validated successfully (and if the token was transformed, the type is not set - this may be considered a
        bug in the STSClient class - see around line 1066 - should be parsing out the TokenType element child of
        RequestSecurityTokenResponse. See CXF jira: https://issues.apache.org/jira/browse/CXF-5462).
         */
    }

    public boolean cancelToken(EndpointSpecification endpointSpecification,
                            SecurityToken token,
                            String tokenType,
                            String keyType) throws SoapSTSConsumerException {
        STSClient client = getSTSClient(
                stsInstanceWsdlUrl,
                endpointSpecification.serviceQName,
                endpointSpecification.portQName);

        client.setTokenType(tokenType);
        client.setKeyType(keyType);
        try {
            return client.cancelSecurityToken(token);
        } catch (Exception e) {
            throw new SoapSTSConsumerException(e.getMessage(), e);
        }
    }

    private STSClient getSTSClient(String wsdlAddress, QName serviceQName, QName portQName) throws SoapSTSConsumerException {
        STSClient stsClient = new STSClient(bus);
        if (logMessages) {
            stsClient.getInInterceptors().add(new LoggingInInterceptor());
            stsClient.getOutInterceptors().add(new LoggingOutInterceptor());
        }
        stsClient.setWsdlLocation(wsdlAddress);
        stsClient.setServiceName(serviceQName.toString());
        stsClient.setEndpointName(portQName.toString());

        Map<String, Object> properties = new HashMap<>();

        properties.put(SecurityConstants.USERNAME, usernameTokenSupportingTokenUsername);
        properties.put(SecurityConstants.CALLBACK_HANDLER, callbackHander);

        /*
        In a asymmetric binding, the client encrypt messages with with the sts' public key.
        Note that this trust (Public Key) keystore entry is not protected by a password, so the SoapSTSConsumerCallbackHandler is
        not asked to provide the password corresponding to this entry.
         */
        properties.put(SecurityConstants.ENCRYPT_USERNAME, stsPublicKeyAlias);

        Crypto crypto;
        try {
            crypto = CryptoFactory.getInstance(getEncryptionProperties());
        } catch (WSSecurityException e) {
            throw new SoapSTSConsumerException(e.getMessage(), e);
        }
        /*
        if the requested key is Public the STS_TOKEN_CRYPTO is used by the STSClient 'to send/process any
        RSA/DSAKeyValue tokens' - from javadocs
         */
        properties.put(SecurityConstants.STS_TOKEN_CRYPTO, crypto);
        properties.put(SecurityConstants.ENCRYPT_CRYPTO, crypto);
        properties.put(SecurityConstants.SIGNATURE_CRYPTO, crypto);

        stsClient.setProperties(properties);

        //handleSTSServerCertCNDNSMismatch(stsClient);

        return stsClient;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
                "org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin"
        );
        properties.put("org.apache.ws.security.crypto.merlin.keystore.password", keystorePassword);
        properties.put("org.apache.ws.security.crypto.merlin.keystore.file", keystoreFile);
        //the CallbackHandler must be able to provide the password corresponding to this key alias
        properties.put("org.apache.ws.security.crypto.merlin.keystore.alias", clientSignatureKeyAlias);

        return properties;
    }

    /**
     * This method must be called in case the CN in the Certificate presented by the container hosting the published sts
     * instance does not match the DNS name of this server. This check should not be relied-upon in production, and is
     * only present to facilitate testing.
     * @param stsClient The stsClient which will make the sts invocations
     */
    private void handleSTSServerCertCNDNSMismatch(STSClient stsClient) throws SoapSTSConsumerException {
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                });
        /*
        CXF client also needs to have disabled the CN check in server-presented cert for TLS cases, if cert CN
        does not match DNS
         */
        TLSClientParameters tlsClientParameters = new TLSClientParameters();
        tlsClientParameters.setDisableCNCheck(true);
        try {
            ((HTTPConduit)stsClient.getClient().getConduit()).setTlsClientParameters(tlsClientParameters);
        } catch (BusException | EndpointException e) {
            throw new SoapSTSConsumerException(e.getMessage(), e);
        }
    }

    /**
     * This method registers the AMSessionToken AssertionBuilder and InterceptorProvider required to consume a sts instance
     * protected by a SecurityPolicy binding specifying OpenAMToken Assertions.
     */
    private void addAMSessionTokenSupport() {
        PolicyInterceptorProviderRegistry pipr = bus
                .getExtension(PolicyInterceptorProviderRegistry.class);
        pipr.register(new OpenAMSessionTokenClientInterceptorProvider());

        AssertionBuilderRegistry abr = bus.getExtension(AssertionBuilderRegistry.class);
        abr.setIgnoreUnknownAssertions(false);
        abr.registerBuilder(AMSTSConstants.AM_SESSION_TOKEN_ASSERTION_QNAME,
                new OpenAMSessionTokenClientAssertionBuilder(callbackHander));
    }
}
