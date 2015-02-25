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

package org.forgerock.openam.sts.rest.service;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.rest.config.RestSTSInjectorHolder;

/**
 * Provides the ConnectionFactory for all Rest STS instances. This method will be called when the
 * Crest servlet is constructed by the web-container. As such it is an appropriate entry point for initializing rest sts
 * infrastructure state - i.e. the state related to the publication of rest sts instances, as this is the first entry
 * point into the rest-sts. The RestSTSInjectorHolder is an enum, and thus referencing it will cause its default
 * constructor to run, which establishes the Guice bindings common to all rest-sts instances. These bindings establish
 * the ConnectionFactory which is obtained and returned from the getConnectionFactory method.
 *
 * Note that a Hollywood-principal-violating call to the Guice context is required because Crest expects to call a static
 * method ot obtain its ConnectionFactory. Even if it expected a non-static method, it would be in control of the class'
 * construction, thereby preventing the injection of any dependencies. Likewise, if a custom servlet were written, which
 * then encapsulated the Crest servlet, the problem would be no different, as the web-container controls the lifecycle of
 * the custom servlet.
 */
public class RestSTSServiceConnectionFactoryProvider {
    public static ConnectionFactory getConnectionFactory() {
        return RestSTSInjectorHolder.getInstance(Key.get(ConnectionFactory.class,
                Names.named(AMSTSConstants.REST_STS_CONNECTION_FACTORY_NAME)));
    }
}
