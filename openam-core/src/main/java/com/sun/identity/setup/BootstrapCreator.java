/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: BootstrapCreator.java,v 1.14 2009/08/03 23:32:54 veiming Exp $
 *
 * Portions Copyrighted 2011-2015 ForgeRock AS.
 */
package com.sun.identity.setup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Properties;

import org.forgerock.openam.utils.AMKeyProvider;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgrBase;
import com.iplanet.services.ldap.IDSConfigMgr;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.util.XMLException;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

/**
 * This class is responsible for creating bootstrap file based on the
 * information in <code>serverconfig.xml</code>.
 */
public class BootstrapCreator {
    private static BootstrapCreator instance = new BootstrapCreator();
    private static boolean isUnix = 
        System.getProperty("path.separator").equals(":");
    
    static final String template =
        "@DS_PROTO@://@DS_HOST@/@INSTANCE_NAME@" +
        "?user=@DSAMEUSER_NAME@&pwd=@DSAMEUSER_PWD@" +
        "&dsbasedn=@BASE_DN@" +
        "&dsmgr=@BIND_DN@" +
        "&dspwd=@BIND_PWD@" +
        "&ver=1.0";

    private static final Debug DEBUG = Debug.getInstance(SetupConstants.DEBUG_NAME);


    private BootstrapCreator() {
    }

    public static BootstrapCreator getInstance() {
        return instance;
    }
    
    public static void updateBootstrap()
        throws ConfigurationException {
        try {
            DSConfigMgrBase dsCfg = new DSConfigMgrBase();
            dsCfg.parseServiceConfigXML();
            instance.update(dsCfg);
        } catch (XMLException | SMSException | SSOException | IOException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    // This is called on every boot
    // we use this as a hook to migrate the legacy bootstrap to the env var + properties.
    private void update(IDSConfigMgr dsCfg)
            throws ConfigurationException, IOException {
        try {
            String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            Properties bootProps = getBootstrapPropsFile(dsCfg);
            // get the password properties
            String dspw = bootProps.getProperty(BootstrapData.DSAME_PWD_KEY);
            String configStorepw = bootProps.getProperty(BootstrapData.CONFIG_PWD_KEY);

            // do migration if the old bootstrap file is there.
            File f = new File(baseDir + "/" + BootstrapData.BOOTSTRAP);

            if (f.exists()) { // start migration of legacy bootstrap and keystore
                String installDir = SystemProperties.get(Constants.AM_INSTALL_DIR);

                DEBUG.message("Migrating keystore");

                // get the old keystore - using decryption for the storepass
                AMKeyProvider amKeyProvider = new AMKeyProvider(installDir, true);
                char[] keystorePass = amKeyProvider.getKeystorePass();
                String keyPassword = amKeyProvider.getPrivateKeyPass();

                // Migrate the keys...
                // change the path to new top level location
                amKeyProvider.setKeyStoreFilePath(baseDir + "/keystore.jceks");
                amKeyProvider.store();
                // create new .storepass and .keypass files
                AMSetupServlet.createPasswordFiles(baseDir, new String(keystorePass), keyPassword);

                // now we can remove the old bootstrap
                if (!f.delete()) {
                    DEBUG.warning("Could not delete the old bootstrap file");
                }
            }

            // add the required boot passwords to the keystore
            AMKeyProvider amk = new AMKeyProvider(baseDir, false);
            amk.setSecretKeyEntry(BootstrapData.DSAME_PWD_KEY, dspw);
            amk.setSecretKeyEntry(BootstrapData.CONFIG_PWD_KEY, configStorepw);
            amk.store();
            // This writes out the boot.properties file (minus the passwords)
            updateBootProps(baseDir, bootProps);

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    // write out the boot properties to boot.properties
    private void updateBootProps(String baseDir, Properties bootstrapProps) throws IOException {
        String file = baseDir + "/boot.properties";
        // we remove the passwords - because they should now be in the keystore
        // The passwords are in the boot props in order to migrate from the legacy bootstrap file
        bootstrapProps.remove(BootstrapData.DSAME_PWD_KEY);
        bootstrapProps.remove(BootstrapData.CONFIG_PWD_KEY);

        try (OutputStream out = new FileOutputStream(file)) {
            bootstrapProps.store(out, "OpenAM bootstrap properties. These values can be overridden with environment variables");
            out.close();
        }
        catch( IOException e) {
            // write a specific warning to the log, and rethrow the error...
            DEBUG.warning("Could not write out the boot.properties file", e);
        }

    }

    /**
     * Get the properties needed for the new boot.properties file. The properties are derived from
     * the current running/boostrapped instance.
     *
     * @param dsCfg handle to the dir server config store
     * @return The bootstrap properties
     */
    private Properties getBootstrapPropsFile(IDSConfigMgr dsCfg) throws ConfigurationException {
        Properties p = new Properties();

        ServerGroup sg = dsCfg.getServerGroup("sms");
        ServerGroup defaultGroup = dsCfg.getServerGroup("default");
        ServerInstance svrCfg;

        if (sg == null) {
            sg = defaultGroup;
            svrCfg = dsCfg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
        } else {
            svrCfg = sg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
        }

        ServerInstance userInstance = defaultGroup.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
        String dsameUserName = userInstance.getAuthID();
        String dsameUserPwd = userInstance.getPasswd();
        String connDN = svrCfg.getAuthID();
        String connPwd = svrCfg.getPasswd();
        String rootSuffix = svrCfg.getBaseDN();

        Collection serverList = sg.getServersList();

        // we should only have one server...
        if (serverList.isEmpty()) {
            throw new ConfigurationException("Server list is empty");
        }

        // We take the first one.
        // With AM 14, there should only be one server (could be many instances, but one logical server)
        Server serverObj = (Server) serverList.iterator().next();

        Server.Type connType = serverObj.getConnectionType();
        String proto = (connType.equals(Server.Type.CONN_SIMPLE)) ? "ldap" : "ldaps";

        p.setProperty(BootstrapData.ENV_OPENAM_CONFIG_STORE_LDAP_HOST, serverObj.getServerName());
        p.setProperty(BootstrapData.ENV_OPENAM_CONFIG_STORE_LDAP_PORT, "" + serverObj.getPort());
        p.setProperty(BootstrapData.ENV_OPENAM_CONFIG_STORE_LDAP_PROTO, proto);

        p.setProperty(BootstrapData.ENV_OPENAM_INSTANCE, SystemProperties.getServerInstanceName());
        p.setProperty(BootstrapData.ENV_OPENAM_CONFIG_STORE_BASE_DN, rootSuffix);

        p.setProperty(BootstrapData.ENV_OPENAM_DSAME_USER, dsameUserName);

        p.setProperty(BootstrapData.ENV_OPENAM_CONFIG_STORE_DIR_MGR, connDN);

        // include the password props for now. These should not be written out to boot.properties
        p.setProperty(BootstrapData.DSAME_PWD_KEY, dsameUserPwd);
        p.setProperty(BootstrapData.CONFIG_PWD_KEY, connPwd);

        return p;
    }
}
