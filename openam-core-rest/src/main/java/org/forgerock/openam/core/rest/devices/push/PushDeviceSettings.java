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
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * Data model representation of an Push device's settings.
 *
 * @since 13.5.0
 */
public final class PushDeviceSettings extends DeviceSettings {

    private String sharedSecret; // 256 bits randomly generated on push device creation
    private String deviceName; // name of the device granted by openam, currently unused
    private String communicationType; // the delivery mechanism type for the device - "gcm" or "apns"
    private String deviceType; // type of device - "ios" or "android"
    private String communicationId; // the device id for this comms. (may be higher level than gcm / apns e.g. asns)
    private String deviceMechanismUID; // identifier for the mechanism on the remote device
    private String deviceId; // the device id as known to the final delivery mechanism (gcm / apns)
    private String issuer; //the name of the issuer as originally advertised to this client

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
     * @param deviceName   An arbitrary identifier for the device. Non-null value.
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
        Reject.ifTrue(StringUtils.isBlank(communicationId), "communicationId can not be null.");
        this.communicationId = communicationId;
    }


    /**
     * Set the device ID used to reference the user's handset.
     *
     * @param deviceId The device id. Can not be null.
     */
    public void setDeviceId(String deviceId) {
        Reject.ifTrue(StringUtils.isBlank(deviceId), "deviceId can not be null.");
        this.deviceId = deviceId;
    }

    /**
     * Set the secret value which is shared between the Push device and an authenticating module.
     *
     * @param sharedSecret The shared secret. Can not be null.
     */
    public void setSharedSecret(String sharedSecret) {
        Reject.ifTrue(StringUtils.isBlank(sharedSecret), "sharedSecret can not be null.");
        this.sharedSecret = sharedSecret;
    }

    /**
     * Set an arbitrary identifier for the device.
     *
     * @param deviceName The identifier. Can not be null.
     */
    public void setDeviceName(String deviceName) {
        Reject.ifTrue(StringUtils.isBlank(deviceName), "deviceName can not be null.");
        this.deviceName = deviceName;
    }

    /**
     * Set the Device Mechanism ID.
     * @param deviceMechanismUID The identifier (unique on the phone) to the specific mechanism.
     */
    public void setDeviceMechanismUID(String deviceMechanismUID) {
        Reject.ifTrue(StringUtils.isBlank(deviceMechanismUID), "deviceMechanismUID can not be null.");
        this.deviceMechanismUID = deviceMechanismUID;
    }

    /**
     * The communication medium used to commune with this device
     * @param communicationType The communication medium.
     */
    public void setCommunicationType(String communicationType) {
        Reject.ifTrue(StringUtils.isBlank(communicationType), "communicationType can not be null");
        this.communicationType = communicationType;
    }

    /**
     * The type of device we are communicating with.
     * @param deviceType The device type (likely ios or android).
     */
    public void setDeviceType(String deviceType) {
        Reject.ifTrue(StringUtils.isBlank(deviceType), "deviceType can not be null");
        this.deviceType = deviceType;
    }

    /**
     * The name of the issuer when this device profile was issued.
     * @param issuer The name of the issuer.
     */
    public void setIssuer(String issuer) {
        Reject.ifTrue(StringUtils.isBlank(issuer), "issuer can not be null");
        this.issuer = issuer;
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
     * Get the communication ID for the Push device on the communication network.
     *
     * @return The communication identifier.
     */
    public String getCommunicationId() {
        return communicationId;
    }

    /**
     * Get the Device Mechanism ID for the push device.
     *
     * @return the Device Mechanism ID.
     */
    public String getDeviceMechanismUID() {
        return deviceMechanismUID;
    }

    /**
     * Get the type of the Push device.
     *
     * @return The identifier.
     */
    public String getDeviceType() {
        return deviceType;
    }

    /**
     * Get the arbitrary identifier for the Push device.
     *
     * @return The identifier.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Get the issuer of the Push device.
     *
     * @return The issuer.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Get the communication ID for the Push device on the communication network..
     *
     * @return The communication identifier.
     */
    public String getCommunicationType() {
        return communicationType;
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

        return Objects.equals(deviceName, that.deviceName)
                && Objects.equals(sharedSecret, that.sharedSecret)
                && Objects.equals(uuid, that.uuid)
                && Objects.equals(deviceType, that.deviceType)
                && Objects.equals(communicationId, that.communicationId)
                && Objects.equals(communicationType, that.communicationType)
                && Objects.equals(deviceMechanismUID, that.deviceMechanismUID)
                && Objects.equals(deviceId, that.deviceId)
                && Objects.equals(issuer, that.issuer)
                && Arrays.equals(recoveryCodes, recoveryCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sharedSecret, deviceName, uuid, communicationId, communicationType, deviceType,
                recoveryCodes, deviceMechanismUID, deviceId, issuer);
    }

    @Override
    public String toString() {
        return "PushDeviceSettings {" +
                "sharedSecret='" + sharedSecret + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", UUID='"+ uuid + '\'' +
                ", communicationId='"+ communicationId + '\'' +
                ", deviceMechanismUID='"+ deviceMechanismUID + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", communicationType='"+ communicationType + '\'' +
                ", deviceId='"+ deviceId + '\'' +
                ", issuer='"+ issuer + '\'' +
                ", recoveryCodes='"+ Arrays.toString(recoveryCodes) + '\'' +
                '}';
    }


}
