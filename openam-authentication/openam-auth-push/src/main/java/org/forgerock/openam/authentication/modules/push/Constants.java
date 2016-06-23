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
public final class Constants {

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
    public static final String AM_AUTH_AUTHENTICATOR_PUSH = "amAuthAuthenticatorPush";
    /** Module configuration key for push timeout. */
    static final String DEVICE_PUSH_WAIT_TIMEOUT = "forgerock-am-auth-push-message-response-timeout";
    /** Module configuration key for authentication level of module. */
    static final String AUTH_LEVEL = "forgerock-am-auth-push-auth-level";
    /** Module configuration key for push message. */
    static final String DEVICE_PUSH_MESSAGE = "forgerock-am-auth-push-message";

    /**
     * MESSAGE CODE KEYS.
     */

    /** The key for the mechanism id. */
    static final String MECHANISM_ID_KEY = "u";
    /** The key for the challenge. */
    static final String CHALLENGE_KEY = "c";
    /** The key for the loadbalancer. */
    static final String LOADBALANCER_KEY = "l";
    /** The key for the time to live. */
    static final String TIME_TO_LIVE_KEY = "t";

    /**
     * STATES.
     */

    /** State to gather username if not already supplied. */
    static final int USERNAME_STATE = 2;
    /** State to display please wait message to the  end-user. */
    static final int STATE_WAIT = 3;
    /** State to use an emergency code. */
    static final int STATE_EMERGENCY = 4;
    /** State when an emergency code has been used. */
    static final int STATE_EMERGENCY_USED = 5;

    /**
     * CALLBACKS.
     */

    /** The Position of the Polling callback in the callbacks step in the xml for this module. */
    static final int POLLING_CALLBACK_POSITION = 0;


    /** The return value submitted when the emergency button is pressed. */
    static final int EMERGENCY_PRESSED = 0;
    /** The default return for when we're auto-submitting the same page, rather than navigating away
     * via the emergency code button. */
    static final int EMERGENCY_NOT_PRESSED = 100;
    /** Text for the emergency code button. */
    static final String[] USE_EMERGENCY_CODE = new String[] { "Use Emergency Code" };
    /** Location of the emergency button callback in XML. */
    static final int EMERGENCY_CALLBACK_POSITION = 1;
    /** Location of the input field for recovery code. */
    static final int RECOVERY_CODE_CALLBACK_POSITION = 0;
    /** Location of the username field for name input. */
    static final int USERNAME_CALLBACK_LOCATION_POSITION = 0;

    /**
     * CONFIG.
     */

    /** Length of a generic secret key (in bytes). */
    public static final int SECRET_BYTE_LENGTH = 32;

}
