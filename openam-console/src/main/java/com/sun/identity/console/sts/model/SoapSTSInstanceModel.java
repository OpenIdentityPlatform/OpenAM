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
import com.sun.identity.shared.datastruct.CollectionHelper;
import org.forgerock.guava.common.collect.Sets;
import org.forgerock.openam.shared.sts.SharedSTSConstants;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Set;

/**
 * The model backing the SoapSTSAddViewBean and the SoapSTSEditViewBean classes
 */
public class SoapSTSInstanceModel extends STSInstanceModelBase {
    /*
    The following three strings must match the prefixes of possible selections in the security-policy-validated-token-config property
    defined in propertySoapSecurityTokenService.xml. The values are used to ensure
    congruence between the selected wsdl file, and the security-policy-validated-token-config selection.
     */
    private static final String OPENAM_SUPPORTING_TOKEN = "OPENAM";
    private static final String USERNAME_SUPPORTING_TOKEN = "USERNAME";
    private static final String X509_SUPPORTING_TOKEN = "X509";

    /*
    The following three strings must match the SupportingToken identifier in the .wsdl file definitions in the
    deployment-wsdl-location property defined in propertySoapSecurityTokenService.xml. The values are used to ensure
    congruence between the selected wsdl file, and the security-policy-validated-token-config selection.
     */
    private static final String USERNAME_SUPPORTING_WSDL_FILE_NAME_CONSTITUENT = "sts_ut";
    private static final String X509_SUPPORTING_WSDL_FILE_NAME_CONSTITUENT = "sts_x509";
    private static final String OPENAM_SUPPORTING_WSDL_FILE_NAME_CONSTITUENT = "sts_am";

    public SoapSTSInstanceModel(HttpServletRequest req, Map map) throws AMConsoleException {
        super(req, AMAdminConstants.SOAP_STS_SERVICE, map);
    }

    @Override
    STSInstanceModelResponse stsTypeSpecificValidation(Map<String, Set<String>> configurationState) {
        if (customWsdlFileLocationEntered(configurationState) &&
                !customWsdlFileSelectedFromDropDown(configurationState)) {
            return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.custom.wsdl.specification.inconsistent.message"));
        }
        if (!customWsdlFileLocationEntered(configurationState) &&
                customWsdlFileSelectedFromDropDown(configurationState)) {
            return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.custom.wsdl.specification.inconsistent.message"));
        }
        if (!customWsdlFileLocationEntered(configurationState) &&
                CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.WSDL_LOCATION))) {
            return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.no.wsdl.specified.message"));
        }
        if (customWsdlSpecified(configurationState)) {
            if (CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.CUSTOM_SERVICE_QNAME)) ||
                        CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.CUSTOM_PORT_QNAME))) {
                return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.custom.wsdl.specification.incomplete.message"));
            } else {
                if (!stringInQNameFormat(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.CUSTOM_SERVICE_QNAME)) ||
                        !stringInQNameFormat(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.CUSTOM_PORT_QNAME))) {
                    return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.custom.service.or.port.not.qname.message"));
                }
            }
        }

        if (CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.AM_DEPLOYMENT_URL))) {
            return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.no.am.deployment.url.message"));
        }
        /*
        For standard wsdl locations, ensure congruence between the selected wsdl and the single entry in the SecurityPolicy
        validated token configuration
         */
        if (!customWsdlSpecified(configurationState)) {
            final Set<String> securityPolicyValidatedTokenSet = configurationState.get(SharedSTSConstants.SECURITY_POLICY_VALIDATED_TOKEN_CONFIG);
            if (CollectionUtils.isEmpty(securityPolicyValidatedTokenSet) || StringUtils.isBlank(securityPolicyValidatedTokenSet.iterator().next())) {
                return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.security.policy.validated.token.config.missing.message"));
            } else if (securityPolicyValidatedTokenSet.size() != 1) {
                return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.security.policy.validated.token.config.wrong.cardinality.message"));

            } else {
                /*
                This value will correspond to one of the possible selections defined in propertySoapSecurityTokenService.xml under the
                security-policy-validated-token-config property
                 */
                final String supportingToken = securityPolicyValidatedTokenSet.iterator().next();
                final String wsdlLocation = CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.WSDL_LOCATION);
                STSInstanceModelResponse congruenceResponse = ensureCongruenceBetweenWsdlLocationAndSupportingToken(supportingToken, wsdlLocation);
                if (!congruenceResponse.isSuccessful())  {
                    return congruenceResponse;
                }
            }
        }
        /*
         if delegation relationship is supported, either out-of-the-box or custom validators have to be specified.
         */
        boolean delegationSupported = Boolean.parseBoolean(
                CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.DELEGATION_RELATIONSHIP_SUPPORTED));
        if (delegationSupported) {
            if (CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.DELEGATION_TOKEN_VALIDATORS)) &&
                    CollectionUtils.isEmpty(configurationState.get(SharedSTSConstants.CUSTOM_DELEGATION_TOKEN_HANDLERS))) {
                return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.no.delegation.hanlders.specified.message"));
            }
        }
        return STSInstanceModelResponse.success();
    }

    private boolean customWsdlFileSelectedFromDropDown(Map<String, Set<String>> configurationState) {
        return SharedSTSConstants.CUSTOM_WSDL_FILE_INDICATOR.equals(CollectionHelper.getMapAttr(configurationState, SharedSTSConstants.WSDL_LOCATION));
    }

    private boolean customWsdlFileLocationEntered(Map<String, Set<String>> configurationState) {
        return CollectionUtils.isNotEmpty(configurationState.get(SharedSTSConstants.CUSTOM_WSDL_LOCATION));
    }

    private boolean customWsdlSpecified(Map<String, Set<String>> configurationState) {
        return customWsdlFileLocationEntered(configurationState) && customWsdlFileSelectedFromDropDown(configurationState);
    }

    private boolean stringInQNameFormat(String customPortOrServiceName) {
        try {
            QName.valueOf(customPortOrServiceName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /*
    Note that this method is called after validation has been performed, so the assumption is that state is correct. Only
    instance-specific state needs to be added.
     */
    @Override
    STSInstanceModelResponse addStsTypeSpecificConfigurationState(Map<String, Set<String>> configurationState) {
        /*
        For standard wsdl locations, the service name and port must be filled-in. As a courtesy to the user, these options
        won't be displayed in the AdminUI, as they are standard to all pre-packaged wsdl files. This logic block will fill
        in these standard values.
        */
        if (!customWsdlSpecified(configurationState)) {
            configurationState.put(SharedSTSConstants.SERVICE_QNAME, Sets.newHashSet(SharedSTSConstants.STANDARD_STS_SERVICE_QNAME.toString()));
            configurationState.put(SharedSTSConstants.PORT_QNAME, Sets.newHashSet(SharedSTSConstants.STANDARD_STS_PORT_QNAME.toString()));
        }
        return STSInstanceModelResponse.success();
    }

    private STSInstanceModelResponse ensureCongruenceBetweenWsdlLocationAndSupportingToken(String supportingToken, String wsdlLocation) {
        /*
        The wsdlLocation will be one of the wsdl locations (besides custom) defined in the deployment-wsdl-location property
        in propertySoapSecurityTokenService. The supportingToken string will be a non-null, non-empty string selected from
        the security-policy-validated-token-config list.
         */
        if (supportingToken.startsWith(OPENAM_SUPPORTING_TOKEN)) {
            if (!wsdlLocation.startsWith(OPENAM_SUPPORTING_WSDL_FILE_NAME_CONSTITUENT)) {
                return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.no.congruence.between.wsdl.and.supporting.token.message"));
            } else {
                return STSInstanceModelResponse.success();
            }
        } else if (supportingToken.startsWith(X509_SUPPORTING_TOKEN)) {
            if (!wsdlLocation.startsWith(X509_SUPPORTING_WSDL_FILE_NAME_CONSTITUENT)) {
                return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.no.congruence.between.wsdl.and.supporting.token.message"));
            } else {
                return STSInstanceModelResponse.success();
            }
        } else if (supportingToken.startsWith(USERNAME_SUPPORTING_TOKEN)) {
            if (!wsdlLocation.startsWith(USERNAME_SUPPORTING_WSDL_FILE_NAME_CONSTITUENT)) {
                return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.no.congruence.between.wsdl.and.supporting.token.message"));
            } else {
                return STSInstanceModelResponse.success();
            }
        } else {
            return STSInstanceModelResponse.failure(getLocalizedString("soap.sts.validation.no.congruence.between.wsdl.and.supporting.token.message"));
        }
    }
}
