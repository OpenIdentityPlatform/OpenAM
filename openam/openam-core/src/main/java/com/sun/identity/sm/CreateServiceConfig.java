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
 * $Id: CreateServiceConfig.java,v 1.14 2009/01/28 05:35:03 ww203982 Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm;

import com.iplanet.services.util.AMEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.xml.XMLUtils;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.util.DN;
import org.w3c.dom.Node;

public class CreateServiceConfig {

    static final String GLOBAL_CONFIG_NODE = "ou=GlobalConfig,";

    static final String ORG_CONFIG_NODE = "ou=OrganizationConfig,";

    static final String PLUGIN_CONFIG_NODE = "ou=PluginConfig,";

    static final String INSTANCES_NODE = "ou=Instances,";

    // ----------------------------------------------------------
    // Protected methods
    // ----------------------------------------------------------
    static void createService(
        ServiceManager sm,
        String sName,
        String version,
        Node configNode,
        AMEncryption decryptObj
    ) throws SMSException, SSOException {
        createService(sm, sName, version, configNode, false, decryptObj);
    }

    static void createService(
        ServiceManager sm,
        String sName,
        String version,
        Node configNode,
        boolean createRealms,
        AMEncryption decryptObj
    ) throws SMSException, SSOException {
        // Make sure schema exists for the given service & version
        SSOToken token = sm.getSSOToken();
        ServiceSchemaManagerImpl ssm = ServiceSchemaManagerImpl.getInstance(
                token, sName, version);

        // Construct the base DN
        String baseDN = ServiceManager.getServiceNameDN(sName, version);
        checkBaseNodes(token, baseDN);

        // Check for instance nodes
        Iterator insNodes = XMLUtils.getChildNodes(configNode,
                SMSUtils.INSTANCE).iterator();
        while (insNodes.hasNext()) {
            Node insNode = (Node) insNodes.next();
            String insName = XMLUtils.getNodeAttributeValue(insNode,
                    SMSUtils.NAME);
            if (insName == null) {
                insName = SMSUtils.DEFAULT;
            }
            String insGroup = XMLUtils.getNodeAttributeValue(insNode,
                    SMSUtils.GROUP);
            if (insGroup == null) {
                insGroup = SMSUtils.DEFAULT;
            }
            String insUri = XMLUtils.getNodeAttributeValue(insNode,
                    SMSUtils.URI);
            // Get Attribute Value Pairs, if any
            Map insAttrs = getAttributeValuePairs(insNode);

            StringBuilder sb = new StringBuilder(100);
            sb.append("ou=").append(insName).append(",").append(INSTANCES_NODE)
                    .append(baseDN);
            CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token, sb
                    .toString());
            if (cEntry.isDirty()) {
                cEntry.refresh();
            }
            SMSEntry insEntry = cEntry.getSMSEntry();
            if (insEntry.isNewEntry()) {
                // create the entry
                insEntry = cEntry.getClonedSMSEntry();
                insEntry.addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                        SMSEntry.OC_TOP);
                insEntry.addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                        SMSEntry.OC_SERVICE_COMP);
                insEntry.addAttribute(SMSEntry.ATTR_SERVICE_ID, insGroup);
                if (insUri != null) {
                    insEntry.addAttribute(SMSEntry.ATTR_LABELED_URI, insUri);
                }
                if (insAttrs != null) {
                    SMSUtils.setAttributeValuePairs(insEntry, insAttrs,
                        Collections.EMPTY_SET);
                }
                insEntry.save(token);
                cEntry.refresh(insEntry);
                updateSubEntriesNode(token, insEntry.getDN());
            } else {
                // throw instance already exists exception
                Object[] args = { sName, version };
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.SMS_service_already_exists, args));
            }
        }

        // Process global configuration
        Iterator globalNodes = XMLUtils.getChildNodes(configNode,
                SMSUtils.GLOBAL_CONFIG).iterator();
        while (globalNodes.hasNext()) {
            Node globalNode = (Node) globalNodes.next();
            ServiceSchemaImpl ss = ssm.getSchema(SchemaType.GLOBAL);
            String globalGroup = XMLUtils.getNodeAttributeValue(globalNode,
                    SMSUtils.GROUP);
            if (globalGroup == null) {
                globalGroup = SMSUtils.DEFAULT;
            }
            StringBuilder sb = new StringBuilder(100);
            sb.append("ou=").append(globalGroup).append(",").append(
                    GLOBAL_CONFIG_NODE).append(baseDN);
            createSubConfig(token, sb.toString(), globalNode, ss, baseDN, 
                decryptObj);
        }

        // Process organization configuration
        Iterator orgNodes = XMLUtils.getChildNodes(configNode,
                SMSUtils.ORG_CONFIG).iterator();
        while (orgNodes.hasNext()) {
            Node orgNode = (Node) orgNodes.next();
            ServiceSchemaImpl ss = ssm.getSchema(SchemaType.ORGANIZATION);
            String orgGroup = XMLUtils.getNodeAttributeValue(orgNode,
                    SMSUtils.GROUP);
            if (orgGroup == null) {
                orgGroup = SMSUtils.DEFAULT;
            }
            // Construct the org name
            String orgDN = SMSEntry.baseDN;
            String orgName = XMLUtils.getNodeAttributeValue(orgNode,
                    SMSUtils.NAME);
            if (orgName != null) {
                if (DN.isDN(orgName)) {
                    orgDN = orgName;
                } else if (orgName.indexOf('/') != -1) {
                    orgDN = DNMapper.orgNameToDN(orgName);
                }
             }
            // Check if config nodes exists
            checkBaseNodesForOrg(token, orgDN, sName, version, createRealms);

            // create sub-config node
            StringBuilder sb = new StringBuilder(100);
            sb.append("ou=").append(orgGroup).append(",").append(
                    ORG_CONFIG_NODE).append("ou=").append(version).append(
                    ",ou=").append(sName).append(",ou=services,").append(orgDN);
            createSubConfig(token, sb.toString(), orgNode, ss, orgDN, 
                decryptObj);

            // Process OrganizationAttributeValuePairs
            Node orgAttrValuePairNode = XMLUtils.getChildNode(orgNode,
                SMSUtils.ORG_ATTRIBUTE_VALUE_PAIR);
            if (orgAttrValuePairNode != null) {
                // Get the attributes
                Map attrs = getAttributeValuePairs(orgAttrValuePairNode);
                OrganizationConfigManager ocm = new
                    OrganizationConfigManager(token, orgDN);
                ocm.setAttributes(sName, attrs);
            }
        }

        // Process Plugin configuration
        Iterator pNodes = XMLUtils.getChildNodes(configNode,
                SMSUtils.PLUGIN_CONFIG).iterator();
        while (pNodes.hasNext()) {
            Node pNode = (Node) pNodes.next();
            String pName = XMLUtils.getNodeAttributeValue(pNode, SMSUtils.NAME);
            String schemaName = XMLUtils.getNodeAttributeValue(pNode,
                    SMSUtils.PLUGIN_CONFIG_SCHEMA_NAME);
            String intName = XMLUtils.getNodeAttributeValue(pNode,
                    SMSUtils.PLUGIN_CONFIG_INT_NAME);
            String orgName = DNMapper.orgNameToDN(XMLUtils
                    .getNodeAttributeValue(pNode,
                            SMSUtils.PLUGIN_CONFIG_ORG_NAME));
            // Get the PluginSchema
            PluginSchemaImpl psi = PluginSchemaImpl.getInstance(token, sName,
                    version, schemaName, intName, orgName);

            // Check if config nodes exists
            checkBaseNodesForOrg(token, orgName, sName, version);

            // Check and create interfaces node
            StringBuilder sb = new StringBuilder(100);
            sb.append("ou=").append(intName).append(",").append(
                    PLUGIN_CONFIG_NODE).append("ou=").append(version).append(
                    ",ou=").append(sName).append(",ou=services,").append(
                    orgName);
            checkAndCreateServiceNode(token, sb.toString());
            // Check and create schema node
            sb.insert(0, ",").insert(0, schemaName).insert(0, "ou=");
            checkAndCreateServiceNode(token, sb.toString());
            // Create plugin config node
            sb.insert(0, ",").insert(0, pName).insert(0, "ou=");
            createSubConfig(token, sb.toString(), pNode, psi, orgName, 
                decryptObj);
        }
    }

    static void createSubConfig(
        SSOToken token, 
        String dn, 
        Node node,
        ServiceSchemaImpl ss,
        String orgdn,
        AMEncryption decryptObj
    ) throws SMSException, SSOException {
        // Get service id and priority
        String id = XMLUtils.getNodeAttributeValue(node, SMSUtils.SERVICE_ID);
        String priority = XMLUtils.getNodeAttributeValue(node,
                SMSUtils.PRIORITY);
        
        // Get the attributes
        Map attrs = getAttributeValuePairs(node);
        if ((decryptObj != null) && (attrs != null) && !attrs.isEmpty()) {
            for (Iterator i = attrs.keySet().iterator(); i.hasNext(); ) {
                String attrName = (String)i.next();
                AttributeSchemaImpl as = ss.getAttributeSchema(attrName);
                AttributeSchema.Syntax syntax = as.getSyntax();
                
                if (syntax.equals(AttributeSchema.Syntax.ENCRYPTED_PASSWORD) ||
                    syntax.equals(AttributeSchema.Syntax.PASSWORD)
                ) {
                    Set values = (Set) attrs.get(attrName);
                    if ((values != null) && !values.isEmpty()) {
                        Set decoded = new HashSet(values.size() * 2);
                        for (Iterator j = values.iterator(); j.hasNext();) {
                            decoded.add(AccessController.doPrivileged(
                                new DecodeAction((String) j.next(), 
                                decryptObj)));
                        }
                        attrs.put(attrName, decoded);
                    }
                }
            }
        }

        // Create the LDAP entry
        createSubConfigEntry(token, dn, ss, id, priority, attrs, orgdn);

        // Check for further sub-configuration
        Iterator subConfigs = XMLUtils.getChildNodes(node, SMSUtils.SUB_CONFIG)
                .iterator();
        while (subConfigs.hasNext()) {
            Node subConfigNode = (Node) subConfigs.next();
            String subConfigName = XMLUtils.getNodeAttributeValue(
                    subConfigNode, SMSUtils.NAME);
            String subConfigID = XMLUtils.getNodeAttributeValue(subConfigNode,
                    SMSUtils.SERVICE_ID);
            if (subConfigID == null) {
                subConfigID = subConfigName;
            }
            createSubConfig(token, ("ou=" + subConfigName + "," + dn),
                subConfigNode, ss.getSubSchema(subConfigID), orgdn, decryptObj);
        }
    }

    static void createSubConfigEntry(SSOToken token, String dn,
            ServiceSchemaImpl ss, String id, String priority, Map attrs,
            String orgDN) throws SMSException, SSOException {
        // Construct the SMSEntry for the node
        CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token, dn);
        if (cEntry.isDirty()) {
            cEntry.refresh();
        }
        SMSEntry entry = cEntry.getClonedSMSEntry();
        if ((ss == null) || !entry.isNewEntry()) {
            SMSEntry.debug.error(
            "CreateServiceConfig.createSubConfigEntry: Entry already exists: " +
                dn);
            throw (new ServiceAlreadyExistsException(
                    IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_service_already_exists_no_args, null));
        }

        // Add LDAP objectclasses
        entry.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
        entry.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_SERVICE_COMP);

        if (attrs != null) {
            // Validate the attributes
            ss.validateAttributes(token, attrs, true, orgDN);
            SMSUtils.setAttributeValuePairs(entry, attrs, ss
                    .getSearchableAttributeNames());
        }

        if (id != null) {
            entry.addAttribute(SMSEntry.ATTR_SERVICE_ID, id);
        }

        if (priority != null) {
            entry.addAttribute(SMSEntry.ATTR_PRIORITY, priority);
        }

        // Save the entry, and add to cache
        entry.save(token);
        cEntry.refresh(entry);
        updateSubEntriesNode(token, entry.getDN());
    }

    static void checkBaseNodesForOrg(
        SSOToken token,
        String orgDN,
        String sName,
        String version
    ) throws SMSException, SSOException {
        checkBaseNodesForOrg(token, orgDN, sName, version, false);
    }

    static void checkBaseNodesForOrg(
        SSOToken token,
        String orgDN,
        String sName,
        String version,
        boolean createRealms
    ) throws SMSException, SSOException {
        // Check if org exists
        SMSEntry entry = new SMSEntry(token, orgDN);
        if (entry.isNewEntry()) {
            // Organization does not exists, create if needed
            if (createRealms) {
                createOrganization(token, orgDN);
            } else {
                Object[] args = { orgDN };
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-org-doesnot-exist", args));
            }
        }

        // Check if services node exists
        String dn = "ou=services," + orgDN;
        checkAndCreateOrgUnitNode(token, dn);

        // Check if service node exists
        dn = "ou=" + sName + "," + dn;
        checkAndCreateServiceNode(token, dn);

        // Check if verion node exists
        dn = "ou=" + version + "," + dn;
        checkAndCreateServiceVersionNode(token, dn, sName);

        // Check orgUnit node
        if (orgDN.equalsIgnoreCase(SMSEntry.getRootSuffix())) {
            // Create all based nodes for root realm
            checkBaseNodes(token, dn);
        } else {
            // create only organization config
            checkAndCreateOrgUnitNode(token, ORG_CONFIG_NODE + dn);
        }
    }

    static void checkBaseNodes(SSOToken t, String baseDN) throws SMSException,
            SSOException {
        // Check global config node
        checkAndCreateOrgUnitNode(t, GLOBAL_CONFIG_NODE + baseDN);
        checkAndCreateOrgUnitNode(t, ORG_CONFIG_NODE + baseDN);
        checkAndCreateServiceNode(t, PLUGIN_CONFIG_NODE + baseDN);
        checkAndCreateOrgUnitNode(t, INSTANCES_NODE + baseDN);
    }

    static void checkAndCreateOrgUnitNode(SSOToken token, String dn)
            throws SMSException, SSOException {
        SMSEntry e = new SMSEntry(token, dn);

        if (SMSEntry.debug.messageEnabled()) {
            SMSEntry.debug.message("CreateServiceConfig." +
                    "checkAndCreateOrgUnitNode() creating entry: " + dn);
        }
        
        if (e.isNewEntry()) {
            // Add needed object classes
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
            e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_ORG_UNIT);
            e.save();
        }
    }

    static void checkAndCreateServiceNode(SSOToken token, String dn)
            throws SMSException, SSOException {
        SMSEntry e = new SMSEntry(token, dn);
        if (e.isNewEntry()) {
            int ndx = dn.indexOf(SMSEntry.SERVICES_RDN);
            if (ndx >= 0) {
                if (dn.indexOf(SMSEntry.SERVICES_RDN, ndx+11) >= 0) {
                    // Add needed object classes for the 'ou=services' node
                    // under the subrealms created.
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, 
                        SMSEntry.OC_SERVICE);
                } else {
                    // Add needed object classes
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, 
                        SMSEntry.OC_SERVICE_COMP);
                }
            }
        }
        e.save();
    }

    static void checkAndCreateServiceVersionNode(SSOToken t, String dn,
            String serviceName) throws SMSException, SSOException {
        SMSEntry e = new SMSEntry(t, dn);
        if (e.isNewEntry()) {
            int ndx = dn.indexOf(SMSEntry.SERVICES_RDN);
            if (ndx >= 0) {
                String firstSvc = dn.substring(ndx);
                if (firstSvc.indexOf(SMSEntry.SERVICES_RDN) >= 0) {
                    // Add needed object classes for the 'ou=services' node
                    // under the subrealms created.
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, 
                        SMSEntry.OC_SERVICE);
                } else {
                    // Add needed object classes and service name.
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, 
                        SMSEntry.OC_SERVICE);
                    e.addAttribute(SMSEntry.PLACEHOLDER_RDN, serviceName);
                }
            }
        }
        e.save();
    }

    static void updateSubEntriesNode(SSOToken token, String sdn)
            throws SMSException {
        // Get the name
        DN dn = new DN(sdn);
        String name = (dn.explodeDN(true))[0];
        // Get the parent DN
        DN parent = dn.getParent();
        CachedSubEntries subEntries = CachedSubEntries.getInstanceIfCached(
            token, parent.toRFCString(), true);
        if (subEntries != null) {
            subEntries.add(name);
        }
    }

    // Returns a map that contains attribute value pairs
    // %%% This must be moved to XMLUtils
    public static Map getAttributeValuePairs(Node n) {
        if (n == null) {
            return (null);
        }
        Map answer = null;
        Iterator attrNodes = XMLUtils.getChildNodes(n,
                SMSUtils.ATTRIBUTE_VALUE_PAIR).iterator();
        while (attrNodes.hasNext()) {
            Node attrValuePair = (Node) attrNodes.next();
            Node attrNode = XMLUtils.getChildNode(attrValuePair,
                    SMSUtils.ATTRIBUTE);
            if (attrNode == null) {
                continue;
            }
            String attrName = XMLUtils.getNodeAttributeValue(attrNode,
                    SMSUtils.NAME);
            Set values = XMLUtils.getAttributeValuePair(attrValuePair);
            if (answer == null) {
                answer = new HashMap();
            }
            answer.put(attrName, values);
        }
        return (answer);
    }

    // Returns a map that contains attribute value pairs
    // If 'unescape' is set to false, xml escaped chars will not
    // be unescaped.
    public static Map getAttributeValuePairs(Node n, boolean unescape) {
        if (n == null) {
            return (null);
        }
        Map answer = null;
        Iterator attrNodes = XMLUtils.getChildNodes(n,
                SMSUtils.ATTRIBUTE_VALUE_PAIR).iterator();
        while (attrNodes.hasNext()) {
            Node attrValuePair = (Node) attrNodes.next();
            Node attrNode = XMLUtils.getChildNode(attrValuePair,
                    SMSUtils.ATTRIBUTE);
            if (attrNode == null) {
                continue;
            }
            String attrName = XMLUtils.getNodeAttributeValue(attrNode,
                    SMSUtils.NAME);
            Set values = 
                XMLUtils.getAttributeValuePair(attrValuePair, unescape);
            if (answer == null) {
                answer = new HashMap();
            }
            answer.put(attrName, values);
        }
        return (answer);
    }

    /*
     * create the sub-organization.
     */
    static void createOrganization(SSOToken token, String orgDN)
        throws SMSException {

        // Check if the organization already exists
        try {
            // Normalize DN, so it can be parsed and compared
            Object args1[] = {orgDN};
            orgDN = (new DN(orgDN)).toRFCString();
            if (orgDN.length() == 0) {
                SMSEntry.debug.error("CreateServiceConfig."+
                    "createOrganization() : Detected invalid characters. "+
                    "Invalid realm name: "+ args1[0]);
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    "sms-invalid-org-name", args1));
            }

            CachedSMSEntry cEntry = CachedSMSEntry.getInstance(token, orgDN);
            if (cEntry.isDirty()) {
                cEntry.refresh();
            }
            SMSEntry e = cEntry.getClonedSMSEntry();
            if (!e.isNewEntry()) {
                SMSEntry.debug.error("Organization already exists: " + orgDN);
                throw (new OrganizationAlreadyExistsException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.SMS_organization_already_exists_no_args,
                        null));
            }
            
            // Need to start from baseDN, to create intermediate nodes
            String[] dns = LDAPDN.explodeDN(orgDN, false);
            String partdn = dns[dns.length -1];
            // Obtain the baseDN
            int index = dns.length -1;
            while ((index > 0) && !partdn.equalsIgnoreCase(DNMapper.serviceDN)){
                partdn = dns[--index] + "," + partdn;
            }
            // Check the intermediate nodes
            while (index >= 1) {
                partdn = dns[--index] + "," + partdn;
                cEntry = CachedSMSEntry.getInstance(token, partdn);
                if (cEntry.isDirty()) {
                    cEntry.refresh();
                }
                e = cEntry.getClonedSMSEntry();
                if (e.isNewEntry()) {
                    // Create the realm
                    // Add needed object classes
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS,
                        SMSEntry.OC_REALM_SERVICE);
                    e.addAttribute(SMSEntry.ATTR_OBJECTCLASS, SMSEntry.OC_TOP);
                    e.save(token);
                    cEntry.refresh(e);
                }
            } 
        } catch (SSOException ssoe) {
            SMSEntry.debug.error("CreateServiceConfig: Unable to "
                    + "create organization ", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }
}
