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
 * $Id: PeopleContainerModel.java,v 1.2 2008/06/25 05:42:56 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Set;
import java.util.Map;

/* - NEED NOT LOG - */

/**
 */
public interface PeopleContainerModel
    extends DMModel
{
    /**
     * Gets the people containers based on the filter.
     *
     * @param filter Search filter for fetching people containers.
     * @return Set of people containers.
     */
    public Set getPeopleContainers(String location, String filter);
    
    /**
     * Deletes the specified people containers from the directory.
     *
     * @param peopleContainers set of people container DNs that should be deleted
     * @return true of the delete operation is successful.
     */
    public boolean deletePeopleContainers(Set peopleContainers);

    /**
     * Returns people container creation property XML.
     *
     * @return people container creation property XML.
     */
    public String getCreatePeopleContainerXML();

    public Map getDataMap();

    public void createPeopleContainer(String loc, Map data) 
        throws AMConsoleException;

    public boolean hasDisplayProperties();
}
