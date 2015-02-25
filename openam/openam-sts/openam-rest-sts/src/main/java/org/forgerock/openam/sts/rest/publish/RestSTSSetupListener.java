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
import com.sun.identity.setup.SetupListener;
import org.forgerock.openam.sts.STSPublishException;
import org.forgerock.openam.sts.rest.config.RestSTSInjectorHolder;
import org.slf4j.Logger;

/**
 * This class is registered with the AMSetupServlet via an entry in the com.sun.identity.setup.SetupListener file contained
 * under resources/META-INF.services under openam-core. These listeners will only be called once OpenAM installation has
 * been completed. The problem is that existing rest-sts instances need to be republished, and the RestSTSPublishServiceListener
 * registered to listen for SMS changes so that rest-sts instances published to other OpenAM instances in a site deployment
 * can be published to the local OpenAM instance. The problem is that ServiceListener registration requires an
 * special user token, which is not available until after OpenAM is installed. So if I register a ServiceListener, and
 * republish existing rest-sts instances only if AMSetupServlet.isCurrentConfigurationValid() returns true, an OpenAM
 * restart is required to get all OpenAM instances in a site into the same state regarding previously-published rest-sts
 * instances, and their ability to stay synchronized when new instances are published.
 *
 * Luckily, the AMStartupServlet#registerListeners() provides a solution to this problem. Here, the registered SetupListeners
 * are called, as soon as OpenAM is installed. This only happens once per OpenAM startup, in AMSetupServlet#init.
 */
public class RestSTSSetupListener implements SetupListener {
    /**
     * Republish any existing rest-sts instances obtained from the SMS, and register a ServiceListener to respond when
     * new rest-sts instances are written to the SMS.
     */
    @Override
    public void addListener() {
        new Thread(
            new Runnable() {
                public void run() {
                    Logger logger = null;
                    try {
                        logger = RestSTSInjectorHolder.getInstance(Key.get(Logger.class));
                        RestSTSInstancePublisher publisher = RestSTSInjectorHolder.getInstance(Key.get(RestSTSInstancePublisher.class));
                        /*
                        Don't register the ServiceListener until after the SMS-resident rest-sts instances have been re-published
                        upon startup. The ServiceListener is only there to bring the rest-sts-instance CREST router in congruence
                        with the state of the SMS in site deployments.
                         */
                        publisher.republishExistingInstances();
                        publisher.registerServiceListener();
                    } catch (STSPublishException e) {
                        if (logger != null) {
                            logger.error("Exception caught republishing existing Rest STS instances: ", e);
                        } else {
                            System.out.println("Exception caught republishing existing Rest STS instances: " + e);
                        }
                    }
                }
            }
        ).start();
    }
}
