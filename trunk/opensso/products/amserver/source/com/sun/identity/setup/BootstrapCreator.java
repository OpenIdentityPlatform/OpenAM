/**
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
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.setup;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.DSConfigMgrBase;
import com.iplanet.services.ldap.IDSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.Server;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.util.XMLException;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.sm.SMSException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;

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
        } catch (XMLException e) {
            throw new ConfigurationException(e.getMessage());
        } catch (SMSException e) {
            throw new ConfigurationException(e.getMessage());
        } catch (SSOException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }
    
    public static void createBootstrap()
        throws ConfigurationException {
        try {
            instance.update(DSConfigMgr.getDSConfigMgr());
        } catch (LDAPServiceException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    private void update(IDSConfigMgr dsCfg)
        throws ConfigurationException {
        try {
            String bootstrapString = getBootStrapURL(dsCfg);
            String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
            String file = baseDir + "/" + BootstrapData.BOOTSTRAP;
            File f = new File(file);
            boolean exist = f.exists();
            boolean writable = exist && f.canWrite();

            // make bootstrap writable if it is not
            if (exist && !writable) {
                f.setWritable(true);
                Thread.sleep(3000);
            }

            AMSetupServlet.writeToFile(file, bootstrapString);

            // not exist means that the product is first configured.
            // set permission to 400
            if (!exist) {
                if (isUnix) {
                    Runtime.getRuntime().exec("/bin/chmod 400 " + file);
                }
            } else {
                // make it not writable if it was previously not writable.
                if (!writable) {
                    f.setWritable(false);
                }
            }
        } catch (InterruptedException e) {
            throw new ConfigurationException(e.getMessage());
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }        

    /**
     * Returns the bootstrap url.
     *
     * @param dsCfg instance of the <code>IDSConfigMgr</code> containing
     *              the connection information to the config store.
     * @exception ConfigurationException if there is an error and cannot
     *     obtain the bootstrap URL. This may be due to connection error.
     */
    public String getBootStrapURL(IDSConfigMgr dsCfg)
        throws ConfigurationException {
        String bootstrapStr = null;
        try {
            ServerGroup sg = dsCfg.getServerGroup("sms");
            ServerGroup defaultGroup = dsCfg.getServerGroup("default") ;
            ServerInstance svrCfg;

            if (sg == null) {
                sg = defaultGroup;
                svrCfg = dsCfg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            } else {
                svrCfg = sg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            }

            ServerInstance userInstance = defaultGroup.getServerInstance(
                LDAPUser.Type.AUTH_ADMIN);
            String dsameUserName = userInstance.getAuthID();
            String dsameUserPwd = JCECrypt.encode(userInstance.getPasswd());

            String connDN = svrCfg.getAuthID();
            String connPwd = JCECrypt.encode(svrCfg.getPasswd());
            String rootSuffix = svrCfg.getBaseDN();

            Collection serverList = sg.getServersList();
            StringBuilder bootstrap = new StringBuilder();

            for (Iterator i = serverList.iterator(); i.hasNext(); ) {
                Server serverObj = (Server)i.next();
                Server.Type connType = serverObj.getConnectionType();
                String proto = (connType.equals(Server.Type.CONN_SIMPLE)) ?
                    "ldap" : "ldaps";
                String url = StringUtils.strReplaceAll(template,
                    "@DS_PROTO@", proto);

                String host = serverObj.getServerName() + ":" +
                    serverObj.getPort();
                url = StringUtils.strReplaceAll(url, "@DS_HOST@", host);
                url = StringUtils.strReplaceAll(url, "@INSTANCE_NAME@",
                    URLEncoder.encode(SystemProperties.getServerInstanceName(),
                    "UTF-8"));
                url = StringUtils.strReplaceAll(url, "@DSAMEUSER_NAME@",
                    URLEncoder.encode(dsameUserName, "UTF-8"));
                url = StringUtils.strReplaceAll(url, "@DSAMEUSER_PWD@",
                    URLEncoder.encode(dsameUserPwd, "UTF-8"));
                url = StringUtils.strReplaceAll(url, "@BASE_DN@",
                    URLEncoder.encode(rootSuffix, "UTF-8"));
                url = StringUtils.strReplaceAll(url, "@BIND_DN@",
                    URLEncoder.encode(connDN, "UTF-8"));
                url = StringUtils.strReplaceAll(url, "@BIND_PWD@",
                    URLEncoder.encode(connPwd, "UTF-8"));
                bootstrap.append(url).append("\n");
            }
            bootstrapStr = bootstrap.toString();
        } catch (IOException e) {
            throw new ConfigurationException(e.getMessage());
        }
        return bootstrapStr;
    }        
}
