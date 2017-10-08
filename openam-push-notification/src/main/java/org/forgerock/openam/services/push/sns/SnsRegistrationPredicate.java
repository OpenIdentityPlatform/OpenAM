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
* Copyright 2016 ForgeRock AS.
*/
package org.forgerock.openam.services.push.sns;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.services.push.PushNotificationException;
import org.forgerock.openam.services.push.PushNotificationServiceConfig;
import org.forgerock.openam.services.push.PushNotificationServiceConfigHelper;
import org.forgerock.openam.services.push.PushNotificationServiceConfigHelperFactory;
import org.forgerock.openam.services.push.dispatch.AbstractPredicate;
import org.forgerock.openam.services.push.sns.utils.SnsClientFactory;
import org.forgerock.openam.services.push.sns.utils.SnsPushResponseUpdater;

/**
 * Acts to register (via communication with SNS) the device that is currently talking to the server
 * from the mobile app.
 *
 * {@see org.forgerock.openam.authentication.modules.push.registration.AuthenticatorPushRegistration} for
 * information on the format which the perform() method alters the data to fit.
 *
 * This predicate does NOT have a listener attached to its config reading subsystem, therefore it will
 * use the values from the config at the point of time the message creates this class.
 */
public class SnsRegistrationPredicate extends AbstractPredicate {

    @JsonIgnore
    private PushNotificationServiceConfigHelperFactory configHelperFactory
            = InjectorHolder.getInstance(PushNotificationServiceConfigHelperFactory.class);

    @JsonIgnore
    private final SnsPushResponseUpdater responseUpdater;

    private String realm;

    /**
     * Generate a new SnsRegistrationPredicate, which will use the supplied realm to read the config
     * to gather the information necessary to communicate with SNS.
     *
     * @param realm The realm in which this predicate exists.
     */
    public SnsRegistrationPredicate(String realm) {
        this();
        this.realm = realm;
    }

    /**
     * Communicates with Amazon to ensure that the device communicating with us is registered with
     * Amazon, and to retrieve the appropriate endpoint ARN which will later be used to
     * communicate with this device.
     *
     * Finally, expands the contents out to be readable by the registration module itself, including
     * the communicationId.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean perform(JsonValue content) {
        PushNotificationServiceConfigHelper configHelper;
        PushNotificationServiceConfig config;
        try {
            configHelper = configHelperFactory.getConfigHelperFor(realm);
            config = configHelper.getConfig();
            responseUpdater.updateResponse(config, content);
        } catch (SSOException | SMSException | PushNotificationException e) {
            return false;
        }

        return true;
    }

    /**
     * Default constructor for the SnsRegistrationPredicate, used for serialization and deserialization
     * purposes.
     */
    public SnsRegistrationPredicate() {
        responseUpdater = new SnsPushResponseUpdater(new SnsClientFactory());
    }

    /**
     * Sets the realm for this predicate. Used when deserilialized from the CTS.
     * @param realm The location for this predicate.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }
}
