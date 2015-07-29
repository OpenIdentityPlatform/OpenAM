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
package com.sun.identity.log.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attempts to parse an agent log message to extract useful information.
 *
 * @since 13.0.0
 */
final class AgentLogParser {

    private enum Extractor {

        WEB_AGENT("^user (\\S+) was (\\S+) access to (\\S+)$", 3, 1, 2),
        JAVA_AGENT("^access to (\\S+) (\\S+) for user (\\S+)$", 1, 3, 2);

        final Pattern pattern;
        final int resourceIndex;
        final int subjectIndex;
        final int statusIndex;

        Extractor(String pattern, int resourceIndex, int subjectIndex, int statusIndex) {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            this.resourceIndex = resourceIndex;
            this.subjectIndex = subjectIndex;
            this.statusIndex = statusIndex;
        }

        Matcher newMatcher(String message) {
            return pattern.matcher(message);
        }

    }

    /**
     * Given the log message, attempts to parse and extract known parts.
     *
     * @param message
     *         the log message
     *
     * @return the log extracts, null if parsing fails
     */
    LogExtracts tryParse(String message) {
        for (Extractor extractor : Extractor.values()) {
            Matcher matcher = extractor.newMatcher(message);

            if (matcher.matches()) {
                return extract(extractor, matcher);
            }
        }

        return null;
    }

    private LogExtracts extract(Extractor extractor, Matcher matcher) {
        String resourceUrl = matcher.group(extractor.resourceIndex);
        String subjectId = matcher.group(extractor.subjectIndex);
        String status = matcher.group(extractor.statusIndex);
        return new LogExtracts(resourceUrl, subjectId, status);
    }

    final static class LogExtracts {

        private final String resourceUrl;
        private final String subjectId;
        private final String status;

        private LogExtracts(String resourceUrl, String subjectId, String status) {
            this.resourceUrl = resourceUrl;
            this.subjectId = subjectId;
            this.status = status;
        }

        String getResourceUrl() {
            return resourceUrl;
        }

        String getSubjectId() {
            return subjectId;
        }

        String getStatus() {
            return status;
        }

    }

}
