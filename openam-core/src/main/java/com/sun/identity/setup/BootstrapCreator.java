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
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.ServletContext;

import org.forgerock.openam.core.guice.ServletContextCache;
import org.forgerock.openam.keystore.KeyStoreConfig;
import org.forgerock.openam.setup.BootstrapConfig;
import org.forgerock.openam.setup.ConfigStoreProperties;
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
    // This method was previously used to write the bootstrap file -we now use it to write the boot.json
    private void update(IDSConfigMgr dsCfg)
            throws ConfigurationException, IOException {

        if (!AMSetupServlet.isCurrentConfigurationValid()) {
            return; // dont try to save bootstrap if are not in a valid state
        }

        try {
            String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            // AMKeyProvider amKeyProvider = new AMKeyProvider();

            BootstrapConfig bootConfig = getBootstrapConfig(dsCfg);
            // get the password properties
            String dspw = bootConfig.getDsameUserPassword();
            ConfigStoreProperties p = bootConfig.getConfigStoreList().get(0);
            String configStorepw = p.getDirManagerPassword();

            // do migration if the old bootstrap file is exists.
            File f = new File(baseDir + "/" + BootstrapData.BOOTSTRAP);
            if (f.exists()) { // start migration of legacy bootstrap
                DEBUG.message("Migrating bootstrap");
                // open the old keystore
                AMKeyProvider amKeyProvider = new AMKeyProvider();

                // get the store password(s)
                String keystorePass = new String(amKeyProvider.getKeystorePass());
                String keyPassword = amKeyProvider.getPrivateKeyPass();
                // decode so we can open keystore at boot before instance key is available
                keystorePass = AMKeyProvider.decodePassword(keystorePass);
                keyPassword = AMKeyProvider.decodePassword(keyPassword);

                String dir = new File(amKeyProvider.getKeystoreFilePath()).getParent();
                // create new .storepass and .keypass files to unlock the keystores
                // the .storepass is not encrypted anymore
                File storeFile = new File(dir + "/.storepass");
                File keypassFile = new File(dir + "/.keypass");
                Files.write(storeFile.toPath(), keystorePass.getBytes());
                Files.write(keypassFile.toPath(), keyPassword.getBytes());
                AMSetupServlet.chmodFileReadOnly(storeFile);
                AMSetupServlet.chmodFileReadOnly(keypassFile);

                // if the old keystore is a jks, we need to create a new jceks for the passwords
                // now we can remove the old bootstrap
                if (!f.delete()) {
                    DEBUG.warning("Could not delete the old bootstrap file");
                }
            }
            KeyStoreConfig ksc = bootConfig.getKeyStoreConfig("default");
            AMKeyProvider amKeyProvider = new AMKeyProvider(ksc);
            // add the required boot passwords to the keystore
            // AMKeyProvider amKeyProvider = new AMKeyProvider();
            amKeyProvider.setSecretKeyEntry(BootstrapData.DSAME_PWD_KEY, dspw);
            amKeyProvider.setSecretKeyEntry(BootstrapData.CONFIG_PWD_KEY, configStorepw);
            amKeyProvider.store();

            bootConfig.writeConfig(baseDir + "/boot.json");

        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }


    /**
     * Get the properties needed for the new boot.properties file. The properties are derived from
     * the current running/boostrapped instance.
     *
     * @param dsCfg handle to the dir server config store
     * @return The bootstrap properties
     */
    private BootstrapConfig getBootstrapConfig(IDSConfigMgr dsCfg) throws ConfigurationException {
        BootstrapConfig bootstrap = new BootstrapConfig();

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
        bootstrap.setInstance(SystemProperties.getServerInstanceName());
        bootstrap.setDsameUserPassword(userInstance.getPasswd());
        bootstrap.setDsameUser(userInstance.getAuthID());
        String connDN = svrCfg.getAuthID();
        String connPwd = svrCfg.getPasswd();
        String rootSuffix = svrCfg.getBaseDN();

        Collection serverList = sg.getServersList();
        if (serverList.isEmpty()) {
            throw new ConfigurationException("Server list is empty");
        }

        Iterator iterator = serverList.iterator();
        while (iterator.hasNext()) {
            Server serverObj = (Server) iterator.next();
            ConfigStoreProperties cfg = new ConfigStoreProperties();
            Server.Type connType = serverObj.getConnectionType();
            String proto = (connType.equals(Server.Type.CONN_SIMPLE)) ? "ldap" : "ldaps";
            cfg.setLdapProtocol(proto);
            cfg.setLdapHost(serverObj.getServerName());
            cfg.setLdapPort(serverObj.getPort());
            cfg.setBaseDN(rootSuffix);
            cfg.setDirManagerDN(connDN);
            cfg.setDirManagerPassword(connPwd);
            bootstrap.addConfigStore(cfg);
        }
        bootstrap.addKeystoreConfig("default", createDefaultKeyStoreConfig());

        return bootstrap;
    }

    // Creates a default keystore configuration using a JCEKS keystore
    private KeyStoreConfig createDefaultKeyStoreConfig() {
        String installDir = SystemProperties.get(Constants.AM_INSTALL_DIR);
        KeyStoreConfig ksc = new KeyStoreConfig();

        // If the config store is not up yet, we have to get the context path
        // from the servlet context and construct a default path
        if (installDir == null) {
            ServletContext ctx = ServletContextCache.getStoredContext();
            installDir = SystemProperties.get(SystemProperties.CONFIG_PATH) + ctx.getContextPath();

            ksc.setKeyPasswordFile(installDir + "/.keypass");
            ksc.setKeyStoreFile(installDir + "/keystore.jceks");
            ksc.setKeyStorePasswordFile(installDir + "/.storepass");
            ksc.setKeyStoreType("JCEKS");
        } else {
            // system props are available, get default keystore implementation
            AMKeyProvider amKeyProvider = new AMKeyProvider();
            ksc.setKeyStoreFile(amKeyProvider.getKeystoreFilePath());
            ksc.setKeyStoreType(amKeyProvider.getKeystoreType());
            ksc.setKeyPasswordFile(amKeyProvider.getKeyPasswordFilePath());
            ksc.setKeyStorePasswordFile(amKeyProvider.getKeystorePasswordFilePath());
        }
        return ksc;
    }
}
