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

package org.forgerock.openam.selfservice.config.beans;

import org.forgerock.openam.sm.config.ConfigAttribute;
import org.forgerock.openam.sm.config.ConfigSource;
import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents forgotten password console configuration.
 *
 * @supported.all.api
 * @since 13.0.0
 */
public final class UserRegistrationConsoleConfig extends CommonConsoleConfig {

    private final String emailVerificationUrl;
    private final int minimumAnswersToDefine;
    private final boolean enabled;
    private final String configProviderClass;
    private final long tokenExpiry;
    private final boolean emailEnabled;
    private final Map<Locale, String> subjectTranslations;
    private final Map<Locale, String> messageTranslations;
    private final boolean captchaEnabled;
    private final boolean kbaEnabled;

    private UserRegistrationConsoleConfig(UserRegistrationBuilder builder) {
        super(builder);
        emailVerificationUrl = builder.emailVerificationUrl;
        minimumAnswersToDefine = builder.minimumAnswersToDefine;
        configProviderClass = builder.configProviderClass;
        enabled = builder.enabled;
        emailEnabled = builder.emailEnabled;
        tokenExpiry = builder.tokenExpiry;
        captchaEnabled = builder.captchaEnabled;
        kbaEnabled = builder.kbaEnabled;
        subjectTranslations = builder.subjectTranslations;
        messageTranslations = builder.messageTranslations;
    }

    @Override
    public String getConfigProviderClass() {
        return configProviderClass;
    }

    /**
     * Whether the service is enabled.
     *
     * @return whether the service is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether email verification is enabled.
     *
     * @return whether email verification is enabled
     */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    /**
     * Gets the token expiry time in seconds.
     *
     * @return the token expiry time
     */
    public long getTokenExpiry() {
        return tokenExpiry;
    }

    /**
     * Whether the captcha stage is enabled.
     *
     * @return whether the captcha stage is enabled
     */
    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    /**
     * Whether the KBA stage is enabled.
     *
     * @return whether the KBA stage is enabled
     */
    public boolean isKbaEnabled() {
        return kbaEnabled;
    }

    /**
     * Gets the map of locales to subject strings.
     *
     * @return the map of locales to subject text strings.
     */
    public Map<Locale, String> getSubjectTranslations() {
        return subjectTranslations;
    }

    /**
     * Gets the map of locales to email body text strings.
     *
     * @return the map of locales to email body text strings.
     */
    public Map<Locale, String> getMessageTranslations() {
        return messageTranslations;
    }

    /**
     * Gets the verification Url to be sent with the email body.
     *
     * @return email verification Url
     */
    public String getEmailVerificationUrl() {
        return emailVerificationUrl;
    }

    /**
     * Get the minimum count of answers to define.
     *
     * @return minimum count
     */
    public int getMinimumAnswersToDefine() {
        return minimumAnswersToDefine;
    }

    @ConfigSource("RestSecurity")
    public static final class UserRegistrationBuilder
            extends CommonConsoleConfigBuilder<UserRegistrationConsoleConfig> {

        private String emailVerificationUrl;
        private int minimumAnswersToDefine;
        private boolean enabled;
        private String configProviderClass;
        private long tokenExpiry;
        private boolean emailEnabled;
        private final Map<Locale, String> subjectTranslations;
        private final Map<Locale, String> messageTranslations;
        private boolean captchaEnabled;
        private boolean kbaEnabled;

        public UserRegistrationBuilder() {
            subjectTranslations = new HashMap<>();
            messageTranslations = new HashMap<>();
        }

        @ConfigAttribute("forgerockRESTSecuritySelfRegistrationEnabled")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @ConfigAttribute("forgerockRESTSecuritySelfRegServiceConfigClass")
        public void setConfigProviderClass(String configProviderClass) {
            this.configProviderClass = configProviderClass;
        }

        @ConfigAttribute("forgerockRESTSecuritySelfRegTokenTTL")
        public void setTokenExpiry(long tokenExpiry) {
            this.tokenExpiry = tokenExpiry;
        }

        @ConfigAttribute("forgerockRESTSecuritySelfRegEmailVerificationEnabled")
        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        @ConfigAttribute(value = "forgerockRESTSecuritySelfRegEmailSubject",
                transformer = LocaleMessageTransformer.class)
        public void setSubjectTranslations(Map<Locale, String> subjectTranslations) {
            this.subjectTranslations.putAll(subjectTranslations);
        }

        @ConfigAttribute(value = "forgerockRESTSecuritySelfRegEmailBody",
                transformer = LocaleMessageTransformer.class)
        public void setMessageTranslations(Map<Locale, String> messageTranslations) {
            this.messageTranslations.putAll(messageTranslations);
        }

        @ConfigAttribute("forgerockRESTSecuritySelfRegCaptchaEnabled")
        public void setCaptchaEnabled(boolean captchaEnabled) {
            this.captchaEnabled = captchaEnabled;
        }

        @ConfigAttribute("forgerockRESTSecuritySelfRegKbaEnabled")
        public void setKbaEnabled(boolean kbaEnabled) {
            this.kbaEnabled = kbaEnabled;
        }

        @ConfigAttribute("forgerockRESTSecuritySelfRegConfirmationUrl")
        public void setEmailVerificationUrl(String emailVerificationUrl) {
            this.emailVerificationUrl = emailVerificationUrl;
        }

        @ConfigAttribute("forgerockRESTSecurityAnswersUserMustProvide")
        public void setMinimumAnswersToDefine(int minimumAnswersToDefine) {
            this.minimumAnswersToDefine = minimumAnswersToDefine;
        }

        @Override
        boolean isCaptchaEnabled() {
            return captchaEnabled;
        }

        @Override
        boolean isKbaEnabled() {
            return kbaEnabled;
        }

        @Override
        UserRegistrationConsoleConfig internalBuild() {
            Reject.ifNull(configProviderClass, "Config provider class name required");
            Reject.ifFalse(tokenExpiry > 0, "Token expiry must be greater than zero");

            if (emailEnabled) {
                Reject.ifNull(emailVerificationUrl, "Email verification Url is required");
                Reject.ifTrue(subjectTranslations.isEmpty(), "Subject translations are required");
                Reject.ifTrue(messageTranslations.isEmpty(), "Message translations are required");
            }

            if (kbaEnabled) {
                Reject.ifFalse(minimumAnswersToDefine > 0, "Minimum answers to be defined must be greater than 0");
            }

            return new UserRegistrationConsoleConfig(this);
        }

    }

}
