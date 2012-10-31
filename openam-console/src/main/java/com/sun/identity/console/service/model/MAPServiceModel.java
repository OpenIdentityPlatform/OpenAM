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
 * $Id: MAPServiceModel.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Set;

/* - NEED NOT LOG - */

public interface MAPServiceModel extends MAPModel {
    /**
     * Returns a set of profile names.
     *
     * @return a set of profile names.
     */
    Set getProfileNames();

    /**
     * Returns a set of device names.
     *
     * @param profileName Name of profile.
     * @param styleName Name of style.
     * @param wildcard Filter string to filter required names.
     * @return a set of device names.
     */
    Set getDeviceNames(String profileName, String styleName, String wildcard);

    /**
     * Removes a client from the external database.
     *
     * @param clientType Client type to be removed.
     * @throws AMConsoleException if client type cannot be deleted.
     */
    public void removeClient(String clientType)
        throws AMConsoleException;
}
