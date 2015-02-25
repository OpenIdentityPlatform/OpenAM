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
 * $Id: AMClientCapData.java,v 1.7 2009/01/28 05:34:49 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2010-2011] [ForgeRock AS]
 */
package com.iplanet.services.cdm.clientschema;

import com.iplanet.am.sdk.AMEntity;
import com.iplanet.am.sdk.AMOrganizationalUnit;
import com.iplanet.am.sdk.AMSearchControl;
import com.iplanet.am.sdk.AMSearchResults;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.services.ldap.event.DSEvent;
import com.iplanet.services.ldap.event.IDSEventListener;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.controls.LDAPPersistSearchControl;
import com.sun.identity.shared.ldap.util.DN;

/**
 * The abstraction to get/set the clients stored in the DSAME. The Client is
 * stored as a DIT in directory server with the properties as attributes.
 */
public class AMClientCapData implements IDSEventListener {
    //
    // static stuff
    //
    private static final String SERVICE_VERSION = "1.0";

    //
    // The service name
    //
    private static final String SERVICE_NAME = "SunAMClientData";

    private static final String BUNDLE_NAME = "amClientData";

    //
    // To be overriden when read from config
    //
    private static String OBJECTCLASS = "objectclass";

    private static String CLIENT_OBJECTCLASS = "sunAMClient";

    private static String UMS_ADD_TEMPLATE_NAME = "Client";

    private static String UMS_SRCH_TEMPLATE_NAME = "BasicClientSearch";

    private static String CLIENT_PREFIX = "sunamclient";

    //
    // Error codes
    //
    private static final String CREATE_FAILED = "901";

    private static final String MISSING_PROP_CT = "903";

    private static final String CT_EXISTS = "904";

    private static final String ADD_FAILED = "905";

    private static final String CANNOT_MOD_INT_DB = "906";

    private static final String MODIFY_FAILED = "907";

    private static final String DELETE_FAILED = "908";

    //
    // Begin attribute/schema names from amClientData.xml //
    //

    //
    // The attribute names in the service.
    // Used by console plug-in. Should match the attribute names in the
    // Service definition xml.
    //
    private static final String[] dsameAttributeNames = {
            "generalPropertyNames", "hardwarePlatformNames",
            "softwarePlatformNames", "networkCharacteristicsNames",
            "browserUANames", "wapCharacteristicsNames",
            "pushCharacteristicsNames", "additionalPropertiesNames" };

    private static final String DBSTORE_SUBSCHEMA_ID = "clientDBStore";

    private static final String CLIENT_SUBSCHEMA_ID = "clientData";

    private static final String INTERNAL_DB = "internalData";

    private static final String EXTERNAL_DB = "externalData";

    private static final String INTERNAL_DATA = "ou=" + INTERNAL_DB;

    private static final String EXTERNAL_DATA = "ou=" + EXTERNAL_DB;

    //
    // Integer representation of the db.
    //
    public static final int INTERNAL = 1;

    public static final int EXTERNAL = 2;

    /**
     * The type of modification
     */
    public static final int ADDED = DSEvent.OBJECT_ADDED;

    public static final int REMOVED = DSEvent.OBJECT_REMOVED;

    public static final int MODIFIED = DSEvent.OBJECT_CHANGED;

    private static final String CLIENT_TYPE = "clientType";

    private static final String USER_AGENT = "userAgent";

    private static final String PARENT_ID = "parentId";

    private static final String ADD_PROPS = "additionalProperties";

    private static final String EQUALS = "=";

    private static final String COMMA = ",";

    //
    // The OU of the DN is the clientType for the device.
    //
    private static String CLIENTTYPE_ATTR = "ou";

    public static final String ADD_PROP_SEPARATOR = EQUALS;

    public static final String ADDITIONAL_PROPERTIES_ATTR = CLIENT_PREFIX
            + ADD_PROPS;

    private static final String PROFILE_MANAGER_XML_ATTR = "profileManagerXML";

    private static final String ENABLE_CLIENT_CREATION_ATTR = 
        "enableClientCreation";

    // End - attribute/schema names from amClientData.xml //

    private static SSOToken adminToken = null;

    private static Debug debug = Debug.getInstance("amClientSchema");

    private static AMStoreConnection amConnection = null;

    private static ServiceManager sManager = null;

    private static String topLevelDN = null;

    //
    // The dn to reach the ou=1.0,ou=SunAMClientData,..
    //
    private static String clientDataDN = null;

    private static String CLIENT_DATA_DN_PREFIX = 
        "ou=1.0,ou=SunAMClientData,ou=ClientData";

    //
    // The actual internal & external instance objects.
    //
    private static AMClientCapData internalInstance = null;

    private static AMClientCapData externalInstance = null;

    private static Set wholeClient = new HashSet();

    private static Set minClient = new HashSet();

    //
    // The schema for the service.
    //
    private static ServiceSchema clientServiceSchema = null;

    //
    // Schema for the client data.
    //
    private static ServiceSchema clientSchema = null;

    private static Map schemaMap = null;

    //
    // Needed since OpenSSO retrieves all LDAP attrs in lowercase &
    // also since we prefix client attrs with "sunamclient"
    //
    private static Map schemaToLDAP = new HashMap();

    private static Map LDAPToSchema = new HashMap();

    // Persisstent search related values
    protected static final String SEARCH_FILTER = "(" + OBJECTCLASS + EQUALS
            + CLIENT_OBJECTCLASS + ")";

    protected static final int OPERATIONS = LDAPPersistSearchControl.ADD
            | LDAPPersistSearchControl.MODIFY | LDAPPersistSearchControl.DELETE;

    // BEGIN: per-instance variables //

    //
    // int representing the db instance
    //
    private int databaseType = 0;

    private String dbStr = null; // for debug messages

    //
    // DSAME Object to access the AMOrganizationalUnit
    //
    private AMOrganizationalUnit amClientOrg = null;

    private List listeners = new ArrayList(2);

    //
    // The dn to reach the ou=internaldata,..
    //
    private String databaseDN = null;

    // END per-instance vars. //

    private AMClientCapData(int dbType) throws Exception {
        String dbName = null;
        databaseType = dbType; // order is important !

        if (isInternalInstance()) {
            dbName = INTERNAL_DATA;
            dbStr = "InternalDB:: "; // for debug messages
        } else {
            dbName = EXTERNAL_DATA;
            dbStr = "ExternalDB:: "; // for debug messages
        }
        init(dbName); // call init after setting per-instance vars
    }

    /**
     * 1. get the admin token (or create one) 2. Create a ServiceManager 3. Get
     * the ServiceSchemaManager for the service 4. Get the ServiceSchema for the
     * Global schema 5. Get the schema for the "internalData" schema. (temp
     * var). 6. Get the schema for the "clientData" schema id. (overwrite 8). 7.
     * Get the ROOT_SUFFIX 8. Read config info & properties schema from
     * ServiceSchema 9. Add Listeners to EventService.
     */
    private synchronized void init(String instanceRDN) throws Exception {
        String srvcName = getServiceName(); // "SunAMClientData"

        if (adminToken == null) { // single static instance
            adminToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());

            sManager = new ServiceManager(adminToken); // (2)

            ServiceSchemaManager schemaManager = sManager.getSchemaManager(
                    srvcName, SERVICE_VERSION); // (3)

            clientServiceSchema = schemaManager.getGlobalSchema(); // (4)

            //
            // the internalDB & externalDB share the same schema (5)
            //
            clientSchema = clientServiceSchema
                    .getSubSchema(DBSTORE_SUBSCHEMA_ID);
            clientSchema = clientSchema.getSubSchema(CLIENT_SUBSCHEMA_ID); //(6)

            amConnection = new AMStoreConnection(adminToken);

            topLevelDN = amConnection.getOrganizationDN(null, null); // (7)

            initClientSchema(); // (8)
            initConfigurationInfo(clientServiceSchema);

            clientDataDN = CLIENT_DATA_DN_PREFIX + COMMA + topLevelDN;

            // TBD : Commented so that persistant search is not setup to
            // directory server when running in remote client SDK mode.
            // This is temporary fix. Proper fix for this problem is TBD.
            // initEventListeners (adminToken, clientDataDN); // (9)
        }

        databaseDN = instanceRDN + COMMA + clientDataDN;

        amClientOrg = amConnection.getOrganizationalUnit(databaseDN);
    }

    private void initConfigurationInfo(ServiceSchema schema) {
        Set vals = getServiceAttribute(schema, "configInfo");
        if (vals == null) {
            vals = Collections.EMPTY_SET;
        }

        Iterator iter = vals.iterator();
        while (iter.hasNext()) {
            String val = (String) iter.next();
            int index = val.indexOf(EQUALS);
            String key = val.substring(0, index);
            String value = val.substring(index + 1);

            if (key.equalsIgnoreCase(OBJECTCLASS)) {
                CLIENT_OBJECTCLASS = value;
            } else if (key.equalsIgnoreCase("umsAddTemplateName")) {
                UMS_ADD_TEMPLATE_NAME = value;
            } else if (key.equalsIgnoreCase("umsSearchTemplateName")) {
                UMS_SRCH_TEMPLATE_NAME = value;
            } else if (key.equalsIgnoreCase("prefixForAttr")) {
                CLIENT_PREFIX = value;
            } else if (key.equalsIgnoreCase("rdn")) {
                CLIENTTYPE_ATTR = value;
            } else if (key.equalsIgnoreCase("clientDN")) {
                CLIENT_DATA_DN_PREFIX = value;
            } else if (key.equalsIgnoreCase("minimalClientAttrs")) {
                addToSet(value, minClient);
            }
        }

        //
        // Default configuration if not obtained from service.
        //
        if (minClient.isEmpty()) {
            String DEF_MIN_CLIENTS = CLIENTTYPE_ATTR + COMMA + USER_AGENT
                    + COMMA + PARENT_ID;

            addToSet(DEF_MIN_CLIENTS, minClient);
        }
    }

    private void addToSet(String val, Set s) {
        StringTokenizer st = new StringTokenizer(val, ",");
        while (st.hasMoreElements()) {
            String str = st.nextToken();
            if (str.equalsIgnoreCase("rdn")) {
                str = CLIENTTYPE_ATTR;
            } else {
                str = CLIENT_PREFIX + str;
            }
            s.add(str);
        }

        return;
    }

    /**
     * @return true if databaseType == INTERNAL
     */
    private boolean isInternalInstance() {
        return (databaseType == INTERNAL);
    }

    private static void initClientSchema() {
        Set props = getSchemaElements();
        Iterator itr = props.iterator();
        while (itr.hasNext()) {
            String propName = (String) itr.next();
            String attrName = CLIENT_PREFIX + propName;

            if (propName.equals(CLIENT_TYPE)) {
                //
                // Map clientType to OU
                //
                attrName = CLIENTTYPE_ATTR;
            }

            wholeClient.add(attrName);

            String attrNameLC = attrName.toLowerCase();

            LDAPToSchema.put(attrNameLC, propName);
            schemaToLDAP.put(propName, attrName);
        }

        return;
    }

    /**
     * @return the serviceName. Used by the console plug-in to get to the
     *         resource bundle.
     */
    public String getServiceName() {
        return (SERVICE_NAME);
    }

    /**
     * Singleton method to get an internal instance
     */
    public synchronized static AMClientCapData getInternalInstance()
            throws AMClientCapException {
        if (internalInstance == null) {
            try {
                internalInstance = new AMClientCapData(INTERNAL);
            } catch (Exception e) {
                internalInstance = null;
                debug.error("InternalDB:: Create instance object failed: ", e);
                throw new AMClientCapException(
                        BUNDLE_NAME, CREATE_FAILED, null);
            }
        }

        return internalInstance;
    }

    /**
     * Singleton method to get an external instance
     */
    public synchronized static AMClientCapData getExternalInstance()
            throws AMClientCapException {
        if (externalInstance == null) {
            try {
                externalInstance = new AMClientCapData(EXTERNAL);
            } catch (Exception e) {
                externalInstance = null;
                debug.error("ExternalDB:: Create instance object failed: ", e);
                throw new AMClientCapException(
                        BUNDLE_NAME, CREATE_FAILED, null);
            }
        }

        return externalInstance;
    }

    /**
     * @return the dn for the client
     */
    private String generateClientDN(String clientType) {
        StringBuilder dn = new StringBuilder();
        dn.append(CLIENTTYPE_ATTR);
        dn.append(EQUALS);
        dn.append(clientType);
        dn.append(COMMA);
        dn.append(databaseDN);

        return (dn.toString());
    }

    /**
     * Get a Map of all the properties for the Client. The Map contains key of
     * property names and a Set for the values.
     * 
     * @param clientType
     *            Client Type Name.
     * @return Map of the properties or null if client not found
     */
    public Map getProperties(String clientType) {
        Map props = null;

        String dn = generateClientDN(clientType);
        try {
            AMEntity amEntity = amConnection.getEntity(dn);

            if (amEntity.isExists()) {
                Map attrsMap = amEntity.getAttributes();
                props = parsePropertyNames(attrsMap);
            }
        } catch (SSOException ssoe) {
            debug.error(dbStr + "Could not get Client, session invalid: " + clientType, ssoe);

            // admin token has timed out, retry
            adminToken = null;
            String dbName = null;

            if (isInternalInstance()) {
                dbName = INTERNAL_DATA;
            } else {
                dbName = EXTERNAL_DATA;
            }

            try {
                init(dbName); // call init after setting per-instance vars
                AMEntity amEntity = amConnection.getEntity(dn);

                if (amEntity.isExists()) {
                    Map attrsMap = amEntity.getAttributes();
                    props = parsePropertyNames (attrsMap);
                }
            } catch (Exception ex) {
                debug.error(dbStr + "Could not get Client, even after retry: " + clientType, ex);
            }
        } catch (Exception ex) {
            debug.warning(dbStr + "Could not get Client: " + clientType, ex);
        }

        return props;
    }

    /**
     * Gets the minimal client info for the specified client.
     */
    public Map loadMinimalClient(String clientType) {
        Map props = null;

        String dn = generateClientDN(clientType);
        try {
            AMEntity amEntity = amConnection.getEntity(dn);

            if (amEntity.isExists()) {
                Map attrsMap = amEntity.getAttributes(minClient);
                props = parsePropertyNames(attrsMap);
            }

        } catch (Exception e) {
            debug.warning(dbStr + "Could not get Client: " + clientType, e);
        }

        return props;
    }

    /**
     * The ldap attribute names for the client properties and prefixed with
     * "sunamclient" to make them unique. This method gets the ldap attrs and
     * converts them to client property names.
     */
    private Map parsePropertyNames(Map m) {
        Map props = new HashMap();

        Iterator allKeys = m.keySet().iterator();

        while (allKeys.hasNext()) {
            String attrName = (String) allKeys.next();
            Set vals = (Set) m.get(attrName);

            if (vals.isEmpty()) {
                continue;
            }

            if (attrName.equalsIgnoreCase(ADDITIONAL_PROPERTIES_ATTR)) {
                Iterator attrs = vals.iterator();
                while (attrs.hasNext()) {
                    String compositeVal = (String) attrs.next();

                    int index = compositeVal.indexOf(EQUALS);
                    String propName = compositeVal.substring(0, index);
                    String propVal = compositeVal.substring(index + 1);

                    addToMap(props, propName, propVal);
                }
            } else {
                String propertyName = (String) LDAPToSchema.get(attrName);
                if (propertyName != null) {
                    addToMap(props, propertyName, vals);
                }
            }
        }

        return props;
    }

    private void addToMap(Map m, String key, String val) {
        Set s = (Set) m.get(key);
        if (s == null) {
            s = new HashSet(2);
            m.put(key, s);
        }

        // works on the Set in the Map, so we dont need another put.
        s.add(val);
    }

    private void addToMap(Map m, String key, Set vals) {
        Set s = (Set) m.get(key);
        if (s == null) {
            m.put(key, vals);
        } else {
            s.addAll(vals);
        }

        return;
    }

    /**
     * Gets the profile manager xlob as a string. Used by the MAPClientDetector.
     * 
     * @return ProfileManager.xml
     */
    public String getProfileManagerXML() {
        String profileManagerXML = null;
        Set vals = getServiceAttribute(PROFILE_MANAGER_XML_ATTR);

        if (vals != null) {
            Iterator iter = vals.iterator();
            profileManagerXML = (String) iter.next();
        }

        return profileManagerXML;
    }

    /**
     * Get a dsame attribute from the internal/external service. used by
     * getProfileManagerXML()
     * 
     * @param attributeName
     *            The name of the attribute.
     * @return String The attribute as a string
     */
    private Set getServiceAttribute(String attributeName) {
        Set set = getServiceAttribute(clientServiceSchema, attributeName);
        return set;
    }

    private Set getServiceAttribute(ServiceSchema schema, String attributeName)
    {
        Set set = null;
        Map map = schema.getAttributeDefaults();

        if (map != null) {
            set = (Set) map.get(attributeName);
        }

        return set;
    }

    /**
     * Get a Set of all the Property names for the classification. Valid
     * classifications are:
     * <ol>
     * <li>generalPropertyNames</li>
     * <li>hardwarePlatformNames</li>
     * <li>softwarePlatformNames</li>
     * <li>networkCharacteristicsNames</li>
     * <li>browserUANames</li>
     * <li>wapCharacteristicsNames</li>
     * <li>pushCharacteristicsNames</li>
     * <li>additionalPropertiesNames</li>
     * </ol>
     * 
     * @return Set of PropertyNames belonging to classification or null if
     *         nothing exists.
     */
    public Set getPropertyNames(String classification) {
        Set names = null;
        if (classification != null) {
            names = getServiceAttribute(classification);
        }

        return names;
    }

    /**
     * Checks if the clientType exists in the db.
     * 
     * @return true if present, false otherwise
     */
    public boolean isClientPresent(String clientType) {
        String dn = generateClientDN(clientType);
        boolean exists = false;

        try {
            AMEntity amEntity = amConnection.getEntity(dn);
            exists = amEntity.isExists();
        } catch (SSOException ssoe) {
            /**
             * Cannot happen since we are using the AdminToken
             */
        }

        return exists;
    }

    /**
     * Get the value of the clientType property from the Map.
     * 
     * @return The value of the "clientType" property
     */
    private String getClientType(Map props) {
        String clientType = null;

        Set tmpVals = (Set) props.get(CLIENT_TYPE);
        if (tmpVals != null && tmpVals.size() > 0) {
            Iterator itr = tmpVals.iterator();
            clientType = (String) itr.next();
        }

        return clientType;
    }

    /**
     * Parse the allProps Map and return the known properties, and the
     * additional ones in a Set of name=value Strings with the Map index being
     * the ldap attr "sunamclientadditionalProperties"
     */
    private Map getKnownProperties(Map allProps) {
        Map newPropsMap = new HashMap();

        Set addProps = new HashSet(5);

        Iterator itr = allProps.keySet().iterator();
        while (itr.hasNext()) {
            String propName = (String) itr.next();
            String ldapAttrName = (String) schemaToLDAP.get(propName);
            Set vals = (Set) allProps.get(propName);

            if (ldapAttrName == null) // not in schema
            {
                if ((vals == null) || (vals.isEmpty())) {
                    continue;
                }

                String val = null;
                Iterator innerItr = vals.iterator();
                while (innerItr.hasNext()) {
                    String prop = (String) innerItr.next();
                    // separate multi-values with comma
                    val = (val == null) ? prop : val + COMMA + prop;
                }

                addProps.add(propName + ADD_PROP_SEPARATOR + val);
            } else {
                newPropsMap.put(ldapAttrName, vals);
            }
        }

        //
        // Add to the ADDITIONAL_PROPERTIES_ATTR in clientschema
        //
        if (addProps.size() > 0) {

            Set e = (Set) allProps.get(ADD_PROPS);
            if (e != null) {
                addProps.addAll(e); // add if the allProps came with one.
            }

            newPropsMap.put(ADDITIONAL_PROPERTIES_ATTR, addProps);
        }

        return newPropsMap;
    }

    /**
     * Add a client. For every property in the Map, it looks up the schema to
     * check if the property is known, if not known adds it to the
     * additionalProperties schema element. <br>
     * 
     * <b>Note: To add a property in the external db to mask the corresponding
     * property value in internal db, add the property with a " "
     * ("&lt;space&gt;") not an empty "" string. This is required because, when
     * dsame fetches the value from directory and sees it has no value, it
     * returns an empty set. (And we discard empty sets internally - bcos dsame
     * stores values for every property defined in the schema).</b>
     * 
     * @param token
     *            SSOToken to validate the user
     * @param props
     *            Map of profiles known to ClientCap. The Map "must" have a
     *            property "clientType"
     * 
     * @return 0 on success
     * @exception AMClientCapException
     *                if Client could not be added - permission problems or if
     *                the clientType property is mising in the Map.
     */
    public int addClient(SSOToken token, Map props) throws AMClientCapException
    {
        int status = 0;
        String ct = getClientType(props);
        Map m = getKnownProperties(props);

        Map entityMap = new HashMap(1);
        entityMap.put(ct, m);

        try {
            AMStoreConnection conn = new AMStoreConnection(token);
            AMOrganizationalUnit amOU = conn.getOrganizationalUnit(databaseDN);
            amOU.createEntities(UMS_ADD_TEMPLATE_NAME, entityMap);
        } catch (Exception e) {
            String[] errArgs = { ct };

            AMClientCapException ace = new AMClientCapException(BUNDLE_NAME,
                    ADD_FAILED, errArgs);

            String msg = ace.getMessage();
            debug.error(dbStr + msg, e);

            throw ace;
        }

        return status;
    }

    /**
     * Modify the properties of the Client instance in externalDB. Valid only
     * with instance of externalDB. <br>
     * <b>Note: To add a property in the external db to mask the corresponding
     * property value in internal db, add the property with a " "
     * ("&lt;space&gt;") not an empty "" string. This is required because, when
     * dsame fetches the value from directory and sees it has no value, it
     * returns an empty set. (And we discard empty sets internally - bcos dsame
     * stores values for every property defined in the schema).</b>
     * 
     * Also, if the additionalProperties are being modified, it should contain
     * both the modified and the unmodified ones. This is required since all the
     * additionalProperties are stored in a single Attribute
     * "additionalProperties".
     * 
     * @param token
     *            SSOToken to validate the user.
     * @param props
     *            Map of profiles known to ClientCapabilities. The Map contains
     *            key of property name(s) and Set for the values. It wont
     *            overwrite the property names not in the Map. A key with an
     *            empty Set for the values will delete the property (DSAME
     *            cannot handle null values - throws NullPointerExcptn). The Map
     *            "must" have a property "clientType"
     * 
     * @return 0 on success
     * @exception AMClientCapException
     *                if Client could not be modified - permission problems OR
     *                if the clientType property is mising in the Map.
     */
    public int modifyClient(SSOToken token, Map props)
            throws AMClientCapException {
        int status = 0;

        if (isInternalInstance()) {
            throw new AMClientCapException(
                    BUNDLE_NAME, CANNOT_MOD_INT_DB, null);
        }

        String ct = getClientType(props);
        try {
            AMStoreConnection localConn = new AMStoreConnection(token);
            String dn = generateClientDN(ct);

            AMEntity amEntity = localConn.getEntity(dn);

            if (amEntity.isExists()) {
                Map m = getKnownProperties(props);
                m.remove(CLIENTTYPE_ATTR); // modify shouldn't have the RDN
                amEntity.setAttributes(m);
                amEntity.store();
            } else {
                //
                // Need to add if the entry doesn't exist
                //
                status = addClient(token, props);
            }

        } catch (Exception e) {
            String[] errArgs = { ct };

            AMClientCapException ace = new AMClientCapException(BUNDLE_NAME,
                    MODIFY_FAILED, errArgs);

            String msg = ace.getMessage();
            debug.error(dbStr + msg, e);

            throw ace;
        }

        return status;
    }

    /**
     * Remove a Client - removes the client from the externalDB. Valid only with
     * instance of externalDB.
     * 
     * @param token
     *            SSOToken to validate the user
     * @param clientType
     *            Client Type Name.
     * 
     * @return 0 on success
     * @exception AMClientCapException
     *                if Client could not be removed - permission problems
     */

    public int removeClient(SSOToken token, String clientType)
            throws AMClientCapException {
        if (isInternalInstance()) {
            throw new AMClientCapException(
                    BUNDLE_NAME, CANNOT_MOD_INT_DB, null);
        }

        try {
            AMStoreConnection localConn = new AMStoreConnection(token);
            String dn = generateClientDN(clientType);

            AMEntity amEntity = localConn.getEntity(dn);
            amEntity.delete();

        } catch (Exception e) {
            String[] errArgs = { clientType };

            AMClientCapException ace = new AMClientCapException(BUNDLE_NAME,
                    DELETE_FAILED, errArgs);

            String msg = ace.getMessage();
            debug.error(dbStr + msg, e);

            throw ace;
        }

        return 0;
    }

    /**
     * This method returns the name of the "defined" client properties in the
     * Schema. To get the AttributeSchema of these elements, iterate through
     * each of these names and call the getAttributeSchema() on it. Can be used
     * with internal/external instance (since they share the same schema).
     * 
     * @return a Set of "defined" property names.
     */
    public synchronized static Set getSchemaElements() {
        Map map = getSchemaMap();
        Set set = map.keySet();

        return set;
    }

    /**
     * Returns the schema for an property given the name. Can be used with
     * internal/external instance (since they share the same schema).
     * 
     * @param propName Name of the property
     * @return com.sun.identity.sm.AttributeSchema Look at OpenSSO
     *         API
     * 
     * @see com.sun.identity.sm.AttributeSchema
     */
    public AttributeSchema getAttributeSchema(String propName) {
        Map map = getSchemaMap();
        AttributeSchema attrSchema = (AttributeSchema) map.get(propName);
        return attrSchema;
    }

    /**
     * Cache for the attribute schemas.
     */
    private synchronized static Map getSchemaMap() {
        if (schemaMap == null) {
            Set set = clientSchema.getAttributeSchemas();
            schemaMap = new HashMap();
            Iterator itr = set.iterator();
            while (itr.hasNext()) {
                AttributeSchema prop = (AttributeSchema) itr.next();
                String name = prop.getName();
                schemaMap.put(name, prop);
            }
        }

        return schemaMap;
    }

    /**
     * Checks the enableClientCliention attr in Service. Used by the
     * ClientTypesManager when creating new Clients in the internal DB.
     * 
     * @return true/false
     */
    public boolean canCreateInternalClients() {
        boolean allow = false;

        Set vals = getServiceAttribute(ENABLE_CLIENT_CREATION_ATTR);
        if (vals != null) {
            Iterator iter = vals.iterator();
            Boolean perms = Boolean.valueOf((String)iter.next());
            allow = perms.booleanValue();
        }

        return allow;
    }

    /**
     * register for listening to Client data changes
     */
    public void addListener(AMClientDataListener cdl) {
        synchronized (listeners) {
            if (!listeners.contains(cdl)) {
                listeners.add(cdl);
            }
        }
    }

    //
    // The ServiceListener interface methods
    //
    public void eventError(String err) {
        debug.warning("Handled eventError() Notification: " + err);
    }

    public void entryChanged(DSEvent event) {
        String dn = event.getID();
        DN dnObject = new DN(dn);

        String[] dnComps = dnObject.explodeDN(true);

        if (debug.messageEnabled()) {
            debug.message("entryChanged() Notification for: " + dn);
        }

        if (dnComps == null || dnComps.length < 2) {
            return; // cannot notify correctly !
        }

        String ct = dnComps[0];
        String db = dnComps[1];
        int type = event.getEventType();

        if (db == null || ct == null) {
            return;
        }

        int dbType = -1;
        if (db.equalsIgnoreCase(INTERNAL_DB)) {
            dbType = INTERNAL;
        } else if (db.equalsIgnoreCase(EXTERNAL_DB)) {
            dbType = EXTERNAL;
        } else {
            debug.warning("Unknown db: " + db + " : client = " + ct);
            return; // unknown dbType
        }

        if (debug.messageEnabled()) {
            debug.message("Notifying Listeners:: ClientType = " + ct
                    + " : DB = " + dbType + " : OP = " + type);
        }

        synchronized (listeners) {
            int size = listeners.size();
            for (int i = 0; i < size; i++) {
                AMClientDataListener cdl = (AMClientDataListener) listeners
                        .get(i);

                try {
                    cdl.clientChanged(ct, dbType, type);
                } catch (Throwable t) {
                    debug.warning("Event Notification failed: ", t);
                }
            }
        }

        return;
    }

    // TODO Add code here to handle the situation of all entries changed.
    public void allEntriesChanged() {

    }

    /**
     * @return the valid classifications.
     */
    public String[] getClassifications() {
        return dsameAttributeNames;
    }

    /**
     * Demand Load stuff
     */

    /**
     * Gets a minimal set of client properties for all clients.
     * 
     * @return Set of Maps. Each Map has the propertyNames for the Key and Value
     *         is Set of Property values. By default, the keys returned are
     *         clientType, userAgent & parentID.
     */
    public Set getMinimalClientInfo() {
        Set clients = new HashSet();
        AMSearchControl amsrchCntrl = new AMSearchControl();
        amsrchCntrl.setReturnAttributes(minClient);

        try {
            long st = System.currentTimeMillis();
            AMSearchResults results = amClientOrg.searchEntities("*",
                    amsrchCntrl, null, UMS_SRCH_TEMPLATE_NAME);

            long end = System.currentTimeMillis();
            if (debug.messageEnabled()) {
                debug.message(dbStr
                        + "getMinimalClientInfo() Srch Time (ms) = "
                        + (end - st));
            }

            st = System.currentTimeMillis();
            Map m = results.getResultAttributes();
            Iterator keys = m.keySet().iterator();

            while (keys.hasNext()) {
                String dn = (String) keys.next();
                Map attrsMap = (Map) m.get(dn);
                Map data = parsePropertyNames(attrsMap);
                clients.add(data);
            }
            end = System.currentTimeMillis();
            if (debug.messageEnabled()) {
                debug.message(dbStr
                        + "getMinimalClientInfo() Parse Time (ms) = "
                        + (end - st));
            }
        } catch (Exception e) {
            debug.error(dbStr + " getMinimalClientInfo(): Search Error: ", e);
        }

        return clients;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#getBase()
     */
    public String getBase() {
        return clientDataDN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#getFilter()
     */
    public String getFilter() {
        return SEARCH_FILTER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#getOperations()
     */
    public int getOperations() {
        return OPERATIONS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#getScope()
     */
    public int getScope() {
        return LDAPConnection.SCOPE_SUB;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.services.ldap.event.IDSEventListener#setListener()
     */
    public void setListeners(Map listener) {
        // noop.
    }

}
