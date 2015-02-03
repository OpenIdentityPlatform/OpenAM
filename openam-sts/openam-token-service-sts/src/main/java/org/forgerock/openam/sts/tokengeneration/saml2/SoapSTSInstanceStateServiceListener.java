package org.forgerock.openam.sts.tokengeneration.saml2;

import org.forgerock.openam.sts.AMSTSConstants;

import javax.inject.Inject;

/**
 * This class implements the ServiceListener interface and leverages the STSInstanceStateProvider interface to
 * invalidate cache entries in the RestSTSInstanceStateProvider when the organizational config of a rest sts instance
 * is updated.
 */
public class SoapSTSInstanceStateServiceListener extends STSInstanceStateServiceListenerBase<SoapSTSInstanceState> {
    @Inject
    SoapSTSInstanceStateServiceListener(STSInstanceStateProvider<SoapSTSInstanceState> instanceStateProvider) {
        super(instanceStateProvider, AMSTSConstants.SOAP_STS_SERVICE_NAME, AMSTSConstants.SOAP_STS_SERVICE_VERSION);
    }
}
