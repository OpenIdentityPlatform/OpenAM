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
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.publish.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.STSInitializationException;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.soap.config.user.SoapSTSKeystoreConfig;
import org.forgerock.openam.sts.soap.publish.STSInstancePublisher;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.soap.config.SoapSTSInstanceModule;
import org.forgerock.openam.sts.soap.config.user.DeploymentConfig;
import org.forgerock.openam.sts.soap.config.user.SoapSTSInstanceConfig;

import javax.jws.WebService;
import javax.xml.namespace.QName;
import java.io.UnsupportedEncodingException;

/**
 * This is a web-service which allows for the programmatic publication of STS instances.
 *
 * And the manner in which the to-be-published STS is specified (via a String) is only there to facilitate the simple
 * creation of a web-service which can publish STS instances, which is necessary for integration tests.
 *
 * The final implementation will likely involve a set of configuration objects which encapsulate all of the configuration
 * state necessary to create an STS instance
 *
 */
@WebService(
        endpointInterface="org.forgerock.openam.sts.soap.publish.web.STSPublish",
        serviceName="STSPublishService",
        portName = "STSPublishPort",
        targetNamespace = "http://org.forgerock.openam.sts.publish")
public class STSPublishImpl implements STSPublish {
    public STSPublishImpl() {
    }

    public void publishSTSEndpoint(String uriElement, String securityPolicyBindingId, String amDeploymentUrl) throws STSInitializationException {
        publishSTS(uriElement, securityPolicyBindingId, amDeploymentUrl);
    }

    private void publishSTS(String uriElement, String securityPolicyBindingId, String amDeploymentUrl) throws STSInitializationException {
        SoapSTSInstanceConfig instanceConfig = createInstanceConfig(uriElement, securityPolicyBindingId, amDeploymentUrl);
        Injector injector = getInjector(instanceConfig);
        injector.getInstance(STSInstancePublisher.class).publishSTSInstance();
    }

    /*
    This is a stubbed method at this point. At some point in the future, the STSInstanceConfig instance will come from
    UI elements providing for the configuration of STS instances.
     */
    private SoapSTSInstanceConfig createInstanceConfig(String uriElement, String bndId, String amDeploymentUrl) throws STSInitializationException {
        /*
        Adding some hard-coded mappings to point x509 cert validation at a specific module. In the final version, all of this
        config state will come from the user. For now, if the STS SecurityPolicy bindings will include x509 protection tokens,
        then these have to be validated against the OpenAM Certificate AuthN Module. I will hard-code its module name for now.
        TODO: also the STSInstanceConfig must come a provider - ultimately, the provider will likely pull this information from OpenAM,
        and a single Provider must be able to pull the STSInstanceConfig for all deployed STS instances. So I need to identify a given
        deployment, and have this key pull the corresponding STSInstanceConfig. Though it might not be a pull model, but rather a
        push model - there will be an entity responsible for deploying a set of STS instances, and this entity will simply pull
        a List<STSInstanceConfig> from a Provider. This class could be considered such an entity. I could formalize this with
        an interface, and inject a Provider<List<STSInstanceConfig>> into this instance - that would get me closer to the final
        deployment.
         */
        AuthTargetMapping mapping;
        if (AMSTSConstants.SYMMETRIC_ENDORSING_CERT_BINDING.equals(bndId)) {
            mapping = AuthTargetMapping
                                        .builder()
                                        .addMapping(TokenType.X509, AMSTSConstants.AUTH_INDEX_TYPE_MODULE, "X509")
                                        .build();
        } else {
            mapping = AuthTargetMapping.builder().build();
        }

        DeploymentConfig deploymentConfig =
                                        DeploymentConfig.builder()
                                        .portQName(getPortQName(bndId))
                                        .serviceQName(getServiceQName(bndId))
                                        .amDeploymentUrl(amDeploymentUrl)
                                        .uriElement(uriElement)
                                        .wsdlLocation(getWsdlLocation(bndId))
                                        .authTargetMapping(mapping)
                                        .build();

        SoapSTSKeystoreConfig keystoreConfig;
        try {
            keystoreConfig = SoapSTSKeystoreConfig.builder()
                    .fileName("stsstore.jks")
                    .password("frstssrvkspw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .encryptionKeyAlias("frstssrval")
                    .signatureKeyAlias("frstssrval")
                    .encryptionKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .signatureKeyPassword("frstssrvpw".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new STSInitializationException(ResourceException.INTERNAL_ERROR, "Unsupported encoding in creating KeystoreConfig instance:" + e, e);
        }

        return SoapSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .soapSTSKeystoreConfig(keystoreConfig)
                .issuerName("OpenAM")
                .addIssueTokenType(TokenType.SAML2)
                .addRenewTokenType(TokenType.SAML2)
                .addValidateTokenStatusType(TokenType.SAML2)
                .addValidateTokenStatusType(TokenType.OPENAM)
                .addValidateTokenStatusType(TokenType.USERNAME)
                .addValidateTokenTransformation(TokenType.USERNAME, TokenType.OPENAM)
                .build();
    }

    private QName getPortQName(String securityPolicyBindingId) {
        if (AMSTSConstants.UNPROTECTED_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.STS_SERVICE_PORT;
        } else if (AMSTSConstants.SYMMETRIC_USERNAME_TOKEN_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.SYMMETRIC_UT_STS_SERVICE_PORT;
        } else if (AMSTSConstants.ASYMMETRIC_USERNAME_TOKEN_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.ASYMMETRIC_UT_STS_SERVICE_PORT;
        } else if (AMSTSConstants.TRANSPORT_USERNAME_TOKEN_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.TRANSPORT_UT_STS_SERVICE_PORT;
        } else if (AMSTSConstants.SYMMETRIC_ENDORSING_CERT_BINDING.equals(securityPolicyBindingId)) {
            return AMSTSConstants.SYMMETRIC_ENDORSING_CERT_STS_SERVICE_PORT;
        } else {
            throw new IllegalArgumentException("Unrecognized securityPolicyBindingId provided: " + securityPolicyBindingId);
        }
    }

    private QName getServiceQName(String securityPolicyBindingId) {
        if (AMSTSConstants.UNPROTECTED_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.STS_SERVICE;
        } else if (AMSTSConstants.SYMMETRIC_USERNAME_TOKEN_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.SYMMETRIC_UT_STS_SERVICE;
        } else if (AMSTSConstants.ASYMMETRIC_USERNAME_TOKEN_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.ASYMMETRIC_UT_STS_SERVICE;
        } else if (AMSTSConstants.TRANSPORT_USERNAME_TOKEN_BINDING.equals(securityPolicyBindingId))  {
            return AMSTSConstants.TRANSPORT_UT_STS_SERVICE;
        } else if (AMSTSConstants.SYMMETRIC_ENDORSING_CERT_BINDING.equals(securityPolicyBindingId)) {
            return AMSTSConstants.SYMMETRIC_ENDORSING_CERT_STS_SERVICE;
        } else {
            throw new IllegalArgumentException("Unrecognized securityPolicyBindingId provided: " + securityPolicyBindingId);
        }
    }

    /*
    Ultimately the wsdl location will determine the SecurityPolicy bindings. For now, this is all hard-coded. In the final
    release, OpenAM will probably ship with a set of .wsdl files which define the common bindings.
     */
    private String getWsdlLocation(String bndId) {
        if (AMSTSConstants.UNPROTECTED_BINDING.equals(bndId)) {
            return "sts_unprotected.wsdl";
        } else if (AMSTSConstants.SYMMETRIC_ENDORSING_CERT_BINDING.equals(bndId)) {
            return "sts_x509.wsdl";
        }
        else {
            return "sts_ut.wsdl";
        }
    }

    private Injector getInjector(SoapSTSInstanceConfig instanceConfig) {
        return  Guice.createInjector(new SoapSTSInstanceModule(instanceConfig));
    }
}
