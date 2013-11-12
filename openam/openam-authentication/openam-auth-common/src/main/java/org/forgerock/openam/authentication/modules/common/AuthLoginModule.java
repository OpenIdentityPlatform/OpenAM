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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.common;

import com.sun.identity.authentication.spi.AuthLoginException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;

/**
 * Sub types of this class represent the actual Authentication Logic for the Authentication Module.
 * Sub types have to implement the same method signatures of the AMLoginModule, but without extending
 * the class itself, therefore enabling unit testing of the authetication logic.
 *
 * Add any required methods from the AMLoginModule that sub types of this class required to here and the
 * AMLoginModuleBinder interface.
 *
 * @author Phill Cunnington phill.cunnington@forgerock.com
 */
public abstract class AuthLoginModule {

    private AMLoginModuleBinder amLoginModuleBinder;

    /**
     * Constructs an instance of the AuthLoginModule.
     *
     * @param amLoginModuleBinder An instance of the AMLoginModuleBinder.
     */
    public void setAMLoginModule(AMLoginModuleBinder amLoginModuleBinder) {
        this.amLoginModuleBinder = amLoginModuleBinder;
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
    public abstract void init(Subject subject, Map sharedState, Map options);

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
    public abstract int process(Callback[] callbacks, int state) throws LoginException;

    /**
     * Abstract method must be implemented by each login module to get the user Principal.
     *
     * @return The Principal.
     */
    public abstract Principal getPrincipal();

    /**
     * Returns the CallbackHandler object for the module.
     *
     * @return CallbackHandler for this request, returns null if the CallbackHandler object could not be obtained.
     */
    public CallbackHandler getCallbackHandler() {
        return amLoginModuleBinder.getCallbackHandler();
    }

    /**
     * Returns the HttpServletRequest object that initiated the call to this module.
     *
     * @return HttpServletRequest for this request, returns null if the HttpServletRequest object could not be obtained.
     */
    public HttpServletRequest getHttpServletRequest() {
        return amLoginModuleBinder.getHttpServletRequest();
    }

    /**
     * Returns the HttpServletResponse object for the servlet request that initiated the call to this module. The
     * servlet response object will be the response to the HttpServletRequest received by the authentication module.
     *
     * @return HttpServletResponse for this request, returns null if the HttpServletResponse object could not be
     * obtained.
     */
    public HttpServletResponse getHttpServletResponse() {
        return amLoginModuleBinder.getHttpServletResponse();
    }

    /**
     * Returns the organization DN for this authentication session.
     *
     * @return The organization DN.
     */
    public String getRequestOrg() {
        return amLoginModuleBinder.getRequestOrg();
    }

    /**
     * Sets a property in the user session. If the session is being force upgraded then set on the old session
     * otherwise set on the current session.
     *
     * @param name The property name.
     * @param value The property value.
     * @throws AuthLoginException If the user session is invalid.
     */
    public void setUserSessionProperty(String name, String value) throws AuthLoginException {
        amLoginModuleBinder.setUserSessionProperty(name, value);
    }
}
