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
 * $Id: BootstrapData.java,v 1.16 2009/05/05 21:24:47 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock AS
 */

package com.sun.identity.setup;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.util.Crypt;
import com.sun.identity.shared.StringUtils;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import com.sun.identity.shared.ldap.util.DN;
import org.forgerock.openam.upgrade.DirectoryContentUpgrader;
import org.forgerock.openam.upgrade.UpgradeException;

public class BootstrapData {
    private List data = new ArrayList();
    
    final static String BOOTSTRAP = "bootstrap";
    
    static final String PROTOCOL = "protocol";
    static final String SERVER_INSTANCE = "serverinstance";
    static final String FF_BASE_DIR = "ffbasedir";
    static final String BASE_DIR = "basedir";
    
    public static final String DS_HOST = "dshost";
    public static final String DS_PORT = "dsport";
    public static final String DS_PROTOCOL = "dsprotocol";
    public static final String DS_PWD = "dspwd";
    public static final String DS_MGR = "dsmgr";
    public static final String DS_BASE_DN = "dsbasedn";
    public static final String DS_REPLICATIONPORT = "dsreplport";
    public static final String DS_REPLICATIONPORT_AVAILABLE = "dsreplportavailable";
    public static final String DS_ISEMBEDDED = "dsisembedded";
    public static final String ENCKEY = "enckey";

    static final String DS_PROTO_TYPE = "dsprototype";

    static final String USER = "user";
    static final String PWD = "pwd";
    static final String PROT_FILE = "file";
    static final String PROT_LDAP = "ldap";
    static final String PROTOCOL_FILE = "file://";
    static final String PROTOCOL_LDAP = "ldap://";

    static final String PROTOCOL_LDAPS = "ldaps://";
    static final String DS_PROTO_LDAPS = "SSL";
    static final String DS_PROTO_LDAP = "SIMPLE";

    private static final String BOOTSTRAPCONFIG = "bootstrapConfig.properties";
    
    private String basedir;
    private String dsameUser;
    private String dsuserbasedn;
    private String dsbasedn;
    private String dsameUserPwd;
    private String instanceName;

    /**
     * Creates an instance of this class
     *
     * @param basedir Base Directory of the installation.
     * @throws IOException if cannot read the file.
     */
    public BootstrapData(String basedir) 
        throws IOException {
        this.basedir = basedir;
        readFile(basedir + "/" + BOOTSTRAP);
    }
    
    public BootstrapData(Map mapConfig)
        throws UnsupportedEncodingException {
        data.add(createBootstrapResource(mapConfig, false));
    }

    /**
     * Returns list of bootstrap data.
     *
     * @return list of bootstrap data.
     */
    public List getData() {
        return data;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getUserBaseDN() {
        return dsuserbasedn;
    }

    public String getBaseDN() {
        return dsbasedn;
    }

    public String getDsameUserPassword() {
        return dsameUserPwd;
    }

    /**
      * Gets attributes in a given row as a <code>Map</code>.
      * @param idx row (starting with 0)
      * @return Map of attributes
      */
    public Map getDataAsMap(int idx) 
           throws MalformedURLException, UnsupportedEncodingException
    {
        String  info = (String) data.get(idx);
        // need to do this because URL class does not understand ldap://
        String dsprotocol = "unknown";
        if (info.startsWith(BootstrapData.PROTOCOL_LDAPS)) {
            info = "http://" +  info.substring(8);
            dsprotocol = "ldaps";
        } else
        if (info.startsWith(BootstrapData.PROTOCOL_LDAP)) {
            info = "http://" +  info.substring(7);
            dsprotocol = "ldap";
        }
        URL url = new URL(info);
        Map mapQuery = queryStringToMap(url.getQuery());
        String dshost = url.getHost();
        mapQuery.put(DS_HOST, dshost);
        String dsport = Integer.toString(url.getPort());
        mapQuery.put(DS_PORT, dsport);
        mapQuery.put(DS_PROTOCOL, dsprotocol);
        return mapQuery;
    }

    public void initSMS(boolean startDS) 
        throws UnsupportedEncodingException, LDAPServiceException,
        MalformedURLException {
        String serverConfigXML = getServerConfigXML(false);
        Properties prop = getBootstrapProperties();
        SystemProperties.initializeProperties(prop, true);
        Crypt.reinitialize();
        loadServerConfigXML(serverConfigXML);
        
        if (startDS) {
            startEmbeddedDS(basedir + AMSetupServlet.OPENDS_DIR);
            if (AMSetupServlet.isOpenDJUpgraded()) {
                try {
                    new DirectoryContentUpgrader(basedir, dsbasedn).upgrade();
                } catch (UpgradeException ue) {
                    throw new IllegalStateException("An error occurred while upgrading directory content", ue);
                }
            }
        } else {
            EmbeddedOpenDS.initializeForClientUse();
        }
    }

    private Properties getBootstrapProperties() {
        Properties prop = new Properties();
        try {
            prop.load(
                getClass().getClassLoader().getResourceAsStream(BOOTSTRAPCONFIG)
                );
            for (Enumeration e = prop.propertyNames(); e.hasMoreElements(); ) {
                String name = (String)e.nextElement();
                String property = prop.getProperty(name);
                property = StringUtils.strReplaceAll(property, "@DS_BASE_DN@",
                    dsbasedn);
                // Replace DSAMEUSER
                property = StringUtils.strReplaceAll(property, "@DSAMEUSER@",
                    dsameUser);
                prop.setProperty(name, property);
            }
        } catch (IOException e) {
            //ignore because bootstrapConfig.properties is always bundled.
            e.printStackTrace();
        }
        return prop;
    }
    
    private static void startEmbeddedDS(String odsDir) {
        File odsDirFile = new File(odsDir);

        if (odsDirFile.exists()) {
            if (!EmbeddedOpenDS.isStarted()) {
                try {
                    SetupProgress.reportStart("emb.startemb", null);
                    EmbeddedOpenDS.startServer(odsDir);
                    SetupProgress.reportEnd("emb.success", null);
                } catch (Exception ex) {
                    //ignore, it maybe started.
                	System.out.println("BootstrapData.startEmbeddedDS: "+ex);
                }
            }
        }
    }
    
    static void loadServerConfigXML(String xml)
        throws LDAPServiceException {
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(xml.getBytes());
            DSConfigMgr.initInstance(bis);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }
    
    /**
     * Returns server configuration XML. It is generated from bootstrap file. 
     * @param bCrypt <code>true</code> to decrypt the password with default key
     *        and encrypt it with the key defined in 
     *        <code>AMConfig.properties</code>.
     * @return server configuration XML.
     * @throws UnsupportedEncodingException if XML encoding is incorrect.
     * @throws MalformedURLException if bootstrap URL is not well formed.
     */
    public String getServerConfigXML(boolean bCrypt)
        throws UnsupportedEncodingException, MalformedURLException {
        boolean first = true;
        StringBuilder buff = new StringBuilder();
        buff.append(BOOTSTRAP_SERVER_START_TAG);
        String serverBlob = null;
        StringBuilder serverBuff = new StringBuilder();
        int counter = 1;
        
        for (Iterator i = data.iterator(); i.hasNext(); ) {
            String info = (String)i.next();
            boolean ldaps = false;
            // need to do this because URL class does not understand ldap://
            if (info.startsWith(BootstrapData.PROTOCOL_LDAPS)) {
                info = "http://" +  info.substring(8);
                ldaps = true;
            } else
            if (info.startsWith(BootstrapData.PROTOCOL_LDAP)) {
                info = "http://" +  info.substring(7);
            }
            URL url = new URL(info);
            if (first) {
                buff.append(getServerConfigXMLUserBlob(url, ldaps, bCrypt));
                serverBlob = getServerConfigXMLServerBlob(url, ldaps, bCrypt);
                first = false;
            }
            serverBuff.append(getServerEntryXMLBlob(url, ldaps, counter++));
        }

        String servers = StringUtils.strReplaceAll(serverBlob, "@SERVER_ENTRY@",
            serverBuff.toString());

        buff.append(servers);
        buff.append(BOOTSTRAP_SERVER_END_TAG);
        return buff.toString();
    }

    private String getServerConfigXMLUserBlob(
        URL url, 
        boolean ldaps, 
        boolean bCrypt
    ) throws UnsupportedEncodingException {
        Map mapQuery = queryStringToMap(url.getQuery());
        String dshost = url.getHost();
        String dsport = Integer.toString(url.getPort());
        String dsmgr = (String)mapQuery.get(DS_MGR);
        String dspwd = (String)mapQuery.get(DS_PWD);
        String pwd = (String)mapQuery.get(PWD);

        dsameUser = (String)mapQuery.get(USER);
        dsbasedn = (String)mapQuery.get(DS_BASE_DN);
        instanceName = URLDecoder.decode(url.getPath(), "UTF-8");
        dsameUserPwd = (String)mapQuery.get(BootstrapData.PWD);

        if (bCrypt) {
            pwd = JCECrypt.decode(pwd);
            pwd = Crypt.encode(pwd);
            
            dspwd = JCECrypt.decode(dspwd);
            dspwd = Crypt.encode(dspwd);
        }
        
        // Check if dsameuser is set
        if ((dsameUser == null) || (dsameUser.length() == 0)) {
            dsameUser = "cn=dsameuser,ou=DSAME Users," + dsbasedn;
            dsuserbasedn = dsbasedn;
        } else {
            // Obtain user base dn from dsamaeUser
            DN dsameUserDn = new DN(dsameUser);
            DN userBaseDN = dsameUserDn.getParent().getParent();
            dsuserbasedn = userBaseDN.toRFCString();
        }
        
        if (instanceName.startsWith("/")) {
            instanceName = instanceName.substring(1);
        }

        String template= BOOTSTRAP_SERVER_CONFIG_USER;

        if (ldaps) {
            template = StringUtils.strReplaceAll(template,
                "@" + DS_PROTO_TYPE + "@", DS_PROTO_LDAPS);
        } else {
            template = StringUtils.strReplaceAll(template,
                "@" + DS_PROTO_TYPE + "@", DS_PROTO_LDAP);
        }

        template = StringUtils.strReplaceAll(template,
            "@" + DS_HOST + "@", XMLUtils.escapeSpecialCharacters(dshost));
        template = StringUtils.strReplaceAll(template,
            "@" + DS_PORT + "@", dsport);
        template = StringUtils.strReplaceAll(template,
            "@" + DS_MGR + "@", XMLUtils.escapeSpecialCharacters(dsmgr));
        template = StringUtils.strReplaceAll(template,
            "@" + USER + "@", XMLUtils.escapeSpecialCharacters(dsameUser));
        template = StringUtils.strReplaceAll(template,
            "@" + DS_PWD + "@", XMLUtils.escapeSpecialCharacters(dspwd));
        template = StringUtils.strReplaceAll(template,
            "@" + PWD + "@", XMLUtils.escapeSpecialCharacters(pwd));
        template = StringUtils.strReplaceAll(template,
            "@" + DS_BASE_DN + "@", 
            XMLUtils.escapeSpecialCharacters(dsuserbasedn));
        return template;
    }
    
    public static Map queryStringToMap(String str)
        throws UnsupportedEncodingException {
        Map map = new HashMap();
        StringTokenizer st = new StringTokenizer(str, "&");

        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            int idx = s.indexOf('=');
            map.put(s.substring(0, idx), URLDecoder.decode(
                s.substring(idx +1), "UTF-8"));
        }
        return map;
    }

    private String getServerConfigXMLServerBlob(
        URL url, 
        boolean ldaps, 
        boolean bCrypt
    ) throws UnsupportedEncodingException {
        Map mapQuery = queryStringToMap(url.getQuery());
        String pwd = (String)mapQuery.get(BootstrapData.PWD);
        String dshost = url.getHost();
        String dsport = Integer.toString(url.getPort());
        dsbasedn = (String)mapQuery.get(DS_BASE_DN);
        String dsmgr = (String)mapQuery.get(DS_MGR);
        String dspwd = (String)mapQuery.get(DS_PWD);
        String template = BOOTSTRAP_SERVER_CONFIG_LDAP_SVR;
        
        if (bCrypt) {
            pwd = JCECrypt.decode(pwd);
            pwd = Crypt.encode(pwd);
            
            dspwd = JCECrypt.decode(dspwd);
            dspwd = Crypt.encode(dspwd);
        }

        if (ldaps) {
            template = StringUtils.strReplaceAll(template,
                "@" + DS_PROTO_TYPE + "@", DS_PROTO_LDAPS);
        } else {
            template = StringUtils.strReplaceAll(template,
                "@" + DS_PROTO_TYPE + "@", DS_PROTO_LDAP);
        }

        template = StringUtils.strReplaceAll(template, "@" + DS_HOST + "@", 
            XMLUtils.escapeSpecialCharacters(dshost));
        template = StringUtils.strReplaceAll(template, "@" + DS_PORT + "@",
            dsport);
        template = StringUtils.strReplaceAll(template, "@" + DS_MGR + "@", 
            XMLUtils.escapeSpecialCharacters(dsmgr));
        template = StringUtils.strReplaceAll(template, "@" + DS_BASE_DN + "@", 
            XMLUtils.escapeSpecialCharacters(dsbasedn));
        template = StringUtils.strReplaceAll(template,
            "@" + DS_PWD + "@", XMLUtils.escapeSpecialCharacters(dspwd));
        template = StringUtils.strReplaceAll(template, "@" + PWD + "@", 
            XMLUtils.escapeSpecialCharacters(pwd));
        return template;    
    }

    private String getServerEntryXMLBlob(URL url, boolean ldaps, int counter) 
        throws UnsupportedEncodingException {
        Map mapQuery = queryStringToMap(url.getQuery());
        String dshost = url.getHost();
        String dsport = Integer.toString(url.getPort());

        String template = BOOTSTRAP_SERVER_CONFIG_LDAP_SVR_ENTRY;
        if (ldaps) {
            template = StringUtils.strReplaceAll(template,
                "@" + DS_PROTO_TYPE + "@", DS_PROTO_LDAPS);
        } else {
            template = StringUtils.strReplaceAll(template,
                "@" + DS_PROTO_TYPE + "@", DS_PROTO_LDAP);
        } 
        template = StringUtils.strReplaceAll(template, "@" + DS_HOST + "@",
            dshost);
        template = StringUtils.strReplaceAll(template, "@" + DS_PORT + "@",
            dsport);
        template = StringUtils.strReplaceAll(template, "@counter@",
            Integer.toString(counter));
        return template;
    }


    static String create(
        String bootstrapFile, 
        Map configuration, 
        boolean legacy
    ) throws IOException {
        File btsFile = new File(bootstrapFile);
        if (!btsFile.getParentFile().exists()) {
            btsFile.getParentFile().mkdirs();
        }
        return createBootstrapResource(configuration, legacy);
    }

    static String createBootstrapResource(Map configuration, boolean legacy)
        throws UnsupportedEncodingException
    {
        if (legacy) {
            return (String) configuration.get(FF_BASE_DIR);
        }

        String protocol = (String)configuration.get(PROTOCOL);
        // remove ://
        protocol = protocol.substring(0, protocol.length() -3);
        String pwd = JCECrypt.encode((String) configuration.get(PWD));
        String serverInstance = (String) configuration.get(
            SERVER_INSTANCE);

        String url = BootstrapCreator.template;
        url = StringUtils.strReplaceAll(url, "@DS_PROTO@", protocol);

        String dsHost = (String)configuration.get(DS_HOST) + ":" +
            (String)configuration.get(DS_PORT);
        url = StringUtils.strReplaceAll(url, "@DS_HOST@", dsHost);
        url = StringUtils.strReplaceAll(url, "@INSTANCE_NAME@",
            URLEncoder.encode(serverInstance, "UTF-8"));
        url = StringUtils.strReplaceAll(url, "@DSAMEUSER_PWD@",
            URLEncoder.encode(pwd, "UTF-8"));
        url = StringUtils.strReplaceAll(url, "@BASE_DN@",
            URLEncoder.encode((String)configuration.get(DS_BASE_DN), "UTF-8"));
        url = StringUtils.strReplaceAll(url, "@BIND_DN@",
            URLEncoder.encode((String)configuration.get(DS_MGR), "UTF-8")); 
        url = StringUtils.strReplaceAll(url, "@BIND_PWD@",
            URLEncoder.encode(
                JCECrypt.encode((String) configuration.get(DS_PWD)), "UTF-8"));
        return url;
    }

    private void readFile(String file) 
        throws IOException
    {
        BufferedReader in = null;
        
        try {
            in = new BufferedReader(new FileReader(file));
            if (in.ready()) {
                String str = in.readLine();

                while (str != null) {
                    str = str.trim();
                    if ((str.length() > 0) && !str.startsWith("#")) {
                        data.add(str);
                    }
                    str = in.readLine();
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    private static final String BOOTSTRAP_SERVER_START_TAG =
        "<iPlanetDataAccessLayer>";
    private static final String BOOTSTRAP_SERVER_END_TAG =
        "</iPlanetDataAccessLayer>";
    private static final String BOOTSTRAP_SERVER_CONFIG_USER =
        "<ServerGroup name=\"default\" minConnPool=\"1\" maxConnPool=\"1\">" +
        "<Server name=\"Server1\" host=\"@" + DS_HOST + "@\" " +
        "port=\"@" + DS_PORT + "@\" type=\"@" + DS_PROTO_TYPE + "@\" />" +
        "<User name=\"User1\" type=\"admin\">" +
        "<DirDN>@" + USER + "@</DirDN>" +
        "<DirPassword>@" + PWD + "@</DirPassword>" +
        "</User>" +
        "<BaseDN>@" + DS_BASE_DN + "@</BaseDN>" +
        "</ServerGroup>";
    private static final String BOOTSTRAP_SERVER_CONFIG_LDAP_SVR = 
        "<ServerGroup name=\"sms\" " +
             "minConnPool=\"1\" maxConnPool=\"10\">" +
        "@SERVER_ENTRY@" + 
        "<User name=\"User2\" type=\"admin\">" +
        "<DirDN>@" + DS_MGR + "@</DirDN>" +
        "<DirPassword>@" + DS_PWD + "@</DirPassword>" +
        "</User>" +
        "<BaseDN>@" + DS_BASE_DN + "@</BaseDN>" +
        "</ServerGroup>";
    private static final String BOOTSTRAP_SERVER_CONFIG_LDAP_SVR_ENTRY = 
        "<Server name=\"Server@counter@\" host=\"@" + DS_HOST + "@\" " +
        "port=\"@" + DS_PORT + "@\" type=\"@" + DS_PROTO_TYPE + "@\" />";
}

