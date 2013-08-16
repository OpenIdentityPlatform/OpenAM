/*
 * Copyright 2013 ForgeRock AS.
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
package org.forgerock.openam.upgrade.steps;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.setup.BootstrapData;
import com.sun.identity.setup.IHttpServletRequest;
import com.sun.identity.setup.JCECrypt;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchemaImpl;
import com.sun.identity.sm.ServiceSchemaModifications;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.upgrade.NewSubSchemaWrapper;
import org.forgerock.openam.upgrade.SchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.ServerUpgrade;
import org.forgerock.openam.upgrade.ServiceSchemaModificationWrapper;
import org.forgerock.openam.upgrade.ServiceSchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.SubSchemaModificationWrapper;
import org.forgerock.openam.upgrade.SubSchemaUpgradeWrapper;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.upgrade.UpgradeHttpServletRequest;
import org.forgerock.openam.upgrade.UpgradeProgress;
import static org.forgerock.openam.upgrade.UpgradeServices.LF;
import static org.forgerock.openam.upgrade.UpgradeServices.tagSwapReport;
import org.forgerock.openam.upgrade.UpgradeStepInfo;
import org.forgerock.openam.upgrade.UpgradeUtils;
import static org.forgerock.openam.utils.CollectionUtils.*;
import org.forgerock.openam.utils.IOUtils;
import org.w3c.dom.Document;

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
    private final List<String> serviceNames = new ArrayList<String>();
    private final Map<String, Document> addedServices = new HashMap<String, Document>();
    private final Map<String, Set<SchemaUpgradeWrapper>> modifiedSchemas =
            new HashMap<String, Set<SchemaUpgradeWrapper>>();
    private final Map<String, Map<String, ServiceSchemaUpgradeWrapper>> modifiedServices =
            new HashMap<String, Map<String, ServiceSchemaUpgradeWrapper>>();
    private final Map<String, Map<String, SubSchemaUpgradeWrapper>> modifiedSubSchemas =
            new HashMap<String, Map<String, SubSchemaUpgradeWrapper>>();
    private final Set<String> deletedServices = new HashSet<String>();

    @Override
    public boolean isApplicable() {
        return !modifiedSchemas.isEmpty() || !addedServices.isEmpty() || !modifiedServices.isEmpty() || !deletedServices.isEmpty()
                || !modifiedSubSchemas.isEmpty();
    }

    @Override
    public void initialize() throws UpgradeException {
        serviceNames.addAll(UpgradeUtils.getPropertyValues(SetupConstants.PROPERTY_FILENAME,
                SetupConstants.SERVICE_NAMES));
//        Map map = ServicesDefaultValues.getDefaultValues();
        Map<String, Document> newServiceDefinitions = new HashMap<String, Document>();
        String basedir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String dirXML = basedir + File.separator + "config" + File.separator + "xml";

        File xmlDirs = new File(dirXML);

        if (!xmlDirs.exists() || !xmlDirs.isDirectory()) {
            xmlDirs.mkdirs();

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Created directory: " + xmlDirs);
            }
        }

        ServicesDefaultValues.setServiceConfigValues(getUpgradeHttpServletRequest(basedir));

        for (String serviceFileName : serviceNames) {
            boolean tagswap = true;

            if (serviceFileName.startsWith("*")) {
                serviceFileName = serviceFileName.substring(1);
                tagswap = false;
            }

            String strXML = null;

            try {
                strXML = IOUtils.readStream(getClass().getClassLoader().getResourceAsStream(serviceFileName));
            } catch (IOException ioe) {
                DEBUG.error("unable to load services file: " + serviceFileName, ioe);
                throw new UpgradeException(ioe);
            }

            // This string 'content' is to avoid plain text password
            // in the files copied to the config/xml directory.
//            String content = strXML;
//
//            if (tagswap) {
//                content = StringUtils.strReplaceAll(content,
//                        "@UM_DS_DIRMGRPASSWD@", "********");
//                content = ServicesDefaultValues.tagSwap(content, true);
//            }
            if (tagswap) {
                strXML = ServicesDefaultValues.tagSwap(strXML, true);
            }

            Document serviceSchema = fetchDocumentSchema(strXML, getAdminToken());

            newServiceDefinitions.put(UpgradeUtils.getServiceName(serviceSchema), serviceSchema);
        }

        diffServiceVersions(newServiceDefinitions, getAdminToken());
    }

    private void diffServiceVersions(Map<String, Document> serviceDefinitions, SSOToken adminToken)
            throws UpgradeException {
        ServiceSchemaModifications modifications;
        Set<String> existingServiceNames = UpgradeUtils.getExistingServiceNames(adminToken);
        Set<String> newServiceNames = listNewServices(serviceDefinitions.keySet(), existingServiceNames);

        for (String newServiceName : newServiceNames) {
            addedServices.put(newServiceName, serviceDefinitions.get(newServiceName));

            if (DEBUG.messageEnabled()) {
                DEBUG.message("found new service: " + newServiceName);
            }
        }

        deletedServices.addAll(listDeletedServices(existingServiceNames));

        for (Map.Entry<String, Document> service : serviceDefinitions.entrySet()) {
            // service is new, skip modification check
            if (newServiceNames.contains(service.getKey())) {
                continue;
            }

            // service has been removed, skip modification check
            if (deletedServices.contains(service.getKey())) {
                continue;
            }

            modifications = new ServiceSchemaModifications(service.getKey(), service.getValue(), adminToken);

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

    private Document fetchDocumentSchema(String xmlContent, SSOToken adminToken)
            throws UpgradeException {
        InputStream serviceStream = null;
        Document doc = null;

        try {
            serviceStream = new ByteArrayInputStream(xmlContent.getBytes());
            doc = UpgradeUtils.parseServiceFile(serviceStream, adminToken);
        } finally {
            IOUtils.closeIfNotNull(serviceStream);
        }

        return doc;
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

            for (Map.Entry<String, Document> serviceToAdd : addedServices.entrySet()) {
                StringBuilder serviceDefinition = new StringBuilder(SERVICE_PROLOG);
                serviceDefinition.append(XMLUtils.print(serviceToAdd.getValue()));
                UpgradeProgress.reportStart("upgrade.addservice", serviceToAdd.getKey());
                UpgradeUtils.createService(serviceDefinition.toString(), getAdminToken());
                UpgradeProgress.reportEnd("upgrade.success");

                if (DEBUG.messageEnabled()) {
                    buffer.append(serviceToAdd.getKey()).append(": ");
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
            for (Map.Entry<String, Document> added : addedServices.entrySet()) {
                buffer.append(added.getKey()).append(" (").append(BUNDLE.getString("upgrade.new")).append(")");
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

            for (Map.Entry<String, Document> added : addedServices.entrySet()) {
                aBuf.append(BULLET).append(UpgradeUtils.getServiceName(added.getValue())).append(delimiter);
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
                if (!(schema.getValue().getAttributes().isEmpty())) {
                    for (AttributeSchemaImpl attrs : schema.getValue().getAttributes()) {
                        buffer.append(INDENT).append(prefix).append(attrs.getName()).append(delimiter);
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

    private IHttpServletRequest getUpgradeHttpServletRequest(String basedir) throws UpgradeException {
        // need to reinitialize the tag swap property map with original install params
        IHttpServletRequest requestFromFile = new UpgradeHttpServletRequest(basedir);

        try {
            Properties foo = ServerConfiguration.getServerInstance(getAdminToken(), WebtopNaming.getLocalServer());
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_ENCRYPTION_KEY, foo.getProperty(Constants.ENC_PWD_PROPERTY));

            String dbOption = (String) requestFromFile.getParameterMap().get(SetupConstants.CONFIG_VAR_DATA_STORE);
            boolean embedded = dbOption.equals(SetupConstants.SMS_EMBED_DATASTORE);

            if (!embedded) {
                setUserAndPassword(requestFromFile, basedir);
            }
        } catch (Exception ex) {
            DEBUG.error("Unable to initialise services defaults", ex);
            throw new UpgradeException("Unable to initialise services defaults: " + ex.getMessage());
        }

        return requestFromFile;
    }

    private void setUserAndPassword(IHttpServletRequest requestFromFile, String basedir) throws UpgradeException {
        try {
            BootstrapData bootStrap = new BootstrapData(basedir);
            Map<String, String> data = bootStrap.getDataAsMap(0);
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_DS_MGR_DN, data.get(BootstrapData.DS_MGR));
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_DS_MGR_PWD,
                    JCECrypt.decode(data.get(BootstrapData.DS_PWD)));
        } catch (IOException ioe) {
            DEBUG.error("Unable to load directory user/password from bootstrap file", ioe);
            throw new UpgradeException("Unable to load bootstrap file: " + ioe.getMessage());
        }
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
