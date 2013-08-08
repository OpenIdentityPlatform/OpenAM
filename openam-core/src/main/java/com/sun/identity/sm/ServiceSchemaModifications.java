/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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
package com.sun.identity.sm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.forgerock.openam.upgrade.ServiceSchemaModificationWrapper;
import org.forgerock.openam.upgrade.ServiceSchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.NewSubSchemaWrapper;
import org.forgerock.openam.upgrade.ServerUpgrade;
import org.forgerock.openam.upgrade.SchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.SubSchemaModificationWrapper;
import org.forgerock.openam.upgrade.SubSchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeHelper;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class determines how a service schema has changed between the current
 * version and the new version in the war file.
 *
 * @author steve
 */
public class ServiceSchemaModifications {
    protected String serviceName = null;
    protected Document serviceSchemaDoc = null;
    protected SSOToken adminToken = null;

    private boolean isServiceModified = false;
    private boolean hasSubSchemaChanges = false;
    private boolean hasSchemaChanges = false;
    private Set<SchemaUpgradeWrapper> schemaModifications = null;
    private Map<String, ServiceSchemaUpgradeWrapper> modifications = null;
    private Map<String, SubSchemaUpgradeWrapper> subSchemaChanges = null;

    public ServiceSchemaModifications(String serviceName,
                          Document schemaDoc,
                          SSOToken adminToken)
    throws UpgradeException {
        this.serviceName = serviceName;
        this.serviceSchemaDoc = schemaDoc;
        this.adminToken = adminToken;

        parseServiceDefinition();
    }

    public boolean isServiceModified() {
        return isServiceModified;
    }

    public boolean hasSchemaChanges() {
        return hasSchemaChanges;
    }

    public boolean hasSubSchemaChanges() {
        return hasSubSchemaChanges;
    }

    public Set<SchemaUpgradeWrapper> getSchemaModifications() {
        return schemaModifications;
    }

    public Map<String, ServiceSchemaUpgradeWrapper> getServiceModifications() {
        return modifications;
    }

    public Map<String, SubSchemaUpgradeWrapper> getSubSchemaChanges() {
        return subSchemaChanges;
    }

    private void parseServiceDefinition() throws UpgradeException {
        //we should only fetch these once to prevent performance problems, also in case of fetchNew to prevent
        //encrypting passwords multiple times.
        Map<String, ServiceSchemaImpl> newSchemaMap = fetchNewServiceAttributes(serviceSchemaDoc);
        Map<String, ServiceSchemaImpl> existingSchemaMap = null;

        try {
            existingSchemaMap = fetchExistingServiceAttributes(serviceName, adminToken);
        } catch (SMSException smse) {
            UpgradeUtils.debug.error("unable to fetch existing service attributes", smse);
            throw new UpgradeException(smse.getMessage());
        } catch (SSOException ssoe) {
            UpgradeUtils.debug.error("unable to fetch existing service attributes", ssoe);
            throw new UpgradeException(ssoe.getMessage());
        }
        if (calculateSchemaChanges(newSchemaMap, existingSchemaMap)) {
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message("service " + serviceName + " has new/deleted schema");
            }
            hasSchemaChanges = true;
        }

        // sub schemas added or removed?
        if (calculateSubSchemaChanges(newSchemaMap, existingSchemaMap)) {
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message("service " + serviceName + " has a modified sub schema");
            }

            hasSubSchemaChanges = true;
        }

        // has the service changed
        if (calculateServiceModifications(newSchemaMap, existingSchemaMap)) {
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message("service " + serviceName + " has changes in schemas");
            }

            isServiceModified = true;
        }
    }

    private static Map<String, ServiceSchemaImpl> getAttributes(Document document) {
        Map<String, ServiceSchemaImpl> schemas = new HashMap<String, ServiceSchemaImpl>();
        Node schemaRoot = XMLUtils.getRootNode(document, SMSUtils.SCHEMA);

        Node childNode = XMLUtils.getChildNode(schemaRoot, SMSUtils.GLOBAL_SCHEMA);

        if (childNode != null) {
            schemas.put(SMSUtils.GLOBAL_SCHEMA, new ServiceSchemaImpl(null, childNode));
        }

        childNode = XMLUtils.getChildNode(schemaRoot, SMSUtils.ORG_SCHEMA);

        if (childNode != null) {
            schemas.put(SMSUtils.ORG_SCHEMA, new ServiceSchemaImpl(null, childNode));
        }

        childNode = XMLUtils.getChildNode(schemaRoot, SMSUtils.DYNAMIC_SCHEMA);

        if (childNode != null) {
            schemas.put(SMSUtils.DYNAMIC_SCHEMA, new ServiceSchemaImpl(null, childNode));
        }

        childNode = XMLUtils.getChildNode(schemaRoot, SMSUtils.USER_SCHEMA);

        if (childNode != null) {
            schemas.put(SMSUtils.USER_SCHEMA, new ServiceSchemaImpl(null, childNode));
        }

        return schemas;
    }

    private boolean calculateSchemaChanges(Map<String, ServiceSchemaImpl> newSchemaMap,
            Map<String, ServiceSchemaImpl> existingSchemaMap) throws UpgradeException {
        schemaModifications = new HashSet<SchemaUpgradeWrapper>();
        for (Map.Entry<String, ServiceSchemaImpl> entry : newSchemaMap.entrySet()) {
            String schemaName = entry.getKey();
            ServiceSchemaImpl schema = entry.getValue();
            if (!existingSchemaMap.containsKey(schemaName)) {
                ServiceSchemaModificationWrapper newAttrs = new ServiceSchemaModificationWrapper(serviceName,
                        schemaName, schema.getAttributeSchemas());
                //NB: only schema additions are currently supported.
                schemaModifications.add(new SchemaUpgradeWrapper(newAttrs));
            }
        }

        return !schemaModifications.isEmpty();
    }

    private boolean calculateSubSchemaChanges(Map<String, ServiceSchemaImpl> newSchemaMap,
            Map<String, ServiceSchemaImpl> existingSchemaMap) throws UpgradeException {
        subSchemaChanges = new HashMap<String, SubSchemaUpgradeWrapper>();

        try {
            for (Map.Entry<String, ServiceSchemaImpl> newAttrSchemaEntry : newSchemaMap.entrySet()) {
                SubSchemaModificationWrapper subSchemaAdded =
                        getSubSchemaAdditionsRecursive(newAttrSchemaEntry.getKey(),
                                                       newAttrSchemaEntry.getKey(),
                                                       newAttrSchemaEntry.getValue(),
                                                       existingSchemaMap.get(newAttrSchemaEntry.getKey()));

                if (subSchemaAdded.subSchemaChanged()) {
                    subSchemaChanges.put(newAttrSchemaEntry.getKey(), new SubSchemaUpgradeWrapper(subSchemaAdded));
                }
            }
        } catch (SMSException smse) {
            UpgradeUtils.debug.error("error whilst determining sub schema changes for service: " + serviceName, smse);
            throw new UpgradeException(smse.getMessage());
        }

        if (UpgradeUtils.debug.messageEnabled()) {
            UpgradeUtils.debug.message("calculateSubSchemaChanges returning " + (!(subSchemaChanges.isEmpty())));
        }

        return !(subSchemaChanges.isEmpty());
    }

    private SubSchemaModificationWrapper getSubSchemaAdditionsRecursive(String schemaName,
                                                                     String compoundName,
                                                                     ServiceSchemaImpl newSchema,
                                                                     ServiceSchemaImpl existingSchema)
    throws SMSException {
        SubSchemaModificationWrapper subSchemaAddedResult = new SubSchemaModificationWrapper()  ;

        if (!newSchema.getSubSchemaNames().isEmpty()) {
            for (String subSchemaName : (Set<String>) newSchema.getSubSchemaNames()) {
                if (!(existingSchema.getSubSchemaNames().contains(subSchemaName))) {
                    subSchemaAddedResult.put(compoundName + "/" + subSchemaName,
                            new NewSubSchemaWrapper(serviceName,
                                                    subSchemaName,
                                                    newSchema.getSubSchema(subSchemaName).getSchemaNode()));
                } else {
                    SubSchemaModificationWrapper subSchemaResult =
                            getSubSchemaAdditionsRecursive(subSchemaName,
                                                           compoundName + "/" + subSchemaName,
                                                           newSchema.getSubSchema(subSchemaName),
                                                           existingSchema.getSubSchema(subSchemaName));
                    if (subSchemaAddedResult.subSchemaChanged()) {
                        subSchemaAddedResult.setSubSchema(subSchemaResult);
                    }
                }
            }
        }

        return subSchemaAddedResult;
    }

    private boolean calculateServiceModifications(Map<String, ServiceSchemaImpl> newSchemaMap,
            Map<String, ServiceSchemaImpl> existingSchemaMap) throws UpgradeException {
        modifications = new HashMap<String, ServiceSchemaUpgradeWrapper>();

        try {
            for (Map.Entry<String, ServiceSchemaImpl> newAttrSchemaEntry : newSchemaMap.entrySet()) {
                ServiceSchemaImpl schema = existingSchemaMap.get(newAttrSchemaEntry.getKey());
                if (schema == null) {
                    //this is a new schema, that's not available in the existing schema, should be covered by
                    //calculateSchemaChanges
                    continue;
                }
                ServiceSchemaModificationWrapper attrsAdded =
                        getServiceAdditionsRecursive(newAttrSchemaEntry.getKey(),
                                                    newAttrSchemaEntry.getValue(),
                                                    schema);
                ServiceSchemaModificationWrapper attrsModified =
                        getServiceModificationsRecursive(newAttrSchemaEntry.getKey(),
                                                    newAttrSchemaEntry.getValue(),
                                                    schema);
                ServiceSchemaModificationWrapper attrsDeleted =
                        getServiceDeletionsRecursive(newAttrSchemaEntry.getKey(),
                                                    newAttrSchemaEntry.getValue(),
                                                    schema);

                if (attrsAdded.hasBeenModified() || attrsModified.hasBeenModified() || attrsDeleted.hasBeenModified()) {
                    modifications.put(newAttrSchemaEntry.getKey(),
                            new ServiceSchemaUpgradeWrapper(attrsAdded, attrsModified, attrsDeleted));
                }
            }
        } catch (SMSException smse) {
            UpgradeUtils.debug.error("error whilst determining schema changes for service: " + serviceName, smse);
            throw new UpgradeException(smse.getMessage());
        }

        if (UpgradeUtils.debug.messageEnabled()) {
            UpgradeUtils.debug.message("calculateServiceModifications returning " + (!(modifications.isEmpty())));
        }

        return !(modifications.isEmpty());
    }

    private ServiceSchemaModificationWrapper getServiceAdditionsRecursive(String schemaName,
                                                                           ServiceSchemaImpl newSchema,
                                                                           ServiceSchemaImpl existingSchema)
    throws SMSException, UpgradeException {
        Set<AttributeSchemaImpl> attrsAdded = new HashSet<AttributeSchemaImpl>();
        ServiceSchemaModificationWrapper attrAddedResult = new ServiceSchemaModificationWrapper(serviceName, schemaName);

        if (newSchema.getAttributeSchemas() != null) {
            attrsAdded = getAttributesAdded(newSchema.getAttributeSchemas(), existingSchema.getAttributeSchemas());
        }

        if (!(attrsAdded.isEmpty())) {
            attrAddedResult.setAttributes(attrsAdded);
        }

        if (!newSchema.getSubSchemaNames().isEmpty()) {
            for (String subSchemaName : (Set<String>) newSchema.getSubSchemaNames()) {
                if (!(existingSchema.getSubSchemaNames().contains(subSchemaName))) {
                    // new sub schema so skip attribute checking
                    continue;
                }

                ServiceSchemaModificationWrapper subSchemaResult = getServiceAdditionsRecursive(subSchemaName,
                                                    newSchema.getSubSchema(subSchemaName),
                                                    existingSchema.getSubSchema(subSchemaName));

                if (subSchemaResult.hasBeenModified()) {
                    attrAddedResult.addSubSchema(subSchemaName, subSchemaResult);
                }
            }
        }

        return attrAddedResult;
    }

    private ServiceSchemaModificationWrapper getServiceModificationsRecursive(String schemaName,
                                                                           ServiceSchemaImpl newSchema,
                                                                           ServiceSchemaImpl existingSchema)
    throws SMSException, UpgradeException {
        Set<AttributeSchemaImpl> attrsModified = new HashSet<AttributeSchemaImpl>();
        ServiceSchemaModificationWrapper attrModifiedResult = new ServiceSchemaModificationWrapper(serviceName, schemaName);

        if (newSchema.getAttributeSchemas() != null) {
            attrsModified = getAttributesModified(newSchema.getAttributeSchemas(), existingSchema.getAttributeSchemas());
        }

        if (!(attrsModified.isEmpty())) {
            attrModifiedResult.setAttributes(attrsModified);
        }

        if (!newSchema.getSubSchemaNames().isEmpty()) {
            for (String subSchemaName : (Set<String>) newSchema.getSubSchemaNames()) {
                if (!(existingSchema.getSubSchemaNames().contains(subSchemaName))) {
                    // new sub schema so skip attribute checking
                    continue;
                }

                ServiceSchemaModificationWrapper subSchemaResult = getServiceModificationsRecursive(subSchemaName,
                                                    newSchema.getSubSchema(subSchemaName),
                                                    existingSchema.getSubSchema(subSchemaName));

                if (subSchemaResult.hasBeenModified()) {
                    attrModifiedResult.addSubSchema(subSchemaName, subSchemaResult);
                }
            }
        }

        return attrModifiedResult;
    }

    private ServiceSchemaModificationWrapper getServiceDeletionsRecursive(String schemaName,
                                                                           ServiceSchemaImpl newSchema,
                                                                           ServiceSchemaImpl existingSchema)
    throws SMSException {
        Set<AttributeSchemaImpl> attrsDeleted = new HashSet<AttributeSchemaImpl>();
        ServiceSchemaModificationWrapper attrDeletedResult = new ServiceSchemaModificationWrapper(serviceName, schemaName);;

        if (newSchema.getAttributeSchemas() != null) {
            attrsDeleted = getAttributesDeleted(newSchema.getAttributeSchemas(), existingSchema.getAttributeSchemas());
        }

        if (!(attrsDeleted.isEmpty())) {
            attrDeletedResult.setAttributes(attrsDeleted);
        }

        if (!newSchema.getSubSchemaNames().isEmpty()) {
            for (String subSchemaName : (Set<String>) newSchema.getSubSchemaNames()) {
                if (!(existingSchema.getSubSchemaNames().contains(subSchemaName))) {
                    // new sub schema so skip attribute checking
                    continue;
                }

                ServiceSchemaModificationWrapper subSchemaResult = getServiceDeletionsRecursive(subSchemaName,
                                                    newSchema.getSubSchema(subSchemaName),
                                                    existingSchema.getSubSchema(subSchemaName));

                if (subSchemaResult.hasBeenModified()) {
                    attrDeletedResult.addSubSchema(subSchemaName, subSchemaResult);
                }
            }
        }

        return attrDeletedResult;
    }

    private Set<AttributeSchemaImpl> getAttributesAdded(Set<AttributeSchemaImpl> newAttrs,
            Set<AttributeSchemaImpl> existingAttrs) throws UpgradeException {
        Set<AttributeSchemaImpl> attrAdded = new HashSet<AttributeSchemaImpl>();

        for (AttributeSchemaImpl newAttr : newAttrs) {
            boolean found = false;
            for (AttributeSchemaImpl existingAttr : existingAttrs) {
                if (newAttr.getName().equals(existingAttr.getName())) {
                    found = true;
                }
            }

            if (!found) {
                UpgradeHelper serviceHelper = ServerUpgrade.getServiceHelper(serviceName);
                if (serviceHelper != null) {
                    newAttr = serviceHelper.addNewAttribute(existingAttrs, newAttr);
                }

                attrAdded.add(newAttr);
            }
        }

        return attrAdded;
    }

    private Set<AttributeSchemaImpl> getAttributesModified(Set<AttributeSchemaImpl> newAttrs, Set<AttributeSchemaImpl> existingAttrs)
    throws UpgradeException {
        Set<AttributeSchemaImpl> attrMods = new HashSet<AttributeSchemaImpl>();

        for (AttributeSchemaImpl newAttr : newAttrs) {
            // skip attributes that are not explicitly named for upgrade
            if (ServerUpgrade.getServiceHelper(serviceName) == null ||
                    !ServerUpgrade.getServiceHelper(serviceName).getAttributes().contains(newAttr.getName())) {
                continue;
            }

            for (AttributeSchemaImpl existingAttr : existingAttrs) {
                if (!existingAttr.getName().equals(newAttr.getName())) {
                    continue;
                }

                try {
                    UpgradeHelper helper = ServerUpgrade.getServiceHelper(serviceName);
                    AttributeSchemaImpl upgradedAttr = helper.upgradeAttribute(existingAttr, newAttr);

                    if (upgradedAttr != null) {
                        attrMods.add(upgradedAttr);
                    }
                } catch (UpgradeException ue) {
                    UpgradeUtils.debug.error("Unable to process upgrade helper", ue);
                    throw ue;
                }
            }
        }

        return attrMods;
    }

    private Set<AttributeSchemaImpl> getAttributesDeleted(Set<AttributeSchemaImpl> newAttrs, Set<AttributeSchemaImpl> existingAttrs) {
        Set<AttributeSchemaImpl> attrDeleted = new HashSet<AttributeSchemaImpl>();

        for (AttributeSchemaImpl existingAttr : existingAttrs) {
            boolean found = false;
            for (AttributeSchemaImpl newAttr  : newAttrs) {
                if (existingAttr.getName().equals(newAttr.getName())) {
                    found = true;
                }
            }

            if (!found) {
                attrDeleted.add(existingAttr);
            }
        }

        return attrDeleted;
    }

    private Map<String, ServiceSchemaImpl> fetchNewServiceAttributes(Document doc)
    throws UpgradeException {
        try {
            ServiceManager.checkAndEncryptPasswordSyntax(doc, true);
        } catch (SMSException smse) {
            UpgradeUtils.debug.error("Unable to encrypt default values for passwords");
            throw new UpgradeException(smse);
        }
        Map<String, ServiceSchemaImpl> schemas = getAttributes(doc);

        return schemas;
    }

    private Map<String, ServiceSchemaImpl> fetchExistingServiceAttributes(String serviceName, SSOToken adminToken)
    throws SMSException, SSOException {
        ServiceSchemaManager ssm = new ServiceSchemaManager(serviceName, adminToken);
        Map<String, ServiceSchemaImpl> schemas = getAttributes(ssm.getDocumentCopy());

        return schemas;
    }
}
