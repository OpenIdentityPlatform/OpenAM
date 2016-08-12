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

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import com.sun.identity.authentication.spi.AMLoginModule;

/**
 * Implementation of the AMLoginModuleBinder, as per described in the interface's JavaDoc, also extends
 * the AMLoginModule, with a constructor which takes an instance of a AuthLoginModule which the calls from the
 * AMLoginModule will be pass through too.
 *
 * This is to enable unit testing on the actual authentication logic of the Authentication Module.
 */
public abstract class AbstractLoginModuleBinder extends AMLoginModule implements AMLoginModuleBinder {

    private final AuthLoginModule authLoginModule;
    private Map sharedState;

    /**
     * Constructs an instance of the AbstractLoginModuleBinder.
     *
     * @param authLoginModule An instance of the AuthLoginModule.
     */
    public AbstractLoginModuleBinder(AuthLoginModule authLoginModule) {
        this.authLoginModule = authLoginModule;
        this.authLoginModule.setAMLoginModule(this);
    }

    /**
     * Initialize this AuthLoginModule.
     * <p>
     * This is an abstract method, must be implemented by user's Login Module to initialize this AuthLoginModule with
     * the relevant information. If this AuthLoginModule does not understand any of the data stored in sharedState or
     * options parameters, they can be ignored.
     *
     * @param subject The Subject to be authenticated.
     * @param sharedState The state shared with other configured LoginModules.
     * @param options The options specified in the login Configuration for this particular AuthLoginModule. It contains
     *                all the global and organization attribute configuration for this module. The key of the map is the
     *                attribute name (e.g. <code>iplanet-am-auth-ldap-server</code>) as String, the value is the value
     *                of the corresponding attribute as Set.
     */
    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        this.sharedState = sharedState;
        authLoginModule.init(subject, sharedState, options);
    }

    /**
     * Abstract method must be implemented by each login module to control the flow of the login process.
     * <p>
     * This method takes an array of submitted Callback, process them and decide the order of next state to go.
     * Return -1 if the login is successful, return 0 if the AuthLoginModule should be ignored.
     *
     * @param callbacks The array of Callbacks for this Login state.
     * @param state The order of state. State order starts with 1.
     * @return order of next state. Return -1 if authentication is successful, return 0 if the AuthLoginModule should be
     *              ignored.
     * @exception LoginException If the login process fails.
     */
    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        return authLoginModule.process(callbacks, state);
    }

    /**
     * Abstract method must be implemented by each login module to get the user Principal.
     *
     * @return The Principal.
     */
    @Override
    public Principal getPrincipal() {
        return authLoginModule.getPrincipal();
    }

    @Override
    public String getAuthenticatingUserName() {
        if (sharedState != null) {
            return (String) sharedState.get(getUserKey());
        }
        return null;
    }

    @Override
    public void setAuthenticatingUserName(final String username) {
        storeUsername(username);
    }
}
