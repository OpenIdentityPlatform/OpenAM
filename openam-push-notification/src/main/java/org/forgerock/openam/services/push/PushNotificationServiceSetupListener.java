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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.services.push;

import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.SetupListener;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Set;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.utils.RealmUtils;

/**
 * Starts the Push Notification Service on each realm as soon as the server starts, to
 * ensure that endpoints are appropriately created and made available.
 */
public class PushNotificationServiceSetupListener implements SetupListener {

    /**
     * Generates a new thread once the setup is completed which
     * executes the task at hand - checking over each realm and calling
     * init on its Push Notification Service.
     */
    @Override
    public void setupComplete() {
        new Thread(
                new Runnable() {
                    public void run() {
                        PushNotificationService service = InjectorHolder.getInstance(PushNotificationService.class);

                        Set<String> realms = new HashSet<>();
                        try {
                            realms.addAll(RealmUtils.getRealmNames(
                                            AccessController.doPrivileged(AdminTokenAction.getInstance())));

                            for (String realm : realms) {
                                try {
                                    service.init(realm);
                                } catch (PushNotificationException e) {
                                    //do nothing - service doesn't exist on this realm
                                }
                            }

                        } catch (SMSException e) {
                            //thrown if the admin token is invalid
                        }

                        service.registerServiceListener();
                    }
                }
        ).start();
    }
}
