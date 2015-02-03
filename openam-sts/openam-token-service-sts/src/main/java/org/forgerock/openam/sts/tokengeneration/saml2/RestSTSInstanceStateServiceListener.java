package org.forgerock.openam.sts.tokengeneration.saml2;

import org.forgerock.openam.sts.AMSTSConstants;

import javax.inject.Inject;

/**
 * This class implements the ServiceListener interface and leverages the STSInstanceStateProvider interface to
 * invalidate cache entries in the RestSTSInstanceStateProvider when the organizational config of a rest sts instance
 * is updated.
 */
public class RestSTSInstanceStateServiceListener extends STSInstanceStateServiceListenerBase<RestSTSInstanceState> {
    @Inject
    RestSTSInstanceStateServiceListener(STSInstanceStateProvider<RestSTSInstanceState> instanceStateProvider) {
        super(instanceStateProvider, AMSTSConstants.REST_STS_SERVICE_NAME, AMSTSConstants.REST_STS_SERVICE_VERSION);
    }
}
