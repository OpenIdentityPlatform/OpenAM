/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: MAPCreateDeviceModel.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface MAPCreateDeviceModel extends MAPModel {
    /**
     * Returns the XML for create device property sheet.
     *
     * @param profileName Name of Profile.
     * @param style Name of Style.
     * @throws AMConsoleException if there are no attributes to display.
     * @return XML for create device property sheet.
     */
    String getCreateDevicePropertyXML(String profileName, String style)
        throws AMConsoleException;

    /**
     * Returns a map of attribute name to its default values.
     *
     * @return a map of attribute name to its default values.
     */
    Map getCreateDeviceDefaultValues(); 

    /**
     * Returns a set of attriute names for device creation.
     *
     * @return a set of attriute names for device creation.
     */
    Set getCreateDeviceAttributeNames();

    /**
     * Create new device.
     *
     * @param values Attribute Values for the new device.
     * @throws AMConsoleException if device cannot be created.
     */
    void createDevice(Map values)
        throws AMConsoleException;

    /**
     * Clones a device.
     *
     * @param origClientType Original Client Type.
     * @param clientType New Client Type.
     * @param deviceName Device Name.
     * @throws AMConsoleException if device cannot be clone
     */
    void cloneDevice(
        String origClientType,
        String clientType,
        String deviceName
    ) throws AMConsoleException;
}
