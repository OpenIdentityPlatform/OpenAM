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

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.DNMapper;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.PollingWaitAssistant;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.dispatch.MessagePromise;
import org.forgerock.openam.services.push.dispatch.Predicate;
import org.forgerock.openam.services.push.dispatch.PushMessageChallengeResponsePredicate;
import org.forgerock.openam.services.push.dispatch.SignedJwtVerificationPredicate;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * ForgeRock Authentication (Push) Authentication Module.
 */
public class AuthenticatorPush extends AMLoginModule {

    private static final Debug DEBUG = Debug.getInstance("amAuthPush");

    private final PushNotificationService pushService =
            InjectorHolder.getInstance(PushNotificationService.class);
    private final MessageDispatcher messageDispatcher =
            InjectorHolder.getInstance(MessageDispatcher.class);

    //From config
    private Map<String, String> sharedState;

    //Internal state
    private String realm;
    private String username;
    private Principal principal;

    private PollingWaitAssistant pollingWaitAssistant;
    private UserPushDeviceProfileManager userPushDeviceProfileManager =
            InjectorHolder.getInstance(UserPushDeviceProfileManager.class);

    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        this.sharedState = sharedState;
        long timeoutInMilliSeconds = Long.valueOf(CollectionHelper.getMapAttr(options, DEVICE_PUSH_WAIT_TIMEOUT));

        this.realm = DNMapper.orgNameToRealmName(getRequestOrg());

        try {
            pushService.init(realm);
        } catch (PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: init() : Unable to init Push system.", e);
        }

        pollingWaitAssistant = new PollingWaitAssistant(timeoutInMilliSeconds);

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
            return awaitState();
        default:
            DEBUG.error("AuthenticatorPush :: process() : Invalid state.");
            throw failedAsLoginException();
        }
    }

    private int awaitState() throws AuthLoginException {

        switch (pollingWaitAssistant.getPollingWaitState()) {
        case TOO_EARLY:
            return STATE_WAIT;
        case NOT_STARTED:
        case WAITING:
            setPollbackTimePeriod(pollingWaitAssistant.getWaitPeriod());
            pollingWaitAssistant.resetWait();
            return STATE_WAIT;
        case COMPLETE:
            storeUsername(username);
            return ISAuthConstants.LOGIN_SUCCEED;
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

    private int loginStart() throws AuthLoginException {

        if (username == null && sharedState != null) {
            username = sharedState.get(getUserKey());
        }

        if (username == null) {
            return USERNAME_STATE;
        } else {
            PushDeviceSettings device = getDevice();
            if (sendMessage(device)) {
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
                .claim(CHALLENGE_KEY, challenge);

        String jwt = new SignedJwtBuilderImpl(new SigningManager()
                .newHmacSigningHandler(Base64.decode(device.getSharedSecret())))
                .claims(jwtClaimsSetBuilder.build())
                .headers().alg(JwsAlgorithm.HS256).done().build();

        PushMessage message = new PushMessage(communicationId, jwt, "Authentication");

        Set<Predicate> servicePredicates = new HashSet<>();
        servicePredicates.add(
                new SignedJwtVerificationPredicate(Base64.decode(device.getSharedSecret()), DATA_JSON_POINTER));
        servicePredicates.add(
                new PushMessageChallengeResponsePredicate(Base64.decode(device.getSharedSecret()), challenge,
                        DATA_JSON_POINTER, DEBUG));

        try {

            servicePredicates.addAll(pushService.getAuthenticationMessagePredicatesFor(realm));

            MessagePromise promise = messageDispatcher.expect(message.getMessageId(), servicePredicates);
            pushService.send(message, realm);
            pollingWaitAssistant.start(promise.getPromise());
            return true;
        } catch (PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: sendMessage() : Failed to transmit message through PushService.");
        }

        return false;
    }

    private PushDeviceSettings getDevice() throws AuthLoginException {

        try {
            PushDeviceSettings firstDevice
                    = CollectionUtils.getFirstItem(userPushDeviceProfileManager.getDeviceProfiles(username, realm));
            if (null == firstDevice) {
                throw failedAsLoginException();
            }
            return firstDevice;
        } catch (IOException e) {
            throw failedAsLoginException();
        }
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    private AuthLoginException failedAsLoginException() throws AuthLoginException {
        setFailureID(username);
        throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
    }

    private void setPollbackTimePeriod(long periodInMilliseconds) throws AuthLoginException {

        PollingWaitCallback newPollingWaitCallback = PollingWaitCallback.makeCallback()
                .asCopyOf((PollingWaitCallback) getCallback(STATE_WAIT)[POLLING_CALLBACK_POSITION])
                .withWaitTime(String.valueOf(periodInMilliseconds))
                .build();
        replaceCallback(STATE_WAIT, POLLING_CALLBACK_POSITION, newPollingWaitCallback);
    }
}
