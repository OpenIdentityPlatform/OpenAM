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
 * $Id: SubConfigModel.java,v 1.3 2008/06/25 05:43:19 qcheng Exp $
 *
 */

package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface SubConfigModel
    extends AMModel
{
    /** 
     * Returns property sheet XML for adding new sub configuration.
     *
     * @param name Name of Schema.
     * @return property sheet XML for adding new sub configuration.
     */
    String getAddConfigPropertyXML(String name)
        throws AMConsoleException;

    /** 
     * Returns property sheet XML for editing sub configuration.
     *
     * @param viewbeanClassName Class name of view bean.
     * @return property sheet XML for editing sub configuration.
     */
    String getEditConfigPropertyXML(String viewbeanClassName)
        throws AMConsoleException;

    /**
     * Returns a map of sub schema name to its localized name. We should
     * be able to create sub configuration with these names.
     *
     * @return Map of sub schema name to its localized name.
     */
    Map getCreateableSubSchemaNames();

    /**
     * Returns a set of attribute names for a sub schema.
     *
     * @param schemaName Name of Schema.
     * @return Set of attribute names for a sub schema.
     */
    Set getAttributeNames(String schemaName);

    /** 
     * Creates a new sub configuration.
     *
     * @param name Name of sub configuration.
     * @param schemaName Name of schema name.
     * @param values Map of attribute name to its values.
     * @throws AMConsoleException if sub configuration cannot be created.
     */
    void createSubConfig(String name, String schemaName, Map values)
        throws AMConsoleException;

    /**
     * Returns attribute values.
     *
     * @return attribute values.
     * @throws AMConsoleException if attribute values cannot be determined.
     */
    Map getSubConfigAttributeValues()
        throws AMConsoleException;

    /**
     * Set attribute values.
     *
     * @param values Attribute values.
     * @throws AMConsoleException if attribute values cannot be set.
     */
    void setSubConfigAttributeValues(Map values)
        throws AMConsoleException;

    /**
     * Returns default values of a schema.
     *
     * @param name Name of Schema.
     * @return default values of a schema.
     * @throws AMConsoleException if default values cannot be determined.
     */
    Map getServiceSchemaDefaultValues(String name)
        throws AMConsoleException;

    /**
     * Returns true if this service has global sub schema.
     *
     * @return true if this service has global sub schema.
     */
    boolean hasGlobalSubSchema();
                                                                                
    /**
     * Returns list of sub configuration objects.
     *
     * @return list of sub configuration objects.
     * @see com.sun.identity.console.base.model.SMSubConfig
     */
    List getSubConfigurations();

    /**
     * Deletes sub configurations.
     *
     * @param names Names of sub configuration which are to be deleted.
     * @throws AMConsoleException if sub configuration cannot be deleted.
     */
    void deleteSubConfigurations(Set names)
        throws AMConsoleException;

    /**
     * Returns plugin name for returning possible sub configuration names.
     * 
     * @param subSchemaName Name of sub schema.
     * @return plugin name for returning possible sub configuration names.
     */
    String getSelectableSubConfigNamesPlugin(String subSchemaName);
        
    /**
     * Returns a set of possible names of sub configuration.
     * 
     * @param subSchemaName Name of sub schema
     * @return a set of possible names of sub configuration.
     */
    Set getSelectableConfigNames(String subSchemaName);
}
