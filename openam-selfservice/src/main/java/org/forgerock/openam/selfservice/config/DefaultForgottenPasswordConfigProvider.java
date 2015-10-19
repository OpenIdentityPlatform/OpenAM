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
import org.forgerock.openam.selfservice.config.custom.CustomSupportConfigVisitor;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.stages.email.EmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.services.context.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * The default forgotten password configuration definition.
 *
 * @since 13.0.0
 */
public final class DefaultForgottenPasswordConfigProvider implements ServiceConfigProvider<ForgottenPasswordConsoleConfig> {

    @Override
    public boolean isServiceEnabled(ForgottenPasswordConsoleConfig config) {
        return config.isEnabled();
    }

    @Override
    public ProcessInstanceConfig<CustomSupportConfigVisitor> getServiceConfig(
            ForgottenPasswordConsoleConfig config, Context context, String realm) {
        String serverUrl = config.getEmailUrl() + "&realm=" + realm;

        VerifyUserIdConfig verifyUserIdConfig = new VerifyUserIdConfig(new EmailAccountConfig())
                .setQueryFields(new HashSet<>(Arrays.asList("uid", "mail")))
                .setIdentityIdField("/uid/0")
                .setIdentityEmailField("/mail/0")
                .setIdentityServiceUrl("/users")
                .setEmailServiceUrl("/email")
                .setEmailSubject("Reset password email")
                .setEmailMessage("<h3>This is your reset email.</h3>"
                        + "<h4><a href=\"%link%\">Email verification link</a></h4>")
                .setEmailMimeType("text/html")
                .setEmailVerificationLinkToken("%link%")
                .setEmailVerificationLink(serverUrl);

        ResetStageConfig resetConfig = new ResetStageConfig()
                .setIdentityServiceUrl("/users")
                .setIdentityPasswordField("userPassword");

        String secret = SystemProperties.get(Constants.ENC_PWD_PROPERTY);
        JwtTokenHandlerConfig jwtTokenConfig = new JwtTokenHandlerConfig()
                .setSharedKey(secret)
                .setKeyPairAlgorithm("RSA")
                .setKeyPairSize(1024)
                .setJweAlgorithm(JweAlgorithm.RSAES_PKCS1_V1_5)
                .setEncryptionMethod(EncryptionMethod.A128CBC_HS256)
                .setJwsAlgorithm(JwsAlgorithm.HS256)
                .setTokenLifeTimeInSeconds(config.getTokenExpiry());

        List<StageConfig<? super CustomSupportConfigVisitor>> stages = new ArrayList<>();
        stages.add(verifyUserIdConfig);
        stages.add(resetConfig);

        return new ProcessInstanceConfig<CustomSupportConfigVisitor>()
                .setStageConfigs(stages)
                .setSnapshotTokenConfig(jwtTokenConfig)
                .setStorageType(StorageType.STATELESS);
    }

}
