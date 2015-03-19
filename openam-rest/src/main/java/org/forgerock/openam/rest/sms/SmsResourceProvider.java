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

package org.forgerock.openam.rest.sms;

import static org.forgerock.openam.forgerockrest.RestUtils.*;

import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.rest.resource.RealmContext;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;

abstract class SmsResourceProvider {

    protected final String serviceName;
    protected final String serviceVersion;

    SmsResourceProvider(ServiceSchema schema) {
        this.serviceName = schema.getServiceName();
        this.serviceVersion = schema.getVersion();
    }

    protected String realmFor(ServerContext context) {
        return context.asContext(RealmContext.class).getResolvedRealm();
    }

    protected ServiceConfigManager getServiceConfigManager(ServerContext context) throws SSOException, SMSException {
        SSOTokenManager mgr = SSOTokenManager.getInstance();
        SSOToken token = mgr.createSSOToken(getCookieFromServerContext(context));
        return new ServiceConfigManager(token, serviceName, serviceVersion);
    }

}
