/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Client.java,v 1.4 2008/06/25 05:41:32 qcheng Exp $
 *
 */

package com.iplanet.services.cdm;

import com.iplanet.services.cdm.clientschema.AMClientCapData;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

/**
 * Provides common access to client data.
 * <p>
 * <p>
 * Client data is accessed for a particular client type. The underlying client
 * data is stored in the profile service, but this interface should always used
 * for accessing it (not by accessing the profile directly).
 * @supported.api
 */

public class Client extends Observable implements ICDMConstants {
    private static final String ADD_PROP_ATTR = 
        AMClientCapData.ADDITIONAL_PROPERTIES_ATTR;

    private static final String SEPARATOR = AMClientCapData.ADD_PROP_SEPARATOR;

    private static G11NSettings g11nSettings = G11NSettings.getInstance();

    private static Debug debug = Debug.getInstance("amClientDetection");

    //
    // The ClientType of this Object.
    //
    private String cType = null;

    //
    // Keep the attribute names & values in a Map. Key = name,
    // value = (Set) of values. synchronizations around this Map are not
    // required since the setProperty() is protected and the Map modifications
    // are done only at object Construction time
    //
    private HashMap profileMap = null;

    //
    // Store the additionalProperties in a Set - used by the console plug-in
    // only
    //
    private Set additionalProperties = null;

    public Client() {
    }

    public Client(String clientType, Map data) {
        profileMap = new HashMap(data.size());
        profileMap.putAll(data);

        cType = clientType; // set our private var
        additionalProperties = separateAdditionalProperties(profileMap);
    }

    /**
     * Get Client instance for a specific client type .
     * 
     * @param clientType
     *            Client Type.
     * @return Requested Client instance.
     * @throws ClientException
     *             if specified client type is null or not defined
     * @deprecated Use ClientsManager#getInstance(String)
     * @supported.api
     */
    public static Client getInstance(String clientType) throws ClientException {
        return ClientsManager.getInstance(clientType);
    }

    /**
     * When setting client data, get a Client instance for a specific client
     * type. A valid user session is required when setting client data.
     * 
     * @param clientType
     *            Client type
     * @param token
     *            SSO Token of the caller
     * @return Client instance
     * @throws ClientException
     *             if client type is null or not defined
     * 
     * @deprecated Use ClientsManager#getInstance(String)
     */
    protected static Client getInstance(String clientType, SSOToken token)
            throws ClientException {
        return getInstance(clientType);
    }

    /**
     * Returns a Client instance for the default client type
     * 
     * @return The Client instance corresponding to the default client type
     * @deprecated Use ClientsManager#getDefaultInstance()
     * @supported.api
     */
    public static Client getDefaultInstance() {
        return ClientsManager.getDefaultInstance();
    }

    /**
     * Returns an iterator of Client objects for all known client types.
     * 
     * @return Iterator of Client objects
     * @deprecated Use ClientsManager#getAllInstances()
     * @supported.api
     */
    public static Iterator getAllInstances() {
        return ClientsManager.getAllInstances();
    }

    /**
     * When setting client data, returns an iterator of Client objects for all
     * known client types. A valid user session is required when setting client
     * data.
     * 
     * @param token
     *            The user's SSO token
     * @return Iterator of Client objects
     * @deprecated Use ClientsManager#getAllInstances()
     */
    protected static Iterator getAllInstances(SSOToken token) {
        return getAllInstances();
    }

    /**
     * Gets the name of the client type for the data in this client instance.
     * 
     * @return Name of the client type
     * @supported.api
     */
    public String getClientType() {
        return cType;
    }

    /**
     * Gets the client property for the specified key.
     * 
     * @param name
     *            The key for the client property to be returned.
     * @return The client property. Return null if name is null or an unknown
     *            key
     * @supported.api
     */
    public String getProperty(String name) {
        String value = null;
        Set properties = null;

        if ((properties = getPropertiesInternal(name)) != null) {
            Iterator iter = properties.iterator();
            value = (String) iter.next(); // get first element
        }

        return (value);
    }

    private Set getPropertiesInternal(String attributeName) {
        Set properties = (Set) profileMap.get(attributeName);
        return (properties);
    }

    /**
     * Gets the client property for the specified key.
     * 
     * @param name
     *            The key for the client property to be returned.
     * @return The set of client property values. Returns null if name is null
     *            or an unknown key
     * @supported.api
     */
    public Set getProperties(String name) {
        Set properties = getPropertiesInternal(name);

        Set umSet = null;
        if (properties != null) {
            umSet = Collections.unmodifiableSet(properties);
        }

        return (umSet);
    }

    /**
     * Returns a set of property names for this client data instance.
     * 
     * @return The set of property names for this client data instance.
     * @supported.api
     */
    public Set getPropertyNames() {
        Set keys = profileMap.keySet();
        return (keys);
    }

    public String getCharset(java.util.Locale loc) {
        try {
            return g11nSettings.getCharset(cType, loc);
        } catch (ClientException ex) {
            debug.error("Client.getCharset ", ex);

        }
        return CDM_DEFAULT_CHARSET;
    }

    /**
     * used by the console plug-in (only) to get the additional properties.
     */
    public Set getAdditionalProperties() {
        return additionalProperties;
    }

    /**
     * Removes the "additionalProperties" element from the Map, adds each of
     * them to the Map with name and value (parsed with "=") and returns the
     * values of the "additionalProperties in the Set.
     * 
     * @return Set of the additionalProperties
     */
    protected Set separateAdditionalProperties(Map m) {
        Set addProps = null;

        if ((m != null) && ((addProps = (Set) m.get(ADD_PROP_ATTR)) != null)
                && (addProps.size() > 0)) {
            m.remove(ADD_PROP_ATTR); // remove it
            Iterator itr = addProps.iterator();
            while (itr.hasNext()) {
                String property = (String) itr.next();
                int index = property.indexOf(SEPARATOR);

                if (index <= 0) {// ignore props with no name
                    continue;
                }

                String name = property.substring(0, index);
                String val = property.substring(index + 1);

                Set set = new HashSet(1);
                set.add(val);

                m.put(name, set);
            }
        }

        return addProps;
    }

}
