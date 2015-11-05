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

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getIntMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getLongMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapSetThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getLocaleMapAttrThrows;
import static org.forgerock.openam.selfservice.config.CommonSmsSelfServiceConstants.CAPTCHA_SECRET_KEY;
import static org.forgerock.openam.selfservice.config.CommonSmsSelfServiceConstants.CAPTCHA_SITE_KEY;
import static org.forgerock.openam.selfservice.config.CommonSmsSelfServiceConstants.CAPTCHA_VERIFICATION_URL;
import static org.forgerock.openam.selfservice.config.CommonSmsSelfServiceConstants.SECURITY_QUESTIONS_KEY;
import static org.forgerock.openam.selfservice.config.SecurityQuestionsHelper.parseQuestions;

import com.sun.identity.shared.datastruct.ValueNotFoundException;

import java.util.Map;
import java.util.Set;

/**
 * Extracts the forgotten password configuration instance from the console configuration attributes.
 *
 * @since 13.0.0
 */
public final class ForgottenPasswordExtractor implements ConsoleConfigExtractor<ForgottenPasswordConsoleConfig> {

    private final static String ENABLED_KEY = "forgerockRESTSecurityForgotPasswordEnabled";
    private final static String EMAIL_VERIFICATION_ENABLED = "forgerockRESTSecurityForgotPassEmailVerificationEnabled";
    private final static String EMAIL_URL_KEY = "forgerockRESTSecurityForgotPassConfirmationUrl";
    private final static String TOKEN_EXPIRY_KEY = "forgerockRESTSecurityForgotPassTokenTTL";
    private final static String SERVICE_CONFIG_CLASS_KEY = "forgerockRESTSecurityForgotPassServiceConfigClass";
    private final static String KBA_ENABLED_KEY = "forgerockRESTSecurityForgotPassKbaEnabled";
    private final static String MIN_QUESTIONS_TO_ANSWERED_KEY = "forgerockRESTSecurityQuestionsUserMustAnswer";
    private final static String CAPTCHA_ENABLED_KEY = "forgerockRESTSecurityForgotPassCaptchaEnabled";
    private final static String SUBJECT_MAP = "forgerockRESTSecurityForgotPassSubjectText";
    private final static String BODY_TEXT_MAP = "forgerockRESTSecurityForgotPassEmailText";

    @Override
    public ForgottenPasswordConsoleConfig extract(Map<String, Set<String>> consoleAttributes) {
        try {
            return ForgottenPasswordConsoleConfig
                    .newBuilder(consoleAttributes)
                    .setEnabled(getBooleanMapAttrThrows(consoleAttributes, ENABLED_KEY))
                    .setEmailVerificationEnabled(getBooleanMapAttrThrows(consoleAttributes, EMAIL_VERIFICATION_ENABLED))
                    .setEmailUrl(getMapAttrThrows(consoleAttributes, EMAIL_URL_KEY))
                    .setSubjectTranslations(getLocaleMapAttrThrows(consoleAttributes, SUBJECT_MAP))
                    .setMessageTranslations(getLocaleMapAttrThrows(consoleAttributes, BODY_TEXT_MAP))
                    .setCaptchaEnabled(getBooleanMapAttrThrows(consoleAttributes, CAPTCHA_ENABLED_KEY))
                    .setSiteKey(getMapAttr(consoleAttributes, CAPTCHA_SITE_KEY))
                    .setSecretKey(getMapAttr(consoleAttributes, CAPTCHA_SECRET_KEY))
                    .setVerificationUrl(getMapAttr(consoleAttributes, CAPTCHA_VERIFICATION_URL))
                    .setKbaEnabled(getBooleanMapAttrThrows(consoleAttributes, KBA_ENABLED_KEY))
                    .setSecurityQuestions(parseQuestions(getMapSetThrows(consoleAttributes, SECURITY_QUESTIONS_KEY)))
                    .setMinQuestionsToAnswer(getIntMapAttrThrows(consoleAttributes, MIN_QUESTIONS_TO_ANSWERED_KEY))
                    .setTokenExpiry(getLongMapAttrThrows(consoleAttributes, TOKEN_EXPIRY_KEY))
                    .setConfigProviderClass(getMapAttrThrows(consoleAttributes, SERVICE_CONFIG_CLASS_KEY))
                    .build();

        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException("Invalid console values", e);
        }
    }

}
