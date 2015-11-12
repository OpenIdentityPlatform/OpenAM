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

import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getIntMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getLocaleMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getLongMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapSetThrows;
import static org.forgerock.openam.selfservice.config.flows.CommonSmsSelfServiceConstants.CAPTCHA_SECRET_KEY;
import static org.forgerock.openam.selfservice.config.flows.CommonSmsSelfServiceConstants.CAPTCHA_SITE_KEY;
import static org.forgerock.openam.selfservice.config.flows.CommonSmsSelfServiceConstants.CAPTCHA_VERIFICATION_URL;
import static org.forgerock.openam.selfservice.config.flows.CommonSmsSelfServiceConstants.SECURITY_QUESTIONS_KEY;

import com.sun.identity.shared.datastruct.ValueNotFoundException;
import org.forgerock.openam.selfservice.config.ConsoleConfigExtractor;

import java.util.Map;
import java.util.Set;

/**
 * Extracts the forgotten password configuration instance from the console configuration attributes.
 *
 * @since 13.0.0
 */
public final class UserRegistrationExtractor implements ConsoleConfigExtractor<UserRegistrationConsoleConfig> {

    private final static String ENABLED_KEY = "forgerockRESTSecuritySelfRegistrationEnabled";
    private final static String CAPTCHA_ENABLED_KEY = "forgerockRESTSecuritySelfRegCaptchaEnabled";
    private final static String EMAIL_VERIFICATION_ENABLED_KEY = "forgerockRESTSecuritySelfRegEmailVerificationEnabled";
    private final static String EMAIL_URL_KEY = "forgerockRESTSecuritySelfRegConfirmationUrl";
    private final static String SUBJECT_TRANSLATION_KEY = "forgerockRESTSecuritySelfRegEmailSubject";
    private final static String BODY_TRANSLATION_KEY = "forgerockRESTSecuritySelfRegEmailBody";
    private final static String KBA_ENABLED_KEY = "forgerockRESTSecuritySelfRegKbaEnabled";
    private final static String MIN_ANSWERS_TO_PROVIDE_KEY = "forgerockRESTSecurityAnswersUserMustProvide";
    private final static String TOKEN_EXPIRY_KEY = "forgerockRESTSecuritySelfRegTokenTTL";
    private final static String SERVICE_CONFIG_CLASS_KEY = "forgerockRESTSecuritySelfRegServiceConfigClass";

    @Override
    public UserRegistrationConsoleConfig extract(Map<String, Set<String>> consoleAttributes) {
        try {
            return UserRegistrationConsoleConfig
                    .newBuilder(consoleAttributes)
                    .setEnabled(getBooleanMapAttrThrows(consoleAttributes, ENABLED_KEY))
                    .setCaptchaEnabled(getBooleanMapAttrThrows(consoleAttributes, CAPTCHA_ENABLED_KEY))
                    .setSiteKey(getMapAttr(consoleAttributes, CAPTCHA_SITE_KEY))
                    .setSecretKey(getMapAttr(consoleAttributes, CAPTCHA_SECRET_KEY))
                    .setVerificationUrl(getMapAttr(consoleAttributes, CAPTCHA_VERIFICATION_URL))
                    .setEmailEnabled(getBooleanMapAttrThrows(consoleAttributes, EMAIL_VERIFICATION_ENABLED_KEY))
                    .setEmailVerificationUrl(getMapAttrThrows(consoleAttributes, EMAIL_URL_KEY))
                    .setSubjectTranslations(getLocaleMapAttrThrows(consoleAttributes, SUBJECT_TRANSLATION_KEY))
                    .setMessageTranslations(getLocaleMapAttrThrows(consoleAttributes, BODY_TRANSLATION_KEY))
                    .setKbaEnabled(getBooleanMapAttrThrows(consoleAttributes, KBA_ENABLED_KEY))
                    .setSecurityQuestions(SecurityQuestionsHelper.parseQuestions(getMapSetThrows(consoleAttributes, SECURITY_QUESTIONS_KEY)))
                    .setMinAnswersToProvide(getIntMapAttrThrows(consoleAttributes, MIN_ANSWERS_TO_PROVIDE_KEY))
                    .setTokenExpiry(getLongMapAttrThrows(consoleAttributes, TOKEN_EXPIRY_KEY))
                    .setConfigProviderClass(getMapAttrThrows(consoleAttributes, SERVICE_CONFIG_CLASS_KEY))
                    .build();

        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException("Invalid console values", e);
        }
    }

}
