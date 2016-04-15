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

import org.forgerock.json.JsonPointer;

/**
 * Constants used by the Authenticator Push Registration Module.
 */
final class Constants {

    private Constants() { }

    /**
     * KEYS.
     */
    /** The Name of the AuthenticatorPush authentication registration module for debug logging purposes. */
    static final String AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION = "amAuthAuthenticatorPushRegistration";
    /** Module configuration key for push timeout. */
    static final String DEVICE_PUSH_WAIT_TIMEOUT = "forgerock-am-auth-push-message-registration-response-timeout";
    /** The Name of the Auth Level key for the AuthenticatorPushRegistration authentication registration. */
    static final String AUTHLEVEL = "forgerock-am-auth-push-reg-auth-level";
    /** The name of the Issuer key for the AuthenticatorPushRegistration authentication registration. */
    static final String ISSUER_OPTION_KEY = "forgerock-am-auth-push-reg-issuer";

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
    /** State to display an error message to the end-user. */
    static final int ERROR_STATE = 6;

    /**
     * Callback Options
     */
    /** NAVIGATION */
    /** Option begin the registration process now. */
    public static final int START_REGISTRATION_OPTION = 1;
    /** Option to navigate to the get the app page. */
    public static final int GET_THE_APP_OPTION = 0;

    /** Index to use to access the QR callback placeholder. */
    public static final int SCRIPT_OUTPUT_CALLBACK_INDEX = 1;
    /** Index to use to access the wait period callback placeholder. */
    public static final int POLLING_TIME_OUTPUT_CALLBACK_INDEX = 2;

    /**
     * Messaging constants
     */
    /** Pointer to the location of the Device name in the mobile message. */
    static final JsonPointer DEVCIE_NAME_JSON_POINTER = new JsonPointer("data/devcieName");
    /** Pointer to the location of the Mobile platform Communication ID in the mobile message. */
    static final JsonPointer DEVICE_COMMUNICATION_ID_JSON_POINTER = new JsonPointer("data/communicationId");
    /** Pointer to the location of the login mechanism id in the mobile message. */
    static final JsonPointer DEVICE_MECHANISM_UID_JSON_POINTER = new JsonPointer("data/mechanismUid");
    /** The Return message  REST endpoint. */
    static final String MESSAGE_RESPONSE_ENDPOINT = "/json/push/gcm/message?_action=send";

    /**
     * QR code constants
     */
    /** The key to put the endpoint value in the QR URL. */
    static final String ENDPOINT_URL_KEY = "endpoint";
    /** The Key for the Message Id query component of the QR code. */
    static final String MESSAGE_ID_QR_CODE_KEY = "messageId";
    /** The Keu for the Issuer query component of the QR code. */
    static final String ISSUER_QR_CODE_KEY = "issuer";
}
