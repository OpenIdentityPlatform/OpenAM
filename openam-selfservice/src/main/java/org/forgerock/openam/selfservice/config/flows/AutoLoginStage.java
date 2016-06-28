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
package org.forgerock.openam.selfservice.config.flows;

import static org.forgerock.selfservice.stages.CommonStateFields.USER_FIELD;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.AuthContext.Status;


/**
 * Auto logic stage attempts to authenticate the registered
 * user and submits the SSO token to the success additions.
 *
 * @since 13.5.0
 */
final class AutoLoginStage implements ProgressStage<AutoLoginStageConfig> {

    private static final Logger logger = LoggerFactory.getLogger(AutoLoginStage.class);

    @Override
    public JsonValue gatherInitialRequirements(ProcessContext context,
            AutoLoginStageConfig config) throws ResourceException {

        Reject.ifFalse(context.containsState(USER_FIELD),
                "User registration stage expects user in the context");
        return RequirementsBuilder.newEmptyRequirements();
    }

    @Override
    public StageResponse advance(ProcessContext context,
            AutoLoginStageConfig config) throws ResourceException {

        try {
            putAuthTokenInSuccessAdditions(context, config);
        } catch (Exception e) {
            logger.warn("Auto login failed to attain an SSO token for registered user", e);
        }

        return StageResponse
                .newBuilder()
                .build();
    }

    private void putAuthTokenInSuccessAdditions(ProcessContext context,
            AutoLoginStageConfig config) throws Exception {

        JsonValue user = context.getState(USER_FIELD);

        AuthContext authContext = new AuthContext(config.getRealm());
        authContext.login();

        while (authContext.hasMoreRequirements()) {
            Callback[] callbacks = authContext.getRequirements();
            handleCallbacks(callbacks, user);
            authContext.submitRequirements(callbacks);
        }

        if (authContext.getStatus() != Status.SUCCESS) {
            throw new AutoLoginException("Authentication for registered user failed with status:"
                    + authContext.getStatus());
        }

        String ssoToken = authContext.getSSOToken().getTokenID().toString();
        String gotoUrl = authContext.getSuccessURL();

        context.putSuccessAddition("tokenId", ssoToken);
        context.putSuccessAddition("successUrl", gotoUrl);
    }

    private void handleCallbacks(Callback[] callbacks, JsonValue user) throws AutoLoginException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                String username = user.get("username")
                        .required()
                        .asString();

                NameCallback nameCallback = (NameCallback) callback;
                nameCallback.setName(username);
            } else if (callback instanceof PasswordCallback) {
                String password = user.get("userPassword")
                        .required()
                        .asString();

                PasswordCallback passwordCallback = (PasswordCallback) callback;
                passwordCallback.setPassword(password.toCharArray());
            } else {
                throw new AutoLoginException("Unsupported callback during auto login:" + callback.getClass().getName());
            }
        }
    }

}
