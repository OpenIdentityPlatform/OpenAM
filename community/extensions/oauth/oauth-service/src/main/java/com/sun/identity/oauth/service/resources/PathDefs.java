/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.oauth.service.resources;

/**
 *
 * @author Hubert A. Le Van Gong <hubert.levangong at Sun.COM>
 */
public class PathDefs {
    // We'll switch to a config file later on.

    // Global paths
    static public final String ServicePath = "/TokenService";
    static final String GenericPath = "/oauth";
    static final String VersionPath = "/v1";

    // OAuth protocol endpoints
    static final String RequestTokenRequestPath = GenericPath + VersionPath + "/get_request_token";
    static final String AccessTokenRequestPath = GenericPath + VersionPath + "/get_access_token";
    static final String ResourceOwnerAuthorizationPath = GenericPath + VersionPath + "/request_authorization";


    // Token Service endpoints
    static final String RequestTokensPath = GenericPath + VersionPath + "/rtoken";
    static final String AccessTokensPath = GenericPath + VersionPath + "/atoken";
    static final String ConsumersPath = GenericPath + VersionPath + "/consumer";

    static final String ConsumerRegistrationPath = GenericPath + VersionPath + "/consumer_registration";

    // Other endpoints
    static final String NoBrowserAuthorizationPath = GenericPath + VersionPath + "/NoBrowserAuthorization";
    static final String ResourceOwnerAuthorizationNextPath = "/request_authorization_next.jsp";
    static final String createAuthorizationPath = GenericPath + VersionPath + "/AuthorizationFactory";

    // OpenSSO authentication endpoint - REST I/F
    static final String OpenSSOAuthenticationEndpoint = "http://localhost:8080/opensso/identity";
}
 