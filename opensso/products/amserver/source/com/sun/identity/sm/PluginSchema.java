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
 * $Id: PluginSchema.java,v 1.7 2008/12/15 21:30:43 goodearth Exp $
 *
 */

/*
 * Portions Copyrighted [2010-2011] [ForgeRock AS]
 */

package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.Iterator;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>PluginSchemaImpl</code> provides the interfaces to obtain
 * the schema information of a plugin that is configured for a service.
 */
public class PluginSchema {

    protected PluginSchemaImpl psi;

    protected SSOToken token;

    protected String serviceName;

    protected String version;

    protected String pluginName;

    private PluginSchema() {
        // Cannot be instantiated
    }

    protected PluginSchema(SSOToken token, String serviceName, String version,
            String pluginName, String iName, String orgName)
            throws SMSException {
        psi = PluginSchemaImpl.getInstance(token, serviceName, version,
                pluginName, iName, orgName);
        if (psi == null) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-plugin-schema-name-not-found", null));
        }
        this.token = token;
        this.serviceName = serviceName;
        this.version = version;
        this.pluginName = pluginName;
    }

    /**
     * Returns the name of the plugin schema
     * 
     * @return name of the plugin
     */
    public String getName() {
        return (pluginName);
    }

    /**
     * Returns the interface name implemented by the plugin
     * 
     * @return plugin interface name
     */
    public String getInterfaceName() {
        validate();
        return (psi.getInterfaceName());
    }

    /**
     * Returns the java class name that implements the interface
     * 
     * @return class name that implements the interface
     */
    public String getClassName() {
        validate();
        return (psi.getClassName());
    }

    /**
     * Returns the URL of the jar file, the contains the complete implementation
     * for the plugin.
     * 
     * @return class name that implements the interface
     */
    public String getJarURL() {
        validate();
        return (psi.getJarURL());
    }

    /**
     * Returns the URL for the plugin's resource bundle
     * 
     * @return URL of the plugin's resource bundle
     */
    public String getI18NJarURL() {
        validate();
        return (psi.getI18NJarURL());
    }

    /**
     * Returns the i18n properties file name
     * 
     * @return i18n properties file name
     */
    public String getI18NFileName() {
        validate();
        return (psi.getI18NFileName());
    }

    /**
     * Returns URL of the view bean for the service
     * 
     * @return URL for view bean
     */
    public String getPropertiesViewBeanURL() {
        validate();
        return (psi.getPropertiesViewBeanURL());
    }

    /**
     * Sets the URL of the view bean for the plugin.
     * 
     * @param url
     *            of the view bean for the plugin.
     * @throws SMSException
     *             if an error occurred while trying to perform the operation.
     * @throws SSOException
     *             if the single sign on token is invalid or expired.
     */
    public void setPropertiesViewBeanURL(String url) throws SMSException,
            SSOException {
        SMSEntry.validateToken(token);

        // FIXME the call to psi.getPropertiesViewBeanURL() needs to be removed
        validatePluginSchema();
        psi.getPropertiesViewBeanURL();

        Document pluginDoc = getDocumentCopy();
        Node pNode = XMLUtils.getRootNode(pluginDoc, SMSUtils.PLUGIN_SCHEMA);
        if (pNode == null) {
            SMSEntry.debug.error("PluginSchema:setPropertiesViewBeanURL "+
                "Invalid plugin interface name. (or/and) \n"+
                "Invalid plugin schema name. " + pNode); 
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                "sms-invalid-plugin-interfaceschema-name", null));
        }
        ((Element) pNode).setAttribute(SMSUtils.PROPERTIES_VIEW_BEAN_URL, url);
        pluginDoc.importNode(pNode, true);

        // Set the psi.viewBeanURL after updating the XML document,
        // to avoid updating the variable (even for a short
        // duration) if the user does not have permissions.
        replacePluginSchema(pluginDoc);
        psi.viewBeanURL = url;
    }

    /**
     * Returns the i18n key to resource bundle that describes the plugin
     * 
     * @return i18n index key to the resource bundle
     */
    public String getI18NKey() {
        validate();
        return (psi.getI18NKey());
    }

    /**
     * Returns the names of the schema attribute defined for the plugin.
     * 
     * @return names of schema attributes defined for the plugin
     */
    public Set getAttributeSchemaNames() {
        validate();
        return (psi.getAttributeSchemaNames());
    }

    /**
     * Returns the schema for an attribute given the name of the attribute,
     * defined for this plugin.
     * 
     * @return schema for the attribute
     * @param attributeSchemaName
     *            name of the schema attribute
     */
    public AttributeSchema getAttributeSchema(String attributeSchemaName) {
        validate();
        AttributeSchemaImpl asi = psi.getAttributeSchema(attributeSchemaName);
        if (asi != null) {
            return (new AttributeSchema(asi, this));
        }
        return (null);
    }

    /**
     * Returns the <code>String</code> represenation of the Plugin schema.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("PluginSchema name: ").append(getName()).
                append("\n\tInterface name: ").append(getInterfaceName()).
                append("\n\tClass name: ").append(getClassName()).
                append("\n\tJar URL: ").append(getJarURL()).
                append("\n\tI18N URL: ").append(getI18NJarURL()).
                append("\n\tI18N File name: ").append(getI18NFileName()).
                append("\n\tI18N Key: ").append(getI18NKey());
        for (Iterator i = getAttributeSchemaNames().iterator(); i.hasNext();) {
            sb.append("\n").append(getAttributeSchema((String) i.next()));
        }
        return (sb.toString());
    }

    // --------------------------------------------------------------
    // Protected methods
    // --------------------------------------------------------------
    protected void validate() {
        try {
            validatePluginSchema();
        } catch (SMSException ex) {
            SMSEntry.debug.error("PluginSchema:validate exception", ex);
        }
    }
    
    protected void validatePluginSchema() throws SMSException {
        if (!psi.isValid()) {
            throw (new SMSException("plugin-schema: " + pluginName +
                " No longer valid. Cache has been cleared. Recreate from" +
                "ServiceSchemaManager"));
        }
    }
    
    protected Document getDocumentCopy() throws SMSException {
        validate();
        return (psi.getDocumentCopy());
    }

    protected Node getPluginSchemaNode(Document doc) throws SMSException {
        NodeList nodes = doc.getElementsByTagName(SMSUtils.PLUGIN_SCHEMA);
        if (nodes == null || (nodes.getLength() == 0)) {
            // Throw an exception
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-plugin-schema-document", null));
        }

        // Search for the plugin schema name
        for (int i = 0; i < nodes.getLength(); i++) {
            if (XMLUtils.getNodeAttributeValue(nodes.item(i), SMSUtils.NAME)
                    .equals(pluginName)) {
                return (nodes.item(i));
            }
        }
        return (null);
    }

    protected void replacePluginSchema(Document document) throws SSOException,
            SMSException {
        // Construct the serialized XML document
        SMSSchema smsSchema = new SMSSchema(document);
        String pSchema = smsSchema.getPluginSchema(pluginName);
        String[] attrs = { pSchema };

        // Get the cached SMSEntry, save and refresh
        validatePluginSchema();
        CachedSMSEntry smsEntry = psi.getCachedSMSEntry();
        SMSEntry e = smsEntry.getClonedSMSEntry();
        e.setAttribute(SMSEntry.ATTR_PLUGIN_SCHEMA, attrs);
        e.save(token);
        smsEntry.refresh(e);
    }

    // --------------------------------------------------------------
    // Protected static method
    // --------------------------------------------------------------
    static void createPluginSchema(SSOToken token, Node node,
            SMSSchema smsSchema) throws SMSException, SSOException {
        String name = XMLUtils.getNodeAttributeValue(node, SMSUtils.NAME);
        String interfaceName = XMLUtils.getNodeAttributeValue(node,
                SMSUtils.PLUGIN_SCHEMA_INT_NAME);
        String orgName = DNMapper.orgNameToDN(XMLUtils.getNodeAttributeValue(
                node, SMSUtils.PLUGIN_SCHEMA_ORG_NAME));

        // Check if the interface name is valid
        ServiceSchemaManagerImpl ssmi = ServiceSchemaManagerImpl.getInstance(
                token, smsSchema.getServiceName(), smsSchema
                        .getServiceVersion());
        if (!ssmi.getPluginInterfaceNames().contains(interfaceName)) {
            throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-plugin-interface-name", null));
        }

        // create required intermediate nodes
        CreateServiceConfig.checkBaseNodesForOrg(token, orgName, smsSchema
                .getServiceName(), smsSchema.getServiceVersion());

        // Construct the DN for the interfaces node
        StringBuilder sb = new StringBuilder(100);
        sb.append("ou=").append(interfaceName).append(",").append(
                CreateServiceConfig.PLUGIN_CONFIG_NODE).append("ou=").append(
                smsSchema.getServiceVersion()).append(",").append("ou=")
                .append(smsSchema.getServiceName()).append(",").append(
                        SMSEntry.SERVICES_RDN).append(",").append(orgName);
        CreateServiceConfig.checkAndCreateOrgUnitNode(token, sb.toString());

        // Construct DN for plugin schema node
        String dn = "ou=" + name + "," + sb.toString();
        CachedSMSEntry ce = CachedSMSEntry.getInstance(token, dn);
        if (ce.isDirty()) {
            ce.refresh();
        }
        SMSEntry e = ce.getClonedSMSEntry();
        if (!e.isNewEntry()) {
            throw (new SMSException("plugin-schema-already-exists",
                    "plugin-schema-already-exists"));
        }

        // Add object classes and attributes
        e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
        e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_SERVICE);
        e.addAttribute(SMSEntry.ATTR_PLUGIN_SCHEMA, smsSchema
                .getPluginSchema(node));
        e.save(token);
        ce.refresh(e);
    }

    public String toXML()
        throws SMSException {
        Document pluginDoc = getDocumentCopy();
        return SMSSchema.nodeToString(XMLUtils.getRootNode(
            pluginDoc, SMSUtils.PLUGIN_SCHEMA));
    }
}
