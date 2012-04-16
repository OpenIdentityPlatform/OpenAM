/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.services.cdm;

import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.ClientException;
import com.iplanet.services.cdm.ClientTypesManager;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides a basic implementation for the CDM ClientTypesManager
 * interface.
 * 
 * The different Client objects are constructed from the contents on the 
 * BasicClientTypesManager.properties file within the OpenAM web application.
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class BasicClientTypesManager implements ClientTypesManager {
    private final ConcurrentHashMap<String,Client> clients =
            new ConcurrentHashMap<String, Client>();
    private final ConcurrentHashMap<String,Map> clientsData =
            new ConcurrentHashMap<String, Map>();

    // final static String definitions
    protected final static String PROPS_FILE =
            "/BasicClientTypesManager.properties";
    protected final static String CLIENTS = "clients";
    protected final static String NAME_SUFFIX = "_name";
    protected final static String ATTRIBUTE_SUFFIX = "_attributes";
    protected final static String COMMA = ",";
    protected final static String EQUALS = "=";
    protected final static String CLASS_NAME = "BasicClientTypesManager";
    protected static Debug debug = Debug.getInstance("amClientDetection");
    
    // client detection module service name
    protected static final String CDM_SERVICE_NAME = "iPlanetAMClientDetection";

    protected static final String CDM_DEFAULT_CLIENT_TYPES_ATTR =
        "iplanet-am-client-detection-default-client-type";

    /**
     * Get the defaultClientType from iPlanetAMClientDetection Service
     */
    private static String defaultClientType = null;

    static {
        try {
            defaultClientType = getDefaultClientTypeFromService();
        } catch (Throwable t) {
            debug.error(CLASS_NAME + "init() failed: ", t);
        }
    }

    /**
     * Initializes the <code>ClientTypesManager</code>.
     */
    public void initManager() {
        Properties clientProps = new Properties();
        InputStream is = null;

        try {
            is = this.getClass().getResourceAsStream(PROPS_FILE);
            clientProps.load(is);
        } catch(Exception ex) {
            debug.error(CLASS_NAME + " unable to load properties file" + PROPS_FILE, ex);
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                debug.error(CLASS_NAME + " unable to close property file");
            }
        }

        if (clientProps != null && !clientProps.isEmpty()) {
            try {
                processProps(clientProps);
            } catch (InvalidPropertiesFormatException ipfe) {
                debug.error(CLASS_NAME + " init unable to process properties file", ipfe);
                return;
            }
        }

        if (debug.messageEnabled()) {
            debug.message(CLASS_NAME + " basic CDM framework loaded with " + clientsData);
        }
    }

    /**
     * Gets all client instance as Map.
     *
     * @return Map of clients. Key is the client type, value is the Client
     *         object
     */
    public Map getAllClientInstances() {
        return clients;
    }

    /**
     * Gets client object for specified client type.
     *
     * @param clientType
     *            requested client type.
     * @return The requested Client object
     */
    public Client getClientInstance(String clientType) {
        if (clientType.equals("default")) {
            clientType = defaultClientType;
        }

        return (Client) clients.get(clientType);
    }

    /**
     * Gets client object for specified client type with specified token
     *
     * @param clientType
     *            requested client type
     * @param token
     *            SSO Token
     * @return The requested Client object, this can be null
     */
    public Client getClientInstance(String clientType, SSOToken token) {
        return getClientInstance(clientType);
    }

    /**
     * Returns properties of the requested client type
     *
     * @param clientType
     *            requested client type
     * @return All properties of the request client type as Map
     */
    public Map getClientTypeData(String clientType) {
        if (clientType.equals("default")) {
            clientType = defaultClientType; // change to the client pointed to.
        }

        return clientsData.get(clientType);
    }

    /**
     * Gets default client type name
     *
     * @return The default client type name
     */
    public String getDefaultClientType() {
        return defaultClientType;
    }

    /**
     * Get names of all client types
     *
     * @return Set of client types as String
     */
    public Set getAllClientTypes() {
        return clients.keySet();
    }

    /**
     * Reload all Client data.
     *
     * @throws ClientException
     *             if having problem update client data
     */
    public void updateClientData()
    throws ClientException {
        // not implemented
    }

    /**
     * Save changed to persistent store.
     *
     * @param token
     *            single sign on Token of the caller.
     * @throws SSOException
     *             if the token is not valid.
     * @throws SMSException
     *             if having problem saving changes.
     */
    public void store(SSOToken token)
    throws SMSException, SSOException {
        // not implemented
    }

    /**
     * Updates client data. Need to call <code>store()</code> after this
     * method.
     *
     * @param clientType
     *            client type
     * @param data
     *            client data. Key is the property name and value is the
     *            property value as String.
     */
    public void setDirty(String clientType, Map data) {
        // not implemented
    }

    /**
     * Utility method used to parse the properties file that configures this client
     * types manager.
     * A simple entry in the properties file would look like this:
     *
     * <code>
     * clients=genericHtml,...
     * genericHtml_name=Generic HTML
     * genericHtml_attributes=filePath=html,contentType=text/html,cookieSupport=true,genericHtml=true,...
     * </code>
     *
     * The clients are the names of the different client types
     * For each client type, there is a list of attributes;
     *
     * _attributes is a comma delimited list of attributes for the client type
     *
     * @param props The properties file to parse
     * @throws InvalidPropertiesFormatException
     */
    protected void processProps(Properties props)
    throws InvalidPropertiesFormatException {
        String statGroupsNames = props.getProperty(CLIENTS);

        if (statGroupsNames == null) {
            throw new InvalidPropertiesFormatException("Unable to find " + CLIENTS + " property.");
        }

        StringTokenizer st = new StringTokenizer(statGroupsNames, COMMA);

        while (st.hasMoreTokens()) {
            populateClient(st.nextToken(), props);
        }
    }

   /**
     * Simple utility method that parsed the properties file for the different
     * clients.
     *
     * @param clientName The name of the client to populate
     * @param props The incoming properties file
     * @throws InvalidPropertiesFormatException
     */
    protected void populateClient(String clientName, Properties props)
    throws InvalidPropertiesFormatException {
        String name = props.getProperty(clientName + NAME_SUFFIX);
        String attrList = props.getProperty(clientName + ATTRIBUTE_SUFFIX);

        //Get Attributes List
        StringTokenizer st = new StringTokenizer(attrList, COMMA);
        Map attributes = new HashMap();
        Set attributeValues = null;

        while (st.hasMoreTokens()) {
            attributeValues = new HashSet();
            String entry = st.nextToken();
            String attrName = entry.substring(0, entry.indexOf(EQUALS));
            String attrValue = entry.substring(entry.indexOf(EQUALS) + 1);
            attributeValues.add(attrValue);
            attributes.put(attrName, attributeValues);
        }

        clients.put(name, new Client(name, attributes));
        clientsData.put(name, attributes);
    }

    /**
     * read service config data from SMS
     */
    private static String getDefaultClientTypeFromService()
    throws SMSException, SSOException {
        // read iPlanetAMClientDetection service using SMS API
        ServiceSchemaManager serviceSchemaManager = new ServiceSchemaManager(
                CDM_SERVICE_NAME, getInternalToken());

        ServiceSchema gsc = serviceSchemaManager.getGlobalSchema();
        Map data = gsc.getAttributeDefaults();

        return CollectionHelper.getMapAttr(data, CDM_DEFAULT_CLIENT_TYPES_ATTR);
    }

    /**
     * Get administration SSOToken
     */
    private static SSOToken getInternalToken()
    throws SSOException {
        return ((SSOToken) AccessController.doPrivileged(AdminTokenAction
                .getInstance()));
    }
}
