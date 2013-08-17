/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 ForgeRock AS All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions copyright [year] [name of copyright owner]"
 */

package org.forgerock.openam.oauth2.provider;

import java.util.Set;

/**
 * Gets the setting of the OAuth2 Provider
 */
public interface OAuth2ProviderSettings {

    /**
     * Gets the seconds that an Authorization Code is valid for
     * @return a number in seconds
     */
    public long getAuthorizationCodeLifetime();

    /**
     * Gets the seconds that an Refresh Token is valid for
     * @return a number in seconds
     */
    public long getRefreshTokenLifetime();

    /**
     * Gets the seconds that an Access Token is valid for
     * @return a number in seconds
     */
    public long getAccessTokenLifetime();

    /**
     * Gets the seconds that an Access Token is valid for
     * @return a number in seconds
     */
    public long getJWTTokenLifetime();

    /**
     * Gets whether or not refresh tokens are enabled
     * @return true - issue refresh tokens, false - don't issue refresh tokens
     */
    public boolean getRefreshTokensEnabledState();

    /**
     * Gets the class that implements OAuth2 Scope
     * @return The package and name of the scope class
     */
    public String getScopeImplementationClass();

    /**
     * Gets the set of response types and their implementation classes.
     * Each string is returned in the format "response_type_value|implementation_class"
     * @return A set of strings representing the response types available.
     */
    public Set<String> getResponseTypes();

    /**
     * Gets a list of OpenAM attributes used to authenticate the resource owner.
     * @return Set of strings used to authenticate the resource owner.
     */
    public Set<String> getListOfAttributesTheResourceOwnerIsAuthenticatedOn();

    /**
     * Gets the OpenAM attribute name to store the list of shared consent values.
     * @return A string that is the attribute to store and retrieve shared consent from.
     */
    public String getSharedConsentAttributeName();

    /**
     * Gets the OpenAM Authorization endpoint
     * @return A string representing the url of the authorization endpoint
     */
    public String getAuthorizationEndpoint();

    /**
     * Gets the OpenAM OAuth 2 Token endpoint
     * @return A string representing the url of the token endpoint.
     */
    public String getTokenEndpoint();

    /**
     * Gets the url of the UserInfo endpoint
     * @return A string representing the url of the user info endpoint.
     */
    public String getUserInfoEndpoint();

    /**
     * Gets the set of scopes the OAuth2 Provider Supports
     * @return
     */
    //public Set<String> getScopesSupported();

    /**
     * Below Is OpenID Connect Settings
     */

    /**
     * Gets the OpenID connect version
     * @return String that is the version number implemented
     */
    public String getOpenIDConnectVersion();

    /**
     * Gets the http url of the OpenAM instance that is the OpenID Provider.
     * @return A String representing the url of the OpenID connect provider.
     */
    public String getOpenIDConnectIssuer();

    /**
     * URL of the endpoint to check the state of a user session using an id_token
     * @return A String representing the url of the check session endpoint
     */
    public String getCheckSessionEndpoint();

    /**
     * Gets the url of the endpoint to end a user session using an id_token
     * @return A String representing the url of the end session endpoint
     */
    public String getEndSessionEndPoint();

    /**
     * Gets the URL where the Json Web Key for the OpenID Connect Provider can be found.
     * @return A string representing the URL to get the OpenID Connect Provider Json Web Key.
     */
    public String getJWKSUri();

    /**
     * Gets the URL of the endpoint to register an OAuth2/OpenID Connect client
     * @return A string representing the URL of the client registration endpoint.
     */
    public String getClientRegistrationEndpoint();

    /**
     * Gets the list of subject types supported
     * @return A Set of Strings representing the subject types supported
     */
    public Set<String> getSubjectTypesSupported();

    /**
     * Gets the set of algorithms that can be used to sign the id_token
     * @return A Set of strings representing the algorithms that can be used to sign id_tokens.
     */
    public Set<String> getTheIDTokenSigningAlgorithmsSupported();

    /**
     * Gets the list of claims supported for the userinfo endpoint.
     * @return A set of strings representing the claims supported for the userinfo endpoint.
     */
    public Set<String> getSupportedClaims();

    /**
     * Gets the name of the key used to sign the JWT tokens.
     * @return The name of the key in the keystore.
     */
    public String getKeyStoreAlias();

    /* Optional Values
       userinfo_signing_alg_values_supported
        OPTIONAL. JSON array containing a list of the JWS [JWS] signing algorithms (alg values) [JWA] supported by the
        UserInfo Endpoint to encode the Claims in a JWT [JWT].
       userinfo_encryption_alg_values_supported
        OPTIONAL. JSON array containing a list of the JWE [JWE] encryption algorithms (alg values) [JWA] supported by the
        UserInfo Endpoint to encode the Claims in a JWT [JWT].
       userinfo_encryption_enc_values_supported
        OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) [JWA] supported by the
        UserInfo Endpoint to encode the Claims in a JWT [JWT].
       acr_values_supported
        OPTIONAL. JSON array containing a list of the Authentication Context Class References that this server supports.
       id_token_encryption_alg_values_supported
        OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values) supported by the
        Authorization Server for the ID Token to encode the Claims in a JWT [JWT].
       id_token_encryption_enc_values_supported
        OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) supported by the
        Authorization Server for the ID Token to encode the Claims in a JWT [JWT].
       request_object_signing_alg_values_supported
        OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported by the Authorization
        Server for the Request Object described in Section 2.9 of OpenID Connect Messages 1.0 [OpenID.Messages].
        These algorithms are used both when the Request Object is passed by value (using the request parameter) and
        when it is passed by reference (using the request_uri parameter). Servers SHOULD support none and RS256.
       request_object_encryption_alg_values_supported
        OPTIONAL. JSON array containing a list of the JWE encryption algorithms (alg values) supported by the
        Authorization Server for the Request Object described in Section 2.9 of OpenID Connect Messages 1.0
        [OpenID.Messages]. These algorithms are used both when the Request Object is passed by value and when
        it is passed by reference.
       request_object_encryption_enc_values_supported
        OPTIONAL. JSON array containing a list of the JWE encryption algorithms (enc values) supported by the
        Authorization Server for the Request Object described in Section 2.9 of OpenID Connect Messages 1.0
        [OpenID.Messages]. These algorithms are used both when the Request Object is passed by value and when it
        is passed by reference.
       token_endpoint_auth_methods_supported
        OPTIONAL. JSON array containing a list of authentication methods supported by this Token Endpoint. The options
        are client_secret_post, client_secret_basic, client_secret_jwt, and private_key_jwt, as described in Section
        2.2.1 of OpenID Connect Messages 1.0 [OpenID.Messages]. Other authentication methods may be defined by
        extensions. If omitted, the default is client_secret_basic -- the HTTP Basic Authentication Scheme as
        specified in Section 2.3.1 of OAuth 2.0 [RFC6749].
       token_endpoint_auth_signing_alg_values_supported
        OPTIONAL. JSON array containing a list of the JWS signing algorithms (alg values) supported by the Token
        Endpoint for the private_key_jwt and client_secret_jwt methods to encode the JWT [JWT]. Servers SHOULD
        support RS256.
       display_values_supported
        OPTIONAL. JSON array containing a list of the display parameter values that the OpenID Provider supports.
         These values are described in Section 2.1.1 of OpenID Connect Messages 1.0 [OpenID.Messages].
       claim_types_supported
        OPTIONAL. JSON array containing a list of the Claim Types that the OpenID Provider supports. These Claim
        Types are described in Section 2.6 of OpenID Connect Messages 1.0 [OpenID.Messages]. Values defined by this
        specification are normal, aggregated, and distributed. If not specified, the implementation supports only
        normal Claims.
       service_documentation
        OPTIONAL. URL of a page containing human-readable information that developers might want or need to know when
        using the OpenID Provider. In particular, if the OpenID Provider does not support Dynamic Client Registration,
        then information on how to register Clients should be provided in this documentation.
       claims_locales_supported
        OPTIONAL. Languages and scripts supported for values in Claims being returned, represented as a JSON array of
        BCP47 [RFC5646] language tag values. Not all languages and scripts may be supported for all Claim values.
       ui_locales_supported
        OPTIONAL. Languages and scripts supported for the user interface, represented as a JSON array of BCP47 [RFC5646]
        language tag values.
       claims_parameter_supported
        OPTIONAL. Boolean value specifying whether the OP supports use of the claims parameter, with true indicating
        support. If omitted, the default value is false.
       request_parameter_supported
        OPTIONAL. Boolean value specifying whether the OP supports use of the request parameter, with true indicating
        support. If omitted, the default value is false.
       request_uri_parameter_supported
        OPTIONAL. Boolean value specifying whether the OP supports use of the request_uri parameter, with true
        indicating support. If omitted, the default value is true.
       require_request_uri_registration
        OPTIONAL. Boolean value specifying whether the OP requires any request_uri values used to be pre-registered
        using the request_uris registration parameter. Pre-registration is REQUIRED when the value is true.
       op_policy_uri
        OPTIONAL. URL that the OpenID Provider provides to the person registering the Client to read about the OP's
        requirements on how the Relying Party may use the data provided by the OP. The registration process SHOULD
        display this URL to the person registering the Client if it is given.
       op_tos_uri
        OPTIONAL. URL that the OpenID Provider provides to the person registering the Client to read about OpenID
        Provider's terms of service. The registration process SHOULD display this URL to the person registering the
        Client if it is given.
     */
}
