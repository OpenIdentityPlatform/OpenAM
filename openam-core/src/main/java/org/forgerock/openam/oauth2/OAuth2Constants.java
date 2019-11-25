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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2018 Open Source Solution Technology Corporation
 */

package org.forgerock.openam.oauth2;

/**
 * interface, storage, or both
 */
public class OAuth2Constants {

    public enum EndpointType {
        /**
         * Authorization endpoint - used to obtain authorization from the
         * resource owner via user-agent redirection.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-3.1">3.1.
         *      Authorization Endpoint</a>
         */
        AUTHORIZATION_ENDPOINT("/authorize"),
        /**
         * Token endpoint - used to exchange an authorization grant for an
         * access token, typically with client authentication.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-3.2">3.2.
         *      Token Endpoint</a>
         */
        TOKEN_ENDPOINT("/access_token"),
        /**
         * Device Authorization Endpoint (OAuth 2.0 Device Flow)
         * - The authorization server's endpoint capable of issuing device
         *   verification codes, user codes, and verification URLs.
         */
        DEVICE_AUTHORIZATION_ENDPOINT("/device/code"),
        /**
         * End-user verification URI (OAuth 2.0 Device Flow)
         * - The end-user verification URI on the authorization server.
         */
        END_USER_VERIFICATION_URI("/device/user"),
        /**
         * Extension grant types MAY define additional endpoints as needed.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-3">3.
         *      Protocol Endpoints</a>
         */
        OTHER("");

        private final String path;

        /**
         * Constructor.
         * @param path The resource path.
         */
        private EndpointType(String path) {
            this.path = path;
        }

        /**
         * Get the resource path.
         * @return The resource path.
         */
        public String getPath() {
            return path;
        }

        /**
         * Get EndpointType from the resource path.
         * @param path The resource path.
         * @return EndpointType
         */
        public static EndpointType get(String path) {
            for (EndpointType type : values()) {
                if (type.getPath().equals(path)) {
                    return type;
                }
            }
            return null;
        }
    }

    /*
     * public static final Set<String> params;
     * 
     * static { Set<String> paramSet = new HashSet<String>();
     * paramSet.add(Params.ACCESS_TOKEN); paramSet.add(Params.CLIENT_ID);
     * paramSet.add(Params.CLIENT_SECRET); paramSet.add(Params.CODE);
     * paramSet.add(Params.ERROR); paramSet.add(Params.ERROR_DESCRIPTION);
     * paramSet.add(Params.ERROR_URI); paramSet.add(Params.EXPIRES_IN);
     * paramSet.add(Params.GRANT_TYPE); paramSet.add(Params.PASSWORD);
     * paramSet.add(Params.REDIRECT_URI); paramSet.add(Params.REFRESH_TOKEN);
     * paramSet.add(Params.RESPONSE_TYPE); paramSet.add(Params.SCOPE);
     * paramSet.add(Params.STATE); paramSet.add(Params.TOKEN_TYPE);
     * paramSet.add(Params.USERNAME); params =
     * Collections.unmodifiableSet(paramSet); }
     */

    /**
     * The OAuth Parameters Registry's initial contents.
     */
    public static class Params {

        /** Parameter usage location: authorization request, token request. */
        public static final String CLIENT_ID = "client_id";

        public static final String ID = "id";

        /** Parameter usage location: token request. */
        public static final String CLIENT_SECRET = "client_secret";

        /** Parameter usage location: authorization request. */
        public static final String RESPONSE_TYPE = "response_type";

        /** Parameter usage location: authorization request. */
        public static final String MAX_AGE = "max_age";

        /** Delimiter that separates the response_type values. */
        public static final String RESPONSE_TYPE_DELIMITER = " ";

        /** Parameter usage location: authorization request, token request. */
        public static final String REDIRECT_URI = "redirect_uri";

        /** Parameter usage location: authorization request, authorization. */
        public static final String SCOPE = "scope";

        /** Parameter usage location: authorization request, authorization. */
        public static final String STATE = "state";

        /** Parameter usage location: authorization response, token request. */
        public static final String CODE = "code";

        /** Parameter usage location: token response, userinfo response. */
        public static final String VALUE = "value";

        /** Parameter usage location: token response, userinfo response. */
        public static final String VALUES = "values";

        /** Parameter usage location: authorization response, token response. */
        public static final String ERROR = "error";

        /** Parameter usage location: authorization response, token response. */
        public static final String ERROR_DESCRIPTION = "error_description";

        /** Parameter usage location: authorization response, token response. */
        public static final String ERROR_URI = "error_uri";

        /** Parameter usage location: token request. */
        public static final String GRANT_TYPE = "grant_type";

        /** Parameter usage location: authorization response, token response, user info form. */
        public static final String ACCESS_TOKEN = "access_token";

        /** Parameter usage locationon: authorization response, token response. */
        public static final String TOKEN_TYPE = "token_type";

        /** Parameter usage location: authorization response, token response. */
        public static final String EXPIRES_IN = "expires_in";

        /** Parameter usage location: token request. */
        public static final String USERNAME = "username";

        /** Parameter usage location: token request. */
        public static final String PASSWORD = "password";

        /** Parameter usage location: token request, token response. */
        public static final String REFRESH_TOKEN = "refresh_token";

        /** Parameter usage location: token request. */
        public static final String REALM = "realm";

        /** Parameter usage location: OpenId Connect request, as value in scope. */
        public static final String OPENID = "openid";

        /**
         * Parameter usage location: OpenID Connect authentication request parameter. Used to specify Authentication
         * Context Class Reference (ACR) values. These represent requested Level of Assurance (LoA), which is similar
         * in concept to AuthLevel, but is may be mapped to any auth type (auth level, auth chain, module, etc).
         *
         * @see <a href="http://openid.net/specs/openid-connect-core-1_0.html#AuthRequest">3.1.2.1 Authentication Request</a>
         */
        public static final String ACR_VALUES = "acr_values";
        
        /** Parameter usage location: OpenId Connect request. */
        public static final String LOGIN_HINT = "login_hint";

        /** Parameter usage location: OpenId Connect End Session request. */
        public static final String END_SESSION_ID_TOKEN_HINT = "id_token_hint";

        /** Parameter usage location: OpenId Connect End Session request. */
        public static final String POST_LOGOUT_REDIRECT_URI = "post_logout_redirect_uri";

        /** Parameter usage location: Specify the authentication chain to use. */
        public static final String AUTH_CHAIN = "auth_chain";
    }

    public static class CoreTokenParams{
        public static final String EXPIRE_TIME = "expireTime";
        public static final String SCOPE = "scope";
        public static final String PARENT = "parent";
        public static final String USERNAME= "userName";
        public static final String REDIRECT_URI = "redirectURI";
        public static final String REFRESH_TOKEN = "refreshToken";
        public static final String ISSUED = "issued";
        public static final String TOKEN_TYPE = "tokenType";
        public static final String REALM = "realm";
        public static final String ID = "id";
        public static final String CLIENT_ID = "clientID";
        public static final String TOKEN_NAME = "tokenName";
        public static final String AUTH_MODULES = "authModules";
        public static final String AUDIT_TRACKING_ID = "auditTrackingId";
        public static final String OAUTH_TOKEN_ADAPTER = "oauthTokenAdapter";
        public static final String RESOURCE_SET_TOKEN_ADAPTER = "resourceSetTokenAdapter";
        public static final String AUTH_GRANT_ID = "authGrantId";
        public static final String AUTH_TIME = "auth_time";
        public static final String CONFIRMATION_KEY = "confirmationKey";
    }

    public static class Token {
        public static final String OAUTH_ACCESS_TOKEN = "access_token";
        public static final String OAUTH_EXPIRES_IN = "expires_in";
        public static final String OAUTH_REFRESH_TOKEN = "refresh_token";
        public static final String OAUTH_TOKEN_TYPE = "token_type";
        public static final String OAUTH_CODE_TYPE = "access_code";
    }

    public static class StoredToken {
        public static final String EXPIRY_TIME = "expiry_time";
        public static final String EXPIRYTIME = "expirytime";
        public static final String ISSUED = "issued";
        public static final String PARENT = "parent";
        public static final String TYPE = "type";
    }

    public static class UserinfoEndpoint {
        public static final String USERINFO = "userinfo";
        public static final String USERINFO_SIGNED_RESPONSE_ALG = "userinfo_signed_response_alg";
        public static final String USERINFO_ENCRYPTED_RESPONSE_ALG = "userinfo_encrypted_response_alg";
        public static final String USERINFO_ENCRYPTED_RESPONSE_ENC = "userinfo_encrypted_response_enc";
    }

    /**
     * 11.3.2. Initial Registry Contents
     *
     * @see <a href="">11.3. The OAuth Authorization Endpoint Response Type
     *      Registry</a>
     */
    public static class AuthorizationEndpoint {
        /**
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.1.1">4.1.1.
         *      Authorization Request</a>
         */
        public static final String CODE = "code";
        /**
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.2.1">4.2.1.
         *      Authorization Request</a>
         */
        public static final String TOKEN = "token";

        public static final String ID_TOKEN = "id_token";

    }

    /**
     * grant_type Registry
     */
    public static class TokenEndpoint {
        /**
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.1.3">4.1.3.
         *      Access Token Request</a>
         */
        public static final String AUTHORIZATION_CODE = "authorization_code";
        /**
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.3.2">4.3.2.
         *      Access Token Request</a>
         */
        public static final String PASSWORD = "password";
        /**
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-4.4.2">4.4.2.
         *      Access Token Request</a>
         */
        public static final String CLIENT_CREDENTIALS = "client_credentials";
        /**
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-6">6.
         *      Refreshing an Access Token</a>
         */
        public static final String REFRESH_TOKEN = "refresh_token";
        /**
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-saml2-bearer">2.1.
         *      Using SAML20BearerServerResource Assertions as Authorization
         *      Grants</a>
         */
        public static final String SAML2_BEARER = "urn:ietf:params:oauth:grant-type:saml2-bearer";

        public static final String JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";

        public static final String DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code";
    }

    /**
     * @see <a href="http://tools.ietf.org/html/draft-ietf-oauth-introspection-04">Token Introspection standard</a>.
     */
    public static class IntrospectionEndpoint {
        public static final String TOKEN = "token";
        public static final String TOKEN_TYPE_HINT = "token_type_hint";
        public static final String ACCESS_TOKEN_TYPE = "access_token";
        public static final String REFRESH_TOKEN_TYPE = "refresh_token";
        public static final String RPT_TYPE = "requesting_party_token";
        public static final String USER_ID = "user_id";
        public static final String TOKEN_TYPE = "token_type";
        public static final String ACTIVE = "active";
    }

    /**
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-24#section-5.2.1">5.2.1.
     *      The "Bearer" Authentication Scheme</a>
     */
    public static class Bearer {
        /**
         * 5.2.1. The "Bearer" Authentication Scheme
         * <p/>
         * Authentication Scheme Name:
         */
        public static final String BEARER = "Bearer";
    }

    /**
     * SAML 2.0 Bearer Assertion Profiles for OAuth 2.0
     *
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-saml2-bearer">SAML
     *      2.0 Bearer Assertion Profiles for OAuth 2.0</a>
     */
    public static class SAML20 {
        /**
         * The value of the "client_assertion" parameter MUST contain a single
         * SAML 2.0 Assertion. The SAML Assertion XML data MUST be encoded using
         * base64url
         */
        public static final String CLIENT_ASSERTION = "client_assertion";
        public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";
        public static final String GRANT_TYPE_URI = "urn:ietf:params:oauth:grant-type:saml2-bearer";
        public static final String CLIENT_ASSERTION_TYPE_URI =
                "urn:ietf:params:oauth:client-assertion-type:saml2-bearer";

        /**
         * The value of the "assertion" parameter MUST contain a single SAML 2.0
         * Assertion. The SAML Assertion XML data MUST be encoded using
         * base64url
         */
        public static final String ASSERTION = "assertion";

        public static final String SUBJECT_CONFIRMATION_METHOD = "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    }

    /**
     * @see <a
     *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-http-mac"></a>
     */
    public static class MAC {

        /**
         * Mac
         * <p/>
         *
         * <pre>
         *  Authorization: MAC id="h480djs93hd8",
         *      ts="1336363200",
         *      nonce="dj83hs9s",
         *      mac="bhCQXTVyfj5cmA9uKkPFx1zeOXM="
         * </pre>
         */
        public static final String MAC = "MAC";

        /**
         * REQUIRED. The MAC key identifier.
         */
        public static final String ID = "id";
        /**
         * REQUIRED. The request timestamp. The value MUST be a positive integer
         * set by the client when making each request to the number of seconds
         * elapsed from a fixed point in time (e.g. January 1, 1970 00:00:00
         * GMT). The value MUST NOT include leading zeros (e.g. "000273154346").
         */
        public static final String TS = "ts";
        /**
         * REQUIRED. A unique string generated by the client. The value MUST be
         * unique across all requests with the same timestamp and MAC key
         * identifier combination.
         */
        public static final String NONCE = "nonce";
        /**
         * OPTIONAL. A string used to include additional information which is
         * covered by the request MAC. The content and format of the string is
         * beyond the scope of this specification.
         */
        public static final String EXT = "ext";
        /**
         * REQUIRED. The HTTP request MAC as described in <a href=
         * "http://tools.ietf.org/html/draft-ietf-oauth-v2-http-mac-01#section-3.2"
         * >Section 3.2</a>
         */
        public static final String MAC_PARAMETER = "mac";

        /*
         * 8.1.1. Registration Template
         * 
         * Algorithm name: The name requested (e.g., "example"). Body hash
         * algorithm: The corresponding algorithm used to calculate the payload
         * body hash. Change controller: For standards-track RFCs, state "IETF".
         * For others, give the name of the responsible party. Other details
         * (e.g., postal address, e-mail address, home page URI) may also be
         * included. Specification document(s): Reference to document that
         * specifies the algorithm, preferably including a URI that can be used
         * to retrieve a copy of the document. An indication of the relevant
         * sections may also be included, but is not required.
         * 
         * 8.1.2. Initial Registry Contents
         * 
         * The HTTP MAC authentication scheme algorithm registry's initial
         * contents are:
         * 
         * 
         * 
         * Hammer-Lahav, et al. Expires November 12, 2011 [Page 22]
         * 
         * Internet-Draft MAC Authentication May 2011
         * 
         * 
         * o Algorithm name: hmac-sha-1 o Body hash algorithm: sha-1 o Change
         * controller: IETF o Specification document(s): [[ this document ]]
         * 
         * o Algorithm name: hmac-sha-256 o Body hash algorithm: sha-256 o
         * Change controller: IETF o Specification document(s): [[ this document
         * ]]
         * 
         * 8.2. OAuth Access Token Type Registration
         * 
         * This specification registers the following access token type in the
         * OAuth Access Token Type Registry.
         * 
         * 8.2.1. The "mac" OAuth Access Token Type
         * 
         * Type name: mac Additional Token Endpoint Response Parameters: secret,
         * algorithm HTTP Authentication Scheme(s): MAC Change controller: IETF
         * Specification document(s): [[ this document ]]
         * 
         * 8.3. OAuth Parameters Registration
         * 
         * This specification registers the following parameters in the OAuth
         * Parameters Registry established by [I-D.ietf-oauth-v2].
         * 
         * 8.3.1. The "mac_key" OAuth Parameter
         * 
         * Parameter name: mac_key Parameter usage location: authorization
         * response, token response Change controller: IETF Specification
         * document(s): [[ this document ]] Related information: None
         * 
         * 8.3.2. The "mac_algorithm" OAuth Parameter
         * 
         * Parameter name: mac_algorithm
         * 
         * 
         * 
         * 
         * 
         * 
         * 
         * Hammer-Lahav, et al. Expires November 12, 2011 [Page 23]
         * 
         * Internet-Draft MAC Authentication May 2011
         * 
         * 
         * Parameter usage location: authorization response, token response
         * Change controller: IETF Specification document(s): [[ this document
         * ]] Related information: None
         */
    }

    /**
     * Constants for the OAuth2 Jwt Bearer extension specification.
     *
     * @see <a href="http://self-issued.info/docs/draft-ietf-oauth-jwt-bearer.html">OAuth2 Jwt Bearer</a>
     */
    public static class JwtProfile {

        /** The parameter name for the client assertion type. */
        public static final String CLIENT_ASSERTION_TYPE = "client_assertion_type";

        /** The parameter name for the client assertion. */
        public static final String CLIENT_ASSERTION = "client_assertion";

        /** The parameter value for the JWT Bearer client assertion type. */
        public static final String JWT_PROFILE_CLIENT_ASSERTION_TYPE
                = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    }

    /**
     * See <a
     * href="http://tools.ietf.org/html/draft-ietf-oauth-v2-25#section-11.4"
     * >11.4. The OAuth Extensions Error Registry</a>
     */
    public static class Error {
        /**
         * A single error code
         */
        public static final String ERROR = "error";

        /**
         * OPTIONAL. A human-readable UTF-8 encoded text providing additional
         * information, used to assist the client developer in understanding the
         * error that occurred.
         */
        public static final String ERROR_DESCRIPTION = "error_description";

        /**
         * OPTIONAL. A URI identifying a human-readable web page with
         * information about the error, used to provide the client developer
         * with additional information about the error.
         */
        public static final String ERROR_URI = "error_uri";

        /**
         * The request is missing a required parameter, includes an invalid
         * parameter value, or is otherwise malformed.
         */
        public static final String INVALID_REQUEST = "invalid_request";

        /**
         * The request is using the incorrect method.
         */
        public static final String METHOD_NOT_ALLOWED = "method_not_allowed";

        /**
         * The client is not authorized to request an access token using this
         * method.
         */
        public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";

        /**
         * The resource owner or authorization server denied the request.
         */
        public static final String ACCESS_DENIED = "access_denied";

        /**
         * The authorization server does not support obtaining an access token
         * using this method.
         */
        public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";

        /**
         * The requested scope is invalid, unknown, or malformed.
         */
        public static final String INVALID_SCOPE = "invalid_scope";

        /**
         * The authorization server encountered an unexpected condition which
         * prevented it from fulfilling the request.
         */
        public static final String SERVER_ERROR = "server_error";

        /**
         * The authorization server is currently unable to handle the request
         * due to a temporary overloading or maintenance of the server.
         */
        public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

        /**
         * The access token provided is expired, revoked, malformed, or invalid
         * for other reasons. The resource SHOULD respond with the HTTP 401
         * (Unauthorized) status code. The client MAY request a new access token
         * and retry the protected resource request.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-bearer-17#section-3.1">Error
         *      Codes</a>
         */
        public static final String INVALID_TOKEN = "invalid_token";

        /**
         * The request requires higher privileges than provided by the access
         * token. The resource server SHOULD respond with the HTTP 403
         * (Forbidden) status code and MAY include the "scope" attribute with
         * the scope necessary to access the protected resource.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-bearer-17#section-3.1">Error
         *      Codes</a>
         */
        public static final String INSUFFICIENT_SCOPE = "insufficient_scope";

        /**
         * The access token provided has expired. Resource servers SHOULD only
         * use this error code when the client is expected to be able to handle
         * the response and request a new access token using the refresh token
         * issued with the expired access token. The resource server MUST
         * respond with the HTTP 401 (Unauthorized) status code.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-5.2.1">5.2.1.
         *      Error Codes</a>
         */
        public static final String EXPIRED_TOKEN = "expired_token";

        /**
         * The client identifier provided is invalid, the client failed to
         * authenticate, the client did not include its credentials, provided
         * multiple client credentials, or used unsupported credentials type.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.3.1">4.3.1.
         *      Error Codes</a>
         */
        public static final String INVALID_CLIENT = "invalid_client";

        /**
         * Handles all errors that don't stem from invalid requests -- e.g.,
         * perhaps errors resulting from databases that are down or logic errors
         * in code.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.3.1">4.3.1.
         *      Error Codes</a>
         */
        public static final String UNKNOWN_ERROR = "unknown_error";

        /**
         * The provided access grant is invalid, expired, or revoked (e.g.
         * invalid assertion, expired authorization token, bad end-user password
         * credentials, or mismatching authorization code and redirection URI).
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.3.1">4.3.1.
         *      Error Codes</a>
         */
        public static final String INVALID_GRANT = "invalid_grant";

        /**
         * The provided access grant is invalid, expired, or revoked (e.g.
         * invalid assertion, expired authorization token, bad end-user password
         * credentials, or mismatching authorization code and redirection URI).
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.3.1">4.3.1.
         *      Error Codes</a>
         */
        public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

        /**
         * The code provided is invalid.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-4.3.1">4.3.1.
         *      Error Codes</a>
         */
        public static final String INVALID_CODE = "invalid_code";

        /**
         * The redirection URI provided does not match a pre-registered value.
         *
         * @see <a
         *      href="http://tools.ietf.org/html/draft-ietf-oauth-v2-10#section-3.2.1">3.2.1.
         *      Error Codes</a>
         */
        public static final String REDIRECT_URI_MISMATCH = "redirect_uri_mismatch";

        /**
         * The requested authentication type is not supported by the
         * authorization server.
         */
        public static final String UNSUPPORTED_AUTH_TYPE = "unsupported_auth_type";

        /**
         * The request is for data which does not exist.
         */
        public static final String NOT_FOUND = "not_found";

        /**
         * The request contains invalid client metadata.
         */
        public static final String INVALID_CLIENT_METADATA = "invalid_client_metadata";

        public static final String BAD_REQUEST = "bad_request";

        /**
         * OpenID Connect Consent error
         */
        public static final String CONSENT_REQUIRED = "consent_required";

        /**
         * OpenID Connect login required error
         */
        public static final String LOGIN_REQUIRED = "login_required";

        /**
         * OpenID Connect interaction required error
         */
        public static final String INTERACTION_REQUIRED = "interaction_required";

        /**
         * OAuth 2
         */
        public static final String REDIRECT_TEMPORARY = "redirection_temporary";
    }

    public static class Custom {
        /**
         * This optional parameter indicates whether the user should be prompted
         * for re-authentication and consent to grant account access to your
         * application each time he tries to complete a particular action.
         * <p/>
         * The default value is auto, which indicates that a user would only need
         * to grant access the first time he tried to access a protected resource.
         * <p/>
         * The parameter value may contain a combination of the following values
         * separated by spaces:
         * <ul>
         *     <li>login - Force re-authentication.</li>
         *     <li>consent - Force re-approval of access; must be combined with login
         *     if the user has not already authenticated.</li>
         *     <li>none - Require that the user has already authenticated and saved
         *     consent; cannot be combined with login or consent.</li>
         * </ul>
         */
        public static final String PROMPT = "prompt";
        public static final String APPROVAL_PROMPT = "approval_prompt";
        public static final String AUTO = "auto";
        public static final String FORCE = "force";
        public static final String DECISION = "decision";
        public static final String ALLOW = "allow";
        public static final String DENY = "deny";
        public static final String NONCE = "nonce";
        public static final String SAVE_CONSENT= "save_consent";
        public static final String SSO_TOKEN_ID = "ssoTokenId";
        public static final String CODE_CHALLENGE = "code_challenge";
        public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
        public static final String CODE_VERIFIER = "code_verifier";
        public static final String CODE_CHALLENGE_METHOD_S_256 = "S256";
        public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";
        public static final String RESPONSE_MODE = "response_mode";
        public static final String FORM_POST = "form_post";

        /**
         * The display mode in which to render the dialog. The default is
         * {@code page} on the www subdomain and {@code wap} on the m subdomain.
         * {@code touch}: Used on smartphone mobile devices, like iPhone and
         * Android. Use this for tablets with small screens (i.e., under 7
         * inches) as well. {@code wap}: Display plain HTML (without JavaScript)
         * on a small screen, such as a Nokia 7500. page: By default, dialogs
         * run in full-page mode with a Facebook header and footer. This is
         * appropriate for apps that do a full-page redirect in a normal
         * desktop/laptop web browser. popup: For use in a browser popup no
         * bigger than 400px by 580px. Use this display type to maintain context
         * for the user while on an external website. iframe: Display the dialog
         * in a lightbox iframe on the current page. Because of the risk of
         * clickjacking, this is only allowed for some certain dialogs, and
         * requires you to pass a valid access_token.
         */
        public static final String DISPLAY = "display";

        public static final String REALM = "realm";
        public static final String CLAIMS = "claims";
        public static final String MODULE = "module";
        public static final String SERVICE = "service";
        public static final String LOCALE = "locale";
        public static final String UI_LOCALES = "ui_locales";
        public static final String GOTO = "goto";
        public static final String AUDIENCE = "audience";
        public static final String USER_ID = "user_id";

        /**
         * The cookie name that contains any login_hint parameter that was provided
         * with an OpenID Connect authorize request, set so that authentication
         * modules in the authentication chain can use its value to improve UX.
         */
        public static final String LOGIN_HINT_COOKIE = "oidcLoginHint";

        public static final String RSR_ENDPOINT = "resource-set-reg-endpoint";
        public static final String JWK_RESOLVER = "jwk-resolver";
    }

    /**
     * Constants relating to the device code flow.
     */
    public static class DeviceCode {
        public static final String DEVICE_CODE = "device_code";
        public static final String USER_CODE = "user_code";
        public static final String VERIFICATION_URI = "verification_uri";
        public static final String INTERVAL = "interval";
    }

    public enum DisplayType {
        PAGE, POPUP, TOUCH, WAP;

        public String getFolder() {
            return name().toLowerCase();
        }
    }

    /**
     * Stores the constants for the OAuth2 Provider Service
     * @author Jason Lemay
     */
    public static class OAuth2ProviderService {
        //service name and version
        public static final String NAME = "OAuth2Provider";
        public static final String VERSION = "1.0";

        //service config fields
        public static final String AUTHZ_CODE_LIFETIME_NAME = "forgerock-oauth2-provider-authorization-code-lifetime";
        public static final String REFRESH_TOKEN_LIFETIME_NAME = "forgerock-oauth2-provider-refresh-token-lifetime";
        public static final String ACCESS_TOKEN_LIFETIME_NAME = "forgerock-oauth2-provider-access-token-lifetime";
        public static final String JWT_TOKEN_LIFETIME_NAME = "forgerock-oauth2-provider-jwt-token-lifetime";
        public static final String ISSUE_REFRESH_TOKEN = "forgerock-oauth2-provider-issue-refresh-token";
        public static final String ISSUE_REFRESH_TOKEN_ON_REFRESHING_TOKEN =
                "forgerock-oauth2-provider-issue-refresh-token-on-refreshing-token";
        public static final String SCOPE_PLUGIN_CLASS= "forgerock-oauth2-provider-scope-implementation-class";
        public static final String TOKEN_PLUGIN_LIST = "forgerock-oauth2-provider-token-map-class";
        public static final String STATELESS_TOKENS_ENABLED = "statelessTokensEnabled";
        public static final String ID_TOKEN_INFO_CLIENT_AUTHENTICATION_ENABLED =
                "idTokenInfoClientAuthenticationEnabled";
        public static final String RESPONSE_TYPE_LIST = "forgerock-oauth2-provider-response-type-map-class";
        public static final String AUTHENITCATION_ATTRIBUTES = "forgerock-oauth2-provider-authentication-attributes";
        public static final String SAVED_CONSENT_ATTRIBUTE = "forgerock-oauth2-provider-saved-consent-attribute";
        public static final String OIDC_CLAIMS_EXTENSION_SCRIPT =
                "forgerock-oauth2-provider-oidc-claims-extension-script";
        public static final String JKWS_URI = "forgerock-oauth2-provider-jkws-uri";
        public static final String CREATED_TIMESTAMP_ATTRIBUTE_NAME =
                "forgerock-oauth2-provider-created-attribute-name";
        public static final String MODIFIED_TIMESTAMP_ATTRIBUTE_NAME =
                "forgerock-oauth2-provider-modified-attribute-name";
        public static final String SUBJECT_TYPES_SUPPORTED = "forgerock-oauth2-provider-subject-types-supported";
        public static final String ID_TOKEN_SIGNING_ALGORITHMS =
                "forgerock-oauth2-provider-id-token-signing-algorithms-supported";
        public static final String TOKEN_SIGNING_RSA_KEYSTORE_ALIAS = "forgerock-oauth2-provider-keypair-name";
        public static final String TOKEN_SIGNING_ECDSA_KEYSTORE_ALIAS = "tokenSigningECDSAKeyAlias";
        public static final String OPEN_DYNAMIC_REGISTRATION_ALLOWED =
                "forgerock-oauth2-provider-allow-open-dynamic-registration";
        public static final String GENERATE_REGISTRATION_ACCESS_TOKENS =
                "forgerock-oauth2-provider-generate-registration-access-tokens";
        public static final String AMR_VALUE_MAPPING = "forgerock-oauth2-provider-amr-mappings";
        public static final String ACR_VALUE_MAPPING = "forgerock-oauth2-provider-loa-mapping";
        public static final String DEFAULT_ACR = "forgerock-oauth2-provider-default-acr";
        public static final String INVALID_SCOPE_BEHAVIOUR = "forgerock-oauth2-provider-invalid-scope-behaviour";
        public static final String PROFILE_MAPPINGS = "org-forgerock-oidc-profile-attribute-mappings";
        public static final String EMAIL_MAPPING = "org-forgerock-oidc-email-attribute-mapping";
        public static final String ADDRESS_MAPPING = "org-forgerock-oidc-address-attribute-mapping";
        public static final String PHONE_MAPPING = "org-forgerock-oidc-phone-attribute-mapping";
        public static final String STORE_OPS_TOKENS = "storeOpsTokens";
        public static final String CLIENTS_CAN_SKIP_CONSENT = "clientsCanSkipConsent";
        public static final String OIDC_SSOPROVIDER_ENABLED = "oidcSsoProviderEnabled";

        public static final String SUPPORTED_CLAIMS = "forgerock-oauth2-provider-supported-claims";
        public static final String DEFAULT_SCOPES = "forgerock-oauth2-provider-default-scopes";
        public static final String SUPPORTED_SCOPES = "forgerock-oauth2-provider-supported-scopes";
        public static final String CLAIMS_PARAMETER_SUPPORTED = "forgerock-oauth2-provider-claims-parameter-supported";
        public static final String HASH_SALT = "forgerock-oauth2-provider-hash-salt";
        public static final String CODE_VERIFIER = "forgerock-oauth2-provider-code-verifier-enforced";

        public static final String ALWAYS_ADD_CLAIMS_TO_TOKEN = "alwaysAddClaimsToToken";
        public static final String USER_DISPLAY_NAME_ATTRIBUTE = "displayNameAttribute";
        public static final String RESOURCE_OWNER_CUSTOM_LOGIN_URL_TEMPLATE = "customLoginUrlTemplate";
        public static final String DEVICE_VERIFICATION_URL = "verificationUrl";
        public static final String DEVICE_COMPLETION_URL = "completionUrl";
        public static final String DEVICE_CODE_LIFETIME = "deviceCodeLifetime";
        public static final String DEVICE_CODE_POLL_INTERVAL = "devicePollInterval";
        public static final String OPENID_CONNECT_VERSION = "3.0";
    }

    public static class AgentOAuth2ProviderService {
        public static final String NAME = "AgentOAuth2Provider";
        public static final String VERSION = "1.0";
        public static final long AUTHORIZATION_CODE_LIFETIME = 120;
        public static final long REFRESH_TOKEN_LIFETIME = 604800;
        public static final long ACCESS_TOKEN_LIFETIME = 3600;
        public static final long OPENID_CONNECT_JWT_TOKEN_LIFETIME = 3600;
    }

    /**
     * Logger file names
     */
    public static final String ACCESS_LOG_NAME = "OAuth2Provider.access";
    public static final String ERROR_LOG_NAME = "OAuth2Provider.error";
    public static final String DEBUG_LOG_NAME = "OAuth2Provider";

    public static class OAuth2Client {
        public static final String REDIRECT_URI = "com.forgerock.openam.oauth2provider.redirectionURIs";
        public static final String SCOPES = "com.forgerock.openam.oauth2provider.scopes";
        public static final String CLAIMS = "com.forgerock.openam.oauth2provider.claims";
        public static final String DEFAULT_SCOPES = "com.forgerock.openam.oauth2provider.defaultScopes";
        public static final String NAME = "com.forgerock.openam.oauth2provider.name";
        public static final String DESCRIPTION = "com.forgerock.openam.oauth2provider.description";
        public static final String GRANT_TYPES = "com.forgerock.openam.oauth2provider.grantTypes";
        public static final String APPLICATION_TYPE = "com.forgerock.openam.oauth2provider.applicationType";
        public static final String RESPONSE_TYPES = "com.forgerock.openam.oauth2provider.responseTypes";
        public static final String CONTACTS = "com.forgerock.openam.oauth2provider.contacts";
        public static final String LOGO_URI = "com.forgerock.openam.oauth2provider.logoURI";
        public static final String TOKEN_ENDPOINT_AUTH_METHOD =
                "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod";
        public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG =
                "com.forgerock.openam.oauth2provider.tokenEndPointAuthSigningAlg";
        public static final String POLICY_URI = "com.forgerock.openam.oauth2provider.policyURI";
        public static final String TOS_URI = "com.forgerock.openam.oauth2provider.tosURI";
        public static final String SECTOR_IDENTIFIER_URI = "com.forgerock.openam.oauth2provider.sectorIdentifierURI";
        public static final String SUBJECT_TYPE = "com.forgerock.openam.oauth2provider.subjectType";
        public static final String REQUEST_OBJECT_SIGNING_ALG =
                "com.forgerock.openam.oauth2provider.requestObjectSigningAlg";
        public static final String REQUEST_OBJECT_ENCRYPTION_ALG =
                "com.forgerock.openam.oauth2provider.requestObjectEncryptionAlg";
        public static final String REQUEST_OBJECT_ENCRYPTION_ENC =
                "com.forgerock.openam.oauth2provider.requestObjectEncryptionEnc";
        public static final String USERINFO_SIGNED_RESPONSE_ALG =
                "com.forgerock.openam.oauth2provider.userinfoSignedResponseAlg";
        public static final String USERINFO_ENCRYPTED_RESPONSE_ALG =
                "com.forgerock.openam.oauth2provider.userinfoEncryptedResponseAlg";
        public static final String USERINFO_SIGN_AND_ENC_RESPONSE_ALG =
                "com.forgerock.openam.oauth2provider.userinfoEncryptedResponseEnc";
        public static final String IDTOKEN_SIGNED_RESPONSE_ALG =
                "com.forgerock.openam.oauth2provider.idTokenSignedResponseAlg";
        public static final String IDTOKEN_ENCRYPTED_RESPONSE_ALG =
                "com.forgerock.openam.oauth2provider.idTokenEncryptedResponseAlg";
        public static final String IDTOKEN_ENC_AND_SIGNED_RESPONSE_ALG =
                "com.forgerock.openam.oauth2provider.idTokenEncryptedResponseEnc";
        public static final String DEFAULT_MAX_AGE = "com.forgerock.openam.oauth2provider.defaultMaxAge";
        public static final String DEFAULT_MAX_AGE_ENABLED = "com.forgerock.openam.oauth2provider.defaultMaxAgeEnabled";
        public static final String REQUIRE_AUTH_TIME = "com.forgerock.openam.oauth2provider.requireAuthTime";
        public static final String DEFAULT_ACR_VALS = "com.forgerock.openam.oauth2provider.defaultACRValues";
        public static final String INIT_LOGIN_URL = "com.forgerock.openam.oauth2provider.initiateLoginUri";
        public static final String POST_LOGOUT_URI = "com.forgerock.openam.oauth2provider.postLogoutRedirectURI";
        public static final String REQUEST_URLs = "com.forgerock.openam.oauth2provider.requestURIs";
        public static final String ACTIVE = "sunIdentityServerDeviceStatus";
        public static final String CLIENT_TYPE = "com.forgerock.openam.oauth2provider.clientType";
        public static final String USERPASSWORD = "userpassword";
        public static final String REALM = "realm";
        public static final String CLIENT_ID = "client_id";
        public static final String CLIENT_SECRET = "client_secret";
        public static final String ACCESS_TOKEN = "com.forgerock.openam.oauth2provider.accessToken";
        public static final String CLIENT_SESSION_URI = "com.forgerock.openam.oauth2provider.clientSessionURI";
        public static final String CLIENT_NAME = "com.forgerock.openam.oauth2provider.clientName";
        public static final String IS_CONSENT_IMPLIED = "isConsentImplied";

        public static final String JWKS_URI = "com.forgerock.openam.oauth2provider.jwksURI";
        public static final String JWKS = "com.forgerock.openam.oauth2provider.jwks";
        public static final String CLIENT_JWT_PUBLIC_KEY = "com.forgerock.openam.oauth2provider.clientJwtPublicKey";
        public static final String PUBLIC_KEY_SELECTOR = "com.forgerock.openam.oauth2provider.publicKeyLocation";

        public static final String AUTHORIZATION_CODE_LIFE_TIME =
                "com.forgerock.openam.oauth2provider.authorizationCodeLifeTime";
        public static final String ACCESS_TOKEN_LIFE_TIME = "com.forgerock.openam.oauth2provider.accessTokenLifeTime";
        public static final String REFRESH_TOKEN_LIFE_TIME = "com.forgerock.openam.oauth2provider.refreshTokenLifeTime";
        public static final String JWT_TOKEN_LIFE_TIME = "com.forgerock.openam.oauth2provider.jwtTokenLifeTime";
    }

    public static class JWTTokenParams {

        public static final String JWT_TOKEN = "JWTToken";
        public static final String ID_TOKEN = "id_token";
        public static final String ISS = "iss";
        public static final String SUB = "sub";
        public static final String AUD = "aud";
        public static final String AZP = "azp";
        public static final String EXP = "exp";
        public static final String IAT =  "iat";
        public static final String AUTH_TIME = "auth:time";
        public static final String NONCE = "nonce";
        public static final String OPS = "org.forgerock.openidconnect.ops";
        public static final String LEGACY_OPS = "ops";
        public static final String UPDATED_AT = "updated_at";
        public static final String ACR = "acr";
        public static final String AMR = "amr";
        public static final String AT_HASH = "at_hash";
        public static final String C_HASH = "c_hash";
        public static final String KEY_ID = "kid";
        public static final String REALM = "realm";
        public static final String KEYS = "keys";
        public static final String SSOTOKEN = "ssoToken";
    }

    /**
     * List of client attributes and their names as specified in the OAuth2/OpenID Connect Spec
     */
    public enum ShortClientAttributeNames {
        REDIRECT_URIS("redirect_uris"),
        RESPONSE_TYPES("response_types"),
        GRANT_TYPES("grant_types"),
        APPLICATION_TYPE("application_type"),
        CONTACTS("contacts"),
        CLIENT_NAME("client_name"),
        LOGO_URI("logo_uri"),
        CLIENT_URI("client_uri"),
        POLICY_URI("policy_uri"),
        TOS_URI("tos_uri"),
        JWKS_URI("jwks_uri"),
        JWKS("jwks"),
        SECTOR_IDENTIFIER_URI("sector_identifier_uri"),
        SUBJECT_TYPE("subject_type"),
        ID_TOKEN_SIGNED_RESPONSE_ALG("id_token_signed_response_alg"),
        ID_TOKEN_ENCRYPTED_RESPONSE_ALG("id_token_encrypted_response_alg"),
        ID_TOKEN_ENCRYPTED_RESONSE_ENC("id_token_encrypted_response_enc"),
        USERINFO_SIGNED_RESPONSE_ALG("userinfo_signed_response_alg"),
        USERINFO_ENCRYPTED_RESPONSE_ALG("userinfo_encrypted_response_alg"),
        USERINFO_ENCRYPTED_RESONSE_ENC("userinfo_encrypted_response_enc"),
        REQUEST_OBJECT_SIGNING_ALG("request_object_signing_alg"),
        REQUEST_OBJECT_ENCRYPTION_ALG("request_object_encryption_alg"),
        REQUEST_OBJECT_ENCRYPTION_ENC("request_object_encryption_enc"),
        TOKEN_ENDPOINT_AUTH_METHOD("token_endpoint_auth_method"),
        TOKEN_ENDPOINT_AUTH_SIGNING_ALG("token_endpoint_auth_signing_alg"),
        DEFAULT_MAX_AGE("default_max_age"),
        DEFAULT_MAX_AGE_ENABLED("default_max_age_enabled"),
        REQUIRE_AUTH_TIME("require_auth_time"),
        DEFAULT_ACR_VALUES("default_acr_values"),
        INITIATE_LOGIN_URI("initiate_login_uri"),
        REQUEST_URIS("request_uris"),
        POST_LOGOUT_REDIRECT_URIS("post_logout_redirect_uris"),
        REGISTRATION_ACCESS_TOKEN("registration_access_token"),
        CLIENT_SESSION_URI("client_session_uri"),
        CLIENT_ID("client_id"),
        CLIENT_SECRET("client_secret"),
        CLIENT_TYPE("client_type"),
        SCOPES("scopes"),
        DEFAULT_SCOPES("default_scopes"),
        DISPLAY_NAME("display_name"),
        CLIENT_DESCRIPTION("client_description"),
        REALM("realm"),
        PUBLIC_KEY_SELECTOR("public_key_selector"),
        X509("x509"),
        AUTHORIZATION_CODE_LIFE_TIME("authorization_code_lifetime"),
        ACCESS_TOKEN_LIFE_TIME("access_token_lifetime"),
        REFRESH_TOKEN_LIFE_TIME("refresh_token_lifetime"),
        JWT_TOKEN_LIFE_TIME("jwt_token_lifetime");

        private String name;

        ShortClientAttributeNames(String name) {
            this.name = name;
        }

        public String getType() {
            return this.name;
        }

        public static ShortClientAttributeNames fromString(String type) {
            if (type != null) {
                for (ShortClientAttributeNames shortClientAttributeNames : ShortClientAttributeNames.values()) {
                    if (type.equalsIgnoreCase(shortClientAttributeNames.name)) {
                        return shortClientAttributeNames;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Indicates the location of an entity in a URL.
     */
    public enum UrlLocation {
        /** The query part of the URL. */
        QUERY,

        /** The fragment part of the URL. */
        FRAGMENT
    }

    /**
     * Constants for resource sets.
     */
    public static class ResourceSets {
        public static final String RESOURCE_SET_ID = "resource_set_id";
        public static final String POLICY_URI = "policy_uri";
        public static final String NAME = "name";
        public static final String URI = "uri";
        public static final String TYPE = "type";
        public static final String SCOPES = "scopes";
        public static final String ICON_URI = "icon_uri";
        public static final String LABELS = "labels";
    }

    /**
     * Constants for scripting implementation
     */
    public static class ScriptParams {
        public static final String SCOPES = "scopes";
        public static final String IDENTITY = "identity";
        public static final String LOGGER = "logger";
        public static final String CLAIMS = "claims";
        public static final String SESSION = "session";
        public static final String REQUESTED_CLAIMS = "requestedClaims";
    }

    /**
     * Constants for supported scopes
     */
    public static class Scopes {

        /** OpenId scope. */
        public static final String OPENID = "openid";

        /** Email scope. */
        public static final String EMAIL = "email";

        /** Address scope. */
        public static final String ADDRESS = "address";

        /** Phone scope. */
        public static final String PHONE = "phone";

        /** Profile scope. */
        public static final String PROFILE = "profile";
    }

    /**
     * Constants for proof of possession as described by RFC-7800.
     */
    public static class ProofOfPossession {

        /**
         * Confirmation claim, expected format is either a valid jwk, jwe or a jku.
         */
        public static final String CNF = "cnf";

        /**
         * OAuth2 request parameter, expected to be base64 encoded.
         */
        public static final String CNF_KEY = "cnf_key";

    }

}
