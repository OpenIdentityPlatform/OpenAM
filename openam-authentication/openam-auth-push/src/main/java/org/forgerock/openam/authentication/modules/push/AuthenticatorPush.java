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

import static org.forgerock.openam.authentication.modules.push.Constants.*;
import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.DNMapper;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.PollingWaitAssistant;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationConstants;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.dispatch.MessagePromise;
import org.forgerock.openam.services.push.dispatch.Predicate;
import org.forgerock.openam.services.push.dispatch.PushMessageChallengeResponsePredicate;
import org.forgerock.openam.services.push.dispatch.SignedJwtVerificationPredicate;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * ForgeRock Authentication (Push) Authentication Module.
 */
public class AuthenticatorPush extends AbstractPushModule {

    private static final Debug DEBUG = Debug.getInstance("amAuthPush");

    //From config
    private Map<String, String> sharedState;

    //Internal state
    private String realm;
    private String username;
    private Principal principal;
    private String lbCookieValue;
    private String lbCookieName;
    private long timeout;
    private String messageId;
    private MessagePromise messagePromise;
    private String issuer;

    private PushDeviceSettings device;

    private PollingWaitAssistant pollingWaitAssistant;

    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        this.sharedState = sharedState;
        timeout = Long.valueOf(CollectionHelper.getMapAttr(options, DEVICE_PUSH_WAIT_TIMEOUT));
        issuer = CollectionHelper.getMapAttr(options, DEVICE_PUSH_ISSUER);

        this.realm = DNMapper.orgNameToRealmName(getRequestOrg());

        try {
            pushService.init(realm);
        } catch (PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: init() : Unable to init Push system.", e);
        }

        try {
            lbCookieValue = sessionCookies.getLBCookie(getSessionId());
        } catch (SessionException e) {
            DEBUG.warning("AuthenticatorPush :: init() : Unable to determine loadbalancer bookie value", e);
        }

        lbCookieName = sessionCookies.getLBCookieName();

        pollingWaitAssistant = new PollingWaitAssistant(timeout, 10000, 8000, 15000);

        String authLevel = CollectionHelper.getMapAttr(options, AUTH_LEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                DEBUG.error("AuthenticatorPush :: init() : Unable to set auth level {}", authLevel, e);
            }
        }
    }

    @Override
    public int process(final Callback[] callbacks, int state) throws LoginException {

        final HttpServletRequest request = getHttpServletRequest();

        if (request == null) {
            DEBUG.error("AuthenticatorPush :: process() : Request was null.");
            throw failedAsLoginException();
        }

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            return loginStart();
        case USERNAME_STATE:
            return usernameState(callbacks);
        case STATE_WAIT:
            return stateWait(callbacks);
        case STATE_EMERGENCY:
            return emergencyState(callbacks, username, realm);
        case STATE_EMERGENCY_USED:
            storeUsername(username);
            return ISAuthConstants.LOGIN_SUCCEED;
        default:
            DEBUG.error("AuthenticatorPush :: process() : Invalid state.");
            throw failedAsLoginException();
        }
    }

    private int stateWait(Callback[] callbacks) throws AuthLoginException {
        checkDeviceExists();
        if (emergencyPressed(callbacks)) {
            return STATE_EMERGENCY;
        } else {
            return pollForResponse();
        }
    }


    private boolean emergencyPressed(Callback[] callbacks) {
        ConfirmationCallback callback = (ConfirmationCallback) callbacks[2];
        return callback.getSelectedIndex() == EMERGENCY_PRESSED;
    }

    private void checkDeviceExists() throws AuthLoginException {
        if (device == null) {
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }
    }

    private int emergencyState(Callback[] callbacks, String username, String realm) throws AuthLoginException {

        NameCallback emergencyCode = (NameCallback) callbacks[0];
        String codeAttempt = emergencyCode.getName();

        List<String> recoveryCodes = new ArrayList<>(Arrays.asList(device.getRecoveryCodes()));
        if (recoveryCodes.contains(codeAttempt)) {
            recoveryCodes.remove(codeAttempt);
            device.setRecoveryCodes(recoveryCodes.toArray(new String[recoveryCodes.size()]));
            userPushDeviceProfileManager.saveDeviceProfile(username, realm, device);

            return STATE_EMERGENCY_USED;
        }

        throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
    }

    private int pollForResponse() throws AuthLoginException {

        switch (pollingWaitAssistant.getPollingWaitState()) {
        case TOO_EARLY:
            setEmergencyButton();
            return STATE_WAIT;
        case NOT_STARTED:
        case WAITING:
            return waitingChecks();
        case COMPLETE:
            return completeChecks();
        case TIMEOUT:
            DEBUG.warning("AuthenticatorPush :: timeout value exceeded while waiting for response.");
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        case SPAMMED:
            DEBUG.warning("AuthenticatorPush :: too many requests sent to Auth module.  "
                    + "Client should obey wait time.");
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        default:
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }
    }

    private int completeChecks() throws AuthLoginException {
        try {
            Jwt signedJwt = new JwtReconstruction().reconstructJwt(
                    messagePromise.getPromise().get().get(JWT).asString(),  Jwt.class);
            Boolean deny = (Boolean) signedJwt.getClaimsSet().getClaim(PushNotificationConstants.DENY_LOCATION);
            if (deny != null && deny) { //denied
                throw failedAsLoginException();
            } else {
                storeUsername(username);
                return ISAuthConstants.LOGIN_SUCCEED;
            }
        } catch (Exception e) {
            DEBUG.error("Reading JWT from local MessageDispatcher failed.", e);
        }

        throw failedAsLoginException();
    }

    private int waitingChecks() throws AuthLoginException {
        try {
            Boolean ctsValue = checkCTS(messageId);
            if (ctsValue != null) {
                messageDispatcher.forget(messageId);

                if (ctsValue) {
                    storeUsername(username);
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else { //denied
                    throw failedAsLoginException();
                }
            }
        } catch (CoreTokenException e) {
            DEBUG.warning("CTS threw exception, falling back to local MessageDispatcher.", e);
        }

        setPollbackTimePeriod(pollingWaitAssistant.getWaitPeriod());
        pollingWaitAssistant.resetWait();
        setEmergencyButton();
        return STATE_WAIT;
    }

    private int loginStart() throws AuthLoginException {

        if (username == null && sharedState != null) {
            username = sharedState.get(getUserKey());
        }

        if (username == null) {
            return USERNAME_STATE;
        } else {
            device = getDevice(username, realm);
            if (sendMessage(device)) {
                setEmergencyButton();
                return STATE_WAIT;
            } else {
                DEBUG.warning("AuthenticatorPush :: sendState() : Failed to send message.");
                throw failedAsLoginException();
            }
        }
    }

    private int usernameState(Callback[] callbacks) throws AuthLoginException {
        Reject.ifNull(callbacks);
        NameCallback nameCallback = (NameCallback) callbacks[0];
        username = nameCallback.getName();

        if (StringUtils.isBlank(username)) {
            DEBUG.warning("AuthenticatorPush :: usernameState() : Username was blank.");
            throw failedAsLoginException();
        }

        AMIdentity id = IdUtils.getIdentity(username, realm);

        try {
            if (id != null && id.isExists() && id.isActive()) {
                principal = new AuthenticatorPushPrincipal(username);
                return ISAuthConstants.LOGIN_START;
            }
        } catch (IdRepoException | SSOException e) {
            DEBUG.warning("AuthenticatorPush :: Failed to locate user {} ", username, e);
        }

        throw failedAsLoginException();
    }

    private boolean sendMessage(PushDeviceSettings device) {

        String communicationId = device.getCommunicationId();
        String mechanismId = device.getDeviceMechanismUID();

        String challenge = userPushDeviceProfileManager.createRandomBytes(SECRET_BYTE_LENGTH);

        JwtClaimsSetBuilder jwtClaimsSetBuilder = new JwtClaimsSetBuilder()
                .claim(Constants.MECHANISM_ID_KEY, mechanismId)
                .claim(LOADBALANCER_KEY,
                        Base64.encode((lbCookieName + "=" + lbCookieValue).getBytes()))
                .claim(CHALLENGE_KEY, challenge)
                .claim(TIME_TO_LIVE_KEY, String.valueOf(timeout / 1000));

        String jwt = new SignedJwtBuilderImpl(new SigningManager()
                .newHmacSigningHandler(Base64.decode(device.getSharedSecret())))
                .claims(jwtClaimsSetBuilder.build())
                .headers().alg(JwsAlgorithm.HS256).done().build();

        PushMessage message = new PushMessage(communicationId, jwt,
                "Login Attempt from " + username + " at " + issuer);

        Set<Predicate> servicePredicates = new HashSet<>();
        servicePredicates.add(
                new SignedJwtVerificationPredicate(Base64.decode(device.getSharedSecret()), JWT));
        servicePredicates.add(
                new PushMessageChallengeResponsePredicate(Base64.decode(device.getSharedSecret()), challenge, JWT));

        try {
            messagePromise = messageDispatcher.expect(message.getMessageId(), servicePredicates);
            pushService.send(message, realm);
            pollingWaitAssistant.start(messagePromise.getPromise());

            servicePredicates.addAll(pushService.getAuthenticationMessagePredicatesFor(realm));

            storeInCTS(message.getMessageId(), servicePredicates, timeout);
            messageId = message.getMessageId();

        } catch (PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: sendMessage() : Failed to transmit message through PushService.");
            return false;
        } catch (JsonProcessingException | CoreTokenException e) {
            DEBUG.warning("Unable to persist token in core token service.", e);
        }

        return true;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    private AuthLoginException failedAsLoginException() throws AuthLoginException {
        setFailureID(username);
        return new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
    }

    private void setPollbackTimePeriod(long periodInMilliseconds) throws AuthLoginException {

        PollingWaitCallback newPollingWaitCallback = PollingWaitCallback.makeCallback()
                .asCopyOf((PollingWaitCallback) getCallback(STATE_WAIT)[POLLING_CALLBACK_POSITION])
                .withWaitTime(String.valueOf(periodInMilliseconds))
                .build();
        replaceCallback(STATE_WAIT, POLLING_CALLBACK_POSITION, newPollingWaitCallback);
    }

    private void setEmergencyButton() throws AuthLoginException {
        ConfirmationCallback confirmationCallback =
                new ConfirmationCallback(ConfirmationCallback.INFORMATION, USE_EMERGENCY_CODE, 0);
        confirmationCallback.setSelectedIndex(EMERGENCY_NOT_PRESSED);
        replaceCallback(STATE_WAIT, EMERGENCY_CALLBACK_POSITION, confirmationCallback);
    }
}
