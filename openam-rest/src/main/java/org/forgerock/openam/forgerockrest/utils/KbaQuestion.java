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
package org.forgerock.openam.forgerockrest.utils;

import org.forgerock.openam.utils.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * This class represents a Knowledge Based Authentication question.  As such, you can retrieve text from the datastore
 * (LDAP) and convert it into a question, but not vice versa.
 *
 * @since 13.0.0
 */
public class KbaQuestion {

    private static final String SEPARATOR = "|";

    private Locale locale;
    private String uuid;
    private String question;

    /**
     * Construct a question directly from the entry in the datastore.
     * @param ldapEntry The string retrieved directly from the datastore.
     */
    public KbaQuestion(String ldapEntry) {
        decode(ldapEntry);
    }

    /**
     * Decode the string (which has most probably come straight from LDAP) into a KBA question, or throw an exception
     * if the format is not valid.
     * @param s The incoming string
     * @throws IllegalArgumentException If the string cannot be parsed.
     */
    private void decode(String s)  throws IllegalArgumentException {
        if (StringUtils.isEmpty(s)) {
            throw new IllegalArgumentException("Cannot decode an empty KBA entry");
        }

        String[] parts = s.split(Pattern.quote(SEPARATOR));
        if (parts.length != 3) {
            throw new IllegalArgumentException("Splitting KBA value \"" + s + "\" gave "
                                        + parts.length
                                        + " parts, not the expected 3");
        }
        setLocale(parts[0]);
        this.question = parts[1];
        this.uuid = parts[2];
    }

    /**
     * @return The locale of the question
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Set the locale of the question
     * @param localeString The local string in its "usual" form.
     */
    public void setLocale(String localeString) {
        if (StringUtils.isNotEmpty(localeString)) {
            switch (localeString.length()) {
                case 2:
                    this.locale = new Locale(localeString);
                    break;
                case 5:
                    this.locale = new Locale(localeString.substring(0, 2), localeString.substring(3, 5));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid locale specified " + localeString);
            }
        }
    }

    /**
     * @return the UUID of the question.
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid The UUID to set.
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return The text of the question.
     */
    public String getQuestion() {
        return question;
    }

    /**
     * @param question The text of the question.
     */
    public void setQuestion(String question) {
        this.question = question;
    }
}
