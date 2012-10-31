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
 * $Id: UMUserResourceOfferingModel.java,v 1.2 2008/06/25 05:49:49 qcheng Exp $
 *
 */

package com.sun.identity.console.user.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;

/* - NEED NOT LOG - */

public interface UMUserResourceOfferingModel
    extends AMModel
{
    /**
     * Returns user name.
     *
     * @param userId Universal ID of user.
     * @return user name.
     */
    String getUserName(String userId);

    /**
     * Returns resource offering entry stored for a given user distinguished
     * name.
     *
     * @param userId Universal ID of user.
     * @return resource offering entry stored for a given user.
     * @throws AMConsoleException if entry cannot be determined.
     */
    SMDiscoveryServiceData getUserDiscoEntry(String userId)
	throws AMConsoleException;

    /**
     * Set resource offering entry.
     *
     * @param userId Universal ID of user.
     * @param smData Resource offering entry.
     * @throws AMConsoleException if entry cannot be set.
     */
    void setUserDiscoEntry(String userId, SMDiscoveryServiceData smData)
        throws AMConsoleException;
}
