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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.authentication.modules.push;

/**
 * Constants for the ForgeRock Authenticator (Push) Auth Module.
 */
final class Constants {

    /**
     * Uninstantiable.
     */
    private Constants() {
        //This section intentionally left blank.
    }

    /**
     * KEYS.
     */
    /** The Name of the AuthenticatorPush authentication module for debug logging purposes. */
    static final String AM_AUTH_AUTHENTICATOR_PUSH = "amAuthAuthenticatorPush";
    /** Module configuration key for push timeout. */
    static final String DEVICE_PUSH_WAIT_TIMEOUT = "forgerock-am-auth-push-message-response-timeout";
    /** Module configuration key for authentication level of module. */
    static final String AUTH_LEVEL = "forgerock-am-auth-push-auth-level";

    /**
     * STATES.
     */
    /** State to gather username if not already supplied. */
    static final int USERNAME_STATE = 2;
    /** State to display please wait message to the  end-user. */
    static final int STATE_WAIT = 3;

    /** The Position of the Polling callback in the callbacks step in the xml for this module. */
    static final int POLLING_CALLBACK_POSITION = 1;

}
