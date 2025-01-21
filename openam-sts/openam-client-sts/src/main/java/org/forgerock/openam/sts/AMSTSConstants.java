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
 * Copyright 2013-2015 ForgeRock AS. All rights reserved.
 * Portions Copyrighted 2025 3A Systems, LLC.
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
    These constants define the QNames referencing services and ports in wsdl documents which define the semantics and
    SecurityPolicy bindings of published STS instances.
     */
    public static final QName STANDARD_STS_SERVICE_NAME = SharedSTSConstants.STANDARD_STS_SERVICE_QNAME;
    public static final QName STANDARD_STS_PORT_QNAME = SharedSTSConstants.STANDARD_STS_PORT_QNAME;

    /*
    Used in conjunction with com.google.inject.Names.named to distinguish e.g. a Provider of the token types for
    issue operations vs. the token types for validate operations. In each case a list<TokenType> is returned, and thus
    we need @Named with one of the values below for disambiguation.
     */
    public static final String ISSUED_TOKEN_TYPES = "issued_token_types";
    public static final String TOKEN_RENEW_OPERATION = "token_renew_operation";

    /*
    Used in conjunction with a @Named annotation to inject a Map<String, Object> instance necessary for the CXF interceptor-set
     */
    public static final String STS_WEB_SERVICE_PROPERTIES = "sts_web_service_properties";

    /*
    The following values are used by the AMTokenValidator.
     */
    public static final String CONTENT_TYPE = SharedSTSConstants.CONTENT_TYPE;
    public static final String APPLICATION_JSON = SharedSTSConstants.APPLICATION_JSON;
    public static final String COOKIE = "Cookie";
    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final String EQUALS = "=";
    public static final String PIPE = "|";
    /*
    This constant defines the name of the OpenAM token, currently used in token validation requests;
     */
    public static final String AM_TOKEN_TYPE = "http://forgerock.org/token/type/OpenAM";

    /*
    The name of the DOM Element and json field used to communicate a OpenAM session identifier in the SecurityToken.
    This will also be used as the local name in the soap-sts wsdl files which define the Policy for an OpenAMSessionToken
    SecurityPolicy.
     */
    public static final String AM_SESSION_TOKEN_ELEMENT_NAME="OpenAMSessionToken";

    /*
    The namespace of the DOM Element used to communicate a OpenAM session identifier in the SecurityToken.
    This will also be used as the namespace in the soap-sts wsdl files which define the Policy for an OpenAMSessionToken
    SecurityPolicy.
     */
    public static final String AM_SESSION_TOKEN_ELEMENT_NAMESPACE="http://schemas.forgerock.org/ws/securitypolicy";

    /*
    QName of the OpenAMSessionToken - needed in the soap-sts to create custom Assertions and the corresponding PolicyInterceptors.
     */
    public static final QName AM_SESSION_TOKEN_ASSERTION_QNAME = new QName(AM_SESSION_TOKEN_ELEMENT_NAMESPACE, AM_SESSION_TOKEN_ELEMENT_NAME);

    /*
    ValueType in the BinarySecurityToken containing the OpenAMSessionToken
     */
    public static final String AM_SESSION_TOKEN_ASSERTION_BST_VALUE_TYPE = AM_SESSION_TOKEN_ELEMENT_NAMESPACE + "#" + AM_SESSION_TOKEN_ELEMENT_NAME;

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
    Used in conjunction with a @Named annotation to inject the AM create access audit event uri element string.
     */
    public static final String REST_CREATE_ACCESS_AUDIT_EVENT_URI_ELEMENT = "am_rest_create_access_audit_event";

    /*
    Used in conjunction with a @Named annotation to inject the AM create access audit event URL.
     */
    public static final String REST_CREATE_ACCESS_AUDIT_EVENT_URL = "am_rest_create_access_audit_event_url";

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
    Used in conjunction with a @Named annotation to provide the uri element corresponding to the Rest STS publish
    service. Consumed by the PublishServiceConsumerImpl to build up the uri necessary to consume this
    service.
     */
    public static final String SOAP_STS_PUBLISH_SERVICE_URI_ELEMENT = "am_soap_sts_publish_service";

    /*
    Used in conjunction with a @Named annotation to provide the uri element corresponding to the agents profile service.
    Consumed by the SoapSTSAgentConfigAccessImpl to build up the uri necessary to obtain the profile state corresponding
    to the soap-sts agent.
     */
    public static final String AGENTS_PROFILE_SERVICE_URI_ELEMENT = "am_agent_profile_service";

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
    Used in a @Named annotation provided to the TokenTranslateOperationImpl to specify the Set<TokenTransformConfig> instances
    used to define the set of supported token transformations. This set will ultimately come from the user.
     */
    public static final String REST_SUPPORTED_TOKEN_TRANSFORMS = "rest_supported_token_transforms";

    /*
    Used in a @Named annotation provided to the TokenTranslateOperationImpl to specify the Set<CustomTokenOperation> instances
    used to define the set of custom token validators. This set will ultimately come from the user.
     */
    public static final String REST_CUSTOM_TOKEN_VALIDATORS = "rest_custom_token_validators";

    /*
    Used in a @Named annotation provided to the TokenTranslateOperationImpl to specify the Set<CustomTokenOperation> instances
    used to define the set of custom token providers. This set will ultimately come from the user.
     */
    public static final String REST_CUSTOM_TOKEN_PROVIDERS = "rest_custom_token_providers";

    /*
    Used in a @Named annotation provided to the TokenTranslateOperationImpl to specify the Set<TokenTransformConfig> instances
    used to define the set of supported token transformations. This set will ultimately come from the user.
     */
    public static final String REST_CUSTOM_TOKEN_TRANSLATIONS = "rest_custom_token_translations";

    /*
    Used when creating the RestSTSInstanceConfig, to specify for which token transformation operations the interim OpenAM
    session (generated after successful validation), should be invalidated.
     */
    public static final boolean INVALIDATE_INTERIM_OPENAM_SESSION = true;

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

    /*
    Used to constitute the header values POSTed to the rest authN.
     */
    public static final String AM_REST_AUTHN_USERNAME_HEADER = "X-OpenAM-Username";

    /*
    Used to constitute the header values POSTed to the rest authN.
     */
    public static final String AM_REST_AUTHN_PASSWORD_HEADER = "X-OpenAM-Password";

    public static final String ROOT_REALM = "/";

    public static final String FORWARD_SLASH = SharedSTSConstants.FORWARD_SLASH;
    /*
    Used for marshalling between byte[] and string representations. Does not seem to be defined anywhere in the pre 1.7 JDK.
     */
    public static final String UTF_8_CHARSET_ID = "UTF-8";

    /*
    used to identify the key referencing the OIDC ID Token in both the json representation of the OIDC ID Token.
    This value is also used as the local name in the AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE, which is the token type
    indicator set by the cxf sts client, and  used by the SoapOpenIdConnectTokenProvider#canHandleToken to
    indicate that it can issue a token of the specified type.
     */
    public static final String OPEN_ID_CONNECT_ID_TOKEN_KEY = "oidc_id_token";

    /*
    Used by the SAML2TokenState class to identify the json field which will contain the SAML2 assertion when this
    assertion is specified in a rest-sts validate invocation.
     */
    public static final String SAML2_TOKEN_KEY = "saml2_token";

    /*
    The namespace of the DOM Element used to communicate an OpenID Connect ID Token. When the SoapOpenIdConnectTokenProvider
    issues an OIDC token, it can only be set as an xml element in the TokenProviderResponse. This constant defines the
    namespace of the xml element.
     */
    public static final String OPEN_ID_CONNECT_ID_TOKEN_ELEMENT_NAMESPACE="http://forgerock.org/OpenAM/tokens";

    /*
    The identifier of an OpenIdConnect token, as specified in the CXF STS client, as the specification of the
    desired token type. SoapOpenIdConnectTokenProvider#canHandleToken will check for this constant to deterime if it can
    handle the issue token request. It is also used as the ValueType in the BinarySecurityToken which will wrap the
    OpenIdConnectToken returned in the TokenProviderResponse returned from SoapOpenIdConnectTokenProvider#createToken.
    */
    public static final String AM_OPEN_ID_CONNECT_TOKEN_ASSERTION_TYPE = OPEN_ID_CONNECT_ID_TOKEN_ELEMENT_NAMESPACE + "#" + OPEN_ID_CONNECT_ID_TOKEN_KEY;

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
    public static final String X509_TOKEN_AUTH_TARGET_HEADER_KEY = "x509_token_auth_target_header_key";

    /*
    Used in a @Named annotation to inject the instance id for the STS, as this is required to make invocations to the
    TokenGenerationService. This state will ultimately be pulled from the STSInstanceConfig.
     */
    public static final String STS_INSTANCE_ID = "sts_instance_id";

    /*
    The JsonValue returned by the TokenGenerationService and from rest-sts token translate invocations,
    will have a single entry, keyed by the string below. The value will be the generated token.
     */
    public static final String ISSUED_TOKEN = "issued_token";

    /*
    The JsonValue returned from the rest-sts token validation invocation will have a single entry, keyed by the
    string below. The value will be true or false.
     */
    public static final String TOKEN_VALID = "token_valid";

    /*
    The name of the rest sts service, as defined in restSTS.xml. Referenced in the RestSTSInstanceConfigStore, to
    write rest sts instance config state to the SMS.
     */
    public static final String REST_STS_SERVICE_NAME = "RestSecurityTokenService";

    /*
    The name of the rest sts service, as defined in soapSTS.xml. Referenced in the SoapSTSInstanceConfigStore, to
    write rest sts instance config state to the SMS.
     */
    public static final String SOAP_STS_SERVICE_NAME = "SoapSecurityTokenService";

    /*
    Corresponds to the version specified in restSTS.xml. Used to register ServiceListeners.
     */
    public static final String REST_STS_SERVICE_VERSION = "1.0";

    /*
    Corresponds to the version specified in soapSTS.xml. Used to register ServiceListeners.
     */
    public static final String SOAP_STS_SERVICE_VERSION = "1.0";

    /*
    The name of the json field corresponding to the deployment path of a successfully-published Rest STS instance.
     */
    public static final String SUCCESSFUL_REST_STS_PUBLISH_URL_ELEMENT = "url_element";

    /*
    The name of the json field in the json rest-sts publish invocation that references the field which allows the
    marshalling logic in the {Rest|Soap}STSPublishServiceRequestHandler to distinguish between programmatic invocations via
    the client stk classes, which will publish with state generated by calling toJson() on an instance of the {Rest|Soap}STSInstanceConfig
    class, and the {Rest|Soap}SecurityTokenServiceViewBean, which will publish with state harvested from the ViewBean property
     sheet, and will thus be in the format of Map<String, Set<String>>.
     */
    public static final String STS_PUBLISH_INVOCATION_CONTEXT = SharedSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT;

    /*
    Used  as the value for the STS_PUBLISH_INVOCATION_CONTEXT key for invocations to the rest sts publish service
    issued by the RestSecurityTokenServiceViewBean.
     */
    public static final String STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN = SharedSTSConstants.STS_PUBLISH_INVOCATION_CONTEXT_VIEW_BEAN;

    /*
    Used as the value for the STS_PUBLISH_INVOCATION_CONTEXT key for invocations to the rest sts publish service
    issued by the client sdk (i.e. by calling toJson() on an instance of the RestSTSInstanceConfig class).
     */
    public static final String STS_PUBLISH_INVOCATION_CONTEXT_CLIENT_SDK = "invocation_context_client_sdk";

    /*
    Used as the key to the JsonValue corresponding to a wrapped Map<String, Set<String>> or the output of
    RestSTSInstanceConfig#toJson(), depending upon the invocation context.
     */
    public static final String STS_PUBLISH_INSTANCE_STATE = SharedSTSConstants.STS_PUBLISH_INSTANCE_STATE;

    /**
     * If a rest-sts instance is configured to support a token transformation with an x509 token as an input token type, the
     * instance must be invoked via a two-way TLS exchange (i.e. where the client presents their certificate). If OpenAM
     * is deployed behind a tls-offloading engine, the client certificate won't be set as a HttpServetRequest attribute
     * referenced by the jakarta.servlet.request.X509Certificate key, but rather the rest sts instance must be configured
     * with the name of the http header where the tls-offloading engine will store the client certificate prior to invoking
     * OpenAM.
     */
    public static final String OFFLOADED_TWO_WAY_TLS_HEADER_KEY = SharedSTSConstants.OFFLOADED_TWO_WAY_TLS_HEADER_KEY;

    /**
     * If a rest-sts instance is configured to support a token transformation with an x509 token as an input token type, the
     * instance must be invoked via a two-way TLS exchange (i.e. where the client presents their certificate). If OpenAM
     * is deployed behind a tls-offloading engine, the client certificate won't be set as a HttpServetRequest attribute
     * referenced by the jakarta.servlet.request.X509Certificate key, but rather the rest sts instance must be configured
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

    /**
     * Used in context of a @Named annotation to identify the String identifying the version of targeted CREST services.
     */
    public static final String CREST_VERSION_SOAP_STS_PUBLISH_SERVICE = "crest_version_soap_sts_publish_service";

    /**
     * Used in context of a @Named annotation to identify the String identifying the version of targeted CREST services.
     */
    public static final String CREST_VERSION_AGENTS_PROFILE_SERVICE = "crest_version_agents_profile_service";

    /**
     * Used in context of a @Named annotation to identify the String identifying the version of targeted CREST services.
     */
    public static final String CREST_VERSION_AUDIT_SERVICE = "crest_version_audit_service";

    /**
     * Used in context of a @Named annotation to specify the Set of TokenTypes for which token validators are plugged-into
     * the IssueOperation to support ActAs and/or OnBehalfOf tokens.\
     */
    public static final String DELEGATED_TOKEN_VALIDATORS = "delegated_token_validators";

    /**
     * For soap-sts instance which support token delegation (ActAs/OnBehalfOf elements), users have the option to publish
     * a soap-sts instance which specifies custom implementations of org.apache.cxf.sts.token.delegation.TokenDelegationHandler.
     * If only such implementations are specified to validate the delegated token, this handler has to set
     * the OpenAM session id corresponding to this delegated token in the additionalProperties map in the
     * org.apache.cxf.sts.token.delegation.TokenDelegationResponse keyed by the string below. If this is not done,
     * the token issue operation will fail, as the principal asserted by the token generation service is based upon the
     * OpenAM session passed in the TGS invocation.
     */
    public static final String CUSTOM_DELEGATION_HANDLER_AM_SESSION_ID = "custom_delegation_handler_am_session_id";

    /**
     * For soap-sts instance which support token delegation (ActAs/OnBehalfOf elements), users have the option to publish
     * a soap-sts instance which specifies custom implementations of org.apache.cxf.sts.token.delegation.TokenDelegationHandler.
     * If only such implementations are specified to validate the delegated token, this handler has to set
     * the OpenAM session id corresponding to this delegated token in the additionalProperties map in the
     * org.apache.cxf.sts.token.delegation.TokenDelegationResponse. In addition, the custom TokenDelegationHandlers can
     * determine whether the interim OpenAM session, generated by the validation of the delegated token type, should be
     * invalidated following the generation of the to-be-issued token. By default, this interim OpenAM session will be
     * invalidated, to prevent the in-memory session table from growing (for stateless sessions). If the custom TokenDelegationHandler
     * wishes to over-ride this default behavior, they can set a Boolean corresponding to the key below in the additionalProperties
     * in the TokenDelegationResponse.
     */
    public static final String CUSTOM_DELEGATION_HANDLER_INVALIDATE_AM_SESSION = "custom_delegation_handler_invalidate_am_session";

    /**
     * Used in the context of a @Named annotation to indicate whether the sts is configured to persist issued tokens in the
     * CTS. Used to determine whether the Validation and Cancellation of sts-issued tokens will be supported, as in the 13
     * release, this support simply reads/mutates CTS token state.
     */
    public static final String ISSUED_TOKENS_PERSISTED_IN_CTS = "issued_tokens_persisted_in_cts";
}
