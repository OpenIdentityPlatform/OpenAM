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

package org.forgerock.openam.rest.devices;

import java.util.Objects;
import java.util.UUID;
import org.forgerock.util.Reject;

/**
 * Data model representation of an OATH device's settings.
 *
 * @since 13.0.0
 */
public final class OathDeviceSettings {

    private String sharedSecret;
    private String deviceName;
    private long lastLogin;
    private int counter;
    private boolean checksumDigit = false;
    private int truncationOffset = 0;
    private String[] recoveryCodes = new String[0];
    private String uuid;

    public OathDeviceSettings() {
        //Empty no-arg constructor for Jackson usage, due to presence of non-default constructor.
        //UUID is injected by Jackson.
    }

    /**
     * @param sharedSecret The device's shared secret. Non-null value.
     * @param deviceName An arbitrary identifier for the device. Non-null value.
     * @param lastLogin The last login time, for TOTP algorithm usage. Non-null value.
     * @param counter The counter value, for HOTP algorithm usage. Non-null value.
     */
    public OathDeviceSettings(String sharedSecret, String deviceName, long lastLogin, int counter) {
        setSharedSecret(sharedSecret);
        setDeviceName(deviceName);
        setLastLogin(lastLogin);
        setCounter(counter);

        //when created w/ the constructor, use a random String
        uuid = UUID.randomUUID().toString();
    }

    /**
     * Sets the remaining recovery codes for this device.
     *
     * @param recoveryCodes the remaining recovery codes for this device. Can not be null.
     */
    public void setRecoveryCodes(String[] recoveryCodes) {
        Reject.ifNull(recoveryCodes);
        this.recoveryCodes = recoveryCodes;
    }

    /**
     * Get the array of recovery codes which are usable for this device.
     *
     * @return an array of the remaining recovery codes for this device.
     */
    public String[] getRecoveryCodes() {
        return recoveryCodes;
    }

    /**
     * Set the secret value which is shared between the OATH device and an authenticating module.
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
     * Set the last login time, in milliseconds, when this device was used. This is relevant for authentication using
     * the TOTP algorithm.
     *
     * @param lastLogin The last login time in ms. Can not be null.
     */
    public void setLastLogin(long lastLogin) {
        Reject.ifNull(lastLogin, "lastLogin can not be null.");
        this.lastLogin = lastLogin;
    }

    /**
     * Set the counter value for this device. This is relevant for authentication using the HOTP algorithm.
     *
     * @param counter The counter value. Can not be null.
     */
    public void setCounter(int counter) {
        Reject.ifNull(counter, "counter can not be null.");
        this.counter = counter;
    }

    /**
     * Set the truncation offset for this device.
     *
     * @param truncationOffset The truncation offset.
     */
    public void setTruncationOffset(int truncationOffset) {
        this.truncationOffset = truncationOffset;
    }

    /**
     * Sets the UUID for this device.
     *
     * @param uuid The UUID.
     */
    public void setUUID(String uuid) {
        Reject.ifNull(uuid, "UUID can not be null.");
        this.uuid = uuid;
    }

    /**
     * Set the checksum digit for this device.
     *
     * @param checksumDigit The checksum digit.
     */
    public void setChecksumDigit(boolean checksumDigit) {
        this.checksumDigit = checksumDigit;
    }

    /**
     * Get the secret value which is shared between the OATH device and an authenticating module.
     *
     * @return sharedSecret The shared secret.
     */
    public String getSharedSecret() {
        return sharedSecret;
    }

    /**
     * Get the arbitrary identifier for the OATH device.
     *
     * @return deviceName The identifier.
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Get the last login time, in milliseconds, when this device was used. This is relevant for authentication using
     * the TOTP algorithm.
     *
     * @return lastLogin The last login time in ms.
     */
    public long getLastLogin() {
        return lastLogin;
    }

    /**
     * Get the counter value for this device. This is relevant for authentication using the HOTP algorithm.
     *
     * @return counter The counter value.
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Get the checksum digit for this device.
     *
     * @return checksumDigit The checksum digit.
     */
    public boolean getChecksumDigit() {
        return checksumDigit;
    }

    /**
     * Get the truncation offset for this device.
     *
     * @return truncationOffset The truncation offset.
     */
    public int getTruncationOffset() {
        return truncationOffset;
    }

    /**
     * Gets the UUID from this class which is used as reference and set
     * on creation.
     *
     * @return UUID the UUID.
     */
    public String getUUID() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OathDeviceSettings that = (OathDeviceSettings) o;

        if (checksumDigit != that.checksumDigit) {
            return false;
        }
        if (counter != that.counter) {
            return false;
        }
        if (!deviceName.equals(that.deviceName)) {
            return false;
        }
        if (lastLogin  != that.lastLogin) {
            return false;
        }
        if (!sharedSecret.equals(that.sharedSecret)) {
            return false;
        }
        if (truncationOffset != that.truncationOffset) {
            return false;
        }
        if (!uuid.equals(that.getUUID())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sharedSecret, deviceName, lastLogin, counter,
                checksumDigit, truncationOffset, recoveryCodes, uuid);
    }

    @Override
    public String toString() {
        return "OathDeviceSettings{" +
                "sharedSecret='" + sharedSecret + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", lastLogin='" + lastLogin + '\'' +
                ", counter='" + counter + '\'' +
                ", checksumDigit='" + checksumDigit + '\'' +
                ", truncationOffset='" + truncationOffset + '\'' +
                ", UUID='"+ uuid + '\'' +
                '}';
    }

    /**
     * Generates numCodes of recovery codes, utilising java.util.UUID.randomUUID() to create random
     * String characters.
     *
     * @param numCodes Number of recovery codes to generate. Must be > 0.
     * @return a String array of randomly generated recovery codes, of size numSize.
     */
    public static String[] generateRecoveryCodes(int numCodes) {
        Reject.ifTrue(numCodes < 1, "numCodes must be greater than or equal to 1.");

        String[] recoveryCodes = new String[numCodes];

        for ( int i = 0; i < numCodes; i++) {
            String temp = UUID.randomUUID().toString();
            temp = temp.replace("-", "").substring(0, 10);
            recoveryCodes[i] = temp;
        }

        return recoveryCodes;
    }
}
