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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
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

import org.apache.commons.lang.StringUtils;
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
    // todo: Rewriting the bootstrap on every boot is part of the legacy OpenAM behaviour.
    // This is needed to capture password changes that might have occurred to update the bootstrap file.
    // This is kludgy and should be revisited in the future once AME-12525 and OPENAM-9900 are resolved
    private void update(IDSConfigMgr dsCfg)
            throws ConfigurationException, IOException {

        if (!AMSetupServlet.isCurrentConfigurationValid()) {
            return; // dont try to save bootstrap if we are not in a valid state
        }
       
        try {
            final String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            final BootstrapConfig bootConfig = getBootstrapConfig(dsCfg);
            // get the passwords
            final String dspw = bootConfig.getDsameUserPassword();
            final ConfigStoreProperties p = bootConfig.getConfigStoreList().get(0);
            final String configStorepw = p.getDirManagerPassword();
            final KeyStoreConfig ksc = bootConfig.getKeyStoreConfig("default");

            // do migration if the old bootstrap file is exists.
            final File bootstrap = new File(baseDir + "/" + BootstrapData.BOOTSTRAP);
            final boolean doMigrate = bootstrap.exists();
            if (doMigrate) { // start migration of legacy bootstrap
                migrateBootstrap(ksc);
            }
            // write the required boot passwords to the keystore
        	final AMKeyProvider amKeyProvider = new AMKeyProvider(ksc);
        	try {
	        	if (!StringUtils.equals(dspw,amKeyProvider.getSecret(BootstrapData.DSAME_PWD_KEY)) || !StringUtils.equals(configStorepw,amKeyProvider.getSecret(BootstrapData.CONFIG_PWD_KEY))) {
	        		throw new KeyStoreException("need rewrite keys");
	        	}
        	}catch (KeyStoreException ke) {
        		try {
        			amKeyProvider.setSecretKeyEntry(BootstrapData.DSAME_PWD_KEY, dspw);
        			amKeyProvider.setSecretKeyEntry(BootstrapData.CONFIG_PWD_KEY, configStorepw);
                  	amKeyProvider.store();
                  }catch (Exception e) {
      				DEBUG.warning("save {} {}",BootstrapData.CONFIG_PWD_KEY, e.toString());
      			}
			}
        	if (SystemProperties.get("org.forgerock.donotupgrade")!=null && new File(baseDir + "/boot.json").exists())
            	return;
        	
            bootConfig.writeConfig(baseDir + "/boot.json");
            // We delay deletion of legacy bootstrap until the very end.
            // If there are exceptions, this will leave the bootstrap in place
            // and make the system stil bootable.
            if (doMigrate) {
                bootstrap.delete();
            }

        } catch (IOException | KeyStoreException  e) {
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

    /**
     * Migrate the old Bootstrap file
     */
    private void migrateBootstrap(KeyStoreConfig ksc)
            throws IOException, KeyStoreException {
        DEBUG.message("Migrating bootstrap");

        // get the store password(s)
        String keystorePass = new String(ksc.getKeyStorePassword());
        String keyPassword = new String(ksc.getKeyPassword());

        // decode so we can open keystore at boot before instance key is available
        keystorePass = AMKeyProvider.decodePassword(keystorePass);
        keyPassword = AMKeyProvider.decodePassword(keyPassword);

        // the .storepass is not encrypted anymore
        File storeFile = new File(ksc.getKeyStorePasswordFile());
        File keypassFile = new File(ksc.getKeyPasswordFile());
        // Remove existing files as they may be readonly and the write will fail
        if (!storeFile.delete() || !keypassFile.delete()) {
            // Subsequent updates to the these files may still work, so don't give up yet
            DEBUG.warning("Could not delete .storepass / .keypass in migration process. Migration may fail");
        }
        // Save the keystore passwords unencrypted
        Files.write(storeFile.toPath(), keystorePass.getBytes());
        Files.write(keypassFile.toPath(), keyPassword.getBytes());
        AMSetupServlet.chmodFileReadOnly(storeFile);
        AMSetupServlet.chmodFileReadOnly(keypassFile);
    }

    // Creates a default keystore configuration
    private KeyStoreConfig createDefaultKeyStoreConfig() {
        KeyStoreConfig ksc = new KeyStoreConfig();
        // Get the default keystore implementation
        AMKeyProvider amKeyProvider = new AMKeyProvider();
        ksc.setKeyPasswordFile(amKeyProvider.getKeyPasswordFilePath());
        ksc.setKeyStorePasswordFile(amKeyProvider.getKeystorePasswordFilePath());
        ksc.setKeyStoreFile(amKeyProvider.getKeystoreFilePath());
        ksc.setKeyStoreType(amKeyProvider.getKeystoreType());

        // If a legacy JKS keystore is the default, we need to use a newer
        // jceks. The old JKS can not store passwords.
        // keystore.jceks should get created as part of the upgrade process.
        // Note that this KeyStoreConfig can not be directly opened at this point
        // because the storepass might still be encrypted
        // The migrateBootstrap() above wil fix up the storepass
        if (amKeyProvider.getKeystoreType().toLowerCase().equals("jks")) {
            // install dir is sometimes not available on first boot
            String dir = SystemProperties.get(Constants.AM_INSTALL_DIR);
            if (dir == null) {
                //use the current keystore path. It is very likely the user has not changed this
                dir = amKeyProvider.getKeystoreFilePath();
                ksc.setKeyStoreFile(dir.replace(".jks", ".jceks"));
            } else {
                ksc.setKeyStoreFile(dir + "/keystore.jceks");
            }
            ksc.setKeyStoreType("JCEKS");
        }
        return ksc;
    }
}
