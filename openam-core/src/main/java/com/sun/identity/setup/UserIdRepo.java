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
 * $Id: UserIdRepo.java,v 1.21 2009/12/23 00:22:34 goodearth Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.setup;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;

import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.ldap.LdifUtils;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.SimpleBindRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.Options;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.time.Duration;

/**
 * This class does Directory Server related tasks for 
 * OpenAM deployed as single web application.
 */
class UserIdRepo {
    private static final String umSunDSForAM;
    private static final String umSunDSGeneric;
    private static UserIdRepo instance = new UserIdRepo();

    private ConnectionFactory factory;

    static {
        ResourceBundle rb = ResourceBundle.getBundle(
            SetupConstants.PROPERTY_FILENAME);
        umSunDSForAM = rb.getString("umSunDSForAM");
        umSunDSGeneric = rb.getString("umSunDSGeneric");
    }
   
    private UserIdRepo() {
    }
    
    public static UserIdRepo getInstance() {
        return instance;
    }
    
    synchronized void configure(
        Map userRepo, 
        String basedir,
        ServletContext servletCtx,
        SSOToken adminToken
    ) throws Exception {
        factory = null;
        String type = 
            (String) userRepo.get(SetupConstants.USER_STORE_TYPE);
        if (type == null) {
            type = SetupConstants.UM_LDAPv3ForODSEE;
        }

        ResourceBundle rb = ResourceBundle.getBundle(
            SetupConstants.SCHEMA_PROPERTY_FILENAME);
        String configName = "";
        String strFiles = rb.getString(SetupConstants.ODSEE_LDIF);
        if (type.equals(SetupConstants.UM_LDAPv3ForOpenDS)) {
            strFiles = rb.getString(SetupConstants.OpenDS_LDIF);
            configName = "OpenDJ";
        } else if (type.equals(SetupConstants.UM_LDAPv3ForAD)) {
            strFiles = rb.getString(SetupConstants.AD_LDIF);
            configName = "Active Directory";
        } else if (type.equals(SetupConstants.UM_LDAPv3ForADDC)) {
            strFiles = rb.getString(SetupConstants.AD_LDIF);
            configName = "Active Directory with Domain Name";
            type = SetupConstants.UM_LDAPv3ForAD;
        } else if (type.equals(SetupConstants.UM_LDAPv3ForADAM)) {
            strFiles = rb.getString(SetupConstants.ADAM_LDIF);
            configName = "Active Directory Application Mode";
        } else if (type.equals(SetupConstants.UM_LDAPv3ForTivoli)) {
            strFiles = rb.getString(SetupConstants.TIVOLI_LDIF);
            configName = "Tivoli Directory Server";
        }

        loadSchema(userRepo, basedir, servletCtx, strFiles, type);
        addSubConfig(userRepo, type, configName, adminToken);
    }

    private void addSubConfig(
        Map userRepo, 
        String type,
        String configName,
        SSOToken adminToken
    ) throws SMSException, SSOException, IOException {
        String xml = null;
        if (type.equals(SetupConstants.UM_LDAPv3ForODSEE)) {
            xml = getResourceContent(umSunDSForAM);
        } else {
            xml = getResourceContent(umSunDSGeneric);
        }

        if (xml != null) {
            Map data = ServicesDefaultValues.getDefaultValues();
            xml = StringUtils.strReplaceAll(xml, "@SM_CONFIG_ROOT_SUFFIX@",
                XMLUtils.escapeSpecialCharacters((String)data.get(
                    SetupConstants.SM_CONFIG_ROOT_SUFFIX)));
            xml = StringUtils.strReplaceAll(xml, "@UM_CONFIG_ROOT_SUFFIX@",
                XMLUtils.escapeSpecialCharacters((String) userRepo.get(
                    SetupConstants.USER_STORE_ROOT_SUFFIX)));
            xml = StringUtils.strReplaceAll(xml,
                "@" + SetupConstants.UM_DIRECTORY_SERVER + "@",
                XMLUtils.escapeSpecialCharacters(getHost(userRepo)));
            xml = StringUtils.strReplaceAll(xml,
                "@" + SetupConstants.UM_DIRECTORY_PORT + "@",
                XMLUtils.escapeSpecialCharacters(getPort(userRepo)));
            xml = StringUtils.strReplaceAll(xml, "@UM_DS_DIRMGRDN@", 
                XMLUtils.escapeSpecialCharacters(getBindDN(userRepo)));
            xml = StringUtils.strReplaceAll(xml, "@UM_DS_DIRMGRPASSWD@",
                XMLUtils.escapeSpecialCharacters(getBindPassword(userRepo)));

            String s = (String) userRepo.get(SetupConstants.USER_STORE_SSL);
            boolean ssl = "SSL".equals(s);
            xml = StringUtils.strReplaceAll(xml, "@LDAP_CONNECTION_MODE@",
                    ssl ? SetupConstants.LDAP_CONNECTION_MODE_LDAPS : SetupConstants.LDAP_CONNECTION_MODE_LDAP);
            xml = StringUtils.strReplaceAll(xml, "@CONFIG_NAME@", configName);
            xml = StringUtils.strReplaceAll(xml, "@CONFIG_ID@", type);

            registerService(xml, adminToken);
        }
    }
    
    private void registerService(String xml, SSOToken adminSSOToken) 
        throws SSOException, SMSException, IOException {
        ServiceManager serviceManager = new ServiceManager(adminSSOToken);
        InputStream serviceStream = null;
        try {
            serviceStream = (InputStream) new ByteArrayInputStream(
                xml.getBytes());
            serviceManager.registerServices(serviceStream);
        } finally {
            if (serviceStream != null) {
                serviceStream.close();
            }
        }
    }
    
    static ServiceConfig getOrgConfig(SSOToken adminToken) 
        throws SMSException, SSOException {
        ServiceConfigManager svcCfgMgr = new ServiceConfigManager(
            IdConstants.REPO_SERVICE, adminToken);
        ServiceConfig cfg = svcCfgMgr.getOrganizationConfig("", null);
        Map values = new HashMap();
        if (cfg == null) {
            OrganizationConfigManager orgCfgMgr =
                new OrganizationConfigManager(adminToken, "/");
            ServiceSchemaManager schemaMgr = new ServiceSchemaManager(
                IdConstants.REPO_SERVICE, adminToken);
            ServiceSchema orgSchema = schemaMgr.getOrganizationSchema();
            Set attrs = orgSchema.getAttributeSchemas();

            for (Iterator iter = attrs.iterator(); iter.hasNext();) {
                AttributeSchema as = (AttributeSchema) iter.next();
                values.put(as.getName(), as.getDefaultValues());
            }
            cfg = orgCfgMgr.addServiceConfig(IdConstants.REPO_SERVICE,
                values);
        }
        return cfg;
    }
    
    static String getHost(Map userRepo) {
        return (String)userRepo.get(SetupConstants.USER_STORE_HOST);
    }
    
    static String getPort(Map userRepo) {
        return (String)userRepo.get(SetupConstants.USER_STORE_PORT);
    }
    
    static String getBindDN(Map userRepo) {
        return (String) userRepo.get(SetupConstants.USER_STORE_LOGIN_ID);
    }
    
    static String getBindPassword(Map userRepo) {
        return (String) userRepo.get(SetupConstants.USER_STORE_LOGIN_PWD);
    }
    
    private String getADAMInstanceGUID(Map userRepo) throws Exception {
        try (Connection ld = getLDAPConnection(userRepo)) {
            String attrName = "schemaNamingContext";
            ConnectionEntryReader res = ld.search(LDAPRequests.newSearchRequest("", SearchScope.BASE_OBJECT,
                    "(objectclass=*)"));
            if (res.hasNext()) {
                SearchResultEntry entry = res.readEntry();
                    Attribute ldapAttr = entry.getAttribute(attrName);
                    if (ldapAttr != null) {
                        String value = ldapAttr.firstValueAsString();
                        int index = value.lastIndexOf("=");
                        if (index != -1) {
                            return value.substring(index + 1).trim();
                        }
                    }
            }
        }

        return null;
    }

    private void loadSchema(
        Map userRepo, 
        String basedir,
        ServletContext servletCtx,
        String strFiles,
        String type
    ) throws Exception {
        try (Connection conn = getLDAPConnection(userRepo)){
            String dbName = getDBName(userRepo, conn);
            for (String file :  writeSchemaFiles(basedir, dbName, servletCtx, strFiles, userRepo, type)) {
                Object[] params = {file};
                SetupProgress.reportStart("emb.loadingschema", params);
                LdifUtils.createSchemaFromLDIF(file, conn);
                SetupProgress.reportEnd("emb.success", null);

                File f = new File(file);
                f.delete();
            }
        }
    }
    
    private List<String> writeSchemaFiles(
        String basedir, 
        String dbName,
        ServletContext servletCtx,
        String strFiles,
        Map userRepo,
        String type
    ) throws Exception {
        List<String> files = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(strFiles);
        while (st.hasMoreTokens()) {
            String file = st.nextToken();
            InputStreamReader fin = new InputStreamReader(AMSetupUtils.getResourceAsStream(servletCtx, file));
            StringBuilder sbuf = new StringBuilder();
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
            FileWriter fout = null;
            try {
                int idx = file.lastIndexOf("/");
                String absFile = (idx != -1) ? file.substring(idx+1) 
                    : file;
                String outfile = basedir + "/" + absFile;
                fout = new FileWriter(outfile);
                String inpStr = sbuf.toString();
                inpStr = StringUtils.strReplaceAll(inpStr, 
                    "@DB_NAME@", dbName);
                String suffix = (String) userRepo.get(
                    SetupConstants.USER_STORE_ROOT_SUFFIX);
                if (suffix != null) {
                    inpStr = StringUtils.strReplaceAll(inpStr, 
                        "@userStoreRootSuffix@", suffix);
                }
                if (type.equals(SetupConstants.UM_LDAPv3ForADAM)) {
                    String adamInstanceGUID = getADAMInstanceGUID(userRepo);
                    if (adamInstanceGUID != null) {
                        inpStr = StringUtils.strReplaceAll(inpStr, 
                            "@INSTANCE_GUID@", adamInstanceGUID);
                    }
                }
                fout.write(ServicesDefaultValues.tagSwap(inpStr));
                files.add(outfile);
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
            }
        }
        return files;
    }
    
    private String getResourceContent(String resName) 
        throws IOException {
        BufferedReader rawReader = null;
        
        String content = null;

        try {
            rawReader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(resName)));
            StringBuilder buff = new StringBuilder();
            String line = null;

            while ((line = rawReader.readLine()) != null) {
                buff.append(line);
            }

            rawReader.close();
            rawReader = null;
            content = buff.toString();
        } finally {
            if (rawReader != null) {
                rawReader.close();
            }
        }
        return content;
    }
    
    private Connection getLDAPConnection(Map userRepo) throws Exception {
        String userSSLStore = (String) userRepo.get(SetupConstants.USER_STORE_SSL);

        // All connections will use authentication.
        SimpleBindRequest request = LDAPRequests.newSimpleBindRequest(getBindDN(userRepo),
                getBindPassword(userRepo).toCharArray());
        Options options = Options.defaultOptions()
                .set(REQUEST_TIMEOUT, new Duration((long)3, TimeUnit.SECONDS))
                .set(AUTHN_BIND_REQUEST, request);

        if (userSSLStore != null && userSSLStore.equals("SSL")) {
            options = options.set(SSL_CONTEXT, SSLContext.getDefault());
        }

        return getConnectionFactory(getHost(userRepo), Integer.parseInt(getPort(userRepo)), options).getConnection();
    }

    private synchronized ConnectionFactory getConnectionFactory(String hostname, int port, Options options) {
        if (factory == null) {
            factory = new LDAPConnectionFactory(hostname, port, options);
            ShutdownManager.getInstance().addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdown() {
                    if (factory != null) {
                        factory.close();
                    }
                }
            });
        }
        return factory;
    }

    private String getDBName(Map userRepo, Connection ld) throws LdapException {
        String suffix = (String) userRepo.get(SetupConstants.USER_STORE_ROOT_SUFFIX);
        return LDAPUtils.getDBName(suffix, ld);
    }
}
