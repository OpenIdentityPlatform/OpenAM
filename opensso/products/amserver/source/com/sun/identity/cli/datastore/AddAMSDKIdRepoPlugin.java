/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AddAMSDKIdRepoPlugin.java,v 1.9 2009/12/11 06:50:36 hengming Exp $
 *
 */

package com.sun.identity.cli.datastore;


import com.iplanet.am.util.SSLSocketFactoryManager;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIUtil;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.LDAPUtils;
import com.sun.identity.common.configuration.ServerConfigXML;
import com.sun.identity.common.configuration.ServerConfigXML.DirUserObject;
import com.sun.identity.common.configuration.ServerConfigXML.ServerGroup;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import com.sun.identity.shared.ldap.LDAPConnection;
import com.sun.identity.shared.ldap.LDAPDN;
import com.sun.identity.shared.ldap.LDAPException;

/**
 * This command creates identity.
 */
public class AddAMSDKIdRepoPlugin extends AuthenticatedCommand {
    private static final String[] params = { "add-amsdk-idrepo-plugin" };
    private List directoryServers;
    private String bindDN;
    private String bindPwd;
    private String basedn;
    private String dUserPwd;
    private String pUserPwd;
    private String namingAttr = "uid";
    private String orgAttr = "o";
    
    private void init(RequestContext rc) throws Exception {

        directoryServers = rc.getOption("directory-servers");
        basedn = getStringOptionValue("basedn").trim();
        bindDN = getStringOptionValue("binddn").trim();
        bindPwd = CLIUtil.getFileContent(getCommandManager(),
            getStringOptionValue("bind-password-file"), true);
        dUserPwd = CLIUtil.getFileContent(getCommandManager(),
            getStringOptionValue("dsame-password-file"), true);
        pUserPwd = CLIUtil.getFileContent(getCommandManager(),
            getStringOptionValue("puser-password-file"), true);

        String attr = getStringOptionValue("user");
        if (attr != null && attr.trim().length() > 0) {
            namingAttr = attr.trim();
        }
        attr = getStringOptionValue("org");
        if (attr != null && attr.trim().length() > 0) {
            orgAttr = attr.trim();
        }
    }
    
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    // @SuppressWarnings("empty-statement")
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        IOutput outputWriter = getOutputWriter();

        try {
            init(rc);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_ADD_AMSDK_PLUGIN", params);

            loadLDIFs();
            // Load DAI service, if not already loaded
            String xmlData = loadDAIService();
            addAMSDKSubSchema(xmlData);
            loadDelegrationPolicies(xmlData);
            updateServerConfigXML();
            updateDSAMEUserPassword();

            outputWriter.printlnMessage(params[0] + ": " +
                getResourceString(
                "datastore-add-amsdk-idrepo-plugin-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_ADD_AMSDK_PLUGIN", params);
        } catch (Exception e) {
            String[] p = {"Adding AMSDK plugin", e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO, 
                "FAILED_ADD_AMSDK_PLUGIN", p);
            outputWriter.printlnMessage(params[0] + ": " + getResourceString(
                "datastore-add-amsdk-idrepo-plugin-failed") + ": " +
                e.getMessage());
        }
    }
    
    private String loadDAIService()
        throws SMSException, SSOException, CLIException, IOException {
        SSOToken adminSSOToken = getAdminSSOToken();

        // Load DAI service, if not already loaded
        String xmlData = null;
        ServiceManager sm = new ServiceManager(adminSSOToken);
        if (!sm.getServiceNames().contains("DAI")) {
            xmlData = getResourceContent("ums.xml");
    
            // Tag swap: @USER_NAMING_ATTR & @ORG_NAMING_ATTR
            xmlData = xmlData.replaceAll("@USER_NAMING_ATTR@",
                namingAttr);
            xmlData = xmlData.replaceAll("@ORG_NAMING_ATTR@", orgAttr);
            registerService(xmlData, adminSSOToken);
        }
        return xmlData;
    }

    private void addAMSDKSubSchema(String xmlData)
        throws SMSException, SSOException, CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        ServiceSchemaManager ssm = new ServiceSchemaManager(
            adminSSOToken, IdConstants.REPO_SERVICE, "1.0");
        ServiceSchema ss = ssm.getOrganizationSchema();
        if (!ss.getSubSchemaNames().contains("amSDK")) {
            xmlData = getResourceContent("idRepoAmSDK.xml");
            // Tag swap: @NORMALIZED_ORGBASED
            xmlData = xmlData.replaceAll("@NORMALIZED_ORGBASE@",
                DNUtils.normalizeDN(basedn));
            InputStream xmlInputStream =
                (InputStream) new ByteArrayInputStream(
                xmlData.getBytes());
            ss.addSubSchema(xmlInputStream);
        }
    }
    
    private void loadDelegrationPolicies(String xmlData) {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();

        // Load delegation policies for Top-level Admin Role and others
        try {
            xmlData = getResourceContent(
                "defaultDelegationPoliciesForAmSDK.xml");
            // Tag swap: @SM_CONFIG_ROOT_SUFFIX@ & @SM_ROOT_SUFFIX_HAT@
            String smsRootSuffix = ServiceManager.getBaseDN();
            xmlData = xmlData.replaceAll("@SM_CONFIG_ROOT_SUFFIX@",
                smsRootSuffix);
            String smsRootHat = smsRootSuffix.replaceAll(",", "^");
            xmlData = xmlData.replaceAll("@SM_ROOT_SUFFIX_HAT@",
                smsRootHat);
            // Tag swap: @ROOT_SUFFIX@
            xmlData = xmlData.replaceAll("@ROOT_SUFFIX@",
                DNUtils.normalizeDN(basedn));
            InputStream xmlInputStream =
                (InputStream) new ByteArrayInputStream(
                xmlData.getBytes());
            PolicyManager pm = new PolicyManager(adminSSOToken,
                "/sunamhiddenrealmdelegationservicepermissions");
            PolicyUtils.createPolicies(pm, xmlInputStream);
        } catch (Exception e) {
            outputWriter.printlnMessage(params[0] + ": " +
                getResourceString(
                "datastore-add-amsdk-idrepo-plugin-policies-failed") +
                ": " + e.getMessage());
        }
    }

    private void updateServerConfigXML() 
        throws Exception {
        SSOToken adminSSOToken = getAdminSSOToken();

        Set servers = ServerConfiguration.getServers(adminSSOToken);
        Map newValues = new HashMap();
        newValues.put("com.sun.am.event.connection.disable.list", "");
        for (Iterator items = servers.iterator(); items.hasNext();) {
            String instance = (String) items.next();
            String serverconfig = ServerConfiguration.getServerConfigXML(
                adminSSOToken, instance);
            ServerConfigXML cxml = new ServerConfigXML(serverconfig);
            ServerGroup defaultGroup = cxml.getDefaultServerGroup();
            // Add directory servers
            if ((directoryServers != null) &&
                !directoryServers.isEmpty()) {
                defaultGroup.hosts.clear();
                int i = 1;
                for (Iterator dshosts = directoryServers.iterator();
                    dshosts.hasNext(); i++) {
                    String dshost = (String) dshosts.next();
                    // Parse the dshost
                    String name = "SERVER" + i;
                    
                    DSEntry dsEntry = new DSEntry(dshost);
                    String type = (dsEntry.ssl) ? "SSL" : "SIMPLE";
                    String host = dsEntry.host;
                    String port = Integer.toString(dsEntry.port);
                    defaultGroup.addHost(name, host, port, type);
                }
            }
            // Set the base dn
            defaultGroup.dsBaseDN = basedn;
            // Set admin & proxy user's password
            for (Iterator users = defaultGroup.dsUsers.iterator();
                users.hasNext();) {
                DirUserObject user = (DirUserObject) users.next();
                if (user.type.equals("proxy")) {
                    user.dn = "cn=puser,ou=DSAME Users," + basedn;
                    user.password = Crypt.encode(pUserPwd);
                } else if (user.type.equals("admin")) {
                    user.dn = "cn=dsameuser,ou=DSAME Users," + basedn;
                    user.password = Crypt.encode(dUserPwd);
                }
            }
            // Saver serverconfig.xml
            ServerConfiguration.setServerConfigXML(adminSSOToken,
                instance, cxml.toXML());

            // Enable psearch for um, aci and sm
            ServerConfiguration.setServerInstance(adminSSOToken,
                instance, newValues);
        }
    }

    private void updateDSAMEUserPassword() throws Exception {
        String dsameuserDN = "cn=dsameuser,ou=DSAME Users," +
            SMSEntry.getRootSuffix();
        AMIdentity dsameuser = IdUtils.getIdentity(ssoToken, dsameuserDN);
            
        Set setNewPwd = new HashSet(2);
        setNewPwd.add(dUserPwd);
        Map mapPassword = new HashMap(2);
        mapPassword.put("userpassword", setNewPwd);
        dsameuser.setAttributes(mapPassword);
        dsameuser.store();
    }

    private String getResourceContent(String resName)
        throws CLIException {
        String configDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        return CLIUtil.getFileContent(getCommandManager(), 
            configDir + "/template/xml/" + resName);
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
    
    private void loadLDIFs() 
        throws Exception {
        CommandManager mgr = getCommandManager();

        List ldifs = getLDIFs();
        
        for (Iterator i = directoryServers.iterator(); i.hasNext(); ) {
            String dshost = (String)i.next();
            LDAPConnection ld = null;
            try {
                ld = getLDAPConnection(new DSEntry(dshost));
                String dbName = LDAPUtils.getDBName(basedn, ld);

                for (Iterator j = ldifs.iterator(); j.hasNext();) {
                    String file = (String) j.next();
                    String content = CLIUtil.getFileContent(mgr, file);

                    String swapped = tagswap(content, dbName);
                    loadLDIF(ld, swapped);
                }
            } finally {
                try {
                    if (ld != null) {
                        ld.disconnect();
                    }
                } catch (LDAPException e) {
                    //ingore
                }
            }
        }
    }
    
    private void loadLDIF(LDAPConnection ld, String ldif)
        throws Exception {
        ByteArrayInputStream reader = null;
        try {
            reader = new ByteArrayInputStream(ldif.getBytes());
            LDAPUtils.createSchemaFromLDIF(new DataInputStream(reader), ld);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    
    private List getLDIFs() {
        List ldifs = new ArrayList();
        String configDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String templateDir = configDir + "/ldif";
        ldifs.add(templateDir +
            "/sunds/amsdk_plugin/amsdk_sunone_schema2.ldif");
        ldifs.add(templateDir + "/sunds/sunds_user_schema.ldif");
        ldifs.add(templateDir + "/sunds/amsdk_plugin/amsdk_init_template.ldif");
        ldifs.add(templateDir + "/sunds/sunds_user_index.ldif");
        ldifs.add(templateDir + "/sunds/sunds_plugin.ldif");
        return ldifs;
    }

    private String tagswap(
        String orig,
        String dbName
    ) throws Exception {
        String normalizedDN = LDAPDN.normalize(basedn);
        String escapedDN = SMSSchema.escapeSpecialCharacters(normalizedDN);
        String[] doms = LDAPDN.explodeDN(normalizedDN, true);
        String rdn = doms[0];
        String peopleContainer = "People_" + basedn.replace(',', '_');
        
        orig = orig.replaceAll("@DB_NAME@", dbName);
        orig = orig.replaceAll("@NORMALIZED_RS@", escapedDN);
        orig = orig.replaceAll("@RS_RDN@", LDAPDN.escapeRDN(rdn));
        orig = orig.replaceAll("@ADMIN_PWD@", dUserPwd);
        orig = orig.replaceAll("@SERVER_HOST@", 
            SystemProperties.get(Constants.AM_SERVER_HOST));
        orig = orig.replaceAll("@ORG_NAMING_ATTR@", orgAttr);
        orig = orig.replaceAll("@ORG_OBJECT_CLASS@", "sunmanagedisorganization");
        orig = orig.replaceAll("@People_NM_ORG_ROOT_SUFFIX@", peopleContainer);
        orig = orig.replaceAll("@AMLDAPUSERPASSWD@", pUserPwd);
        return orig;
    }
    
    private LDAPConnection getLDAPConnection(DSEntry ds) 
        throws Exception {
        LDAPConnection ld = (ds.ssl) ? new LDAPConnection(
            SSLSocketFactoryManager.getSSLSocketFactory()) : 
            new LDAPConnection();
        ld.setConnectTimeout(300);
        ld.connect(3, ds.host, ds.port, bindDN, bindPwd);
        return ld;
    }
    
    class DSEntry {
        boolean ssl;
        String host;
        int port;
        
        DSEntry(String ds) {
            String dslc = ds.toLowerCase();
            ssl = dslc.startsWith("ldaps://");
            String dshost;

            if (ssl) {
                dshost = ds.substring(8);
            } else if (dslc.startsWith("ldap://")) {
                dshost = ds.substring(7);
            } else {
                dshost = ds;
            }

            int portIndex = dshost.indexOf(':');
            host = dshost;
            port = 389;

            if (portIndex != -1) {
                host = dshost.substring(0, portIndex);
                port = Integer.parseInt(dshost.substring(portIndex + 1));
            }
        }
    }

}
