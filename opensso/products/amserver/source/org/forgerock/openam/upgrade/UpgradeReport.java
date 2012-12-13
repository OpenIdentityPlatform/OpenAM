/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openam.upgrade;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.AttributeSchemaImpl;
import com.sun.identity.sm.SMSException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import org.w3c.dom.Document;

/**
 *
 * @author steve
 */
public class UpgradeReport {
    private SSOToken adminToken = null;
    private ServiceUpgradeWrapper serviceChanges = null;
    private final static String HTML_BR = "<br>";
    private final static String TXT_LF = "\n";
    public final static String INDENT = "\t";
    private final String delimiter;
    private ResourceBundle bundle;
    private String reportTxt;
    private final static String BUNDLE_NAME = "amUpgrade";
    private final static String REPORT = "report";
    public final static String SHORT_REPORT = "SHORT_REPORT";
    public final static String LF = "%LF%";
    private final static String CREATED_DATE = "%CREATED_DATE%";
    private final static String EXISTING_VERSION = "%EXISTING_VERSION%";
    private final static String NEW_VERSION = "%NEW_VERSION%";
    private final static String NEW_SERVICES = "%NEW_SERVICES%";
    private final static String MODIFIED_SERVICES = "%MODIFIED_SERVICES%";
    private final static String NEW_SUB_SCHEMAS = "%NEW_SUB_SCHEMAS%";
    private final static String DELETED_SERVICES = "%DELETED_SERVICES%";
    public final static String NEW_ATTRS = "%NEW_ATTRS%";
    public final static String MOD_ATTRS = "%MOD_ATTRS%";
    public final static String DEL_ATTRS = "%DEL_ATTRS%";
    public final static String BULLET = "* ";
    private final static String NONE = "* None";
    private final static String DATE_FORMAT = "yyyyMMddHHmmss";
    private final static String REPORT_FILENAME = "upgradereport";
    private SimpleDateFormat dateFormat;
    private String shortReport = null;
    private String detailedReport = null;
    private String createdDate = null;
    
    public UpgradeReport(SSOToken adminToken, ServiceUpgradeWrapper serviceChanges, boolean html) 
    throws UpgradeException {
        this.adminToken = adminToken;
        this.serviceChanges = serviceChanges;
        delimiter = (html) ? HTML_BR : TXT_LF;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME);
        
        if (bundle == null) {
            throw new UpgradeException("unable to load file: " + BUNDLE_NAME);
        }
        
        reportTxt = bundle.getString(REPORT);
        
        if (reportTxt == null) {
            throw new UpgradeException("property file: " + BUNDLE_NAME + " is missing property: " + REPORT);
        }
        
        dateFormat = new SimpleDateFormat(DATE_FORMAT);
        createdDate = dateFormat.format(new Date());
        generateShortReport();
        generateDetailedReport();
    }
    
    public String getCreatedDate() {
        return createdDate;
    }
    
    public String getShortReport() {
        return shortReport;
    }
    
    private void generateShortReport() {
        StringBuilder buffer = new StringBuilder();
        
        if (serviceChanges.getServicesAdded() != null && 
                !serviceChanges.getServicesAdded().isEmpty()) {
            for (Map.Entry<String, Document> added : serviceChanges.getServicesAdded().entrySet()) {
                buffer.append(UpgradeUtils.getServiceName(added.getValue())).append(" (New)").append(delimiter);
            }
        }
        
        Map<String, ServiceModification> updatedServices = new HashMap<String, ServiceModification>();

        if (serviceChanges.getServicesModified() != null &&
                !serviceChanges.getServicesModified().isEmpty()) {
            for (Map.Entry<String, Map<String, ServiceSchemaUpgradeWrapper>> mod : serviceChanges.getServicesModified().entrySet()) {
                updatedServices.put(mod.getKey(), ServiceModification.ATTR_MOD);
            }
        }
        
        if (serviceChanges.getSubSchemasModified() != null &&
                !serviceChanges.getSubSchemasModified().isEmpty()) {
            for (Map.Entry<String, Map<String, SubSchemaUpgradeWrapper>> ssMod : serviceChanges.getSubSchemasModified().entrySet()) {
                if (updatedServices.keySet().contains(ssMod.getKey())) {
                    updatedServices.put(ssMod.getKey(), ServiceModification.ATTR_AND_NEW_SUB_SCHEMA);
                } else {
                    updatedServices.put(ssMod.getKey(), ServiceModification.NEW_SUB_SCHEMA);
                }
            }
        }
        
        for (Map.Entry<String, ServiceModification> modSvc : updatedServices.entrySet()) {
            buffer.append(modSvc.getKey()).append(modSvc.getValue()).append(delimiter);
        }

        if (serviceChanges.getServicesDeleted() != null &&
                !serviceChanges.getServicesDeleted().isEmpty()) {
            for (String serviceName : serviceChanges.getServicesDeleted()) {
                buffer.append(serviceName).append(" (Deleted)").append(delimiter);
            }
        }
        
        try {
            Map<String, StringBuilder> propReport = ServerConfiguration.upgradeDefaults(adminToken, true, true);
            
            if (propReport.get(SHORT_REPORT) != null) {
                buffer.append(propReport.get(SHORT_REPORT));
            }
        } catch (SSOException ssoe) {
            UpgradeUtils.debug.error("Unable to determine service configuration updates.", ssoe);
        } catch (SMSException smse) {
            UpgradeUtils.debug.error("Unable to determine service configuration updates.", smse);
        } catch (UnknownPropertyNameException upne) {
            UpgradeUtils.debug.error("Unable to determine service configuration updates.", upne);
        }
        
        shortReport = buffer.toString();
    }
    
    public String getDetailedReport() { 
        return detailedReport;
    }
    
    private void generateDetailedReport() {
        StringBuilder buffer = new StringBuilder();
        Map<String, StringBuilder> reportContents = new HashMap<String, StringBuilder>();
        
        reportContents.put(CREATED_DATE, new StringBuilder().append(createdDate));
        reportContents.put(EXISTING_VERSION, new StringBuilder().append(UpgradeUtils.getCurrentVersion()));
        reportContents.put(NEW_VERSION, new StringBuilder().append(UpgradeUtils.getWarFileVersion()));
        reportContents.put(LF, new StringBuilder().append(delimiter));
        
        if (serviceChanges.getServicesAdded() != null && 
                !serviceChanges.getServicesAdded().isEmpty()) {
            StringBuilder aBuf = new StringBuilder();
            
            for (Map.Entry<String, Document> added : serviceChanges.getServicesAdded().entrySet()) {
                aBuf.append(BULLET).append(UpgradeUtils.getServiceName(added.getValue())).append(delimiter);
            }
            
            reportContents.put(NEW_SERVICES, aBuf);
        } else {
            reportContents.put(NEW_SERVICES, new StringBuilder().append(NONE));
        }

        if (serviceChanges.getServicesModified() != null &&
                !serviceChanges.getServicesModified().isEmpty()) {
            StringBuilder mBuf = new StringBuilder();
            
            for (Map.Entry<String, Map<String, ServiceSchemaUpgradeWrapper>> mod : serviceChanges.getServicesModified().entrySet()) {
                mBuf.append(BULLET).append(mod.getKey()).append(delimiter);

                for (Map.Entry<String,ServiceSchemaUpgradeWrapper> serviceType : mod.getValue().entrySet()) {
                    ServiceSchemaUpgradeWrapper sUpdate = serviceType.getValue(); 

                    if (sUpdate != null) {
                        if (sUpdate.getAttributesAdded() != null &&
                                sUpdate.getAttributesAdded().hasBeenModified()) {
                            mBuf.append(calculateAttrModifications(bundle.getString("upgrade.addattr"), sUpdate.getAttributesAdded()));
                        }
                        
                        if (sUpdate.getAttributesModified() != null &&
                                sUpdate.getAttributesModified().hasBeenModified()) {
                            mBuf.append(calculateAttrModifications(bundle.getString("upgrade.modattr"), sUpdate.getAttributesModified()));
                        }

                        if (sUpdate.getAttributesDeleted() != null &&
                                sUpdate.getAttributesDeleted().hasBeenModified()) {
                            mBuf.append(calculateAttrModifications(bundle.getString("upgrade.delattr"), sUpdate.getAttributesDeleted()));
                        }
                    }
                }
            }
            
            reportContents.put(MODIFIED_SERVICES, mBuf);
        } else {
            reportContents.put(MODIFIED_SERVICES, new StringBuilder().append(NONE));
        }
        
        if (serviceChanges.getSubSchemasModified() != null &&
                !serviceChanges.getSubSchemasModified().isEmpty()) {
            StringBuilder ssBuf = new StringBuilder();
            
            for (Map.Entry<String, Map<String, SubSchemaUpgradeWrapper>> ssMod : serviceChanges.getSubSchemasModified().entrySet()) {
                ssBuf.append(BULLET).append(ssMod.getKey()).append(delimiter);
                
                for (Map.Entry<String,SubSchemaUpgradeWrapper> serviceType : ssMod.getValue().entrySet()) {                    
                    SubSchemaUpgradeWrapper ssUpdate = serviceType.getValue();
                    
                    if (ssUpdate != null) {
                        if (ssUpdate.getSubSchemasAdded() != null &&
                                ssUpdate.getSubSchemasAdded().subSchemaChanged()) {
                            buffer.append(INDENT).append(calculateSubSchemaAdditions(ssUpdate.getSubSchemasAdded()));
                        }
                    }
                    
                    buffer.append(delimiter);
                }
            }
            
            reportContents.put(NEW_SUB_SCHEMAS, ssBuf);
        } else {
            reportContents.put(NEW_SUB_SCHEMAS, new StringBuilder().append(NONE));
        }

        if (serviceChanges.getServicesDeleted() != null &&
                !serviceChanges.getServicesDeleted().isEmpty()) {
            StringBuilder dBuf = new StringBuilder();

            for (String serviceName : serviceChanges.getServicesDeleted()) {
                dBuf.append(BULLET).append(serviceName).append(delimiter);
            }
            
            reportContents.put(DELETED_SERVICES, dBuf);
        } else {
            reportContents.put(DELETED_SERVICES, new StringBuilder().append(NONE));
        }
        
        try {
            reportContents.putAll(ServerConfiguration.upgradeDefaults(adminToken, true, false));
        } catch (SSOException ssoe) {
            UpgradeUtils.debug.error("Unable to determine service configuration updates.", ssoe);
        } catch (SMSException smse) {
            UpgradeUtils.debug.error("Unable to determine service configuration updates.", smse);
        } catch (UnknownPropertyNameException upne) {
            UpgradeUtils.debug.error("Unable to determine service configuration updates.", upne);
        }
        
        detailedReport = tagSwapReport(reportContents);
    }
    
    protected String tagSwapReport(Map<String, StringBuilder> reportContents) {        
        for (Map.Entry<String, StringBuilder> contents : reportContents.entrySet()) {
            reportTxt = reportTxt.replace(contents.getKey(), contents.getValue().toString());
        }
        
        return reportTxt;
    }
    
    protected String calculateAttrModifications(String prefix, ServiceSchemaModificationWrapper schemaMods) {
        StringBuilder buffer = new StringBuilder();
        
        if (!(schemaMods.getAttributes().isEmpty())) {
            for (AttributeSchemaImpl attrs : schemaMods.getAttributes()) {
                buffer.append(INDENT).append(attrs.getName()).append(delimiter);
            }
        }
      
        if (schemaMods.hasSubSchema()) {
            for (Map.Entry<String, ServiceSchemaModificationWrapper> schema : schemaMods.getSubSchemas().entrySet()) {
                if (!(schema.getValue().getAttributes().isEmpty())) {
                    for(AttributeSchemaImpl attrs : schema.getValue().getAttributes()) {
                        buffer.append(INDENT).append(prefix).append(attrs.getName()).append(delimiter);
                    }
                }

                if (schema.getValue().hasSubSchema()) {
                    buffer.append(calculateAttrModifications(prefix, schema.getValue()));
                }            
            }
        }
        
        return buffer.toString();
    }
    
    protected String calculateSubSchemaAdditions(SubSchemaModificationWrapper subSchemaMods) {
        StringBuilder buffer = new StringBuilder();
        
        if (subSchemaMods.hasNewSubSchema()) {
            for (Map.Entry<String, NewSubSchemaWrapper> newSubSchema : subSchemaMods.entrySet()) {
                buffer.append(INDENT).append(newSubSchema.getValue().getSubSchemaName()).append(delimiter);
            }
        }
        
        if (subSchemaMods.hasSubSchema()) {
            buffer.append(calculateSubSchemaAdditions(subSchemaMods.getSubSchema()));
        }
        
        return buffer.toString();
    }
    
    protected void writeReport() 
    throws UpgradeException {
        try {
            String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            String reportFile = baseDir + "/upgrade/" + REPORT_FILENAME + "." + createdDate;
    
            File f = new File(reportFile);
            boolean exist = f.exists();

            // if exists then there has been an error
            if (exist) {
                throw new UpgradeException("File " + f.getName() + " already exist!");
            }

            AMSetupServlet.writeToFile(reportFile, detailedReport);
        } catch (IOException ioe) {
            throw new UpgradeException(ioe.getMessage());
        }
    }
    
    enum ServiceModification {
        ATTR_MOD(" (Updated)"), NEW_SUB_SCHEMA(" (New Sub Schema)"), ATTR_AND_NEW_SUB_SCHEMA(" (Updated & New Sub Schema)");
        
        private final String text;
        
        private ServiceModification(String text) {
            this.text = text;
        }
        
        @Override public String toString() {
            return text;
        }
    }
}
