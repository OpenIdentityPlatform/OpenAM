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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.deviceprint;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import java.util.Map;

import static org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave.NAME_PROFILE_STATE;
import static org.forgerock.openam.authentication.modules.deviceprint.DeviceIdSave.SAVE_PROFILE_STATE;

/**
 * Handles the processing of state of the {@link DeviceIdSave} login module.
 *
 * @since 12.0.0
 */
public class PersistModuleProcessor {

    private static final String BUNDLE_NAME = "amAuthDeviceIdSave";
    private static final int STORE_PROFILE_CHOICE = 0;

    private final Map<String, Object> devicePrintProfile;
    private final boolean autoStoreProfiles;
    private final ProfilePersister profilePersister;

    /**
     * Constructs a new instance of the PersistModuleProcessor.
     *
     * @param devicePrintProfile The device print profile.
     * @param autoStoreProfiles {@code true} if new profiles should be automatically stored.
     * @param profilePersister An instance of the ProfilePersister.
     */
    PersistModuleProcessor(Map<String, Object> devicePrintProfile, boolean autoStoreProfiles,
            ProfilePersister profilePersister) {
        this.devicePrintProfile = devicePrintProfile;
        this.autoStoreProfiles = autoStoreProfiles;
        this.profilePersister = profilePersister;
    }

    /**
     * Handles the processing of state.
     *
     * @param callbacks The callbacks.
     * @param state The current state.
     * @return The new state.
     * @throws AuthLoginException If the state is invalid.
     */
    int process(Callback[] callbacks, int state) throws AuthLoginException {

        switch (state) {
            case ISAuthConstants.LOGIN_START: {

                if (devicePrintProfile == null || devicePrintProfile.isEmpty()) {
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else if (autoStoreProfiles) {
                    profilePersister.saveDevicePrint(devicePrintProfile);
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else {
                    return SAVE_PROFILE_STATE;
                }
            }
            case SAVE_PROFILE_STATE: {

                ChoiceCallback choiceCallback = (ChoiceCallback) callbacks[0];

                if (choiceCallback.getSelectedIndexes()[0] == STORE_PROFILE_CHOICE) {
                    return NAME_PROFILE_STATE;
                }

                return ISAuthConstants.LOGIN_SUCCEED;
            }
            case NAME_PROFILE_STATE: {

                NameCallback nameCallback = (NameCallback) callbacks[0];

                String name = nameCallback.getName();

                profilePersister.saveDevicePrint(name, devicePrintProfile);

                return ISAuthConstants.LOGIN_SUCCEED;
            }
            // Just for completeness.
            default: {
                throw new AuthLoginException(BUNDLE_NAME, "invalidauthstate", null);
            }
        }
    }
}
