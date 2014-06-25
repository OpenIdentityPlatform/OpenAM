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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.rest.publish.service;

import com.google.inject.Key;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.openam.sts.rest.config.RestSTSInjectorHolder;
import org.forgerock.openam.sts.rest.publish.RestSTSInstancePublisher;
import org.slf4j.Logger;

/**
 * Referenced in the web.xml. Returns the ConnectionFactory required by the Crest Rest STS instance publish
 * SingletonResourceProvider.
 */
public class RestSTSPublishServiceConnectionFactoryProvider {
    public static ConnectionFactory getConnectionFactory() {
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/publish",
                new RestSTSPublishServiceRequestHandler(RestSTSInjectorHolder.getInstance(Key.get(RestSTSInstancePublisher.class)),
                        RestSTSInjectorHolder.getInstance(Key.get(Logger.class))));
        return Resources.newInternalConnectionFactory(router);
    }
}
