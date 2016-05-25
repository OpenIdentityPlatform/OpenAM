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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.selfservice.config.flows;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.selfservice.KeyStoreJwtTokenConfig;
import org.forgerock.openam.selfservice.config.ServiceConfigProvider;
import org.forgerock.openam.selfservice.config.beans.RegistrationDestination;
import org.forgerock.openam.selfservice.config.beans.UserRegistrationConsoleConfig;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.kba.KbaConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.services.context.Context;

/**
 * The default user registration configuration definition.
 *
 * @since 13.0.0
 */
public final class UserRegistrationConfigProvider
        implements ServiceConfigProvider<UserRegistrationConsoleConfig> {

    @Override
    public boolean isServiceEnabled(UserRegistrationConsoleConfig config) {
        return config.isEnabled();
    }

    @Override
    public ProcessInstanceConfig getServiceConfig(
            UserRegistrationConsoleConfig config, Context context, String realm) {

        List<StageConfig> stages = new ArrayList<>();

        if (config.isCaptchaEnabled()) {
            stages.add(new CaptchaStageConfig()
                    .setRecaptchaSiteKey(config.getCaptchaSiteKey())
                    .setRecaptchaSecretKey(config.getCaptchaSecretKey())
                    .setRecaptchaUri(config.getCaptchaVerificationUrl()));
        }

        if (config.isEmailEnabled()) {
            String serverUrl = config.getEmailVerificationUrl() + "&realm=" + realm;
            stages.add(new VerifyEmailAccountConfig()
                    .setEmailServiceUrl("/email")
                    .setIdentityEmailField(config.getEmailAttributeName())
                    .setSubjectTranslations(config.getSubjectTranslations())
                    .setMessageTranslations(config.getMessageTranslations())
                    .setMimeType("text/html")
                    .setVerificationLinkToken("%link%")
                    .setVerificationLink(serverUrl));
        }

        stages.add(new UserDetailsConfig()
                .setIdentityEmailField(config.getEmailAttributeName()));

        if (config.isKbaEnabled()) {
            stages.add(new SecurityAnswerDefinitionConfig(new KbaConfig())
                    .setQuestions(config.getSecurityQuestions())
                    .setNumberOfAnswersUserMustSet(config.getMinimumAnswersToDefine())
                    .setKbaPropertyName("kbaInfo"));
        }

        stages.add(new UserRegistrationConfig()
                .setIdentityServiceUrl("/users"));

        if (config.getUserRegistrationDestination() == RegistrationDestination.AUTO_LOGIN) {
            stages.add(new AutoLoginStageConfig()
                    .setRealm(realm));
        }

        KeyStoreJwtTokenConfig extendedJwtTokenConfig = new KeyStoreJwtTokenConfig()
                .withEncryptionKeyPairAlias(config.getEncryptionKeyPairAlias())
                .withSigningSecretKeyAlias(config.getSigningSecretKeyAlias())
                .withTokenLifeTimeInSeconds(config.getTokenExpiry());

        return new ProcessInstanceConfig()
                .setStageConfigs(stages)
                .setSnapshotTokenConfig(extendedJwtTokenConfig)
                .setStorageType(StorageType.STATELESS);
    }

}
