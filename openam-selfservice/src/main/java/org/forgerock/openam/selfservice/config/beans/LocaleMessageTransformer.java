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

import static com.sun.identity.shared.locale.Locale.getLocale;

import org.forgerock.openam.sm.config.ConfigTransformer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Locale message transformer.
 * <p/>
 * Expected string format is <pre>locale|message</pre>.
 *
 * @since 13.0.0
 */
final class LocaleMessageTransformer implements ConfigTransformer<Map<Locale, String>> {

    private final static Pattern LOCALE_MESSAGE_PATTERN = Pattern.compile("^(\\w+)\\|(.+)$");

    @Override
    public Map<Locale, String> transform(Set<String> values, Class<?> parameterType) {
        Map<Locale, String> messageTranslations = new HashMap<>();

        for (String value : values) {
            Matcher matcher = LOCALE_MESSAGE_PATTERN.matcher(value);

            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        "Expected format locale|message but got " + value);
            }

            String localeString = matcher.group(1);
            String message = matcher.group(2);

            messageTranslations.put(getLocale(localeString), message);
        }

        return messageTranslations;
    }

}
