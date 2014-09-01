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
 * $Id: ServiceConfigManager.java,v 1.11 2009/07/25 05:11:55 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>ServiceConfigurationManager</code> provides interfaces to
 * manage the service's configuration data. It provides access to
 * <code>ServiceConfig</code> which represents a single "configuration" in the
 * service. It manages configuration data only for GLOBAL and ORGANIZATION
 * types.
 *
 * @supported.api
 */
public class ServiceConfigManager {
    // Instance variables
    private SSOToken token;


    private String serviceName;

    private String version;

    // Pointer to ServiceSchemaManangerImpl
    private ServiceSchemaManagerImpl ssm;

    private ServiceConfigManagerImpl scm;

    /**
     * Constrctor to obtain an instance <code>ServiceConfigManager
     * </code> for
     * a service by providing an authenticated identity of the user.
     * This constructor assumes the server version to be <code>1.0</code>.
     * 
     * @param serviceName
     *            name of the service
     * @param token
     *            single sign on token of authenticated user identity
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     */
    public ServiceConfigManager(String serviceName, SSOToken token)
            throws SMSException, SSOException {
        // Use of the service versions
        this(token, serviceName, ServiceManager.isCoexistenceMode() ?
            ServiceManager.serviceDefaultVersion(token, serviceName) :
            ServiceManager.getVersion(serviceName));
    }

    /**
     * Creates an instance of
     * <code>ServiceConfigManager</code> for the given service and version. It
     * requires an user identity, that will used to perform operations with. It
     * is assumed that the application calling this constructor should
     * authenticate the user.
     * 
     * @param token
     *            single sign on token of the user identity on whose behalf the
     *            operations are performed.
     * @param serviceName
     *            the name of the service
     * @param version
     *            the version of the service
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public ServiceConfigManager(SSOToken token, String serviceName,
            String version) throws SMSException, SSOException {
        if (token == null || serviceName == null || version == null) {
            throw new IllegalArgumentException(SMSEntry.bundle
                    .getString(IUMSConstants.SMS_INVALID_PARAMETERS));
        }
        SSOTokenManager.getInstance().validateToken(token);
        
        // Copy instance variables
        this.token = token;
        this.serviceName = serviceName;
        this.version = version;
        
        // Get the ServiceSchemaManagerImpl
        validateSCM();
    }

    /**
     * Returns the name of the service.
     * 
     * @return the name of the service
     *
     * @supported.api
     */
    public String getName() {
        return (serviceName);
    }

    /**
     * Returns the service version.
     * 
     * @return the version of the service
     *
     * @supported.api
     */
    public String getVersion() {
        return (version);
    }

    /**
     * Returns the service instance names
     * 
     * @return the service instance names
     * @throws SMSException
     *             if an error has occurred while performing the operation
     *
     * @supported.api
     */
    public Set getInstanceNames() throws SMSException {
        try {
            validateSCM();
            return (scm.getInstanceNames(token));
        } catch (SSOException s) {
            SMSEntry.debug.error("ServiceConfigManager: Unable to "
                    + "get Instance Names", s);
        }
        return (Collections.EMPTY_SET);
    }

    /**
     * Returns the configuration group names
     * 
     * @return the service configuration group names
     * @throws SMSException
     *             if an error has occurred while performing the operation
     *
     * @supported.api
     */
    public Set getGroupNames() throws SMSException {
        try {
            validateSCM();
            return (scm.getGroupNames(token));
        } catch (SSOException s) {
            SMSEntry.debug.error("ServiceConfigManager: Unable to "
                    + "get Group Names", s);
        }
        return (Collections.EMPTY_SET);
    }

    /**
     * Returns the service instance given the instance
     * name
     * 
     * @param instanceName
     *            the name of the service instance
     * @return service instance for the given instance name
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *
     * @supported.api
     *             if the user's single sign on token is invalid or expired
     */
    public ServiceInstance getInstance(String instanceName)
            throws SMSException, SSOException {
        validateSCM();
        return (new ServiceInstance(this, 
                scm.getInstance(token, instanceName)));
    }

    /**
     * Removes the instance form the service
     * 
     * @param instanceName
     *            the service instance name
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void removeInstance(String instanceName) throws SMSException,
            SSOException {
        getInstance(instanceName).delete();
    }

    /**
     * Returns the global configuration for the given
     * service instance.
     * 
     * @param instanceName
     *            the service instance name
     * @return the global configuration for the given service instance
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public ServiceConfig getGlobalConfig(String instanceName)
            throws SMSException, SSOException {
        validateSCM();
        ServiceConfigImpl sci = scm.getGlobalConfig(token, instanceName);
        return ((sci == null) ? null : new ServiceConfig(this, sci));
    }

    /**
     * Returns the organization configuration for the
     * given organization and instance name.
     * 
     * @param orgName
     *            the name of the organization
     * @param instanceName
     *            the service configuration instance name
     * @return the organization configuration for the given organization
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public ServiceConfig getOrganizationConfig(String orgName,
            String instanceName) throws SMSException, SSOException {
        // Get ServiceConfigImpl
        validateSCM();
        ServiceConfigImpl sci = scm.getOrganizationConfig(token, orgName,
                instanceName);
        return ((sci == null) ? null : new ServiceConfig(this, sci));
    }

    /**
     * Creates global configuration for the default
     * instance of the service given the configuration attributes.
     * 
     * @param attrs
     *            map of attribute values.
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public ServiceConfig createGlobalConfig(Map attrs) throws SMSException,
            SSOException {
        validateSSM();
        ServiceSchemaImpl ss = ssm.getSchema(SchemaType.GLOBAL);
        if (ss == null) {
            String[] args = { serviceName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-service-does-not-have-global-schema", args));
        }
        // Check base nodes for global attributes
        String orgDN = scm.constructServiceConfigDN(SMSUtils.DEFAULT,
                CreateServiceConfig.GLOBAL_CONFIG_NODE, null);

        // Create the sub config entry
        try {
            CreateServiceConfig.createSubConfigEntry(token, orgDN, ss, null,
                    null, attrs, SMSEntry.baseDN);
        } catch (ServiceAlreadyExistsException slee) {
            // Ignore the exception
        }
        return (getGlobalConfig(null));
    }

    /**
     * Creates organization configuration for the default
     * instance of the service given configuration attributes.
     * 
     * @param orgName
     *            name of organization.
     * @param attrs
     *            map of attribute values.
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public ServiceConfig createOrganizationConfig(String orgName, Map attrs)
            throws SMSException, SSOException {
        validateSSM();
        ServiceSchemaImpl ss = ssm.getSchema(SchemaType.ORGANIZATION);
        if (ss == null) {
            String[] args = { serviceName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-service-does-not-have-org-schema", args));
        }
        // Check base nodes for org
        String orgdn = DNMapper.orgNameToDN(orgName);
        CreateServiceConfig.checkBaseNodesForOrg(token, orgdn, serviceName,
                version);
        String orgDN = scm.constructServiceConfigDN(SMSUtils.DEFAULT,
                CreateServiceConfig.ORG_CONFIG_NODE, orgdn);

        // Create the sub config entry
        try {
            CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token, orgDN);
            if (cEntry.isDirty()) {
                cEntry.refresh();
            }
            if (cEntry.isNewEntry()) {
                CreateServiceConfig.createSubConfigEntry(token, orgDN, ss,
                        null, null, attrs, orgName);
                // if in co-existence mode, need to register the service
                // for AMOrganization
                if (ServiceManager.isCoexistenceMode()) {
                    String smsDN = DNMapper.orgNameToDN(orgName);
                    OrgConfigViaAMSDK amsdk = new OrgConfigViaAMSDK(token,
                            DNMapper.realmNameToAMSDKName(smsDN), smsDN);
                    amsdk.assignService(serviceName);
                }
            } else if (attrs != null && !attrs.isEmpty()) {
                // Set the attributes for the service config
                ServiceConfig sc = getOrganizationConfig(orgName, null);
                sc.setAttributes(attrs);
            }
        } catch (ServiceAlreadyExistsException slee) {
            // Ignore the exception
        }

        return (getOrganizationConfig(orgName, null));
    }

    /**
     * Adds instances, global and organization
     * configurations
     * 
     * @param in
     *            input stream of configuration data.
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void addConfiguration(InputStream in) throws SMSException,
            SSOException {
        ServiceManager sm = new ServiceManager(token);
        // Get the document and search for service name and version
        Document doc = SMSSchema.getXMLDocument(in);
        NodeList nodes = doc.getElementsByTagName(SMSUtils.SERVICE);
        for (int i = 0; (nodes != null) && (i < nodes.getLength()); i++) {
            Node serviceNode = nodes.item(i);
            String sName = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.NAME);
            String sVersion = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.VERSION);
            Node configNode;
            if (sName.equals(serviceName)
                    && (sVersion.equals(version))
                    && ((configNode = XMLUtils.getChildNode(serviceNode,
                            SMSUtils.CONFIGURATION)) != null)) {
                CreateServiceConfig.createService(sm, sName, sVersion,
                        configNode, null);
            }
        }
    }

    /**
     * Deletes the global configuration data for the given
     * group name. If group name is <code>null</code>, it used the default
     * group name.
     * 
     * @param groupName
     *            name of group.
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void removeGlobalConfiguration(String groupName)
            throws SMSException, SSOException {

        if (serviceName.equalsIgnoreCase(IdConstants.REPO_SERVICE) ||
            serviceName.equalsIgnoreCase(ISAuthConstants.AUTH_SERVICE_NAME)) {
            String[] args = { serviceName };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-SERVICE_CORE_CANNOT_DELETE", args));
        }
        if ((groupName == null) || groupName.length() == 0) {
            groupName = SMSUtils.DEFAULT;
        }
        // Construct the sub-config dn
        validateSCM();
        String gdn = scm.constructServiceConfigDN(groupName,
                CreateServiceConfig.GLOBAL_CONFIG_NODE, null);
        // Delete the entry
        CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token, gdn);
        if (cEntry.isDirty()) {
            cEntry.refresh();
        }
        SMSEntry entry = cEntry.getClonedSMSEntry();
        entry.delete(token);
        cEntry.refresh(entry);
    }

    /**
     * Deletes the organization configuration data for the
     * given organization. It removes all the groups within the organization.
     * 
     * @param orgName
     *            name of organization.
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void deleteOrganizationConfig(String orgName) throws SMSException,
            SSOException {
        removeOrganizationConfiguration(orgName, SMSUtils.DEFAULT);
    }

    /**
     * Deletes the organization's group configuration
     * data.
     * 
     * @param orgName
     *            name of organization.
     * @param groupName
     *            name of group.
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void removeOrganizationConfiguration(String orgName,
            String groupName) throws SMSException, SSOException {
        removeOrganizationConfiguration(orgName, groupName, true);
    }

    /**
     * Deletes the organization's group configuration data.
     * 
     * @param orgName
     *            name of organization.
     * @param groupName
     *            name of group.
     * @param checkLegacyMode
     *            boolean to check if legacy or realm passed by amsdk as false.
     * @throws SMSException
     *             if an error has occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     */
    public void removeOrganizationConfiguration(String orgName,
            String groupName, boolean checkLegacyMode) throws SMSException,
            SSOException {
        if ((groupName == null) || groupName.length() == 0) {
            groupName = SMSUtils.DEFAULT;
        }
        // Construct the sub-config dn
        String orgdn = DNMapper.orgNameToDN(orgName);
        validateSCM();
        String odn = scm.constructServiceConfigDN(groupName,
                CreateServiceConfig.ORG_CONFIG_NODE, orgdn);
        // Delete the entry from the REALM DIT
        CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token, odn);
        if (cEntry.isNewEntry()) {
            return;
        }
        // if in legacy/co-existence mode, need to unregister the service
        // from AMOrganization
        if (checkLegacyMode && ServiceManager.isCoexistenceMode()
                && groupName.equalsIgnoreCase(SMSUtils.DEFAULT)) {
            OrgConfigViaAMSDK amsdk = new OrgConfigViaAMSDK(token, DNMapper
                    .realmNameToAMSDKName(orgdn), orgdn);
            amsdk.unassignService(serviceName);
        }
        // Now delete the entry.
        if (!cEntry.isNewEntry()) {
            SMSEntry entry = cEntry.getClonedSMSEntry();
            entry.delete(token);
            cEntry.refresh(entry);
        }
    }

    /**
     * Returns a set of plugins configured for the given plugin interface and
     * plugin schema in a organization
     */
    public Set getPluginConfigNames(String pluginSchemaName,
            String interfaceName, String orgName) throws SMSException,
            SSOException {
        StringBuilder sb = new StringBuilder(100);
        sb.append("ou=").append(pluginSchemaName).append(",ou=").append(
                interfaceName).append(",").append(
                CreateServiceConfig.PLUGIN_CONFIG_NODE).append("ou=").append(
                version).append(",").append("ou=").append(serviceName).append(
                ",").append(SMSEntry.SERVICES_RDN).append(",").append(
                DNMapper.orgNameToDN(orgName));
        // Need to check if the user permission to read plugin names
        CachedSMSEntry.getInstance(token, sb.toString());
        // Get the CachedSubEntries and return sub-entries
        CachedSubEntries cse = CachedSubEntries.getInstance(token, sb
                .toString());
        return (cse.getSubEntries(token));
    }

    /**
     * Returns the plugin configuration parameters for the service
     */
    public PluginConfig getPluginConfig(String name, String pluginSchemaName,
            String interfaceName, String orgName) throws SMSException,
            SSOException {
        validateSCM();
        PluginConfigImpl pci = scm.getPluginConfig(token, name,
                pluginSchemaName, interfaceName, orgName);
        return (new PluginConfig(name, this, pci));
    }

    /**
     * Removes the plugin configuration for the service
     */
    public void removePluginConfig(String name, String pluginSchemaName,
            String interfaceName, String orgName) throws SMSException,
            SSOException {
        PluginConfig pci = getPluginConfig(name, pluginSchemaName,
                interfaceName, orgName);
        if (pci != null) {
            pci.delete();
        }
    }

    /**
     * Registers for changes to service's configuration.
     * The object will be called when configuration for this service and version
     * is changed.
     * 
     * @param listener
     *            callback object that will be invoked when schema changes.
     * @return an ID of the registered listener.
     *
     * @supported.api
     */
    public String addListener(ServiceListener listener) {
        try {
            validateSCM();
            return (scm.addListener(token, listener));
        } catch (Exception e) {
            SMSEntry.debug.error("ServiceConfigManager:addListener exception"
                    + " Service Name: " + serviceName, e);
        }
        return (null);
    }

    /**
     * Removes the listener from the service for the given
     * listener ID. The ID was issued when the listener was registered.
     * 
     * @param listenerID
     *            the listener ID issued when the listener was registered
     *
     * @supported.api
     */
    public void removeListener(String listenerID) {
        if (scm != null) {
            scm.removeListener(listenerID);
        }
    }
    
    private void validateSCM() throws SSOException, SMSException {
        if ((scm == null) || !scm.isValid()) {
            scm = ServiceConfigManagerImpl.getInstance(
                token, serviceName, version);
        }
    }
    
    private void validateSSM() throws SSOException, SMSException {
        if ((ssm == null) || !ssm.isValid()) {
            validateSCM();
            ssm = scm.getServiceSchemaManagerImpl(token);
        }
    }

    // @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (this.serviceName != null ?
            this.serviceName.hashCode() : 0);
        hash = 29 * hash + (this.version != null ?
            this.version.hashCode() : 0);
        return hash;
    }

    /**
     * Compares this object with the given object.
     * 
     * @param o
     *            object for comparison.
     * @return true if objects are equals.
     *
     * @supported.api
     */
    public boolean equals(Object o) {
        if (o instanceof ServiceConfigManager) {
            ServiceConfigManager oscm = (ServiceConfigManager) o;
            if (serviceName.equals(oscm.serviceName)
                    && version.equals(oscm.version)) {
                return (true);
            }
        }
        return (false);
    }

    /**
     * Returns String representation of the service's
     * configuration data, along with instances and groups.
     * 
     * @return String representation of the service's configuration data, along
     *         with instances and groups.
     *
     * @supported.api
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nService Config Manager: ").append(serviceName).append(
                "\n\tVersion: ").append(version);

        // Print Instances with global and base DN's org attributes
        try {
            Iterator instances = getInstanceNames().iterator();
            while (instances.hasNext()) {
                String instanceName = (String) instances.next();
                sb.append(getInstance(instanceName));
                ServiceConfig config = null;
                try {
                    config = getGlobalConfig(instanceName);
                    if (config != null) {
                        sb.append("\nGlobal Configuation:\n").append(config);
                    }
                } catch (SMSException e) {
                    // Ignore the exception
                }
                try {
                    config = getOrganizationConfig(null, instanceName);
                    if (config != null) {
                        sb.append("Org Configuation:\n").append(config);
                    }
                } catch (SMSException e) {
                    // Ignore the exception
                }
            }
            sb.append("\n");
        } catch (SMSException smse) {
            sb.append(smse.getMessage());
        } catch (SSOException ssoe) {
            sb.append(ssoe.getMessage());
        }
        return (sb.toString());
    }
    
    public String toXML(AMEncryption encryptObj) 
        throws SMSException, SSOException {
        StringBuilder buff = new StringBuilder();
        buff.append("<" + SMSUtils.CONFIGURATION + ">");

        Set instances = getInstanceNames();
        
        for (Iterator i = instances.iterator(); i.hasNext(); ) {
            String instanceName = (String)i.next();
            ServiceInstance instance = getInstance(instanceName);
            buff.append(instance.toXML());
        }

        /*
         * Before calling the Global configuration we need to add "default" to
         * the set of instances as getInstances method does not return this.
         */
        instances.add(SMSUtils.DEFAULT);
        
        for (Iterator i = instances.iterator(); i.hasNext(); ) {
            String instanceName = (String)i.next();
            try {
                ServiceConfig sc = getGlobalConfig(instanceName);
                if (sc != null) {
                    buff.append(sc.toXML(SMSUtils.GLOBAL_CONFIG, encryptObj));
                }
            } catch (SMSException e) {
                //ignored
            }
        }

        OrganizationConfigManager orgMgr = new OrganizationConfigManager(
            token, "/");
        Set orgNames = new HashSet();
        Set oNames = orgMgr.getSubOrganizationNames("*", true);
        
        for (Iterator i = oNames.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            if (!name.startsWith("/")) {
                name = "/" + name;
            }
            orgNames.add(name);
        }
        
        orgNames.add("/");
        
        /*
         * we hide the hidden realm that is used for storing the configuration
         * data. Add it accordingly.
         */
        orgNames.add(com.sun.identity.policy.PolicyManager.DELEGATION_REALM); 
        
        for (Iterator i = orgNames.iterator(); i.hasNext(); ) {
            String orgName = (String)i.next();
            for (Iterator j = instances.iterator(); j.hasNext(); ) {
                String instanceName = (String)j.next();
                try {
                    ServiceConfig sc = getOrganizationConfig(
                        orgName, instanceName);
                    if (sc != null) {
                        buff.append(sc.toXML(
                            SMSUtils.ORG_CONFIG, encryptObj, orgName));
                    }
                } catch (SMSException e) {
                    //ignored
                }
            }
        }
        
        buff.append("</" + SMSUtils.CONFIGURATION + ">");
        return buff.toString();
    }

    // ---------------------------------------------------------
    // Protected method
    // ---------------------------------------------------------
    SSOToken getSSOToken() {
        return (token);
    }

    boolean containsGroup(String groupName) throws SMSException, SSOException {
        return (scm.containsGroup(token, groupName));
    }
}
