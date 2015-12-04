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

package org.forgerock.openam.selfservice.config.flows;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.selfservice.config.ServiceConfigProvider;
import org.forgerock.openam.selfservice.config.beans.ForgottenUsernameConsoleConfig;
import org.forgerock.selfservice.core.StorageType;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.kba.KbaConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationConfig;
import org.forgerock.selfservice.stages.tokenhandlers.JwtTokenHandlerConfig;
import org.forgerock.selfservice.stages.user.EmailUsernameConfig;
import org.forgerock.selfservice.stages.user.RetrieveUsernameConfig;
import org.forgerock.selfservice.stages.user.UserQueryConfig;
import org.forgerock.services.context.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * The default forgotten username configuration definition.
 *
 * @since 13.0.0
 */
public final class ForgottenUsernameConfigProvider
        implements ServiceConfigProvider<ForgottenUsernameConsoleConfig> {

    @Override
    public boolean isServiceEnabled(ForgottenUsernameConsoleConfig config) {
        return config.isEnabled();
    }

    @Override
    public ProcessInstanceConfig getServiceConfig(
            ForgottenUsernameConsoleConfig config, Context context, String realm) {

        List<StageConfig> stages = new ArrayList<>();

        if (config.isCaptchaEnabled()) {
            stages.add(new CaptchaStageConfig()
                    .setRecaptchaSiteKey(config.getCaptchaSiteKey())
                    .setRecaptchaSecretKey(config.getCaptchaSecretKey())
                    .setRecaptchaUri(config.getCaptchaVerificationUrl()));
        }

        stages.add(new UserQueryConfig()
                .setValidQueryFields(config.getValidQueryAttributes())
                .setIdentityIdField("/username")
                .setIdentityUsernameField("/username")
                .setIdentityEmailField("/" + config.getEmailAttributeName() + "/0")
                .setIdentityServiceUrl("/users"));

        if (config.isKbaEnabled()) {
            stages.add(new SecurityAnswerVerificationConfig(new KbaConfig())
                    .setQuestions(config.getSecurityQuestions())
                    .setKbaPropertyName("kbaInfo")
                    .setNumberOfQuestionsUserMustAnswer(config.getMinimumAnswersToVerify())
                    .setIdentityServiceUrl("/users"));
        }

        if (config.isEmailEnabled()) {
            stages.add(new EmailUsernameConfig()
                    .setEmailServiceUrl("/email")
                    .setSubjectTranslations(config.getSubjectTranslations())
                    .setMessageTranslations(config.getMessageTranslations())
                    .setMimeType("text/html")
                    .setUsernameToken("%username%"));
        }

        if (config.isShowUsernameEnabled()) {
            stages.add(new RetrieveUsernameConfig());
        }

        String secret = SystemProperties.get(Constants.ENC_PWD_PROPERTY);
        JwtTokenHandlerConfig jwtTokenConfig = new JwtTokenHandlerConfig()
                .setSharedKey(secret)
                .setKeyPairAlgorithm("RSA")
                .setKeyPairSize(1024)
                .setJweAlgorithm(JweAlgorithm.RSAES_PKCS1_V1_5)
                .setEncryptionMethod(EncryptionMethod.A128CBC_HS256)
                .setJwsAlgorithm(JwsAlgorithm.HS256)
                .setTokenLifeTimeInSeconds(config.getTokenExpiry());

        return new ProcessInstanceConfig()
                .setStageConfigs(stages)
                .setSnapshotTokenConfig(jwtTokenConfig)
                .setStorageType(StorageType.STATELESS);
    }

}
