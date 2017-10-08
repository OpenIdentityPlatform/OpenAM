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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.fr.oath.validators;

import com.sun.identity.sm.ServiceAttributeValidator;
import java.util.Set;
import java.util.regex.Pattern;
import org.forgerock.openam.utils.StringUtils;

/**
 * Service Manager validator for alpha numeric characters.
 */
public class AlphaNumericValidator implements ServiceAttributeValidator {

    private static final Pattern NOT_ALPHA_NUM = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * Validates each of the provided members of the Set to confirm that it conforms
     * to the expected validation - only a-z/A-Z, 0-9.
     *
     * @param values the <code>Set</code> of attribute values to validate
     * @return true if everything is valid, false otherwise.
     */
    @Override
    public boolean validate(Set<String> values) {
        for (String toTest : values) {
            if (StringUtils.isEmpty(toTest) || NOT_ALPHA_NUM.matcher(toTest).find()) {
                return false;
            }
        }

        return true;
    }
}
