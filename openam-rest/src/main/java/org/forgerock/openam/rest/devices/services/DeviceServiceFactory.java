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
package org.forgerock.openam.rest.devices.services;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Defines the interface for providing {@link OathServiceFactory}s.
 */
public interface DeviceServiceFactory {

    /**
     * Creates a new OathService for the current realm.
     * @param realm The realm for which to grab the OathService.
     *
     * @throws SMSException If we cannot connect to/read from SMS.
     * @throws SSOException If we cannot utilise the Admin token for access to the service.
     */
    DeviceService create(String realm) throws SMSException, SSOException;

}
