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
package org.forgerock.openam.authentication.modules.push;

import static org.forgerock.json.JsonValue.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Set;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.services.push.PushNotificationConstants;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.services.push.dispatch.Predicate;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.Time;

/**
 * An abstract push module holding the necessary json serialization classes to communicate with the
 * core token service.
 */
public abstract class AbstractPushModule extends AMLoginModule {

    /** Used to make the polling occur every second. Not recommended to be set in production. **/
    protected final String nearInstantProperty = "com.forgerock.openam.authentication.push.nearinstant";

    /** Used to store tokens which may be updated by other machines in the cluster. **/
    protected final CTSPersistentStore coreTokenService = InjectorHolder.getInstance(CTSPersistentStore.class);

    /** Necessary to read data from the appropriate user's attribute. **/
    protected final UserPushDeviceProfileManager userPushDeviceProfileManager =
            InjectorHolder.getInstance(UserPushDeviceProfileManager.class);

    /** Used to communicate messages out from OpenAM through a configurable Push delegate. */
    protected final PushNotificationService pushService
            = InjectorHolder.getInstance(PushNotificationService.class);

    /** Used to understand what loadbalancer cookie we should inform the remote device of. */
    protected final SessionCookies sessionCookies = InjectorHolder.getInstance(SessionCookies.class);

    private final JSONSerialisation jsonSerialization = InjectorHolder.getInstance(JSONSerialisation.class);

    /**
     * Stores the message information in the CTS, to be used across the cluster.
     *
     * @param messageId The ID of this message.
     * @param servicePredicates Predicates associated with this message.
     * @param timeout The timeout on this message in miliseconds.
     * @throws JsonProcessingException If the predicates could not be serialized.
     * @throws CoreTokenException If the CTS could not be written to.
     */
    protected void storeInCTS(String messageId, Set<Predicate> servicePredicates, long timeout)
            throws JsonProcessingException, CoreTokenException {

        Token ctsToken = new Token(messageId, TokenType.PUSH);
        JsonValue jsonRepresentation = json(object());

        for (Predicate predicate : servicePredicates) {
            jsonRepresentation.put(predicate.getClass().getCanonicalName(), predicate.jsonify());
        }

        String result = jsonSerialization.serialise(jsonRepresentation.getObject());

        ctsToken.setAttribute(CoreTokenField.BLOB, result.getBytes());

        Calendar calendar = Time.getCalendarInstance();
        calendar.add(Calendar.SECOND, (int) (timeout / 1000));
        ctsToken.setExpiryTimestamp(calendar);

        coreTokenService.create(ctsToken);
    }

    /**
     * Retrieves a Push Device for a user in a realm.
     *
     * @param username Name of the user whose device to retrieve.
     * @param realm Realm in which the user is operating.
     * @return The user's PushDeviceSettings, or null if none exist.
     * @throws AuthLoginException If we were unable to read the user's profile.
     */
    protected PushDeviceSettings getDevice(String username, String realm) throws AuthLoginException {

        try {
            PushDeviceSettings firstDevice
                    = CollectionUtils.getFirstItem(userPushDeviceProfileManager.getDeviceProfiles(username, realm));
            if (null == firstDevice) {
                setFailureID(username);
                throw new AuthLoginException(Constants.AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
            }
            return firstDevice;
        } catch (IOException e) {
            setFailureID(username);
            throw new AuthLoginException(Constants.AM_AUTH_AUTHENTICATOR_PUSH, "authFailed", null);
        }
    }

    /**
     * Checks the CTS for the existence of a token with the expected name, and ensures that the deny field
     * is not populated with 'true'.
     *
     * @param tokenId The token's Id.
     * @return true if the CTS has the message and it's not denied, false if the CTS has the message and it is
     * denied, null if the CTS has no message.
     * @throws CoreTokenException if there were issues reading from the CTS.
     */
    protected Boolean checkCTSAuth(String tokenId) throws CoreTokenException {
        Token coreToken = coreTokenService.read(tokenId);

        if (coreToken == null) {
            return null;
        }

        Integer deny = coreToken.getValue(CoreTokenField.INTEGER_ONE);

        if (deny == null) {
            return null;
        } else {
            if (deny == PushNotificationConstants.DENY_VALUE) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    /**
     * Checks the CTS for the existence of a token with the expected name, and ensures that the necessary fields
     * are correctly populated.
     *
     * @param tokenId The token's Id.
     * @return jsonValue containing the necessary data for registration as defined at the top of
     * AuthenticatorPushRegistration.
     * @throws CoreTokenException if there were issues reading from the CTS.
     */
    protected JsonValue checkCTSRegistration(String tokenId) throws CoreTokenException {
        Token coreToken = coreTokenService.read(tokenId);

        if (coreToken == null) {
            return null;
        }

        Integer accept = coreToken.getValue(CoreTokenField.INTEGER_ONE);

        if (accept == null) {
            return null;
        }

        if (accept == PushNotificationConstants.ACCEPT_VALUE) {
            return JsonValueBuilder.toJsonValue(new String(coreToken.getBlob()));
        }

        return null;
    }

}
