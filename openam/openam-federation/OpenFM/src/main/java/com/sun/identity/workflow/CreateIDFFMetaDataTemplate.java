/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CreateIDFFMetaDataTemplate.java,v 1.9 2008/11/18 22:38:19 asyhuang Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaSecurityUtils;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Create IDFF Meta Template.
 */
public class CreateIDFFMetaDataTemplate {

    private CreateIDFFMetaDataTemplate() {
    }
    
    public static String createStandardMetaTemplate(
        String entityId,
        Map mapParams,
        String url
    ) throws IDFFMetaException {
        if (url == null) {
            String protocol = SystemPropertiesManager.get(
                Constants.AM_SERVER_PROTOCOL);
            String host = SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
            String port = SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
            String deploymentURI = SystemPropertiesManager.get(
                Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
            url = protocol + "://" + host + ":" + port + deploymentURI;
        }
        
        StringBuffer buff = new StringBuffer();
        buff.append("<EntityDescriptor\n")
            .append("    xmlns=\"urn:liberty:metadata:2003-08\"\n")
            .append("    providerID=\"" + entityId + "\">\n");

        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        if (idpAlias != null) {
            String realm = IDFFMetaUtils.getRealmByMetaAlias(idpAlias);
            addIDFFIdentityProviderTemplate(buff, mapParams, url);
        }
        
        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        if (spAlias != null) {
            String realm = IDFFMetaUtils.getRealmByMetaAlias(spAlias);
            addIDFFServiceProviderTemplate(buff, mapParams, url);
        }
                
        String affiAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFILIATION);       
        if (affiAlias != null) {
            String realm = IDFFMetaUtils.getRealmByMetaAlias(affiAlias);
            addAffiliationTemplate(buff, entityId, affiAlias, url, mapParams);
        }

        buff.append("</EntityDescriptor>\n");
        return buff.toString();
    }

    private static void addIDFFIdentityProviderTemplate(
        StringBuffer buff,
        Map mapParams,
        String url
    ) throws IDFFMetaException {
        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        String idpSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_S_CERT);
        String idpECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_E_CERT);
        
        String maStr = buildMetaAliasInURI(idpAlias);

        buff.append("    <IDPDescriptor\n")
            .append("        protocolSupportEnumeration=")
            .append("\"urn:liberty:iff:2003-08 urn:liberty:iff:2002-12\">\n");
        
        String idpSX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
                idpSCertAlias);
        if (idpSX509Cert != null) {
            buff.append("        <KeyDescriptor use=\"signing\">\n")
                .append("            <KeyInfo xmlns=\"")
                .append(IDFFMetaSecurityUtils.NS_XMLSIG)
                .append("\">\n")
                .append("                <X509Data>\n")
                .append("                    <X509Certificate>\n")
                .append(idpSX509Cert)
                .append("                    </X509Certificate>\n")
                .append("                </X509Data>\n")
                .append("            </KeyInfo>\n")
                .append( "        </KeyDescriptor>\n");
        }
            
        String idpEX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
            idpECertAlias);
        if (idpEX509Cert != null) {
            buff.append("        <KeyDescriptor use=\"encryption\">\n")
                .append("            <EncryptionMethod>http://www.w3.org/2001/04/xmlenc#aes128-cbc</EncryptionMethod>\n")
                .append("            <KeySize>128</KeySize>\n")
                .append("            <KeyInfo xmlns=\"")
                .append(IDFFMetaSecurityUtils.NS_XMLSIG)
                .append("\">\n")
                .append("                <X509Data>\n")
                .append("                    <X509Certificate>\n")
                .append(idpEX509Cert)
                .append("                    </X509Certificate>\n")
                .append("                </X509Data>\n")
                .append("            </KeyInfo>\n")
                .append("        </KeyDescriptor>\n");
        }
        buff.append("        <SoapEndpoint>")
            .append(url)
            .append("/SOAPReceiver")
            .append(maStr)
            .append("</SoapEndpoint>\n")
            .append("        <SingleLogoutServiceURL>")
            .append(url)
            .append("/ProcessLogout")
            .append(maStr)
            .append("</SingleLogoutServiceURL>\n")
            .append("        <SingleLogoutServiceReturnURL>")
            .append(url)
            .append("/ReturnLogout")
            .append(maStr)
            .append("</SingleLogoutServiceReturnURL>\n")
            .append("        <FederationTerminationServiceURL>")
            .append(url)
            .append("/ProcessTermination")
            .append(maStr)
            .append("</FederationTerminationServiceURL>\n")
            .append("        <FederationTerminationServiceReturnURL>")
            .append(url)
            .append("/ReturnTermination")
            .append(maStr)
            .append("</FederationTerminationServiceReturnURL>\n")
            .append("        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-sp-http</FederationTerminationNotificationProtocolProfile>\n")
            .append("        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-sp-soap</FederationTerminationNotificationProtocolProfile>\n")
            .append("        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-sp-http</SingleLogoutProtocolProfile>\n")
            .append("        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-sp-soap</SingleLogoutProtocolProfile>\n")
            .append("        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-sp-http</RegisterNameIdentifierProtocolProfile>\n")
            .append("        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-sp-soap</RegisterNameIdentifierProtocolProfile>\n")
            .append("        <RegisterNameIdentifierServiceURL>")
            .append(url)
            .append("/ProcessRegistration")
            .append(maStr)
            .append("</RegisterNameIdentifierServiceURL>\n")
            .append("        <RegisterNameIdentifierServiceReturnURL>")
            .append(url)
            .append("/ReturnRegistration")
            .append(maStr)
            .append("</RegisterNameIdentifierServiceReturnURL>\n")
            .append("        <SingleSignOnServiceURL>")
            .append(url)
            .append("/SingleSignOnService")
            .append(maStr)
            .append("</SingleSignOnServiceURL>\n")
            .append("        <SingleSignOnProtocolProfile>http://projectliberty.org/profiles/brws-art</SingleSignOnProtocolProfile>\n")
            .append("        <SingleSignOnProtocolProfile>http://projectliberty.org/profiles/brws-post</SingleSignOnProtocolProfile>\n")
            .append("        <SingleSignOnProtocolProfile>http://projectliberty.org/profiles/lecp</SingleSignOnProtocolProfile>\n")
            .append("    </IDPDescriptor>\n");
    }

    private static void addIDFFServiceProviderTemplate(
        StringBuffer buff,
        Map mapParams,
        String url
    ) throws IDFFMetaException {
        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        String spSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_SP_S_CERT);
        String spECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_SP_E_CERT);

        String maStr = buildMetaAliasInURI(spAlias);
        buff.append("    <SPDescriptor\n")
            .append("        protocolSupportEnumeration=\n")
            .append("            \"urn:liberty:iff:2003-08 urn:liberty:iff:2002-12\">\n");
        
        String spSX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
            spSCertAlias);
        if (spSX509Cert != null) {
            buff.append("        <KeyDescriptor use=\"signing\">\n")
                .append("            <KeyInfo xmlns=\"")
                .append(IDFFMetaSecurityUtils.NS_XMLSIG)
                .append("\">\n")
                .append("                <X509Data>\n")
                .append("                    <X509Certificate>\n")
                .append(spSX509Cert )
                .append("                    </X509Certificate>\n")
                .append("                </X509Data>\n")
                .append("            </KeyInfo>\n")
                .append("        </KeyDescriptor>\n");
        }
            
        String spEX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
            spECertAlias);
        if (spEX509Cert != null) {
            buff.append("        <KeyDescriptor use=\"encryption\">\n")
                .append("            <EncryptionMethod>http://www.w3.org/2001/04/xmlenc#aes128-cbc</EncryptionMethod>\n")
                .append("            <KeySize>128</KeySize>\n")
                .append("            <KeyInfo xmlns=\"")
                .append(IDFFMetaSecurityUtils.NS_XMLSIG )
                .append("\">\n")
                .append("                <X509Data>\n")
                .append("                    <X509Certificate>\n")
                .append(spEX509Cert )
                .append("                    </X509Certificate>\n")
                .append("                </X509Data>\n")
                .append("            </KeyInfo>\n")
                .append("        </KeyDescriptor>\n");
        }
        buff.append("        <SoapEndpoint>")
            .append(url )
            .append("/SOAPReceiver")
            .append(maStr )
            .append("</SoapEndpoint>\n")
            .append("        <SingleLogoutServiceURL>")
            .append(url )
            .append("/ProcessLogout")
            .append(maStr)
            .append("</SingleLogoutServiceURL>\n")
            .append("        <SingleLogoutServiceReturnURL>")
            .append(url )
            .append("/ReturnLogout")
            .append(maStr)
            .append("</SingleLogoutServiceReturnURL>\n")
            .append("        <FederationTerminationServiceURL>")
            .append(url )
            .append("/ProcessTermination")
            .append(maStr)
            .append("</FederationTerminationServiceURL>\n")
            .append("        <FederationTerminationServiceReturnURL>")
            .append(url )
            .append("/ReturnTermination")
            .append(maStr)
            .append("</FederationTerminationServiceReturnURL>\n")
            .append("        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-idp-http</FederationTerminationNotificationProtocolProfile>\n")
            .append("        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-idp-soap</FederationTerminationNotificationProtocolProfile>\n")
            .append("        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-idp-http</SingleLogoutProtocolProfile>\n")
            .append("        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-idp-soap</SingleLogoutProtocolProfile>\n")
            .append("        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-idp-http</RegisterNameIdentifierProtocolProfile>\n")
            .append("        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-idp-soap</RegisterNameIdentifierProtocolProfile>\n")
            .append("        <RegisterNameIdentifierServiceURL>")
            .append(url)
            .append("/ProcessRegistration")
            .append(maStr)
            .append("</RegisterNameIdentifierServiceURL>\n")
            .append("        <RegisterNameIdentifierServiceReturnURL>")
            .append(url )
            .append("/ReturnRegistration")
            .append(maStr)
            .append("</RegisterNameIdentifierServiceReturnURL>\n")
            .append("        <AssertionConsumerServiceURL id=\"1\" isDefault=\"true\">")
            .append(url)
            .append("/AssertionConsumerService")
            .append(maStr)
            .append("</AssertionConsumerServiceURL>\n")
            .append("        <AuthnRequestsSigned>false</AuthnRequestsSigned>\n")
            .append("    </SPDescriptor>\n");
    }

    public static String createExtendedMetaTemplate(
        String entityId, 
        Map mapParams
    ) {
        StringBuffer buff = new StringBuffer();
        buff.append("<EntityConfig xmlns=\"urn:sun:fm:ID-FF:entityconfig\"\n")
            .append("    hosted=\"1\"\n")
            .append("    entityID=\"")
            .append(entityId)
            .append("\">\n\n");

        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        if (idpAlias != null) {
            String realm = IDFFMetaUtils.getRealmByMetaAlias(idpAlias);
            buildIDFFIDPConfigTemplate(mapParams, buff);
        }
        
        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        if (spAlias != null) {
            String realm = IDFFMetaUtils.getRealmByMetaAlias(spAlias);
            buildIDFFSPConfigTemplate(mapParams, buff);
        }
        
        String affiAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFILIATION);
        if (affiAlias != null) {
            String realm = IDFFMetaUtils.getRealmByMetaAlias(affiAlias);
            buildAffiliationConfigTemplate(buff, affiAlias, mapParams);
        }     
       
        buff.append("</EntityConfig>\n");
        return buff.toString();
    }
    
    private static void buildIDFFIDPConfigTemplate(
        Map mapParams,
        StringBuffer buff
    ) {
        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        String idpSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_S_CERT);
        String idpECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_E_CERT);

        if (idpSCertAlias == null) {
             idpSCertAlias = "";
         }
         if (idpECertAlias == null) {
             idpECertAlias = "";
         }
        
        buff.append("    <IDPDescriptorConfig metaAlias=\"")
            .append(idpAlias)
            .append("\">\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.PROVIDER_STATUS)
            .append( "\">\n")
            .append("            <Value>active</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.PROVIDER_DESCRIPTION)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.SIGNING_CERT_ALIAS)
            .append("\">\n")
            .append("            <Value>")
            .append(idpSCertAlias)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENCRYPTION_CERT_ALIAS)
            .append("\">\n")
            .append("            <Value>")
            .append(idpECertAlias)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENABLE_NAMEID_ENCRYPTION)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.GENERATE_BOOTSTRAPPING)
            .append("\">\n")
            .append("            <Value>true</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.RESPONDS_WITH)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.FS_USER_PROVIDER_CLASS)
            .append("\">\n")
            .append("            <Value>com.sun.identity.federation.accountmgmt.DefaultFSUserProvider</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.NAMEID_IMPL_CLASS)
            .append("\">\n")
            .append("            <Value>com.sun.identity.federation.services.util.FSNameIdentifierImpl</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.AUTH_TYPE)
            .append("\">\n")
            .append("            <Value>local</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.REGISTRATION_DONE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.TERMINATION_DONE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.LOGOUT_DONE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.LISTOFCOTS_PAGE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ERROR_PAGE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.PROVIDER_HOME_PAGE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ASSERTION_INTERVAL)
            .append("\">\n")
            .append("            <Value>60</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.CLEANUP_INTERVAL)
            .append("\">\n")
            .append("            <Value>180</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ARTIFACT_TIMEOUT)
            .append("\">\n")
            .append("            <Value>120</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ASSERTION_LIMIT)
            .append("\">\n")
            .append("            <Value>0</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ASSERTION_ISSUER)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ATTRIBUTE_PLUGIN)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.IDP_ATTRIBUTE_MAP)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.DEFAULT_AUTHNCONTEXT)
            .append("\">\n")
            .append("            <Value>")
            .append(IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.IDP_AUTHNCONTEXT_MAPPING)
            .append("\">\n")
            .append("            <Value>context=")
            .append(IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD)
            .append("|key=module|value=DataStore|level=0</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENABLE_AUTO_FEDERATION)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.AUTO_FEDERATION_ATTRIBUTE)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ATTRIBUTE_MAPPER_CLASS)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("       <Attribute name=\"")
            .append(COTConstants.COT_LIST)
            .append("\">\n")
            .append("       </Attribute>\n")
            .append("    </IDPDescriptorConfig>\n");
    }

    private static void buildIDFFSPConfigTemplate(
        Map mapParams,
        StringBuffer buff
    ) {
        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        String spSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_SP_S_CERT);
        String spECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_SP_E_CERT);
         if (spSCertAlias == null) {
             spSCertAlias = "";
         }
         if (spECertAlias == null) {
             spECertAlias = "";
         }

        buff.append("    <SPDescriptorConfig metaAlias=\"")
            .append(spAlias)
            .append("\">\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.PROVIDER_STATUS)
            .append("\">\n")
            .append("            <Value>active</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.PROVIDER_DESCRIPTION)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.SIGNING_CERT_ALIAS)
            .append("\">\n")
            .append("            <Value>")
            .append(spSCertAlias)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENCRYPTION_CERT_ALIAS)
            .append("\">\n")
            .append("            <Value>")
            .append(spECertAlias)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENABLE_IDP_PROXY)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.IDP_PROXY_LIST)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.IDP_PROXY_COUNT)
            .append("\">\n")
            .append("            <Value>-1</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.USE_INTRODUCTION_FOR_IDP_PROXY)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENABLE_AFFILIATION)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENABLE_NAMEID_ENCRYPTION)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.SUPPORTED_SSO_PROFILE)
            .append("\">\n")
            .append("            <Value>http://projectliberty.org/profiles/brws-art</Value>\n")
            .append("            <Value>http://projectliberty.org/profiles/brws-post</Value>\n")
            .append("            <Value>http://projectliberty.org/profiles/wml-post</Value>\n")
            .append("            <Value>http://projectliberty.org/profiles/lecp</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.NAMEID_POLICY)
            .append("\">\n")
            .append("            <Value>federated</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.FORCE_AUTHN)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.IS_PASSIVE)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.RESPONDS_WITH)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.FS_USER_PROVIDER_CLASS)
            .append("\">\n")
            .append("            <Value>com.sun.identity.federation.accountmgmt.DefaultFSUserProvider</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.NAMEID_IMPL_CLASS)
            .append("\">\n")
            .append("            <Value>com.sun.identity.federation.services.util.FSNameIdentifierImpl</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.AUTH_TYPE)
            .append("\">\n")
            .append("            <Value>remote</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.REGISTRATION_DONE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.TERMINATION_DONE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.LOGOUT_DONE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.FEDERATION_DONE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.DOFEDERATE_PAGE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.LISTOFCOTS_PAGE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ERROR_PAGE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.SSO_FAILURE_REDIRECT_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.PROVIDER_HOME_PAGE_URL)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.DEFAULT_AUTHNCONTEXT)
            .append("\">\n")
            .append("            <Value>")
            .append(IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.SP_AUTHNCONTEXT_MAPPING)
            .append("\">\n")
            .append("            <Value>context=")
            .append(IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD)
            .append("|level=0</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENABLE_AUTO_FEDERATION)
            .append("\">\n")
            .append("            <Value>false</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.AUTO_FEDERATION_ATTRIBUTE)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ATTRIBUTE_MAPPER_CLASS)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.SP_ATTRIBUTE_MAP)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.FEDERATION_SP_ADAPTER)
            .append("\">\n")
            .append("            <Value>com.sun.identity.federation.plugins.FSDefaultSPAdapter</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.FEDERATION_SP_ADAPTER_ENV)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append( COTConstants.COT_LIST)
            .append("\">\n")
            .append("        </Attribute>\n")
            .append("    </SPDescriptorConfig>\n");
    }

    private static String buildMetaAliasInURI(String alias) {
        return "/" + SAML2MetaManager.NAME_META_ALIAS_IN_URI + alias;
    }
    
    private static void addAffiliationTemplate(
        StringBuffer buff,
        String entityID,        
        String affiAlias,        
        String url,
        Map mapParams
    ) throws IDFFMetaException {
        String maStr = buildMetaAliasInURI(affiAlias);
        String affiOwnerID = (String)mapParams.get(
            MetaTemplateParameters.P_AFFI_OWNERID);       
        
        buff.append("    <AffiliationDescriptor\n")
            .append("        affiliationID=\"")
            .append(entityID)
            .append("\" \n")
            .append("        affiliationOwnerID=\"")
            .append(affiOwnerID)
            .append("\">\n");

        List affiMembers = (List)mapParams.get(
            MetaTemplateParameters.P_AFFI_MEMBERS);
        for(Iterator iter = affiMembers.iterator(); iter.hasNext(); ) {
            String affiMember = (String)iter.next();
            buff.append(
                "        <AffiliateMember>" + affiMember + "</AffiliateMember>\n");
        }
       
        String affiSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFI_S_CERT);              
        String affiSX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
                affiSCertAlias);
        if (affiSX509Cert != null) {
            buff.append("        <KeyDescriptor use=\"signing\">\n")
                .append("            <KeyInfo xmlns=\"")
                .append(IDFFMetaSecurityUtils.NS_XMLSIG)
                .append("\">\n")
                .append("                <X509Data>\n")
                .append("                    <X509Certificate>\n")
                .append(affiSX509Cert)
                .append("                    </X509Certificate>\n")
                .append("                </X509Data>\n")
                .append("            </KeyInfo>\n")
                .append( "        </KeyDescriptor>\n");
        }
                            
        String affiECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFI_E_CERT);
        String affiEX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
            affiECertAlias);
        if (affiEX509Cert != null) {
            buff.append("        <KeyDescriptor use=\"encryption\">\n")
                .append("            <EncryptionMethod>http://www.w3.org/2001/04/xmlenc#aes128-cbc</EncryptionMethod>\n")
                .append("            <KeySize>128</KeySize>\n")
                .append("            <KeyInfo xmlns=\"")
                .append(IDFFMetaSecurityUtils.NS_XMLSIG)
                .append("\">\n")
                .append("                <X509Data>\n")
                .append("                    <X509Certificate>\n")
                .append(affiEX509Cert)
                .append("                    </X509Certificate>\n")
                .append("                </X509Data>\n")
                .append("            </KeyInfo>\n")
                .append("        </KeyDescriptor>\n");
        }
        
        buff.append("    </AffiliationDescriptor>\n");
    }
    
    private static void buildAffiliationConfigTemplate(
        StringBuffer buff,
        String affiAlias,       
        Map mapParams
    )  {
        String affiECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFI_E_CERT);
        String affiSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFI_S_CERT);
        if (affiECertAlias == null) {
            affiECertAlias = "";
        }
        if (affiSCertAlias == null) {
            affiSCertAlias = "";
        }
        
        buff.append("    <AffiliationDescriptorConfig metaAlias=\"")
            .append(affiAlias)
            .append("\">\n")            
            .append("        <Attribute name=\"")
            .append(IFSConstants.PROVIDER_DESCRIPTION)
            .append("\">\n")
            .append("            <Value></Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.SIGNING_CERT_ALIAS)
            .append("\">\n")
            .append("            <Value>")
            .append(affiSCertAlias)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("        <Attribute name=\"")
            .append(IFSConstants.ENCRYPTION_CERT_ALIAS)
            .append("\">\n")
            .append("            <Value>")
            .append(affiECertAlias)
            .append("</Value>\n")
            .append("        </Attribute>\n")
            .append("    </AffiliationDescriptorConfig>\n");
        
    }
}
