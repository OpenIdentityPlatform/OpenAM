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

package org.forgerock.openam.selfservice.config;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.CommonConfigVisitor;
import org.forgerock.selfservice.stages.email.EmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.services.context.Context;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * The default user registration configuration definition.
 *
 * @since 13.0.0
 */
public final class DefaultUserRegistrationProvider implements ServiceProvider<UserRegistrationConsoleConfig> {

    private final SnapshotTokenHandlerFactory tokenHandlerFactory;
    private final ProcessStore processStore;
    private final CommonConfigVisitor stageConfigVisitor;

    /**
     * Constructs the default user registration service provider.
     *
     * @param tokenHandlerFactory
     *         snapshot token handler factory
     * @param processStore
     *         local process store
     * @param stageConfigVisitor
     *         common stage config visitor
     */
    @Inject
    public DefaultUserRegistrationProvider(SnapshotTokenHandlerFactory tokenHandlerFactory,
            ProcessStore processStore, CommonConfigVisitor stageConfigVisitor) {
        this.tokenHandlerFactory = tokenHandlerFactory;
        this.processStore = processStore;
        this.stageConfigVisitor = stageConfigVisitor;
    }

    @Override
    public boolean isServiceEnabled(UserRegistrationConsoleConfig config) {
        return config.isEnabled();
    }

    @Override
    public RequestHandler getService(UserRegistrationConsoleConfig config, Context context, String realm) {
        String serverUrl = config.getEmailUrl() + "&realm=" + realm;

        VerifyEmailAccountConfig emailConfig = new VerifyEmailAccountConfig(new EmailAccountConfig())
                .setEmailServiceUrl("/email")
                .setEmailSubject("Register new account")
                .setEmailMessage("<h3>This is your registration email.</h3>"
                        + "<h4><a href=\"%link%\">Email verification link</a></h4>")
                .setEmailMimeType("text/html")
                .setEmailVerificationLinkToken("%link%")
                .setEmailVerificationLink(serverUrl);

        UserDetailsConfig userDetailsConfig = new UserDetailsConfig()
                .setIdentityEmailField("/mail");

        UserRegistrationConfig registrationConfig = new UserRegistrationConfig()
                .setIdentityServiceUrl("/users");

        String secret = SystemProperties.get(Constants.ENC_PWD_PROPERTY);
        JwtTokenHandlerConfig jwtTokenConfig = new JwtTokenHandlerConfig()
                .setSharedKey(secret)
                .setKeyPairAlgorithm("RSA")
                .setKeyPairSize(1024)
                .setJweAlgorithm(JweAlgorithm.RSAES_PKCS1_V1_5)
                .setEncryptionMethod(EncryptionMethod.A128CBC_HS256)
                .setJwsAlgorithm(JwsAlgorithm.HS256)
                .setTokenLifeTimeInSeconds(config.getTokenExpiry());

        List<StageConfig<? super CommonConfigVisitor>> stages = new ArrayList<>();
        stages.add(emailConfig);
        stages.add(userDetailsConfig);
        stages.add(registrationConfig);

        ProcessInstanceConfig<CommonConfigVisitor> serviceConfig =
                new ProcessInstanceConfig<CommonConfigVisitor>()
                        .setStageConfigs(stages)
                        .setSnapshotTokenConfig(jwtTokenConfig)
                        .setStorageType(StorageType.STATELESS);

        return new AnonymousProcessService<>(serviceConfig, stageConfigVisitor, tokenHandlerFactory, processStore);
    }

}
