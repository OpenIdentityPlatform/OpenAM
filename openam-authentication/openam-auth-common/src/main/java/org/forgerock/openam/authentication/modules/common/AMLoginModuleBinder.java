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
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.authentication.modules.common;

import javax.security.auth.callback.CallbackHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

/**
 * The idea behind this interface is that implementations of this interface will also extend the AMLoginModule
 * class and be instantiated with an instance of the AuthLoginModule interface and the implementation of this
 * interface will then delegate the AMLoginModule calls to the AuthLoginModule instance.
 *
 * This allows the implementation of the AuthLoginModule to be unit testable as the AMLoginModule instance is abstracted
 * away.
 *
 * @see AbstractLoginModuleBinder
 */
public interface AMLoginModuleBinder {

    /**
     * Returns the CallbackHandler object for the module.
     *
     * @return CallbackHandler for this request, returns null if the CallbackHandler object could not be obtained.
     */
    CallbackHandler getCallbackHandler();

    /**
     * Returns the HttpServletRequest object that initiated the call to this module.
     *
     * @return HttpServletRequest for this request, returns null if the HttpServletRequest object could not be obtained.
     */
    HttpServletRequest getHttpServletRequest();

    /**
     * Returns the HttpServletResponse object for the servlet request that initiated the call to this module. The
     * servlet response object will be the response to the HttpServletRequest received by the authentication module.
     *
     * @return HttpServletResponse for this request, returns null if the HttpServletResponse object could not be
     * obtained.
     */
    HttpServletResponse getHttpServletResponse();

    /**
     * Returns the organization DN for this authentication session.
     *
     * @return The organization DN.
     */
    String getRequestOrg();

    /**
     * Sets a property in the user session. If the session is being force upgraded then set on the old session
     * otherwise set on the current session.
     *
     * @param name The property name.
     * @param value The property value.
     * @throws AuthLoginException If the user session is invalid.
     */
    void setUserSessionProperty(String name, String value) throws AuthLoginException;

    /**
     * Returns the username of the currently authenticating user, if known. This can be set by authentication modules
     * using the {@link #setAuthenticatingUserName(String)} method to communicate the username with subsequent
     * modules in the authentication chain.
     * <p>
     * Note that the username returned here is based on user input, and it may not correspond to the user's actual
     * username determined by the data store.
     *
     * @return the name of the user currently authenticating, if known, or {@code null} if not supplied.
     * @see ISAuthConstants#SHARED_STATE_USERNAME
     */
    String getAuthenticatingUserName();

    /**
     * Sets the username of the user that is currently authenticating as determined by the current login module.
     *
     * @param username the name of the currently authenticating user.
     * @see AMLoginModule#storeUsername(String)
     */
    void setAuthenticatingUserName(String username);
}
