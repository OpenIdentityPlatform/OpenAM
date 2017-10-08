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
package org.forgerock.openam.authentication.modules.push.registration;

/**
 * Constants used by the Authenticator Push Registration Module.
 */
final class Constants {

    /**
     * Uninstantiable.
     */
    private Constants() {
        //This section intentionally left blank.
    }

    /**
     * VALUES.
     */

    /** The number of recovery codes to generate for a newly minted device. */
    static final int NUM_RECOVERY_CODES = 10;

    /**
     * KEYS.
     */

    /** The name of the AuthenticatorPush authentication registration module for debug logging purposes. */
    public static final String AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION = "amAuthAuthenticatorPushRegistration";
    /** Module configuration key for push timeout. */
    static final String DEVICE_PUSH_WAIT_TIMEOUT = "forgerock-am-auth-push-message-registration-response-timeout";
    /** The name of the Auth Level key for the Authenticator Push registration. */
    static final String AUTHLEVEL = "forgerock-am-auth-push-reg-auth-level";
    /** The name of the Issuer key for the Authenticator Push registration. */
    static final String ISSUER_OPTION_KEY = "forgerock-am-auth-push-reg-issuer";
    /** The name of the background colour key for the Authenticator Push registration. */
    static final String BGCOLOUR = "forgerock-am-auth-hex-bgcolour";
    /** The name of the img url key for the Authenticator Push registration. */
    static final String IMG_URL = "forgerock-am-auth-img-url";
    /** The name of the apple link key for the Authenticator Push registration. */
    static final String APPLE_LINK = "forgerock-am-auth-apple-link";
    /** The name of the google link key for the Authenticator Push registration. */
    static final String GOOGLE_LINK = "forgerock-am-auth-google-link";

    /**
     * STATES.
     */

    /** State to register device or get the App page. */
    static final int STATE_OPTIONS = 2;
    /** State to display an error message to the end-user. */
    static final int STATE_GET_THE_APP = 3;
    /** State to gather username if not already supplied. */
    static final int STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN = 4;
    /** State to gather username if not already supplied. */
    static final int STATE_CONFIRMATION = 5;

    /**
     * CALLBACK OPTIONS.
     */

    /** Option begin the registration process now. */
    public static final int START_REGISTRATION_OPTION = 0;
    /** Option to navigate to the get the app page. */
    public static final int GET_THE_APP_OPTION = 1;

    /** Index to use to access the QR callback placeholder. */
    public static final int SCRIPT_OUTPUT_CALLBACK_INDEX = 1;
    /** Index to use to access the wait period callback placeholder. */
    public static final int POLLING_TIME_OUTPUT_CALLBACK_INDEX = 2;

    /** Index to use to access the apple link callback placeholder. */
    public static final int APPLE_LINK_CALLBACK_INDEX = 0;
    /** Index to use to access the google link callback placeholder. */
    public static final int GOOGLE_LINK_CALLBACK_INDEX = 1;

    /**
     * QR CODE KEYS.
     */

    /** The key for the Message Id query component of the QR code. */
    static final String MESSAGE_ID_QR_CODE_KEY = "m";
    /** The key for the shared secret query component of the QR code. */
    static final String SHARED_SECRET_QR_CODE_KEY = "s";
    /** The key for the bgcolour query component of the QR code. */
    static final String BGCOLOUR_QR_CODE_KEY = "b";
    /** The key for the Issuer query component of the QR code. */
    static final String REG_QR_CODE_KEY = "r";
    /** The key for the Issuer query component of the QR code. */
    static final String AUTH_QR_CODE_KEY = "a";
    /** The key for the Issuer query component of the QR code. */
    static final String IMG_QR_CODE_KEY = "image";
    /** The key for the loadbalancer information component of the QR code. */
    static final String LOADBALANCER_DATA_QR_CODE_KEY = "l";
    /** The key for the challenge inside the registration challenge. */
    static final String CHALLENGE_QR_CODE_KEY = "c";
    /** The key for the total JWS challenge for registration. */
    static final String ISSUER_QR_CODE_KEY = "issuer";
    /** The key for the time to live. */
    static final String TIME_TO_LIVE_QR_CODE_KEY = "t";
}
