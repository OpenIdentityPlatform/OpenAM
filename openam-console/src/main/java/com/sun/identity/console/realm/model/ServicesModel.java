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
 * $Id: ServicesModel.java,v 1.2 2008/06/25 05:43:12 qcheng Exp $
 *
 */

package com.sun.identity.console.realm.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface ServicesModel
    extends AMModel
{
    String TF_NAME = "tfName";

    /**
     * Returns a map of assigned service name to its localized name under a
     * realm.
     *
     * @param realmName Name of Realm.
     * @return a map of assigned service name to its localized name under a
     *         realm.
     * @throws AMConsoleException if service names cannot be obtained.
     */
    Map getAssignedServiceNames(String realmName)
        throws AMConsoleException;

    /**
     * Returns a map of service name to its display name that can be assigned
     * to a realm.
     *
     * @param realmName Name of Realm.
     * @return a map of service name to its display name that can be assigned
     * to a realm.
     * @throws AMConsoleException if service names cannot be obtained.
     */
    Map getAssignableServiceNames(String realmName)
        throws AMConsoleException;

    /**
     * Unassigns services from realm.
     *
     * @param realmName Name of Realm.
     * @param names Names of services that are to be unassigned.
     * @throws AMConsoleException if services cannot be unassigned.
     */
    void unassignServices(String realmName, Set names)
        throws AMConsoleException;
}
