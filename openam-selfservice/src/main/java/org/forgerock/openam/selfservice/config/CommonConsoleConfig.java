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

import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Represents common console configuration used by all self services.
 *
 * @since 13.0.0
 */
abstract class CommonConsoleConfig implements ConsoleConfig {

    private final Map<String, Set<String>> consoleAttributes;
    private final boolean enabled;
    private final boolean emailVerificationEnabled;
    private final String emailUrl;
    private final boolean captchaEnabled;
    private final String siteKey;
    private final String secretKey;
    private final String verificationUrl;
    private final boolean kbaEnabled;
    private final Map<String, Map<String, String>> securityQuestions;
    private final String configProviderClass;
    private final long tokenExpiry;
    private final Map<Locale, String> subjectTranslations;
    private final Map<Locale, String> messageTranslations;

    protected CommonConsoleConfig(Builder builder) {
        consoleAttributes = builder.consoleAttributes;
        configProviderClass = builder.configProviderClass;
        enabled = builder.enabled;
        emailVerificationEnabled = builder.emailVerificationEnabled;
        emailUrl = builder.emailUrl;
        tokenExpiry = builder.tokenExpiry;
        captchaEnabled = builder.captchaEnabled;
        siteKey = builder.siteKey;
        secretKey = builder.secretKey;
        verificationUrl = builder.verificationUrl;
        kbaEnabled = builder.kbaEnabled;
        securityQuestions = builder.securityQuestions;
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
    public boolean isEmailVerificationEnabled() {
        return emailVerificationEnabled;
    }

    /**
     * Gets the url to be used within the email.
     *
     * @return the email url
     */
    public String getEmailUrl() {
        return emailUrl;
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
     * Gets the captcha site key.
     *
     * @return the captcha site key
     */
    public String getCaptchaSiteKey() {
        return siteKey;
    }

    /**
     * Gets the captcha secret key.
     *
     * @return the captcha secret key
     */
    public String getCaptchaSecretKey() {
        return secretKey;
    }

    /**
     * Gets the captcha verification URL.
     *
     * @return the captcha verification URL
     */
    public String getCaptchaVerificationUrl() {
        return verificationUrl;
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
     * Gets the security questions in the expected format:
     * <pre>Map&lt;id,Map&lt;locale,question&gt;&gt;</pre>
     *
     * @return security questions
     */
    public Map<String, Map<String, String>> getSecurityQuestions() {
        return securityQuestions;
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
     * Retrieves the underlying console attribute for the key.
     *
     * @param key
     *         console attribute key
     *
     * @return corresponding string value
     */
    public String getAttributeAsString(String key) {
        Set<String> attribute = consoleAttributes.get(key);
        return attribute == null || attribute.isEmpty()
                ? null : attribute.iterator().next();
    }

    /**
     * Retrieves the underlying console attribute for the key.
     *
     * @param key
     *         console attribute key
     *
     * @return corresponding set value
     */
    public Set<String> getAttributeAsSet(String key) {
        return consoleAttributes.get(key);
    }

    abstract static class Builder<C extends ConsoleConfig, B extends Builder<C, B>> {

        private final Map<String, Set<String>> consoleAttributes;

        private boolean enabled;
        private boolean emailVerificationEnabled;
        private String emailUrl;
        private boolean captchaEnabled;
        private String siteKey;
        private String secretKey;
        private String verificationUrl;
        private boolean kbaEnabled;
        private final Map<String, Map<String, String>> securityQuestions;
        private String configProviderClass;
        private long tokenExpiry;
        private final Map<Locale, String> subjectTranslations;
        private final Map<Locale, String> messageTranslations;

        protected Builder(Map<String, Set<String>> consoleAttributes) {
            Reject.ifNull(consoleAttributes);
            this.consoleAttributes = consoleAttributes;

            securityQuestions = new HashMap<>();
            subjectTranslations = new HashMap<>();
            messageTranslations = new HashMap<>();
        }

        B setConfigProviderClass(String configProviderClass) {
            this.configProviderClass = configProviderClass;
            return getThis();
        }

        B setEnabled(boolean enabled) {
            this.enabled = enabled;
            return getThis();
        }

        B setEmailVerificationEnabled(boolean emailVerificationEnabled) {
            this.emailVerificationEnabled = emailVerificationEnabled;
            return getThis();
        }

        B setEmailUrl(String emailUrl) {
            this.emailUrl = emailUrl;
            return getThis();
        }

        B setTokenExpiry(long tokenExpiry) {
            this.tokenExpiry = tokenExpiry;
            return getThis();
        }

        B setCaptchaEnabled(boolean captchaEnabled) {
            this.captchaEnabled = captchaEnabled;
            return getThis();
        }

        B setSiteKey(String siteKey) {
            this.siteKey = siteKey;
            return getThis();
        }

        B setSecretKey(String secretKey) {
            this.secretKey = secretKey;
            return getThis();
        }

        B setVerificationUrl(String verificationUrl) {
            this.verificationUrl = verificationUrl;
            return getThis();
        }

        B setKbaEnabled(boolean kbaEnabled) {
            this.kbaEnabled = kbaEnabled;
            return getThis();
        }

        B setSecurityQuestions(Map<String, Map<String, String>> securityQuestions) {
            this.securityQuestions.putAll(securityQuestions);
            return getThis();
        }

        B setSubjectTranslations(Map<Locale, String> subjectTranslations) {
            this.subjectTranslations.putAll(subjectTranslations);
            return getThis();
        }

        B setMessageTranslations(Map<Locale, String> messageTranslations) {
            this.messageTranslations.putAll(messageTranslations);
            return getThis();
        }

        C build() {
            Reject.ifNull(configProviderClass, "Config provide class name required");
            Reject.ifFalse(tokenExpiry > 0, "Token expiry must be greater than zero");

            if (emailVerificationEnabled) {
                Reject.ifNull(emailUrl, "Email verification url required");
                Reject.ifTrue(subjectTranslations.isEmpty(), "Subject translations are missing");
                Reject.ifTrue(messageTranslations.isEmpty(), "Message translations are missing");
            }

            if (captchaEnabled) {
                Reject.ifNull(siteKey, "Captcha site key is required");
                Reject.ifNull(secretKey, "Captcha secret key is required");
                Reject.ifNull(verificationUrl, "Captcha verification url is required");
            }

            if (kbaEnabled) {
                Reject.ifTrue(securityQuestions.isEmpty(), "Security questions required");
            }

            return internalBuild();
        }

        abstract B getThis();

        abstract C internalBuild();
    }

}
