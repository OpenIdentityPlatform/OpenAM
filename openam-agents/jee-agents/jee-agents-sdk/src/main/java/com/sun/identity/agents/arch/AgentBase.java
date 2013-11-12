/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AgentBase.java,v 1.3 2008/07/24 23:05:28 huacui Exp $
 *
 */

/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.agents.arch;

import java.util.Map;
import java.util.Set;

/**
 * The base class for all agent service classes. This class provides access
 * to the underlying <code>Module</code> and <code>Manager</code> related
 * functionality.
 */
public class AgentBase extends ModuleAccess implements ISystemAccess {
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getSystemConfiguration(String, String)
    */
    public String getSystemConfiguration(String id, String defaultValue) {
        return getManager().getSystemConfiguration(id, defaultValue);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getSystemConfiguration(String)
    */
    public String getSystemConfiguration(String id) {
        return getManager().getSystemConfiguration(id);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationMap(String)
    */
    public Map getConfigurationMap(String id) {
        return getManager().getConfigurationMap(id);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationStrings(java.lang.String)
    */
    public String[] getConfigurationStrings(String id) {
        return getManager().getConfigurationStrings(id);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationString(String, String)
    */
    public String getConfigurationString(String id, String defaultValue) {
        return getManager().getConfigurationString(id, defaultValue);
    }

    /* (non-Javadoc)
     * @see IConfigurationAccess#getApplicationConfigurationString(java.lang.String, java.lang.String)
     */
     public String getApplicationConfigurationString(String id, String applicationName) {
         return getManager().getApplicationConfigurationString(id, applicationName);
     }
     

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationString(String)
    */
    public String getConfigurationString(String id) {
        return getManager().getConfigurationString(id);
    }
    
   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationLong(String, long)
    */
    public long getConfigurationLong(String id, long defaultValue) {
        return getManager().getConfigurationLong(id, defaultValue);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationLong(String)
    */
    public long getConfigurationLong(String id) {
        return getManager().getConfigurationLong(id);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationInt(String, int)
    */
    public int getConfigurationInt(String id, int defaultValue) {
        return getManager().getConfigurationInt(id, defaultValue);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationInt(String)
    */
    public int getConfigurationInt(String id) {
        return getManager().getConfigurationInt(id);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationBoolean(String, boolean)
    */
    public boolean getConfigurationBoolean(String id, boolean defaultValue) {
        return getManager().getConfigurationBoolean(id, defaultValue);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfigurationBoolean(String)
    */
    public boolean getConfigurationBoolean(String id) {
        return getManager().getConfigurationBoolean(id);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfiguration(String, String)
    */
    public String getConfiguration(String id, String defaultValue) {
        return getManager().getConfiguration(id, defaultValue);
    }

   /* (non-Javadoc)
    * @see IConfigurationAccess#getConfiguration(java.lang.String)
    */
    public String getConfiguration(String id) {
        return getManager().getConfiguration(id);
    }

    public Map<String, Set<String>> getParsedConditionalUrls(String id) {
        return getManager().getParsedConditionalUrls(id);
    }

   /**
    * Constructs a <code>AgentBase</code> instance using the specified 
    * <code>Manager</code> instance.
    * 
    * @param manager the <code>Manager</code> instance to be used.
    */
    protected AgentBase(Manager manager) {
        super(manager.getModule());
        setManager(manager);
    }
    
   /**
    * Allows subclasses to retrieve the associated <code>Manager</code>
    * instance.
    * 
    * @return the associated <code>Manager</code> instance.
    */
    public Manager getManager() {
        return _manager;
    }
    
   /**
    * Convenience method for accessing the service resolver instance
    * associated with this Agent runtime.
    * 
    * @return the <code>ServiceResolver</code> instance associated with this
    * Agent runtime.
    */
    protected ServiceResolver getResolver() {
        return AgentConfiguration.getServiceResolver();
    }
    
    private void setManager(Manager manager) {
        _manager = manager;
    }

    private Manager _manager;
}
