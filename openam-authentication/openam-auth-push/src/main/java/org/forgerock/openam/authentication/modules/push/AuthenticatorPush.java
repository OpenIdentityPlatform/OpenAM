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

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import java.security.Principal;
import java.util.Map;
import java.util.ResourceBundle;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.services.push.PushMessage;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationService;

/**
 * ForgeRock Authentication (Push) Authentication Module.
 */
public class AuthenticatorPush extends AMLoginModule {

    private static final Debug DEBUG = Debug.getInstance(AM_AUTH_AUTHENTICATOR_PUSH);

    private final PushNotificationService pushService =
            InjectorHolder.getInstance(PushNotificationService.class);

    //From config
    private String deviceId;

    //Internal state
    private ResourceBundle bundle = null;
    private String realm;

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        deviceId = CollectionHelper.getMapAttr(options, DEVICE_MESSAGING_ID);
        bundle = amCache.getResBundle(AM_AUTH_AUTHENTICATOR_PUSH, getLoginLocale());
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
            return processError(bundle.getString("pushNullRequest"),
                    "AuthenticatorPush :: process() : HTTP Request is null - programmatic login is not supported.");
        }

        switch (state) {
        case ISAuthConstants.LOGIN_START:
            if (sendMessage(getDeviceId())) {
                return ISAuthConstants.LOGIN_SUCCEED;
            } else {
                return processError(bundle.getString("invalidLoginState"), "Push Failure", state);
            }
        default:
            return processError(bundle.getString("invalidLoginState"), "Unrecognised login state: {}", state);
        }

    }

    private boolean sendMessage(String deviceId) {
        PushMessage message = new PushMessage(deviceId, json(object()));
        try {
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

    /**
     * Writes out an error debug (if a throwable and debug message are provided) and returns a user-facing
     * error page.
     */
    private int processError(String headerMessage, String debugMessage,
                             Object... messageParameters) throws AuthLoginException {
        if (null != debugMessage) {
            DEBUG.error(debugMessage, messageParameters);
        }
        substituteHeader(STATE_ERROR, headerMessage);
        return STATE_ERROR;
    }

}
