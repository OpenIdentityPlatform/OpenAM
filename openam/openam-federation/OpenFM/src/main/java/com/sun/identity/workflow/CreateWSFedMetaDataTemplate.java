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
 * $Id: CreateWSFedMetaDataTemplate.java,v 1.9 2009/12/14 23:42:49 mallas Exp $
 *
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.ClaimType;
import com.sun.identity.wsfederation.jaxb.wsfederation.DisplayNameType;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.SingleSignOutNotificationEndpointElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerNameElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenSigningKeyInfoElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenType;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenTypesOfferedElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.UriNamedClaimTypesOfferedElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.io.StringWriter;
import java.security.cert.CertificateEncodingException;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import com.sun.identity.wsfederation.jaxb.wsse.SecurityTokenReferenceElement;
import com.sun.identity.wsfederation.jaxb.xmlsig.X509DataElement;
import com.sun.identity.wsfederation.jaxb.xmlsig.X509DataType.X509Certificate;
import com.sun.identity.wsfederation.jaxb.wsaddr.AttributedURIType;

/**
 * Create WS Federation Meta Template.
 */
public class CreateWSFedMetaDataTemplate {

    private CreateWSFedMetaDataTemplate() {
    }
    
    public static String createStandardMetaTemplate(
        String entityId,
        Map mapParams,
        String url
    ) throws JAXBException, CertificateEncodingException {
        JAXBContext jc = WSFederationMetaUtils.getMetaJAXBContext();
        com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory
            objFactory = 
            new com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory();

        FederationElement fed = objFactory.createFederationElement();
        fed.setFederationID(entityId);

        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        if (idpAlias != null) {
            addWSFedIdentityProviderTemplate(entityId, objFactory, fed, 
                mapParams, url);
        }
        
        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        if (spAlias != null) {
            addWSFedServiceProviderTemplate(entityId, objFactory, fed, 
                mapParams, url);
        }

        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter pw = new StringWriter();
        m.marshal(fed, pw);
        return pw.toString();
    }
    
    private static void addWSFedIdentityProviderTemplate(
        String entityId,
        com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory 
        objFactory, 
        FederationElement fed,
        Map mapParams,
        String url
    ) throws JAXBException, CertificateEncodingException {
        if (url == null) {
            url = getHostURL();
        }
        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        String idpSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_S_CERT);
        
        String maStr = buildMetaAliasInURI(idpAlias);
        
        if ((idpSCertAlias != null) && idpSCertAlias.length() > 0) {
            com.sun.identity.wsfederation.jaxb.wsse.ObjectFactory 
                secextObjFactory = new 
                com.sun.identity.wsfederation.jaxb.wsse.ObjectFactory();
            com.sun.identity.wsfederation.jaxb.xmlsig.ObjectFactory 
                dsObjectFactory = new 
                com.sun.identity.wsfederation.jaxb.xmlsig.ObjectFactory();

            TokenSigningKeyInfoElement tski = 
                objFactory.createTokenSigningKeyInfoElement();
            SecurityTokenReferenceElement str = 
                secextObjFactory.createSecurityTokenReferenceElement();
            X509DataElement x509Data = dsObjectFactory.createX509DataElement();
            X509Certificate x509Cert = 
                dsObjectFactory.createX509DataTypeX509Certificate();
            x509Cert.setValue(
                KeyUtil.getKeyProviderInstance().getX509Certificate(idpSCertAlias).getEncoded());
            x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(x509Cert);
            str.getAny().add(x509Data);
            tski.setSecurityTokenReference(str);
            fed.getAny().add(tski);
        }
        
        TokenIssuerNameElement tin = objFactory.createTokenIssuerNameElement();
        tin.setValue(entityId);
        fed.getAny().add(tin);
        
        TokenIssuerEndpointElement tie = 
            objFactory.createTokenIssuerEndpointElement();
        com.sun.identity.wsfederation.jaxb.wsaddr.ObjectFactory addrObjFactory =
            new com.sun.identity.wsfederation.jaxb.wsaddr.ObjectFactory();
        AttributedURIType auri = addrObjFactory.createAttributedURIType();
        auri.setValue(url + "/WSFederationServlet" + maStr);
        tie.setAddress(auri);        
        fed.getAny().add(tie);
        
        TokenTypesOfferedElement tto = 
            objFactory.createTokenTypesOfferedElement();
        TokenType tt = objFactory.createTokenType();
        tt.setUri(WSFederationConstants.URN_OASIS_NAMES_TC_SAML_11);
        tto.getTokenType().add(tt);
        fed.getAny().add(tto);
        
        UriNamedClaimTypesOfferedElement uncto = 
            objFactory.createUriNamedClaimTypesOfferedElement();
        ClaimType ct = objFactory.createClaimType();
        ct.setUri(WSFederationConstants.NAMED_CLAIM_TYPES[
            WSFederationConstants.NAMED_CLAIM_UPN]);
        DisplayNameType dnt = objFactory.createDisplayNameType();
        dnt.setValue(WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
            WSFederationConstants.NAMED_CLAIM_UPN]);
        ct.setDisplayName(dnt);
        uncto.getClaimType().add(ct);
        fed.getAny().add(uncto);
    }
    
    private static void addWSFedServiceProviderTemplate(
        String entityId,
        com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory 
        objFactory, 
        FederationElement fed,
        Map mapParams,
        String url
    ) throws JAXBException {
        if (url == null) {
            url = getHostURL();
        }
        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        String maStr = buildMetaAliasInURI(spAlias);
        
        TokenIssuerNameElement tin = objFactory.createTokenIssuerNameElement();
        tin.setValue(entityId);
        fed.getAny().add(tin);
        
        TokenIssuerEndpointElement tie = 
            objFactory.createTokenIssuerEndpointElement();
        com.sun.identity.wsfederation.jaxb.wsaddr.ObjectFactory addrObjFactory =
            new com.sun.identity.wsfederation.jaxb.wsaddr.ObjectFactory();
        AttributedURIType auri = addrObjFactory.createAttributedURIType();
        auri.setValue(url + "/WSFederationServlet" + maStr);
        tie.setAddress(auri);        
        fed.getAny().add(tie);

        SingleSignOutNotificationEndpointElement ssne = 
            objFactory.createSingleSignOutNotificationEndpointElement();
        AttributedURIType ssneUri = addrObjFactory.createAttributedURIType();
        ssneUri.setValue(url + "/WSFederationServlet" + maStr);
        ssne.setAddress(auri);        
        fed.getAny().add(ssne);
    }
        
    public static String createExtendedMetaTemplate(
        String entityId, 
        Map mapParams
    ) throws JAXBException {
        JAXBContext jc = WSFederationMetaUtils.getMetaJAXBContext();
        com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory 
            objFactory =
            new com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory();
        FederationConfigElement fedConfig =
            objFactory.createFederationConfigElement();

        fedConfig.setFederationID(entityId);
        fedConfig.setHosted(true);

        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        if (idpAlias != null) {
            buildWSFedIDPConfigTemplate(objFactory, fedConfig, mapParams);
        }
        
        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        if (spAlias != null) {
            buildWSFedSPConfigTemplate(objFactory, fedConfig, mapParams);
        }

        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter pw = new StringWriter();
        m.marshal(fedConfig, pw);
        return pw.toString();
    }
    
    private static void buildWSFedIDPConfigTemplate(
        com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory 
        objFactory, 
        FederationConfigElement fedConfig,
        Map mapParams
    ) throws JAXBException {
        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        String idpSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_S_CERT);
        
        String[][] configDefaults = { 
            { WSFederationConstants.DISPLAY_NAME, idpAlias },
            { WSFederationConstants.NAMEID_FORMAT, "" },
            { WSFederationConstants.NAMEID_ATTRIBUTE, "" },
            { WSFederationConstants.NAME_INCLUDES_DOMAIN, "" },
            { WSFederationConstants.DOMAIN_ATTRIBUTE, "" }, 
            { WSFederationConstants.UPN_DOMAIN, getHostDomain() },
            { SAML2Constants.SIGNING_CERT_ALIAS, idpSCertAlias },
            { SAML2Constants.ASSERTION_NOTBEFORE_SKEW_ATTRIBUTE, "600" },
            { SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE, "600" },
            { SAML2Constants.IDP_AUTHNCONTEXT_MAPPER_CLASS, 
                  "com.sun.identity.wsfederation.plugins.DefaultIDPAuthenticationMethodMapper" 
            },
            { SAML2Constants.IDP_ACCOUNT_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultIDPAccountMapper" },
            { SAML2Constants.IDP_ATTRIBUTE_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultIDPAttributeMapper" },
            { SAML2Constants.ATTRIBUTE_MAP, "" },
            { COTConstants.COT_LIST, null },
        };

        com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement 
            idpSSOConfig = objFactory.createIDPSSOConfigElement();

        idpSSOConfig.setMetaAlias(idpAlias);

        for ( int i = 0; i < configDefaults.length; i++ )
        {
            com.sun.identity.wsfederation.jaxb.entityconfig.AttributeElement 
                attribute = objFactory.createAttributeElement();
            attribute.setName(configDefaults[i][0]);
            if (configDefaults[i][1] != null) {
                attribute.getValue().add(configDefaults[i][1]);
            }

            idpSSOConfig.getAttribute().add(attribute);
        }
        
        fedConfig.getIDPSSOConfigOrSPSSOConfig().add(idpSSOConfig);
    }
    
    private static void buildWSFedSPConfigTemplate(
        com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory 
        objFactory,
        FederationConfigElement fedConfig,
        Map mapParams
    ) throws JAXBException {
        String url = getHostURL();
        String spAlias = (String) mapParams.get(MetaTemplateParameters.P_SP);
        String spSCertAlias = (String) mapParams.get(
            MetaTemplateParameters.P_SP_S_CERT);

        String maStr = buildMetaAliasInURI(spAlias);

        String[][] configDefaults = { 
            { WSFederationConstants.DISPLAY_NAME, spAlias },
            { WSFederationConstants.ACCOUNT_REALM_SELECTION, "cookie" },
            { WSFederationConstants.ACCOUNT_REALM_COOKIE_NAME, 
                  "amWSFederationAccountRealm" },
            { WSFederationConstants.HOME_REALM_DISCOVERY_SERVICE, 
                  url + "/RealmSelection" + maStr },
            { SAML2Constants.SIGNING_CERT_ALIAS, 
                  ( spSCertAlias != null ) ? spSCertAlias : "" },
            { SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE, "600" },
            { SAML2Constants.SP_ACCOUNT_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultADFSPartnerAccountMapper" },
            { SAML2Constants.SP_ATTRIBUTE_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultSPAttributeMapper" },
            { SAML2Constants.SP_AUTHCONTEXT_MAPPER, 
                  SAML2Constants.DEFAULT_SP_AUTHCONTEXT_MAPPER },
            { SAML2Constants.SP_AUTH_CONTEXT_CLASS_REF_ATTR, 
                  SAML2Constants.SP_AUTHCONTEXT_CLASSREF_VALUE },
            { SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE, 
                  SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE_VALUE },
            { SAML2Constants.ATTRIBUTE_MAP, "" },
            { SAML2Constants.AUTH_MODULE_NAME, "" },
            { SAML2Constants.DEFAULT_RELAY_STATE, "" },
            { SAML2Constants.ASSERTION_TIME_SKEW, "300" },
            { SAML2Constants.ASSERTION_CACHE_ENABLED, "true" },
            { WSFederationConstants.WANT_ASSERTION_SIGNED, "true" },
            { COTConstants.COT_LIST, null },
        };

        com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement 
            spSSOConfig = objFactory.createSPSSOConfigElement();

        spSSOConfig.setMetaAlias(spAlias);

        for ( int i = 0; i < configDefaults.length; i++ )
        {
            com.sun.identity.wsfederation.jaxb.entityconfig.AttributeElement 
                attribute = objFactory.createAttributeElement();
            attribute.setName(configDefaults[i][0]);
            if (configDefaults[i][1] != null) {
                attribute.getValue().add(configDefaults[i][1]);
            }

            spSSOConfig.getAttribute().add(attribute);
        }
        
        fedConfig.getIDPSSOConfigOrSPSSOConfig().add(spSSOConfig);
    }

    private static String getHostURL() {
        String protocol = SystemPropertiesManager.get(
            Constants.AM_SERVER_PROTOCOL);
        String host = SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
        String port = SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
        String deploymentURI = SystemPropertiesManager.get(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        return  protocol + "://" + host + ":" + port + deploymentURI;
    }

    private static String buildMetaAliasInURI(String alias) {
        return "/" + SAML2MetaManager.NAME_META_ALIAS_IN_URI + alias;
    }
    
    private static String getHostDomain() {
        String host = SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
        int dot = host.indexOf('.');
        if ( dot == -1 || (dot + 1) == host.length() ) {
            // There must be a dot and it can't be the last character
            return null;
        }
        return host.substring(dot + 1);
    }
}
