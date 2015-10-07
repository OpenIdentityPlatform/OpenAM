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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.selfservice;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.selfservice.config.ConsoleConfigHandler;
import org.forgerock.openam.selfservice.config.ConsoleConfig;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.email.EmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.services.context.Context;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * Utilises the common anonymous process service to deliver user registration behaviour.
 *
 * @since 13.0.0
 */
final class UserRegistrationRequestHandler extends AbstractSelfServiceRequestHandler {

    @Inject
    public UserRegistrationRequestHandler(ProgressStageFactory stageFactory,
            SnapshotTokenHandlerFactory tokenHandlerFactory, ProcessStore localStore,
            ConsoleConfigHandler configHandler) {
        super(stageFactory, tokenHandlerFactory, localStore, configHandler);
    }

    @Override
    protected boolean isServiceEnabled(ConsoleConfig config) {
        return config.getUserRegistration().isEnabled();
    }

    @Override
    protected ProcessInstanceConfig getServiceConfig(ConsoleConfig config, Context context, String realm) {
        String serverUrl = config.getUserRegistration().getEmailUrl() + "&realm=" + realm;

        StageConfig emailConfig = new VerifyEmailAccountConfig(new EmailAccountConfig())
                .setEmailServiceUrl("/email")
                .setEmailFrom("info@admin.org")
                .setEmailSubject("Register new account")
                .setEmailMessage("<h3>This is your registration email.</h3>"
                        + "<h4><a href=\"%link%\">Email verification link</a></h4>")
                .setEmailMimeType("text/html")
                .setEmailVerificationLinkToken("%link%")
                .setEmailVerificationLink(serverUrl);

        StageConfig userDetailsConfig = new UserDetailsConfig()
                .setIdentityEmailField("/mail");

        StageConfig registrationConfig = new UserRegistrationConfig()
                .setIdentityServiceUrl("/users");

        String secret = SystemProperties.get(Constants.ENC_PWD_PROPERTY);
        JwtTokenHandlerConfig jwtTokenConfig = new JwtTokenHandlerConfig();
        jwtTokenConfig.setSharedKey(secret);
        jwtTokenConfig.setKeyPairAlgorithm("RSA");
        jwtTokenConfig.setKeyPairSize(1024);
        jwtTokenConfig.setJweAlgorithm(JweAlgorithm.RSAES_PKCS1_V1_5);
        jwtTokenConfig.setEncryptionMethod(EncryptionMethod.A128CBC_HS256);
        jwtTokenConfig.setJwsAlgorithm(JwsAlgorithm.HS256);
        jwtTokenConfig.setTokenLifeTimeInSeconds(config.getUserRegistration().getTokenExpiry());

        return new ProcessInstanceConfig()
                .setStageConfigs(Arrays.asList(emailConfig, userDetailsConfig, registrationConfig))
                .setSnapshotTokenConfig(jwtTokenConfig)
                .setStorageType(StorageType.STATELESS);
    }

}
