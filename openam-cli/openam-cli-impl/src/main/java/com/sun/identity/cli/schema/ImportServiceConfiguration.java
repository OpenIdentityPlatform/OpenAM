/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ImportServiceConfiguration.java,v 1.10 2010/01/11 17:34:33 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */
package com.sun.identity.cli.schema;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.util.AMEncryption;
import com.iplanet.services.util.ConfigurableKey;
import com.iplanet.services.util.Crypt;
import com.iplanet.services.util.JCEEncryption;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.InvalidAuthContextException;
import com.sun.identity.common.LDAPUtils;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIUtil;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.InitializeSystem;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.CachedSubEntries;
import com.sun.identity.sm.DirectoryServerVendor;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.sm.ServiceManager;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import javax.security.auth.login.LoginException;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPException;
import com.sun.identity.shared.ldap.util.LDIF;
import java.io.FileReader;

/**
 * Import service configuration data.
 */
public class ImportServiceConfiguration extends AuthenticatedCommand {
    private static final String DS_LDIF = "odsee_config_schema.ldif";
    private static final String DS_IDX = "odsee_config_index.ldif";

    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);

        String xmlFile = getStringOptionValue(IArgument.XML_FILE);
        String encryptSecret = getStringOptionValue(IArgument.ENCRYPT_SECRET);
        try {
            encryptSecret = CLIUtil.getFileContent(getCommandManager(), encryptSecret).trim();
        } catch (CLIException clie) {
            //There is no encryptSecret file
        }
        validateEncryptSecret(xmlFile, encryptSecret);
        
        // disable notification
        SystemProperties.initializeProperties(
            Constants.SMS_ENABLE_DB_NOTIFICATION, "true");
        SystemProperties.initializeProperties(
            "com.sun.am.event.connection.disable.list", "sm,aci,um");

        // disable error debug messsage
        SystemProperties.initializeProperties(
            Constants.SYS_PROPERTY_INSTALL_TIME, "true");

        LDAPConnection ldConnection = null;
        IOutput outputWriter = getOutputWriter();
        try {
            InitializeSystem initSys = CommandManager.initSys;
   
            SSOToken ssoToken = initSys.getSSOToken(getAdminPassword());
            ldConnection = getLDAPConnection();
            
            DirectoryServerVendor.Vendor vendor = 
                DirectoryServerVendor.getInstance().query(ldConnection);
            if (!vendor.name.equals(DirectoryServerVendor.OPENDJ)
                    && !vendor.name.equals(DirectoryServerVendor.OPENDS)
                    && !vendor.name.equals(DirectoryServerVendor.ODSEE)
                    ) {
                throw new CLIException(getResourceString(
                        "import-service-configuration-unknown-ds"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            loadLDIF(vendor, ldConnection);
            String ouServices = "ou=services," + initSys.getRootSuffix();
            
            if (this.isOuServicesExists(ssoToken, ouServices)) {
                System.out.print(getResourceString(
                    "import-service-configuration-prompt-delete") + " ");
                String value = (new BufferedReader(
                    new InputStreamReader(System.in))).readLine();
                value = value.trim();
                if (value.equalsIgnoreCase("y") || value.equalsIgnoreCase("yes")
                ) {
                    outputWriter.printlnMessage(getResourceString(
                        "import-service-configuration-processing"));
                    deleteOuServicesDescendents(ssoToken, ouServices);
                    importData(xmlFile, encryptSecret, ssoToken);
                }
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "import-service-configuration-processing"));
                importData(xmlFile, encryptSecret, ssoToken);
            }
        } catch (SMSException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (LDAPException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (LoginException e) {
            throw new CLIException(
                getCommandManager().getResourceBundle().getString(
                "exception-LDAP-login-failed"), ExitCodes.LDAP_LOGIN_FAILED);
        } catch (InvalidAuthContextException e) {
            throw new CLIException(
                getCommandManager().getResourceBundle().getString(
                "exception-LDAP-login-failed"), ExitCodes.LDAP_LOGIN_FAILED);
        } finally {
            disconnectDServer(ldConnection);
        }
    }
    
    private String getEncKey(String xmlFile)
        throws IOException {
        String encKey = null;
        BufferedReader reader = null;
        
        try {
            reader = new BufferedReader(new FileReader(xmlFile));
 
            String line = reader.readLine();
            String prefix = "<Value>" + Constants.ENC_PWD_PROPERTY + "=";
            while ((line != null) && (encKey == null)) {
                line = line.trim();
                if (line.startsWith(prefix)) {
                    encKey = line.substring(prefix.length());
                    encKey = encKey.substring(0, encKey.indexOf("</Value>"));
                    encKey = SMSSchema.unescapeName(encKey);
                    if (encKey.equals("@AM_ENC_PWD@")) {
                        //don't fall for tagswappable value
                        encKey = null;
                    }
                }
                line = reader.readLine();
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return encKey;
    }
    
    private boolean isOuServicesExists(SSOToken ssoToken, String ouServices)
        throws SMSException, SSOException {
        CachedSubEntries smsEntry = CachedSubEntries.getInstance(
            ssoToken, ouServices);
        Set children = smsEntry.getSubEntries(ssoToken, "*");
        return (children != null) && !children.isEmpty();
    }
    
    private void deleteOuServicesDescendents(
        SSOToken ssoToken, 
        String ouServices
    ) throws SSOException, SMSException {
        CachedSubEntries smsEntry = CachedSubEntries.getInstance(
            ssoToken, ouServices);
        Set children = smsEntry.searchSubOrgNames(ssoToken, "*", false);
        for (Iterator i = children.iterator(); i.hasNext();) {
            String child = (String) i.next();
            child = "o=" + child + "," + ouServices;
            SMSEntry s = new SMSEntry(ssoToken, child);
            s.delete();
        }

        { // hardcoding hidden realm, cannot find a better option.
            SMSEntry s = new SMSEntry(ssoToken,
                "o=sunamhiddenrealmdelegationservicepermissions," +
                ouServices);
            s.delete();
        }

        children = smsEntry.getSubEntries(ssoToken, "*");

        for (Iterator i = children.iterator(); i.hasNext();) {
            String child = (String) i.next();
            child = "ou=" + child + "," + ouServices;
            SMSEntry s = new SMSEntry(ssoToken, child);
            s.delete();
        }

        ServiceManager mgr = new ServiceManager(ssoToken);
        mgr.clearCache();
        AMIdentityRepository.clearCache();
    }

    private void importData(
        String xmlFile, 
        String encryptSecret, 
        SSOToken ssoToken
    ) throws CLIException, SSOException, SMSException, IOException {
        // set the correct password encryption key.
        // without doing so, the default encryption key will be used.
        String encKey = getEncKey(xmlFile);
        if (encKey != null) {
            SystemProperties.initializeProperties(Constants.ENC_PWD_PROPERTY,
                encKey);
            Crypt.reinitialize();
        }
        IOutput outputWriter = getOutputWriter();        
        FileInputStream fis = null;

        try {
            AMEncryption encryptObj = new JCEEncryption();
            ((ConfigurableKey)encryptObj).setPassword(encryptSecret);
            
            ServiceManager ssm = new ServiceManager(ssoToken);
            fis = new FileInputStream(xmlFile);
            ssm.registerServices(fis, encryptObj);
            
            InitializeSystem initSys = CommandManager.initSys;
            String instanceName = initSys.getInstanceName();
            String serverConfigXML = initSys.getServerConfigXML();
            ServerConfiguration.setServerConfigXML(ssoToken, instanceName, 
                serverConfigXML);
            outputWriter.printlnMessage(getResourceString(
                "import-service-configuration-succeeded"));
        } catch (IOException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (Exception e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ie) {
                    //ignore if file input stream cannot be closed.
                }
            }
        }
    }

    private LDAPConnection getLDAPConnection()
        throws CLIException {

        IOutput outputWriter = getOutputWriter();
        if (isVerbose()) {
            outputWriter.printlnMessage(
                getResourceString(
                    "import-service-configuration-connecting-to-ds"));
        }
        
        LDAPConnection ld = null;

        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            ServerGroup sg = dsCfg.getServerGroup("sms");
            if (sg != null) {
                ld = dsCfg.getNewConnection("sms",LDAPUser.Type.AUTH_ADMIN);
            } else  {
                throw new CLIException(
                    getResourceString(
                        "import-service-configuration-not-connect-to-ds"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED, null);
            }
           
            if (isVerbose()) {
                outputWriter.printlnMessage(
                    getResourceString(
                        "import-service-configuration-connected-to-ds"));
            }
            return ld;
        } catch (LDAPServiceException e) {
            throw new CLIException(
                getResourceString(
                    "import-service-configuration-not-connect-to-ds"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED, null);
        }
    }

    private void disconnectDServer(LDAPConnection ld) {
        if ((ld != null) && ld.isConnected()) {
            try {
                ld.disconnect();
            } catch (LDAPException e) {
                debugWarning("cannot discount from directory server", e);
            }
        }
    }

    private void loadLDIF(
        DirectoryServerVendor.Vendor vendor, 
        LDAPConnection ld
    ) throws CLIException {
        DataInputStream ldif = null;
        DataInputStream index = null;

        try {
            String vendorName = vendor.name;
            if (vendorName.equals(DirectoryServerVendor.ODSEE)) {
                ldif = new DataInputStream(
                    getClass().getClassLoader().getResourceAsStream(DS_LDIF));
                index = new DataInputStream(
                    getClass().getClassLoader().getResourceAsStream(DS_IDX));
                LDAPUtils.createSchemaFromLDIF(new LDIF(ldif), ld);
                LDAPUtils.createSchemaFromLDIF(new LDIF(index), ld);
            }
        } catch (LDAPException e) {
            e.printStackTrace();
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED, null);
        } catch (IOException e) {
            throw new CLIException(e.getMessage(),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED, null);
        } finally {
            if (ldif != null) {
                try {
                    ldif.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
            if (index != null) {
                try {
                    index.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
    }

    private void validateEncryptSecret(String xmlFile, String encryptSecret)
        throws CLIException {
        String xml = CLIUtil.getFileContent(getCommandManager(), xmlFile);
        int start = xml.lastIndexOf("<!-- ");
        if (start == -1) {
            throw new CLIException(getResourceString(
                "import-service-configuration-unable-to-locate-hash-secret"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED, null);
        }
        int end = xml.indexOf(" -->", start);
        if (end == -1) {
            throw new CLIException(getResourceString(
                "import-service-configuration-unable-to-locate-hash-secret"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED, null);
        }
        String hashed = xml.substring(start+5, end);
        if (!Hash.hash(encryptSecret).equals(hashed)) {
            throw new CLIException(getResourceString(
                "import-service-configuration-secret-key"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED, null);
        }
    }
}
