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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.authentication.modules.push.Constants.*;
import static org.forgerock.openam.services.push.PushMessage.*;

import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.forgerock.openam.authentication.callbacks.helpers.PollingWaitAssistant;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * ForgeRock Authentication (Push) Authentication Module.
 */
public class AuthenticatorPush extends AMLoginModule {

    private static final Debug DEBUG = Debug.getInstance(AM_AUTH_AUTHENTICATOR_PUSH);

    private final PushNotificationService pushService =
            InjectorHolder.getInstance(PushNotificationService.class);
    private final MessageDispatcher messageDispatcher =
            InjectorHolder.getInstance(MessageDispatcher.class);

    //From config
    private String deviceId;
    private Map<String, String> sharedState;

    //Internal state
    private String realm;
    private Promise<JsonValue, Exception> promise;
    private String username;
    private Principal principal;

    private PollingWaitAssistant pollingWaitAssistant;

    @Override
    public void init(Subject subject, Map sharedState, Map options) {

        this.sharedState = sharedState;
        this.deviceId = CollectionHelper.getMapAttr(options, DEVICE_MESSAGING_ID);
        long timeoutInMilliSeconds = Long.valueOf(CollectionHelper.getMapAttr(options, DEVICE_PUSH_WAIT_TIMEOUT));

        this.realm = DNMapper.orgNameToRealmName(getRequestOrg());

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
            if (sendMessage(getDeviceId())) {
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

    private boolean sendMessage(String deviceId) {
        PushMessage message = new PushMessage(deviceId, json(object(
                field("message", "User logged in to realm \"" + realm + "\" using OpenAM")
        )));
        try {
            promise = messageDispatcher.expect(message.getData().get(MESSAGE_ID).toString());
            pushService.send(message, realm);
            pollingWaitAssistant.start(promise);
            return true;
        } catch (PushNotificationException e) {
            DEBUG.error("AuthenticatorPush :: sendMessage() : Failed to transmit message through PushService.");
        }

        return false;
    }

    private String getDeviceId() {
        return deviceId;
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
