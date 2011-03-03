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
 * $Id: UMOrganizationModel.java,v 1.2 2008/06/25 05:42:57 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.Set;

/*- NEED NOT LOG - */

/**
 * <code>UMOrganizationModel</code> defines a set of methods that are 
 * required by the organization related views.
 */
public interface UMOrganizationModel
    extends DMModel
{
    /**
     * Gets the Organizations based on the filter
     *
     * @param filter Search filter for fetching Organizations
     * @return Set of Organizations
     */
    public Set getOrganizations(String location, String filter);

    /**
     * Returns property sheet XML for organization creation page.
     *
     * @throws AMConsoleException if XML cannot be determined.
     */
    String getCreateOrganizationPropertyXML()
	throws AMConsoleException;


    /**
     * Returns organization profile property XML.
     *
     * @param realmName Realm/Organization Name.
     * @param viewbeanClassName Class Name of view bean.
     * @return organization profile property XML.
     * @throws AMConsoleException if XML cannot be determined.
     */
    String getOrganizationProfileXML(
	String realmName,
	String viewbeanClassName
    ) throws AMConsoleException;


    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @return attribute values.
     */
    public Map getDataMap();

    public void updateOrganization(String name, Map values)
        throws AMConsoleException;

    public void createOrganization(String location, String name, Map values)
        throws AMConsoleException;

    public Map getValues(String name) 
        throws AMConsoleException;

    /**
     * Returns assignable services of an organization.
     *
     * @param location Location DN of the organization.
     * @return assignable services of an organization.
     */
    public Map getAssignableServiceNames(String location)
        throws AMConsoleException;

    public Map getAssignedServices(String location);

   // public void removeServices(String location, Set services)
    //    throws AMConsoleException; 

    public void registerService(String organization, String service) 
        throws AMConsoleException;
}
