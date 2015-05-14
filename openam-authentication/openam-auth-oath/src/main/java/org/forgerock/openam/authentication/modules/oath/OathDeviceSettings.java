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

package org.forgerock.openam.authentication.modules.oath;

import org.forgerock.util.Reject;

/**
 * Data model representation of an OATH device's settings.
 *
 * @since 13.0.0
 */
public final class OathDeviceSettings {

    private String sharedSecret;
    private String deviceName;
    private String lastLogin;
    private String counter;
    private String checksumDigit;
    private String truncationOffset;

    /**
     * @param sharedSecret The device's shared secret. Non-null value.
     * @param deviceName An arbitrary identifier for the device. Non-null value.
     * @param lastLogin The last login time, for TOTP algorithm usage. Non-null value.
     * @param counter The counter value, for HOTP algorithm usage. Non-null value.
     */
    public OathDeviceSettings(String sharedSecret, String deviceName, String lastLogin, String counter) {
        setSharedSecret(sharedSecret);
        setDeviceName(deviceName);
        setLastLogin(lastLogin);
        setCounter(counter);
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
     * @param lastLogin The last login time. Can not be null.
     */
    public void setLastLogin(String lastLogin) {
        Reject.ifNull(lastLogin, "lastLogin can not be null.");
        this.lastLogin = lastLogin;
    }

    /**
     * Set the counter value for this device. This is relevant for authentication using the HOTP algorithm.
     *
     * @param counter The counter value. Can not be null.
     */
    public void setCounter(String counter) {
        Reject.ifNull(counter, "counter can not be null.");
        this.counter = counter;
    }

    /**
     * Set the truncation offset for this device.
     *
     * @param truncationOffset The truncation offset.
     */
    public void setTruncationOffset(String truncationOffset) {
        this.truncationOffset = truncationOffset;
    }

    /**
     * Set the checksum digit for this device.
     *
     * @param checksumDigit The checksum digit.
     */
    public void setChecksumDigit(String checksumDigit) {
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
     * @return lastLogin The last login time.
     */
    public String getLastLogin() {
        return lastLogin;
    }

    /**
     * Get the counter value for this device. This is relevant for authentication using the HOTP algorithm.
     *
     * @return counter The counter value.
     */
    public String getCounter() {
        return counter;
    }

    /**
     * Get the checksum digit for this device.
     *
     * @return checksumDigit The checksum digit.
     */
    public String getChecksumDigit() {
        return checksumDigit;
    }

    /**
     * Get the truncation offset for this device.
     *
     * @return truncationOffset The truncation offset.
     */
    public String getTruncationOffset() {
        return truncationOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OathDeviceSettings that = (OathDeviceSettings) o;

        if (checksumDigit != null ? !checksumDigit.equals(that.checksumDigit) : that.checksumDigit != null)
            return false;
        if (!counter.equals(that.counter)) return false;
        if (!deviceName.equals(that.deviceName)) return false;
        if (!lastLogin.equals(that.lastLogin)) return false;
        if (!sharedSecret.equals(that.sharedSecret)) return false;
        if (truncationOffset != null ? !truncationOffset.equals(that.truncationOffset) : that.truncationOffset != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sharedSecret.hashCode();
        result = 31 * result + deviceName.hashCode();
        result = 31 * result + lastLogin.hashCode();
        result = 31 * result + counter.hashCode();
        result = 31 * result + (checksumDigit != null ? checksumDigit.hashCode() : 0);
        result = 31 * result + (truncationOffset != null ? truncationOffset.hashCode() : 0);
        return result;
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
                '}';
    }
}
