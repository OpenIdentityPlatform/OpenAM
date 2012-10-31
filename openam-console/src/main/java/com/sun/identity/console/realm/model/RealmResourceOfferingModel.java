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
 * $Id: RealmResourceOfferingModel.java,v 1.2 2008/06/25 05:49:43 qcheng Exp $
 *
 */

package com.sun.identity.console.realm.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;

/* - NEED NOT LOG - */

public interface RealmResourceOfferingModel
    extends AMModel
{
    /**
     * Returns resource offering entry stored for a given realm
     * name.
     *
     * @param realm Name of realm
     * @return resource offering entry stored for a given realm.
     * @throws AMConsoleException if entry cannot be determined.
     */
    SMDiscoveryServiceData getRealmDiscoEntry(String realm)
	throws AMConsoleException;

    /**
     * Assigns service to a realm.
     *
     * @param realm Realm Name.
     * @throws AMConsoleException if values cannot be set.
     */
    void assignService(String realm)
	throws AMConsoleException;

    /**
     * Set resource offering entry.
     *
     * @param realm Name of realm
     * @param smData Resource offering entry.
     * @throws AMConsoleException if entry cannot be set.
     */
    void setRealmDiscoEntry(String realm, SMDiscoveryServiceData smData)
        throws AMConsoleException;
}
