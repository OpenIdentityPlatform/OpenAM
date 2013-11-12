/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServerConfigMgr.java,v 1.13 2009/01/28 05:34:49 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.iplanet.services.ldap;

import com.sun.identity.authentication.spi.AuthLoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.security.auth.login.LoginException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.XMLException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.Guid;
import com.iplanet.ums.IUMSConstants;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.UMSObject;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.setup.Bootstrap;
import com.sun.identity.setup.ConfiguratorException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.tools.bundles.VersionCheck;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import com.sun.identity.shared.ldap.util.DN;

/**
 * The class <code>ServiceConfigMgr</code> provides interfaces to set the
 * directory server information such as hostname, port number, admin DN and
 * password, and proxy user DN and password.
 */
public class ServerConfigMgr {
    
    // Private static varibales
    private static final String HELP = "--help";
    
    private static final String S_HELP = "-h";
    
    private static final String Q_HELP = "?";
    
    private static final String SQ_HELP = "-?";
    
    private static final String ADMIN = "--admin";
    
    private static final String S_ADMIN = "-a";
    
    private static final String PROXY = "--proxy";
    
    private static final String S_PROXY = "-p";
    
    private static final String OLD = "--old";
    
    private static final String S_OLD = "-o";
    
    private static final String NEW = "--new";
    
    private static final String S_NEW = "-n";
    
    private static final String ENCRYPT = "--encrypt";
    
    private static final String S_ENCRYPT = "-e";
    
    private static final String RESOURCE_BUNDLE_NAME = "amSDK";
    
    private static final int MIN_PASSWORD_LEN = 8;
    
    // Run time property key to obtain serverconfig.xml path
    private static final String RUN_TIME_CONFIG_PATH =
        "com.iplanet.coreservices.configpath";
    private static boolean isAMSDKConfigured;
    
    private boolean isLegacy;
    
    private static ResourceBundle i18n = ResourceBundle.getBundle(
        RESOURCE_BUNDLE_NAME);
    private static Debug debug;
    
    private String configFile;
    private Node root;
    private Node defaultServerGroup;
    private String strXMLDeclarationHdr;
    private SSOToken ssoToken;
    
    
    /**
     * Constructor that get the serverconfig.xml file and gets the XML document.
     */
    public ServerConfigMgr() throws Exception {
        ssoToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        isLegacy = ServerConfiguration.isLegacy();
        isAMSDKConfigured = ServiceManager.isAMSDKConfigured();
        getServerConfigXMLDoc();
    }
    
    private void getServerConfigXMLDoc()
    throws Exception {
        InputStream is = null;
        
        try {
            if (isLegacy) {
                configFile = getServiceConfigXMLFileLocation();
                is = new FileInputStream(configFile);
            } else {
                String strXML = ServerConfiguration.getServerConfigXML(ssoToken,
                    SystemProperties.getServerInstanceName());
                is = new ByteArrayInputStream(strXML.getBytes());
            }
            
            Document document = XMLUtils.getXMLDocument(is);
            if (document == null) {
                throw (new XMLException(
                    i18n.getString("dscfg-error-reading-config-file") +
                    "\n" + i18n.getString("dscfg-corrupted-serverconfig")));
            }
            
            root = XMLUtils.getRootNode(document, DSConfigMgr.ROOT);
            if (root == null) {
                throw new XMLException(
                    i18n.getString("dscfg-unable-to-find-root-node") + "\n" +
                    i18n.getString("dscfg-corrupted-serverconfig"));
            }
            
            defaultServerGroup = XMLUtils.getNamedChildNode(root,
                DSConfigMgr.SERVERGROUP, DSConfigMgr.NAME, DSConfigMgr.DEFAULT);
            if (defaultServerGroup == null) {
                throw new XMLException(
                    i18n.getString("dscfg-unable-to-find-default-servergroup")
                    + "\n" + i18n.getString("dscfg-corrupted-serverconfig"));
            }
            strXMLDeclarationHdr = getXMLDeclarationHeader(
                ssoToken, configFile);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
    
    private String getXMLDeclarationHeader(SSOToken ssoToken, String configFile)
    throws Exception {
        StringBuilder xml = new StringBuilder();
        InputStream is = null;
        
        try {
            if (isLegacy) {
                is = new FileInputStream(configFile);
            } else {
                String strXML = ServerConfiguration.getServerConfigXML(ssoToken,
                    SystemProperties.getServerInstanceName());
                is = new ByteArrayInputStream(strXML.getBytes());
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line = in.readLine();
            
            while (line != null) {
                int index = line.indexOf(DSConfigMgr.ROOT);
                if (index == -1) {
                    xml.append(line).append("\n");
                } else {
                    if (--index > 0) {
                        xml.append(line.substring(0, index)).append("\n");
                    }
                    break;
                }
                line = in.readLine();
            }
        } finally {
            is.close();
        }
        
        return xml.toString();
    }
    
    private static String getServiceConfigXMLFileLocation()
    throws Exception {
        String path = SystemProperties.get(SystemProperties.CONFIG_PATH);
        if (path == null) { // For Backward compatibility obtain from runtime
            path = System.getProperty(RUN_TIME_CONFIG_PATH);
        }
        String fileLoc = path + System.getProperty("file.separator")
        + SystemProperties.CONFIG_FILE_NAME;
        File file = new File(fileLoc);
        if (!file.exists() || !file.canRead() || !file.canWrite()) {
            String objs[] = {fileLoc};
            throw new Exception(MessageFormat.format(
                i18n.getString("dscfg-no-file-permission"), (Object[])objs));
        }
        return fileLoc;
    }
    
    
    
    private static void validateArguments(String[] args) {
        if (args.length == 0) {
            System.err.println(i18n.getString("dscfg-usage"));
            System.exit(1);
        } else {
            String action = args[0];
            if (!action.equals(HELP) && !action.equals(S_HELP) &&
                !action.equals(Q_HELP) && !action.equals(SQ_HELP) &&
                !action.equals(ADMIN) && !action.equals(S_ADMIN) &&
                !action.equals(PROXY) && !action.equals(S_PROXY) &&
                !action.equals(ENCRYPT) && !action.equals(S_ENCRYPT)
                ) {
                Object[] objs = {action};
                System.err.println(MessageFormat.format(
                    i18n.getString("dscfg-invalid-option"), objs));
                System.err.println(i18n.getString("dscfg-usage"));
                System.exit(1);
            }
            
            if (action.equals(ADMIN) || action.equals(S_ADMIN) ||
                action.equals(PROXY) || action.equals(S_PROXY)
                ) {
                if (args.length != 5) {
                    System.err.println(i18n.getString("dscfg-illegal-args"));
                    System.err.println(i18n.getString("dscfg-usage"));
                    System.exit(1);
                }
            } else if (action.equals(ENCRYPT) || action.equals(S_ENCRYPT)) {
                if (args.length != 2) {
                    System.err.println(i18n.getString("dscfg-illegal-args"));
                    System.err.println(i18n.getString("dscfg-usage"));
                    System.exit(1);
                }
            } else {
                if (args.length != 1) {
                    System.err.println(i18n.getString("dscfg-illegal-args"));
                    System.err.println(i18n.getString("dscfg-usage"));
                    System.exit(1);
                }
            }
        }
    }
    
    private static boolean printHelpMessage(String[] args) {
        boolean processed = false;
        if (args[0].equals(HELP) || args[0].equals(S_HELP) ||
            args[0].equals(Q_HELP) || args[0].equals(SQ_HELP)
            ) {
            processed = true;
            System.out.println(i18n.getString("dscfg-usage"));
        }
        return processed;
    }
    
    private static boolean encryptPassword(String[] args) {
        boolean processed = false;
        if (args[0].equals(S_ENCRYPT) || args[0].equals(ENCRYPT)) {
            processed = true;
            String password = null;
            
            if (args.length > 1) {
                try {
                    password = readOneLinerFromFile(args[1]);
                    if ((password == null) || (password.length() == 0)) {
                        Object messageArgs[] = { args[1] };
                        System.err.println(MessageFormat.format(i18n.getString(
                            "dscfg-null-password"), messageArgs));
                        System.err.println(i18n.getString("dscfg-usage"));
                        System.exit(1);
                    }
                    
                    System.out.println((String) AccessController
                        .doPrivileged(new EncodeAction(password)));
                } catch (FileNotFoundException e) {
                    Object messageArgs[] = { args[1] };
                    System.err.println(MessageFormat.format(i18n.getString(
                        "dscfg-passwd-file-not-found"), messageArgs));
                    System.exit(1);
                } catch (IOException ioe) {
                    Object messageArgs[] = { args[1] };
                    System.err.println(MessageFormat.format(i18n.getString(
                        "dscfg-passwd-file-not-found"), messageArgs));
                    System.exit(1);
                }
            } else {
                Object messageArgs[] = { args[0] };
                System.err.println(MessageFormat.format(i18n.getString(
                    "dscfg-incorrect-usage"), messageArgs));
                System.err.println(i18n.getString("dscfg-usage"));
                System.exit(1);
            }
        }
        return processed;
    }
    
    private static boolean changePassword(String[] args)
    throws Exception {
        boolean adminPassword = false;
        boolean proxyPassword = false;
        
        if (args[0].equals(S_ADMIN) || args[0].equals(ADMIN)) {
            adminPassword = true;
        } else {
            proxyPassword = true;
        }
        
        isAMSDKConfigured = ServiceManager.isAMSDKConfigured();
        if (proxyPassword && !isAMSDKConfigured) {
            System.err.println(i18n.getString("dscfg-proxy-no-suppport"));
            System.exit(1);
        }
        
        String oldPassword = null;
        String newPassword = null;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals(OLD) || args[i].equals(S_OLD)) {
                oldPassword = readOneLinerFromFile(args[++i]);
            } else if (args[i].equals(NEW) || args[i].equals(S_NEW)) {
                newPassword = readOneLinerFromFile(args[++i]);
            } else {
                Object[] objs = { args[i] };
                System.err.println(MessageFormat.format(i18n.getString(
                    "dscfg-invalid-option"),objs));
                System.err.println(i18n.getString("dscfg-usage"));
                System.exit(1);
            }
        }
        validatePasswords(oldPassword, newPassword);
        
        if (adminPassword && !isAMSDKConfigured) {
            SSOToken ssoToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            if (!authenticateDsameUser(ssoToken, oldPassword, newPassword)) {
                throw new Exception(i18n.getString("dscfg-invalid-password"));
            }
            String dsameuserDN = "cn=dsameuser,ou=DSAME Users," +
                SMSEntry.getRootSuffix();
            AMIdentity dsameuser = IdUtils.getIdentity(ssoToken, dsameuserDN);
            
            Set setNewPwd = new HashSet(2);
            setNewPwd.add(newPassword);
            Map mapPassword = new HashMap(2);
            mapPassword.put("userpassword", setNewPwd);
            dsameuser.setAttributes(mapPassword);
            dsameuser.store();
        } else {
            ServerConfigMgr scm = new ServerConfigMgr();
            DN adminDN = new DN(scm.getUserDN(DSConfigMgr.VAL_AUTH_ADMIN));
            DN proxyDN = new DN(scm.getUserDN(DSConfigMgr.VAL_AUTH_PROXY));
            if (adminDN.equals(proxyDN)) {
                adminPassword = true;
                proxyPassword = true;
            }
            
            if (adminPassword) {
                scm.setAdminUserPassword(oldPassword, newPassword);
            }
            if (proxyPassword) {
                scm.setProxyUserPassword(oldPassword, newPassword);
            }
            scm.save();
        }
        System.out.println(i18n.getString("dscfg-passwd-success"));
        
        return true;
    }
    
    private static boolean authenticateDsameUser(
        SSOToken ssoToken,
        String oldPassword,
        String newPassword
        ) {
        Callback[] idCallbacks = new Callback[2];
        NameCallback nameCallback = new NameCallback("dummy");
        nameCallback.setName("dsameuser");
        idCallbacks[0] = nameCallback;
        PasswordCallback passwordCallback = new PasswordCallback(
            "dummy", false);
        passwordCallback.setPassword(oldPassword.toCharArray());
        idCallbacks[1] = passwordCallback;
        
        try {
            AMIdentityRepository amir = new AMIdentityRepository(ssoToken, "/");
            if (!amir.authenticate(idCallbacks)) {
                passwordCallback.setPassword(newPassword.toCharArray());
                return amir.authenticate(idCallbacks);
            }
            return true;
        } catch (SSOException ex) {
            return false;
        } catch (AuthLoginException ex) {
            return false;
        } catch (IdRepoException ex) {
            return false;
        }
    }
    
    public static void main(String args[]) {
        try {
            Bootstrap.load();
            
            if (VersionCheck.isVersionValid() == 1) {
                System.exit(1);
            }
            
            debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
            validateArguments(args);
            boolean proceeded = printHelpMessage(args) ||
                encryptPassword(args) || changePassword(args);
        } catch (ConfiguratorException ex) {
            System.err.println(ex.getL10NMessage(Locale.getDefault()));
            System.exit(1);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        } finally {
            ShutdownManager shutdownMan = ShutdownManager.getInstance();
            if (shutdownMan.acquireValidLock()) {
                try {
                    shutdownMan.shutdown();
                } finally {
                    shutdownMan.releaseLockAndNotify();
                }
            }
        }
    }
    
    
    // ----------------------------------------------------------------------
    // Currently supported methods
    // ----------------------------------------------------------------------
    
    /**
     * Sets the admin user's password.
     */
    public void setAdminUserPassword(String oldPassword, String newPassword)
    throws Exception {
        changePassword(DSConfigMgr.VAL_AUTH_ADMIN, oldPassword, newPassword);
        if (!isLegacy) {
            Bootstrap.modifyDSAMEUserPassword(newPassword);
        }
    }
    
    /**
     * Sets the proxy user's password.
     */
    protected void setProxyUserPassword(String oldPassword, String newPassword)
    throws Exception {
        changePassword(DSConfigMgr.VAL_AUTH_PROXY, oldPassword, newPassword);
    }
    
    /**
     * Stores the directory server configuration information to the file system.
     */
    public void save() throws Exception {
        String xml = strXMLDeclarationHdr + SMSSchema.nodeToString(root);
        
        if (isLegacy) {
            PrintWriter out = new PrintWriter(new FileOutputStream(configFile));
            out.print(xml);
            out.close();
        } else {
            ServerConfiguration.setServerConfigXML(ssoToken,
                SystemProperties.getServerInstanceName(), xml);
        }
    }
    
    /**
     * Checks and sets the password
     */
    private void changePassword(
        String userType,
        String oldPassword,
        String newPassword
        ) throws Exception {
        String fileEncPassword = getUserPassword(userType);
        String userDN = getUserDN(userType);
        
        if ((fileEncPassword == null) || (fileEncPassword.length() == 0) ||
            (userDN == null) || (userDN.length() == 0)
            ) {
            debug.error("Null password or user DN for user type: " + userType +
                " from file: " + configFile);
            throw new XMLException(
                i18n.getString("dscfg-corrupted-serverconfig"));
        }
        
        // Verify old password
        if (!oldPassword.equals(AccessController.doPrivileged(
            new DecodeAction(fileEncPassword)))
            ) {
            throw new Exception(i18n.getString("dscfg-old-passwd-donot-match"));
        }
        
        if (isAMSDKConfigured) {
            // this is to check if updating of DS is required.
            try {
                new AuthContext(new AuthPrincipal(userDN),
                    newPassword.toCharArray());
                if (debug.messageEnabled()) {
                    debug.message("DN: " + userDN +
                        " new password is already updated in the directory");
                }
            } catch (LoginException lee) {
                try {
                    AuthContext ac = new AuthContext(new AuthPrincipal(userDN),
                        oldPassword.toCharArray());
                    PersistentObject user = UMSObject.getObject(ac.getSSOToken(),
                        new Guid(userDN));
                    if (debug.messageEnabled()) {
                        debug.message("For DN: " + userDN +
                            " changing password in directory");
                    }
                    user.setAttribute(new Attr("userPassword", newPassword));
                    user.save();
                } catch (LoginException le) {
                    if (debug.warningEnabled()) {
                        debug.warning("For DN: " + userDN +
                            " new and old passwords donot match with directory");
                    }
                    throw new Exception(i18n.getString("dscfg-invalid-password") +
                        "\n" + le.getMessage());
                }
            }
        }
        
        setUserPassword(userType, newPassword);
    }
    
    /**
     * Returns the user DN for the given user type
     */
    private String getUserDN(String userType) throws Exception {
        Node dnNode = XMLUtils.getChildNode(getUserNode(userType),
            DSConfigMgr.AUTH_ID);
        if (dnNode == null) {
            throw (new XMLException(i18n.getString(
                "dscfg-corrupted-serverconfig")));
        }
        return (XMLUtils.getValueOfValueNode(dnNode));
    }
    
    /**
     * Returns the user password for the given user type
     */
    private String getUserPassword(String userType) throws Exception {
        Node pwdNode = XMLUtils.getChildNode(getUserNode(userType),
            DSConfigMgr.AUTH_PASSWD);
        if (pwdNode == null) {
            throw (new XMLException(i18n.getString(
                "dscfg-corrupted-serverconfig")));
        }
        return (XMLUtils.getValueOfValueNode(pwdNode));
    }
    
    private void setUserPassword(String userType, String password)
    throws Exception {
        Node pwdNode = XMLUtils.getChildNode(getUserNode(userType),
            DSConfigMgr.AUTH_PASSWD);
        if (pwdNode == null) {
            throw (new XMLException(i18n.getString(
                "dscfg-corrupted-serverconfig")));
        }
        // Encrypt the new password and store
        String encPassword = (String) AccessController.doPrivileged(
            new EncodeAction(password));
        NodeList textNodes = pwdNode.getChildNodes();
        Node textNode = textNodes.item(0);
        textNode.setNodeValue(encPassword);
        // Delete the remaining text nodes
        for (int i = 1; i < textNodes.getLength(); i++) {
            pwdNode.removeChild(textNodes.item(i));
        }
    }
    
    /**
     * Returns the user node given the user type
     */
    private Node getUserNode(String userType) throws Exception {
        Node userNode = XMLUtils.getNamedChildNode(defaultServerGroup,
            DSConfigMgr.USER, DSConfigMgr.AUTH_TYPE, userType);
        if (userNode == null) {
            debug.error("Unable to get user type: " + userType
                + " node from file: " + configFile);
            throw (new XMLException(i18n
                .getString("dscfg-corrupted-serverconfig")));
        }
        return (userNode);
    }
    
    private static void validatePasswords(
        String oldPassword,
        String newPassword
        ) {
        if ((oldPassword == null) || (oldPassword.length() == 0)) {
            System.err.println(i18n.getString("dscfg-null-old-password"));
            System.err.println(i18n.getString("dscfg-usage"));
            System.exit(1);
        }
        if ((newPassword == null) || (newPassword.length() == 0)) {
            System.err.println(i18n.getString("dscfg-null-new-password"));
            System.err.println(i18n.getString("dscfg-usage"));
            System.exit(1);
        }
        
        if (newPassword.length() < MIN_PASSWORD_LEN) {
            String objs[] = { Integer.toString(MIN_PASSWORD_LEN) };
            System.err.println(MessageFormat.format(
                i18n.getString("dscfg-password-lenght-not-met"),
                (Object[])objs));
            System.exit(1);
        } else if (newPassword.equals(oldPassword)) {
            System.err.println(i18n.getString("dscfg-passwords-are-same"));
            System.exit(1);
        }
    }
    
    private static String readOneLinerFromFile(String fileName)
    throws FileNotFoundException, IOException {
        BufferedReader br = null;
        String lineData = null;
        try {
            FileReader fr = new FileReader(fileName);
            br = new BufferedReader(fr);
            lineData = br.readLine();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ie) {
                    // Ignore
                }
            }
        }
        return lineData;
    }
}
