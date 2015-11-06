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

import org.forgerock.selfservice.core.ProgressStageBinder;
import org.forgerock.selfservice.stages.CommonConfigVisitor;
import org.forgerock.selfservice.stages.captcha.CaptchaStage;
import org.forgerock.selfservice.stages.captcha.CaptchaStageConfig;
import org.forgerock.selfservice.stages.dynamic.DynamicConfigVisitor;
import org.forgerock.selfservice.stages.dynamic.DynamicStageConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountStage;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerDefinitionStage;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationConfig;
import org.forgerock.selfservice.stages.kba.SecurityAnswerVerificationStage;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationStage;
import org.forgerock.selfservice.stages.reset.ResetStage;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.user.RetrieveUsernameConfig;
import org.forgerock.selfservice.stages.user.RetrieveUsernameStage;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.selfservice.stages.user.UserDetailsStage;
import org.forgerock.selfservice.stages.user.UserQueryConfig;
import org.forgerock.selfservice.stages.user.UserQueryStage;

import javax.inject.Inject;

/**
 * This visitor is responsible for building progress stages.
 *
 * @since 13.0.0
 */
public final class BasicStageConfigVisitor implements CommonConfigVisitor {

    private final VerifyEmailAccountStage verifyEmailAccountStage;
    private final SecurityAnswerDefinitionStage securityAnswerDefinitionStage;
    private final UserRegistrationStage userRegistrationStage;
    private final UserDetailsStage userDetailsStage;
    private final UserQueryStage userQueryStage;
    private final SecurityAnswerVerificationStage securityAnswerVerificationStage;
    private final ResetStage resetStage;
    private final CaptchaStage captchaStage;

    private final DynamicConfigVisitor dynamicConfigVisitor;

    /**
     * Constructs a new user registration visitor.
     *
     * @param verifyEmailAccountStage
     *         verify email account stage
     * @param securityAnswerDefinitionStage
     *         security answer definition stage
     * @param userRegistrationStage
     *         user registration stage
     * @param userQueryStage
     *         user query stage
     * @param securityAnswerVerificationStage
     *         security answer verification stage
     * @param resetStage
     *         reset password stage
     * @param captchaStage
     *         captcha stage
     * @param dynamicConfigVisitor
     *         dynamic config visitor
     */
    @Inject
    public BasicStageConfigVisitor(VerifyEmailAccountStage verifyEmailAccountStage,
            SecurityAnswerDefinitionStage securityAnswerDefinitionStage, UserRegistrationStage userRegistrationStage,
            UserDetailsStage userDetailsStage, UserQueryStage userQueryStage, ResetStage resetStage,
            CaptchaStage captchaStage, SecurityAnswerVerificationStage securityAnswerVerificationStage,
            DynamicConfigVisitor dynamicConfigVisitor) {
        this.verifyEmailAccountStage = verifyEmailAccountStage;
        this.securityAnswerDefinitionStage = securityAnswerDefinitionStage;
        this.userRegistrationStage = userRegistrationStage;
        this.userDetailsStage = userDetailsStage;
        this.userQueryStage = userQueryStage;
        this.securityAnswerVerificationStage = securityAnswerVerificationStage;
        this.resetStage = resetStage;
        this.captchaStage = captchaStage;

        this.dynamicConfigVisitor = dynamicConfigVisitor;
    }

    @Override
    public ProgressStageBinder<?> build(VerifyEmailAccountConfig verifyEmailAccountConfig) {
        return ProgressStageBinder.bind(verifyEmailAccountStage, verifyEmailAccountConfig);
    }

    @Override
    public ProgressStageBinder<?> build(SecurityAnswerDefinitionConfig securityAnswerDefinitionConfig) {
        return ProgressStageBinder.bind(securityAnswerDefinitionStage, securityAnswerDefinitionConfig);
    }

    @Override
    public ProgressStageBinder<?> build(UserRegistrationConfig userRegistrationConfig) {
        return ProgressStageBinder.bind(userRegistrationStage, userRegistrationConfig);
    }

    @Override
    public ProgressStageBinder<?> build(UserDetailsConfig userDetailsConfig) {
        return ProgressStageBinder.bind(userDetailsStage, userDetailsConfig);
    }

    @Override
    public ProgressStageBinder<?> build(SecurityAnswerVerificationConfig securityAnswerVerificationConfig) {
        return ProgressStageBinder.bind(securityAnswerVerificationStage, securityAnswerVerificationConfig);
    }

    @Override
    public ProgressStageBinder<?> build(ResetStageConfig resetStageConfig) {
        return ProgressStageBinder.bind(resetStage, resetStageConfig);
    }

    @Override
    public ProgressStageBinder<?> build(CaptchaStageConfig captchaStageConfig) {
        return ProgressStageBinder.bind(captchaStage, captchaStageConfig);
    }

    @Override
    public ProgressStageBinder<?> build(UserQueryConfig userQueryConfig) {
        return ProgressStageBinder.bind(userQueryStage, userQueryConfig);
    }

    @Override
    public ProgressStageBinder<?> build(RetrieveUsernameConfig retrieveUsernameConfig) {
        return ProgressStageBinder.bind(new RetrieveUsernameStage(), retrieveUsernameConfig);
    }

    @Override
    public ProgressStageBinder<?> build(DynamicStageConfig dynamicStageConfig) {
        return dynamicConfigVisitor.build(dynamicStageConfig);
    }

}
