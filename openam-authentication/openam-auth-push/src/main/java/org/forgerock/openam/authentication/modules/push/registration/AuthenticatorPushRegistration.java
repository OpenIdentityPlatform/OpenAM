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

import static org.forgerock.openam.authentication.modules.push.registration.Constants.*;
import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.PollingWaitAssistant;
import org.forgerock.openam.authentication.callbacks.helpers.QRCallbackBuilder;
import org.forgerock.openam.authentication.modules.push.AuthenticatorPushPrincipal;
import org.forgerock.openam.authentication.modules.push.UserPushDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.util.encode.Base64url;
import org.forgerock.util.promise.Promise;

/**
 * The Authenticator Push Registration Module is a registration module that does not authenticate a user but
 * allows a user already authenticated earlier in the chain to register their mobile device.
 */
public class AuthenticatorPushRegistration extends AMLoginModule {

    private static final Debug DEBUG = Debug.getInstance("amAuthPush");

    private final MessageDispatcher messageResponseHandler = InjectorHolder.getInstance(MessageDispatcher.class);
    private final UserPushDeviceProfileManager userDeviceHandler
            = InjectorHolder.getInstance(UserPushDeviceProfileManager.class);
    private final PushNotificationService pushMessageSendingService
            = InjectorHolder.getInstance(PushNotificationService.class);

    private PollingWaitAssistant pollingWaitAssistant;

    private AMIdentity amIdentityPrincipal;
    private PushDeviceSettings newDeviceRegistrationProfile;
    private Promise<JsonValue, Exception> deviceResponsePromise;
    private String issuer;

    private String bgColour;
    private String imgUrl;

    @Override
    public void init(final Subject subject, final Map sharedState, final Map options) {
        DEBUG.message("{}::init", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);

        final String authLevel = CollectionHelper.getMapAttr(options, AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                DEBUG.error("{} :: init() : Unable to set auth level {}",
                        AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, authLevel, e);
            }
        }
        this.issuer = CollectionHelper.getMapAttr(options, ISSUER_OPTION_KEY);
        this.imgUrl = CollectionHelper.getMapAttr(options, IMG_URL);
        this.bgColour = CollectionHelper.getMapAttr(options, BGCOLOUR);

        if (bgColour != null && bgColour.startsWith("#")) {
            bgColour = bgColour.substring(1);
        }

        amIdentityPrincipal = establishPreauthenticatedUser(sharedState);
        pollingWaitAssistant = setUpPollingWaitCallbackAssistant(options);

        try {
            pushMessageSendingService.init(amIdentityPrincipal.getRealm());
        } catch (PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: initialiseService() : Unable to initialiseService Push system.", e);
        }

    }

    private PollingWaitAssistant setUpPollingWaitCallbackAssistant(final Map options) {
        final long timeoutInMilliSeconds = Long.valueOf(CollectionHelper.getMapAttr(options, DEVICE_PUSH_WAIT_TIMEOUT));
        return new PollingWaitAssistant(timeoutInMilliSeconds);
    }

    private AMIdentity establishPreauthenticatedUser(final Map sharedState) {
        final String subjectName = (String) sharedState.get(getUserKey());
        final String realm = DNMapper.orgNameToRealmName(getRequestOrg());
        return IdUtils.getIdentity(subjectName, realm);
    }

    @Override
    public int process(final Callback[] callbacks, final int state) throws LoginException {
        final HttpServletRequest request = getHttpServletRequest();

        if (request == null) {
            DEBUG.error("{} :: process() : Request was null.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw failedAsLoginException();
        }

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            return STATE_OPTIONS;
        case STATE_OPTIONS:
            return navigateOptions(callbacks);
        case STATE_GET_THE_APP:
            return startRegistration();
        case STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN:
            return awaitState();
        case STATE_CONFIRMATION:
            return ISAuthConstants.LOGIN_SUCCEED;
        default:
            DEBUG.error("{} :: process() : Invalid state.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw failedAsLoginException();
        }
    }

    private int navigateOptions(Callback[] callbacks) throws AuthLoginException {
        if (null == callbacks || callbacks.length < 1) {
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }

        switch (((ConfirmationCallback) callbacks[0]).getSelectedIndex()) {
        case START_REGISTRATION_OPTION:
            return startRegistration();
        case GET_THE_APP_OPTION:
            return STATE_GET_THE_APP;
        default:
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }
    }

    private int startRegistration() throws AuthLoginException {

        newDeviceRegistrationProfile = userDeviceHandler.createDeviceProfile();

        String registrationConversationId = UUID.randomUUID().toString();

        paintRegisterDeviceCallback(amIdentityPrincipal, registrationConversationId);
        this.deviceResponsePromise = messageResponseHandler.expect(registrationConversationId);
        pollingWaitAssistant.start(deviceResponsePromise);

        return STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN;
    }

    private int awaitState() throws AuthLoginException {

        switch (pollingWaitAssistant.getPollingWaitState()) {
        case TOO_EARLY:
            return STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN;
        case NOT_STARTED:
        case WAITING:
            setPollbackTimePeriod(pollingWaitAssistant.getWaitPeriod());
            pollingWaitAssistant.resetWait();
            return STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN;
        case COMPLETE:
            saveDeviceDetailsUnderUserAccount();
            return STATE_CONFIRMATION;
        case TIMEOUT:
            DEBUG.warning("{} :: timeout value exceeded while waiting for response.",
                    AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        case SPAMMED:
            DEBUG.warning("{} :: too many requests sent to Auth module.  "
                    + "Client should obey wait time.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION);
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        default:
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }
    }

    private void saveDeviceDetailsUnderUserAccount() throws AuthLoginException {

        try {
            JsonValue deviceResponse = deviceResponsePromise.get();

            newDeviceRegistrationProfile.setDeviceName(deviceResponse.get(DEVICE_NAME_JSON_POINTER).asString());
            newDeviceRegistrationProfile.setCommunicationId(deviceResponse.get(
                    DEVICE_COMMUNICATION_ID_JSON_POINTER).asString());
            newDeviceRegistrationProfile.setDeviceMechanismUID(deviceResponse.get(
                    DEVICE_MECHANISM_UID_JSON_POINTER).asString());
            newDeviceRegistrationProfile.setCommunicationType(deviceResponse.get(
                    DEVICE_COMMUNICATION_TYPE_JSON_POINTER).asString());
            newDeviceRegistrationProfile.setDeviceType(deviceResponse.get(
                    DEVICE_TYPE_JSON_POINTER).asString());
            newDeviceRegistrationProfile.setDeviceId(deviceResponse.get(
                    DEVICE_ID_JSON_POINTER).asString());
            newDeviceRegistrationProfile.setRecoveryCodes(DeviceSettings.generateRecoveryCodes(NUM_RECOVERY_CODES));

            userDeviceHandler.saveDeviceProfile(
                    amIdentityPrincipal.getName(), amIdentityPrincipal.getRealm(), newDeviceRegistrationProfile);

        } catch (InterruptedException | ExecutionException e) {
            DEBUG.error("{} :: Failed to save device settings.", AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, e);
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
        }

    }

    private void paintRegisterDeviceCallback(AMIdentity id, String messageId) throws AuthLoginException {

        replaceCallback(
                STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN,
                SCRIPT_OUTPUT_CALLBACK_INDEX,
                createQRCodeCallback(newDeviceRegistrationProfile, id, messageId, SCRIPT_OUTPUT_CALLBACK_INDEX));
    }

    private Callback createQRCodeCallback(PushDeviceSettings deviceProfile, AMIdentity id, String messageId,
                                          int callbackIndex) {
        return new QRCallbackBuilder().withUriScheme("pushauth")
                .withUriHost("push")
                .withUriPath("forgerock")
                .withUriPort(id.getName())
                .withCallbackIndex(callbackIndex)
                .addUriQueryComponent(ISSUER_QR_CODE_KEY, issuer)
                .addUriQueryComponent(MESSAGE_ID_QR_CODE_KEY, messageId)
                .addUriQueryComponent(SHARED_SECRET_QR_CODE_KEY, deviceProfile.getSharedSecret())
                .addUriQueryComponent(BGCOLOUR_QR_CODE_KEY, bgColour)
                .addUriQueryComponent(REG_QR_CODE_KEY,
                        getMessageResponseUrl(pushMessageSendingService.getRegServiceAddress(id.getRealm())))
                .addUriQueryComponent(AUTH_QR_CODE_KEY,
                        getMessageResponseUrl(pushMessageSendingService.getAuthServiceAddress(id.getRealm())))
                .addUriQueryComponent(IMG_QR_CODE_KEY, Base64url.encode(imgUrl.getBytes()))
                .build();
    }

    private String getMessageResponseUrl(String component) {
        String localServerURL = WebtopNaming.getLocalServer() + "/json/";
        return Base64url.encode((localServerURL + component).getBytes());
    }

    private AuthLoginException failedAsLoginException() throws AuthLoginException {
        setFailureID(amIdentityPrincipal.getName());
        throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH_REGISTRATION, "authFailed", null);
    }

    @Override
    public Principal getPrincipal() {
        return new AuthenticatorPushPrincipal(amIdentityPrincipal.getName());
    }

    private void setPollbackTimePeriod(long periodInMilliseconds) throws AuthLoginException {

        Callback[] callback = getCallback(STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN);
        PollingWaitCallback newPollingWaitCallback = PollingWaitCallback.makeCallback()
                .asCopyOf((PollingWaitCallback) callback[POLLING_TIME_OUTPUT_CALLBACK_INDEX])
                .withWaitTime(String.valueOf(periodInMilliseconds))
                .build();
        replaceCallback(STATE_WAIT_FOR_RESPONSE_FROM_QR_SCAN,
                POLLING_TIME_OUTPUT_CALLBACK_INDEX, newPollingWaitCallback);
    }
}
