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
 * $Id: AuthConfigurationModel.java,v 1.2 2008/06/25 05:42:45 qcheng Exp $
 *
 */

package com.sun.identity.console.authentication.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.Map;
import java.util.Set;
import java.util.List;

/* - NEED NOT LOG - */

public interface AuthConfigurationModel extends AMModel
{
    /** 
     * Creates a new named authentication configuration object. This object
     * will be used by the various objects for authentication.
     *
     * @param name used to reference the configuration.
     * @throws AMConsoleException if the configuration cannot be created.
     */
    public void createAuthConfiguration(String name) 
        throws AMConsoleException;

    /**
     * Deletes the named authentication configuration object.
     *
     * @param realm name where configuration is locate.
     * @param names names of entries.
     */
    public void deleteAuthConfiguration(String realm, Set names)
        throws AMConsoleException;

    /**
     * Gets the module name for the given module index in the current
     * authentication configuration attributes.
     *
     * @param moduleIndex the index of the module to retrieve name from.
     * @return the module name
     */
    public String getModuleName(int moduleIndex);

    /**
     * Gets the module flag for the given module index in the current
     * authentication configuration attributes
     *
     * @param moduleIndex the index of the module to retrieve Flag from.
     * @return the module flag
     */
    public String getModuleFlag(int moduleIndex);

    /**
     * Gets the module options string for the given module index in the
     * current authentication configuration attributes
     *
     * @param moduleIndex the index of the module to retrieve Flag from.
     * @return the module options string
     */
    public String getModuleOptions (int moduleIndex);

    /**
     * Returns the value for the post processing class set for the current
     * configuration object. If the value is not present the emptry string
     * will be returned.
     *
     * @param realm name where configuration is locate.
     * @param configuration name of entry.
     * @return value of postprocessing class
     */
    public String getPostProcessingClass(String realm, String configuration);

    /**
     * Returns the number of entries in the current configuration.
     *
     * @return number of entries
     */
    public int getNumberEntries();

    /**
     * Returns the xml blob that represents the auth entries which 
     * make up the specified auth configuration.
     *
     * @param realm name where configuration exists.
     * @param config name of the configuration object.
     * @return String xml blob.
     */
    public String getXMLValue(String realm, String config);

    /**
     * Sets the xml blob that represents the auth entries which  
     * make up the specified auth configuration.
     *
     * @param value of the xml blob.
     */
    public void setXMLValue(String value);

    /**
     * Remove the entries selected in the UI. This is done by creating a new
     * List of the selected entries, then removing that List from the
     * original list of all the entries.
     *
     * @param entries array of index id's
     */
    public void removeAuthEntries(Integer[] entries);

    /**
     * Add a new entry to the configuration. Copy the last element in the list,
     * and add it to the configuration. This is not permanent, the value still
     * needs to be stored/saved.
     *
     * @param newEntry 
    public String addAuthEntry(AuthConfigurationEntry ace) 
        throws AMConsoleException;
     */

    /**
     * Initialize configuration entry information for the given realm
     * and named configuration.
     *
     * @param realm name where configuration is locate.
     * @param config name of entry.
     */
    public void initialize(String realm, String config);

    /**
     * Stores locally the list of authentication entries for the auth config 
     * object. 
     *
     * @param entries list of <code>AuthConfigurationEntry</code> objects.
     */
    public void setEntries(List entries);

    /** 
     * Sets the value for the post authentication properties class.
     *
     * @param className name of the class used for post authentication 
     *        processing
     */
    public void setPostAuthPropertiesClass(String className);

    /**     
     * Save the value for the given realm and configuration.
     *  
     * @param realm name where configuration is locate.
     * @param config name of entry.
     */ 
    public void store(String realm, String config) throws AMConsoleException;

    /**
     * Used to restore the config data settings to their original values.
     *
     * @param realm name where configuration is locate.
     * @param config name of entry.
     */
    public void reset(String realm, String config);

    /**
     * Returns a <code>Map</code> object containing the 
     * <code>AuthenticationEntry</code> criteria flags. The key is the value
     * and the display string is the value.
     *
     * @return map of criteria values.
     */
    public Map getCriteriaMap();

    /**
     * Returns a map of the attributes that make up an auth config entry.
     * The map will contain the success and failure url lists, the post
     * processing class, and the list of auth modules that make up the 
     * auth configuration entry.
     *
     * @return Map of attribute values.
     * @throws AMConsoleException if there is an error retrieving the data
     *    from the entry.
     */
    public Map getValues() throws AMConsoleException;

    /**
     * Sets the specified attributes in the auth config entry.  The data 
     * will not be committed to the data store until <code>store()</code>
     * is called.
     * 
     * @param data that will be set in the config entry.
     */
    public void setValues(Map data);
}
