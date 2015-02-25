/**
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
 * $Id: IFSConstants.java,v 1.12 2008/08/29 04:57:15 exu Exp $
 * Portions Copyrights 2014 ForgeRock AS
 */


package com.sun.identity.federation.common;

import com.sun.identity.cot.COTConstants;
/**
 * This interface represents a collection of common constants used by
 * the classes in Federation Service.  
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public interface IFSConstants {
    /**
     * HTTPS URL prefix 
     */
    public static final String HTTPS_URL_PREFIX  = "https://";
    /**
     * Dot character
     */
    public static final char   DOT               = '.';
    /**
     * Federation error 
     */
    public static final String FEDERROR         ="federror";
    /**
     * Federation remark 
     */
    public static final String FEDREMARK        ="fedremark";
    /**
     * Provider or entity status
     */
    public static final String ACTIVE            = "active";
    /**
     * Provider or entity status
     */
    public static final String INACTIVE          = "inactive";
    /**
     * IDFF 1.1 meta version
     */
    public static final String ENUM_ZERO         = "urn:liberty:iff:2002-12";
    /**
     * IDFF 1.2 meta version
     */
    public static final String ENUM_ONE          = "urn:liberty:iff:2003-08";
    /**
     * Affiliation descriptor
     */
    public static final String AFFILIATE_TYPE  = "urn:liberty:iff:affiliate";
    /**
     * Provider descriptor
     */
    public static final String CONTAINER_TYPE    = "urn:liberty:iff:provider";  
    /**
     * SOAP binding 
     */
    public static final String SOAP              = "SOAP";
    /**
     * HTTP redirect
     */
    public static final String HTTP_REDIRECT     = "HTTP Redirect";
    /**
     * HTTP redirect 
     */
    public static final String HTTP_GET          = "HTTP Get";
    /**
     * PASSIVE mode
     */
    public static final String PASSIVE           = "passive";
    /**
     * Local login page
     */
    public static final String LOCAL_LOGIN       = "locallogin";
    /**
     * Common login page
     */
    public static final String COMMON_LOGIN       = "commonlogin";
    /**
     * Provider acts SP and IDP role
     */
    public static final String SP_IDP            = "SP/IDP";
    /**
     * Supported protocol type
     */
    public static final String ALLOWED_PROTOCOL_KEY =
        "com.sun.identity.federation.allowedProtocol";
    /**
     * Specify keystore location in config file 
     */    
    public static final String KEY_STORE =
        "com.sun.identity.saml.xmlsig.keystore";
    /**
     * Specify keystore password file in config file 
     */        
    public static final String STORE_PASS =
        "com.sun.identity.saml.xmlsig.storepass";
    /**
     * Specify private key password in config file 
     */        
    public static final String KEY_PASS =
        "com.sun.identity.saml.xmlsig.keypass";

    /**
     * Specify login URL IDP will redirect to when there is no valid session 
     */        
    public static final String IDP_LOGIN_URL =
        "com.sun.identity.federation.services.idpLoginURL";

    /**
     * Internal error
     */     
    public static final String INTERNAL_ERROR = "internalError";
   
    public static final int LOCAL_NAME_IDENTIFIER = 0;
    public static final int REMOTE_NAME_IDENTIFIER = 1;
 
    // **********************************
    // these are keys whose values store in "libIDFF.properties" file.
    // **********************************
    /**
     * Account management: hosted descriptor config is null. 
     */
    public static final String NULL_HOSTED_CONFIG = 
        "actmgmt-hosted-config-is-null";
    /**
     * Account management: hosted entity id is null. 
     */
    public static final String NULL_PROVIDER_ID = 
        "actmgmt-provider-id-is-null";
    /**
     * Account management: meta alias is null. 
     */
    public static final String NULL_META_ALIAS = 
        "actmgmt-meta-alias-is-null";
    /**
     * Account management: User DN is null. 
     */    
    public static final String NULL_USER_DN = 
        "actmgmt-user-dn-is-null";
    /**
     * Account management: Opaque handle is null. 
     */    
    public static final String NULL_NAME = 
        "actmgmt-name-is-null";
    /**
     * Account management: Namespace is null. 
     */    
    public static final String NULL_NAME_SPACE = 
        "actmgmt-name-space-is-null";
    /**
     * Account management: Name identifier is null. 
     */     
    public static final String NULL_NAME_IDENTIFIER = 
        "actmgmt-name-identifier-is-null";
    /**
     * Account management: FedInfo is null. 
     */    
    public static final String NULL_FED_INFO_OBJECT =
        "actmgmt-account-fed-info-object-is-null";
    /**
     * Account management: FedInfo key is null. 
     */    
    public static final String NULL_FED_INFO_KEY_OBJECT = 
        "actmgmt-account-fed-info-key-object-is-null";
    /**
     * Account management: Account federation info is 
     * modified in the directory. 
     */    
    public static final String INVALID_ACT_FED_INFO_IN_IDS = 
        "actmgmt-fed-info-modified-in-ids";
    /**
     * Account management: Invalid name identifier type.
     */      
    public static final String INVALID_NAME_IDENTIFIER_TYPE = 
        "actmgmt-nameIdentifierType-invalid";
    /**
     * Account management: Account federation info not found.
     */       
    public static final String ACT_FED_INFO_NOT_FOUND =
        "actmgmt-fed-info-not-found";
    /**
     * Account management: Multiple UIDs matched.
     */   
    public static final String MULTIPLE_UID_MATCHED = 
        "actmgmt-multiple-uid-matched";
    /**
     * Account management: UID does not match.
     */ 
    public static final String UID_NOT_MATCHED =
        "actmgmt-uid-not-matched";
    /**
     * Account management: User does not match.
     */     
    public static final String USER_NOT_FOUND =
        "actmgmt-user-not-found";
    /**
     * Maximum number of Assertion
     */
    public static final int ASSERTION_MAX_NUMBER_DEFAULT = 10;
    /**
     * Cleanup time interval
     */
    public static final int CLEANUP_INTERVAL_DEFAULT = 180;
    /**
     * User is used in basic authentication.
     */
    public static final String USER = "iplanet-am-saml-user";
    /**
     * Password for the user (basic authentication).
     */
    public static final String PASSWORD="iplanet-am-saml-password";
    /**
     * Basic authentication
     */ 
    public static final String BASICAUTH= "BASICAUTH";
    /**
     * SSL with basic authentication
     */  
    public static final String SSLWITHBASICAUTH = "SSLWITHBASICAUTH";
    /**
     * No authentication enforced 
     */
    public static final String NOAUTH = "NOAUTH";
    /**
     * SSL
     */
    public static final String SSL = "SSL";
    /**
     * Status code for saml protocol success
     */
    public static final String STATUS_CODE_SUCCESS = "samlp:Success";
    /**
     * Status code for saml protocol success without prefix 
     */
    public static final String STATUS_CODE_SUCCESS_NO_PREFIX = ":Success";
    /**
     * http protocol 
     */
    public static final String HTTP="http";
    /**
     * https protocol 
     */
    public static final String HTTPS="https";
    /**
     * Post AuthnResponse parameter 
     */   
    public static final String POST_AUTHN_RESPONSE_PARAM = "LARES";
    /**
     * Post AuthnRequest parameter 
     */   
    public static final String POST_AUTHN_REQUEST_PARAM = "LAREQ";
    /**
     * Authentication indicator parameter
     */   
    public static final String AUTHN_INDICATOR_PARAM = "Authn";
    /**
     * Default value for Authentication indicator parameter
     */ 
    public static final String AUTHN_INDICATOR_VALUE = "true";  
    /**
     * Attribute name in the session
     */
    public static final String SESSION_COTSET_ATTR = "CotSet";
    /**
     * Maximum length of URL
     */
    public static final int URL_MAX_LENGTH = 1256;
    /**
     * Error in metadata setting.
     */     
    public static final String METADATA_ERROR =
        "metadata-error-check";
    /**
     * Contact system administrator to report this problem.
     */
    public static final String CONTACT_ADMIN = "contact-admin";
    /**
     * Federation access log file name 
     */
    public static final String FS_ACCESS_LOG_NAME = "Federation.access";
    /**
     * Federation error log file name 
     */
    public static final String FS_ERROR_LOG_NAME = "Federation.error";
    /**
     * Status code: <code>samlp:Success</code>  
     */
    public static final String SAML_SUCCESS = "samlp:Success";
    /**
     * Status code: <code>samlp:Responder</code>  
     */
    public static final String SAML_RESPONDER = "samlp:Responder";
    /**
     * Status code: <code>samlp:Requester</code>  
     */
    public static final String SAML_REQUESTER = "samlp:Requester";
    /**
     * Status code: <code>samlp:VersionMismatch</code>  
     */
    public static final String SAML_VERSION_MISMATCH = "samlp:VersionMismatch";

    /**
     * Liberty unsupported: <code>lib:Unsupported</code>
     */
    public static final String SAML_UNSUPPORTED = "lib:Unsupported";
    /**
     * Liberty failure: <code>lib:Failure</code>
     */
    public static final String REGISTRATION_FAILURE_STATUS = "lib:Failure";
    /**
     * Liberty federation does not exist:
     * <code>lib:FederationDoesNotExist</code>
     */
    public static final String FEDERATION_NOT_EXISTS_STATUS = 
        "lib:FederationDoesNotExist";
    /**
     * Register NameIdentifier Request
     */
    public static final String NAME_REGISTRATION_REQUEST = 
        "RegisterNameIdentifierRequest";
    /**
     * Register NameIdentifier Response
     */    
    public static final String NAME_REGISTRATION_RESPONSE = 
        "RegisterNameIdentifierResponse";
    /**
     * <code>lib</code> tag 
     */    
    public static final String LIB = "lib";
    /**
     * <code>yes</code>
     */
    public static final String YES = "yes";
    /**
     * <code>no</code>
     */
    public static final String NO = "no";
    /**
     * SP role
     */
    public static final String SP = "SP";
    /**
     * IDP role 
     */
    public static final String IDP = "IDP";
    /**
     * SP and IDP role 
     */
    public static final String BOTH ="IDP/SP";
    /**
     * Preferred cookie seperator
     */
    public static final String PREFERRED_COOKIE_SEPERATOR = " ";
    /**
     * Session cookie 
     */
    public static final String SESSION_COOKIE = "SESSION";
    /**
     * Persistent cookie
     */
    public static final String PERSISTENT_COOKIE = "PERSISTENT";
    /**
     * Persistent cookie expiration, by default 365 days
     */
    public static final int PERSISTENT_COOKIE_AGE = 31536000; 
    /**
     * Session cookie expiration
     */
    public static final int SESSION_COOKIE_AGE = -1;
    /**
     * Repeat termination flag
     */
    public static final String TERMINATION_REPEAT_FLAG = "repeatTerm";
    /**
     * Repeat logout flag
     */
    public static final String LOGOUT_REPEAT_FLAG = "repeatLogout";
    /**
     * Partner session
     */
    public static final String PARTNER_SESSION = "PartnerSession";
    /**
     * Session index 
     */
    public static final String SESSION_INDEX = "SessionIndex";
    /**
     * Provider 
     */
    public static final String PROVIDER = "Provider";
    /**
     * Missing value for security key.
     */
    public static final String MISSING_KEYINFO = "missingKeyInfo";
    /**
     * Provider ID not in request and not able to reset preferred IDP
     */
    public static final String INTRODUCTION_NO_PROVIDER_ID = 
        "introduction-no-provider-id";
    /**
     * Redirect URL not specified.
     */ 
    public static final String INTRODUCTION_NO_REDIRECT_URL = 
        "introduction-no-redirect-url";
    /**
     * Invalid Redirect URL scheme.
     */
    public static final String INTRODUCTION_INVALID_REDIRECT_URL_SCHEME = 
        "invalid-redirect-url-scheme";
    /**
     * Preferred IDP Cookie not found.
     */
    public static final String INTRODUCTION_COOKIE_NOT_FOUND = 
        "preferred-idp-cookie-not-found";
    /**
     *Invalid Property file. No providerid code.
     */
    public static final String INTRODUCTION_PROPERTY_FILE_PROVIDER_INVALID = 
        "introduction-invalid-providerid-property-file";
    /**
     * Invalid Property File
     */    
    public static final String FEDERATION_PROPERTY_FILE_INVALID = 
        "federation-invalid-property-file";
    /**
     * Failed to get Meta Manager Instance.
     */
    public static final String FEDERATION_FAILED_META_INSTANCE = 
        "federation-failed-meta-instance";
    /**
     * Failed to get Account Manager Instance.
     */
    public static final String FEDERATION_FAILED_ACCOUNT_INSTANCE =
        "federation-failed-account-instance";
    /**
     * Redirection failed. Cannot process further
     */
    public static final String FEDERATION_REDIRECT_FAILED = 
        "federation-redirection-failed";
    /**
     * Failed to get Pre/Logout handler.
     */
    public static final String LOGOUT_INVALID_HANDLER =
        "logout-invalid-handler";
    /**
     * Logout failed. Manager instance not available.
     */
    public static final String LOGOUT_FAILED_MANAGER = 
        "logout-failed-manager";
    /**
     * Logout failed to redirect due to incorrect URL.
     */
    public static final String LOGOUT_REDIRECT_FAILED =
        "logout-redirect-failed";
    /**
     * Logout failed due to invalid session.
     * Redirect to authentication.
     */
    public static final String LOGOUT_TOKEN_INVALID = 
        "logout-token-invalid";
    /**
     * Single logout failed.
     */     
    public static final String LOGOUT_FAILED = "logout-failed";
    /**
     * Logout request not formed properly. Cannot proceed.
     */
    public static final String LOGOUT_REQUEST_IMPROPER =
        "logout-request-improper";
    /**
     * Creation of logout request failed.
     */
    public static final String LOGOUT_REQUEST_CREATION = 
        "logout-request-creation-failed";
    /**
     * Failed to process Logout request.
     */    
    public static final String LOGOUT_REQUEST_PROCESSING_FAILED = 
        "logout-request-processing-failed";
    /**
     * Logout request does not have provider specified properly.
     * Cannot process request.
     */
    public static final String LOGOUT_REQUEST_NO_PROVIDER = 
        "logout-request-no-provider";
    /**
     * Unable to find cert alias. Cannot sign or verify request.
     */    
    public static final String NO_CERT_ALIAS = "cannot-find-cert-alias";
    /**
     * Unable to find certificate. Cannot sign or verify request.
     */    
    public static final String NO_CERT = "cannot-find-cert";
    /**
     * Request does not contain proper provider ID.
     */
    public static final String REQUEST_NO_PROVIDER = "request-no-provider";
    /**
     * Failed to create SPProvidedNameIdentifier.
     */
    public static final String REGISTRATION_FAILED_SP_NAME_IDENTIFIER = 
        "registration-failed-sp-name-identifier";
    /**
     * Error in processing Name Registration at IDP.
     */
    public static final String REGISTRATION_FAILED_REMOTE = 
        "registration-failed-remote";
    /**
     * Error in sending Name Registration at IDP.
     */
    public static final String REGISTRATION_FAILED_SEND_REMOTE = 
        "registration-failed-send-remote";
    /**
     * Error in processing Name Registration at SP.
     */
    public static final String REGISTRATION_FAILED_LOCAL = 
        "registration-failed-local";
    /**
     * Successful processing of Name Registration at IDP.
     */
    public static final String REGISTRATION_SUCCESS_REMOTE =
        "registration-success-remote";
    /**
     * Successful processing of Name Registration at SP. 
     */
    public static final String REGISTRATION_SUCCESS_LOCAL =
        "registration-success-local";
    /**
     * Failed to create Name Registration request.
     */
    public static final String REGISTRATION_REQUEST_CREATION_FAILED = 
        "registration-request-creation-failed";
    /**
     * LECP header name 
     */
    public static final String LECP_HEADER_NAME = "Liberty-Enabled";
    /**
     * WML header value 
     */
    public static final String WML_HEADER_VALUE = "text/vnd.wap.wml";
    /**
     * WML profile 
     */
    public static final String WML_PROFILE = 
        "http://projectliberty.org/profiles/wml-post";
    /**
     * Exception in Federation Termination. Unknown Error.
     */    
    public static final String TERMINATION_EXCEPTION =
        "termination-exception-unknown";
    /**
     * Account federation for user with provider does not exist.
     */    
    public static final String TERMINATION_INVALID_FEDERATION = 
        "termination-invalid-federation";
    /**
     * Termination cannot proceed. Provider not found.
     */    
    public static final String TERMINATION_NO_PROVIDER = 
        "termination-no-provider";
    /**
     * Invalid Termination Handler
     */    
    public static final String TERMINATION_INVALID_HANDLER =
        "termination-handle-invalid";
    /**
     * Federation Termination failed during processTerminationRequest.
     */    
    public static final String TERMINATION_REQUEST_PROCESSING_FAILED =  
        "termination-request-processing-failed";
    /**
     * Request not proper. Cannot proceed federation termination.
     */
    public static final String TERMINATION_REQUEST_IMPROPER =
        "termination-request-improper";
    /**
     * Request creation failed. Cannot proceed with federation ]
     * termination.
     */
    public static final String TERMINATION_REQUEST_CREATION = 
        "termination-request-creation-failed";
    /**
     * Federation Termination failed at remote provider.
     */    
    public static final String TERMINATION_REMOTE_FAILED =
        "termination-failed-remotely";
    /**
     * Federation Termination failed locally. Cannot update account.
     */    
    public static final String TERMINATION_LOCAL_FAILED =
        "termination-failed-locally";
    /**
     * Federation Termination succeeded locally. User account updated.
     */    
    public static final String TERMINATION_LOCAL_SUCCEEDED =
        "termination-succeeded-locally";
    /** 
     * Failed to get termination handler.
     */
    public static final String TERMINATION_HANDLE_CREATION_FAILED = 
        "termination-handle-creation-failed";
    /**
     * Failed in sending SOAP request to remote end.
     */    
    public static final String TERMINATION_SOAP_SEND_FAILED =
        "termination-soap-send-failed";
    /**
     * Invalid Federation Termination Service Return URL.
     */    
    public static final String TERMINATION_INVALID_REDIRECT_URL =
        "termination-invalid-redirect-url";
    /**
     * Federation Termination Successful
     */    
    public static final String TERMINATION_SUCCEEDED =
        "federation-termination-successful";
    /**
     * Signature verification of federation termination request failed.
     */    
    public static final String  TERMINATION_INVALID_SIGNATURE =
        "termination-invalid-signature";
    /**
     * Failed to construct SOAP message from request object. 
     */ 
    public static final String TERMINATION_FAILED_SOAP_MESSAGE_CREATION =
        "termination-failed-soap-creation";
    /**
     * Failed to send termination message to remote provider.
     */
    public static final String TERMINATION_FAILED_SEND_REMOTE =
        "termination-failed-send-remote";
    /**
     * Federation termination servlet
     */
    public static final String TERMINATE_SERVLET = 
        "/federation-terminate";
    /**
     * Name registration servlet 
     */    
    public static final String REGISTRATION_SERVLET =
        "/InitiateRegistration"; 
    /**
     * Session cookie name 
     */
//    public static String SESSION_COOKIE_NAME = 
 //       "com.sun.identity.federation.sessioncookie";
    /**
     * Circle of trust id 
     */
  //  public static final String COT_INFO = 
   //     "com.sun.identity.federation.services.cotid";
    /**
     * Authentication login url 
     */
 //   public static final String authURL = 
  //      "com.sun.identity.federation.services.authLoginUrl";
    /**
     * Termination profile 
     */    
   // public static final String TERMINATION_PROFILE =
    //    "com.sun.identity.federation.services.termination.profile";
    /**
     * Termination profile relay state 
     */    
    public static String TERMINATION_RELAY_STATE = "RelayState";
    /**
     * Logout relay state
     */
    public static String LOGOUT_RELAY_STATE = "RelayState";
    /**
     * Name registration indicator
     */
    public static String NAMEREGIS_INDICATOR_PARAM = "nameregis";
    /**
     * Question mark
     */
    public static final char   QUESTION_MARK = '?';
    /**
     * Ampersand 
     */
    public static final char   AMPERSAND = '&';
    /**
     *Equal to
     */
    public static final char   EQUAL_TO = '=';
    /**
     * Role
     */
    public static String ROLE = "ROLE";
    /**
     * Response to 
     */
    public static String RESPONSE_TO = "responseTo";
    /**
     * IDP Single logout via idp 
     */
    public static final String LOGOUT_IDP_SOAP_PROFILE =
        "http://projectliberty.org/profiles/slo-idp-soap";
    /**
     * IDP Single logout via http
     */    
    public static final String LOGOUT_IDP_REDIRECT_PROFILE =
        "http://projectliberty.org/profiles/slo-idp-http";
    /**    
     * IDP Single logout via http get
     */ 
    public static final String LOGOUT_IDP_GET_PROFILE =
        "http://projectliberty.org/profiles/slo-idp-http-get";
    /**
     * SP Single logout via soap 
     */    
    public static final String LOGOUT_SP_SOAP_PROFILE =
        "http://projectliberty.org/profiles/slo-sp-soap";
    /**
     * SP Single logout via http 
     */    
    public static final String LOGOUT_SP_REDIRECT_PROFILE = 
        "http://projectliberty.org/profiles/slo-sp-http";
    /**
     * IDP Termination via http 
     */    
    public static final String TERMINATION_IDP_HTTP_PROFILE =
        "http://projectliberty.org/profiles/fedterm-idp-http";
    /**
     * IDP Termination via soap 
     */    
    public static final String TERMINATION_IDP_SOAP_PROFILE =
        "http://projectliberty.org/profiles/fedterm-idp-soap";
    /**
     * SP Termination via soap 
     */    
    public static final String TERMINATION_SP_SOAP_PROFILE = 
        "http://projectliberty.org/profiles/fedterm-sp-soap";
    /**
     * SP Termination via http 
     */    
    public static final String TERMINATION_SP_HTTP_PROFILE =
        "http://projectliberty.org/profiles/fedterm-sp-http";
    /**
     * Name registration via soap 
     */    
    public static final String NAME_REGISTRATION_PROFILE =
        "http://projectliberty.org/profiles/rni-soap";

    /**
     * URI for HTTP-Redirect-based Name Identifier Registration Profile
     * initiated at Service Provider
     */
    public static final String NAME_REGISTRATION_SP_HTTP_PROFILE =
        "http://projectliberty.org/profiles/rni-sp-http";
    /**
     * URI for SOAP/HTTP-based Name Identifier Registration Profile
     * initiated at Service Provider
     */
    public static final String NAME_REGISTRATION_SP_SOAP_PROFILE =
        "http://projectliberty.org/profiles/rni-sp-soap";
    /**
     * URI for HTTP-Redirect-based Name Identifier Registration Profile
     * initiated at Identity Provider
     */
    public static final String NAME_REGISTRATION_IDP_HTTP_PROFILE =
        "http://projectliberty.org/profiles/rni-idp-http";

    /**
     * URI for SOAP/HTTP-based Name Identifier Registration Profile
     * initiated at Identity Provider
     */
    public static final String NAME_REGISTRATION_IDP_SOAP_PROFILE =
        "http://projectliberty.org/profiles/rni-idp-soap";


    /**
     * FedCookie name 
     */   
    public static final String FEDERATE_COOKIE_NAME =
        "com.sun.identity.federation.fedCookieName";
    /**
     * Quote
     */ 
    public static final String QUOTE                    = "\"";
    /** 
     * New line
     */
    public static final String NL                       = "\n";
    /**
     * Left angle
     */
    public static final String LEFT_ANGLE               = "<";
    /** 
     * Right angle
     */
    public static final String RIGHT_ANGLE              = ">";
    /**
     * End element 
     */
    public static final String END_ELEMENT              = "/>";
    /** 
     * Start end element 
     */
    public static final String START_END_ELEMENT        = "</";
    /**
     * Space
     */
    public static final String SPACE                    = " ";
     /** 
      * SAML tag
      */
    public static final String SAML_LOG_NAME            = "SAML";
    /**
     * AuthenticationContext  prefix 
     */
    public static final String AC_PREFIX                = "AC:";
    /**
     * lib prefix
     */
    public static final String LIB_PREFIX               = "lib:";
    /**
     * Liberty namespace uri
     */
    public static final String libertyMessageNamespaceURI =
        "http://projectliberty.org/schemas/core/2002/12";
    /**
     * Liberty name space 
     */    
    public static final String LIB_NAMESPACE_STRING =
        " xmlns:lib=\"http://projectliberty.org/schemas/core/2002/12\"";
    /**
     * AuthenticationContext name space 
     */    
    public static final String AC_NAMESPACE_STRING =
        " xmlns:ac=\"http://projectliberty.org/schemas/authctx/2002/05\"";
    /**
     * AuthenticationContext namespace uri
     */
    public static final String AC_XML_NS = 
        "http://projectliberty.org/schemas/authctx/2002/05";
    /**
     * SAML assertion name space 
     */
    public static final String assertionSAMLNameSpaceURI =
        "urn:oasis:names:tc:SAML:1.0:assertion";
    /**
     * SOAP envelope uri
     */    
    public static final String SOAP_URI =
        "http://schemas.xmlsoap.org/soap/envelope/";
    /**
     * SOAP prefix 
     */    
    public static final String SOAP_ENV_PREFIX = "soap-env";
    /**
     * SAML protocol namespace 
     */
    public static final String PROTOCOL_NAMESPACE_URI =
        "urn:oasis:names:tc:SAML:1.0:protocol";
    /**
     * XML Digital signature namespace 
     */    
    public static final String DSSAMLNameSpace =
        "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"";
    /**
     * XML schema namespace 
     */    
    public static final String XSI_NAMESPACE_STRING =
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
    /**
     * SAML assertion namespace 
     */    
    public static final String assertionDeclareStr =
        " xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"";
    /**
     * SAML protocol namespace 
     */    
    public static final String PROTOCOL_NAMESPACE_STRING =
        " xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\"";
    /**
     * SAML Actions namespace identifiers: 
     * Read/Write/Execute/Delete/Control
     */    
    public static final String ACTIONS_NAMESPACE =
        "urn:oasis:names:tc:SAML:1.0:action:rwedc";
    /**
     * SAML Actions namespace identifiers: 
     * Read/Write/Execute/Delete/Control with negation
     */    
    public static final String ACTIONS_NAMESPACE_NEGATION = 
        "urn:oasis:names:tc:SAML:1.0:action:rwedc-negation";
    /**
     * SAML Actions namespace identifiers: 
     * Get/Head/Put/Post
     */  
    public static final String ACTIONS_NAMESPACE_GHPP =
        "urn:oasis:names:tc:SAML:1.0:ghpp";
    /**
     * SAML Actions namespace identifiers: 
     * Unix file permissions
     */     
    public static final String ACTIONS_NAMESPACE_UNIX =
        "urn:oasis:names:tc:SAML:1.0:action:unix";
    /**
     * SAML prefix
     */
    public static final String ASSERTION_PREFIX = "saml:";
    /**
     * SAML protocol prefix 
     */
    public static final String PROTOCOL_PREFIX = "samlp:";
    /**
     * Assertion major version
     */
    public static final int ASSERTION_MAJOR_VERSION = 1;
    /**
     * Assertion minor version
     */
    public static final int ASSERTION_MINOR_VERSION = 0;
    /** 
     * Protocol major version
     */
    public static final int PROTOCOL_MAJOR_VERSION = 1;
    /**
     * Protocol minor version
     */
    public static final int PROTOCOL_MINOR_VERSION = 0;
    /**
     * Default encoding
     */
    public static final String DEFAULT_ENCODING = "UTF-8";
    /**
     * Confirmation method: Artifact 
     */
    public static final String CONFIRMATION_METHOD_ARTIFACT =
        "urn:oasis:names:tc:SAML:1.0:cm:artifact-01";
    /**
     * Confirmation method
     */    
    public static final String CONFIRMATION_METHOD_DSAME =
        "urn:sun.com:sunone:ims";
    /**
     * Confirmation method: Bearer
     */    
    public static final String CONFIRMATION_METHOD_BEARER =
        "urn:oasis:names:tc:SAML:1.0:cm:bearer";
    /**
     * Default Artifact time out
     */    
    public static final int ARTIFACT_TIMEOUT_DEFAULT = 120;
    /**
     * Default Assertion time out
     */
    public static final int ASSERTION_TIMEOUT_DEFAULT = 60;
    /**
     * Assertion timeout allowed difference  
     */
    public static final int ASSERTION_TIMEOUT_ALLOWED_DIFFERENCE = 60000;
    /**
     * Site id
     */
    public static final String SITE_ID = "iplanet-am-saml-siteid";
    /**
     * Default Artifact name 
     */
    public static final String ARTIFACT_NAME_DEFAULT = "SAMLart" ;
    /**
     * <code>RelayState</code> parameter
     */
    public static final String LRURL = "RelayState";
    /**
     * Length of an identifier
     */
    public static final int ID_LENGTH = 20;
    /**
     * XML Digital Signing algorithm
     */
    public static final String ALGO_ID_SIGNATURE_DSA =
        "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
    /**
     * XML Digital Signing algorithm
     */    
    public static final String ALGO_ID_SIGNATURE_RSA =
        "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    /**
     * XML Digital Signing constant
     */       
    public static final String DEF_SIG_ALGO =
        "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
    /**
     * XML Digital Signing constant
     */ 
    public static final String DEF_SIG_ALGO_JCA =
        "SHA1withRSA";
    /**
     * XML Digital Signing constant
     */  
    public static final String ALGO_ID_SIGNATURE_DSA_JCA = 
        "SHA1withDSA";
    /**
     * XML Digital Signing constant
     */  
    public static final String ALGO_ID_SIGNATURE_RSA_JCA =
        "SHA1withRSA";
    /**
     * XML Digital Signing: DSA key 
     */      
    public static final String KEY_ALG_DSA = "DSA";
    /**
     * XML Digital Signing: RSA key 
     */  
    public static final String KEY_ALG_RSA = "RSA";
    /**
     * Liberty namespace uri 
     */
    public static final String LIB_NAMESPACE_URI =
        "http://projectliberty.org/schemas/core/2002/12";
    /**
     * Liberty browser artifact profile 
     */    
    public static final String SSO_PROF_BROWSER_ART =
        "http://projectliberty.org/profiles/brws-art";
    /**
     * Liberty browser post profile 
     */        
    public static final String SSO_PROF_BROWSER_POST =
        "http://projectliberty.org/profiles/brws-post";
    /**
     * Liberty WML post profile 
     */        
    public static final String SSO_PROF_WML_POST =
        "http://projectliberty.org/profiles/wml-post";
    /**
     * Liberty LECP profile 
     */         
    public static final String SSO_PROF_LECP =
        "http://projectliberty.org/profiles/lecp";
    /**
     * Local opaque handler
     */     
    public static final int LOCAL_OPAQUE_HANDLE = 0; 
    /**
     * Remote opaque handler
     */ 
    public static final int REMOTE_OPAQUE_HANDLE = 1;
    /**
     * Meta Alias
     */
    public static final String META_ALIAS = "metaAlias";
    /**
     * Goto parameter
     */
    public static final String GOTO_URL_PARAM = "goto";
    /**
     * Session host provider attribute 
     */
    public static final String SESSION_HOST_PROVIDER_ATTR = "hostid";
    /**
     * AuthnContext 
     */
    public static final String AUTHN_CONTEXT = "AuthnContext";
    /**
     * Artifact length 
     */
    public static final int ART_ID_LENGTH = 20;
    /** 
     * Sourceid encoding
     */
    public static final String SOURCEID_ENCODING = "ISO-8859-1";

    /**
     * Max session time
     */
    public static final int MAX_SESSION_TIME = 12000;
    /**
     * Max ide time
     */
    public static final int MAX_IDLE_TIME = 3000;
    /**
     * Max caching time
     */
    public static final int MAX_CACHING_TIME = 500;
    /**
     * Max session time key 
     */
    public static final String MAX_SESSION_TIME_KEY = "SESSION_TIME";
    /**
     * Max idle time key
     */
    public static final String MAX_IDLE_TIME_KEY = "IDLE_TIME";
    /**
     * Max caching time key
     */
    public static final String MAX_CACHING_TIME_KEY = "CACHING_TIME";
    /**
     * Error code for Missing Authentication Context in entity metadata.
     */ 
    public static final String META_MISSING_AUTH_CONTEXT =
        "meta_missing_auth_context";

    /**
     * Error code for Invalid value for Priority in entity metadata.
     */     
    public static final String META_INVALID_LEVEL =
        "meta_invalid_level";
    /**
     * Error code for Contact person type
     */                             
    public static final String TECHNICAL        = "technical";
    /**
     * Error code for Contact person type
     */  
    public static final String ADMINISTRATIVE   = "administrative";
    /**
     * Error code for Contact person type
     */  
    public static final String BILLING          = "billing";
    /**
     * Error code for Contact person type
     */  
    public static final String OTHER            = "other";       
     /**
     * Specified logout profile is not supported. 
     */    
    public static final String LOGOUT_PROFILE_NOT_SUPPORTED = 
        "logout-profile-not-supported";
    /**
     * No provider is specified in name registration.
     */     
    public static final String REGISTRATION_NO_PROVIDER = 
        "registration-no-provider";
    /**
     * Invalid Provider in Registration.
     */  
    public static final String REGISTRATION_INVALID_PROVIDER =
        "registration-invalid-provider";
    /**
     * Invalid Signature for Registration.
     */  
    public static final String  REGISTRATION_INVALID_SIGNATURE= 
        "registration-invalid-signature";
    /**
     * Registration failed.
     */
    public static final String REGISTRATION_FAILED      = 
        "registration-failed";
    /**
     * Improper Registration Request
     */
    public static final String REGISTRATION_REQUEST_IMPROPER = 
        "registration-request-improper";   
    /**
     * Console service 
     */ 
    public static final String CONSOLE_SERVICE =
        "iPlanetAMAdminConsoleService";
    /**
     * Console service version
     */
    public static final String CONSOLE_SERVICE_VERSION =
        "1.0";
    /**
     * Liberty enabled console
     */
    public static final String LIBERTY_ENABLED_KEY =
       "iplanet-am-admin-console-liberty-enabled";
    /**
     * Local configuration is invalid. 
     */   
    public static final String LOCALCONFIG_INVALID = "localconfig_invalid";
    /**
     * Invalid circle of trust list
     */
    public static final String COTLIST_INVALID =
        "cotlist_invalid";
    /**
     * <code>AuthnRequest</code> RequestID attribute
     */  
    public static final String AUTH_REQUEST_ID = "RequestID";
    /**
     * <code>arg</code> key
     */  
    public static final String ARGKEY = "arg";
    /**
     * New Session
     */
    public static final String NEWSESSION = "newsession";
    /**
     * Post-login page
     */
    public static final String POST_LOGIN_PAGE = "/postLogin";
    /**
     * Pre-login page
     */
    public static final String PRE_LOGIN_PAGE = "/preLogin";
    /**
     * Federate keyword
     */
    public static final String FEDERATEKEY = "federate";
    /**
     * Default value of the <code>federate</code>
     */
    public static final String FEDERATEVALUE = "false";
    /**
     * sso keyword
     */
    public static final String SSOKEY = "sso";
    /**
     * Default value of the <code>sso</code>
     */
    public static final String SSOVALUE = "true";
    /**
     * cotkey keyword
     */
    public static final String COTKEY = "cotkey";
    /**
     * Chosen provider
     */
    public static final String SELECTEDPROVIDER = "selectedprovider";
    /**
     * goto parameter 
     */
    public static final String GOTOKEY = "goto";
    /**
     * org parameter
     */
    public static final String ORGKEY = "org";
    /**
     * authlevel parameter 
     */
    public static final String AUTH_LEVEL_KEY = "authlevel";
    /** 
     * AuthContext Comparison type
     */
    public static final String  MINIMUM = "minimum";
    /** 
     * AuthContext Comparison type
     */
    public static final String EXACT = "exact";
    /** 
     * AuthContext Comparison type
     */
    public static final String BETTER = "better";
    /** 
     * AuthContext Comparison type
     */
    public static final String MAXIMUM = "maximum";
    /**
     * Parameter used in introduction profile
     */
    public static final String PROVIDER_ID_KEY = "_liberty_idp" ;
    /**
     * Parameter used to pass realm value.
     */
     public static final String REALM = "realm";
    /**
     * Intersite transfer URL
     */
    public static final String INTERSITE_URL = "IntersiteTransferService";
    /**
     * Part of the login URL 
     */
    public static final String LOGIN_PAGE = "/UI/Login";
    /**
     * Useraction value: Cancel 
     */
    public static final String CANCEL = "cancel";
    /**
     * <code>action</code> parameter in the request object 
     */
    public static final String USERACTION = "action";  
    /**
     * Consent handler 
     */
    public static final String CONSENT_HANDLER = "consentHandler";
    /**
     * Federation handler 
     */
    public static final String FEDERATION_HANDLER = "federation";
    /**
     * Key used to search for header info in a map 
     */
    public static final String HEADER_KEY = "headerKey";
    /**
     * Key used to search for URL info in a map   
     */
    public static final String URL_KEY = "urlKey";
    /**
     * Key used to search for response info in a map   
     */
    public static final String RESPONSE_DATA_KEY = "responseData";
    /**
     * Hosted provider
     */
    public static final String PROVIDER_HOSTED = "hosted";
    /**
     * Remote provider 
     */
    public static final String PROVIDER_REMOTE = "remote";
    /**
     * Entity descriptor
     */
    public static final String ENTITY_DESCRIPTOR = "entitydescriptor";
    /** 
     * Local AuthType
     */
    public static final String AUTH_LOCAL = "local";
    /**
     * Remote AuthType
     */
    public static final String AUTH_REMOTE = "remote";
    /**
     * Home page file name
     */
    public static final String HOMEPAGE_URL_VALUE ="/index.html";
    /**
     * Common login page file name 
     */
    public static final String COMMON_LOGIN_PAGE_URL_VALUE ="/CommonLogin.jsp";
    /**
     * JSP file location 
     */
    public static final String JSP_LOCATION = "/config/federation/default";
    /**
     * SOAP endpoint value
     */
    public static final String SOAP_END_POINT_VALUE = "/SOAPReceiver";
    /**
     * Single logout endpoint value
     */
    public static final String SLO_VALUE =  "/ProcessLogout";
    /**
     * Single logout return endpoint value
     */
    public static final String SLO_RETURN_VALUE = "/ReturnLogout";
    /**
     * Federation termination endpoint value
     */
    public static final String TERM_VALUE = "/ProcessTermination";
    /**
     * Federation termination  return endpoint value
     */
    public static final String TERM_RETURN_VALUE = "/ReturnTermination";
    /**
     * Assertion consumer service endpoint value
     */
    public static final String ASSERTION_CONSUMER_VALUE =
        "/AssertionConsumerService";
    /**
     * Single signon endpoint value
     */
    public static final String SSO_VALUE =  "/SingleSignOnService";
    /**
     * Configuration root directory
     */
    public static final String CONFIG_ROOT = "/config/federation";
    /**
     * Resource lookup related variables.
     */
    public static final String CONFIG_DIR = "config";
    public static final String FEDERATION_DIR = "federation";
    public static final String DEFAULT_DIR = "default";

    /**
     * AuthenticatonStatement prefix
     */
    public static final String RESPOND_WITH =
        "lib:AuthenticationStatement";
    /**
     * Federation jsp name
     */
    public static final String DOFEDERATE_PAGE_NAME = 
        "Federate.jsp";
    /**
     * Logout completed jsp name 
     */
    public static final String LOGOUTDONE_PAGE_NAME = 
        "LogoutDone.jsp";
    /**
     * Federation completed jsp name 
     */
    public static final String FEDERATIONDONE_PAGE_NAME =
        "FederationDone.jsp";
    /**
     * Error page
     */    
    public static final String ERROR_PAGE_NAME = "Error.jsp";
    /**
     * List of circle of trust page 
     */
    public static final String LISTOFCOTS_PAGE_NAME = "ListOfCOTs.jsp";
    /**
     * Federation termination done jsp name 
     */
    public static final String TERMINATION_DONE_PAGE_NAME = 
        "TerminationDone.jsp";
    /**
     * Name registration done jsp name 
     */   
    public static final String NAME_REGISTRATION_DONE_PAGE_NAME =
        "NameRegistrationDone.jsp";
    /**
     * Common login jsp name  
     */      
    public static final String COMMON_LOGIN_PAGE_NAME = 
        "CommonLogin.jsp";
    /**
     * Termination status 
     */
    public static final String TERMINATION_STATUS ="termStatus";
    /**
     * Termination status : Sucess
     */
    public static final String TERMINATION_SUCCESS ="termSuccess";
    /**
     * Termination status : Failure 
     */
    public static final String TERMINATION_FAILURE ="termFailure";
    /**
     * Provider id key is usd in Termination. 
     */
    public static final String TERMINATION_PROVIDER_ID =
        "_liberty_id";
    /**
     * Failed to get hosted descriptor
     */
    public static final String FAILED_HOSTED_DESCRIPTOR =
        "failed-reading-hosted-descriptor";
    /**
     * Logout status
     */
    public static final String LOGOUT_STATUS="logoutStatus";
    /**
     * Logout status: Success
     */
    public static final String LOGOUT_SUCCESS="logoutSuccess";
    /**
     * Logout status: Failure
     */
    public static final String LOGOUT_FAILURE="logoutFailure";
    /**
     * Logout status: No Session
     */
    public static final String LOGOUT_NO_SESSION="noSession";
    /**
     * boolean: <code>true</code>
     */
    public static final String TRUE = "true";
    /**
     * boolean: <code>false</code>
     */
    public static final String FALSE = "false";
    /**
     * Optional attribute
     */
    public static final String OPTIONAL = "optional";
    /**
     * Provider id key is used in name registration.
     */    
    public static final String REGISTRATION_PROVIDER_ID="_liberty_id";
    /**
     * Failed to  create name registration handlde
     */
    public static final String REGISTRATION_HANDLE_CREATION_FAILED = 
        "registration-handle-creation-failed";
    /**
     * Registration status 
     */    
    public static final String REGISTRATION_STATUS="regisStatus";
    /**
     * Registration status : Success
     */   
    public static final String REGISTRATION_SUCCESS="regisSuccess";
    /**
     * Registration status : Failure
     */   
    public static final String REGISTRATION_FAILURE="regisFailure";
    /**
     * Relay state
     */
    public static String REGISTRATION_RELAY_STATE = "RELAYSTATE";
    /**
     * Name Registration profile 
     */
    public static final String REGISTRATION_IDP_HTTP_PROFILE =
        "http://projectliberty.org/profiles/rni-idp-http";
    /**
     * Name Registration profile 
     */
    public static final String REGISTRATION_IDP_SOAP_PROFILE = 
        "http://projectliberty.org/profiles/rni-idp-soap";
    /**
     * Name Registration profile 
     */
    public static final String REGISTRATION_SP_SOAP_PROFILE =
        "http://projectliberty.org/profiles/rni-sp-soap";
    /**
     * Name Registration profile 
     */
    public static final String REGISTRATION_SP_HTTP_PROFILE = 
        "http://projectliberty.org/profiles/rni-sp-http";
    /**
     * Single signon service 
     */
    public static final String SSO_URL = "/SingleSignOnService";
    /**
     * Process registration uri
     */
    public static final String REGISTRATION_VALUE = "/ProcessRegistration";
    /**
     * Registration return uri
     */
    public static final String REGISTRATION_RETURN_VALUE = 
        "returnRegistration";
    /**
     * Do name registration after federation
     */    
    public static final String REGIS_FEDERATION = "AfterFederation"; 
    /**
     * Do name registration during single signon process
     */  
    public static final String REGIS_SSO = "DuringSSO"; 
    /**
     * Do name registration on demand 
     */  
    public static final String REGIS_LINK = "STAND_ALONE";
    /**
     * Signature Id used in termination profile 
     */
    public static final String TERMINATIONID = "term-Sig-ID";
    /**
     * Signature Id used in name registration profile 
     */
    public static final String REGISTRATIONID = "reg-Sig-ID";
    /**
     * Signature Id used in logout profile 
     */
    public static final String LOGOUTID = "logout-sig-ID";
    /**
     * LECP request header 
     */
    public static final String LECP_CONTENT_TYPE_HEADER = 
        "application/vnd.liberty-request+xml";
    /**
     * LECP response header 
     */   
    public static final String LECP_RESP_CONTENT_TYPE_HEADER =
        "application/vnd.liberty-response+xml";
    /**
     * LECP indicator parameter 
     */   
    public static final String LECP_INDICATOR_PARAM = "LECP";
    /**
     * LECP indicator parameter's default value 
     */   
    public static final String LECP_INDICATOR_VALUE = "TRUE";
    /** 
     * Goto this URL if no federation cookie presents.
     */
    public static final String GOTO_ON_FEDCOOKIE_NO_URL = "gotoOnFedCookieNo"; 
    /**
     * <code>isPassive</code> 
     */
    public static final String IS_PASSIVE_QUERY = "isPassive"; 
    /**
     * Take action if no federation cookie presents.
     */
    public static final String ACTION_ON_NO_FED_COOKIE = "actionOnNoFedCookie";

    /**
     * Boot strapping
     */
    public static final String DISCO_RESOURCE_OFFERING_NAME = 
                    "DiscoveryResourceOffering";
    /**
     * Termination notification profile
     */                         
    public static final String TERMINATION_NOTIFICATION_PROFILE = 
        "http://projectliberty.org/profiles/rel-term-soap";
    /**
     * IDFF1.2 Name space
     */
    public static final String LIB_12_NAMESPACE_STRING =
        " xmlns:lib=\"urn:liberty:iff:2003-08\"";
    /**
     * Authentication context Name space
     */    
    public static final String AC_12_NAMESPACE_STRING=
        " xmlns:ac=\"urn:liberty:ac:2003-08\"";
    /**
     * IDFF1.2 Name space
     */    
    public static final String FF_12_XML_NS="urn:liberty:iff:2003-08";
    /**
     * Authentication context Name space urn
     */
    public static final String AC_12_XML_NS="urn:liberty:ac:2003-08";
    /**
     * IDFF 1.2 saml protocol minor version
     */
    public static final int FF_12_SAML_PROTOCOL_MINOR_VERSION = 1;
    /**
     * IDFF 1.1 saml protocol minor version
     */ 
    public static final int FF_11_SAML_PROTOCOL_MINOR_VERSION = 0;
    /**
     * IDFF 1.2 saml assertion minor version (POST profile)
     */   
    public static final int FF_12_POST_ASSERTION_MINOR_VERSION = 2;
    /**
     * IDFF 1.2 saml assertion minor version (Artifact profile)
     */   
    public static final int FF_12_ART_ASSERTION_MINOR_VERSION = 2;
    /**
     * IDFF 1.1 saml assertion minor version (Artifact profile)
     */  
    public static final int FF_11_ASSERTION_MINOR_VERSION = 0;
    /**
     * IDFF 1.2 saml protocol minor version
     */ 
    public static final int FF_12_PROTOCOL_MINOR_VERSION = 2;
    /**
     * IDFF 1.1 saml protocol minor version
     */ 
    public static final int FF_11_PROTOCOL_MINOR_VERSION = 0;
    /**
     * Name Identifier format URI 
     */
    public static final String NI_FEDERATED_FORMAT_URI =
        "urn:liberty:iff:nameid:federated";
    /**
     * Name Identifier format URI 
     */
    public static final String NI_ONETIME_FORMAT_URI =
        "urn:liberty:iff:nameid:one-time";
    /**
     * Name Identifier format URI 
     */
    public static final String NI_ENCRYPTED_FORMAT_URI =
        "urn:liberty:iff:nameid:encrypted";
    /**
     * Discovery Resource Offering
     */    
    public static final String DISCOVERY_RESOURCE_OFFERING =
        "DiscoveryResourceOffering";
    /**
     * Assertion ID
     */
    public static final String ASSERTION_ID="AssertionID"; 
    /**
     * Request ID
     */
    public static final String REQUEST_ID="RequestID"; 
    /**
     * Response ID
     */
    public static final String RESPONSE_ID="ResponseID";
    /**
     * ID 
     */
    public static final String ID="id";
    
    /**
     * NameIDPolicy Element name
     */
    public static final String NAMEID_POLICY_ELEMENT="NameIDPolicy";

    /**
     * NameIDPolicy configuration attribute name
     */
    public static final String NAMEID_POLICY="nameIDPolicy";

    /**
     * Key is used for signing.
     */
    public static final String KEYTYPE_SIGNING="signing";
    /**
     * Key is used for encryption.
     */
    public static final String KEYTYPE_ENCRYPTION="encryption";
    /**
     * <code>NameIDPolicy</code> value type
     */
    public static final String NAME_ID_POLICY_NONE = "none";
     /**
     * <code>NameIDPolicy</code> value type
     */
    public static final String NAME_ID_POLICY_ONETIME = "onetime";
    /**
     * <code>NameIDPolicy</code> value type
     */
    public static final String NAME_ID_POLICY_FEDERATED = "federated";
    /**
     * <code>NameIDPolicy</code> value type
     */
    public static final String NAME_ID_POLICY_ANY = "any";
    /**
     * Idp finder URL
     */
    public static final String IDP_FINDER_URL ="/idpfinder";
    /**
     * XPATH for Assertion Artifact 
     */
    public static final String ARTIFACT_XPATH = 
       "/Envelope/Body/Request/AssertionArtifact";
    /**
     * Affiliation id for Affiliation descriptor
     */   
    public static final String AFFILIATED = "Affiliated";
    /**
     * Auth Federation attribute 
     */
    public static final String AUTO_FED_ATTR = "AutoFederateAttribute";
    /**
     * Provider id separator 
     */
    public static final String PROVIDER_ID_SEPARATOR = "|";
    /**
     * SP provider suffix 
     */
    public static final String SP_PROVIDER_SFX =
        PROVIDER_ID_SEPARATOR + "SP";
    /**
     * IDP provider suffix 
     */ 
    public static final String IDP_PROVIDER_SFX = 
        PROVIDER_ID_SEPARATOR + "IDP";
    /**
     * Used in local configuration descriptor to track the version
     */
    public static final int PROVIDER_CONFIG_70_VERSION = 30;
    /**
     * Used in local configuration descriptor to track the version
     */
    public static final int PROVIDER_CONFIG_71_VERSION = 40;
    /**
     * Anonymous Onetime federation
     */
    public static final String ANONYMOUS_ONETIME = "anonymousOnetime";
    /**
     * Parameter for status code. Used in FederationSPAdapter.
     */
    public static final String STATUS_CODE = "StatusCode";
    /**
     * Parameter for failure code. Used in FederationSPAdapter.
     */
    public static final String FAILURE_CODE = "FailureCode";

    /**
     * Used by <code>FSUserProvider</code> to pass in termination request
     * through env map.
     */
    public static final String FS_USER_PROVIDER_ENV_TERMINATION_KEY =
                "FSFederationTerminationNotification";
    /**
     * Used by <code>FSUserProvider</code> to pass in authn response
     * through env map.
     */
    public static final String FS_USER_PROVIDER_ENV_AUTHNRESPONSE_KEY =
                "FSAuthnResponse";
    /**
     * Used by <code>FSUserProvider</code> to pass in logout request
     * through env map.
     */
    public static final String FS_USER_PROVIDER_ENV_LOGOUT_KEY =
                "FSLogoutNotification";
    /**
     * Used by <code>FSUserProvider</code> to pass in registration request
     * through env map.
     */
    public static final String FS_USER_PROVIDER_ENV_REGISTRATION_KEY =
                "FSNameRegistrationRequest";
    /**
     * Used by <code>FSUserProvider</code> to pass in saml response
     * through env map.
     */
    public static final String FS_USER_PROVIDER_ENV_FSRESPONSE_KEY =
                "FSResponse";
    /**
     * Used by <code>FSUserProvider</code> to pass in name ID mapping request
     * through env map.
     */
    public static final String FS_USER_PROVIDER_ENV_NAMEMAPPING_KEY =
                "FSNameIdentifierMappingRequest";

    /**
     * Default <code>FSUserProvider</code> implemeation class.
     */
    public static final String FS_USER_PROVIDER_DEFAULT =
                "com.sun.identity.federation.accountmgmt.DefaultFSUserProvider";
    /**
     * ID-FF component name used in obtaining provider.
     */
    public static final String IDFF = "IDFF";

    /**
     * Key name for the federation attribute mapper.
     */
    public static final String FS_ATTRIBUTE_MAPPER =
        "com.sun.identity.liberty.fed.attributemapper";
                                                                              
    /**
     * Key name for the webservices security attribute mapper.
     */
    public static final String WS_ATTRIBUTE_PLUGIN =
        "com.sun.identity.liberty.ws.attributeplugin";
    
    /**
     * Assertion
     */
    public static final String ASSERTION = "Assertion";

    /**
     * Major Version
     */
    public static final String MAJOR_VERSION="MajorVersion";

    /**
     * Minor Version
     */
    public static final String MINOR_VERSION="MinorVersion";

    /**
     * Issuer
     */
    public static final String ISSUER="Issuer";
    
    /**
     * InResponseTo
     */
    public static final String IN_RESPONSE_TO="InResponseTo";
    
    /**
     * IssueInstant
     */
    public static final String ISSUE_INSTANT="IssueInstant";

    /**
     * Conditions
     */
    public static final String CONDITIONS="Conditions";
    
    /**
     * Advice
     */
    public static final String ADVICE="Advice";
    
    /**
     * AuthenticationStatement
     */
    public static final String AUTHENTICATIONSTATEMENT 
                                      = "AuthenticationStatement";
    /**
     * AuthorizationDecisionStatement
     */
    public static final String AUTHZDECISIONSTATEMENT =
                                     "AuthorizationDecisionStatement";

    /**
     * AttributeStatement
     */
    public static final String ATTRIBUTESTATEMENT = "AttributeStatement";

    /**
     * Signature
     */
    public static final String SIGNATURE = "Signature";
    
    /**
     * AssertionType
     */
    public static final String ASSERTION_TYPE = "AssertionType";

    /**
     * xsi type
     */
    public static final String XSI_TYPE = "xsi:type";
    
    /**
     * Authentication Method
     */
    public static final String AUTHENTICATION_METHOD="AuthenticationMethod";   
    
    /**
     * Authentication Instant
     */
    public static final String AUTHENTICATION_INSTANT="AuthenticationInstant";

    /**
     * Authority Binding
     */
    public static final String AUTHORITY_BINDING= "AuthorityBinding";   
    
    /**
     * SubjectLocality
     */
    public static final String SUBJECT_LOCALITY="SubjectLocality";

    
    /**
     * ReauthenticationOnOrAfter
     */
    public static final String REAUTH_ON_OR_AFTER="ReauthenticateOnOrAfter";

    /**
     * Authentication Subject
     */
    public static final String AUTH_SUBJECT="Subject";

    /**
     * Authentication Statement Type
     */
    public static final String AUTHENTICATIONSTATEMENT_TYPE
            = "AuthenticationStatementType";

    /**
     * Authentication Request 
     */
    public static final String AUTHN_REQUEST="AuthnRequest";

    /**
     * RespondWith
     */
    public static final String RESPONDWITH="RespondWith";

    /**
     * Consent
     */
    public static final String CONSENT="consent";
    
    /**
     * Provider Identifier
     */
    public static final String PROVIDER_ID="ProviderID";   
    
    /**
     * Federate
     */
    public static final String FEDERATE ="Federate";
    
    /**
     * Major Version Value
     */
       
    public static final String ONE ="1";

    /**
     * ProtocolProfile
     */
    public static final String PROTOCOL_PROFILE="ProtocolProfile";
    
    /**
     * Requested AuthnContext
     */
    
    public static final String REQUEST_AUTHN_CONTEXT="RequestAuthnContext";
    
    /**
     * Affliation Identifier
     */
    public static final String	AFFILIATIONID = "AffiliationID";
    
    /**
     * Extension
     */
    public static final String	EXTENSION = "Extension";
    
    /**
     * Scoping
     */
    public static final String	SCOPING = "Scoping";
 
    /**
     * Assertion Consumer Service Identifier
     */
    public static final String ASSERTION_CONSUMER_SVC_ID=
            "AssertionConsumerServiceID";
    
    /**
     * Authenticaion Context Comparison
     */
    public static final String AUTHN_CONTEXT_COMPARISON=
            "AuthnContextComparison";

    /**
     * RelayState
     */
    public static final String RELAY_STATE ="RelayState";
    
    /**
     * NameIdentifier Mapping Request
     */
    public static final String NAMEID_MAPPING_REQUEST =
            "NameIdentifierMappingRequest";
    
    /**
     * NameIdentifier Mapping Response
     */
    public static final String NAMEID_MAPPING_RESPONSE =
            "NameIdentifierMappingResponse";
    
    /**
     * Target Name Space
     */
    public static final String TARGET_NAME_SPACE="TargetNamespace";
    
    /**
     * XML Prefix String
     */
    public static final String XML_PREFIX="<?xml version=\"1.0\" encoding=\"";
    
    /**
     * Name Identifier
     */
    public static final String NAME_IDENTIFIER="NameIdentifier";
    
    /**
     * Federation Termination Notification
     */
    public static final String FEDERATION_TERMINATION_NOTICFICATION =
                                            "FederationTerminationNotification";
    
    /**
     * Name 
     */
    public static final String NAME = "Name";
    
    /** 
     * Name Qualifier
     */
    
    public static final String NAME_QUALIFIER = "NameQualifier";
    
    /**
     * NameFormat
     */
    public static final String NAME_FORMAT = "NameFormat";
    
    /**
     * GetComplete
     */
    public static final String GET_COMPLETE = "GetComplete";
    
    /**
     * IDP Entries 
     */
    public static final String IDP_ENTRIES = "IDPEntries";
    
    /**
     * LogoutRequest
     */
    public static final String LOGOUT_REQUEST = "LogoutRequest";
    
    /**
     * LogoutResponse
     */
     
    public static final String LOGOUT_RESPONSE = "LogoutResponse";
    
    /**
     * NotOnOrAfter
     */
    public static final String NOT_ON_OR_AFTER = "NotOnOrAfter";
    
    /**
     * Value
     */
    public static final String VALUE = "Value";

    /**
     * IDPList
     */
    public static final String IDP_LIST="IDPList";
    
    /**
     * Authentication Request Envelope
     */
    public static final String AUTHN_REQUEST_ENVELOPE="AuthnRequestEnvelope";
    
    /**
     * Assertion Consumer Service URL
     */
    public static final String ASSERTION_CONSUMER_SERVICE_URL=
            "AssertionConsumerServiceURL";

    /**
     * Authentication Response
     */
    public static final String AUTHN_RESPONSE="AuthnResponse";
    
    /**
     * Authentication Response Envelope
     */
    public static final String AUTHN_RESPONSE_ENVELOPE
                                        ="AuthnResponseEnvelope";  
    
    /**
     * Recipient
     */
    public static final String RECIPIENT = "Recipient";

    /**
     * Status
     */
    public static final String STATUS = "Status";
    
    /**
     * ProviderName
     */
    public static final String PROVIDER_NAME="ProviderName";
    
    /**
     * IsPassive Element Name
     */
    public String IS_PASSIVE_ELEM = "IsPassive"; 

    /**
     * Force Authentication Element Name
     */
    public String FORCE_AUTHN_ELEM = "ForceAuthn";


    /**
     * Provider Status
     */
    public String PROVIDER_STATUS = "providerStatus";

    /**
     * Signing Certificate Alias 
     */ 
    public String SIGNING_CERT_ALIAS = "signingCertAlias";

    /**
     * Encryption Certificate Alias
     */
    public static final String ENCRYPTION_CERT_ALIAS = "encryptionCertAlias";

    /**
     * Enable IDP Proxy 
     */ 
    public String ENABLE_IDP_PROXY = "enableIDPProxy";

    /**
     * IDP Proxy Name List 
     */ 
    public String IDP_PROXY_LIST = "idpProxyList";

    /**
     * IDP Proxy Count 
     */ 
    public String IDP_PROXY_COUNT = "idpProxyCount";

    /**
     * Use Introduction for IDP Proxy  
     */ 
    public String USE_INTRODUCTION_FOR_IDP_PROXY = "useIntroductionForIDPProxy";

    /**
     * Enable Name Identifier Encryption 
     */ 
    public String ENABLE_NAMEID_ENCRYPTION = "enableNameIDEncryption";

    /**
     * Generate Bootstrapping in Single Sign-on Assertion 
     */ 
    public String GENERATE_BOOTSTRAPPING = "generateBootstrapping";

    /**
     * Responds with 
     */ 
    public String RESPONDS_WITH = "respondsWith";

    /**
     * Name Identifier Implementation Class 
     */ 
    public String NAMEID_IMPL_CLASS = "nameIDImplementationClass";

    /**
     * Authentication Type (remote/local)
     */ 
    public String AUTH_TYPE = "authType";

    /**
     * Registration Done URL 
     */ 
    public String REGISTRATION_DONE_URL = "registrationDoneURL";

    /**
     * Termination Done URL 
     */ 
    public String TERMINATION_DONE_URL = "terminationDoneURL";

    /**
     * Logout Done URL 
     */ 
    public String LOGOUT_DONE_URL = "logoutDoneURL";

    /**
     * Federation Done URL 
     */ 
    public String FEDERATION_DONE_URL = "federationDoneURL";

    /**
     * Single Sign-on failure redirection URL
     */ 
    public String SSO_FAILURE_REDIRECT_URL = "ssoFailureRedirectURL";

    /**
     * Error page redirection URL
     */ 
    public String ERROR_PAGE_URL = "errorPageURL";

    /**
     * List of COTs page URL.
     */
    public String LISTOFCOTS_PAGE_URL = "listOfCOTsPageURL";

    /**
     * Do federate page URL.
     */
    public String DOFEDERATE_PAGE_URL = "doFederatePageURL";

    /**
     * Implementation class for SPI <code>FSUserProvider</code>.
     */
    public String FS_USER_PROVIDER_CLASS = "userProviderClass";

    /**
     * Provider Home page URL.
     */ 
    public String PROVIDER_HOME_PAGE_URL = "providerHomePageURL";

    /**
     * Assertion valid interval 
     */ 
    public String ASSERTION_INTERVAL = "assertionInterval"; 

    /**
     * Internal Thread Cleanup Internal 
     */ 
    public String CLEANUP_INTERVAL = "cleanupInterval";

    /**
     * Artifact Timeout Interval 
     */ 
    public String ARTIFACT_TIMEOUT = "artifactTimeout";

    /**
     * Maximum Assertion Limit 
     */ 
    public String ASSERTION_LIMIT = "assertionLimit";

    /**
     * Assertion Issuer 
     */ 
    public String ASSERTION_ISSUER = "assertionIssuer";

    /**
     * Attribute Plugin 
     */ 
    public String ATTRIBUTE_PLUGIN = "attributePlugin";

    /**
     * IDP Attribute Map 
     */ 
    public String IDP_ATTRIBUTE_MAP = "idpAttributeMap";

    /**
     * Whether to initialize registration after sso/federation.
     * Currently, it is not used.
     */
    public String ENABLE_REGISTRATION_AFTER_SSO = "enableRegistrationAfterSSO";

    /**
     * Default AuthnContext  
     */ 
    public String DEFAULT_AUTHNCONTEXT = "defaultAuthnContext";

    /**
     * Default AuthnContext Password
     */ 
    public String DEFAULT_AUTHNCONTEXT_PASSWORD = 
        "http://www.projectliberty.org/schemas/authctx/classes/Password";

    /**
     * IDP AuthnContext Mapping 
     */ 
    public String IDP_AUTHNCONTEXT_MAPPING = "idpAuthnContextMapping";

    /**
     * Enable Auto-Federation 
     */ 
    public String ENABLE_AUTO_FEDERATION = "enableAutoFederation";

    /**
     * Auto-Federation Attribute 
     */ 
    public String AUTO_FEDERATION_ATTRIBUTE = "autoFederationAttribute";

    /**
     * Attribute Mapper Class 
     */ 
    public String ATTRIBUTE_MAPPER_CLASS = "attributeMapperClass";

    /**
     * Circle-of-trust list 
     */ 
    public String COT_LIST = COTConstants.COT_LIST;

    /**
     * Enable Affiliation 
     */ 
    public String ENABLE_AFFILIATION = "enableAffiliation";

    /**
     * Force Authentication at IDP 
     */ 
    public String FORCE_AUTHN = "forceAuthn";

    /**
     * Request IDP to be Passive 
     */ 
    public String IS_PASSIVE = "isPassive";

    /**
     * Service Provider AuthnContext Mapping 
     */ 
    public String SP_AUTHNCONTEXT_MAPPING = "spAuthnContextMapping";

    /**
     * Service Provider Attribute Map 
     */ 
    public String SP_ATTRIBUTE_MAP = "spAttributeMap";

    /**
     * Federation Service Provider Adapter 
     */ 
    public String FEDERATION_SP_ADAPTER = "federationSPAdapter";

    /**
     * Environment variables for Federation Service Provider Adapter
     */ 
    public String FEDERATION_SP_ADAPTER_ENV = "federationSPAdapterEnv";

    /**
     * Service Provider's supported profiles for single sign on.
     * First one is the default one.
     */
    public String SUPPORTED_SSO_PROFILE = "supportedSSOProfile";

    /**
     * Constants for authn context mapping values.
     */
    public String ATTRIBUTE_SEPARATOR = "|";
    public String KEY_VALUE_SEPARATOR = "=";
    public String AUTH_CONTEXT_NAME = "context";
    public String MODULE_INDICATOR_KEY = "key";
    public String MODULE_INDICATOR_VALUE = "value";
    public String LEVEL = "level";

    /**
     * Provider description.
     */
    public String PROVIDER_DESCRIPTION = "providerDescription";

    /**
     * Service Provider's default relay state.
     */
    public String SP_DEFAULT_RELAY_STATE =
        "/samples/idff/sp/index.jsp";
    
    /**
     * boolean to indicate if this is SOAP profile
     */
    public String IS_SOAP_PROFILE = "isSOAPProfile";

    /**
     * root realm.
     */
    public String ROOT_REALM = "/";

    /**
     * SAMLRequest query parameter name
     */
    public String SAML_REQUEST = "SAMLRequest";

    /**
     * SAMLResponse query parameter name
     */
    public String SAML_RESPONSE = "SAMLResponse";

    /**
     * Parameter name for SAML artifact in http request.
     */
    public String SAML_ART = "SAMLart";

}

