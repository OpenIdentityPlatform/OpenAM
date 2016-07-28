/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAML2Constants.java,v 1.44 2009/11/24 21:53:02 madan_ranganath Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */
package com.sun.identity.saml2.common;

import com.sun.identity.cot.COTConstants;

/**
 * This interface defines constants common to all SAMLv2 elements.
 *
 * @supported.all.api
 */
public interface SAML2Constants {

    /**
     * XML name space URI
     */
    public String NS_XML = "http://www.w3.org/2000/xmlns/";
    
    /**
     * String used to declare SAMLv2 assertion namespace prefix.
     */
    public String ASSERTION_PREFIX = "saml:";
    
    /**
     * String used to declare SAMLv2 assertion namespace.
     */
    public String ASSERTION_DECLARE_STR =
    " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"";
    
    /**
     * SAMLv2 assertion namespace URI.
     */
    public String ASSERTION_NAMESPACE_URI =
    "urn:oasis:names:tc:SAML:2.0:assertion";
    
    /**
     * Default namespace attribute for <code>Action</code>.
     */
    public String ACTION_NAMESPACE_NEGATION =
    "urn:oasis:names:tc:SAML:1.0:action:rwedc-negation";
    
    /**
     * String used to declare SAMLv2 protocol namespace prefix.
     */
    public String PROTOCOL_PREFIX = "samlp:";
    
    /**
     * String used to declare SAMLv2 protocol namespace.
     */
    public String PROTOCOL_NAMESPACE = "urn:oasis:names:tc:SAML:2.0:protocol";
    
    /**
     * String used to declare SAMLv2 protocol namespace.
     */
    public String PROTOCOL_DECLARE_STR =
                " xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"";
    
    /**
     * String used to represent HTTP Redirect Binding.
     */
    public String HTTP_REDIRECT =
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

    /**
     * String used to represent SOAP Binding.
     */
    public String SOAP =
                "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

    /**
     * String used to represent PAOS Binding.
     */
    public static final String PAOS =
                "urn:oasis:names:tc:SAML:2.0:bindings:PAOS";

    /**
     * String used to represent HTTP POST Binding.
     */
    public String HTTP_POST =
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    /**
     * String used to represent HTTP ARTIFACT Binding.
     */
    public String HTTP_ARTIFACT =
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";

    /**
     * String used to represent URI Binding.
     */
    public String URI =
                "urn:oasis:names:tc:SAML:2.0:bindings:URI";

    /**
     * String to represent Name Identifier Format name space
     */
    public String NAMEID_FORMAT_NAMESPACE=
                "urn:oasis:names:tc:SAML:2.0:nameid-format:";

    /**
     * String to represent Name Identifier Format name space
     * version 1.1
     */
    public static final String NAMEID_FORMAT_NAMESPACE_V_1_1=
          "urn:oasis:names:tc:SAML:1.1:nameid-format:";

    /**
     * String to represent Encrypted Format Name Identifier
     */
    public String ENCRYPTED =
                NAMEID_FORMAT_NAMESPACE + "encrypted";


    /**
     * String to represent Persitent Name Identifier
     */
    public String PERSISTENT =
                NAMEID_FORMAT_NAMESPACE + "persistent";
    
    /**
     * String to represent Unspecified Name Identifier
     */
    public String UNSPECIFIED =
        NAMEID_FORMAT_NAMESPACE_V_1_1 + "unspecified";
  
    /**
     * String to represent Email Address Name Identifier
     */
    public String EMAIL_ADDRESS =
        NAMEID_FORMAT_NAMESPACE_V_1_1 + "emailAddress";

    /**
     * String to represent Entity Name Identifier
     */
    public String ENTITY =
                NAMEID_FORMAT_NAMESPACE + "entity";

    /**
     * String to represent X509 Subejct Name Identifier
     */
    public String X509_SUBJECT_NAME =
        NAMEID_FORMAT_NAMESPACE_V_1_1 + "X509SubjectName";

    /**
     * String to represent Windows Domain Qualified Name Identifier
     */
    public String WINDOWS_DOMAIN_QUALIFIED_NAME =
        NAMEID_FORMAT_NAMESPACE_V_1_1 + "WindowsDomainQualifiedName";

    /**
     * String to represent Kerberos Principal Name Identifier
     */
    public String KERBEROS_PRINCIPAL_NAME =
        NAMEID_FORMAT_NAMESPACE + "kerberos";

    /**
     * String to represent the authentication service url
     */
    public String AUTH_URL = "AuthUrl";

    /**
     * Used when the SAML endpoints are RP'd to a non-server/site URL, typically
     * to DAS
     */
    public String RP_URL = "RpUrl";

    /**
     * Strings represent primitive top-level StatusCode values 
     */
    public String SUCCESS =
        "urn:oasis:names:tc:SAML:2.0:status:Success";

    public String REQUESTER =
        "urn:oasis:names:tc:SAML:2.0:status:Requester";

    public String RESPONDER =
        "urn:oasis:names:tc:SAML:2.0:status:Responder";

    public String NOPASSIVE =
        "urn:oasis:names:tc:SAML:2.0:status:NoPassive";
    
    public String VERSION_MISMATCH =
        "urn:oasis:names:tc:SAML:2.0:status:VersionMismatch";

    public String UNKNOWN_PRINCIPAL =
        "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal";

    public String AUTHN_FAILED =
        "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed";

    public String INVALID_ATTR_NAME_OR_VALUE =
        "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue";

    public String INVALID_NAME_ID_POLICY =
        "urn:oasis:names:tc:SAML:2.0:status:InvalidNameIDPolicy";

    public String NO_AUTHN_CONTEXT =
        "urn:oasis:names:tc:SAML:2.0:status:NoAuthnContext";

    /**
     * Basic name format
     */
    public String BASIC_NAME_FORMAT =
        "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";

    /**
     * Basic attribute profile
     */
    public String BASIC_ATTRIBUTE_PROFILE =
        "urn:oasis:names:tc:SAML:2.0:profiles:attribute:basic";

    /**
     * Attribute Query default profile
     */
    public static final String DEFAULT_ATTR_QUERY_PROFILE =
        "urn:oasis:names:tc:SAML:2.0:profiles:query";

    /**
     * Attribute Query x509 Subject profile
     */
    public static final String X509_SUBJECT_ATTR_QUERY_PROFILE =
        "urn:oasis:names:tc:SAML:2.0:profiles:query:attribute:X509";

    /**
     * Attribute Query default profile alias
     */
    public static final String DEFAULT_ATTR_QUERY_PROFILE_ALIAS =
        "default";

    /**
     * Attribute Query x509 Subject profile alias
     */
    public static final String X509_SUBJECT_ATTR_QUERY_PROFILE_ALIAS =
        "x509Subject";

    /**
     * Strings represent subject confirmation methods
     */
    public String SUBJECT_CONFIRMATION_METHOD_BEARER =
        "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    
    /**
     * Confirmation method for holder of key
     */
    public String SUBJECT_CONFIRMATION_METHOD_HOLDER_OF_KEY =
        "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key";
    
    /**
     * Confirmation method for sender vouches
     */
    public String SUBJECT_CONFIRMATION_METHOD_SENDER_VOUCHES =
        "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches";
    
    /**
     * Session Property name indicating if response is redirected or not
     */
    public String RESPONSE_REDIRECTED = "SAML2ResponseRedirected";

    /**
     * Length for SAMLv2 IDs.
     */
    public int ID_LENGTH = 20;
    
    /**
     * SAMLv2 Version String
     */
    public String VERSION_2_0 = "2.0";

    /**
     * SAMLRequest query parameter name
     */
    public String SAML_REQUEST = "SAMLRequest";

    /**
     * SAMLResponse query parameter name
     */
    public String SAML_RESPONSE = "SAMLResponse";
    
    
    /**
     * Maximum value of unsigned integer/short type.
     */
    public int MAX_INT_VALUE=65535;
    
    /**
     * Start Tag for XML String
     */
    public String START_TAG="<";
    /**
     * End Tag for XML String
     */
    public String END_TAG =">";
    
    /**
     * Constant for space
     */
    public String SPACE=" ";
    /**
     * Constant for equal
     */
    public String EQUAL= "=";
    
    /**
     * Constant for quote
     */
    public String QUOTE = "\"";
    
    /**
     * Constant for newline
     */
    public String NEWLINE= "\n";
    
    /**
     * Constant for xml name space
     */
    public String NAMESPACE_PREFIX="xmlns";
    
    /**
     * Constant for SAML2 end tag
     */
    public String SAML2_END_TAG="</samlp:";
    
    /**
     * Constant for AuthnRequest
     */
    public String AUTHNREQUEST="AuthnRequest";

    /**
     * Constant for LogoutRequest
     */
    public String LOGOUT_REQUEST="LogoutRequest";

    /**
     * Constant for LogoutResponse
     */
    public String LOGOUT_RESPONSE="LogoutResponse";

    /**
     * Constant for AssertionIDRequest
     */
    public String ASSERTION_ID_REQUEST = "AssertionIDRequest";

    /**
     * Constant for AttributeQuery
     */
    public String ATTRIBUTE_QUERY = "AttributeQuery";

    /**
     * Constant for AuthnQuery
     */
    public String AUTHN_QUERY = "AuthnQuery";

    /**
     * Constant for NameIDMappingRequest
     */
    public String NAME_ID_MAPPING_REQUEST = "NameIDMappingRequest";

    /**
     * Constant for NameIDMappingResponse
     */
    public String NAME_ID_MAPPING_RESPONSE = "NameIDMappingResponse";

    /**
     * Constant for AssertionIDRef
     */
    public String ASSERTION_ID_REF = "AssertionIDRef";

    /**
     * Constant for Attribute
     */
    public String ATTRIBUTE="Attribute";

    /**
     * Constant for SessionIndex
     */
    public String SESSION_INDEX="SessionIndex";

    /**
     * Constant for BaseID
     */
    public String BASEID="BaseID";

    /**
     * Constant for NameID
     */
    public String NAMEID="NameID";

    /**
     * Constant for EncryptedID
     */
    public String ENCRYPTEDID="EncryptedID";
    
    /**
     * Constant for Reason
     */
    public String REASON="Reason";

    /**
     * Constant for NotOnOrAfter
     */
    public String NOTONORAFTER="NotOnOrAfter";

    /**
     * Constant for NotOnOrAfter
     */
    public String NOTBEFORE="NotBefore";

    /**
     * Constant for InResponseTo
     */
    public String INRESPONSETO="InResponseTo";
   
    /**
     * Constant for ID
     */
    public String ID="ID";
    
    
    /**
     * Constant for Version
     */
    public String VERSION="Version";
    
    
    /**
     * Constant for IssueInstant
     */
    public String ISSUE_INSTANT="IssueInstant";
    
    /**
     * Constant for Destination
     */
    public String DESTINATION="Destination";
    
    /**
     * Constant for Value
     */
    public String VALUE="Value";
    
    /**
     * Constant for Destination
     */
    public String CONSENT="Consent";
    
    /**
     * Constant for Issuer
     */
    public String ISSUER="Issuer";
    
    
    /**
     * Constant for Signature
     */
    public String SIGNATURE="Signature";
    
    /**
     * Constant for forceAuthn attribute
     */
    public String FORCEAUTHN="ForceAuthn";
    
    /**
     * Constant for IsPassive attribute
     */
    public String ISPASSIVE="IsPassive";
    
    /**
     * Constant for AllowCreate attribute
     */
    public String ALLOWCREATE="AllowCreate";

    /**
     * Constant for ProtocolBinding attribute
     */
    public String PROTOBINDING="ProtocolBinding";
    
    /**
     * Constant for mustUnderstand attribute
     */
    public static final String MUST_UNDERSTAND = "mustUnderstand";

    /**
     * Constant for actor attribute
     */
    public static final String ACTOR = "actor";

    /**
     * Constant for Binding parameter name
     */
    public String BINDING="binding";

    /**
     * Constant for reqBinding parameter name
     */
    public String REQ_BINDING = "reqBinding";

    /**
     * Constant for affiliationID parameter name
     */
    public String AFFILIATION_ID = "affiliationID";

    /**
     * Constant for Binding namespace
     */
    public String BINDING_PREFIX =
    "urn:oasis:names:tc:SAML:2.0:bindings:";

    /**
     * Constant for AssertionConsumerServiceIndex attribute
     */
    public String ASSERTION_CONSUMER_SVC_INDEX=
    "AssertionConsumerServiceIndex";
    /**
     * Constant for AssertionConsumerServiceURL attribute
     */
    public String ASSERTION_CONSUMER_SVC_URL=
    "AssertionConsumerServiceURL";
    /**
     * Constant for AttributeConsumingServiceIndex attribute
     */
    public String ATTR_CONSUMING_SVC_INDEX=
    "AttributeConsumingServiceIndex";
    /**
     * Constant for ProviderName attribute
     */
    public String PROVIDER_NAME="ProviderName";
    
    /**
     * Constant for Subject Element
     */
    public String SUBJECT="Subject";

    /**
     * Constant for AuthnRequest object
     */
    public String AUTHN_REQUEST = "AuthnRequest";
    
    /**
     * Constant for NameIDPolicy Element
     */
    public String NAMEID_POLICY="NameIDPolicy";
    
    /**
     * Constant for Conditions Element.
     */
    public String CONDITIONS="Conditions";
    
    /**
     * Constant for RequestedAuthnContext Element.
     */
    public String REQ_AUTHN_CONTEXT="RequestedAuthnContext";

    /** 
     * Constant for Comparison Attribute
     */
    public String COMPARISON ="Comparison";

    /**
     * Constant for Scoping Element.
     */
    public String SCOPING="Scoping";
    
    /**
     * Constant for Extensions Element.
     */
    public String EXTENSIONS="Extensions";
    
    /**
     * Constant for StatusDetail Element.
     */
    public String STATUS_DETAIL="StatusDetail";
    
    /**
     * Constant for StatusCode Element.
     */
    public String STATUS_CODE="StatusCode";
    
    /**
     * Constant for Status Element.
     */
    public String STATUS="Status";
    
    /**
     * Constant for StatusMessage Element.
     */
    public String STATUS_MESSAGE="StatusMessage";
    
    /**
     * Constant for GetComplete Element.
     */
    public String GETCOMPLETE="GetComplete";
    
    /**
     * Constant for IDPEntry Element.
     */
    public String IDPENTRY="IDPEntry";
    
    /**
     * Constant for IDPList Element.
     */
    public String IDPLIST="IDPList";
    
    /**
     * Constant for NameIDPolicy Element.
     */
    public String NAMEIDPOLICY="NameIDPolicy";
    
    /**
     * Constant for RequesterID Element.
     */
    public String REQUESTERID="RequesterID";

    // for SAMLPOSTProfileServlet
    public String SOURCE_SITE_SOAP_ENTRY = "sourceSite";
    public String POST_ASSERTION = "assertion";
    public String CLEANUP_INTERVAL_NAME =
                                "iplanet-am-saml-cleanup-interval";

    /**
     * NameID info attribute.
     */ 
    public String NAMEID_INFO = "sun-fm-saml2-nameid-info";

    /**
     * NameID info key attribute.
     */
    public String NAMEID_INFO_KEY = "sun-fm-saml2-nameid-infokey";

    /**
     * SAML2 data store provider name.
     */ 
    public String SAML2 = "saml2";

    /**
     * Auto federation attribute.
     */
    public String AUTO_FED_ATTRIBUTE = 
                        "autofedAttribute";

    /**
     * Auto federation enable attribute.
     */
    public String AUTO_FED_ENABLED =
                        "autofedEnabled";

    /**
     * Transient federation users.
     */
    public String TRANSIENT_FED_USER =
                        "transientUser";

    public String NAMEID_TRANSIENT_FORMAT = 
         NAMEID_FORMAT_NAMESPACE + "transient";

    /**
     * certficate alias attribute.
     */
    public String CERT_ALIAS = "sun-fm-saml2-cert-alias";
 
    /**
     * NameID format map configuration.
     */
    public String NAME_ID_FORMAT_MAP = "nameIDFormatMap";

    /**
     * Attribute map configuration.
     */
    public String ATTRIBUTE_MAP = "attributeMap";

    /**
     * Service provider adapter implementation class
     */
    public String SP_ADAPTER_CLASS = "spAdapter";
    
    /**
     * Environment (attribute/value pair) for Service provider adapter 
     * implementation class. Those variables will be passed down as
     * Map to the implementation class for initialization.
     */
    public String SP_ADAPTER_ENV = "spAdapterEnv";
    
    /**
     * Fedlet adapter implementation class.
     */
    public String FEDLET_ADAPTER_CLASS = "fedletAdapter";
    
    /**
     * Environment (attribute/value pair) for fedlet adapter 
     * implementation class. Those variables will be passed down as
     * Map to the implementation class for initialization.
     */
    public String FEDLET_ADAPTER_ENV = "fedletAdapterEnv";
    
    /**
     * Service provider account mapper.
     */
    public String SP_ACCOUNT_MAPPER = 
                        "spAccountMapper";

    /**
     * Use NameID value as local user ID in service provider account mapper.
     */ 
    public String USE_NAMEID_AS_SP_USERID = "useNameIDAsSPUserID";

    /**
     * Service provider attribute mapper.
     */
    public String SP_ATTRIBUTE_MAPPER = 
                        "spAttributeMapper";

    /**
     * Identity provider account mapper.
     */
    public String IDP_ACCOUNT_MAPPER = 
                        "idpAccountMapper";

    /**
     * Identity provider attribute mapper.
     */
    public String IDP_ATTRIBUTE_MAPPER = 
                        "idpAttributeMapper";

    /**
     * Attribute authority mapper.
     */
    public String ATTRIBUTE_AUTHORITY_MAPPER = 
                        "attributeAuthorityMapper";

    /**
     * Assertion ID request mapper.
     */
    public String ASSERTION_ID_REQUEST_MAPPER = 
                        "assertionIDRequestMapper";

    /**
     * RelayState Parameter
     */
    public String RELAY_STATE="RelayState";

    /**
     * RelayState Alias Parameter
     */
    public String RELAY_STATE_ALIAS="RelayStateAlias";

    /**
     * Realm Parameter
     */
    public String REALM="realm";

    /**
     * AssertionConsumerServiceIndex Parameter
     */
    public String ACS_URL_INDEX="AssertionConsumerServiceIndex";

    /**
     * AttributeConsumingServiceIndex Parameter
     */
    public String ATTR_INDEX="AttributeConsumingServiceIndex";

    /**
     * NameIDPolicy Format Identifier Parameter
     */
    public String NAMEID_POLICY_FORMAT="NameIDFormat";

    /**
     * True Value String
     */
    public String TRUE="true";

    /**
     * False Value String
     */
    public String FALSE="false";

    public String AUTH_LEVEL="AuthLevel";
    public String ORGANIZATION = "Organization";
    public String AUTH_LEVEL_ATTR="sunFMAuthContextComparison";
    public String AUTH_TYPE="authType";
    public String AUTH_LEVEL_ADVICE = "sunamcompositeadvice";

    public String AUTH_TYPE_ATTR ="sunFMAuthContextType";

    public String DECLARE_REF_AUTH_TYPE = "AuthContextDeclareRef";
    public String CLASS_REF_AUTH_TYPE = "AuthContextClassRef";

    public String AUTH_CONTEXT_DECL_REF ="AuthContextDeclRef";
    public String AUTH_CONTEXT_DECL_REF_ATTR 
                                        ="sunFMAuthContextDeclareRef";

    public String AUTH_CONTEXT_CLASS_REF ="AuthnContextClassRef";

    public String AUTH_CONTEXT_CLASS_REF_ATTR 
                                        ="sunFMAuthContextClassRef";

    /**
     * Parameter name for SAML artifact in http request.
     */
    public String SAML_ART = "SAMLart";

    /**
     * Service Provider Role
     */
    public String SP_ROLE = "SPRole";

    /**
     * Identity Provider Role
     */
    public String IDP_ROLE = "IDPRole";
    
    /**
     * Constant value for entity acting as both SP and IDP role.
     */
    public String DUAL_ROLE ="DualRole";


    /**
     * Policy Decision Point Role
     */
    String PDP_ROLE = "PDPRole";

    /**
     * Policy Enforcement Point Role
     */
    String PEP_ROLE = "PEPRole";
    
    /**
     * Attribute Authority Role
     */
    String ATTR_AUTH_ROLE = "AttrAuthRole";

    /**
     * Attribute Query Role
     */
    String ATTR_QUERY_ROLE = "AttrQueryRole";

    /**
     * Authentication Authority Role
     */
    String AUTHN_AUTH_ROLE = "AuthnAuthRole";

    /**
     * Unknown Role
     */
    public String UNKNOWN_ROLE = "UNKNOWN";
    

    /**
     * Attribute to be configured in SPSSOConfig for SAML2 authentication
     * module instance name.
     */
    public String AUTH_MODULE_NAME = "saml2AuthModuleName";

    /**
     * Attribute to be configured in SPSSOConfig for local authentication url.
     */
    public String LOCAL_AUTH_URL = "localAuthURL";

    /**
     * Attribute to be configured in SPSSOConfig for intermediate url.
     */
    public String INTERMEDIATE_URL = "intermediateUrl";

    /**
     * Attribute to be configure in SPSSOConfig for default relay state url.
     */
    public String DEFAULT_RELAY_STATE = "defaultRelayState";

    /**
     * This is an attribute in entity config for the
     * entity description
     */
    public String ENTITY_DESCRIPTION = "description";

    /**
     * This is an attribute in entity config for the
     * signing certificate alias
     */
    public String SIGNING_CERT_ALIAS = "signingCertAlias";

    /**
     * This is an attribute in entity config for the
     * signing certificate encrypted keypass
     */
    public String SIGNING_CERT_KEYPASS = "signingCertKeyPass";

    /**
     * This is an attribute in entity config for the
     * encryption certificate alias
     */
    public String ENCRYPTION_CERT_ALIAS = "encryptionCertAlias";
    
    /**
     * The entity role
     */
    public String ROLE = "role";

    public String SIG_PROVIDER =
    "com.sun.identity.saml2.xmlsig.SignatureProvider";

    public String ENC_PROVIDER =
    "com.sun.identity.saml2.xmlenc.EncryptionProvider";
    
    /**
     * Signing  
     */
    public String SIGNING = "signing";
    
    /**
     * Encryption  
     */
    public String ENCRYPTION = "encryption";
    
    // Delimiter used to separate multiple NameIDKey values.
    public String SECOND_DELIM = ";";

    /**
     * Http request parameter used to indicate whether the intent is
     * federation or not. Its values are "true" and "false".
     */
    public String FEDERATE = "federate";
    
    /** xmlsig signing parameters*/
    public String CANONICALIZATION_METHOD =
         "com.sun.identity.saml.xmlsig.c14nMethod";
    public String TRANSFORM_ALGORITHM =
         "com.sun.identity.saml.xmlsig.transformAlg";
    public String XMLSIG_ALGORITHM =
         "com.sun.identity.saml.xmlsig.xmlSigAlgorithm";
    public String DIGEST_ALGORITHM =
         "com.sun.identity.saml.xmlsig.digestAlgorithm";
    /**
     * Property name for the global default query signature algorithm for RSA keys.
     */
    public String QUERY_SIGNATURE_ALGORITHM_RSA = "org.forgerock.openam.saml2.query.signature.alg.rsa";
    /**
     * Property name for the global default query signature algorithm for DSA keys.
     */
    public String QUERY_SIGNATURE_ALGORITHM_DSA = "org.forgerock.openam.saml2.query.signature.alg.dsa";
    /**
     * Property name for the global default query signature algorithm for EC keys.
     */
    public String QUERY_SIGNATURE_ALGORITHM_EC = "org.forgerock.openam.saml2.query.signature.alg.ec";
    public String DSA = "DSA";
    public String RSA = "RSA";      

    public String SIG_ALG = "SigAlg"; 
    public String SHA1_WITH_DSA = "SHA1withDSA";
    public String SHA1_WITH_RSA = "SHA1withRSA";

    public String DEFAULT_ENCODING = "UTF-8";

    // SOAP fault code for requester error
    public String CLIENT_FAULT = "Client";

    // SOAP fault code for responder error
    public String SERVER_FAULT = "Server";

    public String SESSION = "session";

    // more constants defined for auth module
    public String ASSERTIONS = "assertions";
    public String MAX_SESSION_TIME = "maxSessionTime";
    public String IN_RESPONSE_TO = "inResponseTo";

    public String SP_METAALIAS = "spMetaAlias";
    public String METAALIAS = "metaAlias";
    public String SPENTITYID = "spEntityID";
    public String IDPENTITYID = "idpEntityID";
    public String REQUESTTYPE = "requestType";
    
    // Encryption attributes
    /**
     * SP Entity Config attribute name. Used to specify whether it wants
     * Assertion encrypted or not.
     */
    public String WANT_ASSERTION_ENCRYPTED = "wantAssertionEncrypted";

    public String WANT_ATTRIBUTE_ENCRYPTED 
                                   = "wantAttributeEncrypted";
    public String WANT_NAMEID_ENCRYPTED = "wantNameIDEncrypted";

    // Signing attributes
    /**
     * IDP Entity Config attribute name. Used to specify whether it wants
     * ArtifactResolve signed or not.
     */
    public String WANT_ARTIFACT_RESOLVE_SIGNED = "wantArtifactResolveSigned";

    /**
     * SP Entity Config attribute name. Used to specify whether it wants
     * ArtifactResponse signed or not.
     */
    public String WANT_ARTIFACT_RESPONSE_SIGNED =
                              "wantArtifactResponseSigned";
    public String WANT_LOGOUT_REQUEST_SIGNED  
                                   = "wantLogoutRequestSigned";
    public String WANT_LOGOUT_RESPONSE_SIGNED   
                                   = "wantLogoutResponseSigned";
    public String WANT_MNI_REQUEST_SIGNED = "wantMNIRequestSigned";
    public String WANT_MNI_RESPONSE_SIGNED 
                                   = "wantMNIResponseSigned";
    public String WANT_POST_RESPONSE_SIGNED = "wantPOSTResponseSigned";
    
    /**
     * SP Entity Config attribute name. Used to specify IDPList child element
     * of ECP request.
     */
    public static final String ECP_REQUEST_IDP_LIST =
        "ECPRequestIDPList";

    /**
     * SP Entity Config attribute name. Used to specify an implementation class
     * that finds IDPList child element of ECP request.
     */
    public static final String ECP_REQUEST_IDP_LIST_FINDER_IMPL =
        "ECPRequestIDPListFinderImpl";

    /**
     * SP Entity Config attribute name. Used to specify attribute 'GetComplete'
     * of IDPList child element of ECP request
     */
    public static final String ECP_REQUEST_IDP_LIST_GET_COMPLETE =
        "ECPRequestIDPListGetComplete";

    /**
     * Attribute Authority Config attribute name. Used to specify data store
     * attribute name that contains X509 subject DN.
     */
    public String X509_SUBJECT_DATA_STORE_ATTR_NAME =
        "x509SubjectDataStoreAttrName";

    /**
     * Constant for SAML2IDPSessionIndex SSO token property
     */
    public String IDP_SESSION_INDEX = "SAML2IDPSessionIndex";
    /**
     * Constant for IDPMetaAlias SSO token property
     */
    public String IDP_META_ALIAS="IDPMetaAlias";

    // Basic auth for SOAP binding
    public String BASIC_AUTH_ON = "basicAuthOn";
    public String BASIC_AUTH_USER = "basicAuthUser";
    public String BASIC_AUTH_PASSWD = "basicAuthPassword";

    /**
     * Service provider AuthnContext mapper.
     */
    public String SP_AUTHCONTEXT_MAPPER =
                        "spAuthncontextMapper";

    /**
     * Default value for Service provider AuthnContext mapper value.
     */
    public String DEFAULT_SP_AUTHCONTEXT_MAPPER =
        "com.sun.identity.saml2.plugins.DefaultSPAuthnContextMapper";

    /**
     * Service provider AuthnContext Class Reference and AuthLevel Mapping.
     */
    public String SP_AUTH_CONTEXT_CLASS_REF_ATTR=
                        "spAuthncontextClassrefMapping";

    /**
     * Constant for AuthnContext Class Reference namespace
     */
    public String AUTH_CTX_PREFIX =
    "urn:oasis:names:tc:SAML:2.0:ac:classes:";

    /**
     * Service provider AuthnContext Comparison Type attribute name.
     */
    public String SP_AUTHCONTEXT_COMPARISON_TYPE =
                        "spAuthncontextComparisonType";

    /**
     * Default Service provider AuthnContext Comparison Type 
     * attribute value.
     */
    public String SP_AUTHCONTEXT_COMPARISON_TYPE_VALUE = "exact";

    /**
     * Flag to indicate if the RequestedAuthnContext should be included in an AuthnRequest.
     */
    public String INCLUDE_REQUESTED_AUTHN_CONTEXT = "includeRequestedAuthnContext";

    /**
     * Service provider AuthnContext Comparison Parameter Name
     */
    public String SP_AUTHCONTEXT_COMPARISON = "AuthComparison";

    // Time Skew for Assertion NotOnOrAfter. In seconds.
    public String ASSERTION_TIME_SKEW = "assertionTimeSkew";
    public int ASSERTION_TIME_SKEW_DEFAULT = 300;

    // key for SAML2 SDK class mapping
    public String SDK_CLASS_MAPPING = 
        "com.sun.identity.saml2.sdk.mapping.";

    // Default assertion effective time in seconds
    public int ASSERTION_EFFECTIVE_TIME = 600;

    // Default assertion NotBefore skew in seconds
    public int NOTBEFORE_ASSERTION_SKEW_DEFAULT = 600;

    // Assertion effective time attribute name
    public String ASSERTION_EFFECTIVE_TIME_ATTRIBUTE = 
                            "assertionEffectiveTime";

    // NotBefore Assertion skew attribute name
    public String ASSERTION_NOTBEFORE_SKEW_ATTRIBUTE =
                             "assertionNotBeforeTimeSkew";

    // IDP authn context mapper class attribute name
    public String IDP_AUTHNCONTEXT_MAPPER_CLASS =
                            "idpAuthncontextMapper";

    // IDP ECP Session mapper class attribute name
    public static final String IDP_ECP_SESSION_MAPPER_CLASS =
                            "idpECPSessionMapper";

    // Default IDP authn context mapper class name
    public String DEFAULT_IDP_AUTHNCONTEXT_MAPPER_CLASS =
        "com.sun.identity.saml2.plugins.DefaultIDPAuthnContextMapper";

    // Default IDP account mapper class name
    public String DEFAULT_IDP_ACCOUNT_MAPPER_CLASS =
        "com.sun.identity.saml2.plugins.DefaultIDPAccountMapper";

    // Default SP account mapper class name
    public String DEFAULT_SP_ACCOUNT_MAPPER_CLASS =
        "com.sun.identity.saml2.plugins.DefaultSPAccountMapper";

    /**
     * Default SP attribute mapper class name
     */
    public String DEFAULT_SP_ATTRIBUTE_MAPPER_CLASS = "com.sun.identity.saml2.plugins.DefaultSPAttributeMapper";

    // Default IDP attribute mapper class name
    public String DEFAULT_IDP_ATTRIBUTE_MAPPER_CLASS =
        "com.sun.identity.saml2.plugins.DefaultIDPAttributeMapper";

    // Default Attribute Authority mapper class name
    public static final String DEFAULT_ATTRIBUTE_AUTHORITY_MAPPER_CLASS =
        "com.sun.identity.saml2.plugins.DefaultAttributeAuthorityMapper";

    // Default Assertion ID request mapper class name
    public static final String DEFAULT_ASSERTION_ID_REQUEST_MAPPER_CLASS =
        "com.sun.identity.saml2.plugins.DefaultAssertionIDRequestMapper";

    // Default IDP ECP Session mapper class name
    public static final String DEFAULT_IDP_ECP_SESSION_MAPPER_CLASS =
        "com.sun.identity.saml2.plugins.DefaultIDPECPSessionMapper";

    // IDP authn context class reference mapping attribute name
    public String IDP_AUTHNCONTEXT_CLASSREF_MAPPING =
                            "idpAuthncontextClassrefMapping";

    // AuthnContext Class Reference names
    public String CLASSREF_PASSWORD_PROTECTED_TRANSPORT =
        "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";

    // Represents an Authentication Level of 0
    public Integer AUTH_LEVEL_ZERO = Integer.valueOf(0);

    /**
     * Default Service provider AuthnContext Class Reference and
     * AuthLevel Mapping value.
     */
    public String SP_AUTHCONTEXT_CLASSREF_VALUE =
            CLASSREF_PASSWORD_PROTECTED_TRANSPORT + "|" + AUTH_LEVEL_ZERO + "|default";

    // COT List
    public String COT_LIST = COTConstants.COT_LIST;

    // http parameter to default.jsp
    public String MESSAGE = "message";

    // Cache Cleanup interval attribute name in AMConfig.properties.
    // value in seconds
    public String CACHE_CLEANUP_INTERVAL = 
                "com.sun.identity.saml2.cacheCleanUpInterval";

    // default Cache cleanup interval in seconds.
    public int CACHE_CLEANUP_INTERVAL_DEFAULT = 600;

    // minimum Cache cleanup interval in seconds (5 mins).
    public int CACHE_CLEANUP_INTERVAL_MINIMUM = 300;

    // IDP SLO parameter name for logout all sessions
    public String LOGOUT_ALL = "logoutAll";

    // IDP response info ID
    public String RES_INFO_ID = "resInfoID";
     
    // Default query parameter to use for RelayState if
    // RelayState is no specified and if RelayState cannot
    // be obtained from query parameters list specified in 
    // RelayStateAlias 

    public String GOTO = "goto";
    
    // Delimiter for values of multi-valued property set in SSO token
    public char DELIMITER = '|';

    // Escape string for the <code>DELIMITER</code> contained in the values
    // of multi-valued property set in SSO token
    public String ESCAPE_DELIMITER = "&#124;";

    /**
     * Namespace declaration for XML Encryption
     */
    public String NS_XMLENC = "http://www.w3.org/2001/04/xmlenc#";

    /**
     * Namespace declaration for XML Digital Signature
     */
    public String NS_XMLSIG = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * Want XACML Authorization Decision Query Signed.
     */
    String WANT_XACML_AUTHZ_DECISION_QUERY_SIGNED =
        "wantXACMLAuthzDecisionQuerySigned";

    /**
     * Want Authorization Decision Response Signed.
     */
    String WANT_XACML_AUTHZ_DECISION_RESPONSED_SIGNED =
        "wantXACMLAuthzDecisionResponseSigned";

    /**
     * Generate Discovery Bootstrapping
     */
    public String DISCO_BOOTSTRAPPING_ENABLED =
        "discoveryBootstrappingEnabled";

    /**
     * Constant for Response Artifact message encoding property
     */
    public String RESPONSE_ARTIFACT_MESSAGE_ENCODING =
        "responseArtifactMessageEncoding";

    /**
     * URI encoding
     */
    public String URI_ENCODING = "URI";

    /**
     * FORM encoding 
     */
    public String FORM_ENCODING = "FORM";

    /**
     * Cache Assertion
     */
    public String ASSERTION_CACHE_ENABLED =
        "assertionCacheEnabled";

    /**
     * Attribute name format for ID-WSF 1.1 Discovery bootstrap
     */    
    public String DISCOVERY_BOOTSTRAP_ATTRIBUTE_NAME_FORMAT =
        "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";

    /**
     * Attribute name for ID-WSF 1.1 Discovery bootstrap
     */    
    public String DISCOVERY_BOOTSTRAP_ATTRIBUTE_NAME =
        "urn:liberty:disco:2003-08:DiscoveryResourceOffering";

    /**
     * Constant for Discovery bootstrap credentials SSO token
     * property
     */
    public String DISCOVERY_BOOTSTRAP_CREDENTIALS =
        "DiscoveryBootstrapCrendentials";
    
    /**
     * XML Schema Instance namespace URI
     */
    public String NS_XSI = 
        "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * String used to declare XML Schema Instance namespace.
     */
    public String XSI_DECLARE_STR =
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";

    /**
     * List of SAE appliation name to encrypted secret mapping.
     */
    public String SAE_APP_SECRET_LIST = "saeAppSecretList";

    /**
     * List of valid Relay State Urls
     */
    public String RELAY_STATE_URL_LIST = "relayStateUrlList";

    /**
     * IDP SAE endpoint url.
     */
    public String SAE_IDP_URL = "saeIDPUrl";

    /**
     * SP SAE endpoint url.
     */
    public String SAE_SP_URL = "saeSPUrl";

    /**
     * SP SAE logout url.
     */
    public String SAE_SP_LOGOUT_URL = "saeSPLogoutUrl";

    /**
     * SAE : Extended meta param : SPApp url
     */
    public String SAE_XMETA_URL = "url";

    /**
     * SAE : Extended meta param : shared secret for symmetric crypto
     */
    public String SAE_XMETA_SECRET = "secret";

    /**
     * SAE : Derived from SAML2 meta
     */
    public String SAE_XMETA_PKEY_ALIAS = "privatekeyalias";

    /**
     * HTTP parameters that will be passed to SAE auth modules.
     */
    public String SAE_REALM = "realm";
    public String SAE_IDP_ENTITYID = "idpEntityID";
    public String SAE_IDPAPP_URL = "idpAppUrl";
            
    /**
     * Enable IDP Proxy
     */
    public String ENABLE_IDP_PROXY = "enableIDPProxy";

    /**
     * Always proxy the Authn Request
     */
    public String ALWAYS_IDP_PROXY = "alwaysIdpProxy";

    /**
     *IDP Proxy Name List
     */
    public String IDP_PROXY_LIST = "idpProxyList";
 
    /**
     * IDP Proxy Count
     */
    public String IDP_PROXY_COUNT = "idpProxyCount";
 
    /**
     * Use Introduction for IDP Proxy
     */
    public String USE_INTRODUCTION_FOR_IDP_PROXY =
        "useIntroductionForIDPProxy";      

    /**
     * Idp finder URL
     */
    public String IDP_FINDER_URL ="/idpfinder";
    
    /**
     * IDP Proxy finder name
     */
    public String IDP_PROXY_FINDER_NAME = 
         "com.sun.identity.saml2.idpproxy"; 
         
    /**
     * Default class name of IDP Proxy finder   
     */     
    public String  DEFAULT_IDP_PROXY_FINDER = 
        "com.sun.identity.saml2.plugins.SAML2IDPProxyImpl";

    /**
     * IDP Proxy finder attribute name in the IDP Extended metadata
     */
    public String IDP_PROXY_FINDER_ATTR_NAME = "idpProxyFinder";

    /**
     * IDP Proxy finder implmentation classe attribute name
     * in the IDP Extended metadata
     */
    public static final String PROXY_IDP_FINDER_CLASS = "proxyIDPFinderClass";
    
    /**
     * Flag to indicate if the IdP must enable the IdP Finder
     * This is the name of the attribute flag in the IDP Extended metadata
     */
    public static final String ENABLE_PROXY_IDP_FINDER_FOR_ALL_SPS =
            "enableProxyIDPFinderForAllSPs";
    
    /**
     * Attribute Name in the extended metadata that takes the value of
     * the JSP that will present the list of IdPs to the user
     */
    public static final String PROXY_IDP_FINDER_JSP =
            "proxyIDPFinderJSP";

    /**
     * Default IDP Proxy Finder JSP
     */
    public static final String DEFAULT_PROXY_IDP_FINDER = "proxyidpfinder.jsp";

    /**
     * IDP Adapter class attribute name
     */
    public static final String IDP_ADAPTER_CLASS = "idpAdapter";

    /**
     * Default IDP Adapter class
     */
    public static final String DEFAULT_IDP_ADAPTER = "com.sun.identity.saml2.plugins.DefaultIDPAdapter";

    /**
     * Key used to save IDP Session in a map
     */    
    public String IDP_SESSION = "IDPSESSION"; 
    
    /**
     * Key used to save session partners in a map 
     */
    public String  PARTNERS = "PARTNERS";        

    /**
     * String used to declare ECP namespace prefix.
     */
    public static final String ECP_PREFIX = "ecp:";
    
    /**
     * ECP namespace URI.
     */
    public static final String ECP_NAMESPACE =
        "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp";
    
    /**
     * String used to declare ECP namespace.
     */
    public static final String ECP_DECLARE_STR =
        "xmlns:ecp=\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\"";

    /**
     * Constant for ECP end tag
     */
    public static final String ECP_END_TAG="</ecp:";

    /**
     * ECP service name in PAOS header
     */
    public static final String PAOS_ECP_SERVICE =
        "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp";

    /**
     * String used to declare SOAP envelope namespace prefix.
     */
    public static final String SOAP_ENV_PREFIX = "soap-env:";
    
    /**
     * SOAP envelope namespace URI.
     */
    public static final String SOAP_ENV_NAMESPACE =
        "http://schemas.xmlsoap.org/soap/envelope/";
    
    /**
     * String used to declare SOAP envelope namespace.
     */
    public static final String SOAP_ENV_DECLARE_STR =
        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\"";

    /**
     * SOAP actor.
     */
    public static final String SOAP_ACTOR_NEXT =
        "http://schemas.xmlsoap.org/soap/actor/next";
    
    /**
     * Check Certificate status
     */
    public static final String CHECK_SAML2_CERTIFICATE_STATUS =
        "com.sun.identity.saml2.crl.check";

    /**
     * Check CA Certificate status
     */
    public static final String CHECK_SAML2_CA_STATUS =
        "com.sun.identity.saml2.crl.check.ca";

    /**
     * Wild card to indicate mapping any attribute name as it is in
     * the Assertion
     */
    public String ATTR_WILD_CARD = "*";

    /**
     * Key name for Response object
     */
    public String RESPONSE = "Response"; 

    /**
     * Key name for Assertion object
     */
    public String ASSERTION = "Assertion"; 
    
    /**
     * One Time Use.
     */
    public String ONETIME="ONE"; 
    /**
     * Is Bearer assertion
     */
    public String IS_BEARER="isBearer";  

    /**
     * String to represent the logout url for external application.
     * SAML2 component will send request to the external logout URL 
     * using back channel HTTP POST mechanism.
     * This is used when the single logout is initiated from remote party
     * (SP or IDP).
     */
    public String APP_LOGOUT_URL = "appLogoutUrl";

    /**
     * URL parameter name in external application logout URL for requesting 
     * user session property. Value is a session property name whose
     * value will be posted to application as http header and content for its 
     * logout use.
     */
    public String APP_SESSION_PROPERTY = "appsessionproperty";
  
    /**
     * IDP Session Synchronize Enabled
     */
    public String IDP_SESSION_SYNC_ENABLED =
            "idpSessionSyncEnabled";

    /**
     * SP Session Synchronize Enabled
     */
    public String SP_SESSION_SYNC_ENABLED =
            "spSessionSyncEnabled";

    /**
     * Map key used in fedlet case to specify federation info key.
     */
    public String INFO_KEY = "infoKey";

    /**
     * Single Sign-On service.
     */
    public String SSO_SERVICE = "sso";

    /**
     * NameIDMapping service.
     */
    public String NAMEID_MAPPING_SERVICE = "nip";

    /**
     * AssertionIDRequest service.
     */
    public String ASSERTION_ID_REQUEST_SERVICE = "air";

    /**
     * ArtifactResolution service.
     */
    public String ARTIFACT_RESOLUTION_SERVICE = "ars";

    /**
     * SingleLogout service.
     */
    public String SLO_SERVICE = "slo";

    /**
     * ManageNameID service.
     */
    public String MNI_SERVICE = "mni";

    /**
     * AssertionConsumer service.
     */
    public String ACS_SERVICE = "acs";

    /**
     * Map key used in SLO request redirect code
     */
    public static final String AM_REDIRECT_URL = "AM_REDIRECT_URL";

    /**
     * Map key used in SLO request redirect code
     */
    public static final String OUTPUT_DATA = "OUTPUT_DATA";

    public static final String RESPONSE_CODE = "RESPONSE_CODE";

    /**
     * Flag to Indicate that we do not want to write the Federation info in the local User Data Store. This flag is
     * set in the local/remote SP extended metadata configuration.
     */
    public static final String SP_DO_NOT_WRITE_FEDERATION_INFO = "spDoNotWriteFederationInfo";

    /**
     * Flag to indicate that we do not want to write the federation info in the IdP's local User Data Store. This flag
     * is set in the local IdP extended metadata configuration.
     */
    String IDP_DISABLE_NAMEID_PERSISTENCE = "idpDisableNameIDPersistence";

    /**
     * Property to determine whether SAML SP Decryption Debug mode has been enabled.
     */
    String SAML_DECRYPTION_DEBUG_MODE = "openam.saml.decryption.debug.mode";

    /**
     * Property name used to store the remote IdP's SAML response as an attribute of the HttpServletRequest.
     */
    String SAML_PROXY_IDP_RESPONSE_KEY = "openam.saml.idpproxy.idp.response";

    /**
     * property name used to store whether or not saml single logout in enabled.
     */
    String SINGLE_LOGOUT = "openam.saml.singlelogout.enabled";

    /**
     * Default Value for the SAML2 Server Port
     */
    int DEFAULT_SERVER_PORT = 18080;
}
