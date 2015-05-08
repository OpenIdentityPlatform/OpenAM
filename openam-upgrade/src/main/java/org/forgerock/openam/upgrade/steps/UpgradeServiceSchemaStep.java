/*
 * Copyright 2013-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */

/*
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package org.forgerock.openam.upgrade.steps;

import static org.forgerock.openam.upgrade.UpgradeServices.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

import java.io.File;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.sm.datalayer.api.ConnectionType;
import org.forgerock.openam.sm.datalayer.api.DataLayer;
import org.forgerock.openam.upgrade.NewServiceWrapper;
import org.forgerock.openam.upgrade.NewSubSchemaWrapper;
import org.forgerock.openam.upgrade.SchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.ServerUpgrade;
import org.forgerock.openam.upgrade.ServiceSchemaModificationWrapper;
import org.forgerock.openam.upgrade.ServiceSchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.SubSchemaModificationWrapper;
import org.forgerock.openam.upgrade.SubSchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeProgress;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;
import org.w3c.dom.Document;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchemaImpl;
import com.sun.identity.sm.ServiceSchemaModifications;

/**
 * Detects changes in the service schema and upgrades them if required.
 *
 * @author Peter Major
 */
@UpgradeStepInfo(dependsOn = "org.forgerock.openam.upgrade.steps.UpgradeDirectoryContentStep")
public class UpgradeServiceSchemaStep extends AbstractUpgradeStep {

    private static final String SERVICE_PROLOG = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
            + "<!DOCTYPE ServicesConfiguration\n"
            + "PUBLIC \"=//iPlanet//Service Management Services (SMS) 1.0 DTD//EN\"\n"
            + "\"jar://com/sun/identity/sm/sms.dtd\">\n";
    private static final String NEW_SERVICES = "%NEW_SERVICES%";
    private static final String MODIFIED_SERVICES = "%MODIFIED_SERVICES%";
    private static final String NEW_SCHEMAS = "%NEW_SCHEMAS%";
    private static final String NEW_SUB_SCHEMAS = "%NEW_SUB_SCHEMAS%";
    private static final String DELETED_SERVICES = "%DELETED_SERVICES%";
    private final List<NewServiceWrapper> addedServices = new ArrayList<NewServiceWrapper>();
    private final Map<String, Set<SchemaUpgradeWrapper>> modifiedSchemas =
            new HashMap<String, Set<SchemaUpgradeWrapper>>();
    private final Map<String, Map<String, ServiceSchemaUpgradeWrapper>> modifiedServices =
            new HashMap<String, Map<String, ServiceSchemaUpgradeWrapper>>();
    private final Map<String, Map<String, SubSchemaUpgradeWrapper>> modifiedSubSchemas =
            new HashMap<String, Map<String, SubSchemaUpgradeWrapper>>();
    private final Set<String> deletedServices = new HashSet<String>();

    @Inject
    public UpgradeServiceSchemaStep(final PrivilegedAction<SSOToken> adminTokenAction,
            @DataLayer(ConnectionType.DATA_LAYER) final ConnectionFactory factory) {
        super(adminTokenAction, factory);
    }

    @Override
    public boolean isApplicable() {
        return !modifiedSchemas.isEmpty() || !addedServices.isEmpty() || !modifiedServices.isEmpty() || !deletedServices.isEmpty()
                || !modifiedSubSchemas.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        String basedir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String dirXML = basedir + File.separator + "config" + File.separator + "xml";

        File xmlDirs = new File(dirXML);

        if (!xmlDirs.exists() || !xmlDirs.isDirectory()) {
            xmlDirs.mkdirs();

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Created directory: " + xmlDirs);
            }
        }

        Map<String, Document> newServiceDefinitions = UpgradeServiceUtils.getServiceDefinitions(getAdminToken());
        diffServiceVersions(newServiceDefinitions, getAdminToken());
    }

    private void diffServiceVersions(Map<String, Document> serviceDefinitions, SSOToken adminToken)
            throws UpgradeException {
        ServiceSchemaModifications modifications;
        Set<String> existingServiceNames = UpgradeUtils.getExistingServiceNames(adminToken);
        Set<String> newServiceNames = listNewServices(serviceDefinitions.keySet(), existingServiceNames);

        deletedServices.addAll(listDeletedServices(existingServiceNames));

        for (Map.Entry<String, Document> service : serviceDefinitions.entrySet()) {
            // service has been removed, skip modification check
            if (deletedServices.contains(service.getKey())) {
                continue;
            }

            final boolean newService = newServiceNames.contains(service.getKey());
            modifications = new ServiceSchemaModifications(service.getKey(), service.getValue(), adminToken, newService);

            if (newService) {
                addedServices.add(modifications.getNewServiceWrapper());
            }

            if (modifications.hasSchemaChanges()) {
                modifiedSchemas.put(service.getKey(), modifications.getSchemaModifications());
            }

            if (modifications.isServiceModified()) {
                modifiedServices.put(service.getKey(), modifications.getServiceModifications());
            }

            if (modifications.hasSubSchemaChanges()) {
                modifiedSubSchemas.put(service.getKey(), modifications.getSubSchemaChanges());
            }
        }
    }

    private Set<String> listNewServices(Set<String> serviceNames, Set<String> existingServices) {
        Set<String> newServiceNames = new HashSet<String>(serviceNames);

        if (newServiceNames.removeAll(existingServices)) {
            return newServiceNames;
        }

        return Collections.EMPTY_SET;
    }

    private Set<String> listDeletedServices(Set<String> existingServices)
            throws UpgradeException {
        Set<String> toDelete = new HashSet<String>();

        // only delete services that still exist
        for (String serviceName : ServerUpgrade.getServicesToDelete()) {
            if (existingServices.contains(serviceName)) {
                toDelete.add(serviceName);
            }
        }

        return toDelete;
    }

    @Override
    public void perform() throws UpgradeException {
        UpgradeProgress.reportStart("upgrade.upgradeservices");
        UpgradeProgress.reportEnd("upgrade.blank");

        if (!addedServices.isEmpty()) {
            StringBuilder buffer = new StringBuilder();

            if (DEBUG.messageEnabled()) {
                buffer.append("services to add: ");
            }

            for (NewServiceWrapper serviceToAdd : addedServices) {
                final StringBuilder serviceDefinition = new StringBuilder(SERVICE_PROLOG);
                serviceDefinition.append(XMLUtils.print(serviceToAdd.getServiceDocument()));
                UpgradeProgress.reportStart("upgrade.addservice", serviceToAdd.getServiceName());
                UpgradeUtils.createService(serviceDefinition.toString(), serviceToAdd, getAdminToken());
                UpgradeProgress.reportEnd("upgrade.success");

                if (DEBUG.messageEnabled()) {
                    buffer.append(serviceToAdd.getServiceName()).append(": ");
                }
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("services to add: " + buffer.toString());
            }
        }

        if (!modifiedSchemas.isEmpty()) {
            for (Map.Entry<String, Set<SchemaUpgradeWrapper>> entry : modifiedSchemas.entrySet()) {
                String serviceName = entry.getKey();
                for (SchemaUpgradeWrapper schemaUpgradeWrapper : entry.getValue()) {
                    UpgradeProgress.reportStart("upgrade.addschema",
                            schemaUpgradeWrapper.getNewSchema().getSchemaName(), serviceName);
                    UpgradeUtils.addNewSchema(serviceName, schemaUpgradeWrapper, getAdminToken());
                    UpgradeProgress.reportEnd("upgrade.success");
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("modified schema: " + serviceName);
                }
            }
        }
        if (!modifiedServices.isEmpty()) {
            for (Map.Entry<String, Map<String, ServiceSchemaUpgradeWrapper>> serviceToModify : modifiedServices.entrySet()) {
                UpgradeProgress.reportStart("upgrade.modservice", serviceToModify.getKey());
                UpgradeUtils.modifyService(serviceToModify.getKey(), serviceToModify.getValue(), getAdminToken());
                UpgradeProgress.reportEnd("upgrade.success");

                if (DEBUG.messageEnabled()) {
                    DEBUG.message("modified service: " + serviceToModify.getKey());
                }
            }
        }

        if (!modifiedSubSchemas.isEmpty()) {
            for (Map.Entry<String, Map<String, SubSchemaUpgradeWrapper>> ssMod : modifiedSubSchemas.entrySet()) {
                UpgradeProgress.reportStart("upgrade.addsubschema", ssMod.getKey());
                UpgradeUtils.addNewSubSchemas(ssMod.getKey(), ssMod.getValue(), getAdminToken());
                UpgradeProgress.reportEnd("upgrade.success");

                if (DEBUG.messageEnabled()) {
                    DEBUG.message("modified sub schema: " + ssMod.getKey());
                }
            }
        }

        if (!deletedServices.isEmpty()) {
            for (String serviceToDelete : deletedServices) {
                UpgradeProgress.reportStart("upgrade.delservice", serviceToDelete);
                UpgradeUtils.deleteService(serviceToDelete, getAdminToken());
                UpgradeProgress.reportEnd("upgrade.success");

                if (DEBUG.messageEnabled()) {
                    DEBUG.message("deleted service: " + serviceToDelete);
                }
            }
        }
    }

    @Override
    public String getShortReport(String delimiter) {
        StringBuilder buffer = new StringBuilder();

        if (addedServices != null && !addedServices.isEmpty()) {
            for (NewServiceWrapper added : addedServices) {
                buffer.append(added.getServiceName()).append(" (").append(BUNDLE.getString("upgrade.new")).append(")");
                buffer.append(delimiter);
            }
        }

        Map<String, Set<ServiceModification>> updatedServices = new HashMap<String, Set<ServiceModification>>();

        if (!modifiedSchemas.isEmpty()) {
            for (Map.Entry<String, Set<SchemaUpgradeWrapper>> schemaMod : modifiedSchemas.entrySet()) {
                updatedServices.put(schemaMod.getKey(), asOrderedSet(ServiceModification.NEW_SCHEMA));
            }
        }

        if (!modifiedServices.isEmpty()) {
            for (Map.Entry<String, Map<String, ServiceSchemaUpgradeWrapper>> mod : modifiedServices.entrySet()) {
                if (updatedServices.keySet().contains(mod.getKey())) {
                    updatedServices.get(mod.getKey()).add(ServiceModification.ATTR_MOD);
                } else {
                    updatedServices.put(mod.getKey(), asOrderedSet(ServiceModification.ATTR_MOD));
                }
            }
        }

        if (!modifiedSubSchemas.isEmpty()) {
            for (Map.Entry<String, Map<String, SubSchemaUpgradeWrapper>> ssMod : modifiedSubSchemas.entrySet()) {
                if (updatedServices.keySet().contains(ssMod.getKey())) {
                    updatedServices.get(ssMod.getKey()).add(ServiceModification.NEW_SUB_SCHEMA);
                } else {
                    updatedServices.put(ssMod.getKey(), asOrderedSet(ServiceModification.NEW_SUB_SCHEMA));
                }
            }
        }

        for (Map.Entry<String, Set<ServiceModification>> modSvc : updatedServices.entrySet()) {
            buffer.append(modSvc.getKey()).append(" (").append(StringUtils.join(modSvc.getValue(), " & "));
            buffer.append(")").append(delimiter);
        }

        if (!deletedServices.isEmpty()) {
            for (String serviceName : deletedServices) {
                buffer.append(serviceName).append(" (").append(BUNDLE.getString("upgrade.deleted"));
                buffer.append(")").append(delimiter);
            }
        }

        return buffer.toString();
    }

    @Override
    public String getDetailedReport(String delimiter) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(LF, delimiter);

        if (!modifiedSchemas.isEmpty()) {
            StringBuilder sBuf = new StringBuilder();
            for (Map.Entry<String, Set<SchemaUpgradeWrapper>> schemaMod : modifiedSchemas.entrySet()) {
                sBuf.append(BULLET).append(schemaMod.getKey()).append(delimiter);

                for (SchemaUpgradeWrapper schemaUpgradeWrapper : schemaMod.getValue()) {
                    sBuf.append(INDENT).append(schemaUpgradeWrapper.getNewSchema().getSchemaName()).append(delimiter);
                }
                sBuf.append(delimiter);
            }

            tags.put(NEW_SCHEMAS, sBuf.toString());
        } else {
            tags.put(NEW_SCHEMAS, BUNDLE.getString("upgrade.none"));
        }

        if (addedServices != null && !addedServices.isEmpty()) {
            StringBuilder aBuf = new StringBuilder();

            for (NewServiceWrapper added : addedServices) {
                aBuf.append(BULLET).append(added.getServiceName()).append(delimiter);
            }

            tags.put(NEW_SERVICES, aBuf.toString());
        } else {
            tags.put(NEW_SERVICES, BUNDLE.getString("upgrade.none"));
        }

        if (!modifiedServices.isEmpty()) {
            StringBuilder mBuf = new StringBuilder();

            for (Map.Entry<String, Map<String, ServiceSchemaUpgradeWrapper>> mod : modifiedServices.entrySet()) {
                mBuf.append(BULLET).append(mod.getKey()).append(delimiter);

                for (Map.Entry<String, ServiceSchemaUpgradeWrapper> serviceType : mod.getValue().entrySet()) {
                    ServiceSchemaUpgradeWrapper sUpdate = serviceType.getValue();

                    if (sUpdate != null) {
                        if (sUpdate.getAttributesAdded() != null && sUpdate.getAttributesAdded().hasBeenModified()) {
                            mBuf.append(calculateAttrModifications(BUNDLE.getString("upgrade.addattr"),
                                    sUpdate.getAttributesAdded(), delimiter));
                        }

                        if (sUpdate.getAttributesModified() != null
                                && sUpdate.getAttributesModified().hasBeenModified()) {
                            mBuf.append(calculateAttrModifications(BUNDLE.getString("upgrade.modattr"),
                                    sUpdate.getAttributesModified(), delimiter));
                        }

                        if (sUpdate.getAttributesDeleted() != null
                                && sUpdate.getAttributesDeleted().hasBeenModified()) {
                            mBuf.append(calculateAttrModifications(BUNDLE.getString("upgrade.delattr"),
                                    sUpdate.getAttributesDeleted(), delimiter));
                        }
                    }
                }
            }

            tags.put(MODIFIED_SERVICES, mBuf.toString());
        } else {
            tags.put(MODIFIED_SERVICES, BUNDLE.getString("upgrade.none"));
        }

        if (!modifiedSubSchemas.isEmpty()) {
            StringBuilder ssBuf = new StringBuilder();

            for (Map.Entry<String, Map<String, SubSchemaUpgradeWrapper>> ssMod : modifiedSubSchemas.entrySet()) {
                ssBuf.append(BULLET).append(ssMod.getKey()).append(delimiter);

                for (Map.Entry<String, SubSchemaUpgradeWrapper> serviceType : ssMod.getValue().entrySet()) {
                    SubSchemaUpgradeWrapper ssUpdate = serviceType.getValue();

                    if (ssUpdate != null) {
                        if (ssUpdate.getSubSchemasAdded() != null
                                && ssUpdate.getSubSchemasAdded().subSchemaChanged()) {
                            ssBuf.append(INDENT).append(calculateSubSchemaAdditions(ssUpdate.getSubSchemasAdded(),
                                    delimiter));
                        }
                    }

                    ssBuf.append(delimiter);
                }
            }

            tags.put(NEW_SUB_SCHEMAS, ssBuf.toString());
        } else {
            tags.put(NEW_SUB_SCHEMAS, BUNDLE.getString("upgrade.none"));
        }

        if (!deletedServices.isEmpty()) {
            StringBuilder dBuf = new StringBuilder();

            for (String serviceName : deletedServices) {
                dBuf.append(BULLET).append(serviceName).append(delimiter);
            }

            tags.put(DELETED_SERVICES, dBuf.toString());
        } else {
            tags.put(DELETED_SERVICES, BUNDLE.getString("upgrade.none"));
        }

        return tagSwapReport(tags, "upgrade.servicereport");
    }

    private String calculateAttrModifications(String prefix, ServiceSchemaModificationWrapper schemaMods,
            String delimiter) {
        StringBuilder buffer = new StringBuilder();

        if (!(schemaMods.getAttributes().isEmpty())) {
            for (AttributeSchemaImpl attrs : schemaMods.getAttributes()) {
                buffer.append(INDENT).append(prefix).append(attrs.getName()).append(delimiter);
            }
        }

        if (schemaMods.hasSubSchema()) {
            for (Map.Entry<String, ServiceSchemaModificationWrapper> schema : schemaMods.getSubSchemas().entrySet()) {
                buffer.append(INDENT).append("* ").append(schema.getKey()).append(delimiter);
                if (!(schema.getValue().getAttributes().isEmpty())) {
                    for (AttributeSchemaImpl attrs : schema.getValue().getAttributes()) {
                        buffer.append(INDENT).append(INDENT).append(prefix).append(attrs.getName()).append(delimiter);
                    }
                }

                if (schema.getValue().hasSubSchema()) {
                    buffer.append(calculateAttrModifications(prefix, schema.getValue(), delimiter));
                }
            }
        }

        return buffer.toString();
    }

    private String calculateSubSchemaAdditions(SubSchemaModificationWrapper subSchemaMods, String delimiter) {
        StringBuilder buffer = new StringBuilder();

        if (subSchemaMods.hasNewSubSchema()) {
            for (Map.Entry<String, NewSubSchemaWrapper> newSubSchema : subSchemaMods.entrySet()) {
                buffer.append(INDENT).append(newSubSchema.getValue().getSubSchemaName()).append(delimiter);
            }
        }

        if (subSchemaMods.hasSubSchema()) {
            buffer.append(calculateSubSchemaAdditions(subSchemaMods.getSubSchema(), delimiter));
        }

        return buffer.toString();
    }

    private enum ServiceModification {

        ATTR_MOD("upgrade.updated"),
        NEW_SCHEMA("upgrade.newschema"),
        NEW_SUB_SCHEMA("upgrade.newsubschema");
        private final String key;

        private ServiceModification(String text) {
            this.key = text;
        }

        @Override
        public String toString() {
            return BUNDLE.getString(key);
        }
    }
}
