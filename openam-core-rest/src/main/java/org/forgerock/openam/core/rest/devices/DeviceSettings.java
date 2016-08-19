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
package org.forgerock.openam.core.rest.devices;

import java.util.UUID;
import org.forgerock.util.Reject;

/**
 * Abstract concept of DeviceSettings, used to ensure that any device(s) stored have a unique identifier
 * so that they can be individually referenced.
 *
 * Despite this, most interactions will be with the entire field rather than specific instances of devices as
 * multiple individual devices per user per realm is currently not directly supported.
 */
public abstract class DeviceSettings {

    protected String uuid;
    protected String[] recoveryCodes = new String[0];

    /**
     * Configures the internal UUID.
     */
    public DeviceSettings() {
        uuid = UUID.randomUUID().toString();
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
     * Gets the UUID from this device which is used as reference and set
     * on creation.
     *
     * @return UUID the UUID.
     */
    public String getUUID() {
        return uuid;
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

}
