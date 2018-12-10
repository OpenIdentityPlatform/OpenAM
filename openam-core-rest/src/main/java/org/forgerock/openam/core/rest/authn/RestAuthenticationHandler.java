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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.authn;

import static org.forgerock.openam.core.rest.authn.RestAuthenticationConstants.*;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.SignatureException;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.audit.context.AuditRequestContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.authn.core.AuthIndexType;
import org.forgerock.openam.core.rest.authn.core.AuthenticationContext;
import org.forgerock.openam.core.rest.authn.core.LoginAuthenticator;
import org.forgerock.openam.core.rest.authn.core.LoginConfiguration;
import org.forgerock.openam.core.rest.authn.core.LoginProcess;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthErrorCodeException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthException;
import org.forgerock.openam.core.rest.authn.exceptions.RestAuthResponseException;
import org.forgerock.openam.security.whitelist.ValidGotoUrlExtractor;
import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.utils.JsonObject;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * Handles the initial authenticate and subsequent callback submit RESTful calls.
 */
public class RestAuthenticationHandler {

    private static final Debug DEBUG = Debug.getInstance("amAuthREST");

    private final LoginAuthenticator loginAuthenticator;
    private final RestAuthCallbackHandlerManager restAuthCallbackHandlerManager;
    private final AMAuthErrorCodeResponseStatusMapping amAuthErrorCodeResponseStatusMapping;
    private final AuthIdHelper authIdHelper;
    private final CoreWrapper coreWrapper;
    private final RedirectUrlValidator<String> urlValidator =
            new RedirectUrlValidator<String>(ValidGotoUrlExtractor.getInstance());
    
    /**
     * Constructs an instance of the RestAuthenticationHandler.
     *
     * @param loginAuthenticator An instance of the LoginAuthenticator.
     * @param restAuthCallbackHandlerManager An instance of the RestAuthCallbackHandlerManager.
     * @param amAuthErrorCodeResponseStatusMapping An instance of the AMAuthErrorCodeResponseStatusMapping.
     * @param authIdHelper An instance of the AuthIdHelper.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    @Inject
    public RestAuthenticationHandler(LoginAuthenticator loginAuthenticator,
            RestAuthCallbackHandlerManager restAuthCallbackHandlerManager,
            AMAuthErrorCodeResponseStatusMapping amAuthErrorCodeResponseStatusMapping, AuthIdHelper authIdHelper,
            CoreWrapper coreWrapper) {
        this.loginAuthenticator = loginAuthenticator;
        this.restAuthCallbackHandlerManager = restAuthCallbackHandlerManager;
        this.amAuthErrorCodeResponseStatusMapping = amAuthErrorCodeResponseStatusMapping;
        this.authIdHelper = authIdHelper;
        this.coreWrapper = coreWrapper;
    }

    /**
     * Handles authentication requests from HTTP both GET and POST. Will then either create the Login
     * Process, as the request will be a new authentication request.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param authIndexType The authentication index type.
     * @param indexValue The authentication index value.
     * @param sessionUpgradeSSOTokenId The SSO Token Id of the user's current session, null if not performing a session
     *                                 upgrade.
     * @return The Response of the authentication request.
     */
    public JsonValue initiateAuthentication(HttpServletRequest request, HttpServletResponse response,
            String authIndexType, String indexValue, String sessionUpgradeSSOTokenId) throws RestAuthException {
        return authenticate(request, response, null, authIndexType, indexValue, sessionUpgradeSSOTokenId);
    }

    /**
     * Handles authentication requests from HTTP POST. Will then either create or retrieve the Login Process,
     * dependent on if the request is a new authentication request or a continuation of one.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param postBody The JsonValue post body of the request.
     * @param sessionUpgradeSSOTokenId The SSO Token Id of the user's current session, null if not performing a session
     *                                 upgrade.
     * @return The Response of the authentication request.
     */
    public JsonValue continueAuthentication(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, String sessionUpgradeSSOTokenId) throws RestAuthException {
        return authenticate(request, response, postBody, null, null, sessionUpgradeSSOTokenId);
    }

    /**
     * Handles either the creation or retrieval of the Login Process, dependent on if the request is a new
     * authentication request or a continuation of one.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param postBody The post body of the request.
     * @param authIndexType The authentication index type.
     * @param indexValue The authentication index value.
     * @param sessionUpgradeSSOTokenId The SSO Token Id of the user's current session, null if not performing a session
     *                                 upgrade.
     * @return The Response of the authentication request.
     */
    private JsonValue authenticate(HttpServletRequest request, HttpServletResponse response, JsonValue postBody,
            String authIndexType, String indexValue, String sessionUpgradeSSOTokenId)
            throws RestAuthException {

        LoginProcess loginProcess = null;
        try {

            AuthIndexType indexType = getAuthIndexType(authIndexType);

            String authId = null;
            String sessionId = null;

            if (postBody != null) {
                authId = getAuthId(postBody);

                if (authId != null) {
                    SignedJwt jwt = authIdHelper.reconstructAuthId(authId);
                    sessionId = getSessionId(jwt);
                    indexType = getAuthIndexType(jwt);
                    indexValue = getAuthIndexValue(jwt);
                    String realmDN = getRealmDomainName(jwt);
                    AuditRequestContext.putProperty(SESSION_ID, sessionId);
                    authIdHelper.verifyAuthId(realmDN, authId);
                }
            }

            LoginConfiguration loginConfiguration = new LoginConfiguration()
                    .httpRequest(request)
                    .httpResponse(response)
                    .indexType(indexType)
                    .indexValue(indexValue)
                    .sessionId(sessionId)
                    .forceAuth(request.getParameter(AuthUtils.FORCE_AUTH))
                    .sessionUpgrade(sessionUpgradeSSOTokenId);

            loginProcess = loginAuthenticator.getLoginProcess(loginConfiguration);

            return processAuthentication(request, response, postBody, authId, loginProcess, loginConfiguration);

        } catch (RestAuthException e) {
            if (loginProcess != null) {
                String failureUrl = urlValidator.getRedirectUrl(loginProcess.getAuthContext().getOrgDN(),
                        loginProcess.getFailureURL(), null);
                e.setFailureUrl(failureUrl);
            }
            throw e;
        } catch (L10NMessageImpl e) {
            throw new RestAuthException(amAuthErrorCodeResponseStatusMapping.getAuthLoginExceptionResponseStatus(
                    e.getErrorCode()), e);
        } catch (JsonException e) {
            throw new RestAuthException(ResourceException.INTERNAL_ERROR, e);
        } catch (SignatureException e) {
            throw new RestAuthException(ResourceException.INTERNAL_ERROR, e);
        } catch (AuthLoginException e) {
            throw new RestAuthException(amAuthErrorCodeResponseStatusMapping.getAuthLoginExceptionResponseStatus(
                    e.getErrorCode()), e);
        } catch (JwsSigningException jse) {
            DEBUG.error("JwsSigningException", jse);
            throw new RestAuthException(ResourceException.INTERNAL_ERROR, "JwsSigningException, " + jse.getMessage());
        }
    }

    private String getRealmDomainName(SignedJwt jwt) {
        return jwt.getClaimsSet().getClaim("realm", String.class);
    }

    private String getAuthIndexValue(SignedJwt jwt) {
        return jwt.getClaimsSet().getClaim("authIndexValue", String.class);
    }

    private AuthIndexType getAuthIndexType(SignedJwt jwt) throws RestAuthException {
        AuthIndexType indexType;
        String authIndexTypeString = jwt.getClaimsSet().getClaim("authIndexType", String.class);
        indexType = getAuthIndexType(authIndexTypeString);
        return indexType;
    }

    private String getSessionId(SignedJwt jwt) {
        return jwt.getClaimsSet().getClaim("sessionId", String.class);
    }

    private String getAuthId(JsonValue postBody) {
        return postBody.get("authId").asString();
    }

    /**
     * Using the given LoginProcess will process the authentication by getting the required callbacks and either
     * completing and submitting them or sending the requirements back to the client as JSON. If the authentication
     * process has completed it will then check the completion status and will either return an error or the SSO Token
     * Id to the client.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param postBody The post body of the request.
     * @param loginProcess The LoginProcess used to track the login.
     * @param loginConfiguration The LoginConfiguration used to configure the login process.
     * @return A ResponseBuilder which contains the contents of the response to return to the client.
     * @throws AuthLoginException If there is a problem submitting the callbacks.
     * @throws SignatureException If there is a problem creating the JWT to use in the response to the client.
     */
    private JsonValue processAuthentication(HttpServletRequest request,
            HttpServletResponse response, JsonValue postBody, String authId,
            LoginProcess loginProcess, LoginConfiguration loginConfiguration)
            throws AuthLoginException, SignatureException, RestAuthException {

        switch (loginProcess.getLoginStage()) {
            case REQUIREMENTS_WAITING: {

                Callback[] callbacks = loginProcess.getCallbacks();

                JsonValue jsonCallbacks;
                try {
                    if (callbacks.length == 1 && callbacks[0] instanceof RedirectCallback && postBody != null) {
                        jsonCallbacks = null;
                    } else {
                        jsonCallbacks = handleCallbacks(request, response, postBody, callbacks);
                    }
                } catch (RestAuthResponseException e) {
                    // Include the authId in the JSON response.
                    if (authId == null) {
                        authId = authIdHelper.createAuthId(loginConfiguration, loginProcess.getAuthContext());
                    }
                    e.getJsonResponse().put(AUTH_ID, authId);
                    AuditRequestContext.putProperty(AUTH_ID, authId);
                    throw e;
                }

                if (jsonCallbacks != null && jsonCallbacks.size() > 0) {
                    JsonValue jsonValue = createJsonCallbackResponse(authId, loginConfiguration, loginProcess,
                            jsonCallbacks);
                    return jsonValue;
                } else {
                    loginProcess = loginProcess.next(callbacks);
                    return processAuthentication(request, response, null, authId,
                            loginProcess, loginConfiguration);
                }
            }
            case COMPLETE: {
                loginProcess.cleanup();

                if (loginProcess.isSuccessful()) {
                    // send token to client
                    JsonObject jsonResponseObject = JsonValueBuilder.jsonValue();

                    SSOToken ssoToken = loginProcess.getSSOToken();
                    if (ssoToken != null) {
                        String tokenId = ssoToken.getTokenID().toString();
                        jsonResponseObject.put(TOKEN_ID, tokenId);
                        AuditRequestContext.putProperty(TOKEN_ID, tokenId);
                    } else {
                        jsonResponseObject.put("message", "Authentication Successful");
                    }

                    String gotoUrl = urlValidator.getRedirectUrl(loginProcess.getOrgDN(),
                            urlValidator.getValueFromJson(postBody, RedirectUrlValidator.GOTO),
                            loginProcess.getSuccessURL());

                    jsonResponseObject.put("successUrl", gotoUrl);
                    jsonResponseObject.put("realm", coreWrapper.convertOrgNameToRealmName(loginProcess.getOrgDN()));

                    return jsonResponseObject.build();

                } else {
                    // send Error to client
                    AuthenticationContext authContext = loginProcess.getAuthContext();
                    String errorCode = authContext.getErrorCode();
                    String errorMessage = authContext.getErrorMessage();

                    throw new RestAuthErrorCodeException(errorCode, errorMessage);
                }
            }
        }

        // This should never happen
        throw new RestAuthException(ResourceException.INTERNAL_ERROR, "Unknown Authentication State!");
    }

    private JsonValue handleCallbacks(HttpServletRequest request, HttpServletResponse response,
            JsonValue postBody, Callback[] callbacks)
            throws RestAuthException {

        JsonValue jsonCallbacks = null;
        if (postBody == null || postBody.size() == 0) {
            jsonCallbacks = restAuthCallbackHandlerManager.handleCallbacks(request, response, callbacks);

        } else if (!postBody.get("callbacks").isNull()) {
            JsonValue jCallbacks = postBody.get("callbacks");

            restAuthCallbackHandlerManager.handleJsonCallbacks(callbacks, jCallbacks);
        } else {
            restAuthCallbackHandlerManager.handleResponseCallbacks(request, response, callbacks, postBody);
        }

        return jsonCallbacks;
    }

    private JsonValue createJsonCallbackResponse(String authId, LoginConfiguration loginConfiguration,
            LoginProcess loginProcess, JsonValue jsonCallbacks) throws SignatureException, RestAuthException {

        PagePropertiesCallback pagePropertiesCallback = loginProcess.getPagePropertiesCallback();

        JsonObject jsonResponseObject = JsonValueBuilder.jsonValue();

        if (authId == null) {
            authId = authIdHelper.createAuthId(loginConfiguration, loginProcess.getAuthContext());
        }
        jsonResponseObject.put(AUTH_ID, authId);
        AuditRequestContext.putProperty(AUTH_ID, authId);
        if (pagePropertiesCallback != null) {
            jsonResponseObject.put("template", pagePropertiesCallback.getTemplateName());
            String moduleName = pagePropertiesCallback.getModuleName();
            String state = pagePropertiesCallback.getPageState();
            jsonResponseObject.put("stage", moduleName + state);
            jsonResponseObject.put("header", pagePropertiesCallback.getHeader());
            jsonResponseObject.put("infoText", pagePropertiesCallback.getInfoText());
        }
        jsonResponseObject.put("callbacks", jsonCallbacks.getObject());

        return jsonResponseObject.build();
    }

    /**
     * Gets the AuthIndexType for the given authentication index type string.
     *
     * @param authIndexType The authentication index string.
     * @return The AuthIndexType enum.
     */
    private AuthIndexType getAuthIndexType(String authIndexType) throws RestAuthException {

        try {
            return AuthIndexType.getAuthIndexType(authIndexType);
        } catch (IllegalArgumentException e) {
            DEBUG.message("Unknown Authentication Index Type, " + authIndexType);
            throw new RestAuthException(ResourceException.BAD_REQUEST, "Unknown Authentication Index Type, "
                    + authIndexType);
        }
    }
}
