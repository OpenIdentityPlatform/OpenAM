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

package org.forgerock.openam.authentication.modules.fr.oath;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.rest.devices.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.OathDevicesDao;
import org.forgerock.util.Reject;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;

/**
 * Factory object for OATH device profiles. Ensures that consistent device profiles are generated when registering a
 * new OATH 2FA device, for both HOTP and TOTP algorithms. A fresh random shared secret will be generated for each
 * device, from a secure random number generator. The size of the shared secret will be at least as long as the
 * configured minimum secret size. The HOTP counter and TOTP last login times will be initialised to zero.
 *
 * <blockquote>It is not the oath that makes us believe the man, but the man the oath.</blockquote> - Aeschylus.
 *
 * <blockquote>For so sworn good or evil an oath may not be broken and it shall pursue oathkeeper and
 * oathbreaker to the world's end.</blockquote> - J.R.R. Tolkien, The Silmarillion.
 *
 * @since 13.0.0
 */
final class OathMaker {
    private static final int MIN_SHARED_SECRET_BYTE_LENGTH = 8;
    private static final String DEVICE_NAME = "OATH Device";
    private static final long INITIAL_LAST_LOGIN_TIME = 0L;
    private static final int INITIAL_COUNTER_VALUE = 0;

    private final SecureRandom secureRandom;
    private final OathDevicesDao devicesDao;
    private final Debug debug;

    @Inject
    OathMaker(final @Nonnull OathDevicesDao devicesDao,
              final @Nonnull @Named("amAuthOATH") Debug debug,
              final @Nonnull SecureRandom secureRandom) {
        Reject.ifNull(devicesDao, debug, secureRandom);
        this.devicesDao = devicesDao;
        this.debug = debug;
        this.secureRandom = secureRandom;
    }

    /**
     * Creates and saves a fresh device profile for the given user. This will generate a fresh random shared secret
     * for the device, and initialise the counter and last login times to 0, ensuring a valid device profile is
     * present for device registration.
     *
     * @param minSharedSecretLength the minimum length (in hex digits) of the shared secret. The generated random
     *                              shared secret will be at least this many hex digits long.
     * @return the generated device profile.
     */
    OathDeviceSettings createDeviceProfile(int minSharedSecretLength) {
        Reject.ifFalse(minSharedSecretLength >= 0, "minSharedSecretLength must not be negative");

        // Shared secret length is in number of hex digits, multiply by 2 to get bytes.
        int sharedSecretByteLength = Math.max(MIN_SHARED_SECRET_BYTE_LENGTH, (int)Math.ceil(minSharedSecretLength / 2d));

        byte[] secretBytes = new byte[sharedSecretByteLength];
        secureRandom.nextBytes(secretBytes);
        String sharedSecret = DatatypeConverter.printHexBinary(secretBytes);

        return new OathDeviceSettings(sharedSecret, DEVICE_NAME, INITIAL_LAST_LOGIN_TIME,
                INITIAL_COUNTER_VALUE);
    }

    /**
     * Saves the OATH device settings to the user's profile, overwriting any existing device profile.
     *
     * @param user the username of the user to generate a device profile for. Cannot be null.
     * @param realm the realm of the user. Cannot be null.
     * @param deviceSettings the device profile to save. Cannot be null.
     * @throws AuthLoginException if the device profile cannot be saved.
     */
    void saveDeviceProfile(@Nonnull String user, @Nonnull String realm, @Nonnull OathDeviceSettings deviceSettings)
            throws AuthLoginException {
        Reject.ifNull(user, realm, deviceSettings);
        try {
            devicesDao.saveDeviceProfiles(user, realm,
                    JsonConversionUtils.toJsonValues(Collections.singletonList(deviceSettings)));
        } catch (IOException e) {
            debug.error("OathMaker.createDeviceProfile(): Unable to save device profile for user {} in realm {}",
                    user, realm, e);
            throw new AuthLoginException(e);
        }
    }

}
