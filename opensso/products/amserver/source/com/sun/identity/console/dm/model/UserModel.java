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
 * $Id: UserModel.java,v 1.2 2008/06/25 05:42:57 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/*- NEED NOT LOG - */

/**
 * <code>UserModel</code> defines a set of methods that are 
 * required by the organization related views.
 */
public interface UserModel
    extends DMModel
{
    public static final String PEOPLE_CONTAINER = "peopleContainer";

    /**
     * Gets the users based on the filter
     *
     * @param filter Search filter for fetching users
     * @return Set of users
     */
    public Set getUsers(String location, String filter);

    public String getCreateUserPropertyXML(String location);

    /**
     * Returns user profile XML.
     *
     * @param userDN DN of users.
     * @param viewbeanClassName Class Name of View Bean.
     * @return user profile XML.
     */
    public String getUserProfileXML(String userDN, String viewbeanClassName);

    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @return attribute values.
     */
    public Map getDataMap();

    public void updateUser(String name, Map values)
        throws AMConsoleException;

    public void createUser(String location, Map values)
        throws AMConsoleException;

    public Map getValues(String name) 
        throws AMConsoleException;

    public Map getAssignableServiceNames(String location)
        throws AMConsoleException;

    public Map getAssignedServices(String location);

    public void assignService(String user, String serviceName) 
	throws AMConsoleException;

    public void removeServices(String location, Set services)
        throws AMConsoleException; 

    public Set getAssignedRoles(String name);

    /**
     * Returns set of roles that are available for assignment to a user.
     *
     * @param name Name of user.
     * @param assigned Collection of assigned role.
     */
    public Set getAvailableRoles(String name, Collection assigned);

    public void updateRoles(String name, Set roles)
        throws AMConsoleException;

    public String getRoleType(String role);

    public void updateGroups(String name, Set groups)
        throws AMConsoleException;

    /**
     * Returns set of groups that are available for assignment to a user.
     *
     * @param userName Name of user.
     * @param assigned Collection of assigned groups.
     */
    public Set getAvailableGroups(String userName, Collection assigned);

    public Set getAssignedGroups(String userName);

    /**
     * Returns true if administrator has permission to modify user information.
     *
     * @param userDN DN of User.
     * @param viewbeanClassName Class Name of View Bean.
     * @return true if administrator has permission to modify user information.
     */
    boolean canModify(String userDN, String viewbeanClassName);

    public Set getPeopleContainers(String location);
}
