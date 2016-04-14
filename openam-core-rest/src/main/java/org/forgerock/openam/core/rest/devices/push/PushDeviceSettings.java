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

package org.forgerock.openam.core.rest.devices.push;

import java.util.Arrays;
import java.util.Objects;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.util.Reject;

/**
 * Data model representation of an Push device's settings.
 *
 * @since 13.5.0
 */
public final class PushDeviceSettings extends DeviceSettings {

    private String sharedSecret;
    private String deviceName;
    private String communicationId;

    /**
     * Empty no-arg constructor for Jackson usage, due to presence of non-default constructor.
     */
    public PushDeviceSettings() {
        //This section intentionally left blank.
    }

    /**
     * Construct a new PushDeviceSettings object with the provided values.
     *
     * @param sharedSecret The device's shared secret. Non-null value.
     * @param deviceName An arbitrary identifier for the device. Non-null value.
     */
    public PushDeviceSettings(String sharedSecret, String deviceName) {
        super();
        setSharedSecret(sharedSecret);
        setDeviceName(deviceName);
    }

    /**
     * Set the communication ID used to reference the user's handset on the wire.
     *
     * @param communicationId The communication id. Can not be null.
     */
    public void setCommunicationId(String communicationId) {
        Reject.ifNull(communicationId, "communicationId can not be null.");
        this.communicationId = communicationId;
    }

    /**
     * Set the secret value which is shared between the Push device and an authenticating module.
     *
     * @param sharedSecret The shared secret. Can not be null.
     */
    public void setSharedSecret(String sharedSecret) {
        Reject.ifNull(sharedSecret, "sharedSecret can not be null.");
        this.sharedSecret = sharedSecret;
    }

    /**
     * Set an arbitrary identifier for the OATH device.
     *
     * @param deviceName The identifier. Can not be null.
     */
    public void setDeviceName(String deviceName) {
        Reject.ifNull(deviceName, "deviceName can not be null.");
        this.deviceName = deviceName;
    }

    /**
     * Get the secret value which is shared between the OATH device and an authenticating module.
     *
     * @return The shared secret.
     */
    public String getSharedSecret() {
        return sharedSecret;
    }

    /**
     * Get the arbitrary identifier for the Push device.
     *
     * @return The identifier.
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Get the communication ID for the Push device on the communication network..
     *
     * @return The communication identifier.
     */
    public String getCommunicationId() {
        return communicationId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PushDeviceSettings that = (PushDeviceSettings) o;

        if (!deviceName.equals(that.deviceName)) {
            return false;
        }

        if (!sharedSecret.equals(that.sharedSecret)) {
            return false;
        }

        if (!uuid.equals(that.getUUID())) {
            return false;
        }

        if (!communicationId.equals(that.getCommunicationId())) {
            return false;
        }

        if (!Arrays.equals(recoveryCodes, that.getRecoveryCodes())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sharedSecret, deviceName, uuid, communicationId, recoveryCodes);
    }

    @Override
    public String toString() {
        return "PushDeviceSettings {" +
                "sharedSecret='" + sharedSecret + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", UUID='"+ uuid + '\'' +
                ", communicationId='"+ communicationId + '\'' +
                ", recoveryCodes='"+ Arrays.toString(recoveryCodes) + '\'' +
                '}';
    }
}
