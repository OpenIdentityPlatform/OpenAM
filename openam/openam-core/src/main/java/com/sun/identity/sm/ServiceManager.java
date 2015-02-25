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
 * $Id: ServiceManager.java,v 1.27 2009/10/28 04:24:26 hengming Exp $
 *
 */

/*
 * Portions Copyrighted 2012-2013 ForgeRock AS
 */

package com.sun.identity.sm;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>ServiceManager</code> class provides methods to register/remove
 * services and to list currently registered services. It also provides methods
 * to obtain an instance of <code>ServiceSchemaManager</code> and an instance
 * of <code>ServiceConfigManager</code>.
 *
 * @supported.api
 */
public class ServiceManager {

    // Initialization parameters
    private static boolean initialized;

    private static boolean loadedAuthServices;

    protected static final String serviceDN = SMSEntry.SERVICES_RDN
            + "," + SMSEntry.baseDN;

    // For realms and co-existance support
    protected static final String COEXISTENCE_ATTR_NAME = "coexistenceMode";

    protected static HashMap serviceNameDefaultVersion =
        new CaseInsensitiveHashMap();

    protected static final String REALM_ATTR_NAME = "realmMode";

    public static final String REALM_SERVICE = 
        "sunidentityrepositoryservice";

    protected static final String DEFAULT_SERVICES_FOR_REALMS = 
        "serviceNamesForAutoAssignment";

    protected static final String SERVICE_VERSION = "1.0";

    protected static final String REALM_ENTRY = "ou=" + SERVICE_VERSION
            + ",ou=" + REALM_SERVICE + "," + serviceDN;

    protected static final String PLATFORM_SERVICE = "iPlanetAMPlatformService";

    protected static final String ATTR_SERVER_LIST = 
        "iplanet-am-platform-server-list";

    private static boolean realmCache;

    private static boolean coexistenceCache = true;

    private static boolean ditUpgradedCache;
    
    protected static Set requiredServices;

    protected static Set defaultServicesToLoad;

    // constants for IdRepo management
    private static final String SERVICE_OC_ATTR_NAME = "serviceObjectClasses";

    private static final String ALL_SERVICES = "null";

    private static Map serviceNameAndOCs = new CaseInsensitiveHashMap();

    // List of sub-services
    protected static SMSEntry smsEntry;

    protected static CachedSubEntries serviceNames;

    protected static HashMap serviceVersions = new CaseInsensitiveHashMap();

    protected static Set accessManagerServers;

    // SSOToken of the caller
    private SSOToken token;

    private CachedSubEntries subEntries = null;

    // Debug & I18n
    private static Debug debug = SMSEntry.debug;

    private static boolean amsdkChecked;

    private static boolean isAMSDKEnabled;

    /**
     * Creates an instance of <code>ServiceManager</code>.
     * The <code>SSOToken</code> is used to identify the user performing
     * service operations.
     * 
     * @param token
     *            the authenticated single sign on token.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public ServiceManager(SSOToken token) throws SSOException, SMSException {
        // Initilaize the static variables and caches
        initialize(token);

        // Validate SSOToken
        SSOTokenManager.getInstance().validateToken(token);
        this.token = token;
    }

    /**
     * Returns the <code>ServiceSchemaManager</code> for
     * the given service name and version.
     * 
     * @param serviceName
     *            the name of the service
     * @param version
     *            the version of the service
     * @return the <code>ServiceSchemaManager</code> for the given service
     *         name and version
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public ServiceSchemaManager getSchemaManager(String serviceName,
            String version) throws SMSException, SSOException {
        return (new ServiceSchemaManager(token, serviceName, version));
    }

    /**
     * Returns the <code>ServiceConfigManager</code> for
     * the given service name and version.
     * 
     * @param serviceName
     *            the name of the service
     * @param version
     *            the version of the service
     * @return the <code>ServiceConfigManager</code> for the given service
     *         name and version.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public ServiceConfigManager getConfigManager(String serviceName,
            String version) throws SMSException, SSOException {
        return (new ServiceConfigManager(token, serviceName, version));
    }

    /**
     * Returns the <code>OrganizationConfigManager</code> for the given
     * organization name. If the <code>orgName</code> either <code>
     * null</code>
     * or empty or "/", the organization configuration for the root organization
     * will be returned.
     * 
     * @param orgName
     *            the name of the organization
     * @return the <code>OrganizationConfigManager</code> for the given
     *         organization name
     * 
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public OrganizationConfigManager getOrganizationConfigManager(
        String orgName) throws SMSException, SSOException {
        return (new OrganizationConfigManager(token, orgName));
    }

    /**
     * Returns all the service names that have been
     * registered.
     * 
     * @return the set of names of services that have been registered
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public Set getServiceNames() throws SMSException {
        try {
            if (serviceNames == null) {
                serviceNames = CachedSubEntries.getInstance(token, serviceDN);
            }
            return (serviceNames.getSubEntries(token));
        } catch (SSOException s) {
            debug.error("ServiceManager: Unable to get service names", s);
            throw (new SMSException(s, "sms-service-not-found"));
        }
    }

    /**
     * Returns a map of service names and the related object classes for the
     * given <code>schemaType</code>.
     * 
     * @param schemaType
     *            name of the schema
     * @return Map of service names and objectclasses
     */
    public Map getServiceNamesAndOCs(String schemaType) {
        if (schemaType == null) {
            schemaType = ALL_SERVICES;
        } else if (schemaType.equalsIgnoreCase("realm")) {
            schemaType = "filteredrole";
        }
        Map answer = (Map) serviceNameAndOCs.get(schemaType);
        if (answer == null) {
            try {
                answer = new HashMap();
                Set sNames = getServiceNames();
                if (sNames != null && !sNames.isEmpty()) {
                    Iterator it = sNames.iterator();
                    while (it.hasNext()) {
                        try {
                            String service = (String) it.next();
                            ServiceSchemaManagerImpl ssm;
                            if (isCoexistenceMode()) {
                                // For backward compatibility, get the
                                // version from the service.
                                // no hardcoding to '1.0', even if it
                                // improves performance in OpenSSO.
                                // Otherwise, it breaks for services like
                                // iplanetAMProviderConfigService with
                                // '1.1' as version.
                                ssm = ServiceSchemaManagerImpl.getInstance(
                                    token, service, serviceDefaultVersion(
                                    token, service));
                            } else {
                                ssm = ServiceSchemaManagerImpl.getInstance(
                                    token, service, 
                                    ServiceManager.getVersion(service));
                            }
                            if (ssm != null) {
                                // Check if service has schemaType
                                if (schemaType != null &&
                                        ssm.getSchema(new SchemaType(
                                                schemaType)) == null) {
                                    // If the schema type is "User"
                                    // check for "Dynamic" also
                                    if (schemaType.equalsIgnoreCase(
                                            SMSUtils.USER_SCHEMA)
                                         && ssm.getSchema(SchemaType.DYNAMIC) 
                                         == null) 
                                    {
                                        continue;
                                    }
                                    // If the schema type is "Role:
                                    // check for "Dynamic" also
                                    if (schemaType.toLowerCase()
                                            .indexOf("role") != -1
                                            && ssm.getSchema(SchemaType.DYNAMIC)
                                            == null) 
                                    {
                                        continue;
                                    }
                                }
                                ServiceSchemaImpl ss = ssm
                                        .getSchema(SchemaType.GLOBAL);
                                if (ss != null) {
                                    Map attrs = ss.getAttributeDefaults();
                                    if (attrs.containsKey(SERVICE_OC_ATTR_NAME))
                                    {
                                        answer.put(service, attrs
                                                .get(SERVICE_OC_ATTR_NAME));
                                    }
                                }
                            }
                        } catch (SMSException smse) {
                            // continue with next service. Best effort to get
                            // all service names.
                            if (debug.messageEnabled()) {
                                debug.message(
                                        "ServiceManager.getServiceNamesandOCs"
                                          + " caught SMSException ", smse);
                            }
                        }
                    }
                }
                serviceNameAndOCs.put(schemaType, answer);
            } catch (SMSException smse) {
                // ignore
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager.getServiceNamesandOCs"
                            + " caught SMSException ", smse);
                }
            } catch (SSOException ssoe) {
                // ignore
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager.getServiceNamesandOCs"
                            + " caught SSOException ", ssoe);
                }
            }
        }
        return (SMSUtils.copyAttributes(answer));
    }

    /**
     * Returns all versions supported by the service.
     * 
     * @param serviceName
     *            service name.
     * @return the set of versions supported by the service
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public Set getServiceVersions(String serviceName) throws SMSException {
        try {
            return (getVersions(token, serviceName));
        } catch (SSOException s) {
            debug.error("ServiceManager: Unable to get service versions", s);
            throw (new SMSException(s, "sms-version-not-found"));
        }
    }

    /**
     * Registers one or more services, defined by the XML
     * input stream that follows the SMS DTD.
     * 
     * @param xmlServiceSchema
     *            the input stream of service metadata in XML conforming to SMS
     *            DTD.
     * @return set of registered service names.
     * @throws SMSException if an error occurred while performing the operation.
     * @throws SSOException if the user's single sign on token is invalid or 
     *         expired.
     *
     * @supported.api
     */
    public Set registerServices(InputStream xmlServiceSchema)
        throws SMSException, SSOException {
        return registerServices(xmlServiceSchema, null);
    }
    
    /**
     * Registers one or more services, defined by the XML
     * input stream that follows the SMS DTD.
     *
     * @param xmlServiceSchema
     *        the input stream of service metadata in XML conforming to SMS
     *        DTD.
     * @param decryptObj Object to decrypt the password in the XML.
     * @return set of registered service names.
     * @throws SMSException if an error occurred while performing the operation
     * @throws SSOException if the user's single sign on token is invalid or
     *         expired.
     */
    public Set registerServices(
        InputStream xmlServiceSchema,
        AMEncryption decryptObj
    ) throws SMSException, SSOException {
        // Validate SSO Token
        SMSEntry.validateToken(token);
        Set sNames = new HashSet();
        List serviceNodes = new ArrayList();
        // Get the XML document and get the list of service nodes
        Document doc = SMSSchema.getXMLDocument(xmlServiceSchema);

        if (!validSMSDtdDocType(doc)) {
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_xml_invalid_doc_type, null);
        }
        
        // Before validating service schema, we need to check
        // for AttributeSchema having the syntax of "password"
        // and if present, encrypt the DefaultValues if any
        checkAndEncryptPasswordSyntax(doc, true, decryptObj);

        // Create service schema
        NodeList nodes = doc.getElementsByTagName(SMSUtils.SERVICE);
        for (int i = 0; (nodes != null) && (i < nodes.getLength()); i++) {
            Node serviceNode = nodes.item(i);
            String name = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.NAME);
            String version = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.VERSION);

            // Obtain the SMSSchema for Schema and PluginSchema
            SMSSchema smsSchema = new SMSSchema(name, version, doc);

            // Check if the schema element exists
            if (XMLUtils.getChildNode(serviceNode, SMSUtils.SCHEMA) != null) {
                validateServiceSchema(serviceNode);
                ServiceSchemaManager.createService(token, smsSchema);

                // Update the service name and version cached SMSEntry
                if (serviceNames == null) {
                    serviceNames = CachedSubEntries.getInstance(token,
                            serviceDN);
                }
                serviceNames.add(name);
                CachedSubEntries sVersions = (CachedSubEntries) serviceVersions
                        .get(name);
                if (sVersions == null) {
                    // Not present, hence create it and add it
                    sVersions = CachedSubEntries.getInstance(token,
                            getServiceNameDN(name));
                    serviceVersions.put(name, sVersions);
                }
                sVersions.add(version);
                sNames.add(name);
            }

            // Check if PluginSchema nodes exists
            for (Iterator pluginNodes = XMLUtils.getChildNodes(serviceNode,
                    SMSUtils.PLUGIN_SCHEMA).iterator(); pluginNodes.hasNext();)
            {
                Node pluginNode = (Node) pluginNodes.next();
                PluginSchema.createPluginSchema(token, pluginNode, smsSchema);
            }

            if (XMLUtils.getChildNode(serviceNode, SMSUtils.CONFIGURATION) 
                != null) {
                serviceNodes.add(serviceNode);
            }
        }

        if (serviceNodes.size() > 0) {
            clearCache();
        }
        /*
         * Need to do this after all the schema has been loaded
         */
        for (Iterator i = serviceNodes.iterator(); i.hasNext(); ) {
            Node svcNode = (Node)i.next();
            String name = XMLUtils.getNodeAttributeValue(svcNode,
                SMSUtils.NAME);
            String version = XMLUtils.getNodeAttributeValue(svcNode,
                SMSUtils.VERSION);
            Node configNode = XMLUtils.getChildNode(svcNode,
                SMSUtils.CONFIGURATION);
            /*
             * Store the configuration, will throw exception if
             * the service configuration already exists
             */
            CreateServiceConfig.createService(this, name, version,
                configNode, true, decryptObj);
        }
        return sNames;
    }
    
    public Document parseServicesFile(InputStream xmlServiceSchema)
    throws SMSException, SSOException {
        return parseServicesFile(xmlServiceSchema, null);
    }
    
    public Document parseServicesFile(
        InputStream xmlServiceSchema,
        AMEncryption decryptObj
    ) throws SMSException, SSOException {
        // Validate SSO Token
        SMSEntry.validateToken(token);
        //Set<SMSSchema> smsSchemas = new HashSet<SMSSchema>();
        Map<String, SMSSchema> smsSchemas = new HashMap<String, SMSSchema>();
        List serviceNodes = new ArrayList();
        // Get the XML document and get the list of service nodes
        Document doc = SMSSchema.getXMLDocument(xmlServiceSchema);

        return doc;
    }
    
    /*
        if (!validSMSDtdDocType(doc)) {
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_xml_invalid_doc_type, null);
        }
        
        // Before validating service schema, we need to check
        // for AttributeSchema having the syntax of "password"
        // and if present, encrypt the DefaultValues if any
        checkAndEncryptPasswordSyntax(doc, true, decryptObj);

        // Create service schema
        NodeList nodes = doc.getElementsByTagName(SMSUtils.SERVICE);
        for (int i = 0; (nodes != null) && (i < nodes.getLength()); i++) {
            Node serviceNode = nodes.item(i);
            String name = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.NAME);
            String version = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.VERSION);

            // Obtain the SMSSchema for Schema and PluginSchema
            SMSSchema smsSchema = new SMSSchema(name, version, doc);
            smsSchemas.put(name, smsSchema);
            
        }

            // Check if the schema element exists
            /*if (XMLUtils.getChildNode(serviceNode, SMSUtils.SCHEMA) != null) {
                validateServiceSchema(serviceNode);
                ServiceSchemaManager.createService(token, smsSchema);

                // Update the service name and version cached SMSEntry
                if (serviceNames == null) {
                    serviceNames = CachedSubEntries.getInstance(token,
                            serviceDN);
                }
                serviceNames.add(name);
                CachedSubEntries sVersions = (CachedSubEntries) serviceVersions
                        .get(name);
                if (sVersions == null) {
                    // Not present, hence create it and add it
                    sVersions = CachedSubEntries.getInstance(token,
                            getServiceNameDN(name));
                    serviceVersions.put(name, sVersions);
                }
                sVersions.add(version);
                sNames.add(name);
            }

            // Check if PluginSchema nodes exists
            for (Iterator pluginNodes = XMLUtils.getChildNodes(serviceNode,
                    SMSUtils.PLUGIN_SCHEMA).iterator(); pluginNodes.hasNext();)
            {
                Node pluginNode = (Node) pluginNodes.next();
                PluginSchema.createPluginSchema(token, pluginNode, smsSchema);
            }

            if (XMLUtils.getChildNode(serviceNode, SMSUtils.CONFIGURATION) 
                != null) {
                serviceNodes.add(serviceNode);
            }
            
        }

        if (serviceNodes.size() > 0) {
            clearCache();
        }
        /*
         * Need to do this after all the schema has been loaded
         */
            /*
        for (Iterator i = serviceNodes.iterator(); i.hasNext(); ) {
            Node svcNode = (Node)i.next();
            String name = XMLUtils.getNodeAttributeValue(svcNode,
                SMSUtils.NAME);
            String version = XMLUtils.getNodeAttributeValue(svcNode,
                SMSUtils.VERSION);
            Node configNode = XMLUtils.getChildNode(svcNode,
                SMSUtils.CONFIGURATION);
            /*
             * Store the configuration, will throw exception if
             * the service configuration already exists
             */
            /*
            CreateServiceConfig.createService(this, name, version,
                configNode, true, decryptObj);
        }
        return sNames;
        return smsSchemas;
    }*/
    
    private boolean validSMSDtdDocType(Document doc) {
        boolean valid = false;
        DocumentType docType = doc.getDoctype();

        if (docType != null) {
            String dtdPath = docType.getSystemId();
            if (dtdPath != null) {
                int idx = dtdPath.lastIndexOf('/');
                if (idx != -1) {
                    dtdPath = dtdPath.substring(idx + 1);
                }
                valid = dtdPath.equals("sms.dtd");
            }
        }

        return valid;
    }

    /**
     * Adds a new plugin schema to an existing service
     *
     * @param pluginDoc 
     * @throws SMSException if an error occurred while performing the operation
     * @throws SSOException if the user's single sign on token is invalid or
     *         expired.
     */
    public void addPluginSchema(Document pluginDoc)
    throws SMSException, SSOException {
        // Validate SSO Token
        SMSEntry.validateToken(token);

        Node serviceNode = XMLUtils.getRootNode(pluginDoc, SMSUtils.SERVICE);
        String serviceName = XMLUtils.getNodeAttributeValue(serviceNode,
                SMSUtils.NAME);
        ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName, token);
        Document schemaDoc = ssm.getDocumentCopy();

        Node pluginSchemaDoc = XMLUtils.getRootNode(pluginDoc, SMSUtils.PLUGIN_SCHEMA);

        // Obtain the SMSSchema for Schema and PluginSchema
        SMSSchema smsSchema = new SMSSchema(schemaDoc);
        PluginSchema.createPluginSchema(token, pluginSchemaDoc, smsSchema);
    }

    /**
     * Removes a plugin schema from a service
     *
     * @param serviceName The name of the service
     * @param interfaceName The name of the plugin interface
     * @param pluginName The name of the plugin schema
     * @throws SMSException if an error occurred while performing the operation
     * @throws SSOException if the user's single sign on token is invalid or
     *         expired.
     */
    public void removePluginSchema(String serviceName,
                               String interfaceName,
                               String pluginName)
    throws SMSException, SSOException {
        ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName, token);
        String version = ssm.getVersion();

        // Check if PluginSchema nodes exists
        Set pluginSchemaNames = ssm.getPluginSchemaNames(interfaceName, null);

        // if they match, delete
        if (pluginSchemaNames.contains(pluginName)) {
            StringBuilder sb = new StringBuilder(100);

            // Construct the DN and get CachedSMSEntry
            sb.append("ou=").append(pluginName).append(",").append("ou=").append(
                interfaceName).append(",").append(
                CreateServiceConfig.PLUGIN_CONFIG_NODE).append("ou=").append(
                version).append(",").append("ou=").append(serviceName).append(
                ",").append(SMSEntry.SERVICES_RDN).append(",");

            CachedSMSEntry ce;

            try {
                ce = CachedSMSEntry.getInstance(token, sb.toString()
                        + SMSEntry.baseDN);
                SMSEntry smsEntry = ce.getClonedSMSEntry();
                smsEntry.forceDelete(token);
                ce.refresh(smsEntry);
            } catch (SSOException ssoe) {
                throw (new SMSException(ssoe, "sms-INVALID_SSO_TOKEN"));
            }
        } else {
            throw new SMSException("Condition does not exist");
        }

        if (debug.messageEnabled()) {
            debug.message("removePluginSchema: remove plugin " + pluginName +
                    "from service " + serviceName);
        }
    }

    /**
     * Removes the service schema and configuration for
     * the given service name.
     * 
     * @param serviceName
     *            the name of the service
     * @param version
     *            the version of the service
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void removeService(String serviceName, String version)
            throws SMSException, SSOException {
        // Find all service entries that have the DN
        // Search for (&(ou=<serviceName>)(objectclass=top))
        // construct the rdn with the given version, look for the entry
        // in iDS and if entry exists(service with that version), delete.
        if (serviceName.equalsIgnoreCase(IdConstants.REPO_SERVICE) ||
            serviceName.equalsIgnoreCase(ISAuthConstants.AUTH_SERVICE_NAME)) {
            Object args[] = { serviceName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-SERVICE_CORE_CANNOT_DELETE", args));
        }
        SMSEntry.validateToken(token);
        String[] objs = { serviceName };
        Iterator results = SMSEntry.search(token, SMSEntry.baseDN,
            MessageFormat.format(SMSEntry.FILTER_PATTERN, (Object[])objs),
            0, 0, false, false).iterator();
        while (results.hasNext()) {
            String dn = (String) results.next();
            String configdn = SMSEntry.PLACEHOLDER_RDN + SMSEntry.EQUALS
                    + version + SMSEntry.COMMA + dn;
            CachedSMSEntry configsmse = CachedSMSEntry.getInstance(token,
                    configdn);
            if (configsmse.isDirty()) {
                configsmse.refresh();
            }
            SMSEntry confige = configsmse.getClonedSMSEntry();
            if (!confige.isNewEntry()) {
                confige.delete(token);
                configsmse.refresh(confige);
            }
            // If there are no other service version nodes for that service,
            // delete that node(schema).
            CachedSMSEntry smse = CachedSMSEntry.getInstance(token, dn);
            if (smse.isDirty()) {
                smse.refresh();
            }
            SMSEntry e = smse.getSMSEntry();
            Iterator versions = 
                e.subEntries(token, "*", 0, false, false).iterator();
            if (!versions.hasNext()) {
                e.delete(token);
                smse.refresh(e);
            }
        }
    }

    /**
     * Deletes only the schema for the given service name. This is provided only
     * for backward compatibility for DSAME 5.0 and will be deprecated in the
     * future release. Alternative is to use
     * <code>ServiceSchemaManager.replaceSchema()</code>.
     * 
     * @param serviceName
     *            Name of service to be deleted.
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     */
    public void deleteService(String serviceName) throws SMSException,
            SSOException {
        if (serviceName.equalsIgnoreCase(IdConstants.REPO_SERVICE) ||
            serviceName.equalsIgnoreCase(ISAuthConstants.AUTH_SERVICE_NAME)) {
            Object args[] = { serviceName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-SERVICE_CORE_CANNOT_DELETE", args));
        }

        Iterator versions = getServiceVersions(serviceName).iterator();
        while (versions.hasNext()) {
            String version = (String) versions.next();
            CachedSMSEntry ce = CachedSMSEntry.getInstance(token,
                    getServiceNameDN(serviceName, version));
            if (ce.isDirty()) {
                ce.refresh();
            }
            SMSEntry e = ce.getClonedSMSEntry();
            String[] values = { SMSSchema.getDummyXML(serviceName, version) };
            e.setAttribute(SMSEntry.ATTR_SCHEMA, values);
            e.save(token);
            ce.refresh(e);
        }
    }

    /**
     * Returns the base DN (or root DN) that was set in
     * <code>serverconfig.xml</code> at install time.
     */
    public static String getBaseDN() {
        return (SMSEntry.baseDN);
    }

    /**
     * Returns all AM Server instance. Read the configured servers from platform
     * service's <code>iplanet-am-platform-server-list</code>
     */
    public static Set getAMServerInstances() {
        // Check cache
        if (accessManagerServers == null) {
            // Get AdminToken
            try {
                SSOToken token = (SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance());
                accessManagerServers = ServerConfiguration.getServers(token);
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager.getAMServerInstances: "
                        + "server list: " + accessManagerServers);
                }
            } catch (SMSException e) {
                if (debug.warningEnabled()) {
                    debug.warning("ServiceManager.getAMServerInstances: " +
                        "Unable to get server list", e);
                }
            } catch (SSOException e) {
                if (debug.warningEnabled()) {
                    debug.warning("ServiceManager.getAMServerInstances: " +
                        "Unable to get server list", e);
                }
            }
        }
        return (accessManagerServers == null ? new HashSet() : new HashSet(
                accessManagerServers));
    }

    /**
     * Returns organization names that match the given attribute name and
     * values. Only exact matching is supported, and if more than one value is
     * provided the organization must have all these values for the attribute.
     * Basically an AND is performed for attribute values for searching.
     * 
     * @param serviceName
     *            service name under which the attribute is to be sought.
     * @param attrName
     *            name of the attribute to search.
     * @param values
     *            set of attribute values to search.
     * @return organizations that match the attribute name and values.
     * @throws SMSException
     *             if an error occurred while performing the operation.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired.
     */
    public Set searchOrganizationNames(String serviceName, String attrName,
            Set values) throws SMSException, SSOException {

        try {
            if (subEntries == null) {
                subEntries = CachedSubEntries.getInstance(token,
                        SMSEntry.SERVICES_RDN + SMSEntry.COMMA
                                + SMSEntry.baseDN);
            }
            return (subEntries.searchOrgNames(token, serviceName.toLowerCase(),
                    attrName, values));
        } catch (SSOException ssoe) {
            debug.error("OrganizationConfigManagerImpl: Unable to "
                    + "get sub organization names", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Removes all the SMS cached entries. This method
     * should be called to clear the cache for example, if ACIs for the SMS
     * entries are changed in the directory. Also, this clears the SMS entries
     * only in this JVM instance. If multiple instances (of JVM) are running
     * this method must be called within each instance.
     *
     * @supported.api
     */
    public synchronized void clearCache() {
        // Clear the local caches
        serviceNameAndOCs = new CaseInsensitiveHashMap();
        serviceVersions = new CaseInsensitiveHashMap();
        serviceNameDefaultVersion = new CaseInsensitiveHashMap();
        accessManagerServers = null;
        amsdkChecked = false;

        // Call respective Impl classes
        CachedSMSEntry.clearCache();
        CachedSubEntries.clearCache();
        // ServiceSchemaManagerImpl.clearCache();
        PluginSchemaImpl.clearCache();
        ServiceInstanceImpl.clearCache();
        ServiceConfigImpl.clearCache();
        ServiceConfigManagerImpl.clearCache();
        OrganizationConfigManagerImpl.clearCache();
        OrgConfigViaAMSDK.clearCache();

        // Re-initialize the flags
        try {
            checkFlags(token);
            OrganizationConfigManager.initializeFlags();
            DNMapper.clearCache();
        } catch (Exception e) {
            debug.error("ServiceManager::clearCache unable to " +
                "re-initialize global flags", e);
        }
    }

    /**
     * Returns the flag which lets IdRepo and SM know that we are running in the
     * co-existence mode.
     * 
     * @return true or false depending on if the coexistence flag is enabled or
     *         not.
     */
    public static boolean isCoexistenceMode() {
        isRealmEnabled();
        return (coexistenceCache);
    }

    /**
     * Returns the version for a service. This is to handle the co-existence
     * of OpenSSO and AM 7.1 in realm mode. The co-existence of OpenSSO and
     * AM 7.1 in legacy mode is handled by the call to isCoexistenceMode() 
     * method. There is a special service named "iPlanetAMProviderConfigService"
     * used in AM 7.x code for ID-FF metadata, the version for the service
     * is "1.1", all the rest of service is "1.0" right now. This method can 
     * be removed if no need to support Co-existence of OpenSSO and AM 7.x 
     * any more.
     * @param serviceName Name of the service.
     * @return version of the service, the value will be 1.0 or 1.1.
     */
    protected static String getVersion(String serviceName) {
        if ("iPlanetAMProviderConfigService".equals(serviceName)) {
            return "1.1";
        } else {
            return "1.0";
        }
    }
 
    /**
     * Returns <code>true</code> if current service
     * configuration uses the realm model to store the configuration data.
     * 
     * @return <code>true</code> is realm model is used for storing
     *         configuration data; <code>false</code> otherwise.
     *
     * @supported.api
     */
    public static boolean isRealmEnabled() {
        if (!initialized) {
            try {
                initialize((SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance()));
            } catch (Exception ssme) {
                debug.error("ServiceManager::isRealmEnabled unable to "
                        + "initialize", ssme);
            }
        }
        return (realmCache);
    }
    
    
    /**
     * Returns <code>true</code> if AMSDK IdRepo plugin is
     * configured in any of the realms
     */
    public static boolean isAMSDKConfigured() throws SMSException {
        if (!isRealmEnabled() || OrgConfigViaAMSDK.isAMSDKConfigured("/")) {
            // Legacy mode, AMSDK is configured by default
            return (true);
        }

        // Iterate through all the realms to check if AMSDK is configured
        SSOToken token = (SSOToken) AccessController
            .doPrivileged(AdminTokenAction.getInstance());
        Set realms = (new OrganizationConfigManager(token, "/"))
            .getSubOrganizationNames("*", true);
        for (Iterator items = realms.iterator(); items.hasNext();) {
            String realm = items.next().toString();
            if (OrgConfigViaAMSDK.isAMSDKConfigured(realm)) {
                return (true);
            }
        }
        return (false);
    }
    
    /**
     * Returns <code>true</code> if configuration data has been migrated to
     * Access Manager 7.0. Else <code>false</code> otherwise.
     * 
     * @return <code>true</code> if configuration data has been migrated to AM
     *         7.0; <code>false</code> otherwise
     */
    public static boolean isConfigMigratedTo70() {
        isRealmEnabled();
        return (ditUpgradedCache);
    }

    // ------------------------------------------------------------
    // Protected methods
    // ------------------------------------------------------------

    // Called by CreateServiceConfig.java to create LDAP entries
    SSOToken getSSOToken() {
        return (token);
    }

    protected static String getCacheIndex(String serviceName, String version) {
        StringBuilder sb = new StringBuilder(20);
        return (
            sb.append(serviceName).append(version).toString().toLowerCase());
    }

    protected static String getServiceNameDN(String serviceName) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(SMSEntry.PLACEHOLDER_RDN).append(SMSEntry.EQUALS).append(
                serviceName).append(SMSEntry.COMMA).append(serviceDN);
        return (sb.toString());
    }

    protected static String getServiceNameDN(String serviceName, String version)
    {
        StringBuilder sb = new StringBuilder(100);
        sb.append(SMSEntry.PLACEHOLDER_RDN).append(SMSEntry.EQUALS).append(
                version).append(SMSEntry.COMMA).append(
                getServiceNameDN(serviceName));
        return (sb.toString());
    }

    protected static Set getVersions(SSOToken token, String serviceName)
            throws SMSException, SSOException {
        CachedSubEntries sVersions = (CachedSubEntries) serviceVersions
                .get(serviceName);
        if (sVersions == null) {
            sVersions = CachedSubEntries.getInstance(token,
                    getServiceNameDN(serviceName));
            if (sVersions == null || sVersions.getSMSEntry().isNewEntry()
                    || sVersions.getSubEntries(token).isEmpty()) {
                String[] msgs = { serviceName };
                throw (new ServiceNotFoundException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.SMS_service_does_not_exist, msgs));
            }
            serviceVersions.put(serviceName, sVersions);
        }
        return (sVersions.getSubEntries(token));
    }

    protected static void checkAndEncryptPasswordSyntax(Document doc,
        boolean encrypt
    ) throws SMSException {
         checkAndEncryptPasswordSyntax(doc, encrypt, null);
    }

    protected static void checkAndEncryptPasswordSyntax(
        Document doc,
        boolean encrypt,
        AMEncryption encryptObj
    ) throws SMSException {
        // Get the node list of all AttributeSchema
        NodeList nl = doc.getElementsByTagName(SMSUtils.SCHEMA_ATTRIBUTE);
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            // Check if the "syntax" attribute is "password"
            String syntax = XMLUtils.getNodeAttributeValue(node,
                    SMSUtils.ATTRIBUTE_SYNTAX);
            if (syntax.equals(AttributeSchema.Syntax.PASSWORD.toString())) {
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager: encrypting password syntax");
                }
                // Get the DefaultValues and encrypt then
                Node defaultNode;
                if ((defaultNode = XMLUtils.getChildNode(node,
                        SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT)) != null) {
                    // Get NodeList of "Value" nodes and encrypt them
                    for (Iterator items = XMLUtils.getChildNodes(defaultNode,
                            SMSUtils.ATTRIBUTE_VALUE).iterator(); items
                            .hasNext();) {
                        Node valueNode = (Node) items.next();
                        String value = XMLUtils.getValueOfValueNode(valueNode);
                        String encValue;
                        
                        // skip empty passwords
                        if (value.equals("null")) {
                            continue;
                        }

                        if (encrypt) {
                            if (encryptObj != null) {
                                value = (String)AccessController
                                    .doPrivileged(new DecodeAction(
                                        value, encryptObj));
                                if (value.equals("&amp;#160;")) {
                                    try {
                                        byte[] b = new byte[1];
                                        b[0] = -96;
                                        value = new String(b, "ISO-8859-1");
                                    } catch (UnsupportedEncodingException e) {
                                        //ignore
                                    }
                                }
                            }
                            encValue = (String)AccessController.doPrivileged(
                                new EncodeAction(value));
                        } else {
                            encValue = AccessController.doPrivileged(new DecodeAction(value));

                            if (encValue == null) {
                                encValue = "&amp;#160;";
                            } else {
                                try {
                                    //this is catch the whitespace for password
                                    byte[] b = encValue.getBytes("ISO-8859-1");
                                    if ((b.length == 1) && (b[0] == -96)) {
                                        encValue = "&amp;#160;";
                                    }
                                } catch (UnsupportedEncodingException e) {
                                    //ignore
                                }
                            }
                            if (encryptObj != null) {
                                encValue = (String)AccessController
                                    .doPrivileged(new EncodeAction(
                                        encValue, encryptObj));
                            }
                        }

                        // Construct the encrypted "Value" node
                        StringBuilder sb = new StringBuilder(100);
                        sb.append(AttributeSchema.VALUE_BEGIN).append(encValue)
                          .append(AttributeSchema.VALUE_END);
                        Document newDoc = SMSSchema.getXMLDocument(
                            sb.toString(), false);
                        Node newValueNode = XMLUtils.getRootNode(newDoc,
                                SMSUtils.ATTRIBUTE_VALUE);
                        // Replace the node
                        Node nValueNode = doc.importNode(newValueNode, true);
                        defaultNode.replaceChild(nValueNode, valueNode);
                    }
                }
            }
        }
    }

    protected static boolean validateServiceSchema(Node serviceNode)
            throws SMSException {
        Node schemaRoot = XMLUtils.getChildNode(serviceNode, SMSUtils.SCHEMA);
        String[] schemaNames = { SMSUtils.GLOBAL_SCHEMA, SMSUtils.ORG_SCHEMA,
                SMSUtils.DYNAMIC_SCHEMA, SMSUtils.USER_SCHEMA,
                SMSUtils.POLICY_SCHEMA, SMSUtils.GROUP_SCHEMA,
                SMSUtils.DOMAIN_SCHEMA };
        for (int i = 0; i < schemaNames.length; i++) {
            Node childNode = XMLUtils.getChildNode(schemaRoot, schemaNames[i]);
            if (childNode != null) {
                ServiceSchemaImpl ssi = new ServiceSchemaImpl(null, childNode);
                Map attrs = ssi.getAttributeDefaults();
                ssi.validateAttributes(attrs, false);
            }
        }
        return (true);
    }

    // Gets called by OrganizationConfigManager when service schema has changed
    protected static void schemaChanged() {
        // Reset the service names and OCs used by IdRepo
        serviceNameAndOCs = new CaseInsensitiveHashMap();
        // Reset the schema types and service names
        // Reset the service names
        serviceNames = null;
    }

    protected static String serviceDefaultVersion(SSOToken token,
    String serviceName) throws SMSException, SSOException {
        String version = (String) serviceNameDefaultVersion.get(serviceName);
        if (version == null) {
            Iterator iter = getVersions(token, serviceName).iterator();
            if (iter.hasNext()) {
                version = (String) iter.next();
            } else {
                String msgs[] = { serviceName };
                throw (new ServiceNotFoundException(
                    IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_service_does_not_exist,
                    msgs));
            }
            serviceNameDefaultVersion.put(serviceName, version);
        }
        return (version);
    }

    /**
     * Returns service names that will be assigned to a realm during creation.
     */
    public static Set servicesAssignedByDefault() {
        if (!loadedAuthServices) {
            AuthenticationServiceNameProvider provider = 
                AuthenticationServiceNameProviderFactory.getProvider();
            defaultServicesToLoad.addAll(provider
                    .getAuthenticationServiceNames());
            if (debug.messageEnabled()) {
                debug.message("ServiceManager::servicesAssignedByDefault:"
                        + "defaultServicesToLoad = " + defaultServicesToLoad);
            }
            loadedAuthServices = true;
            defaultServicesToLoad = Collections
                    .unmodifiableSet(defaultServicesToLoad);
        }
        return (defaultServicesToLoad);
    }
    
    /**
     * Returns service names configured via IdRepo service to be
     * added as required services
     */
    static Set requiredServices() {
        return (requiredServices);
    }

    static void initialize(SSOToken token) throws SMSException, SSOException {
        // Validate SSOToken
        SMSEntry.validateToken(token);

        // Check if already initialized
        if (initialized)
            return;
        // Initilaize the parameters
        try {
            // Get the service names and cache it
            serviceNames = CachedSubEntries.getInstance(token, serviceDN);
            if (serviceNames.getSMSEntry().isNewEntry()) {
                if (debug.warningEnabled()) {
                    debug.warning("SeviceManager:: Root service node "
                            + "does not exists: " + serviceDN);
                }
                String[] msgs = new String[1];
                msgs[0] = serviceDN;
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.SMS_services_node_does_not_exist, msgs));
            }
        } catch (SMSException e) {
            debug.error("ServiceManager::unable to get " + "services node: "
                    + serviceDN, e);
            throw (e);
        }
        // Check if realm is enabled and set appropriate flags
        checkFlags(token);
        initialized = true;
    }

    static void checkFlags(SSOToken token) throws SMSException, SSOException {
        try {
            CachedSMSEntry entry = CachedSMSEntry.getInstance(token,
                REALM_ENTRY);
            if (entry.isDirty()) {
                entry.refresh();
            }
            if (!entry.isNewEntry()) {
                ditUpgradedCache = true;
                ServiceConfigManagerImpl ssm = ServiceConfigManagerImpl
                        .getInstance(token, REALM_SERVICE, SERVICE_VERSION);
                ServiceConfigImpl sc = null;
                Map map = null;
                if (ssm == null
                        || (sc = ssm.getGlobalConfig(token, null)) == null
                        || (map = sc.getAttributes()) == null) {
                    return;
                }
                Set coexistEntry = (Set) map.get(COEXISTENCE_ATTR_NAME);
                if (coexistEntry != null && coexistEntry.contains("false")) {
                    coexistenceCache = false;
                }
                Set realmEntry = (Set) map.get(REALM_ATTR_NAME);
                if (realmEntry != null && realmEntry.contains("true")) {
                    realmCache = true;
                }
                // Get the default services to be loaded
                requiredServices = (Set) map
                        .get(DEFAULT_SERVICES_FOR_REALMS);
                defaultServicesToLoad = new HashSet();
                defaultServicesToLoad.addAll(requiredServices);

                // Make this flag false, for always the union of 
                // auto assignment services from idRepoService.xml and
                // auth services from AMAuthenticationManager code
                // should be returned for deep copy for a newly created
                // sub realm.
                loadedAuthServices = false;
            }
            if (debug.messageEnabled()) {
                debug.message("ServiceManager::checkFlags:realmEnabled="
                        + realmCache);
                debug.message("ServiceManager::checkFlags:coexistenceMode="
                        + coexistenceCache);
            }
        } catch (SMSException e) {
            debug.error("ServiceManager::unable to check "
                    + "if Realm is enabled: ", e);
            throw (e);
        }
    }
    
    public String toXML(AMEncryption encryptObj)
        throws SMSException, SSOException
    {
        StringBuilder buff = new StringBuilder();
        buff.append(SMSSchema.XML_ENC)
            .append("\n")
            .append("<!DOCTYPE ServicesConfiguration\n")
            .append(
       "PUBLIC \"=//iPlanet//Service Management Services (SMS) 1.0 DTD//EN\"\n")
            .append("\"jar://com/sun/identity/sm/sms.dtd\">\n\n");
        buff.append("<ServicesConfiguration>\n");

        Set serviceNames = getServiceNames();
        
        for (Iterator i = serviceNames.iterator(); i.hasNext(); ) {
            String serviceName = (String)i.next();
            Set versions = getServiceVersions(serviceName);
        
            for (Iterator j = versions.iterator(); j.hasNext(); ) {
                String version = (String)j.next();
                ServiceSchemaManager ssm = new 
                    ServiceSchemaManager(token, serviceName, version);
                String xml = ssm.toXML(encryptObj);
                ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, token);
                int idx = xml.lastIndexOf("</" + SMSUtils.SERVICE + ">");
                xml = xml.substring(0, idx) + scm.toXML(encryptObj) + "</" + 
                    SMSUtils.SERVICE + ">";
                buff.append(xml).append("\n");
            }
        }

        buff.append("</ServicesConfiguration>\n");
        return buff.toString().replaceAll("&amp;#160;", "&#160;");
    }

    /**
     * Returns <code>true</code> if AMSDK IdRepo plugin is enabled/present
     * in IdRepo Service Configuration schema
     */
    public static boolean isAMSDKEnabled() {
        if (amsdkChecked) {
            return (isAMSDKEnabled);
        }
        SSOToken adminToken = (SSOToken) AccessController
            .doPrivileged(AdminTokenAction.getInstance());
        try {
            if (!ServiceManager.isRealmEnabled()) {
                amsdkChecked = true;
                // If in legacy mode, then amSDK plugin would always be there.
                isAMSDKEnabled = true;
            } else {
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                    IdConstants.REPO_SERVICE, adminToken);
                ServiceSchema idRepoSubSchema = ssm.getOrganizationSchema();
                Set idRepoPlugins = idRepoSubSchema.getSubSchemaNames();
                if (idRepoPlugins.contains("amSDK")) {
                    isAMSDKEnabled = true;
                }
                amsdkChecked = true;
            }
        } catch (Exception e) {
            debug.error("IdUtils.isAMSDKEnabled() " +
                "Error in checking AM.SDK being configured", e);
        }
        amsdkChecked = true;
        return (isAMSDKEnabled);
    }
}
