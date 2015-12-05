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

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import org.forgerock.openam.selfservice.config.SelfServiceConsoleConfig;
import org.forgerock.openam.sm.config.ConfigAttribute;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.util.Reject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents common console configuration used by all self services.
 *
 * @supported.all.api
 * @since 13.0.0
 */
abstract class CommonConsoleConfig implements SelfServiceConsoleConfig {

    private final Map<String, Set<String>> attributes;
    private final String siteKey;
    private final String secretKey;
    private final String verificationUrl;
    private final Map<String, Map<String, String>> securityQuestions;
    private final String emailAttributeName;

    protected CommonConsoleConfig(CommonConsoleConfigBuilder builder) {
        attributes = builder.attributes;
        siteKey = builder.siteKey;
        secretKey = builder.secretKey;
        verificationUrl = builder.verificationUrl;
        securityQuestions = builder.securityQuestions;
        emailAttributeName = builder.emailAttributeName;
    }

    /**
     * Gets the captcha site key.
     *
     * @return the captcha site key
     */
    public final String getCaptchaSiteKey() {
        return siteKey;
    }

    /**
     * Gets the captcha secret key.
     *
     * @return the captcha secret key
     */
    public final String getCaptchaSecretKey() {
        return secretKey;
    }

    /**
     * Gets the captcha verification URL.
     *
     * @return the captcha verification URL
     */
    public final String getCaptchaVerificationUrl() {
        return verificationUrl;
    }

    /**
     * Gets the security questions.
     * Expected format: <pre>Map&lt;id,Map&lt;locale,question&gt;&gt;</pre>
     *
     * @return security questions
     */
    public final Map<String, Map<String, String>> getSecurityQuestions() {
        return securityQuestions;
    }

    /**
     * Gets the email attribute name.
     *
     * @return the email attribute name
     */
    public final String getEmailAttributeName() {
        return emailAttributeName;
    }

    /**
     * Retrieves the underlying console attribute for the key.
     *
     * @param key
     *         console attribute key
     *
     * @return corresponding string value
     *
     * @supported.api
     */
    public final String getAttributeAsString(String key) {
        Set<String> attribute = attributes.get(key);
        return isEmpty(attribute) ? null : attribute.iterator().next();
    }

    /**
     * Retrieves the underlying console attribute for the key.
     *
     * @param key
     *         console attribute key
     *
     * @return corresponding set value
     *
     * @supported.api
     */
    public final Set<String> getAttributeAsSet(String key) {
        return attributes.get(key);
    }

    protected abstract static class CommonConsoleConfigBuilder<C> implements ConsoleConfigBuilder<C> {

        private Map<String, Set<String>> attributes;
        private String siteKey;
        private String secretKey;
        private String verificationUrl;
        private final Map<String, Map<String, String>> securityQuestions;
        private String emailAttributeName;

        protected CommonConsoleConfigBuilder() {
            securityQuestions = new HashMap<>();
        }

        @ConfigAttribute(value = "selfServiceCaptchaSiteKey", required = false)
        public final void setSiteKey(String siteKey) {
            this.siteKey = siteKey;
        }

        @ConfigAttribute(value = "selfServiceCaptchaSecretKey", required = false)
        public final void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        @ConfigAttribute("selfServiceCaptchaVerificationUrl")
        public final void setVerificationUrl(String verificationUrl) {
            this.verificationUrl = verificationUrl;
        }

        @ConfigAttribute(value = "selfServiceKBAQuestions", transformer = SecurityQuestionTransformer.class)
        public final void setSecurityQuestions(Map<String, Map<String, String>> securityQuestions) {
            this.securityQuestions.putAll(securityQuestions);
        }

        @ConfigAttribute("openamEmailAttribute")
        public final void setEmailAttributeName(String emailAttributeName) {
            this.emailAttributeName = emailAttributeName;
        }

        @Override
        public final C build(Map<String, Set<String>> attributes) {
            this.attributes = attributes;

            if (isCaptchaEnabled()) {
                Reject.ifNull(siteKey, "Captcha site key is required");
                Reject.ifNull(secretKey, "Captcha secret key is required");
                Reject.ifNull(verificationUrl, "Captcha verification url is required");
            }

            if (isKbaEnabled()) {
                Reject.ifNull(securityQuestions, "Security questions are required");
                Reject.ifTrue(securityQuestions.isEmpty(), "Security questions are required");
            }

            return internalBuild();
        }

        abstract boolean isCaptchaEnabled();

        abstract boolean isKbaEnabled();

        abstract C internalBuild();

    }

}
