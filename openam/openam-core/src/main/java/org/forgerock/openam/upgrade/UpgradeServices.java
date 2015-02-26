/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock, Inc. All Rights Reserved
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
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.AMEncryption;
import com.iplanet.services.util.ConfigurableKey;
import com.iplanet.services.util.JCEEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.password.plugins.PasswordGenerator;
import com.sun.identity.password.plugins.RandomPasswordGenerator;
import com.sun.identity.password.ui.model.PWResetException;
import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.BootstrapData;
import com.sun.identity.setup.IHttpServletRequest;
import com.sun.identity.setup.JCECrypt;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchemaModifications;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import org.forgerock.openam.upgrade.helpers.AuthenticationModuleServiceResourceResolutionHelper;
import org.w3c.dom.Document;

/**
 * This is the primary upgrade class that determines the how the services need
 * to be upgraded and performs the upgrade.
 * 
 * @author steve
 */
public class UpgradeServices {
    protected static Debug debug = Debug.getInstance("amUpgrade");
    protected String fileName = null;
    private final static String DEFAULT_PASSWORD = "f7e2lu!l3d";
    protected static final List<String> serviceNames = new ArrayList<String>();
        
    static {
        ResourceBundle rb = ResourceBundle.getBundle(
            SetupConstants.PROPERTY_FILENAME);
        String names = rb.getString(SetupConstants.SERVICE_NAMES);
        StringTokenizer st = new StringTokenizer(names);
        
        while (st.hasMoreTokens()) {
            serviceNames.add(st.nextToken());
        }
    }
    
    public ServiceUpgradeWrapper preUpgrade(SSOToken adminToken) 
    throws UpgradeException {
        createUpgradeDirectories(SystemProperties.get(SystemProperties.CONFIG_PATH));
        return preUpgradeProcessing(adminToken);
    }
    
    public static void createUpgradeDirectories(String baseDir)
    throws UpgradeException {
            String upgradeDir = baseDir + "/upgrade/";
            String backupDir = baseDir + "/backups/";
            
            createDirectory(backupDir);
            createDirectory(upgradeDir);
    }
    
    protected static void createDirectory(String dirName) 
    throws UpgradeException {
        File d = new File(dirName);
            
        if (d.exists() && d.isFile()) {
            throw new UpgradeException("Directory: " + dirName + 
                    " cannot be created as file of the same name already exists");
        }

        if (!d.exists()) {
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message("Created directory: " + dirName);
            }

            d.mkdir();
        } else if (!d.canWrite()) {
            // make bootstrap writable if it is not
            d.setWritable(true);
            
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ie) {
                UpgradeUtils.debug.error("Unexpected thread exception", ie);
                throw new UpgradeException("Unexpected thread exception: " + ie.getMessage());
            }
        }
    }
    
    /**
     * Kick off the upgrade process
     * 
     * @param adminToken A valid admin SSOToken
     * @param serviceChanges changes to be upgraded
     * @throws UpgradeException 
     */
    public void upgrade(SSOToken adminToken, ServiceUpgradeWrapper serviceChanges) 
    throws UpgradeException {            
        if (debug.messageEnabled()) {
            debug.message("Upgrade startup.");
        }
        
        UpgradeProgress.reportStart("upgrade.writingbackup", null);
        writeBackup(adminToken);
        UpgradeProgress.reportEnd("upgrade.success", null);

        UpgradeReport report = null;
        if (serviceChanges != null) {
            try {
                report = new UpgradeReport(adminToken, serviceChanges, false);
                processUpgrade(adminToken, serviceChanges);
            } catch (UpgradeException ue) {
                UpgradeProgress.reportEnd("upgrade.failed", null);
                throw ue;
            }
        }
        
        UpgradeProgress.reportStart("upgrade.writinglog", null);
        report.writeReport();
        UpgradeProgress.reportEnd("upgrade.success", null);
        
        if (debug.messageEnabled()) {
            debug.message("Upgrade complete.");
        }
        
        // reset the version is newer flag
        // reenable this after the CLI upgrade notifies the console correctly
        //AMSetupServlet.isVersionNewer();
        AMSetupServlet.upgradeCompleted();
    }
            
    public String generateDetailedUpgradeReport(SSOToken adminToken, ServiceUpgradeWrapper serviceChanges, boolean html) 
    throws UpgradeException {
        UpgradeReport report = null;
                
        try {
            report = new UpgradeReport(adminToken, serviceChanges, html);
            report.writeReport();
        } catch (UpgradeException ue) {
            UpgradeUtils.debug.error("Unable to generate detailed upgrade report", ue);
            throw ue;
        }
        
        return report.getDetailedReport();
    }
    
    public String generateShortUpgradeReport(SSOToken adminToken, ServiceUpgradeWrapper serviceChanges, boolean html) 
    throws UpgradeException{
        UpgradeReport report = null;
        
        try {
            report = new UpgradeReport(adminToken, serviceChanges, html);
        } catch (UpgradeException ue) {
            UpgradeUtils.debug.error("Unable to generate short upgrade report", ue);
            throw ue;           
        }
        
        return report.getShortReport();
    }
    
    protected void writeBackup(SSOToken adminToken)
    throws UpgradeException {
        FileOutputStream fout = null;
        String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String backupDir = baseDir + "/backups/";
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStamp = dateFormat.format(new Date());
        File backupFile = new File(backupDir + "servicebackup." + dateStamp);
        File backupPasswdFile = new File(backupDir + "servicebackup.password." + dateStamp);
        String backupPassword = generateBackupPassword();
        
        if (backupFile.exists()) {
            debug.error("Upgrade cannot continue as backup file exists! " + backupFile.getName());
            throw new UpgradeException("Upgrade cannot continue as backup file exists");
        }
        
        try {
            fout = new FileOutputStream(backupFile);
            ServiceManager sm = new ServiceManager(adminToken);
            AMEncryption encryptObj = new JCEEncryption();
            ((ConfigurableKey)encryptObj).setPassword(backupPassword);

            String resultXML = sm.toXML(encryptObj);
            resultXML += "<!-- " + Hash.hash(backupPassword) + " -->";
        
            fout.write(resultXML.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException uee) {
            debug.error("Unable to write backup: ", uee);
            throw new UpgradeException("Unable to write backup: "+ uee.getMessage());
        } catch (IOException ioe) {
            debug.error("Unable to write backup: ", ioe);
            throw new UpgradeException("Unable to write backup: " + ioe.getMessage());
        } catch (SSOException ssoe) {
            debug.error("Unable to write backup: ", ssoe);
            throw new UpgradeException("Unable to write backup: " + ssoe.getMessage());
        } catch (SMSException smse) {
            debug.error("Unable to write backup: ", smse);
            throw new UpgradeException("Unable to write backup: " + smse.getMessage());
        } catch (Exception ex) {
            debug.error("Unable to write backup: ", ex);
            throw new UpgradeException("Unable to write backup: " + ex.getMessage());
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException ioe) {
                    //ignored
                }
            }
        }
        
        if (backupPasswdFile.exists()) {
            debug.error("Upgrade cannot continue as backup password file exists! " + backupPasswdFile.getName());
            throw new UpgradeException("Upgrade cannot continue as backup password file exists");
        }
        
        PrintWriter out = null;
        
        try {
            out = new PrintWriter(new FileOutputStream(backupPasswdFile));
            out.println(backupPassword);
            out.flush();
        } catch (IOException ioe) {
            debug.error("Unable to write backup: ", ioe);
            throw new UpgradeException("Unable to write backup: " + ioe.getMessage());
        } catch (Exception ex) {
            debug.error("Unable to write backup: ", ex);
            throw new UpgradeException("Unable to write backup: " + ex.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    /*
     * Determine what needs to be upgraded in the configuration repository
     */
    protected ServiceUpgradeWrapper preUpgradeProcessing(SSOToken adminToken) 
    throws UpgradeException {
        Map map = ServicesDefaultValues.getDefaultValues();
        Map<String, Document> newServiceDefinitions = new HashMap<String, Document>();
        String basedir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String dirXML = basedir + "/config/xml";
        
        File xmlDirs = new File(dirXML);
        
        if (!xmlDirs.exists() || !xmlDirs.isDirectory()) {
            xmlDirs.mkdirs();
            
            if (debug.messageEnabled()) {
                debug.message("Created directory: " + xmlDirs);
            }
        }
        
        // need to reinitialize the tag swap property map with original install params
        IHttpServletRequest requestFromFile = new UpgradeHttpServletRequest(basedir);
        
        try {
            Properties foo = ServerConfiguration.getServerInstance(adminToken, WebtopNaming.getLocalServer());
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_ENCRYPTION_KEY, foo.getProperty(Constants.ENC_PWD_PROPERTY));
            
            String dbOption = (String) requestFromFile.getParameterMap().get(SetupConstants.CONFIG_VAR_DATA_STORE);
            boolean embedded = dbOption.equals(SetupConstants.SMS_EMBED_DATASTORE);
            
            if (!embedded) {
                setUserAndPassword(requestFromFile, basedir);
            }
        } catch (Exception ex) {
            debug.error("Unable to initialise services defaults", ex);
            throw new UpgradeException("Unable to initialise services defaults: " + ex.getMessage());
        }
        
        ServicesDefaultValues.setServiceConfigValues(requestFromFile);

        for (String serviceFileName : serviceNames) {
            boolean tagswap = true;
            
            if (serviceFileName.startsWith("*")) {
                serviceFileName = serviceFileName.substring(1);
                tagswap = false;
            }

            Object[] params = { serviceFileName };
            String strXML = null;
            
            try {
                strXML =
                AuthenticationModuleServiceResourceResolutionHelper.getResourceContent(this.getClass(),serviceFileName);
            } catch (IOException ioe) {
                debug.error("unable to load services file: " + serviceFileName, ioe);
                throw new UpgradeException(ioe);
            }

            // This string 'content' is to avoid plain text password
            // in the files copied to the config/xml directory.
            String content = strXML;
            if ( (strXML == null) || (strXML.length() <= 0) )
            {
                String errorMessage = "Unable to load services file: " + serviceFileName;
                debug.error(errorMessage);
                throw new UpgradeException(errorMessage);
            }
            
            if (tagswap) {
                content = StringUtils.strReplaceAll(content,
                    "@UM_DS_DIRMGRPASSWD@", "********");
                content =
                    ServicesDefaultValues.tagSwap(content, true);
            }
            
            if (tagswap) {
                strXML = ServicesDefaultValues.tagSwap(strXML, true);
            }
            
            Document serviceSchema = null;
            
            try {
                serviceSchema = fetchDocumentSchema(strXML, adminToken);
            } catch (IOException ioe) {
                debug.error("unable to load document schema", ioe);
                throw new UpgradeException(ioe);
            }
            
            newServiceDefinitions.put(UpgradeUtils.getServiceName(serviceSchema), serviceSchema);
        }
        
        return diffServiceVersions(newServiceDefinitions, adminToken);
    }
    
    protected void processUpgrade(SSOToken adminToken, ServiceUpgradeWrapper serviceChanges) 
    throws UpgradeException {
        UpgradeProgress.reportStart("upgrade.upgradeservices", null);
        UpgradeProgress.reportEnd("upgrade.blank", null);
        
        if (serviceChanges.getServicesAdded() != null && 
            !serviceChanges.getServicesAdded().isEmpty()) {

            StringBuilder buffer = new StringBuilder();
            
            if (debug.messageEnabled()) {
                buffer.append("services to add: ");
            }
            
            for (Map.Entry<String, Document> serviceToAdd : serviceChanges.getServicesAdded().entrySet()) {
                StringBuilder serviceDefinition = new StringBuilder();
                serviceDefinition.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
                serviceDefinition.append("<!DOCTYPE ServicesConfiguration\n");
                serviceDefinition.append("PUBLIC \"=//iPlanet//Service Management Services (SMS) 1.0 DTD//EN\"\n");
                serviceDefinition.append("\"jar://com/sun/identity/sm/sms.dtd\">\n");
                serviceDefinition.append(XMLUtils.print(serviceToAdd.getValue()));
                Object[] params = { serviceToAdd.getKey() };
                UpgradeProgress.reportStart("upgrade.addservice", params);
                UpgradeUtils.createService(serviceDefinition.toString(), adminToken);
                UpgradeProgress.reportEnd("upgrade.success", null);
                
                if (debug.messageEnabled()) {
                    buffer.append(serviceToAdd.getKey()).append(": ");
                }
            }
            
            if (debug.messageEnabled()) {
                debug.message("services to add: " + buffer.toString());
            }
        }
        
        if (serviceChanges.getServicesModified() != null &&
            !serviceChanges.getServicesModified().isEmpty()) {
            for (Map.Entry<String, Map<String, ServiceSchemaUpgradeWrapper>> serviceToModify : serviceChanges.getServicesModified().entrySet()) {
                Object[] params = { serviceToModify.getKey() };
                UpgradeProgress.reportStart("upgrade.modservice", params);
                UpgradeUtils.modifyService(serviceToModify.getKey(), serviceToModify.getValue(), adminToken);
                UpgradeProgress.reportEnd("upgrade.success", null);
                
                if (debug.messageEnabled()) {
                    debug.message("modified service: " + serviceToModify.getKey());
                }
            }
        }
        
        if (serviceChanges.getSubSchemasModified() != null &&
            !serviceChanges.getSubSchemasModified().isEmpty()) {
            for (Map.Entry<String, Map<String, SubSchemaUpgradeWrapper>> ssMod : serviceChanges.getSubSchemasModified().entrySet()) {
                Object[] params = { ssMod.getKey() };
                UpgradeProgress.reportStart("upgrade.addsubschema", params);
                UpgradeUtils.addNewSubSchemas(ssMod.getKey(), ssMod.getValue(), adminToken);
                UpgradeProgress.reportEnd("upgrade.success", null);
                
                if (debug.messageEnabled()) {
                    debug.message("modified sub schema: " + ssMod.getKey());
                }
            }
        }
        
        if (serviceChanges.getServicesDeleted() != null &&
            !serviceChanges.getServicesDeleted().isEmpty()) {
            for (String serviceToDelete : serviceChanges.getServicesDeleted()) {
                Object[] params = { serviceToDelete };
                UpgradeProgress.reportStart("upgrade.delservice", params);
                UpgradeUtils.deleteService(serviceToDelete, adminToken);
                UpgradeProgress.reportEnd("upgrade.success", null);
                
                if (debug.messageEnabled()) {
                    debug.message("deleted service: " + serviceToDelete);
                }
            }
        }
        
        try {
            UpgradeProgress.reportStart("upgrade.platformupdate", null);
            ServerConfiguration.upgradeDefaults(adminToken, false, false);
            UpgradeProgress.reportEnd("upgrade.success", null);
        } catch (SSOException ssoe) {
            debug.error("Unable to process service configuration upgrade", ssoe);
        } catch (SMSException smse) {
            debug.error("Unable to process service configuration upgrade", smse);
        } catch (UnknownPropertyNameException upne) {
            debug.error("Unable to process service configuration upgrade", upne);
        }
    }
    
    protected ServiceUpgradeWrapper diffServiceVersions(Map<String, Document> serviceDefinitions, SSOToken adminToken) 
    throws UpgradeException {
        ServiceSchemaModifications modifications = null;
        Set<String> newServiceNames = 
                listNewServices(serviceDefinitions.keySet(), UpgradeUtils.getExistingServiceNames(adminToken));
        
        Map<String, Document> sAdd = new HashMap<String, Document>();
        
        for (String newServiceName : newServiceNames) {
            sAdd.put(newServiceName, serviceDefinitions.get(newServiceName));
            
            if (debug.messageEnabled()) {
                debug.message("found new service: " + newServiceName);
            }
        }
                
        Map<String, Map<String, ServiceSchemaUpgradeWrapper>> sMod = 
                new HashMap<String, Map<String, ServiceSchemaUpgradeWrapper>>();
        Map<String, Map<String, SubSchemaUpgradeWrapper>> ssMod =
                new HashMap<String, Map<String, SubSchemaUpgradeWrapper>>();
        Set<String> deletedServices = listDeletedServices(UpgradeUtils.getExistingServiceNames(adminToken));
        
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
            
            if (modifications.isServiceModified()) {
                sMod.put(service.getKey(), modifications.getServiceModifications());
            }
            
            if (modifications.hasSubSchemaChanges()) {
                ssMod.put(service.getKey(), modifications.getSubSchemaChanges());
            }
        }
        
        ServiceUpgradeWrapper serviceWrapper = new ServiceUpgradeWrapper(sAdd, sMod, ssMod, deletedServices);
        return serviceWrapper;
    }    
    
    protected Set<String> listNewServices(Set<String> serviceNames, Set<String> existingServices) {
        Set<String> newServiceNames = new HashSet<String>(serviceNames);
        
        if (newServiceNames.removeAll(existingServices)) {
            return newServiceNames;
        }
        
        return Collections.EMPTY_SET;
    }
    
    protected Set<String> listDeletedServices(Set<String> existingServices)
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
    
    protected Document fetchDocumentSchema(String xmlContent, SSOToken adminToken) 
    throws UpgradeException, IOException {
        InputStream serviceStream = null;
        Document doc = null;
        
        try {
            serviceStream = (InputStream) new ByteArrayInputStream(xmlContent.getBytes());
            doc = UpgradeUtils.parseServiceFile(serviceStream, adminToken);
        } finally {
            if (serviceStream != null) {
                serviceStream.close();
            }
        }
        
        return doc;
    }
    


    private String generateBackupPassword() {
        PasswordGenerator passwordGenerator = new RandomPasswordGenerator();
        String password = null;
        
        try {
            password = passwordGenerator.generatePassword(null);
        } catch (PWResetException pre) {
            // default implementation will not do this
            password = DEFAULT_PASSWORD;
        }
        
        return password;
    }
    
    private void setUserAndPassword(IHttpServletRequest requestFromFile, String basedir) throws UpgradeException {
        try {
            BootstrapData bootStrap = new BootstrapData(basedir);
            Map<String, String> data = bootStrap.getDataAsMap(0);
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_DS_MGR_DN, data.get(BootstrapData.DS_MGR));
            requestFromFile.addParameter(SetupConstants.CONFIG_VAR_DS_MGR_PWD,
                    JCECrypt.decode(data.get(BootstrapData.DS_PWD)));
        } catch (IOException ioe) {
            debug.error("Unable to load directory user/password from bootstrap file", ioe);
            throw new UpgradeException("Unable to load bootstrap file: " + ioe.getMessage());
        }
    }
}
