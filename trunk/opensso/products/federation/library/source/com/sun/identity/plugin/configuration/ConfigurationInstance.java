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
 * $Id: ConfigurationInstance.java,v 1.3 2008/06/25 05:47:25 qcheng Exp $
 *
 */

package com.sun.identity.plugin.configuration;

import java.util.Map;
import java.util.Set;

/**
 * <code>ConfigurationInstance</code> is the interface that provides the
 * operations on service configuration. 
 *
 * @supported.all.api
 */
public interface ConfigurationInstance {

    /**
     * Initializer.
     * @param componentName Name of the components, e.g. SAML1, SAML2, ID-FF
     * @param session FM Session object.
     * @exception ConfigurationException if could not initialize the instance.
     */
    public void init(String componentName, Object session) 
        throws ConfigurationException;

    /**
     * Returns Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp".
     *     The configName could be null or empty string, which means the default
     *     configuration for this components. 
     * @return Map of key/value pairs, key is the attribute name, value is
     *     a Set of attribute values or null if service configuration doesn't
     *     exist. 
     * @exception ConfigurationException if an error occurred while getting
     *            service configuration.
     */
    public Map getConfiguration(String realm, String configName)
        throws ConfigurationException;

    /**
     * Sets Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName configuration instance name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not set service configuration
     *     or service configuration doesn't exist.
     * @exception UnsupportedOperationException if this operation is not
     *     supported by the implementation.
     */
    public void setConfiguration(String realm,
        String configName, Map avPairs)
        throws ConfigurationException,UnsupportedOperationException ;

    /**
     * Creates Configurations.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the
     *     default configuration for this components.
     * @param avPairs Map of key/value pairs to be set in the service
     *     configuration, key is the attribute name, value is
     *     a Set of attribute values. 
     * @exception ConfigurationException if could not create service 
     *     configuration.
     * @exception UnsupportedOperationException if this operation is not
     *     supported by the implementation.   
     */
    public void createConfiguration(String realm,
        String configName, Map avPairs)
        throws ConfigurationException, UnsupportedOperationException;

    /**
     * Deletes Configuration.
     * @param realm the name of organization at which the configuration resides.
     * @param configName service configuration name. e.g. "/sp"
     *     The configName could be null or empty string, which means the default
     *     configuration for this components.
     * @param attributes A set of attributes to be deleted from the Service
     *     configuration. If the value is null or empty, deletes all service 
     *     configuration.
     * @exception ConfigurationException if could not delete service 
     *     configuration.
     * @exception UnsupportedOperationException if this operation is not
     *     supported by the implementation.   
     */
    public void deleteConfiguration(String realm, 
        String configName, Set attributes)
        throws ConfigurationException, UnsupportedOperationException;

    /**
     * Returns all service configuration name for this components.
     * @param realm the name of organization at which the configuration resides.
     * @return Set of service configuration names. Return null if there 
     *     is no service configuration for this component, return empty set
     *     if there is only default configuration instance.
     * @exception ConfigurationException if could not get all service 
     *     configuration names.
     * @exception UnsupportedOperationException if this operation is not
     *     supported by the implementation.   
     */
    public Set getAllConfigurationNames(String realm) 
        throws ConfigurationException, UnsupportedOperationException;

    /**
     * Registers for changes to the component's configuration. The object will
     * be called when configuration for this component is changed.
     * @return the registered id for this listener instance.
     * @exception ConfigurationException if could not register the listener.
     * @exception UnsupportedOperationException if this operation is not
     *     supported by the implementation.   
     */
    public String addListener(ConfigurationListener listener)
        throws ConfigurationException, UnsupportedOperationException;

    /**
     * Unregisters the listener from the component for the given
     * listener ID. The ID was issued when the listener was registered.
     * @param listenerID the returned id when the listener was registered.
     * @exception ConfigurationException if could not register the listener.
     * @exception UnsupportedOperationException if this operation is not
     *     supported by the implementation.
     */
    public void removeListener(String listenerID)
        throws ConfigurationException, UnsupportedOperationException;
 }
