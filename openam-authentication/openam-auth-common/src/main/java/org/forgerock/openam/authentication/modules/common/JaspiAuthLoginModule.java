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

package org.forgerock.openam.authentication.modules.common;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * A class which wraps the JASPI Authentication Module framework, allowing JASPI Authentication Modules
 * to be used with JAAS Authentication Modules and the OpenAM Post Authentication Procoess.
 */
public abstract class JaspiAuthLoginModule extends AuthLoginModule {

    private final Debug debug;

    private final String resourceBundleName;
    private final JaspiAuthModuleWrapper jaspiAuthModule;

    /**
     * Constructs an instance of the JaspiAuthModuleWrapper.
     *
     * @param resourceBundleName The name of the authentication module's resource bundle.
     * @param jaspiAuthModule TODO
     */
    public JaspiAuthLoginModule(String resourceBundleName,
                                JaspiAuthModuleWrapper jaspiAuthModule) {
        this.resourceBundleName = resourceBundleName;
        this.jaspiAuthModule = jaspiAuthModule;
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
            Map<String, Object> config = generateConfig(subject, sharedState, options);

            jaspiAuthModule.initialize(getCallbackHandler(), config);
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
    protected abstract Map<String, Object> generateConfig(Subject subject, Map sharedState, Map options);

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

        if (state == ISAuthConstants.LOGIN_START) {
            final Subject clientSubject = new Subject();
            MessageInfo messageInfo = jaspiAuthModule.prepareMessageInfo(getHttpServletRequest(),
                    getHttpServletResponse());
            if (process(messageInfo, clientSubject, callbacks)) {
                AuthStatus authStatus = jaspiAuthModule.validateRequest(messageInfo, clientSubject);

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
        throw new AuthLoginException(resourceBundleName, "incorrectState", null);
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

}
