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

package org.forgerock.openam.sts.tokengeneration.state;

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
