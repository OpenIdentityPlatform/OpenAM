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
 * Copyright 22014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token;

/**
 * @see org.forgerock.openam.sts.token.UrlConstituentCatenator
 */
public class UrlConstituentCatenatorImpl implements UrlConstituentCatenator {
    private static final String FORWARD_SLASH = "/";
    private static final String QUESTION_MARK = "?";
    //TODO: varargs here - why just two constituents?
    public String catenateUrlConstituents(String first, String second) {
        if ((first==null) || (second==null)) {
            throw new IllegalArgumentException("Arguments cannot be null.");
        }
        if (!first.endsWith(FORWARD_SLASH) && !second.startsWith(FORWARD_SLASH) && !second.startsWith(QUESTION_MARK)) {
            return new StringBuilder(first).append(FORWARD_SLASH).append(second).toString();
        } else if (first.endsWith(FORWARD_SLASH) && second.startsWith(FORWARD_SLASH)) {
            return new StringBuilder(first).append(second.substring(1)).toString();
        } else {
            return new StringBuilder(first).append(second).toString();
        }
    }

    public StringBuilder catentateUrlConstituent(StringBuilder existingUrl, String toBeAddedConstituent) {
        return new StringBuilder(catenateUrlConstituents(existingUrl.toString(), toBeAddedConstituent));
    }
}
