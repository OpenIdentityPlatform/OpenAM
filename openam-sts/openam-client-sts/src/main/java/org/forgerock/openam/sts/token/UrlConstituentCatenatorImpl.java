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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token;

import org.forgerock.openam.utils.StringUtils;

/**
 * @see org.forgerock.openam.sts.token.UrlConstituentCatenator
 */
public class UrlConstituentCatenatorImpl implements UrlConstituentCatenator {
    private static final String FORWARD_SLASH = "/";
    private static final String QUESTION_MARK = "?";
    private static final char FORWARD_SLASH_CHAR = '/';

    @Override
    public String catenateUrlConstituents(String... constituents) {
        StringBuilder aggregator = new StringBuilder(constituents.length);
        boolean queryParamDelimiterEncountered = false;
        for (String constituent : constituents) {
            queryParamDelimiterEncountered |= catenateUrlConstituentsInternal(aggregator, constituent,
                    queryParamDelimiterEncountered);
        }
        return aggregator.toString();
    }

    private boolean catenateUrlConstituentsInternal(StringBuilder aggregator, String toBeAddedConstituent,
                                                    boolean queryParamDelimiterPreviouslyEncountered) {
        if (aggregator == null) {
            throw new IllegalArgumentException("StringBuilder parameter cannot be null.");
        }
        if (StringUtils.isEmpty(toBeAddedConstituent)) {
            return false;
        }
        boolean newQueryParamDelimiterEncountered = toBeAddedConstituent.contains(QUESTION_MARK) ||
                (!queryParamDelimiterPreviouslyEncountered && aggregator.toString().contains(QUESTION_MARK));
        int aggregatorLength = aggregator.length();
        if (aggregatorLength == 0) {
            aggregator.append(toBeAddedConstituent);
        } else if ((FORWARD_SLASH_CHAR != aggregator.charAt(aggregatorLength - 1)) &&
                !toBeAddedConstituent.startsWith(FORWARD_SLASH) && !toBeAddedConstituent.startsWith(QUESTION_MARK)
                && !newQueryParamDelimiterEncountered && !queryParamDelimiterPreviouslyEncountered) {
            aggregator.append(FORWARD_SLASH).append(toBeAddedConstituent);
        } else if ((FORWARD_SLASH_CHAR == aggregator.charAt(aggregatorLength - 1)) && toBeAddedConstituent.startsWith(FORWARD_SLASH)) {
            aggregator.append(toBeAddedConstituent.substring(1));
        } else if ((FORWARD_SLASH_CHAR == aggregator.charAt(aggregatorLength - 1)) && toBeAddedConstituent.startsWith(QUESTION_MARK)) {
            aggregator.deleteCharAt(aggregatorLength - 1).append(toBeAddedConstituent);
        } else {
            aggregator.append(toBeAddedConstituent);
        }
        return newQueryParamDelimiterEncountered;
    }
}
