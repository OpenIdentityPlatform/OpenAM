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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Represents forgotten username console configuration.
 *
 * @supported.all.api
 * @since 13.0.0
 */
public final class ForgottenUsernameConsoleConfig extends CommonConsoleConfig {

    private final int minimumAnswersToVerify;
    private final boolean showUsernameEnabled;
    private final boolean enabled;
    private final String configProviderClass;
    private final long tokenExpiry;
    private final boolean emailEnabled;
    private final Map<Locale, String> subjectTranslations;
    private final Map<Locale, String> messageTranslations;
    private final boolean captchaEnabled;
    private final boolean kbaEnabled;
    private final Set<String> validQueryAttributes;

    private ForgottenUsernameConsoleConfig(ForgottenUsernameBuilder builder) {
        super(builder);
        minimumAnswersToVerify = builder.minimumAnswersToVerify;
        showUsernameEnabled = builder.showUsernameEnabled;
        configProviderClass = builder.configProviderClass;
        enabled = builder.enabled;
        emailEnabled = builder.emailEnabled;
        tokenExpiry = builder.tokenExpiry;
        captchaEnabled = builder.captchaEnabled;
        kbaEnabled = builder.kbaEnabled;
        subjectTranslations = builder.subjectTranslations;
        messageTranslations = builder.messageTranslations;
        validQueryAttributes = builder.validQueryAttributes;
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
     * Get the minimum count of questions to verify.
     *
     * @return minimum count
     */
    public int getMinimumAnswersToVerify() {
        return minimumAnswersToVerify;
    }

    /**
     * Whether or the not the username should be displayed.
     *
     * @return whether username should be shown
     */
    public boolean isShowUsernameEnabled() {
        return showUsernameEnabled;
    }

    /**
     * Get set of valid query attributes.
     *
     * @return valid query attributes
     */
    public Set<String> getValidQueryAttributes() {
        return validQueryAttributes;
    }

    /**
     * Builder for {@link ForgottenUsernameConsoleConfig}.
     */
    @ConfigSource({"MailServer", "selfService"})
    public static final class ForgottenUsernameBuilder
            extends CommonConsoleConfigBuilder<ForgottenUsernameConsoleConfig> {

        private int minimumAnswersToVerify;
        private boolean showUsernameEnabled;
        private boolean enabled;
        private String configProviderClass;
        private long tokenExpiry;
        private boolean emailEnabled;
        private final Map<Locale, String> subjectTranslations;
        private final Map<Locale, String> messageTranslations;
        private boolean captchaEnabled;
        private boolean kbaEnabled;
        private final Set<String> validQueryAttributes;

        /**
         * Constructs a new forgotten username builder.
         */
        public ForgottenUsernameBuilder() {
            subjectTranslations = new HashMap<>();
            messageTranslations = new HashMap<>();
            validQueryAttributes = new HashSet<>();
        }

        /**
         * Sets whether the service is enabled.
         *
         * @param enabled
         *         whether the service is enabled
         */
        @ConfigAttribute("selfServiceForgottenUsernameEnabled")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Sets the config provider class.
         *
         * @param configProviderClass
         *         config provider class
         */
        @ConfigAttribute("selfServiceForgottenUsernameServiceConfigClass")
        public void setConfigProviderClass(String configProviderClass) {
            this.configProviderClass = configProviderClass;
        }

        /**
         * Sets the token expiry time.
         *
         * @param tokenExpiry
         *         token expiry time
         */
        @ConfigAttribute("selfServiceForgottenUsernameTokenTTL")
        public void setTokenExpiry(long tokenExpiry) {
            this.tokenExpiry = tokenExpiry;
        }

        /**
         * Sets whether email is enabled.
         *
         * @param emailEnabled
         *         whether email is enabled
         */
        @ConfigAttribute("selfServiceForgottenUsernameEmailUsernameEnabled")
        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        /**
         * Sets the email subject translations.
         *
         * @param subjectTranslations
         *         email subject translations
         */
        @ConfigAttribute(value = "selfServiceForgottenUsernameEmailSubject",
                transformer = LocaleMessageTransformer.class)
        public void setSubjectTranslations(Map<Locale, String> subjectTranslations) {
            this.subjectTranslations.putAll(subjectTranslations);
        }

        /**
         * Sets the email body translations.
         *
         * @param messageTranslations
         *         email body translations
         */
        @ConfigAttribute(value = "selfServiceForgottenUsernameEmailBody",
                transformer = LocaleMessageTransformer.class)
        public void setMessageTranslations(Map<Locale, String> messageTranslations) {
            this.messageTranslations.putAll(messageTranslations);
        }

        /**
         * Sets whether captcha is enabled.
         *
         * @param captchaEnabled
         *         whether captcha is enabled
         */
        @ConfigAttribute("selfServiceForgottenUsernameCaptchaEnabled")
        public void setCaptchaEnabled(boolean captchaEnabled) {
            this.captchaEnabled = captchaEnabled;
        }

        /**
         * Sets whether KBA is enabled.
         *
         * @param kbaEnabled
         *         whether KBA is enabled
         */
        @ConfigAttribute("selfServiceForgottenUsernameKbaEnabled")
        public void setKbaEnabled(boolean kbaEnabled) {
            this.kbaEnabled = kbaEnabled;
        }

        /**
         * Sets the minimum number of answers to be verified.
         *
         * @param minimumAnswersToVerify
         *         minimum number of answers to be verified
         */
        @ConfigAttribute("selfServiceMinimumAnswersToVerify")
        public void setMinimumAnswersToVerify(int minimumAnswersToVerify) {
            this.minimumAnswersToVerify = minimumAnswersToVerify;
        }

        /**
         * Sets whether show username is enabled.
         *
         * @param showUsernameEnabled
         *         whether show username is enabled
         */
        @ConfigAttribute("selfServiceForgottenUsernameShowUsernameEnabled")
        public void setShowUsernameEnabled(boolean showUsernameEnabled) {
            this.showUsernameEnabled = showUsernameEnabled;
        }

        /**
         * Sets the set of valid query attributes.
         *
         * @param validQueryAttributes
         *         valid query attributes
         */
        @ConfigAttribute("selfServiceValidQueryAttributes")
        public void setValidQueryAttributes(Set<String> validQueryAttributes) {
            this.validQueryAttributes.addAll(validQueryAttributes);
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
        ForgottenUsernameConsoleConfig internalBuild() {
            Reject.ifNull(configProviderClass, "Config provider class name required");
            Reject.ifFalse(tokenExpiry > 0, "Token expiry must be greater than zero");

            if (emailEnabled) {
                Reject.ifTrue(subjectTranslations.isEmpty(), "Subject translations are required");
                Reject.ifTrue(messageTranslations.isEmpty(), "Message translations are required");
            }

            if (kbaEnabled) {
                Reject.ifFalse(minimumAnswersToVerify > 0, "Minimum questions to be verified must be greater than 0");
            }

            return new ForgottenUsernameConsoleConfig(this);
        }

    }

}
