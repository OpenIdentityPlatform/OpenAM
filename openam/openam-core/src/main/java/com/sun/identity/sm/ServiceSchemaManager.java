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
 * $Id: ServiceSchemaManager.java,v 1.12 2009/07/25 05:11:55 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.sm;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The class <code>ServiceSchemaManager</code> provides interfaces to manage
 * the service's schema. It provides access to <code>ServiceSchema</code>,
 * which represents a single "schema" in the service.
 *
 * @supported.api
 */
public class ServiceSchemaManager {
    
    private SSOToken token;
    
    private String serviceName;
    
    private String version;
    
    private ServiceSchemaManagerImpl ssm;
    
    private static Debug debug = Debug.getInstance("amSMS");
    
    /**
     * Constructor for service's schema manager to manage the attributes and
     * sub configurations. Assumes service version number to be <class>1.0
     * </class>.
     *
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     */
    public ServiceSchemaManager(String serviceName, SSOToken token)
    throws SMSException, SSOException {
        this(token, serviceName, ServiceManager.isCoexistenceMode() ?
            ServiceManager.serviceDefaultVersion(token, serviceName) :
            ServiceManager.getVersion(serviceName));
    }
    
    /**
     * Creates an instance of
     * <code>ServiceSchemaManager</code> for the given service and version
     * pair. It requires an user identity, that will used to perform operations
     * with. It is assumed that the application calling this constructor should
     * authenticate the user.
     *
     * @param token
     *            single sign on token of the user identity on whose behalf the
     *            operations are performed.
     * @param serviceName
     *            the name of the service.
     * @param version
     *            the version of the service.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     *
     * @supported.api
     */
    public ServiceSchemaManager(SSOToken token, String serviceName,
        String version) throws SMSException, SSOException {
        if (token == null || serviceName == null || version == null) {
            throw new IllegalArgumentException(SMSEntry.bundle
                .getString(IUMSConstants.SMS_INVALID_PARAMETERS));
        }
        SMSEntry.validateToken(token);
        this.token = token;
        this.serviceName = serviceName;
        this.version = version;
        ssm = ServiceSchemaManagerImpl.getInstance(token, serviceName, version);
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
     * Returns the version of the service.
     *
     * @return the version of the service
     *
     * @supported.api
     */
    public String getVersion() {
        return (version);
    }
    
    /**
     * Returns the I18N properties file name for the
     * service.
     *
     * @return the I18N properties file name for the service
     *
     * @supported.api
     */
    public String getI18NFileName() {
        validate();
        return (ssm.getI18NFileName());
    }
    
    /**
     * Sets the I18N properties file name for the service
     *
     * @param url
     *            properties file name
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void setI18NFileName(String url) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        String tmpS = ssm.getI18NFileName();
        ssm.setI18NFileName(url);
        try {
            replaceSchema(ssm.getDocument());
        } catch (SMSException se) {
            ssm.setI18NFileName(tmpS);
            throw se;
        }
    }
    
    /**
     * Returns the URL of the JAR file that contains the
     * I18N properties file. The method could return null, in which case the
     * properties file should be in <code>CLASSPATH</code>.
     *
     * @return the URL of the JAR file containing the <code>I18N</code>
     *         properties file.
     *
     * @supported.api
     */
    public String getI18NJarURL() {
        validate();
        return (ssm.getI18NJarURL());
    }
    
    /**
     * Sets the URL of the JAR file that contains the I18N
     * properties
     *
     * @param url
     *            URL
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     *
     * @supported.api
     */
    
    public void setI18NJarURL(String url) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        String tmpS = ssm.getI18NJarURL();
        ssm.setI18NJarURL(url);
        try {
            replaceSchema(ssm.getDocument());
        } catch (SMSException se) {
            ssm.setI18NJarURL(tmpS);
            throw se;
        }
    }
    
    /**
     * Returns the service's hierarchy.
     *
     * @return service hierarchy in slash format.
     *
     * @supported.api
     */
    public String getServiceHierarchy() {
        validate();
        return (ssm.getServiceHierarchy());
    }
    
    /**
     * Sets the service's hierarchy
     *
     * @param newhierarchy
     *            service hierarchy
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void setServiceHierarchy(String newhierarchy) throws SMSException,
        SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        String tmpS = getServiceHierarchy();
        ssm.setServiceHierarchy(newhierarchy);
        try {
            replaceSchema(ssm.getDocument());
        } catch (SMSException e) {
            ssm.setServiceHierarchy(tmpS);
            throw e;
        }
    }
    
    /**
     * Returns i18nKey of the schema.
     *
     * @return i18nKey of the schema.
     *
     * @supported.api
     */
    public String getI18NKey() {
        validate();
        return (ssm.getI18NKey());
    }
    
    /**
     * Sets the i18nKey of the schema.
     *
     * @param i18nKey
     *            <code>i18nKey</code> of the schema.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation.
     * @throws SSOException
     *             if the single sign on token is invalid or expired.
     *
     * @supported.api
     */
    public void setI18NKey(String i18nKey) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        String tmp = ssm.getI18NKey();
        ssm.setI18NKey(i18nKey);
        
        try {
            replaceSchema(ssm.getDocument());
        } catch (SMSException e) {
            ssm.setI18NKey(tmp);
            throw e;
        }
    }
    
    /**
     * Returns URL of the view bean for the service
     *
     * @return URL for view bean
     *
     * @supported.api
     */
    public String getPropertiesViewBeanURL() {
        validate();
        return (ssm.getPropertiesViewBeanURL());
    }
    
    /**
     * Sets the URL of the view bean for the service.
     *
     * @param url
     *            of the view bean for the service.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation.
     * @throws SSOException
     *             if the single sign on token is invalid or expired.
     *
     * @supported.api
     */
    public void setPropertiesViewBeanURL(String url) throws SMSException,
        SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        String tmpS = ssm.getPropertiesViewBeanURL();
        ssm.setPropertiesViewBeanURL(url);
        try {
            replaceSchema(ssm.getDocument());
        } catch (SMSException e) {
            ssm.setPropertiesViewBeanURL(tmpS);
            throw e;
        }
    }
    
    /**
     * iPlanet_PUBLIC-METHOD Returns the revision number of the service schema.
     *
     * @return the revision number of the service schema
     */
    public int getRevisionNumber() {
        validate();
        return (ssm.getRevisionNumber());
    }
    
    /**
     * iPlanet_PUBLIC-METHOD Sets the revision number for the service schema.
     *
     * @param revisionNumber
     *            revision number of the service schema.
     * @throws SMSException
     *             if there is a problem setting the value in the data store.
     * @throws SSOException
     *             If the user has an invalid SSO token.
     */
    public void setRevisionNumber(int revisionNumber) throws SMSException,
        SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        int tmpS = ssm.getRevisionNumber();
        ssm.setRevisionNumber(revisionNumber);
        try {
            replaceSchema(ssm.getDocument());
        } catch (SMSException e) {
            ssm.setRevisionNumber(tmpS);
            throw (e);
        }
    }
    
    /**
     * Returns the schema types available with this
     * service.
     *
     * @return set of <code>SchemaTypes</code> in this service.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public Set getSchemaTypes() throws SMSException {
        SMSEntry.validateToken(token);
        validate();
        return (ssm.getSchemaTypes());
    }
    
    /**
     * Returns the configuration schema for the given
     * schema type
     *
     * @param type
     *            schema type.
     * @return service schema.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public ServiceSchema getSchema(String type) throws SMSException {
        validate();
        SchemaType t = null;
        if (type.equalsIgnoreCase("role")
        || type.equalsIgnoreCase("filteredrole")
        || type.equalsIgnoreCase("realm")) {
            t = SchemaType.DYNAMIC;
        } else if (type.equalsIgnoreCase("user")) {
            t = SchemaType.USER;
        } else {
            t = new SchemaType(type);
        }
        return (getSchema(t));
    }
    
    /**
     * Returns the configuration schema for the given
     * schema type
     *
     * @param type
     *            schema type.
     * @return service schema.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public ServiceSchema getSchema(SchemaType type) throws SMSException {
        SMSEntry.validateToken(token);
        validate();
        ServiceSchemaImpl ss = ssm.getSchema(type);
        if ((ss == null) && type.equals(SchemaType.USER)) {
            type = SchemaType.DYNAMIC;
            ss = ssm.getSchema(type);
        }
        if (ss != null) {
            return (new ServiceSchema(ss, "", type, this));
        }
        return (null);
    }
    
    /**
     * Returns the organization creation configuration schema if present; else
     * returns <code>null</code>
     *
     * @return service schema.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     */
    public ServiceSchema getOrganizationCreationSchema() throws SMSException {
        SMSEntry.validateToken(token);
        validate();
        ServiceSchemaImpl ss = ssm.getSchema(SchemaType.ORGANIZATION);
        if (ss != null) {
            ServiceSchemaImpl ssi = ss.getOrgAttrSchema();
            if (ssi != null) {
                return (new ServiceSchema(ssi, "", SchemaType.ORGANIZATION,
                    this, true));
            }
        }
        return (null);
    }
    
    /**
     * Returns the attribute schemas for the given schema
     * type excluding status and service identifier attributes.
     *
     * @param type
     *            schema type.
     * @return service schema.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public Set getServiceAttributeNames(SchemaType type) throws SMSException {
        SMSEntry.validateToken(token);
        validate();
        ServiceSchema ss = getSchema(type);
        return (ss.getServiceAttributeNames());
    }
    
    /**
     * Returns the global service configuration schema.
     *
     * @return the global service configuration schema
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public ServiceSchema getGlobalSchema() throws SMSException {
        return (getSchema(SchemaType.GLOBAL));
    }
    
    /**
     * Returns the organization service configuration
     * schema.
     *
     * @return the organization service configuration schema
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public ServiceSchema getOrganizationSchema() throws SMSException {
        return (getSchema(SchemaType.ORGANIZATION));
    }
    
    /**
     * Returns the dynamic service configuration schema.
     *
     * @return the dynamic service configuration schema
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public ServiceSchema getDynamicSchema() throws SMSException {
        return (getSchema(SchemaType.DYNAMIC));
    }
    
    /**
     * Returns the user service configuration schema.
     *
     * @return the user service configuration schema
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public ServiceSchema getUserSchema() throws SMSException {
        return (getSchema(SchemaType.USER));
    }
    
    /**
     * Returns the policy service configuration schema.
     *
     * @return the policy service configuration schema
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public ServiceSchema getPolicySchema() throws SMSException {
        return (getSchema(SchemaType.POLICY));
    }
    
    /**
     * Returns the service schema in XML for this service.
     *
     * @return the service schema in XML for this service
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     *
     * @supported.api
     */
    public InputStream getSchema() throws SMSException {
        SMSEntry.validateToken(token);
        validate();
        return (ssm.getSchema());
    }
    
    /**
     * Replaces the existing service schema with the given
     * schema defined by the XML input stream that follows the SMS DTD.
     *
     * @param xmlServiceSchema
     *            the XML format of the service schema
     * @throws SMSException
     *             if an error occurred while trying to perform the operation
     * @throws SSOException
     *             if the single sign on token is invalid or expired
     * @throws IOException
     *             if an error occurred with the <code> InputStream </code>
     *
     * @supported.api
     */
    public void replaceSchema(InputStream xmlServiceSchema)
    throws SSOException, SMSException, IOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        CachedSMSEntry smsEntry = ssm.getCachedSMSEntry();
        smsEntry.writeXMLSchema(token, xmlServiceSchema);
    }

    // @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (serviceName != null ? serviceName.hashCode() : 0);
        hash = 67 * hash + (version != null ? version.hashCode() : 0);
        return hash;
    }
    
    /**
     * Returns true if the given object equals this
     * object.
     *
     * @param o
     *            object for comparison.
     * @return true if the given object equals this object.
     *
     * @supported.api
     */
    public boolean equals(Object o) {
        if (o instanceof ServiceSchemaManager) {
            ServiceSchemaManager ossm = (ServiceSchemaManager) o;
            if (serviceName.equals(ossm.serviceName)
            && version.equals(ossm.version)) {
                return (true);
            }
        }
        return (false);
    }
    
    /**
     * Returns the string representation of the Service
     * Schema.
     *
     * @return the string representation of the Service Schema.
     *
     * @supported.api
     */
    public String toString() {
    	validate();
    	return (ssm.toString());
    }
    
    /**
     * Registers for changes to service's schema. The
     * object will be called when schema for this service and version is
     * changed.
     *
     * @param listener
     *            callback object that will be invoked when schema changes.
     * @return an ID of the registered listener.
     *
     * @supported.api
     */
    public String addListener(ServiceListener listener) {
    	validate();
    	return (ssm.addListener(listener));
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
        if (ssm !=null ) {
    	    ssm.removeListener(listenerID);
        }
    }
    
    /**
     * Returns the last modified time stamp of this service schema. This method
     * is expensive because it does not cache the modified time stamp but goes
     * directly to the data store to obtain the value of this entry
     *
     * @return The last modified time stamp as a string with the format of
     *         <code>yyyyMMddhhmmss</code>
     * @throws SMSException if there is an error trying to read from the
     *         datastore.
     * @throws SSOException if the single sign-on token of the user is invalid.
     */
    public String getLastModifiedTime() throws SMSException, SSOException {
        validateServiceSchemaManagerImpl();
        CachedSMSEntry ce = ssm.getCachedSMSEntry();
        if (ce.isDirty()) {
            ce.refresh();
        }
        SMSEntry e = ce.getSMSEntry();
        String vals[] = e.getAttributeValues(SMSEntry.ATTR_MODIFY_TIMESTAMP,
            true);
        String mTS = null;
        if (vals != null) {
            mTS = vals[0];
        }
        return mTS;
    }
    
    // ================= Plugin Interface Methods ========
    
    /**
     * Returns the names of the plugin interfaces used by the service
     *
     * @return service's plugin interface names
     */
    public Set getPluginInterfaceNames() {
        validate();
        return (ssm.getPluginInterfaceNames());
    }
    
    /**
     * Returns the <code>PluginInterface</code> object of the service for the
     * specified plugin interface name
     *
     * @param pluginInterfaceName
     *            name of the plugin interface
     * @return plugin interface configured for the service; else
     *         <code>null</code>
     */
    public PluginInterface getPluginInterface(String pluginInterfaceName) {
        validate();
        return (ssm.getPluginInterface(pluginInterfaceName));
    }
    
    /**
     * Adds a new plugin interface objct to service's schema.
     *
     * @param interfaceName
     *            name for the plugin interface
     * @param interfaceClass
     *            fully qualified interface class name
     * @param i18nKey
     *            I18N key that will by used by UI to get messages to display
     *            the interface name
     */
    public void addPluginInterface(String interfaceName, String interfaceClass,
        String i18nKey) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        if ((interfaceName == null) || (interfaceClass == null)) {
            throw (new IllegalArgumentException());
        }
        StringBuilder sb = new StringBuilder(100);
        sb.append("<").append(SMSUtils.PLUGIN_INTERFACE).append(" ").append(
            SMSUtils.NAME).append("=\"").append(interfaceName)
            .append("\" ").append(SMSUtils.PLUGIN_INTERFACE_CLASS).append(
            "=\"").append(interfaceClass).append("\"");
        if (i18nKey != null) {
            sb.append(" ").append(SMSUtils.I18N_KEY).append("=\"").append(
                i18nKey).append("\"");
        }
        sb.append("></").append(SMSUtils.PLUGIN_INTERFACE).append(">");
        // Construct XML document
        Document pluginDoc = SMSSchema.getXMLDocument(sb.toString(), false);
        Node node = XMLUtils.getRootNode(pluginDoc, SMSUtils.PLUGIN_INTERFACE);
        
        // Added to XML document and write it
        Document schemaDoc = ssm.getDocumentCopy();
        Node pluginNode = schemaDoc.importNode(node, true);
        Node schemaNode = XMLUtils.getRootNode(schemaDoc, SMSUtils.SCHEMA);
        schemaNode.appendChild(pluginNode);
        replaceSchema(schemaDoc);
    }
    
    /**
     * Removes the plugin interface object from the service schema.
     *
     * @param interfacename Name of the plugin class.
     */
    public void removePluginInterface(String interfacename)
    throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        validateServiceSchemaManagerImpl();
        Document schemaDoc = ssm.getDocumentCopy();
        Node schemaNode = XMLUtils.getRootNode(schemaDoc, SMSUtils.SCHEMA);
        // Get the plugin interface node
        Node pluginNode = XMLUtils.getNamedChildNode(schemaNode,
            SMSUtils.PLUGIN_INTERFACE, SMSUtils.NAME, interfacename);
        if (pluginNode != null) {
            schemaNode.removeChild(pluginNode);
            replaceSchema(schemaDoc);
        }
    }
    
    // -----------------------------------------------------------
    // Plugin Schema
    // -----------------------------------------------------------
    /**
     * Returns the names of plugins configured for the plugin interface. If
     * organization is <code>null</code>, returns the plugins configured for
     * the "root" organization.
     */
    public Set getPluginSchemaNames(String interfaceName, String orgName)
    throws SMSException {
        SMSEntry.validateToken(token);
        validate();
        // Construct the DN to get CachedSubEntries
        StringBuilder sb = new StringBuilder(100);
        sb.append("ou=").append(interfaceName).append(",").append(
            CreateServiceConfig.PLUGIN_CONFIG_NODE).append("ou=").append(
            version).append(",").append("ou=").append(serviceName).append(
            ",").append(SMSEntry.SERVICES_RDN).append(",").append(
            DNMapper.orgNameToDN(orgName));
        CachedSubEntries cse = CachedSubEntries.getInstance(token, sb
            .toString());
        try {
            return (cse.getSubEntries(token));
        } catch (SSOException s) {
            debug.error("ServiceSchemaManager: Unable to get "
                + "Plugin Schema Names", s);
        }
        return (Collections.EMPTY_SET);
    }
    
    /**
     * Returns the PluginSchema object given the schema name and the interface
     * name for the specified organization. If organization is
     * <code>null</code>, returns the PluginSchema for the "root" organization.
     */
    public PluginSchema getPluginSchema(String pluginSchemaName,
        String interfaceName, String orgName) throws SMSException {
        SMSEntry.validateToken(token);
        validate();
        return (new PluginSchema(token, serviceName, version, pluginSchemaName,
            interfaceName, orgName));
    }
    
    // -----------------------------------------------------------
    // Internal protected method
    // -----------------------------------------------------------
    SSOToken getSSOToken() {
        return (token);
    }
    
    protected Document getDocumentCopy() throws SMSException {
    	validate();
    	return (ssm.getDocumentCopy());
    }
    
    protected synchronized void replaceSchema(Document document)
    throws SSOException, SMSException {
    	validate();
    	CachedSMSEntry smsEntry = ssm.getCachedSMSEntry();
        SMSSchema smsSchema = new SMSSchema(document);
        smsEntry.writeXMLSchema(token, smsSchema.getSchema());
    }
    
    private void validate() {
        try {
            validateServiceSchemaManagerImpl();
        } catch (SSOException e) {
            // Since method signatures cannot be changed, a runtime
            // exception is thrown. This conditions would happen only
            // when SSOToken has become invalid or service has been
            // removed.
            debug.error("ServiceSchemaManager:validate failed for SN: " +
                serviceName, e);
            throw (new RuntimeException(e.getMessage()));
        } catch (SMSException e) {
            // Ignore the exception
        }
    }
    
    private void validateServiceSchemaManagerImpl()
        throws SMSException, SSOException {
        if (ssm == null || !ssm.isValid()) {
            // Recreate the SSM
            ssm = ServiceSchemaManagerImpl.getInstance(token,
                serviceName, version);
        }
    }
    
    // -----------------------------------------------------------
    // Static method to create a new service schema
    // -----------------------------------------------------------
    static void createService(SSOToken token, SMSSchema smsSchema)
    throws SMSException, SSOException {
        // Service node
        SMSEntry smsEntry = new SMSEntry(token, ServiceManager
            .getServiceNameDN(smsSchema.getServiceName()));
        
        if (smsEntry.isNewEntry()) {
            // create this entry
            smsEntry.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
            smsEntry.addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                SMSEntry.OC_SERVICE);
            smsEntry.save();
        }
        // Version node
        CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token,
            ServiceManager.getServiceNameDN(smsSchema.getServiceName(),
            smsSchema.getServiceVersion()));
        if (cEntry.isDirty()) {
            cEntry.refresh();
        }
        smsEntry = cEntry.getSMSEntry();
        String[] schema = new String[1];
        if ((smsEntry.getAttributeValues(SMSEntry.ATTR_SCHEMA) == null)
        || ((smsEntry.getAttributeValues(SMSEntry.ATTR_SCHEMA))[0]
            .equalsIgnoreCase(SMSSchema.getDummyXML(smsSchema
            .getServiceName(), smsSchema
            .getServiceVersion())))) {
            schema[0] = smsSchema.getSchema();
            smsEntry.setAttribute(SMSEntry.ATTR_SCHEMA, schema);
        } else {
            // Throw service already exists exception
            Object[] args = { smsSchema.getServiceName(),
            smsSchema.getServiceVersion() };
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                IUMSConstants.SMS_service_already_exists, args));
        }
        if (smsEntry.isNewEntry()) {
            // add object classes
            smsEntry.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
            smsEntry.addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                SMSEntry.OC_SERVICE);
        }
        smsEntry.save(token);
        cEntry.refresh(smsEntry);
    }
    
    public String toXML(AMEncryption encryptObj)
        throws SMSException {
    	validate();
    	String xml = ssm.toXML(encryptObj);
        int idx = xml.lastIndexOf("</" + SMSUtils.SERVICE + ">");
        StringBuffer buff = new StringBuffer();
        buff.append(xml.substring(0, idx));

        Set realms = new HashSet();
        realms.add("/");

        for (Iterator i = getPluginInterfaceNames().iterator(); i.hasNext(); ) {
            String iName = (String)i.next();
            getPlugSchemaXML(buff, iName, realms);
        }

        buff.append("</" + SMSUtils.SERVICE + ">");
        return buff.toString();
    }

    private void getPlugSchemaXML(
        StringBuffer buff,
        String interfaceName,
        Set realms
    ) throws SMSException {
        for (Iterator i = realms.iterator(); i.hasNext(); ){
            String realm = (String)i.next();
            Set schemaNames = getPluginSchemaNames(interfaceName, realm);
            for (Iterator j = schemaNames.iterator(); j.hasNext();) {
                String pName = (String)j.next();
                PluginSchema pSchema = getPluginSchema(
                    pName, interfaceName, realm);
                buff.append(pSchema.toXML());
            }
        }
    }
}
