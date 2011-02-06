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
 * $Id: DMModel.java,v 1.2 2008/06/25 05:42:56 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

/**
 * <code>DMModel</code> defines a set of methods that are required by
 * navigation view bean in user management
 */
public interface DMModel
    extends AMModel, DMConstants
{
    Map deleteObject(Set dnSet)
	throws AMConsoleException;

    /**
     * Gets error message that were generated in the model.
     *
     * @return error message List
     */    
    String getErrorMessage();

    /**
     * Set the error message to the model. This will be displayed by the
     * viewbean.
     *
     * @param msgStr to be displayed.
     */     
    void setErrorMessage(String msgStr);

    /**
     * Gets the attributes list stored in the model
     *
     * @return the attributes  list stored in the model
     */
    Set getAttrList();

    /**
     * Set attribute list in model.
     *
     * @param set data to be stored.
     */
    void setAttrList(Set set);

    /**
     * Returns value of an attribute from the search results for a given
     * organization.
     *
     * @param roleDN of the organization.
     * @param attrName attribute name.
     * @return value of an attribute from the search results for a given people
     *         container.
     */
    String getAttributeValue(String roleDN, String attrName);

    /**
     * Gets the user map data stored in the model.
     *
     * @return the user map data stored in the model.
     */
    Map getAttrMap();

    /**
     * Set attribute map for users.
     *
     * @param map data for users.
     */
    void setAttrMap(Map map);

    /**
     * Gets the search attribute list stored in the model.
     *
     * @return the search attribute list stored in the model.
     */
    List getAttrSearchList();

    /**
     * Set attribute search list.
     *
     * @param list attribute search list.
     */
    void setAttrSearchList(List list);

    /**
     * Gets the type of object for a given DN.
     *
     * @param name DN of an object.
     * @return type of object.
     */
    int getObjectType(String name);

    /**
     * Get the list of tabs to display.
     *
     * @return a list of tabs to display.
     */
    List getTabMenu();

    /**
     * Removes the specified set of services from the location.
     *
     * @param location of entry.
     * @param services to remove.
     */
    void removeServices(String location, Set services) 
       throws AMConsoleException;

    /**
     * Returns Universal Id of a given Distinguished Name.
     *
     * @param dn Distinguished Name.
     */
    String getUniversalId(String dn);

    /**
     * Returns true if the current user can create group container at
     * the specified location.
     *
     * @param location where the group container will be created.
     * @return true if the group container can be created.
     */
    public boolean createGroupContainer(String location);

    /**
     * Returns true if the current user can create a group at
     * the specified location.
     *
     * @param location where the group will be created.
     * @return true if the group can be created.
     */
    public boolean createGroup(String location);

    /**
     * Returns true if the current user can create a people container at
     * the specified location.
     *
     * @param location where the people container will be created.
     * @return true if the people container can be created.
     */
    public boolean createPeopleContainer(String location);

    /**
     * Returns true if the current user can create a user at
     * the specified location.
     *
     * @param location where user will be created.
     * @return true if the user can be created.
     */
    public boolean createUser(String location);

    /**
     * Returns true if the current user can create a role at
     * the specified location.
     *
     * @param location where the role will be created.
     * @return true if the role can be created.
     */
    public boolean createRole(String location);
    
    /**
     * Returns true if the current user can create an organization at
     * the specified location.
     *
     * @param location where organization will be created.
     * @return true if the organization can be created.
     */
    public boolean createOrganization(String location);

    /**
     * Returns true if the current user can create an organization unit at
     * the specified location.
     *
     * @param location where the organization unit will be created.
     * @return true if the organization unit can be created.
     */
    public boolean createOrganizationUnit(String location);

    /**
     * Returns <code>true</code> if organizations should be displayed.
     *
     * @return <code>true</code> to show group containers.
     */
    boolean showOrganizations();

    /**
     * Returns <code>true</code> if group containers are displayed.
     *
     * @return <code>true</code> to show group containers.
     */
    boolean showGroupContainers();

    /**
     * Returns <code>true</code> if people containers are displayed.
     *
     * @return <code>true</code> to show people containers.
     */
    boolean showPeopleContainers();

    /**
     * Returns <code>true</code> if organizational units are displayed.
     *
     * @return <code>true</code> to show organizational units.
     */
    boolean showOrgUnits();

    /**
     * Returns a list of parentage path strings from <code>dn</code> to user's
     * start DN.
     * For example <code>o=org1,o=org2,dc=sun,dc=com</code>, this method returns
     * list with entries = <code>[sun,org2,org1]</code>.
     *
     * @param dn location DN
     * @return parentage path from a location to root
     */
    List pathToDisplayString(String dn);

    /**
     * Returns a list of parentage path strings from <code>dn</code> to
     * <code>startDN</code>.
     *
     * @param dn location DN.
     * @param startDN start DN of parentage path.
     * @param isRole true if <code>dn</code> is a Role DN.
     * @return a list of parentage path strings.
     */
    List pathToDisplayString(String dn, String startDN, boolean isRole);

    /**
     * Converts a dn to a readable format which can be displayed by a client.
     * Each RDN will be separated by the '>' character.
     *
     * @param dn Distinguished Name to be converted.
     * @return the formated string.
     */
    String getDisplayPath(String dn);

    /**
     * Returns relative distinguished name.
     *
     * @param dn Distinguished name.
     * @param isRoleDN <code>true</code> if is role DN.
     * @return name of relative distinguished name.
     */
    String DNToName(String dn, boolean isRoleDN);
    
    /**
     * When a search is performed a set of attributes from each entry
     * is also returned. This set of attributes is mapped to the dn of the
     * entry returned. This call can be used to retrieve the first attribute
     * from the set of attributes returned. Used for display purpose in the
     * user, role, and group member listing views. Note, since the group 
     * member pages may contain other groups, we need to flag if the entry
     * is a user or not.
     *
     * @param dn the entry being displayed.
     * @return value of the attribute to display.
     */
    public String getUserDisplayValue(String dn);

    /**
     * Returns the value to display for the "Name" column in tables which 
     * display user entries. The value returned will be the first entry 
     * found in the <code>Search Return Attribute</code> from the Administration
     * service. This will help users understand what value is being displayed
     * in this column.
     *
     */
    public String getNameColumnLabel();
}
