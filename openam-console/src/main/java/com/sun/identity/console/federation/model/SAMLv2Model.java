/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAMLv2Model.java,v 1.34 2009/11/24 21:48:40 madan_ranganath Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock, Inc.
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.federation.SAMLv2AuthContexts;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SAMLv2Model
    extends EntityModel {
    
    //SAMLv2 General attributes
    public static final String TF_NAME = "tfName";
    public static final String TF_KEY_NAME = "keySize";
    public static final String TF_ALGORITHM = "Algorithm";
    public static final String httpRedirect = "HTTP-Redirect";
    public static final String httpPost = "HTTP-POST";
    public static final String soap = "SOAP";
    public static final String artifact = "Artifact";
    public static final String httpArtifact ="HTTP-Artifact";
    public static final String post = "POST";
    public static final String paos = "PAOS";
    public static final String httpRedirectBinding = 
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
    public static final String httpPostBinding = 
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    public static final String soapBinding = 
            "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";
    public static final String httpartifactBinding = 
            "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";
    public static final String paosBinding = 
            "urn:oasis:names:tc:SAML:2.0:bindings:PAOS";
    public static final String uriBinding =
            "urn:oasis:names:tc:SAML:2.0:bindings:URI";
    
    //SAMLv2 IDP Standard attributes
    public static final String WANT_AUTHN_REQ_SIGNED =
        "WantAuthnRequestsSigned";
    public static final String PROTOCOL_SUPP_ENUM =
        "protocolSupportEnumeration";
    public static final String ART_RES_LOCATION = "artLocation";
    public static final String ART_RES_INDEX = "index";
    public static final String ART_RES_ISDEFAULT = "isDefault";
    public static final String SINGLE_LOGOUT_HTTP_LOCATION =
        "slohttpLocation";
    public static final String SINGLE_LOGOUT_HTTP_RESP_LOCATION =
        "slohttpResponseLocation";
    public static final String SINGLE_LOGOUT_SOAP_LOCATION =
        "slosoapLocation";
    public static final String MANAGE_NAMEID_HTTP_LOCATION =
        "mnihttpLocation";
    public static final String MANAGE_NAMEID_HTTP_RESP_LOCATION =
        "mnihttpResponseLocation";
    public static final String MANAGE_NAMEID_SOAP_LOCATION =
        "mnisoapLocation";
    public static final String NAMEID_FORMAT = "nameidlist";
    public static final String SINGLE_SIGNON_HTTP_LOCATION =
        "ssohttpLocation";
    public static final String SINGLE_SIGNON_SOAP_LOCATION =
        "ssosoapLocation";
    public static final String NAME_ID_MAPPPING = "NameIDMappingService";
    public static final String SLO_POST_LOC = "slopostLocation";
    public static final String SLO_POST_RESPLOC = "slopostResponseLocation";
    public static final String MNI_POST_LOC = "mnipostLocation";
    public static final String MNI_POST_RESPLOC = "mnipostResponseLocation";
    public static final String SSO_SOAPS_LOC = "ssosoapsLocation";
    public static final String SINGLE_LOGOUT_DEFAULT =
        "singleChoiceSingleLogOutProfile";
    public static final String SINGLE_MANAGE_NAMEID_DEFAULT =
        "singleChoiceMangeNameIDProfile";
    public static final String COT_LIST = "cotlist";
    
    //SAMLv2 SP Standard attributes
    public static final String IS_AUTHN_REQ_SIGNED = "AuthnRequestsSigned";
    public static final String WANT_ASSERTIONS_SIGNED =
        "WantAssertionsSigned";
    public static final String SP_PROTOCOL_SUPP_ENUM =
        "protocolSupportEnumeration";
    public static final String SP_SINGLE_LOGOUT_HTTP_LOCATION =
        "slohttpLocation";
    public static final String SP_SINGLE_LOGOUT_HTTP_RESP_LOCATION =
        "slohttpResponseLocation";
    public static final String SP_SINGLE_LOGOUT_SOAP_LOCATION =
        "slosoapLocation";
    public static final String SP_MANAGE_NAMEID_HTTP_LOCATION =
        "mnihttpLocation";
    public static final String SP_MANAGE_NAMEID_HTTP_RESP_LOCATION =
        "mnihttpResponseLocation";
    public static final String SP_MANAGE_NAMEID_SOAP_LOCATION =
        "mnisoapLocation";
    public static final String SP_MANAGE_NAMEID_SOAP_RESP_LOCATION =
        "mnisoapResponseLocation";
    public static final String SP_SLO_POST_LOC = "slopostLocation";
    public static final String SP_SLO_POST_RESPLOC = "slopostResponseLocation";
    public static final String SP_MNI_POST_LOC = "mnipostLocation";
    public static final String SP_MNI_POST_RESPLOC = "mnipostResponseLocation";
    public static final String SP_LOGOUT_DEFAULT =
        "isDefaultSLO";
    public static final String SP_MNI_DEFAULT =
        "isDefaultMNI";
    
    //SAML2 IDP Extended Attributes
    public static final String IDP_SIGN_CERT_ALIAS =
        "signingCertAlias";
    public static final String IDP_SIGN_CERT_KEYPASS =
         "signingCertKeyPass";
    public static final String IDP_ENCRYPT_CERT_ALIAS =
        "encryptionCertAlias";
    public static final String IDP_BASIC_AUTH_ON = "basicAuthOn";
    public static final String IDP_BASIC_AUTH_USER = "basicAuthUser";
    public static final String IDP_BASIC_AUTH_PWD = "basicAuthPassword";
    public static final String IDP_AUTO_FED_ENABLED = "autofedEnabled";
    public static final String IDP_AUTO_FED_ATTR = "autofedAttribute";
    public static final String IDP_ATTR_MAP = "attributeMap";
    public static final String IDP_NAMEID_ENCRYPTED = 
        "wantNameIDEncrypted";
    public static final String NAMEID_FORMAT_MAP =
        "nameIDFormatMap";
    public static final String IDP_LOGOUT_REQ_SIGN =
        "wantLogoutRequestSigned";
    public static final String IDP_LOGOUT_RESP_SIGN =
        "wantLogoutResponseSigned";
    public static final String IDP_MNI_REQ_SIGN = "wantMNIRequestSigned";
    public static final String IDP_MNI_RESP_SIGN = "wantMNIResponseSigned";
    public static final String ASSERT_EFFECT_TIME = "assertionEffectiveTime";
    public static final String IDP_ACCT_MAPPER = "idpAccountMapper";
    public static final String IDP_AUTHN_CONTEXT_MAPPER =
        "idpAuthncontextMapper";
    public static final String IDP_AUTHN_CONTEXT_CLASS_REF_MAPPING_DEFAULT=
        "idpDefaultAuthnContext";
    public static final String IDP_AUTHN_CONTEXT_CLASS_REF_MAPPING =
        "idpAuthncontextClassrefMapping";
    public static final String IDP_ATTR_MAPPER = "idpAttributeMapper";
    public static final String ASSERT_NOT_BEFORE_TIMESKEW =
        "assertionNotBeforeTimeSkew";
    public static final String BOOT_STRAP_ENABLED =
        "discoveryBootstrappingEnabled";
    public static final String ARTIF_RESOLVE_SIGN =
        "wantArtifactResolveSigned";
    public static final String AUTH_URL =
        "AuthUrl";
    public static final String RP_URL = "RpUrl";
    public static final String APP_LOGOUT_URL = "appLogoutUrl";
    public static final String ASSERTION_CACHE_ENABLED =
        "assertionCacheEnabled";
    public static final String IDP_META_ALIAS =
        "metaAlias";
    public static final String IDP_SESSION_SYNC_ENABLED =
        "idpSessionSyncEnabled";
    public static final String PROXY_IDP_FINDER_CLASS = "proxyIDPFinderClass";
    public static final String ENABLE_PROXY_IDP_FINDER_FOR_ALL_SPS =
            "enableProxyIDPFinderForAllSPs";
    public static final String PROXY_IDP_FINDER_JSP =
            "proxyIDPFinderJSP";

    
    //SAML2 SP Extended Attributes
    public static final String SP_SIGN_CERT_ALIAS = "signingCertAlias";
    public static final String SP_SIGN_CERT_KEYPASS = "signingCertKeyPass";
    public static final String SP_ENCRYPT_CERT_ALIAS = "encryptionCertAlias";
    public static final String SP_BASIC_AUTH_ON = "basicAuthOn";
    public static final String SP_BASIC_AUTH_USER = "basicAuthUser";
    public static final String SP_BASIC_AUTH_PWD = "basicAuthPassword";
    public static final String SP_AUTO_FED_ENABLED = "autofedEnabled";
    public static final String SP_AUTO_FED_ATTR = "autofedAttribute";
    public static final String SP_ATTR_MAP = "attributeMap";
    public static final String SP_NAMEID_ENCRYPTED = "wantNameIDEncrypted";
    public static final String SP_LOGOUT_REQ_SIGN =
        "wantLogoutRequestSigned";
    public static final String SP_LOGOUT_RESP_SIGN =
        "wantLogoutResponseSigned";
    public static final String SP_MNI_REQ_SIGN = "wantMNIRequestSigned";
    public static final String SP_MNI_RESP_SIGN = "wantMNIResponseSigned";
    public static final String TRANSIENT_USER = "transientUser";
    public static final String SP_ACCT_MAPPER = "spAccountMapper";
    public static final String SP_USE_NAMEID = "useNameIDAsSPUserID";
    public static final String SP_AUTHN_CONTEXT_MAPPER = "spAuthncontextMapper";
    public static final String SP_ATTR_MAPPER = "spAttributeMapper";
    public static final String SP_AUTHN_CONTEXT_CLASS_REF_MAPPING_DEFAULT=
        "spDefaultAuthnContext";
    public static final String SP_AUTHN_CONTEXT_CLASS_REF_MAPPING =
        "spAuthncontextClassrefMapping";
    public static final String SP_AUTHN_CONTEXT_COMPARISON =
        "spAuthncontextComparisonType";
    public static final String SAML2_AUTH_MODULE = "saml2AuthModuleName";
    public static final String LOCAL_AUTH_URL = "localAuthURL";
    public static final String INTERMEDIATE_URL = "intermediateUrl";
    public static final String DEFAULT_RELAY_STATE = "defaultRelayState";
    public static final String ASSERT_TIME_SKEW = "assertionTimeSkew";
    public static final String WANT_ATTR_ENCRYPTED =
        "wantAttributeEncrypted";
    public static final String WANT_ASSERTION_ENCRYPTED =
        "wantAssertionEncrypted";
    public static final String WANT_ARTIF_RESP_SIGN = 
        "wantArtifactResponseSigned";
    public static final String WANT_POST_RESP_SIGN =
        "wantPOSTResponseSigned";
    public static final String SP_META_ALIAS =
        "metaAlias";
    public static final String ARTI_MSG_ENCODE = 
            "responseArtifactMessageEncoding";
    public static final String SP_SESSION_SYNC_ENABLED =
        "spSessionSyncEnabled";


    
    //IDP PROXY
    public static final String ALWAYS_IDP_PROXY = "alwaysIdpProxy";
    public static final String ENABLE_IDP_PROXY = "enableIDPProxy";
    public static final String IDP_PROXY_LIST = "idpProxyList";
    public static final String IDP_PROXY_COUNT = "idpProxyCount";
    public static final String IDP_PROXY_INTROD =
        "useIntroductionForIDPProxy";
    public static final String IDP_PROXY_FINDER =
        "useIDPFinder";
    
    //ECP IDP
    public static final String ATTR_IDP_ECP_SESSION_MAPPER =
        "idpECPSessionMapper";
    
    //ECP SP
    public static final String ATTR_ECP_REQUEST_IDP_LIST_FINDER_IMPL =
        "ECPRequestIDPListFinderImpl";
    public static final String ATTR_ECP_REQUEST_IDP_LIST =
        "ECPRequestIDPList";
    public static final String ATTR_ECP_REQUEST_IDP_LIST_GET_COMPLETE =
        "ECPRequestIDPListGetComplete";
    
    // SAE IDP
    public static final String ATTR_SAE_IDP_APP_SECRET_LIST = "saeAppSecretList";
    public static final String ATTR_SAE_IDP_URL = "saeIDPUrl";
    
    //SAE SP
    public static final String ATTR_SAE_SP_APP_SECRET_LIST = "saeAppSecretList";
    public static final String ATTR_SAE_SP_URL =  "saeSPUrl";
    public static final String ATTR_SAE_LOGOUT_URL = "saeSPLogoutUrl";

    // Relay State URL List IDP
    public static final String ATTR_RELAY_STATE_IDP_URL_LIST = "relayStateUrlList";
    
    // Relay State URL List SP
    public static final String ATTR_RELAY_STATE_SP_URL_LIST = "relayStateUrlList";
    
    // SAMLv2 Service Provider Adapter feature
     public static final String ATTR_SP_ADAPTER = "spAdapter";
     public static final String ATTR_SP_ADAPTER_ENV = "spAdapterEnv";

     // SAMLv2 Service Provider Do not Write Federation Info Feature
     public static final String ATTR_DO_NOT_WRITE_FEDERATION_INFO = "spDoNotWriteFederationInfo";

     //SAML AUTHORITY
     public static final String ATTR_SEFVICE_DEFAULT_LOCATION = 
             "attrSerdefaultLocation";
     public static final String ATTR_SEFVICE_LOCATION = "attrSerLocation";
     public static final String SUPPORTS_X509 = "supportsx";
     public static final String ATTRIBUTE_PROFILE = "AttributeProfile";
     public static final String AUTHN_QUERY_SERVICE = "authnQueryServLocation";
     public static final String ATTR_NAMEID_FORMAT = "attrnameidlist";
     public static final String ASSERTION_ID_SAOP_LOC = 
             "soapAssertionidrequest";
     public static final String ASSERTION_ID_URI_LOC = 
             "uriAssertionIDRequest";
     
     //SAML AUTHORITY Extended
     public static final String SIGN_CERT_ALIAS = "signingCertAlias";
     public static final String ENCRYPT_CERT_ALIAS = "encryptionCertAlias";
     public static final String DEF_AUTH_MAPPER = 
             "default_attributeAuthorityMapper";
     public static final String X509_AUTH_MAPPER = 
             "x509Subject_attributeAuthorityMapper";
     public static final String SUB_DATA_STORE = 
             "x509SubjectDataStoreAttrName";
     public static final String ASSERTION_ID_REQ_MAPPER = 
             "assertionIDRequestMapper";
     public static final String AFFILIATE_MEMBER = 
             "memberlist";
     public static final String AFFILIATE_OWNER = 
             "affiliationOwnerID";

    // XACML PDP/PEP
    public static final String ATTR_TXT_PROTOCOL_SUPPORT_ENUM =
        "txtProtocolSupportEnum";
    public static final String ATTR_XACML_AUTHZ_SERVICE_BINDING =
        "XACMLAuthzServiceBinding";
    public static final String ATTR_XACML_AUTHZ_SERVICE_LOCATION =
        "XACMLAuthzServiceLocation";
    public static final String ATTR_WANT_ASSERTION_SIGNED =
        "wantAssertionSigned";
    public static final String ATTR_SIGNING_CERT_ALIAS = "signingCertAlias";
    public static final String ATTR_ENCRYPTION_CERT_ALIAS =
        "encryptionCertAlias";
    public static final String ATTR_BASIC_AUTH_ON = "basicAuthOn";
    public static final String ATTR_BASIC_AUTH_USER = "basicAuthUser";
    public static final String ATTR_BASIC_AUTH_PASSWORD = "basicAuthPassword";
    public static final String ATTR_WANT_XACML_AUTHZ_DECISION_QUERY_SIGNED =
        "wantXACMLAuthzDecisionQuerySigned";
    public static final String ATTR_WANT_XACML_AUTHZ_DECISION_RESPONSE_SIGNED =
        "wantXACMLAuthzDecisionResponseSigned";
    public static final String ATTR_WANT_ASSERTION_ENCRYPTED =
        "wantAssertionEncrypted";
    public static final String ATTR_COTLIST = "cotlist";
    
    // SAMLv2 IDP Adapter feature 
    public static final String ATTR_IDP_ADAPTER = "idpAdapter"; 
    
    /**
     * Returns a map with standard identity provider attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with standard attribute values of Identity Provider.
     * @throws AMConsoleException if unable to retrieve the Identity Provider
     *     attrubutes based on the realm and entityName passed.
     */
    public Map getStandardIdentityProviderAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with extended identity provider attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with extended attribute values of Identity Provider.
     * @throws AMConsoleException if unable to retrieve the Identity Provider
     *     attrubutes based on the realm and entityName passed.
     */
    public Map getExtendedIdentityProviderAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with standard service provider attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with standard attribute values of Service Provider.
     * @throws AMConsoleException if unable to retrieve the Service Provider
     *     attrubutes based on the realm and entityName passed.
     */
    public Map getStandardServiceProviderAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;

    /**
     * Returns a List with Assertion Consumer Service attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return List with Assertion Consumer values of Service Provider.
     * @throws AMConsoleException if unable to retrieve the Service Provider
     *     Assertion Consumer values based on the realm and entityName passed.
     */
    
    public List getAssertionConsumerServices(
        String realm,
        String entityName
        ) throws AMConsoleException;

    /*
     *Returns a new AssertionConsumerServiceElement.
     *
     * @throws AMConsoleException if unable to retrieve. 
     */
    AssertionConsumerServiceElement getAscObject() throws AMConsoleException;
    
    /**
     * Returns a map with extended service provider attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with extended attribute values of Service Provider.
     * @throws AMConsoleException if unable to retrieve the Service Provider
     *     attrubutes based on the realm and entityName passed.
     */
    public Map getExtendedServiceProviderAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Saves the standard attribute values for the Identiy Provider.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param idpStdValues Map which contains the standard attribute values.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    void setIDPStdAttributeValues(
        String realm,
        String entityName,
        Map idpStdValues
        ) throws AMConsoleException;
    
    /**
     * Saves the extended attribute values for the Identity Provider.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param idpExtValues Map which contains the standard attribute values.
     * @param location has the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    void setIDPExtAttributeValues(
        String realm,
        String entityName,
        Map idpExtValues,
        String location
        ) throws AMConsoleException;
    
    /**
     * Saves the standard attribute values for the Service Provider.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param spStdValues Map which contains the standard attribute values.
     * @param assertionConsumer List with assertion consumer service values.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    void setSPStdAttributeValues(
        String realm,
        String entityName,
        Map spStdValues,
        List assertionConsumer
        ) throws AMConsoleException;
    
    /**
     * Saves the extended attribute values for the Service Provider.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param spExtValues Map which contains the standard attribute values.
     * @param location has the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    void setSPExtAttributeValues(
        String realm,
        String entityName,
        Map spExtValues,
        String location
        ) throws AMConsoleException;
    
    /**
     * Returns SAMLv2 Extended Service Provider attribute values.
     *
     * @return SAMLv2 Extended Service Provider attribute values.
     */
    Map getSPEXDataMap();
    
    /**
     * Returns SAMLv2 Extended Service Provider values for Assertion Content.
     *
     * @return SAMLv2 Extended Service Provider values for Assertion Content.
     */
    Map getSPEXACDataMap();
    
    /**
     * Returns SAMLv2 Extended Service Provider values for Assertion Processing.
     *
     * @return SAMLv2 Extended Service Provider values for Assertion Processing.
     */
    Map getSPEXAPDataMap();
    
    /**
     * Returns SAMLv2 Extended Service Provider attribute values for Services.
     *
     * @return SAMLv2 Extended Service Provider attribute values for Services.
     */
    Map getSPEXSDataMap();
    
    /**
     * Returns SAMLv2 Extended Service Provider attribute values for Advanced.
     *
     * @return SAMLv2 Extended Service Provider attribute values for Advanced.
     */
    Map getSPEXAdDataMap();   
    
    /**
     * Returns SAMLv2 Extended Identity Provider attribute values.
     *
     * @return SAMLv2 Extended Identity Provider attribute values.
     */
    Map getIDPEXDataMap();
    
    /**
     * Returns SAMLv2 Extended Identity Provider values for Assertion Content.
     *
     * @return SAMLv2 Extended Identity Provider values for Assertion Content.
     */
    Map getIDPEXACDataMap();
    
    /**
     * Returns SAMLv2 Extended Identity Provider values for Assertion Processing.
     *
     * @return SAMLv2 Extended Identity Provider values for Assertion Processing.
     */
    Map getIDPEXAPDataMap();
    
    /**
     * Returns SAMLv2 Extended Identity Provider attribute values for Services.
     *
     * @return SAMLv2 Extended Identity Provider attribute values for Services.
     */
    Map getIDPEXSDataMap();
    
    /**
     * Returns SAMLv2 Extended Identity Provider attribute values for Advanced.
     *
     * @return SAMLv2 Extended Identity Provider attribute values for Advanced.
     */
    Map getIDPEXAdDataMap();
         
    /**
     * Returns SAMLv2 Xacml PEP ExtendedMeta
     *
     * @return SAMLv2 Xacml PEP Extended Meta.
     */
  
    public Map getXacmlPEPExtendedMetaMap();
   
     /**
     * Returns SAMLv2 Xacml PDP ExtendedMeta
     *
     * @return SAMLv2 Xacml PDP Extended Meta.
     */
    public Map getXacmlPDPExtendedMetaMap();
  
    /**
     * Returns a Map of PEP descriptor data.(Standard Metadata)
     *
     * @param realm realm of Entity
     * @param entityName entity name of Entity Descriptor.
     * @return key-value pair Map of PEP descriptor data.
     * @throws AMConsoleException if unable to retrieve the PEP
     *         standard metadata attribute
     */
    public Map getPEPDescriptor(String realm, String entityName)
    throws AMConsoleException;
    /**
     * Returns a Map of PDP descriptor data.(Standard Metadata)
     *
     * @param realm realm of Entity
     * @param entityName entity name of Entity Descriptor.
     * @return key-value pair Map of PDP descriptor data.
     * @throws AMConsoleException if unable to retrieve the PDP
     *         standard metadata attribute
     */
    public Map getPDPDescriptor(String realm, String entityName)
    throws AMConsoleException;
    
    /**
     * Returns a Map of PEP config data.(Extended Metadata)
     *
     * @param realm realm of Entity
     * @param entityName entity name of Entity Descriptor.
     * @param location entity is remote or hosted
     * @throws AMConsoleException if unable to retrieve the PEP
     *         extended metadata attribute
     */
    public Map getPEPConfig(
        String realm,
        String entityName,
        String location
        ) throws AMConsoleException;
    
    /**
     * Returns a Map of PDP Config data.(Extended Metadata)
     *
     * @param realm where entity exists.
     * @param entityName entity name of Entity Descriptor.
     * @param location entity is remote or hosted
     * @throws AMConsoleException if unable to retrieve the PEP
     *         extended metadata attribute
     */
    public Map getPDPConfig(
        String realm,
        String entityName,
        String location
        ) throws AMConsoleException;
    
    /**
     * save data for PDP descriptor data.(Standard Metadata)
     *
     * @param realm where entity exists.
     * @param entityName entity name of Entity Descriptor.
     * @param attrValues key-value pair Map of PDP standed data.
     * @throws AMConsoleException if there is an error
     */
    public void updatePDPDescriptor(
        String realm,
        String entityName,
        Map attrValues
        ) throws AMConsoleException;
    
    /**
     * save data for PDP Config data.(Extended Metadata)
     *
     * @param realm realm of Entity
     * @param entityName entity name of Entity Descriptor.
     * @param location entity is remote or hosted
     * @param attrValues key-value pair Map of PDP extended config.
     */
    public void updatePDPConfig(
        String realm,
        String entityName,
        String location,
        Map attrValues
        ) throws AMConsoleException;
    
    /**
     * save data for PEP descriptor data.(Standard Metadata)
     *
     * @param realm realm of Entity
     * @param entityName name of Entity Descriptor.
     * @param attrValues key-value pair Map of PEP descriptor data.
     * @throws AMConsoleException if there is an error
     */
    public void updatePEPDescriptor(
        String realm,
        String entityName,
        Map attrValues
        ) throws AMConsoleException;
    
    
    /**
     * Save the configuration data for the policy enforcment point (PEP) entity.
     *
     * @param realm where entity exists.
     * @param entityName name of Entity Descriptor.
     * @param location entity is remote or hosted.
     * @param attrValues key-value pair Map of PEP extended config.
     * @throws AMConsoleException if there is an error
     */
    public void updatePEPConfig(
        String realm,
        String entityName,
        String location,
        Map attrValues
        ) throws AMConsoleException;
    
        
    /**
     * Returns the object of Auththentication Contexts in IDP.
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.
     * @return SAMLv2AuthContexts contains IDP authContexts values.
     * @throws AMConsoleException if unable to retrieve the IDP 
     *         Authentication Contexts          
     */
    public SAMLv2AuthContexts getIDPAuthenticationContexts(
        String realm,
        String entityName       
    ) throws AMConsoleException ;
    
    /**
     * Returns  the object of Auththentication Contexts in SP.
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.    
     * @return SAMLv2AuthContexts contains SP authContexts values.
     * @throws AMConsoleException if unable to retrieve the SP
     *         Authentication Contexts
     */
    public SAMLv2AuthContexts getSPAuthenticationContexts(
        String realm,
        String entityName       
    ) throws AMConsoleException ;
    
     /**
     * update IDP Authentication Contexts
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.         
     * @param cxt SAMLv2AuthContexts object contains IDP 
     *        Authentication Contexts values
     * @throws AMConsoleException if fails to update IDP
     *         Authentication Contexts.
     */
    public void updateIDPAuthenticationContexts(
        String realm,
        String entityName, 
        SAMLv2AuthContexts cxt
    ) throws AMConsoleException;
    
     /**
     * update SP Authentication Contexts
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.       
     * @param cxt SAMLv2AuthContexts object contains SP 
     *        Authentication Contexts values
     * @throws AMConsoleException if fails to update SP
     *         Authentication Contexts.
     */
    public void updateSPAuthenticationContexts(
        String realm,
        String entityName, 
        SAMLv2AuthContexts cxt
    ) throws AMConsoleException;
    
    /**
     * Returns a map with standard AttributeAuthority attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with AttributeAuthority values.
     * @throws AMConsoleException if unable to retrieve the std
     *      AttributeAuthority values based on the realm and entityName passed.
     */
    public Map getStandardAttributeAuthorityAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with extended AttributeAuthority attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with extended AttributeAuthority values.
     * @throws AMConsoleException if unable to retrieve the  extended
     *  AttributeAuthority attributes based on the realm and entityName passed.
     */
    public Map getExtendedAttributeAuthorityAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with standard AuthnAuthority attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with AuthnAuthority values.
     * @throws AMConsoleException if unable to retrieve std AuthnAuthority 
     *       values based on the realm and entityName passed.
     */
    public Map getStandardAuthnAuthorityAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with extended AuthnAuthority attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with extended AuthnAuthority values.
     * @throws AMConsoleException if unable to retrieve ext AuthnAuthority
     *     attributes based on the realm and entityName passed.
     */
    public Map getExtendedAuthnAuthorityAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with standard AttrQuery attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with AttrQuery values.
     * @throws AMConsoleException if unable to retrieve std AttrQuery 
     *       values based on the realm and entityName passed.
     */
    public Map getStandardAttrQueryAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with extended AttrQuery attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with extended AttrQuery values.
     * @throws AMConsoleException if unable to retrieve ext AttrQuery
     *     attributes based on the realm and entityName passed.
     */
    public Map getExtendedAttrQueryAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Saves the standard attribute values for Attribute Authority.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param attrAuthValues Map which contains standard attribute auth values.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setStdAttributeAuthorityValues(
        String realm,
        String entityName,
        Map attrAuthValues
        ) throws AMConsoleException;
    
    /**
     * Returns SAMLv2 Extended Attribute Authority values.
     *
     * @return SAMLv2 Extended Attribute Authority values.
     */
    public Map getattrAuthEXDataMap();
    
    /**
     * Saves the extended attribute values for Attribute Authority.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param attrAuthExtValues Map which contains the extended values.
     * @param location has the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setExtAttributeAuthorityValues(
        String realm,
        String entityName,
        Map attrAuthExtValues,
        String location
        ) throws AMConsoleException;
    
    /**
     * Saves the standard attribute values for Authn Authority.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param authnAuthValues Map which contains standard authn authority values.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setStdAuthnAuthorityValues(
        String realm,
        String entityName,
        Map authnAuthValues
        ) throws AMConsoleException;
    
    /**
     * Returns SAMLv2 Extended Authn Authority values.
     *
     * @return SAMLv2 Extended Authn Authority values.
     */
    public Map getauthnAuthEXDataMap();
    
    /**
     * Saves the extended attribute values for Authn Authority.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param authnAuthExtValues Map which contains the extended authn values.
     * @param location has the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setExtauthnAuthValues(
        String realm,
        String entityName,
        Map authnAuthExtValues,
        String location
        ) throws AMConsoleException;
    
     /**
     * Saves the standard attribute values for Attribute Query.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param attrQueryValues Map which contains standard attribute query values.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setStdAttributeQueryValues(
        String realm,
        String entityName,
        Map attrQueryValues
        ) throws AMConsoleException;
    
    /**
     * Returns SAMLv2 Extended Attribute Query values.
     *
     * @return SAMLv2 Extended Attribute Query values.
     */
    public Map getattrQueryEXDataMap();
    
    /**
     * Saves the extended attribute values for Attribute Query.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param attrQueryExtValues Map which contains the extended values.
     * @param location has the information whether remote or hosted.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setExtAttributeQueryValues(
        String realm,
        String entityName,
        Map attrQueryExtValues,
        String location
        ) throws AMConsoleException;
    
    /**
     *Returns the metaAlias of the entity.
     *
     *@param realm to which the entity belongs.
     *@param entityName is the entity id.
     *@param role the Role of entity.
     *@return the metaAlias of the entity.
     *@throws AMConsoleException if unable to retrieve metaAlias.
     */
    public String getMetaalias(
            String realm, 
            String entityName,
            String role
            ) throws AMConsoleException;
    
    /**
     * Returns a map with standard Affiliation attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with Affiliation values.
     * @throws AMConsoleException if unable to retrieve standard Affiliation 
     *       values based on the realm and entityName passed.
     */
    public Map getStandardAffiliationAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Returns a map with extended Affiliation attributes and values.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @return Map with extended Affiliation values.
     * @throws AMConsoleException if unable to retrieve extended Affiliation
     *     attributes based on the realm and entityName passed.
     */
    public Map getExtendedAffiliationyAttributes(
        String realm,
        String entityName
        ) throws AMConsoleException;
    
    /**
     * Saves the standard attribute values for Affilaition.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param affiliationValues Map which contains standard affiliation values.
     * @param members Set which contains all members.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void setStdAffilationValues(
        String realm,
        String entityName,
        Map affiliationValues,
        Set members
        ) throws AMConsoleException;
    
    /**
     * Returns a set with all Service Providers under the realm.
     *
     * @param realm to which the entity belongs.
     * @return Set with all service providers under the realm passed.
     * @throws AMConsoleException if unable to retrieve service providers.
     *
     */
    public Set getallSPEntities(String realm) throws AMConsoleException;
    
    /**
     * Saves the signing and encryption values for entity.
     *
     * @param realm to which the entity belongs.
     * @param entityName is the entity id.
     * @param extValues Map which contains the extended attribute values.
     * @param stdValues Map which contains the standard attribute values.
     * @param isIDP has information whether entity is an idp or sp.
     * @throws AMConsoleException if saving of attribute value fails.
     */
    public void updateKeyinfo(
        String realm,
        String entityName,
        Map extValues,
        Map stdValues,
        boolean isIDP
        ) throws AMConsoleException;

}
