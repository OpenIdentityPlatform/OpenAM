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
package org.forgerock.openam.core.rest.devices.services;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

/**
 * Produces AuthenticatorOathService's for a specific realm.
 */
public class AuthenticatorOathServiceFactory implements DeviceServiceFactory {

    @Override
    public AuthenticatorOathService create(String realm) throws SSOException, SMSException {
        return new AuthenticatorOathService(realm);
    }
}
