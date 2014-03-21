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
 * Copyright 2014 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.oidc;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;

import com.sun.identity.shared.datastruct.CollectionHelper;
import org.forgerock.jaspi.modules.openid.OpenIdConnectModule;
import org.forgerock.openam.authentication.modules.common.JaspiAuthModuleWrapper;
import org.forgerock.util.Reject;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit-testable implementation of the AMLoginModule (by virtue of extending JaspiAuthModuleWrapper), which
 * delegates AMLoginModule calls to the jaspi OpenIdConnectModule.
 */
public class OpenIdConnectAuthModule extends JaspiAuthModuleWrapper<OpenIdConnectModule> {
    private static final String RESOURCE_BUNDLE_NAME = "amAuthOpenIdConnect";
    private static final String HEADER_NAME_KEY = "openam-auth-openidconnect-header-name";
    private static final String KEYSTORE_LOCATION_KEY = "openam-auth-openidconnect-keystore-location";
    private static final String KEYSTORE_PASSWORD_KEY = "openam-auth-openidconnect-keystore-password";
    private static final String KEYSTORE_TYPE_KEY = "openam-auth-openidconnect-keystore-type";

    private String headerName;

    public OpenIdConnectAuthModule() {
        this(new OpenIdConnectModule(), RESOURCE_BUNDLE_NAME);
    }

    /**
     * Constructs an instance of the JaspiAuthModuleWrapper. Used by unit tests.
     *
     * @param serverAuthModule   An instance of the underlying JASPI ServerAuthModule.
     * @param resourceBundleName The name of the authentication module's resource bundle.
     */
    OpenIdConnectAuthModule(OpenIdConnectModule serverAuthModule, String resourceBundleName) {
        super(serverAuthModule, resourceBundleName);
    }

    /**
     * Called by JaspiAuthModuleWrapper to obtain the Map<String, Object> passed to the initialize method defined
     * in the jaspi ServerAuthModule class.
     * @param subject The Subject to be authenticated.
     * @param sharedState The state shared with other configured LoginModules.
     * @param options The options specified in the Login Configuration for this particular LoginModule.
     * @return a Map<String, Object> containing the state needed by the OpenIdConnectModule's initialize method.
     *
     *
    {
    “serverAuthContext” : {
    “authModules” : [
    "className" : “org.forgerock.jaspi.modules.openid.OpenIdConnectModule”,
    “properties” : {
    “openIdConnectHeader” : “X-OPENAM-OPENID”,
    “keystoreLocation” : “resources/cacert.jks”,
    “keystorePassword” : “storepass”,
    “keystoreType” : “JKS”,
    “keystorePrivatePass” : “”,
    “resolvers” : [
    “keyAlias” : “google”,
    “issuer” : “accounts.google.com”,
    “clientId” : “…apps.googleusercontent.com"
    ]
    }

    ]
    }

    }     */
    @Override
    protected Map<String, Object> initialize(Subject subject, Map sharedState, Map options) {
        Map<String, Object> optionsMap = new HashMap<String, Object>();

        headerName = CollectionHelper.getMapAttr(options, HEADER_NAME_KEY);
        String keyStoreLocation = CollectionHelper.getMapAttr(options, KEYSTORE_LOCATION_KEY);
        String keyStorePassword = CollectionHelper.getMapAttr(options, KEYSTORE_PASSWORD_KEY);
        String keyStoreType = CollectionHelper.getMapAttr(options, KEYSTORE_TYPE_KEY);
        Reject.ifNull(headerName, HEADER_NAME_KEY + " must be set.");
        Reject.ifNull(keyStoreLocation, KEYSTORE_LOCATION_KEY + " must be set.");
        Reject.ifNull(keyStorePassword, KEYSTORE_PASSWORD_KEY + " must be set.");
        Reject.ifNull(keyStoreType, KEYSTORE_TYPE_KEY + " must be set.");

        optionsMap.put(OpenIdConnectModule.HEADER_KEY, headerName);
        optionsMap.put(OpenIdConnectModule.KEYSTORE_LOCATION_KEY, keyStoreLocation);
        optionsMap.put(OpenIdConnectModule.KEYSTORE_PASSWORD_KEY, keyStorePassword);
        optionsMap.put(OpenIdConnectModule.KEYSTORE_TYPE_KEY, keyStoreType);
        /*
        what about resolvers - how to configure in UI/properties
        final List<Map<String, String>> resolvers =
                (List<Map<String, String>>) config.get(OpenIdConnectModule.RESOLVERS_KEY);
        */
        return optionsMap;
    }

    /**
     * Called by JaspiModuleWrapper's implementation of the AMLoginModule's process method prior to calling validateRequest
     * on the underlying jaspi ServerAuthModule. Provides an opportunity to manipulate the state provided to validateRequest
     * prior to the invocation. The OpenIdConnectModule expects the OIDC ID Token in the HttpServletRequest referenced by
     * the OpenIdConnectModule.HEADER_KEY.
     * @param messageInfo The ServerAuthModules MessageInfo instance.
     * @param clientSubject A Subject that represents the source of the service request.
     * @param callbacks An array of Callbacks for this Login state.
     * @return true or false, indicating whether the validateRequest should be invoked. If false is returned, the authentication
     * will fail.
     * @throws LoginException
     */
    @Override
    protected boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks) throws LoginException {
        final HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        final String jwtValue = request.getHeader(headerName);
        //if the ID Token is present, return true to indicate that processing should continue.
        return jwtValue != null && !jwtValue.isEmpty();
    }

    /**
     * This method is called from onLoginSuccess in the JaspiAuthModuleWrapper. It is intended to initialize a newly created
     * jaspi ServerAuthModule (not clear why one is created - perhaps new instance is called prior to the secureResponse invocation).
     * Then onLoginSuccess is called on this class, and then secureResponse on the OpenIdConnectModule. The bottom line is that
     * secureResponse is a no-op in the OpenIdConnectModule, so it does not appear that there is anything to do here.
     * @param requestParamsMap A Map containing the HttpServletRequest parameters.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param ssoToken The authentication user's SSOToken.
     * @return
     * @throws AuthenticationException
     */
    @Override
    protected Map<String, Object> initialize(Map requestParamsMap, HttpServletRequest request,
                                             HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
        return null;
    }

    /**
     * Called before secureResponse is called on the underlying jaspi module. Nothing to do here.
     * @param messageInfo The ServerAuthModules MessageInfo instance.
     * @param requestParamsMap A Map containing the HttpServletRequest parameters.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param ssoToken The authentication user's SSOToken.
     * @throws AuthenticationException
     */
    @Override
    protected void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
                                  HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

}