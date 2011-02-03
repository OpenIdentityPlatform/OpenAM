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
 * $Id: RoleModel.java,v 1.2 2008/06/25 05:42:56 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import java.util.List;
import java.util.Set;
import java.util.Map;
import com.sun.identity.console.base.model.AMConsoleException;

/* - NEED NOT LOG - */

/**
 * <code>RoleModel</code> defines a set of methods required by role
 * navigation view bean.
 */
public interface RoleModel
    extends DMModel
{ 
    String ATTRIBUTE_NAME_PERMISSION = "permission";
 
    public static final String ROLE_TYPE = "role-page-type";
    /**
     * Gets the roles based on the filter.
     *
     * @param filter Search filter for fetching Roles
     * @return Set of Roles
     */
    public Set getRoles(String location, String filter);
    
    /**
     * Returns value of the return attribute in the administration service.
     *
     * @return the value of the return attribute in the administration
     *         service.
     */
    public List getSearchReturnAttributes();

    /**
     * Returns localized name of ttribute name.
     *
     * @param name of attribute.
     * @return localized name of attribute name.
     */
    public String getAttributeLocalizedName(String name);

    /**
     * Returns a map of the attribute names. The values for all are empty.
     * This is used in the construction of the page and when retrieving
     * values from the form submission.
     */
    public Map getDataMap(int type);

    /**
     * Creates a role below the current organization or organization unit.
     *
     * @param location where to create the given role
     * @param values The map which contains the role name and the optional and
     *        required attributes.
     */
    public void createRole(String location, Map values)
        throws AMConsoleException;

    /**
     * Gets a map of default role types
     *
     * @return a map of default role types
     */
    public Map getDefaultTypes();

    /**
     * Gets the set of default permissions used for creating roles
     *
     * @return set of permission values
     */
    public Set getDefaultPermissions();

    /**
     * Gets the value for the empty permission
     *
     * @return empty permission value
     */
    public String getEmptyPermission();

    /**
     * Gets the localized string for the default permissions list
     *
     * @param option value of the permission
     * @return localized string for permission
     */
    public String getOptionString(String option);

    public String getRoleCreateXML(int type);

    public String getRoleProfileXML(int type);

    public String getServiceXML(String service);
    
    /**
     * Returns a list of members of the role.
     *   
     * @param location of role
     * @param filter pattern string to match users
     *
     * @return a list of members of the group.
     */  
    public Set getMembers(String location, String filter)
        throws AMConsoleException;
    
    /**
     * Removes users from the group.
     *   
     * @param setDNs set of distinguished names of users to be removed.
     * @throws AMConsoleException if users cannot be removed.
     */  
    public void removeUsers(String location, Set setDNs)
        throws AMConsoleException;

    public Map getValues(String name) throws AMConsoleException;
    
    /**
     * Use to modify the editable properties for a role. The current
     * properties which can be set are the role description and the 
     * role permission description.
     *
     * @param name of the role to update.
     * @param values to set for the description fields
     */
    public void updateRole(String name, Map values) 
	throws AMConsoleException; 
    
    /**
     * Gets the services which are assigned to this organization.
     *   
     * @param location name or current organization location
     * @return a set of organizations
     */  
    public Map getAssignedServices(String location); 

    /**
     * Returns true if the specified role is a filtered role.
     *
     * @param roleName the name of the role to test.
     * @return true if the role is a filtered role.
     */
    public boolean isFilteredRole(String roleName);
}
