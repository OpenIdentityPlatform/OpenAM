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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * A class which wraps the JASPI Authentication Module framework, allowing JASPI Authentication Modules
 * to be used with JAAS Authentication Modules and the OpenAM Post Authentication Procoess.
 *
 * @param <T> Implementation of the ServerAuthModule interface.
 */
public abstract class JaspiAuthModuleWrapper<T extends ServerAuthModule> extends AuthLoginModule
        implements AMPostAuthProcessInterface {

    private final Debug debug;

    private final T serverAuthModule;
    private final String resourceBundleName;

    /**
     * Constructs an instance of the JaspiAuthModuleWrapper.
     *
     * @param serverAuthModule An instance of the underlying JASPI ServerAuthModule.
     * @param resourceBundleName The name of the authentication module's resource bundle.
     */
    public JaspiAuthModuleWrapper(T serverAuthModule, String resourceBundleName) {
        this.serverAuthModule = serverAuthModule;
        this.resourceBundleName = resourceBundleName;
        debug = Debug.getInstance(resourceBundleName);
    }

    /**
     * Initialises this LoginModule, which initialises the underlying JASPI ServerAuthModule.
     *
     * @param subject {@inheritDoc}
     * @param sharedState {@inheritDoc}
     * @param options {@inheritDoc}
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        try {
            Map<String, Object> config = initialize(subject, sharedState, options);

            serverAuthModule.initialize(createRequestMessagePolicy(), null, getCallbackHandler(), config);
        } catch (AuthException e) {
            debug.error("Failed to initialise the underlying JASPI Server Auth Module.", e);
        }
    }

    /**
     * Initialises the underlying JASPI ServerAuthModule with the required configuration.
     *
     * @param subject The Subject to be authenticated.
     * @param sharedState The state shared with other configured LoginModules.
     * @param options The options specified in the Login Configuration for this particular LoginModule.
     * @return The Map of configuration information for the underlying JASPI ServerAuthModule.
     */
    protected abstract Map<String, Object> initialize(Subject subject, Map sharedState, Map options);

    /**
     * Controls the flow of the login process.
     *
     * Only one state is applicable, which is ISAuthConstants.LOGIN_START. Calls the internal process method to allow
     * the subtype implementation to perform any required logic prior to calling the JASPI ServerAuthModule's
     * validateRequest method.
     *
     * @param callbacks {@inheritDoc}
     * @param state {@inheritDoc}
     * @return {@inheritDoc}
     * @throws LoginException {@inheritDoc}
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {

        switch (state) {
        case ISAuthConstants.LOGIN_START: {
            final Subject clientSubject = new Subject();
            MessageInfo messageInfo = prepareMessageInfo(getHttpServletRequest(), getHttpServletResponse());
            if (process(messageInfo, clientSubject, callbacks)) {
                AuthStatus authStatus = serverAuthModule.validateRequest(messageInfo, clientSubject, null);

                if (AuthStatus.SUCCESS.equals(authStatus)) {
                    // The module has successfully authenticated the client.
                    debug.message("Successfully validated request");
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                    // The module may have completely/partially/not authenticated the client.
                    debug.message("Successfully validated request");
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
                    // The module has failed to authenticate the client.
                    debug.error("The underlying JASPI Authentication Module has failed.");
                    throw new AuthLoginException(resourceBundleName, "authFailed", null);
                } else if (AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                    // The module has not completed authenticating the client.
                    debug.message("JASPI Authentication Module returned SEND_CONTINUE so ignoring the module");
                    return ISAuthConstants.LOGIN_IGNORE;
                }
            }
        }
        default: {
            throw new AuthLoginException(resourceBundleName, "incorrectState", null);
        }
        }
    }

    /**
     * Internal call to subtype implementation to perform any required logic before the validateRequest method is
     * called on the underlying JASPI ServerAuthModule.
     *
     * @param messageInfo The ServerAuthModules MessageInfo instance.
     * @param clientSubject A Subject that represents the source of the service request.
     * @param callbacks An array of Callbacks for this Login state.
     * @return Whether or not the internal processing has succeeded or not.
     * @throws LoginException If the login process fails.
     */
    protected abstract boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks)
            throws LoginException;

    /**
     * Post processing of successful authentication, which initialises the underlying JASPI ServerAuthModule, as a new
     * instance of this class is created for the Post Authentication Process, and then calls the subtypes
     * onLoginSuccess method, and then finally calls the JASPI ServerAuthModule's secureResponse method.
     *
     * @param requestParamsMap {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @param ssoToken {@inheritDoc}
     * @throws AuthenticationException {@inheritDoc}
     */
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response,
            SSOToken ssoToken) throws AuthenticationException {

        try {
            Map<String, Object> config = initialize(requestParamsMap, request, response, ssoToken);

            serverAuthModule.initialize(createRequestMessagePolicy(), null, null, config);

            MessageInfo messageInfo = prepareMessageInfo(request, response);

            onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

            AuthStatus authStatus = serverAuthModule.secureResponse(messageInfo, null);

            if (AuthStatus.SEND_SUCCESS.equals(authStatus)) {
                // nothing to do here just carry on
                debug.message("Successfully secured response.");
            } else if (AuthStatus.SEND_FAILURE.equals(authStatus)) {
                // Send HttpServletResponse to client and exit.
                debug.message("Failed to secured response, included response message");
                throw new AuthenticationException(resourceBundleName, "authFailed", null);
            } else if (AuthStatus.SEND_CONTINUE.equals(authStatus)) {
                // Send HttpServletResponse to client and exit.
                debug.message("Has not finished securing response. Requires more information from client.");
                throw new AuthenticationException(resourceBundleName, "authFailed", null);
            } else {
                debug.error("Invalid AuthStatus, " + authStatus.toString());
                throw new AuthenticationException(resourceBundleName, "authFailed", null);
            }

        } catch (AuthException e) {
            debug.error("Authentication Failed", e);
            throw new AuthenticationException(resourceBundleName, "authFailed", null);
        }
    }

    /**
     * Initialises the underlying JASPI ServerAuthModule with the required configuration.
     *
     * @param requestParamsMap A Map containing the HttpServletRequest parameters.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param ssoToken The authentication user's SSOToken.
     * @return The Map of configuration information for the underlying JASPI ServerAuthModule.
     * @throws AuthenticationException If an error occurs.
     */
    protected abstract Map<String, Object> initialize(Map requestParamsMap, HttpServletRequest request,
            HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException;

    /**
     * Internal call to subtype to perform any required logic before the secureResponse method is called on the
     * underlying JASPI ServerAuthModule.
     *
     * @param messageInfo The ServerAuthModules MessageInfo instance.
     * @param requestParamsMap A Map containing the HttpServletRequest parameters.
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param ssoToken The authentication user's SSOToken.
     * @throws AuthenticationException If an error occurs.
     */
    protected abstract void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
            HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException;

    /**
     * Post processing on failed authentication.
     *
     * @param requestParamsMap {@inheritDoc}
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @throws AuthenticationException {@inheritDoc}
     */
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
    }

    /**
     * Post processing on Logout.
     *
     * @param request {@inheritDoc}
     * @param response {@inheritDoc}
     * @param ssoToken {@inheritDoc}
     */
    public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) {
    }

    /**
     * Gets the underlying JASPI ServerAuthModule instance.
     *
     * @return The JASPI ServerAuthModule instance.
     */
    protected T getServerAuthModule() {
        return serverAuthModule;
    }

    /**
     * Creates a MessagePolicy instance.
     *
     * @return A MessagePolicy instance.
     */
    protected MessagePolicy createRequestMessagePolicy() {
        MessagePolicy.Target[] targets = new MessagePolicy.Target[]{};
        MessagePolicy.ProtectionPolicy protectionPolicy = new MessagePolicy.ProtectionPolicy() {
            @Override
            public String getID() {
                return MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;
            }
        };
        MessagePolicy.TargetPolicy targetPolicy = new MessagePolicy.TargetPolicy(targets, protectionPolicy);
        MessagePolicy.TargetPolicy[] targetPolicies = new MessagePolicy.TargetPolicy[]{targetPolicy};
        return new MessagePolicy(targetPolicies, true);
    }

    /**
     * Creates a MessageInfo instance containing the given HttpServletRequest and HttpServletResponse.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @return A MessageInfo instance.
     */
    protected MessageInfo prepareMessageInfo(final HttpServletRequest request, final HttpServletResponse response) {
        final HashMap<Object, Object> properties = new HashMap<>();
        return new MessageInfo() {

            @Override
            public Object getRequestMessage() {
                return request;
            }

            @Override
            public Object getResponseMessage() {
                return response;
            }

            @Override
            public void setRequestMessage(Object ignored) {
                //Not able to set request
            }

            @Override
            public void setResponseMessage(Object ignored) {
                //Not able to set request
            }

            @Override
            public Map getMap() {
                return properties;
            }
        };
    }
}
