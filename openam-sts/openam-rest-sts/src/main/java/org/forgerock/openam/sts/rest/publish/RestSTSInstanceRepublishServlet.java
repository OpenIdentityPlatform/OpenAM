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

package org.forgerock.openam.sts.rest.publish;

import com.google.inject.Key;
import com.sun.identity.setup.AMSetupServlet;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.rest.config.RestSTSInjectorHolder;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 *  This class is an HttpServlet which implements only the init method. All other methods return 400/405 errors as
 *  implemented in the HttpServlet class. The init method will re-constitute previously-published Rest STS instances by
 *  1. calling a GET on the publish service to obtain the configuration corresponding to previously-published instances
 *  and 2. POSTing to the publish service with the configuration instances obtained from #1 to re-expose these
 *  previously-published instances.
 */
public class RestSTSInstanceRepublishServlet extends HttpServlet {
    private final Logger logger;

    public RestSTSInstanceRepublishServlet() {
        logger = RestSTSInjectorHolder.getInstance(Key.get(Logger.class));
    }
    @Override
    public void init() throws ServletException {
        /*
        Don't reference the RestSTSInstancePublisher if we are installing OpenAM, as the RestSTSInstancePublisherImpl
        ctor attempts to register a ServiceListener, which requires an Admin token, which will fail prior to OpenAM
        installation.
         */
        if (AMSetupServlet.isCurrentConfigurationValid()) {
            try {
                RestSTSInstancePublisher publisher = RestSTSInjectorHolder.getInstance(Key.get(RestSTSInstancePublisher.class));
                /*
                Don't register the ServiceListener until after the SMS-resident rest-sts instances have been re-published
                upon startup. The ServiceListener is only there to bring the rest-sts-instance CREST router in congruence
                with the state of the SMS in site deployments.
                 */
                publisher.republishExistingInstances();
                publisher.registerServiceListener();
            } catch (STSPublishException e) {
                logger.error("Exception caught republishing existing Rest STS instances: " + e);
            }
        }
    }
}
