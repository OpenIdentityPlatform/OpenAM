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
package org.forgerock.openam.oauth2.validation;

import com.sun.identity.shared.validation.URLValidator;
import com.sun.identity.shared.validation.ValidationException;
import com.sun.identity.shared.validation.ValidatorBase;
import java.net.MalformedURLException;
import java.net.URL;
import org.forgerock.openam.utils.StringUtils;

/**
 * This is simply used to verify that URIs entered to the OAuth2 system's
 * redirect don't contain fragments.
 */
public class OpenIDConnectURLValidator extends ValidatorBase {

    private static OpenIDConnectURLValidator instance = new OpenIDConnectURLValidator();

    private OpenIDConnectURLValidator() {
    }

    /**
     * Returns an instance of this validator.
     */
    public static OpenIDConnectURLValidator getInstance() {
        return instance;
    }

    protected void performValidation(String strData)
            throws ValidationException {
        if (StringUtils.isBlank(strData)) {
            throw new ValidationException(resourceBundleName, URLValidator.ERROR_CODE);
        }

        if (strData.contains("#")) {
            throw new ValidationException(resourceBundleName, URLValidator.ERROR_CODE);
        }

        try {
            new URL(strData);
        } catch (MalformedURLException e) {
            throw new ValidationException(resourceBundleName, URLValidator.ERROR_CODE);
        }
    }
}
