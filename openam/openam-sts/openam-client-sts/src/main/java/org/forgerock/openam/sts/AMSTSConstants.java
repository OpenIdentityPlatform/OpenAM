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

package org.forgerock.openam.sts;

import org.forgerock.openam.shared.sts.SharedSTSConstants;

import javax.xml.namespace.QName;

public class AMSTSConstants {
    /*
    Necessary to distinguish the originator of the invocation to the Token Generation Service. Calls need to
    be distinguished so that the appropriate STSInstanceConfigPersister can be referenced to pull config state for the
    STS instance.
     */
    public enum STSType {REST, SOAP}
    /*
    The namespace defined by the WS-Trust specification.
     */
    public static final String WS_TRUST_NAMESPACE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/";

    /*
    These constants define the QNames referencing services and ports in wsdl documents which define the semantics and
    SecurityPolicy bindings of published STS instances.
     */
    public static final QName STS_SERVICE = new QName(WS_TRUST_NAMESPACE, "unprotected_sts_service");
    public static final QName STS_SERVICE_PORT = new QName(WS_TRUST_NAMESPACE, "unprotected_sts_service_port");
    public static final QName SYMMETRIC_UT_STS_SERVICE = new QName(WS_TRUST_NAMESPACE, "symmetric_ut_sts_service");
    public static final QName SYMMETRIC_UT_STS_SERVICE_PORT = new QName(WS_TRUST_NAMESPACE, "symmetric_ut_sts_service_port");
    public static final QName ASYMMETRIC_UT_STS_SERVICE = new QName(WS_TRUST_NAMESPACE, "asymmetric_ut_sts_service");
    public static final QName ASYMMETRIC_UT_STS_SERVICE_PORT = new QName(WS_TRUST_NAMESPACE, "asymmetric_ut_sts_service_port");
    public static final QName TRANSPORT_UT_STS_SERVICE = new QName(WS_TRUST_NAMESPACE, "transport_ut_sts_service");
    public static final QName TRANSPORT_UT_STS_SERVICE_PORT = new QName(WS_TRUST_NAMESPACE, "transport_ut_sts_service_port");
    public static final QName SYMMETRIC_ENDORSING_CERT_STS_SERVICE = new QName(WS_TRUST_NAMESPACE, "symmetric_endorsing_cert_sts_service");
    public static final QName SYMMETRIC_ENDORSING_CERT_STS_SERVICE_PORT = new QName(WS_TRUST_NAMESPACE, "symmetric_endorsing_cert_sts_service_port");

    /*
    These constants define some strings which are used to specify the type of STS instance to publish. This is
    a simple specification used only when consuming the STS-instance-publishing web-service in integration tests.
    These values will be replaced by more a more detailed specification of STS instance details and the associated
    configuration state. They are being kept around for ease of use in integration tests.
     */
    public static final String UNPROTECTED_BINDING = "unprotected_binding";
    public static final String SYMMETRIC_USERNAME_TOKEN_BINDING = "symmetric_username_token_binding";
    public static final String ASYMMETRIC_USERNAME_TOKEN_BINDING = "asymmetric_username_token_binding";
    public static final String TRANSPORT_USERNAME_TOKEN_BINDING = "transport_username_token_binding";
    public static final String SYMMETRIC_ENDORSING_CERT_BINDING = "symmetric_endorsing_cert_binding";

    /*
    Used in conjunction with com.google.inject.Names.named to distinguish e.g. a Provider of the token types for
    issue operations vs. the token types for validate operations. In each case a list<TokenType> is returned, and thus
    we need @Named with one of the values below for disambiguation.
     */
    public static final String TOKEN_ISSUE_OPERATION = "token_issue_operation";
    public static final String TOKEN_VALIDATE_OPERATION_STATUS = "token_validate_operation_status";
    public static final String TOKEN_RENEW_OPERATION = "token_renew_operation";

    /*
    Used in conjunction with a @Named annotation to inject a Map<String, Object> instance necessary for the CXF interceptor-set
     */
    public static final String STS_WEB_SERVICE_PROPERTIES = "sts_web_service_properties";

    /*
    The following values are used by the AMTokenValidator.
     */
    public static final String CONTENT_TYPE = SharedSTSConstants.CONTENT_TYPE;
    public static final String ACCEPT = "Accept";
    public static final String APPLICATION_JSON = SharedSTSConstants.APPLICATION_JSON;
    public static final String COOKIE = "Cookie";
    public static final String POST = "POST";
    public static final String EQUALS = "=";
    public static final String PIPE = "|";
    /*
    This constant defines the name of the OpenAM token, currently used in token validation requests;
     */
    public static final String AM_TOKEN_TYPE = "http://forgerock.org/token/type/OpenAM";

    /*
    The name of the DOM Element and json field used to communicate a OpenAM session identifier in the SecurityToken
     */
    public static final String AM_SESSION_ID_ELEMENT_NAME="openamsessionid";

    /*
    The namespace of the DOM Element used to communicate a OpenAM session identifier in the SecurityToken
     */
    public static final String AM_SESSION_ID_ELEMENT_NAMESPACE="http://forgerock.org/token/type/OpenAM/openamsessionid";

    /*
    Used in conjunction with a @Named annotation to inject the realm string.
     */
    public static final String REALM = "am_realm";

    /*
    Used in conjunction with a @Named annotation to inject the AM rest authN uri element string.
     */
    public static final String REST_AUTHN_URI_ELEMENT = "am_rest_authn";

    /*
    Used in conjunction with a @Named annotation to inject the AM rest logout uri element string.
     */
    public static final String REST_LOGOUT_URI_ELEMENT = "am_rest_logout";

    /*
    Used in conjunction with a @Named annotation to inject the AM rest username from session id uri element string.
     */
    public static final String REST_ID_FROM_SESSION_URI_ELEMENT = "am_rest_id_from_session";

    /*
    Used in conjunction with a @Named annotation to inject the AM rest token generation service uri element string.
     */
    public static final String REST_TOKEN_GENERATION_SERVICE_URI_ELEMENT = "am_rest_token_gen_service";

    /*
    Used in conjunction with a @Named annotation to provide the uri element corresponding to the Rest STS publish
    service. Consumed by the RestSTSInstanceReconstitutionServlet to build up the uri necessary to consume this
    service.
     */
    public static final String REST_STS_PUBLISH_SERVICE_URI_ELEMENT = "am_rest_sts_publish_service";

    /*
    Used in conjunction with a @Named annotation to inject the url string corresponding to the AM deployment.
     */
    public static final String AM_DEPLOYMENT_URL = "am_deployment_url";

    /*
    String passed to Debug.getInstance(String id).
     */
    public static final String SOAP_STS_DEBUG_ID = "am_soap_sts";

    /*
    String passed to Debug.getInstance(String id).
     */
    public static final String REST_STS_DEBUG_ID = "am_rest_sts";

    /*
    The sts.auth.*TokenValidator instances will be configured with state that allows the AuthenticationUriProvider to
    know the module/service/? to be consumed as defined in the com.sun.identity.authentication.AuthContext.IndexType inner-class.
    The AuthContext.IndexType uses old-school enums, based on strings, to define the set of possible auth index types (i.e. the
    types of authentication facilities that can be invoked). These values are passed to the REST authN context, and the strings
    must marshall to the enum values defined in AuthContext.IndexType. I cannot import this class directly, as this would pull
    a dependency on openam-core, so I will re-define the string values here, and reference them where needed in STS code.
     */
    public static final String AUTH_INDEX_TYPE_MODULE = "module_instance";

    /*
    The name of the key identifying the token type in the json blob representing a token
     */
    public static final String TOKEN_TYPE_KEY = "token_type";

    /*
    The json representation of a UsernameToken will have the following key identifying the username.
     */
    public static final String USERNAME_TOKEN_USERNAME = "username";

    /*
    The json representation of a UsernameToken will have the following key identifying the password.
     */
    public static final String USERNAME_TOKEN_PASSWORD = "password";

    /*
    The json representation of a AM Session will have the following key identifying the sessionId.
     */
    public static final String AM_SESSION_TOKEN_SESSION_ID = "session_id";

    /*
    Used in a @Named annotation provided to the TokenTranslateOperationImpl to specify the Map<String,String> instances
    used to define the set of supported token transformations. This set will ultimately come from the user.
     */
    public static final String REST_SUPPORTED_TOKEN_TRANSLATIONS = "rest_supported_token_translations";

    /*
    Used when creating the RestSTSInstanceConfig, to specify for which token transformation operations the interim OpenAM
    session (generated after successful validation), should be invalidated.
     */
    public static final boolean INVALIDATE_INTERIM_OPENAM_SESSION = true;

    /*
    Used to access the headers in restlet ClientResource instances
     */
    public static final String RESTLET_HEADER_KEY = "org.restlet.http.headers";

    /*
    the /json/users/?_action=idFromSession needs the iPDP value in a cookie with a name corresponding to the cookie name
    in the AM deployment.
     */
    public static final String AM_SESSION_COOKIE_NAME = "am_session_cookie_name";

    /*
    set to /json. In composing the authentication, idFromSession, or logout urls, the realm has to be between
     e.g. authenticate and /json.
     */
    public static final String AM_REST_AUTHN_JSON_ROOT = "am_rest_authn_json_root";


    public static final String ROOT_REALM = "/";

    public static final String FORWARD_SLASH = SharedSTSConstants.FORWARD_SLASH;
    /*
    Used for marshalling between byte[] and string representations. Does not seem to be defined anywhere in the pre 1.7 JDK.
     */
    public static final String UTF_8_CHARSET_ID = "UTF-8";

    /*
    Used to identify the json-resource ConnectionFactory which will be bound globally to all Rest STS instances.
     */
    public static final String REST_STS_CONNECTION_FACTORY_NAME = "rest_sts_connection_factory_name";

    /*
    used to identify the key referencing the OIDC ID Token in both the json and xml representation of the OIDC ID Token
     */
    public static final String OPEN_ID_CONNECT_ID_TOKEN_KEY = "oidc_id_token";

    /*
    The namespace of the DOM Element used to communicate an OpenID Connect ID Token. Yes, it is strange to provide an XML
    representation of a token which is only defined in json, but the CXF-STS engine handles only tokens defined in xml.
     */
    public static final String OPEN_ID_CONNECT_ID_TOKEN_ELEMENT_NAMESPACE="http://forgerock.org/token/type/OpenAM/oidc_id_token";

    /*
    When validating OIDC ID Tokens, the OpenAM authN module needs to be configured with the header which will reference
    the Id token. This value needs to be set in the context map associated with the AuthTargetMapping corresponding to
    the token transformation which takes the OIDC token as input.
     */
    public static final String OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY = "oidc_id_token_auth_target_header_key";

    /*
    When validating X509Tokens, the OpenAM Certificate authN module must be configured in portal mode. This involves setting
    the Trusted Remote Hosts value to something other than none, and specifying the header name which will reference the
    client's x509 certificate. This header name must be configured in the AuthTargetMapping
     */
    public static final String X509_TOKEN_AUTH_TARGET_HEADER_KEY = "x509_token_token_auth_target_header_key";
    /*
    This value is used to key the SAML2SubjectConfirmation instance in additionalProperties Map<String, Object> encapsulated
    in the TokenProviderParameters so that the AMSAMLTokenProvider
    can determine the type of subject confirmation specified in the REST invocation, avoiding the WS-Trust secret decoder
    ring of KeyType and OnBehalfOf values to make this determination.
     */
    public static final String SAML2_SUBJECT_CONFIRMATION_KEY = "saml2_subject_confirmation_key";

    /*
    This value is used to key the TokenType instance in additionalProperties Map<String, Object> encapsulated
    in the TokenProviderParameters so that the AMSAMLTokenProvider can use it to determine the AuthnContext passed to the
    TokenGenerationService when issuing SAML2 assertions.
     */
    public static final String VALIDATED_TOKEN_TYPE_KEY = "validated_token_type_key";

    /*
    This value is used to key the ProofTokenState instance in additionalProperties Map<String, Object> encapsulated
    in the TokenProviderParameters so that the AMSAMLTokenProvider can pass it to the TokenGenerationService when issuing
    HolderOfKey assertions.
     */
    public static final String PROOF_TOKEN_STATE_KEY = "proof_token_state_key";

    /*
    This value is used to key the JsonValue instance in additionalProperties Map<String, Object> encapsulated
    in the TokenProviderParameters so that the AMSAMLTokenProvider can pass it to AuthnContextMapper to obtain the
    appropriate AuthnContext for the generated SAML2 assertion.
     */
    public static final String INPUT_TOKEN_STATE_KEY = "input_token_state_key";

    /*
    This property is defined to allow end-users to implement a custom implementation of the
    org.forgerock.openam.sts.rest.token.provider.AuthnContextMapper class. If this property is set, and the specified class
    can be instantiated, and it implements the AuthnContextMapper, then it will be used to map the AuthnContext value
    sent to the TokenGenerationService when issuing SAML2 assertions.
     */
    public static final String CUSTOM_STS_AUTHN_CONTEXT_MAPPER_PROPERTY = "org.forgerock.openam.sts.rest.custom.AuthnContextMapper";

    /*
    Used in a @Named annotation to inject the instance id for the STS, as this is required to make invocations to the
    TokenGenerationService. This state will ultimately be pulled from the STSInstanceConfig.
     */
    public static final String STS_INSTANCE_ID = "sts_instance_id";

    /*
    The JsonValue returned by the TokenGenerationService will have a single entry, keyed by the string below. The value
    will be the generated token (now just a SAML2 assertion).
     */
    public static final String ISSUED_TOKEN = "issued_token";

    /*
    The name of the rest sts service, as defined in restSTS.xml. Referenced in the RestSTSInstanceConfigPersister, to
    write rest sts instance config state to the SMS.
     */
    public static final String REST_STS_SERVICE_NAME = "RestSecurityTokenService";

    public static final String REST_STS_SERVICE_VERSION = "1.0";
    /*
    The name of the json field corresponding to the deployment path of a successfully-published Rest STS instance.
     */
    public static final String SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT = "url_element";

    /*
    The name of the json field in the json rest-sts publish invocation that references the field which allows the
    marshalling logic in the RestSTSPublishServiceRequestHandler to distinguish between programmatic invocations via
    the client stk classes, which will publish with state generated by calling toJson() on an instance of the RestSTSInstanceConfig
    class, and the RestSecurityTokenServiceViewBean, which will publish with state harvested from the ViewBean property
     sheet, and will thus be in the format of Map<String, Set<String>>.
     */
    public static final String REST_STS_PUBLISH_INVOCATION_CONTEXT = SharedSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT;

    /*
    Used  as the value for the REST_STS_PUBLISH_INVOCATION_CONTEXT key for invocations to the rest sts publish service
    issued by the RestSecurityTokenServiceViewBean.
     */
    public static final String REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN = SharedSTSConstants.REST_STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN;

    /*
    Used as the value for the REST_STS_PUBLISH_INVOCATION_CONTEXT key for invocations to the rest sts publish service
    issued by the client sdk (i.e. by calling toJson() on an instance of the RestSTSInstanceConfig class).
     */
    public static final String REST_STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK = "invocation_context_client_sdk";

    /*
    Used as the key to the JsonValue corresponding to a wrapped Map<String, Set<String>> or the output of
    RestSTSInstanceConfig#toJson(), depending upon the invocation context.
     */
    public static final String REST_STS_PUBLISH_INSTANCE_STATE = SharedSTSConstants.REST_STS_PUBLISH_INSTANCE_STATE;

    /**
     * If a rest-sts instance is configured to support a token transformation with an x509 token as an input token type, the
     * instance must be invoked via a two-way TLS exchange (i.e. where the client presents their certificate). If OpenAM
     * is deployed behind a tls-offloading engine, the client certificate won't be set as a HttpServetRequest attribute
     * referenced by the javax.servlet.request.X509Certificate key, but rather the rest sts instance must be configured
     * with the name of the http header where the tls-offloading engine will store the client certificate prior to invoking
     * OpenAM.
     */
    public static final String OFFLOADED_TWO_WAY_TLS_HEADER_KEY = SharedSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY;

    /**
     * If a rest-sts instance is configured to support a token transformation with an x509 token as an input token type, the
     * instance must be invoked via a two-way TLS exchange (i.e. where the client presents their certificate). If OpenAM
     * is deployed behind a tls-offloading engine, the client certificate won't be set as a HttpServetRequest attribute
     * referenced by the javax.servlet.request.X509Certificate key, but rather the rest sts instance must be configured
     * with the name of the http header where the tls-offloading engine will store the client certificate prior to invoking
     * OpenAM. The rest-sts instance will undertake the further check to determine if the ip address invoking the rest-sts
     * corresponds to the set of IP-addresses corresponding to the TLS-offload-engine hosts.
     */
    public static final String TLS_OFFLOAD_ENGINE_HOSTS = SharedSTSConstants.TLS_OFFLOAD_ENGINE_HOSTS;

    /**
     * The name of the CREST header identifying the version of a targeted service.
     */
    public static final String CREST_VERSION_HEADER_KEY = SharedSTSConstants.CREST_VERSION_HEADER_KEY;

    /**
     * Used in context of a @Named annotation to identify the String identifying the version of targeted CREST services.
     */
    public static final String CREST_VERSION_SESSION_SERVICE = "crest_version_session_service";

    /**
     * Used in context of a @Named annotation to identify the String identifying the version of targeted CREST services.
     */
    public static final String CREST_VERSION_AUTHN_SERVICE = "crest_version_authn_service";

    /**
     * Used in context of a @Named annotation to identify the String identifying the version of targeted CREST services.
     */
    public static final String CREST_VERSION_TOKEN_GEN_SERVICE = "crest_version_token_gen_service";

    /**
     * Used in context of a @Named annotation to identify the String identifying the version of targeted CREST services.
     */
    public static final String CREST_VERSION_USERS_SERVICE = "crest_version_users_service";
}
