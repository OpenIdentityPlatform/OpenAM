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

import static com.sun.identity.shared.datastruct.CollectionHelper.getIntMapAttrThrows;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapSetThrows;

import com.sun.identity.shared.datastruct.ValueNotFoundException;
import org.forgerock.openam.selfservice.config.ConsoleConfigExtractor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts out the KBA console configuration.
 *
 * @since 13.0.0
 */
public final class KbaConsoleConfigExtractor implements ConsoleConfigExtractor<KbaConsoleConfig> {

    private final static String SECURITY_QUESTIONS_KEY = "forgerockRESTSecurityKBAQuestions";
    private final static String MIN_QUESTIONS_TO_ANSWERED_KEY = "forgerockRESTSecurityQuestionsUserMustAnswer";
    private final static String MIN_ANSWERS_TO_PROVIDE_KEY = "forgerockRESTSecurityAnswersUserMustProvide";

    private final static Pattern questionPattern = Pattern.compile("^(\\w+)\\|(\\w+)\\|(.+)$");

    @Override
    public KbaConsoleConfig extract(Map<String, Set<String>> consoleAttributes) {
        try {
            return KbaConsoleConfig
                    .newBuilder()
                    .setSecurityQuestions(parseQuestions(getMapSetThrows(consoleAttributes, SECURITY_QUESTIONS_KEY)))
                    .setMinimumAnswersToDefine(getIntMapAttrThrows(consoleAttributes, MIN_ANSWERS_TO_PROVIDE_KEY))
                    .setMinimumAnswersToVerify(getIntMapAttrThrows(consoleAttributes, MIN_QUESTIONS_TO_ANSWERED_KEY))
                    .build();

        } catch (ValueNotFoundException e) {
            throw new IllegalArgumentException("Invalid console values", e);
        }
    }

    private Map<String, Map<String, String>> parseQuestions(Collection<String> questions) {
        Map<String, Map<String, String>> localisedQuestions = new HashMap<>();

        for (String question : questions) {
            Matcher matcher = questionPattern.matcher(question);

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
