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
 * $Id: GroupModel.java,v 1.2 2008/06/25 05:42:56 qcheng Exp $
 *
 */

package com.sun.identity.console.dm.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

/**
 * <code>GroupModel</code> defines a set of methods that are required
 * to perform group related operations.
 */
public interface GroupModel
    extends DMModel
{
    public static final String GROUP_TYPE = "group";

    /**
     * Gets the Managed Groups based on the filter
     *
     * @param location Where to start the search.
     * @param filter Search filter for fetching Managed Groups.
     * @return Set of Managed Groups
     */
    public Set getGroups(String location, String filter);
    
    /**
     * Set managed groups in model.
     * This method is called when the Search for the groups is completed in the
     * <code>dataframe</code>. The search viewbean in the <code>dataframe</code>
     * sets the results into the group navigation viewbeans model in order to
     * display the results in the Navigation frame.
     *
     * @param groupDNs Set of group DNs
     */
    public void setManagedGroups(Set groupDNs);
    
    /**
     * Returns value of the return attribute in the administration service.
     *
     * @return the value of the return attribute in the administration
     *         service.
     */
    public List getSearchReturnAttributes();

    /**
     *
     * @param groupDN  DN of the group.
     * @return value of the of the last attribute from the search results.
     */
    public String getAttributeValue(String groupDN, String attr);

    /**
     * Returns localized name of attribute name.
     *
     * @param name of attribute.
     * @return localized name of attribute name.
     */
    public String getAttributeLocalizedName(String name);

    /**
     * Returns Map of attribute name to empty set of values.
     *
     * @return attribute values.
     */
    public Map getDataMap(String type);

    /**
     * Update attribute values. 
     *
     * @param name Name of the group.
     * @param values Map of attribute values to set.
     * @throws AMConsoleException if values cannot be set.
     */
    public void updateGroup(String name, Map values) 
        throws AMConsoleException;

    /**
     * Returns attribute values. Map of attribute name to set of values.
     *
     * @param name Name of group.
     * @throws AMConsoleException if values cannot be retrieved.
     * @return attribute values.
     */
    public Map getValues(String name)
	throws AMConsoleException;

    /**
     * Returns group profile page XML.
     *
     * @param viewbeanClassName Class responsible for generating the page
     * @param type Type of group object being displayed.
     * @return group profile XML string.
     */
    public String getGroupProfileXML(String viewbeanClassName, int type);

    /**
     * Returns XML string used for creating a static group.
     *
     * @return XML to create a static group.
     */
    public String getCreateStaticGroupXML();

    /**
     * Returns XML string used for creating a dynamic group.
     *
     * @return XML to create a dynamic group.
     */
    public String getCreateDynamicGroupXML();

    /**
     * Creates a group in the container below the current organization or
     * organization unit. A reload of the navigation frame will be performed
     * if the group is successfully created.
     *
     * @param location where to create the given group
     * @param values map which contains the group name and the optional and
     *               required attributes.
     * @return true if the group was created, false otherwise
     */
    public boolean createGroup(String location, Map values)
	throws AMConsoleException;

    /**
     * Returns the type of group to use as specified in the Adminstration 
     * Service.
     * @return type of managed group being used.
     */
    public String getManagedGroupType();

    /**
     * Returns a set of entries which are the members for a group. The 
     * members can be a mixture of groups and user entries.
     *
     * @param filter Filter used to limit the number of users returned.
     * @param group name of group to get the members.
     * @throws AMConsoleException
     */
    public Set getMembers(String filter, String group)
        throws AMConsoleException;
    
    /**
     * Removes member entries from the group. Entries can be either groups
     * or users.
     *
     * @param location Name of the group.
     * @param memberNames Set of names to remove from the group.
     * @throws AMConsoleException if members cannot be removed.
     */
    public void removeMembers(String location, Set memberNames) 
        throws AMConsoleException;
    
    /**
     * Returns true if the entry is a dynamic group.
     *
     * @param dn of object to test.
     * @return true is dn is a dynamic group.
     */
    public boolean isDynamicGroup(String dn);

    /**
     * Returns true if the entry is a user.
     *
     * @param dn of object to test.
     * @return true is dn is a dynamic group.
     */
    public boolean isUser(String dn);

}
