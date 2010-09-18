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
 * $Id: GroupContainerModel.java,v 1.2 2008/06/25 05:42:56 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Set;
import java.util.Map;

/* - NEED NOT LOG - */

/**
 * <code>GroupContainerModel</code> defines a set of methods required by role
 * navigation view bean.
 */
public interface GroupContainerModel
    extends DMModel
{
    /**
     * Gets the group containers based on the filter.
     *
     * @param location where to start the search.
     * @param filter used to narrow search.
     * @return Set of Roles
     */
    public Set getGroupContainers(String location, String filter);
    
    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @return attribute values.
     */
    public Map getDataMap();

    /**
     * Create a new group container entry.
     *
     * @param location Name of parent where container will be created. Name 
     *        must be in DN format.
     * @param data Map of attribute values used to create container.
     * @throws AMConsoleException if container cannot be created.
     */
    public void createGroupContainer(String location, Map data) 
        throws AMConsoleException;
    
    /**
     * Returns people container creation property XML.
     *
     * @return people container creation property XML.
     */
    public String getCreateGroupContainerXML();

    /**
     * Returns true if there are properties to display for this entry.
     *
     * @return true if there are properties to display for this entry.
     */
    public boolean hasDisplayProperties();
}
