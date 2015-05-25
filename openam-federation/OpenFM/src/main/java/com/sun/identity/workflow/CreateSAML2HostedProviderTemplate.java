/*
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
 * $Id: CreateSAML2HostedProviderTemplate.java,v 1.29 2009/11/24 21:49:04 madan_ranganath Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.workflow;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Create SAML2 Hosted Provider Template.
 */
public class CreateSAML2HostedProviderTemplate {
    private CreateSAML2HostedProviderTemplate() {
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

    public static String createExtendedDataTemplate(
        String entityID,
        Map mapParams,
        String url
    )  {
        return createExtendedDataTemplate(entityID, mapParams, url, true);
    }
    
    public static String createExtendedDataTemplate(
        String entityID,
        Map mapParams,
        String url,
        boolean hosted
    )  {
        if (url == null) {
            url = getHostURL();
        }
        StringBuffer buff = new StringBuffer();
        String strHosted = (hosted) ? "1" : "0";
        buff.append(
            "<EntityConfig xmlns=\"urn:sun:fm:SAML:2.0:entityconfig\"\n"+
            "    xmlns:fm=\"urn:sun:fm:SAML:2.0:entityconfig\"\n" +
            "    hosted=\"" + strHosted + "\"\n" +
            "    entityID=\"" + entityID + "\">\n\n");

        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        if (idpAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(idpAlias);
            buildIDPConfigTemplate(buff, idpAlias, url, mapParams);
        }

        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        if (spAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(spAlias);
            buildSPConfigTemplate(buff, spAlias, url, mapParams);
        }

        String attraAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_AUTHORITY);
        if (attraAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(attraAlias);
            buildAttributeAuthorityConfigTemplate(buff, attraAlias, url,
                mapParams);
        }

        String attrqAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_QUERY_PROVIDER);
        if (attrqAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(attrqAlias);
            buildAttributeQueryConfigTemplate(buff, attrqAlias, url, mapParams);
        }

        String authnaAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AUTHN_AUTHORITY);
        if (authnaAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(authnaAlias);
            buildAuthnAuthorityConfigTemplate(buff, authnaAlias, url,
                mapParams);
        }

        String affiAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFILIATION);
        if (affiAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(affiAlias);
            buildAffiliationConfigTemplate(buff, affiAlias, url,
                mapParams);
        }

        String pdpAlias = (String)mapParams.get(MetaTemplateParameters.P_PDP);
        if (pdpAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(pdpAlias);
            buildPDPConfigTemplate(buff, pdpAlias, mapParams);
        }

        String pepAlias = (String)mapParams.get(MetaTemplateParameters.P_PEP);
        if (pepAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(pepAlias);
            buildPEPConfigTemplate(buff, pepAlias, mapParams);
        }

        buff.append("</EntityConfig>\n");
        return buff.toString();
    }

    private static void buildIDPConfigTemplate(
        StringBuffer buff,
        String idpAlias,
        String url,
        Map mapParams
    )  {
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

        buff.append(
            "    <IDPSSOConfig metaAlias=\"" + idpAlias + "\">\n" +
            "        <Attribute name=\"" + SAML2Constants.ENTITY_DESCRIPTION +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + idpSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + idpECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON +
            "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ENABLED +
            "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ATTRIBUTE +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE + "\">\n" +
            "            <Value>600</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.IDP_AUTHNCONTEXT_MAPPER_CLASS + "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultIDPAuthnContextMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.IDP_AUTHNCONTEXT_CLASSREF_MAPPING + "\">\n" +
            "            <Value>" +
            SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT + "|0||default</Value>\n"+
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.IDP_ACCOUNT_MAPPER +
            "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultIDPAccountMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.IDP_DISABLE_NAMEID_PERSISTENCE + "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.IDP_ATTRIBUTE_MAPPER +
            "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultIDPAttributeMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.ASSERTION_ID_REQUEST_MAPPER +
            "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultAssertionIDRequestMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.NAME_ID_FORMAT_MAP +
            "\">\n" +
            "           <Value>" + SAML2Constants.EMAIL_ADDRESS + "=mail" + "</Value>\n" +
            "           <Value>" + SAML2Constants.X509_SUBJECT_NAME + "=" + "</Value>\n" +
            "           <Value>" + SAML2Constants.WINDOWS_DOMAIN_QUALIFIED_NAME + "=" + "</Value>\n" +
            "           <Value>" + SAML2Constants.KERBEROS_PRINCIPAL_NAME + "=" + "</Value>\n" +
            "           <Value>" + SAML2Constants.UNSPECIFIED + "=" + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.IDP_ECP_SESSION_MAPPER_CLASS +"\">\n" +
            "            <Value>" +
            SAML2Constants.DEFAULT_IDP_ECP_SESSION_MAPPER_CLASS + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.ATTRIBUTE_MAP +
            "\">\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_NAMEID_ENCRYPTED + "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_ARTIFACT_RESOLVE_SIGNED + "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_LOGOUT_REQUEST_SIGNED + "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_LOGOUT_RESPONSE_SIGNED + "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_MNI_REQUEST_SIGNED + "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_MNI_RESPONSE_SIGNED + "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.DISCO_BOOTSTRAPPING_ENABLED + "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ASSERTION_CACHE_ENABLED + "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ASSERTION_NOTBEFORE_SKEW_ATTRIBUTE + "\">\n" +
            "            <Value>600</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.SAE_APP_SECRET_LIST + "\">\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\""+SAML2Constants.SAE_IDP_URL+"\">\n" +
            "            <Value>" + url + "/idpsaehandler/metaAlias" +
            idpAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\""+SAML2Constants.AUTH_URL+"\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.APP_LOGOUT_URL +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.IDP_SESSION_SYNC_ENABLED + "\">\n" +
            "           <Value>false</Value>\n" +
            "       </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.RELAY_STATE_URL_LIST + "\">\n" +
            "        </Attribute>\n" +
            "    </IDPSSOConfig>\n"
        );
    }

    private static void buildSPConfigTemplate(
        StringBuffer buff,
        String spAlias,
        String url,
        Map mapParams
    )  {
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
        
        buff.append(
            "    <SPSSOConfig metaAlias=\"" + spAlias + "\">\n" +
            "        <Attribute name=\"" + SAML2Constants.ENTITY_DESCRIPTION +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + spSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + spECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON +
            "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ENABLED +
            "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ATTRIBUTE +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.TRANSIENT_FED_USER +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.SP_ADAPTER_CLASS +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.SP_ADAPTER_ENV +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.SP_ACCOUNT_MAPPER +
            "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultSPAccountMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.USE_NAMEID_AS_SP_USERID +
            "\">\n" +
            "            <Value>false" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.SP_ATTRIBUTE_MAPPER +
            "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultSPAttributeMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.INCLUDE_REQUESTED_AUTHN_CONTEXT + "\">\n" +
            "            <Value>true</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.SP_AUTHCONTEXT_MAPPER + "\">\n" +
            "            <Value>" +
            SAML2Constants.DEFAULT_SP_AUTHCONTEXT_MAPPER + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\""
            + SAML2Constants.SP_AUTH_CONTEXT_CLASS_REF_ATTR + "\">\n" +
            "            <Value>" +
            SAML2Constants.SP_AUTHCONTEXT_CLASSREF_VALUE + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE + "\">\n" +
            "           <Value>" +
            SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE_VALUE + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.ATTRIBUTE_MAP +
            "\">\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.AUTH_MODULE_NAME +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.LOCAL_AUTH_URL +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.INTERMEDIATE_URL +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.DEFAULT_RELAY_STATE +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.APP_LOGOUT_URL +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.ASSERTION_TIME_SKEW +
            "\">\n" +
            "           <Value>300</Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_ATTRIBUTE_ENCRYPTED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_ASSERTION_ENCRYPTED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_NAMEID_ENCRYPTED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_POST_RESPONSE_SIGNED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_LOGOUT_REQUEST_SIGNED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_LOGOUT_RESPONSE_SIGNED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
                SAML2Constants.WANT_MNI_REQUEST_SIGNED+"\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.WANT_MNI_RESPONSE_SIGNED + "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.RESPONSE_ARTIFACT_MESSAGE_ENCODING + "\">\n" +
            "           <Value>" + SAML2Constants.URI_ENCODING + "</Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.SAE_APP_SECRET_LIST +
            "\">\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.SAE_SP_URL + "\">\n" +
            "           <Value>" + url + "/spsaehandler/metaAlias" + spAlias +
            "</Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.SAE_SP_LOGOUT_URL +
            "\">\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.ECP_REQUEST_IDP_LIST_FINDER_IMPL + "\">\n" +
            "           <Value>com.sun.identity.saml2.plugins.ECPIDPFinder</Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.ECP_REQUEST_IDP_LIST +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.ECP_REQUEST_IDP_LIST_GET_COMPLETE +"\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.ENABLE_IDP_PROXY +
            "\">\n" +
            "           <Value>false</Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.IDP_PROXY_LIST +
            "\">\n" +
            "           <Value></Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" + SAML2Constants.IDP_PROXY_COUNT +
            "\">\n" +
            "           <Value>0</Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.USE_INTRODUCTION_FOR_IDP_PROXY + "\">\n" +
            "           <Value>false</Value>\n" +
            "       </Attribute>\n" +
            "       <Attribute name=\"" +
            SAML2Constants.SP_SESSION_SYNC_ENABLED + "\">\n" +
            "           <Value>false</Value>\n" +
            "       </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.RELAY_STATE_URL_LIST + "\">\n" +
            "        </Attribute>\n" +
            "    </SPSSOConfig>\n"
        );
    }
    
    private static void buildAttributeAuthorityConfigTemplate(
        StringBuffer buff,
        String attraAlias,
        String url,
        Map mapParams
    )  {
        String attraECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_AUTHORITY_E_CERT);
        String attraSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_AUTHORITY_S_CERT);
        
        if (attraECertAlias == null) {
            attraECertAlias = "";
        }
        if (attraSCertAlias == null) {
            attraSCertAlias = "";
        }
        
        buff.append(
            "    <AttributeAuthorityConfig metaAlias=\"" + attraAlias + "\">\n"+
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + attraSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + attraECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.DEFAULT_ATTR_QUERY_PROFILE_ALIAS + "_" +
            SAML2Constants.ATTRIBUTE_AUTHORITY_MAPPER + "\">\n" +
            "            <Value>" +
            SAML2Constants.DEFAULT_ATTRIBUTE_AUTHORITY_MAPPER_CLASS +
            "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.X509_SUBJECT_ATTR_QUERY_PROFILE_ALIAS + "_" +
            SAML2Constants.ATTRIBUTE_AUTHORITY_MAPPER + "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.X509SubjectAttributeAuthorityMapper</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.X509_SUBJECT_DATA_STORE_ATTR_NAME + "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.ASSERTION_ID_REQUEST_MAPPER +
            "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultAssertionIDRequestMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "    </AttributeAuthorityConfig>\n"
        );
    }

    private static void buildAttributeQueryConfigTemplate(
        StringBuffer buff,
        String attrqAlias,
        String url,
        Map mapParams
    )  {
        String attrqSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_S_CERT);
        String attrqECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_E_CERT);
        
        if (attrqSCertAlias == null) {
            attrqSCertAlias = "";
        }
        if (attrqECertAlias == null) {
            attrqECertAlias = "";
        }        

        buff.append(
            "    <AttributeQueryConfig metaAlias=\"" + attrqAlias + "\">\n" +
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + attrqSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + attrqECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "    </AttributeQueryConfig>\n"
        );
    }

    private static void buildAuthnAuthorityConfigTemplate(
        StringBuffer buff,
        String authnaAlias,
        String url,
        Map mapParams
    )  {
        String authnaECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AUTHN_AUTHORITY_E_CERT);
        String authnaSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AUTHN_AUTHORITY_S_CERT);
        if (authnaECertAlias == null) {
            authnaECertAlias = "";
        }
        if (authnaSCertAlias == null) {
            authnaSCertAlias = "";
        }
        
        buff.append(
            "    <AuthnAuthorityConfig metaAlias=\"" + authnaAlias + "\">\n"+
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + authnaSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + authnaECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.ASSERTION_ID_REQUEST_MAPPER +
            "\">\n" +
            "            <Value>com.sun.identity.saml2.plugins.DefaultAssertionIDRequestMapper" +
            "</Value>\n" +
            "        </Attribute>\n" +
            "    </AuthnAuthorityConfig>\n"
        );
    }

    private static void buildAffiliationConfigTemplate(
        StringBuffer buff,
        String affiAlias,
        String url,
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
        
        buff.append(
            "    <AffiliationConfig metaAlias=\"" + affiAlias + "\">\n"+
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + affiSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + affiECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "    </AffiliationConfig>\n"
        );
    }

    private static void buildPDPConfigTemplate(
        StringBuffer buff,
        String pdpAlias,
        Map mapParams
    )  {
        String pdpECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PDP_E_CERT);
        String pdpSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PDP_S_CERT);
        if (pdpECertAlias == null) {
            pdpECertAlias = "";
        }
        if (pdpSCertAlias == null) {
            pdpSCertAlias = "";
        }
        buff.append(
            "    <XACMLPDPConfig metaAlias=\"" + pdpAlias + "\">\n" +
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + pdpSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + pdpECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON +
            "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_XACML_AUTHZ_DECISION_QUERY_SIGNED +  "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
            "        </Attribute>\n" +
            "   </XACMLPDPConfig>\n");
    }
    
    private static void buildPEPConfigTemplate(
        StringBuffer buff,
        String pepAlias,
        Map mapParams
    )  {
        String pepECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PEP_E_CERT);
        String pepSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PEP_S_CERT);
        if (pepECertAlias == null) {
            pepECertAlias = "";
        }
        if (pepSCertAlias == null) {
            pepSCertAlias = "";
        }
        buff.append(
            "   <XACMLAuthzDecisionQueryConfig metaAlias=\"" + pepAlias +
            "\">\n" +
            "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS +
            "\">\n" +
            "            <Value>" + pepSCertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
            "            <Value>" + pepECertAlias + "</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON +
            "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD +
            "\">\n" +
            "            <Value></Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_XACML_AUTHZ_DECISION_RESPONSED_SIGNED + "\">\n"+
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" +
            SAML2Constants.WANT_ASSERTION_ENCRYPTED +  "\">\n" +
            "            <Value>false</Value>\n" +
            "        </Attribute>\n" +
            "        <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
            "        </Attribute>\n" +            
            "  </XACMLAuthzDecisionQueryConfig>\n");
    }

    public static String buildMetaDataTemplate(
        String entityID,
        Map mapParams,
        String url
    ) throws SAML2MetaException {
        StringBuffer buff = new StringBuffer();
        if (url == null) {
            url = getHostURL();
        }
        buff.append(
            "<EntityDescriptor\n" +
            "    xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n" +
            "    entityID=\"" + entityID + "\">\n");

        String idpAlias = (String)mapParams.get(MetaTemplateParameters.P_IDP);
        if (idpAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(idpAlias);
            addIdentityProviderTemplate(buff, idpAlias, url, mapParams);
        }

        String spAlias = (String)mapParams.get(MetaTemplateParameters.P_SP);
        if (spAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(spAlias);
            addServiceProviderTemplate(buff, spAlias, url, mapParams);
        }

        String attraAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_AUTHORITY);
        if (attraAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(attraAlias);
            addAttributeAuthorityTemplate(buff, attraAlias, url, mapParams);
        }

        String attrqAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_QUERY_PROVIDER);
        if (attrqAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(attrqAlias);
            addAttributeQueryTemplate(buff, attrqAlias, url, mapParams);
        }

        String authnaAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AUTHN_AUTHORITY);
        if (authnaAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(authnaAlias);
            addAuthnAuthorityTemplate(buff, authnaAlias, url, mapParams);
        }

        String affiAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFILIATION);
        if (affiAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(affiAlias);
            String affiOwnerID = (String)mapParams.get(
                MetaTemplateParameters.P_AFFI_OWNERID);
            addAffiliationTemplate(buff, affiOwnerID, affiAlias,
                url, mapParams);
        }

        String pdpAlias = (String)mapParams.get(MetaTemplateParameters.P_PDP);
        if (pdpAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(pdpAlias);
            addPDPTemplate(buff, pdpAlias, url, mapParams);
        }

        String pepAlias = (String)mapParams.get(MetaTemplateParameters.P_PEP);
        if (pepAlias != null) {
            String realm = SAML2MetaUtils.getRealmByMetaAlias(pepAlias);
            addPEPTemplate(buff, url, mapParams);
        }
        buff.append("</EntityDescriptor>\n");
        return buff.toString();
    }

    private static void addIdentityProviderTemplate(
        StringBuffer buff,
        String idpAlias,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        String maStr = buildMetaAliasInURI(idpAlias);
        
        buff.append(
            "    <IDPSSODescriptor\n" +
            "        WantAuthnRequestsSigned=\"false\"\n" +
            "        protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n"
        );
        
        String idpSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_S_CERT);
        String idpECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_IDP_E_CERT);

        String idpSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            idpSCertAlias);

        if (idpSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + idpSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        
        String idpEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            idpECertAlias);
        if (idpEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + idpEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }
        
        buff.append(
            "        <ArtifactResolutionService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/ArtifactResolver" + maStr +
            "\"\n" +
            "            index=\"0\"\n" +
            "            isDefault=\"1\"/>\n" +
            
            "        <SingleLogoutService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
            "            Location=\"" + url + "/IDPSloRedirect" +maStr+ "\"\n" +
            "            ResponseLocation=\"" + url + "/IDPSloRedirect" +
            maStr + "\"/>\n" +
            "        <SingleLogoutService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
            "            Location=\"" + url + "/IDPSloPOST" + maStr + "\"\n" +
            "            ResponseLocation=\"" + url + "/IDPSloPOST" +
            maStr + "\"/>\n" +
            "        <SingleLogoutService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/IDPSloSoap" + maStr + "\"/>\n" +
            "        <ManageNameIDService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
            "            Location=\"" + url + "/IDPMniRedirect" +maStr+ "\"\n" +
            "            ResponseLocation=\"" + url + "/IDPMniRedirect" +
            maStr + "\"/>\n" +
            "        <ManageNameIDService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
            "            Location=\"" + url + "/IDPMniPOST" +maStr+ "\"\n" +
            "            ResponseLocation=\"" + url + "/IDPMniPOST" +
            maStr + "\"/>\n" +
            "        <ManageNameIDService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/IDPMniSoap" + maStr + "\"/>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.PERSISTENT + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.NAMEID_TRANSIENT_FORMAT + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.EMAIL_ADDRESS + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.UNSPECIFIED + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "          " + SAML2Constants.WINDOWS_DOMAIN_QUALIFIED_NAME + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.KERBEROS_PRINCIPAL_NAME + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.X509_SUBJECT_NAME + "\n" +
            "        </NameIDFormat>\n" +
            "        <SingleSignOnService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
            "            Location=\"" + url + "/SSORedirect" + maStr + "\"/>\n" +
            "        <SingleSignOnService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
            "            Location=\"" + url + "/SSOPOST" + maStr + "\"/>\n" +
            "        <SingleSignOnService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/SSOSoap" + maStr + "\"/>\n" +
            "        <NameIDMappingService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/NIMSoap" + maStr + "\"/>\n" +
            "        <AssertionIDRequestService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/AIDReqSoap/" + SAML2Constants.IDP_ROLE + maStr + "\"/>\n" +
            "        <AssertionIDRequestService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:URI\"\n" +
            "            Location=\"" + url + "/AIDReqUri/" + SAML2Constants.IDP_ROLE + maStr + "\"/>\n" +
            "    </IDPSSODescriptor>\n"
        );
    }

    private static void addServiceProviderTemplate(
        StringBuffer buff,
        String spAlias,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        String maStr = buildMetaAliasInURI(spAlias);
        buff.append(
            "    <SPSSODescriptor\n" +
            "        AuthnRequestsSigned=\"false\"\n" +
            "        WantAssertionsSigned=\"false\"\n" +
            "        protocolSupportEnumeration=\n" +
            "            \"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        
        String spSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_SP_S_CERT);
        String spECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_SP_E_CERT);

        String spSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            spSCertAlias);
        String spEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            spECertAlias);
        
        if (spSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + spSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        
        if (spEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + spEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }
        
        buff.append(
            "        <SingleLogoutService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
            "            Location=\"" + url + "/SPSloRedirect" + maStr + "\"\n"+
            "            ResponseLocation=\"" + url + "/SPSloRedirect" + maStr +
            "\"/>\n" +
            "        <SingleLogoutService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
            "            Location=\"" + url + "/SPSloPOST" + maStr + "\"\n"+
            "            ResponseLocation=\"" + url + "/SPSloPOST" + maStr +
            "\"/>\n" +
            "        <SingleLogoutService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/SPSloSoap" + maStr + "\"/>\n" +
            "        <ManageNameIDService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
            "            Location=\"" + url + "/SPMniRedirect" + maStr + "\"\n"+
            "            ResponseLocation=\"" + url + "/SPMniRedirect" + maStr +
            "\"/>\n" +
            "        <ManageNameIDService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
            "            Location=\"" + url + "/SPMniPOST" + maStr + "\"\n"+
            "            ResponseLocation=\"" + url + "/SPMniPOST" + maStr +
            "\"/>\n" +
            "        <ManageNameIDService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/SPMniSoap" + maStr + "\"\n" +
            "            ResponseLocation=\"" + url + "/SPMniSoap" + maStr +
            "\"/>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.PERSISTENT + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.NAMEID_TRANSIENT_FORMAT + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.EMAIL_ADDRESS + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.UNSPECIFIED + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "          " + SAML2Constants.WINDOWS_DOMAIN_QUALIFIED_NAME + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.KERBEROS_PRINCIPAL_NAME + "\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            " + SAML2Constants.X509_SUBJECT_NAME + "\n" +
            "        </NameIDFormat>\n" +
            "        <AssertionConsumerService\n" +
            "            isDefault=\"true\"\n" +
            "            index=\"0\"\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\"\n" +
            "            Location=\"" + url + "/Consumer" + maStr + "\"/>\n" +
            "        <AssertionConsumerService\n" +
            "            index=\"1\"\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
            "            Location=\"" + url + "/Consumer" + maStr + "\"/>\n" +
            "        <AssertionConsumerService\n" +
            "            index=\"2\"\n" +
            "            Binding=\"" + SAML2Constants.PAOS + "\"\n" +
            "            Location=\"" + url + "/Consumer/ECP" + maStr +
            "\"/>\n" +
            "    </SPSSODescriptor>\n");
    }

    private static void addAttributeAuthorityTemplate(
        StringBuffer buff,
        String attraAlias,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        String maStr = buildMetaAliasInURI(attraAlias);
        
        buff.append(
            "    <AttributeAuthorityDescriptor\n" +
            "        protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        
        String attraECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_AUTHORITY_E_CERT);
        String attraSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_AUTHORITY_S_CERT);

        String attraSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            attraSCertAlias);
        if (attraSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + attraSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        
        String attraEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            attraECertAlias);
        if (attraEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + attraEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }
        
        buff.append(
            "        <AttributeService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/AttributeServiceSoap/" +
            SAML2Constants.DEFAULT_ATTR_QUERY_PROFILE_ALIAS + maStr + "\"/>\n" +
            "        <AttributeService\n" +
            "            xmlns:x509qry=\"urn:oasis:names:tc:SAML:metadata:X509:query\"\n" +
            "            x509qry:supportsX509Query=\"true\"\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/AttributeServiceSoap/" +
            SAML2Constants.X509_SUBJECT_ATTR_QUERY_PROFILE_ALIAS + maStr +
            "\"/>\n" +
            "        <AssertionIDRequestService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/AIDReqSoap/" + SAML2Constants.ATTR_AUTH_ROLE + maStr + "\"/>\n" +
            "        <AssertionIDRequestService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:URI\"\n" +
            "            Location=\"" + url + "/AIDReqUri/" + SAML2Constants.ATTR_AUTH_ROLE + maStr + "\"/>\n" +
            "        <AttributeProfile>" +
            SAML2Constants.BASIC_ATTRIBUTE_PROFILE + "</AttributeProfile>\n" +
            "    </AttributeAuthorityDescriptor>\n");
    }
    
    private static void addAttributeQueryTemplate(
        StringBuffer buff,
        String attrqAlias,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        String maStr = buildMetaAliasInURI(attrqAlias);
        buff.append(
            "    <RoleDescriptor\n" +
            "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "        xmlns:query=\"urn:oasis:names:tc:SAML:metadata:ext:query\"\n" +
            "        xsi:type=\"query:AttributeQueryDescriptorType\"\n" +
            "        protocolSupportEnumeration=\n" +
            "            \"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        
        String attrqSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_S_CERT);
        String attrqECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_ATTR_QUERY_PROVIDER_E_CERT);
        String attrqSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            attrqSCertAlias);
        String attrqEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            attrqECertAlias);
        
        if (attrqSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + attrqSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        
        if (attrqEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + attrqEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }
        
        buff.append(
            "        <NameIDFormat>\n" +
            "            urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            urn:oasis:names:tc:SAML:2.0:nameid-format:transient\n" +
            "        </NameIDFormat>\n" +
            "        <NameIDFormat>\n" +
            "            urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\n" +
            "        </NameIDFormat>\n" +
            "    </RoleDescriptor>\n");
    }

    private static void addAuthnAuthorityTemplate(
        StringBuffer buff,
        String authnaAlias,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        String maStr = buildMetaAliasInURI(authnaAlias);
        
        buff.append(
            "    <AuthnAuthorityDescriptor\n" +
            "        protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        
        String authnaECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AUTHN_AUTHORITY_E_CERT);
        String authnaSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AUTHN_AUTHORITY_S_CERT);

        String authnaSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            authnaSCertAlias);
        if (authnaSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + authnaSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        
        String authnaEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            authnaECertAlias);
        if (authnaEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + authnaEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }
        
        buff.append(
            "        <AuthnQueryService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/AuthnQueryServiceSoap" +
            maStr + "\"/>\n" +
            "        <AssertionIDRequestService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
            "            Location=\"" + url + "/AIDReqSoap/" + SAML2Constants.AUTHN_AUTH_ROLE + maStr + "\"/>\n" +
            "        <AssertionIDRequestService\n" +
            "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:URI\"\n" +
            "            Location=\"" + url + "/AIDReqUri/" + SAML2Constants.AUTHN_AUTH_ROLE + maStr + "\"/>\n" +
            "    </AuthnAuthorityDescriptor>\n");
    }

    private static void addAffiliationTemplate(
        StringBuffer buff,
        String affiOwnerID,
        String affiAlias,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        String maStr = buildMetaAliasInURI(affiAlias);
        
        buff.append(
            "    <AffiliationDescriptor\n" +
            "        affiliationOwnerID=\"" + affiOwnerID + "\">\n");

        List affiMembers = (List)mapParams.get(
            MetaTemplateParameters.P_AFFI_MEMBERS);
        for(Iterator iter = affiMembers.iterator(); iter.hasNext(); ) {
            String affiMember = (String)iter.next();

            buff.append(
                "        <AffiliateMember>" + affiMember + "</AffiliateMember>\n");
        }

        String affiECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFI_E_CERT);
        String affiSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_AFFI_S_CERT);

        String affiSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            affiSCertAlias);
        if (affiSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + affiSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        
        String affiEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            affiECertAlias);
        if (affiEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + affiEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" +
                SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }
        buff.append(
            "    </AffiliationDescriptor>\n");
    }

    private static void addPDPTemplate(
        StringBuffer buff,
        String pdpAlias,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        String maStr = buildMetaAliasInURI(pdpAlias);
        buff.append(
            "    <XACMLPDPDescriptor " + 
            "protocolSupportEnumeration=" +
            "\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");

        String pdpECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PDP_E_CERT);
        String pdpSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PDP_S_CERT);

        String pdpSX509Cert  = SAML2MetaSecurityUtils.buildX509Certificate(
            pdpSCertAlias);
        String pdpEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            pdpECertAlias);

        if (pdpSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" + 
                                    SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + pdpSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }

        if (pdpEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" +
                                    SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + pdpEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" + 
                                    SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }

       buff.append(
            "         <XACMLAuthzService " +
                   "Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"" +
            " Location=\"" + url + "/saml2query" + maStr + "\"/>\n" +
            "    </XACMLPDPDescriptor>\n");
    }

    private static void addPEPTemplate(
        StringBuffer buff,
        String url,
        Map mapParams
    ) throws SAML2MetaException {
        buff.append("    <XACMLAuthzDecisionQueryDescriptor " +
            "WantAssertionsSigned=\"false\" " +
            "protocolSupportEnumeration=" +
            "\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");

        String pepECertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PEP_E_CERT);
        String pepSCertAlias = (String)mapParams.get(
            MetaTemplateParameters.P_PEP_S_CERT);
        
        String pepSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            pepSCertAlias);
        String pepEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
            pepECertAlias);

        if (pepSX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" +
                                    SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + pepSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }

        if (pepEX509Cert != null) {
            buff.append(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <KeyInfo xmlns=\"" + 
                                    SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + pepEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "            <EncryptionMethod Algorithm=" +
                "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                "                <KeySize xmlns=\"" + 
                                    SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                "128</KeySize>\n" +
                "            </EncryptionMethod>\n" +
                "        </KeyDescriptor>\n");
        }
        buff.append("    </XACMLAuthzDecisionQueryDescriptor>\n");
    }

    private static String buildMetaAliasInURI(String alias) {
        return "/" + SAML2MetaManager.NAME_META_ALIAS_IN_URI + alias;
    }
}
