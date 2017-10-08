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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.sm.validation;

import com.sun.identity.sm.ServiceAttributeValidator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import org.forgerock.openam.utils.StringUtils;

/**
 * Attempts to create a URL from each provided attributed. If any element in the
 * provided set throws an error, the validation fails.
 *
 * Empty values ARE allowed. To enforce a field as required, use the RequiredValueValidator
 * in addition to this validator.
 */
public class URLValidator implements ServiceAttributeValidator {

    @Override
    public boolean validate(Set<String> values) {

        try {
            for (String value : values) {
                if (StringUtils.isEmpty(value)) {
                    continue;
                }

                new URL(value);
            }
        } catch (MalformedURLException e) {
            return false;
        }

        return true;
    }
}
