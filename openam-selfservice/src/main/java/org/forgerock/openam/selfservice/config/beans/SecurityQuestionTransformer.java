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

import org.forgerock.openam.sm.config.ConfigTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Security question transformer.
 * <p/>
 * Expected string format is <pre>id|locale|question</pre>.
 *
 * @since 13.0.0
 */
public final class SecurityQuestionTransformer implements ConfigTransformer<Map<String, Map<String, String>>> {

    private final static Pattern QUESTION_PATTERN = Pattern.compile("^(\\w+)\\|(\\w+)\\|(.+)$");

    @Override
    public Map<String, Map<String, String>> transform(Set<String> values, Class<?> parameterType) {
        Map<String, Map<String, String>> localisedQuestions = new HashMap<>();

        for (String question : values) {
            Matcher matcher = QUESTION_PATTERN.matcher(question);

            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        "Expected question format id|locale|question but got " + question);
            }

            String id = matcher.group(1);
            String locale = matcher.group(2);
            String text = matcher.group(3);

            if (!localisedQuestions.containsKey(id)) {
                localisedQuestions.put(id, new HashMap<String, String>());
            }

            localisedQuestions.get(id).put(locale, text);
        }

        return localisedQuestions;
    }

}
