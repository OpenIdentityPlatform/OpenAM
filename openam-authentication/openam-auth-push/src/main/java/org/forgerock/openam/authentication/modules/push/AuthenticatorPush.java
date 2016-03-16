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
import static org.forgerock.openam.services.push.PushMessage.MESSAGE_ID;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import java.security.Principal;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.services.push.dispatch.MessageDispatcher;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;
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

    //Internal state
    private String realm;
    private Promise<JsonValue, Exception> promise;

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        deviceId = CollectionHelper.getMapAttr(options, DEVICE_MESSAGING_ID);
        realm = DNMapper.orgNameToRealmName(getRequestOrg());

        String authLevel = CollectionHelper.getMapAttr(options, AUTH_LEVEL);

        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                DEBUG.error("AuthenticatorPush :: init() : Unable to set auth level {}", authLevel, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int process(final Callback[] realCallbacks, int state) throws LoginException {

        final HttpServletRequest request = getHttpServletRequest();

        if (null == request) {
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            if (sendMessage(getDeviceId())) {
                return AWAIT_STATE;
            } else {
                throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
            }
        case AWAIT_STATE:
            if (promise.isDone()) {
                return ISAuthConstants.LOGIN_SUCCEED;
            } else {
                return AWAIT_STATE;
            }

        default:
            throw new AuthLoginException(AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }

    }

    private boolean sendMessage(String deviceId) {
        PushMessage message = new PushMessage(deviceId, json(object(
                field("message", "User logged in to realm \"" + realm + "\" using OpenAM")
        )));
        try {
            promise = messageDispatcher.expect(message.getData().get(MESSAGE_ID).toString());
            pushService.send(message, realm);
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
        return null;
    }

}
