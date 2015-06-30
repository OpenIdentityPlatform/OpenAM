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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.console.sts.model;

import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.utils.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is the model backing the RestSTSAddViewBean and RestSTSEditViewBean classes.
 */
public class RestSTSInstanceModel extends STSInstanceModelBase {
    /*
    A string matching a regular expression which will match the '|' character, which needs to be escaped to
    escape its regular-expression semantics.
     */
    private static final String REGEX_PIPE = "\\|";

    public RestSTSInstanceModel(HttpServletRequest req, Map map) throws AMConsoleException {
        super(req, AMAdminConstants.REST_STS_SERVICE, map);
    }

    @Override
    STSInstanceModelResponse addStsTypeSpecificConfigurationState(Map<String, Set<String>> configurationState) {
        // no rest-sts specific state to add
        return STSInstanceModelResponse.success();
    }

    @Override
    STSInstanceModelResponse stsTypeSpecificValidation(Map<String, Set<String>> configurationState) {
        final Set<String> supportedTokenTransforms = configurationState.get(SharedSTSConstants.SUPPORTED_TOKEN_TRANSFORMS);
        if (CollectionUtils.isEmpty(supportedTokenTransforms)) {
            return STSInstanceModelResponse.failure(getLocalizedString("rest.sts.validation.tokentransforms.message"));
        }
        /*
        Need to check if selected transforms include both the validate_interim_session and !invalidate_interim_session
        flavors. If the token transformation set includes two entries for a specific input token type, then this is the
        case, and the configuration must be rejected.
         */
        if (duplicateTransformsSpecified(supportedTokenTransforms)) {
            return STSInstanceModelResponse.failure(getLocalizedString("rest.sts.validation.tokentransforms.duplicate.message"));
        }
        return STSInstanceModelResponse.success();
    }

    /**
     * The set of possible token transformation definition selections, as defined in the supported-token-transforms property
     * in propertyRestSecurityTokenService.xml, is as follow:
     *      USERNAME|SAML2|true
     *      USERNAME|SAML2|false
     *      OPENIDCONNECT|SAML2|true
     *      OPENIDCONNECT|SAML2|false
     *      OPENAM|SAML2|true
     *      OPENAM|SAML2|false
     *      X509|SAML2|true
     *      X509|SAML2|false
     *      USERNAME|OPENIDCONNECT|true
     *      USERNAME|OPENIDCONNECT|false
     *      OPENIDCONNECT|OPENIDCONNECT|true
     *      OPENIDCONNECT|OPENIDCONNECT|false
     *      OPENAM|OPENIDCONNECT|true
     *      OPENAM|OPENIDCONNECT|false
     *      X509|OPENIDCONNECT|true
     *      X509|OPENIDCONNECT|false
     * This method will return true if the supportedTokenTransforms method specified by the user contains more than a single
     * entry for a given input token type per given output token type.
     * @param supportedTokenTransforms The set of supported token transformations specified by the user
     * @return true if duplicate transformations are specified - i.e. the user cannot specify token transformations with
     * USERNAME input which specify that interim OpenAM sessions should be, and should not be, invalidated.
     */
    private boolean duplicateTransformsSpecified(Set<String> supportedTokenTransforms) {
        Set<String> inputOutputComboSet = new HashSet<>(supportedTokenTransforms.size());
        for (String transform : supportedTokenTransforms) {
            String[] breakdown = transform.split(REGEX_PIPE);
            String entry = breakdown[0] + breakdown[1];
            if (inputOutputComboSet.contains(entry)) {
                return true;
            } else {
                inputOutputComboSet.add(entry);
            }
        }
        return false;
    }
}
