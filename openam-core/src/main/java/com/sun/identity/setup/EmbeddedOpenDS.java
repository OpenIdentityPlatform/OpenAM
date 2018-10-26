/*
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
 * $Id: EmbeddedOpenDS.java,v 1.27 2010/01/15 01:22:39 goodearth Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 */

package com.sun.identity.setup;

import static org.forgerock.opendj.ldap.LDAPConnectionFactory.AUTHN_BIND_REQUEST;
import static org.forgerock.opendj.ldap.LDAPConnectionFactory.CONNECT_TIMEOUT;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletContext;

import com.google.common.io.ByteStreams;
import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.openam.ldap.LDAPRequests;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.config.dsconfig.DSConfig;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Attributes;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.util.Options;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownPriority;
import org.forgerock.util.time.Duration;
import org.opends.quicksetup.TempLogFile;
import org.opends.server.core.DirectoryServer;
import org.opends.server.extensions.SaltedSHA512PasswordStorageScheme;
import org.opends.server.tools.InstallDS;
import org.opends.server.tools.RebuildIndex;
import org.opends.server.tools.dsreplication.ReplicationCliMain;
import org.opends.server.types.DirectoryEnvironmentConfig;
import org.opends.server.util.EmbeddedUtils;
import org.opends.server.util.ServerConstants;
import org.opends.server.util.TimeThread;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSEntry;

// OpenDS, now OpenDJ, does not have APIs to install and setup replication yet

/**
 * This class encapsulates all <code>OpenDJ</code>  dependencies.
 * All the interfaces are invoked from <code>AMSetupServlet</code> class
 * at different points : initial installation, normal startup and
 * normal shutdown of the embedded <code>OpenDJ</code> instance.
 */
public class EmbeddedOpenDS {
    private static final String OPENDS_1x_VER = "5097";
    private static final String OPENDS_230B2_VER = "6500";
    private static final String OPENDS_UPGRADE_DIR = "/config/upgrade/";
    private static final String OPENDS_CONFIG_LDIF = "config.ldif";
    private static boolean serverStarted = false;

    private static ConnectionFactory factory;

    /**
     * Returns <code>true</code> if the server has already been started.
     *
     * @return <code>true</code> if the server has already been started.
     */
    public static boolean isStarted() {
        return serverStarted;
    }

    /**
     * Sets up embedded OpenDJ during initial installation :
     * <ul>
     * <li>lays out the filesystem directory structure needed by OpenDJ
     * <li>sets up port numbers for ldap and replication
     * <li>invokes <code>EmbeddedUtils</code> to start the embedded server.
     * </ul>
     *
     * @param map        Map of properties collected by the configurator.
     * @param servletCtx Servlet Context to read deployed war contents.
     * @throws Exception on encountering errors.
     */
    public static void setup(Map map, ServletContext servletCtx)
            throws Exception {
        // Determine Cipher to be used
        SetupProgress.reportStart("emb.installingemb.null", null);
        String xform = getSupportedTransformation();

        if (xform == null) {
            SetupProgress.reportEnd("emb.noxform", null);
            throw new Exception("No transformation found");
        } else {
            map.put(OPENDS_TRANSFORMATION, xform);
            Object[] params = {xform};
            SetupProgress.reportEnd("emb.success.param", params);
        }

        String basedir = (String) map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        String odsRoot = getOpenDJBaseDir(map);
        new File(basedir).mkdir();
        new File(odsRoot).mkdir();

        SetupProgress.reportStart("emb.opends.start", null);
        String zipFileName = "/WEB-INF/template/opendj/opendj.zip";
        BufferedInputStream bin = new BufferedInputStream(
                AMSetupUtils.getResourceAsStream(servletCtx, zipFileName), 10000);
        BufferedOutputStream bout = new BufferedOutputStream(
                new FileOutputStream(odsRoot + "/opendj.zip"), 10000);

        try {
            ByteStreams.copy(bin, bout);
        } catch (IOException ioe) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "EmbeddedOpenDS.setup(): Error copying zip file", ioe);
            throw ioe;
        } finally {
            IOUtils.closeIfNotNull(bin);
            IOUtils.closeIfNotNull(bout);
        }

        ZipFile opendsZip = new ZipFile(odsRoot + "/opendj.zip");
        Enumeration files = opendsZip.entries();
        // Process the OpenDJ Archive File.
        while (files.hasMoreElements()) {
            ZipEntry file = (ZipEntry) files.nextElement();
            File f = new File(odsRoot + "/" + file.getName());

            if (file.isDirectory()) {
                f.mkdir();
                continue;
            }

            BufferedInputStream is =
                    new BufferedInputStream(opendsZip.getInputStream(file), 10000);
            BufferedOutputStream fos =
                    new BufferedOutputStream(new java.io.FileOutputStream(f), 10000);

            try {
                ByteStreams.copy(is, fos);
            } catch (IOException ioe) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "EmbeddedOpenDS.setup(): Error loading ldifs", ioe);
                throw ioe;
            } finally {
                IOUtils.closeIfNotNull(is);
                IOUtils.closeIfNotNull(fos);
            } // End of Inner Finally.

            if (file.getName().endsWith("sh") || file.getName().startsWith("bin")) {
                f.setExecutable(true);
            }
        } // End of File Elements from Zip for OpenDJ.

        // create tag swapped files
        String[] tagSwapFiles = {
                "template/ldif/openam_suffix.ldif.template"
        };

        for (int i = 0; i < tagSwapFiles.length; i++) {
            String fileIn = odsRoot + "/" + tagSwapFiles[i];
            FileReader fin = new FileReader(fileIn);

            StringBuilder sbuf = new StringBuilder();
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
            FileWriter fout = null;

            try {
                fout = new FileWriter(odsRoot + "/" +
                        tagSwapFiles[i].substring(0, tagSwapFiles[i].indexOf(".template")));
                String inpStr = sbuf.toString();
                fout.write(ServicesDefaultValues.tagSwap(inpStr));
            } catch (IOException e) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "EmbeddedOpenDS.setup(): Error tag swapping files", e);
                throw e;
            } finally {
                IOUtils.closeIfNotNull(fin);
                IOUtils.closeIfNotNull(fout);
            }
        }

        // remove zip
        File toDelete = new File(odsRoot + "/opendj.zip");
        if (!toDelete.delete()) {
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "EmbeddedOpenDS.setup(): Unable to delete zip file:" +toDelete.getAbsolutePath());
        }

        SetupProgress.reportEnd("emb.opends.stop", null);

        // now setup OpenDJ
        System.setProperty("org.opends.quicksetup.Root", odsRoot);
        System.setProperty(ServerConstants.PROPERTY_SERVER_ROOT, odsRoot);
        System.setProperty(ServerConstants.PROPERTY_INSTANCE_ROOT, odsRoot);
        EmbeddedOpenDS.setupOpenDS(map);

        Object[] params = {odsRoot};
        SetupProgress.reportStart("emb.installingemb", params);
        EmbeddedOpenDS.startServer(odsRoot);

        // Check: If adding a new server to a existing cluster
        if (!isMultiServer(map)) {
            // Default: single / first server.
            SetupProgress.reportStart("emb.creatingfamsuffix", null);
            int ret = EmbeddedOpenDS.loadLDIF(map, odsRoot, odsRoot + "/ldif/openam_suffix.ldif");

            if (ret == 0) {
                SetupProgress.reportEnd("emb.creatingfamsuffix.success", null);
            } else {
                Object[] error = {Integer.toString(ret)};
                SetupProgress.reportEnd("emb.creatingfamsuffix.failure", error);
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                        "EmbeddedOpenDS.setupOpenDS. Error loading OpenAM suffix");
                throw new ConfiguratorException(
                        "emb.creatingfamsuffix.failure");
            }

        } // End of single / first server check.
    }

    /**
     * Helper Method to Copy from one Byte Channel to another.
     * @param from
     * @param to
     * @throws IOException
     */
    protected static void channelCopy(ReadableByteChannel from, WritableByteChannel to)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        try {
            // Read
            while (from.read(buffer) != -1) {
                buffer.flip();
                to.write(buffer);
                buffer.compact();
            }
            // Flip the Buffer
            buffer.flip();
            // Write
            while (buffer.hasRemaining()) {
                to.write(buffer);
            } // End of While Loop
        } finally {
            // Handle In Channel Closure
            if (from != null) {
                try {
                    from.close();
                } catch (Exception ex) {
                    //No handling required
                }
            }
            // Handle Out Channel Closure
            if (to != null) {
                try {
                    to.close();
                } catch (Exception ex) {
                    //No handling required
                }
            }
        } // End of Finally
    }

    /**
     * Preferred transforms
     */
    final static String[] preferredTransforms =
            {
                    "RSA/ECB/OAEPWithSHA1AndMGF1Padding",      // Sun JCE
                    "RSA/ /OAEPPADDINGSHA-1",                  // IBMJCE
                    "RSA/ECB/OAEPWithSHA-1AndMGF-1Padding",    // BouncyCastle
                    "RSA/ECB/PKCS1Padding"                     // Fallback
            };
    final static String OPENDS_TRANSFORMATION = "OPENDS_TRANSFORMATION";

    /**
     * Traverses <code>preferredTransforms</code> list in order to
     * find a Cipher supported by underlying JCE providers.`
     *
     * @returns transformation available.
     */
    private static String getSupportedTransformation() {
        for (int i = 0; i < preferredTransforms.length; i++) {
            try {
                Cipher.getInstance(preferredTransforms[i]);
                return preferredTransforms[i];
            } catch (NoSuchAlgorithmException ex) {
            } catch (NoSuchPaddingException ex) {
            }
        }
        return null;
    }

    /**
     * Runs the OpenDJ setup command to create our instance
     *
     * @param map The map of configuration options
     * @throws Exception upon encountering errors.
     */
    public static void setupOpenDS(Map map)
            throws Exception {
        SetupProgress.reportStart("emb.setupopends", null);

        int ret = runOpenDSSetup(map);

        if (ret == 0) {
            SetupProgress.reportEnd("emb.setupopends.success", null);
            Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                    "EmbeddedOpenDS.setupOpenDS: OpenDS setup succeeded.");
        } else {
            Object[] params = {Integer.toString(ret)};
            SetupProgress.reportEnd("emb.setupopends.failed.param", params);
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "EmbeddedOpenDS.setupOpenDS. Error setting up OpenDS");
            throw new ConfiguratorException(
                    "configurator.embsetupopendsfailed");
        }
    }

    /**
     * Runs the OpenDJ setup command like this:
     * $ ./setup --cli --adminConnectorPort 4444
     * --baseDN dc=openam,dc=openidentityplatform,dc=org --rootUserDN "cn=directory manager"
     * --doNotStart --ldapPort 50389 --skipPortCheck --rootUserPassword xxxxxxx
     * --jmxPort 1689 --no-prompt
     *
     * @param map Map of properties collected by the configurator.
     * @return status : 0 == success, !0 == failure
     */
    public static int runOpenDSSetup(Map map) {
        String[] setupCmd = {
                "--cli",                        // 0
                "--adminConnectorPort",         // 1
                "4444",                         // 2
                "--baseDN",                     // 3
                Constants.DEFAULT_ROOT_SUFFIX,  // 4
                "--rootUserDN",                 // 5
                "cn=Directory Manager",         // 6
                "--ldapPort",                   // 7
                "50389",                        // 8
                "--skipPortCheck",              // 9
                "--rootUserPassword",           // 10
                "xxxxxxx",                      // 11
                "--jmxPort",                    // 12
                "1689",                         // 13
                "--no-prompt",                  // 14
                "--doNotStart",                 // 15
                "--hostname",                   // 16
                "hostname",                     // 17
                "--noPropertiesFile",           // 18
                "--backendType",                // 19
                "je"                            // 20
        };

        setupCmd[2] = (String) map.get(SetupConstants.CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT);
        setupCmd[4] = (String) map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX);
        setupCmd[6] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_DN);
        setupCmd[8] = (String) map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT);
        setupCmd[13] = (String) map.get(SetupConstants.CONFIG_VAR_DIRECTORY_JMX_SERVER_PORT);
        setupCmd[17] = getOpenDJHostName(map);
        setupCmd[20] = SystemProperties.get(SetupConstants.DJ_BACKEND_TYPE_CONFIG_NAME,
                SetupConstants.DJ_BACKEND_TYPE_DEFAULT);

        Object[] params = {concat(setupCmd)};
        SetupProgress.reportStart("emb.setupcommand", params);

        setupCmd[11] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);

        int ret = InstallDS.mainCLI(
                setupCmd,
                SetupProgress.getOutputStream(),
                SetupProgress.getOutputStream(),
                TempLogFile.newTempLogFile("opendj-setup-"));

        if (ret == 0) {
            SetupProgress.reportEnd("emb.success", null);
        } else {
            SetupProgress.reportEnd("emb.failed", null);
        }

        return ret;
    }

    /**
     * Starts the embedded <code>OpenDJ</code> instance.
     *
     * @param odsRoot File system directory where <code>OpenDJ</code>
     *                is installed.
     * @throws Exception upon encountering errors.
     */
    public static void startServer(String odsRoot) throws Exception {
        if (isStarted()) {
            return;
        }
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        debug.message("EmbeddedOpenDS.startServer(" + odsRoot + ")");

        DirectoryEnvironmentConfig config = new DirectoryEnvironmentConfig();
        config.setServerRoot(new File(odsRoot));
        config.setForceDaemonThreads(true);
        config.setConfigFile(new File(odsRoot + "/config", "config.ldif"));
        debug.message("EmbeddedOpenDS.startServer:starting DS Server...");
        EmbeddedUtils.startServer(config);
        debug.message("...EmbeddedOpenDS.startServer:DS Server started.");

        int sleepcount = 0;
        while (!EmbeddedUtils.isRunning() && (sleepcount < 60)) {
            sleepcount++;
            SetupProgress.reportStart("emb.waitingforstarted", null);
            Thread.sleep(1000);
        }

        if (EmbeddedUtils.isRunning()) {
            SetupProgress.reportEnd("emb.success", null);
        } else {
            SetupProgress.reportEnd("emb.failed", null);
        }

        serverStarted = true;

        ShutdownManager shutdownMan = com.sun.identity.common.ShutdownManager.getInstance();

        shutdownMan.addShutdownListener(new ShutdownListener() {
            public void shutdown() {
                try {
                    shutdownServer("Graceful Shutdown");
                } catch (Exception ex) {
                    Debug debug = Debug.getInstance(
                            SetupConstants.DEBUG_NAME);
                    debug.error("EmbeddedOpenDS:shutdown hook failed",
                            ex);
                }
            }
        }, ShutdownPriority.LOWEST);

    }


    /**
     * Gracefully shuts down the embedded OpenDJ instance.
     *
     * @param reason string representing reason why shutdown was called.
     * @throws Exception on encountering errors.
     */
    public static void shutdownServer(String reason) throws Exception {
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        if (isStarted()) {
            debug.message("EmbeddedOpenDS.shutdown server...");
            DirectoryServer.shutDown(
                    "com.sun.identity.setup.EmbeddedOpenDS",
                    LocalizableMessage.EMPTY);
            int sleepcount = 0;
            while (DirectoryServer.isRunning() && (sleepcount < 60)) {
                sleepcount++;
                Thread.sleep(1000);
            }
            serverStarted = false;
            debug.message("EmbeddedOpenDS.shutdown server success.");
        }
    }

    public static void setupReplication(Map map) throws Exception {
        // Setup replication
        SetupProgress.reportStart("emb.creatingreplica", null);
        int ret = setupReplicationEnable(map);
        if (ret == 0) {
            ret = setupReplicationInitialize(map);
            SetupProgress.reportEnd("emb.success", null);
            Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                    "EmbeddedOpenDS.setupReplication: replication setup succeeded.");
        } else {
            Object[] params = {Integer.toString(ret)};
            SetupProgress.reportEnd("emb.failed.param", params);
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "EmbeddedOpenDS.setupReplication. Error setting up replication");
            throw new ConfiguratorException(
                    "configurator.embreplfailed");
        }
    }

    /**
     * Setups replication between two OpenDJ sms and user stores.
     * $ dsreplication enable
     * --no-prompt
     * --host1 host1 --port1 1389 --bindDN1 "cn=Directory Manager"
     * --bindPassword1 password --replicationPort1 8989
     * --host2 host2 --port2 2389 --bindDN2 "cn=Directory Manager"
     * --bindPassword2 password --replicationPort2 8990
     * --adminUID admin --adminPassword password
     * --baseDN "dc=example,dc=com"
     *
     * @param map Map of properties collected by the configurator.
     * @return status : 0 == success, !0 == failure
     */
    public static int setupReplicationEnable(Map map) {
        String[] enableCmd = {
                "enable",                // 0
                "--no-prompt",           // 1
                "--host1",               // 2
                "host1val",              // 3
                "--port1",               // 4
                "port1ival",             // 5
                "--bindDN1",             // 6
                "cn=Directory Manager",  // 7
                "--bindPassword1",       // 8
                "xxxxxxxx",              // 9
                "--replicationPort1",    // 10
                "8989",                  // 11
                "--host2",               // 12
                "host2val",              // 13
                "--port2",               // 14
                "port2ival",             // 15
                "--bindDN2",             // 16
                "cn=Directory Manager",  // 17
                "--bindPassword2",       // 18
                "xxxxxxxx",              // 19
                "--replicationPort2",    // 20
                "8989",                  // 21
                "--adminUID",            // 22
                "admin",                 // 23
                "--adminPassword",       // 24
                "xxxxxxxx",              // 25
                "--baseDN",              // 26
                "dc=example,dc=com",     // 27
                "--trustAll",            // 28
                "--configFile",          // 29
                "path/to/config.ldif",   // 30
                "--noPropertiesFile"     // 31
        };
        enableCmd[3] = (String) map.get(SetupConstants.DS_EMB_REPL_HOST2);
        enableCmd[5] = (String) map.get(SetupConstants.DS_EMB_REPL_ADMINPORT2);
        enableCmd[11] = (String) map.get(SetupConstants.DS_EMB_REPL_REPLPORT2);
        enableCmd[13] = getOpenDJHostName(map);
        enableCmd[15] = (String) map.get(SetupConstants.CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT);
        enableCmd[21] = (String) map.get(SetupConstants.DS_EMB_REPL_REPLPORT1);
        enableCmd[27] = (String) map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX);
        enableCmd[30] = getOpenDJConfigFile(map);

        Object[] params = {concat(enableCmd)};
        SetupProgress.reportStart("emb.replcommand", params);

        enableCmd[9] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        enableCmd[19] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        enableCmd[25] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);

        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        if (debug.messageEnabled()) {
            debug.message("EmbeddedOpenDS.setupReplicationEnable: " +
                    "Host 1 " + enableCmd[3]);
            debug.message("EmbeddedOpenDS.setupReplicationEnable: " +
                    "Host 2 " + enableCmd[13]);
            debug.message("EmbeddedOpenDS.setupReplicationEnable: " +
                    "Port 1 " + enableCmd[5]);
            debug.message("EmbeddedOpenDS.setupReplicationEnable: " +
                    "Port 2 " + enableCmd[15]);
        }
        int ret = ReplicationCliMain.mainCLI(
                enableCmd, false,
                SetupProgress.getOutputStream(),
                SetupProgress.getOutputStream());

        if (ret == 0) {
            SetupProgress.reportEnd("emb.success", null);
        } else {
            SetupProgress.reportEnd("emb.failed", null);
        }
        return ret;
    }

    /**
     * Syncs replication data between two OpenDJ sms and user stores.
     * $ dsreplication initialize
     * --baseDN "dc=example,dc=com" --adminUID admin --adminPassword pass
     * --hostSource host1 --portSource 1389
     * --hostDestination host2 --portDestination 2389
     * --trustAll
     *
     * @param map Map of properties collected by the configurator.
     * @return status : 0 == success, !0 == failure
     */
    public static int setupReplicationInitialize(Map map) {
        String[] initializeCmd = {
                "initialize",                 // 0
                "--no-prompt",                // 1
                "--baseDN",                   // 2
                Constants.DEFAULT_ROOT_SUFFIX,// 3 Placeholder
                "--adminUID",                 // 4
                "admin",                      // 5
                "--adminPassword",            // 6
                "xxxxxxxx",                   // 7
                "--hostSource",               // 8
                "localhost",                  // 9
                "--portSource",               // 10
                "50389",                      // 11
                "--hostDestination",          // 12
                "localhost",                  // 13
                "--portDestination",          // 14
                "51389",                      // 15
                "--trustAll",                 // 16
                "--configFile",               // 17
                "path/to/config.ldif",        // 18
                "--noPropertiesFile"          // 19
        };
        initializeCmd[3] = (String) map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX);
        initializeCmd[9] = (String) map.get(SetupConstants.DS_EMB_REPL_HOST2);
        initializeCmd[11] = (String) map.get(SetupConstants.DS_EMB_REPL_ADMINPORT2);
        initializeCmd[13] = getOpenDJHostName(map);
        initializeCmd[15] = (String) map.get(
                SetupConstants.CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT);
        initializeCmd[18] = getOpenDJConfigFile(map);

        Object[] params = {concat(initializeCmd)};
        SetupProgress.reportStart("emb.replcommand", params);

        initializeCmd[7] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        int ret = ReplicationCliMain.mainCLI(initializeCmd, false,
                SetupProgress.getOutputStream(), SetupProgress.getOutputStream());

        if (ret == 0) {
            SetupProgress.reportEnd("emb.success", null);
        } else {
            SetupProgress.reportEnd("emb.failed", null);
        }
        return ret;
    }

    /**
     * Returns Replication Status by invoking OpenDJ <code>dsreplication</code>
     * CLI
     *
     * @param port   LDAP port number of embedded OpenDJ
     * @param passwd Directory Manager password
     * @param oo     Standard output
     * @param err    : Standard error
     * @return <code>dsreplication</code> CLI exit code.
     */
    public static int getReplicationStatus(String port, String passwd,
                                           OutputStream oo, OutputStream err) {
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);

        String[] statusCmd = {
                "status", "--no-prompt",
                "-h", "localhost",
                "-p", port,
                "--adminUID", "admin",
                "--adminPassword", passwd,
                "-s",
                "--configFile",
                baseDir + "/opends/config/config.ldif",
                "--noPropertiesFile"
        };
        if (debug.messageEnabled()) {
            String dbgcmd = concat(statusCmd).replaceAll(passwd, "****");
            debug.message("EmbeddedOpenDS:getReplicationStatus:exec dsreplication :"
                    + dbgcmd);
        }
        int ret = ReplicationCliMain.mainCLI(statusCmd, false, oo, err);
        if (debug.messageEnabled()) {
            debug.message("EmbeddedOpenDS:getReplicationStatus:dsreplication ret:"
                    + ret);
        }
        return ret;
    }

    /**
     * @return true if multi server option is selected in the configurator.
     */
    public static boolean isMultiServer(Map map) {
        String replFlag = (String) map.get(SetupConstants.DS_EMB_REPL_FLAG);
        if (replFlag != null && replFlag.startsWith(
                SetupConstants.DS_EMP_REPL_FLAG_VAL)) {
            return true;
        }
        return false;
    }

    private static String concat(String[] args) {
        String ret = "";
        for (int i = 0; i < args.length; i++)
            ret += args[i] + " ";

        return ret;
    }

    /**
     * Utility function to preload data in the embedded instance.
     * Must be called when the directory instance is shutdown.
     *
     * @param odsRoot Local directory where <code>OpenDJ</code> is installed.
     * @param ldif    Full path of the ldif file to be loaded.
     */
    public static int loadLDIF(Map map, String odsRoot, String ldif) {
        int ret = 0;

        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        File ldifFile = new File(ldif);
        if (!ldifFile.exists()) {
            debug.error("LDIF File:" + ldifFile.getAbsolutePath() + " does not exist, unable to load!");
            return -1;
        }
        try {
            if (debug.messageEnabled()) {
                debug.message("EmbeddedOpenDS:loadLDIF(" + ldif + ")");
            }

            String[] args1 = new String[]{
                    "-f",
                    odsRoot + "/config/config.ldif",
                    "-n",
                    "userRoot",
                    "-l",
                    ldif,
                    "--trustAll",
                    "-D",
                    (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_DN),
                    "-w",
                    (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD),
                    "--noPropertiesFile"
            };
            ret = org.opends.server.tools.ImportLDIF.mainImportLDIF(args1, false,
                    SetupProgress.getOutputStream(), SetupProgress.getOutputStream());

            if (debug.messageEnabled()) {
                debug.message("EmbeddedOpenDS:loadLDIF Success");
            }
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS:loadLDIF:ex=", ex);
        }

        return ret;
    }

    /**
     * Returns a one-way hash for passwd using SSHA512 scheme.
     *
     * @param p Clear password string
     * @return hash value
     */
    public static String hash(String p) {
        String str = null;
        try {
            byte[] bb = p.getBytes();
            str = SaltedSHA512PasswordStorageScheme.encodeOffline(bb);
        } catch (Exception ex) {
            Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
            debug.error("EmbeddedOpenDS.hash failed : ex=" + ex);
        }
        return str;
    }

    /**
     * Get replication port
     *
     * @param username
     * @param password
     * @param hostname
     * @param port
     * @return port number if replication is setup, null if not or on error.
     */
    public static String getReplicationPort(
            String username,
            String password,
            String hostname,
            String port
    ) {
        final String replDN =
                "cn=replication server,cn=Multimaster Synchronization,cn=Synchronization Providers,cn=config";
        final String[] attrs = {"ds-cfg-replication-port"};
        String replPort = null;
        Connection ld = null;
        username = "cn=Directory Manager";
        try (Connection conn = getLDAPConnection(hostname, port, username, password)) {
            // We'll use Directory Manager
            if (conn != null) {
                SearchResultEntry le = conn.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(replDN, attrs));
                if (le != null) {
                    Attribute la = le.getAttribute(attrs[0]);
                    if (la != null) {
                        replPort = la.firstValueAsString();
                    }
                }
            }
        } catch (Exception ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME)
                    .error("EmbeddedOpenDS.getReplicationPort(). Error getting replication port:", ex);
        }
        return replPort;

    }

    /**
     * Get admin port of the OpenDJ server
     *
     * @param username The username of the directory admin
     * @param password The password of the directory admin
     * @param hostname The hostname of the directory server
     * @param port     The port of the directory server
     * @return The admin port
     */
    public static String getAdminPort(
            String username,
            String password,
            String hostname,
            String port
    ) {
        final String adminConnectorDN = "cn=Administration Connector,cn=config";
        final String[] attrs = {"ds-cfg-listen-port"};
        String adminPort = null;
        Connection ld = null;

        try (Connection conn = getLDAPConnection(hostname, port, username, password)) {
            if (conn != null) {
                SearchResultEntry le = conn.searchSingleEntry(
                        LDAPRequests.newSingleEntrySearchRequest(adminConnectorDN, attrs));

                if (le != null) {
                    Attribute la = le.getAttribute(attrs[0]);

                    if (la != null) {
                        adminPort = la.firstValueAsString();
                    }
                }
            }
        } catch (Exception ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME)
                    .error("EmbeddedOpenDS.getAdminPort(). Error getting admin port:", ex);
        }

        return adminPort;
    }

    /**
     * Synchronizes replication server info with current list of OpenAM servers.
     */
    public static boolean syncReplicatedServers(
            Set currServerSet, String port, String passwd) {
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        debug.message("EmbeddedOPenDS:syncReplication:start processing.");
        String[] args = {
                "-p", port,      // 1 : ds port num
                "-h", "localhost",
                "-D", "cn=directory manager",
                "-w", passwd,    // 7 : password
                "list-replication-server",
                "--provider-name", "Multimaster Synchronization",
                "--property", "replication-server",
                "--property", "replication-port", "--no-prompt", "--trustAll",
                "--noPropertiesFile"
        };
        if (debug.messageEnabled()) {
            String dbgcmd = concat(args).replaceAll(passwd, "****");
            debug.message("EmbeddedOpenDS:syncReplication:exec dsconfig:"
                    + dbgcmd);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream boe = new ByteArrayOutputStream();

        DSConfig.main(args, bos, boe);
        String str = bos.toString();
        String stre = boe.toString();
        final boolean hasReplicationServers = !stre.contains("Unable to continue since there are no Replication " +
                "Server currently");
        if (!stre.isEmpty() && hasReplicationServers) {
            debug.error("EmbeddedOpenDS:syncReplication: stderr is not empty:"
                    + stre);
            return false;
        } else {
            if (debug.messageEnabled()) {
                debug.message("EmbeddedOpenDS:syncReplication: stderr is not empty:"
                        + stre);
            }
        }

        /*
             v2.6 output example

             Replication Server : replication-server                           : replication-port
             -------------------:----------------------------------------------:-----------------
             replication-server : dj1.example.com:58989, dj2.example.com:50889 : 50889

         */
        BufferedReader brd = new BufferedReader(new StringReader(str));
        String line = null;
        try {
            line = brd.readLine(); // 1st line
            line = brd.readLine(); // 2nd line
            line = brd.readLine(); // 3rd line
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS:syncReplication:Failed:", ex);
        }
        if (line == null) {
            if (hasReplicationServers) {
                debug.error("EmbeddedOpenDS:syncReplication:cmd failed" + str);
            }
            return false;
        }
        try {
            int lastcolon = line.lastIndexOf(':');
            int stcolon = line.indexOf(':');
            String replservers = line.substring(stcolon + 1, lastcolon);

            StringTokenizer stok = new StringTokenizer(replservers, ",");
            // Check if this server is part of server list
            List cmdlist = new ArrayList();
            cmdlist.add("-p");
            cmdlist.add(port);
            cmdlist.add("-h");
            cmdlist.add("localhost");
            cmdlist.add("-D");
            cmdlist.add("cn=directory manager");
            cmdlist.add("-w");
            cmdlist.add(passwd);
            cmdlist.add("--no-prompt");
            cmdlist.add("--trustAll");
            cmdlist.add("set-replication-server-prop");
            cmdlist.add("--provider-name");
            cmdlist.add("Multimaster Synchronization");
            cmdlist.add("--noPropertiesFile");

            int numremoved = 0;
            while (stok.hasMoreTokens()) {
                String tok = stok.nextToken().trim();
                if (!currServerSet.contains(tok)) {
                    cmdlist.add("--remove");
                    cmdlist.add("replication-server:" + tok);
                    numremoved++;
                }
            }
            if (numremoved > 0) {
                String[] args1 =
                        (String[]) cmdlist.toArray(new String[cmdlist.size()]);
                if (debug.messageEnabled()) {
                    String dbgcmd1 = concat(args1).replaceAll(passwd, "****");
                    debug.message("EmbeddedOpenDS:syncReplication:Execute:" +
                            dbgcmd1);
                }
                bos = new ByteArrayOutputStream();
                boe = new ByteArrayOutputStream();
                DSConfig.main(args1, bos, boe);
                str = bos.toString();
                stre = boe.toString();
                if (debug.messageEnabled()) {
                    debug.message("EmbeddedOpenDS:syncReplication:Result:" +
                            str);
                }
                if (stre.length() != 0) {
                    debug.error("EmbeddedOpenDS:syncReplication:cmd stderr:"
                            + stre);
                }
            }
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS:syncReplication:Failed:", ex);
            return false;
        }
        return true;
    }

    /**
     * Synchronizes replication domain info with current list of OpenAM servers.
     */
    public static boolean syncReplicatedDomains(
            Set currServerSet, String port, String passwd) {
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        debug.message("EmbeddedOpenDS:syncReplication:Domains:started");
        String[] args = {
                "-p", port,      // 1 : ds port num
                "-h", "localhost",
                "-D", "cn=directory manager",
                "-w", passwd,    // 7 : password
                "list-replication-domains",
                "--provider-name", "Multimaster Synchronization",
                "--property", "replication-server",
                "--no-prompt",
                "--trustAll",
                "--noPropertiesFile"
        };
        if (debug.messageEnabled()) {
            String dbgcmd = concat(args).replaceAll(passwd, "****");
            debug.message("EmbeddedOpenDS:syncReplication:exec dsconfig:"
                    + dbgcmd);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayOutputStream boe = new ByteArrayOutputStream();
        DSConfig.main(args, bos, boe);
        String str = bos.toString();
        String stre = boe.toString();
        if (stre.length() != 0) {
            debug.error("EmbeddedOpenDS:syncReplication:stderr:" + stre);
        }
        /*
            v2.6 output example

            Replication Domain            : replication-server
            ------------------------------:---------------------------------------------
            cn=admin data                 : dj1.example.com:58989, dj2.example.com:50889
            cn=schema                     : dj1.example.com:58989, dj2.example.com:50889
            dc=openam,dc=openidentityplatform,dc=org : dj1.example.com:58989, dj2.example.com:50889
         */

        BufferedReader brd = new BufferedReader(new StringReader(str));
        String line = null;
        try {
            line = brd.readLine(); // 1st line
            line = brd.readLine(); // 2nd line
            while ((line = brd.readLine()) != null) {
                try {
                    int dcolon = line.indexOf(':');
                    String domainname = line.substring(0, dcolon).trim();
                    int stcolon = dcolon + 1;
                    String replservers = line.substring(stcolon + 1);
                    if (debug.messageEnabled()) {
                        debug.message("EmbeddedOpenDS:syncRepl:domain=" +
                                domainname + " replservers=" + replservers);
                    }

                    StringTokenizer stok = new StringTokenizer(replservers, ",");
                    // Check if this server is part of server list
                    List cmdlist = new ArrayList();
                    cmdlist.add("-p");
                    cmdlist.add(port);
                    cmdlist.add("-h");
                    cmdlist.add("localhost");
                    cmdlist.add("-D");
                    cmdlist.add("cn=directory manager");
                    cmdlist.add("-w");
                    cmdlist.add(passwd);
                    cmdlist.add("--no-prompt");
                    cmdlist.add("--trustAll");
                    cmdlist.add("set-replication-domain-prop");
                    cmdlist.add("--provider-name");
                    cmdlist.add("Multimaster Synchronization");
                    cmdlist.add("--domain-name");
                    cmdlist.add(domainname);
                    cmdlist.add("--noPropertiesFile");

                    int numremoved = 0;
                    while (stok.hasMoreTokens()) {
                        String tok = stok.nextToken().trim();
                        if (!currServerSet.contains(tok)) {
                            cmdlist.add("--remove");
                            cmdlist.add("replication-server:" + tok);
                            numremoved++;
                        }
                    }
                    if (numremoved > 0) {
                        String[] args1 =
                                (String[]) cmdlist.toArray(new String[cmdlist.size()]);
                        if (debug.messageEnabled()) {
                            String dbgcmd1 =
                                    concat(args1).replaceAll(passwd, "****");
                            debug.message("EmbeddedOpenDS:syncReplication:Execute:" +
                                    dbgcmd1);
                        }
                        bos = new ByteArrayOutputStream();
                        boe = new ByteArrayOutputStream();
                        DSConfig.main(args1, bos, boe);
                        str = bos.toString();
                        stre = boe.toString();
                        if (stre.length() != 0) {
                            debug.error("EmbeddedOpenDS:syncRepl:stderr=" + stre);
                        }
                        if (debug.messageEnabled()) {
                            debug.message("EmbeddedOpenDS:syncReplication:Result:" +
                                    str);
                        }
                    }
                } catch (Exception ex) {
                    debug.error("EmbeddedOpenDS:syncReplication:Failed:", ex);
                    return false;
                }
            }
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS:syncReplication:Failed:", ex);
            return false;
        }
        return true;
    }

    /**
     * Synchronizes replication domain info with current list of OpenAM servers.
     */
    public static boolean syncReplicatedServerList(
            Set currServerSet, String port, String passwd) {
        try (Connection conn = getLDAPConnection("localhost", port, "cn=Directory Manager", passwd)) {
            Set<String> dsServers = getServerSet(conn);

            if (dsServers == null) {
                return false;
            }
            for (String tok : dsServers) {
                if (!currServerSet.contains(tok)) {
                    delOpenDSServer(conn, tok);
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    /**
     * Helper method to return Ldap connection to a embedded OpenDJ
     * server.
     *
     * @return Ldap connection
     */
    private static Connection getLDAPConnection(String dsHostName, String dsPort, String dsManager,
            String dsAdminPwd) {
        try {
            return getLDAPConnectionFactory(dsHostName, dsPort, dsManager, dsAdminPwd).getConnection();
        } catch (LdapException ex) {
            Debug.getInstance(SetupConstants.DEBUG_NAME)
                    .error("EmbeddedOpenDS.setup(). Error getting LDAPConnection:", ex);
        }
        return null;
    }

    private static synchronized ConnectionFactory getLDAPConnectionFactory(String dsHostName, String dsPort,
            String dsManager, String dsAdminPwd) {
        if (factory == null) {

            // All connections will use authentication
            Options options = Options.defaultOptions()
                    .set(AUTHN_BIND_REQUEST, LDAPRequests.newSimpleBindRequest(dsManager, dsAdminPwd.toCharArray()))
                    .set(CONNECT_TIMEOUT, new Duration((long)3, TimeUnit.SECONDS));

            factory = new LDAPConnectionFactory(dsHostName, Integer.parseInt(dsPort), options);
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

    static final String replDN = "cn=all-servers,cn=Server Groups,cn=admin data";

    /**
     * Removes host:port from OpenDJ replication
     */
    public static void delOpenDSServer(Connection lc, String delServer) {
        String replServerDN =
                "cn=" + delServer + ",cn=Servers,cn=admin data";
        final String[] attrs = {"ds-cfg-key-id"};
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        if (lc == null) {
            debug.error("EmbeddedOpenDS:syncOpenDSServer():" +
                    "Could not connect to local OpenDJ instance." + replServerDN);
            return;
        }
        String trustKey = null;
        try {
            SearchResultEntry le = lc.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(replServerDN, attrs));
            if (le != null) {
                Attribute la = le.getAttribute(attrs[0]);
                if (la != null) {
                    trustKey = la.firstValueAsString();
                }
                String keyDN = "ds-cfg-key-id=" + trustKey +
                        ",cn=instance keys,cn=admin data";
                lc.delete(LDAPRequests.newDeleteRequest(keyDN));
            } else {
                debug.error("EmbeddedOpenDS:syncOpenDSServer():" +
                        "Could not find trustkey for:" + replServerDN);
            }
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS.syncOpenDSServer()." +
                    " Error getting replication key:", ex);

        }
        try {
            lc.delete(LDAPRequests.newDeleteRequest(replServerDN));
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS.syncOpenDSServer()." +
                    " Error getting deleting server entry:" + replServerDN, ex);

        }
        try {
            ModifyRequest modifyRequest = LDAPRequests.newModifyRequest(replDN)
                    .addModification(new Modification(ModificationType.DELETE,
                            Attributes.singletonAttribute("uniqueMember", "cn=" + delServer)));
            lc.modify(modifyRequest);
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS.syncOpenDSServer()." +
                    " Error getting removing :" + replDN, ex);

        }
    }

    /**
     * Gets list of replicated servers from local OpenDJ directory.
     */
    public static Set getServerSet(Connection lc) {
        final String[] attrs = {"uniqueMember"};
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        try {
            if (lc != null) {
                SearchResultEntry le = lc.searchSingleEntry(LDAPRequests.newSingleEntrySearchRequest(replDN, attrs));
                if (le != null) {
                    Set hostSet = new HashSet();
                    Attribute la = le.getAttribute(attrs[0]);
                    if (la != null) {
                        for (ByteString value : la) {
                            hostSet.add(value.toString().substring(3, value.length()));
                        }
                    }
                    return hostSet;
                } else {
                    debug.error("EmbeddedOpenDS:syncOpenDSServer():" +
                            "Could not find trustkey for:" + replDN);
                }
            } else {
                debug.error("EmbeddedOpenDS:syncOpenDSServer():" +
                        "Could not connect to local opends instance.");
            }
        } catch (Exception ex) {
            debug.error("EmbeddedOpenDS.syncOpenDSServer()." +
                    " Error getting replication key:", ex);

        }
        return null;
    }


    /**
     * Rebuilds SMS indexes for the embedded DJ config store.
     *
     * @return the status code.
     */
    public static int rebuildSMSIndex() throws Exception {
        return rebuildIndex(AMSetupServlet.getBaseDir(), SMSEntry.getRootSuffix());
    }

    /**
     * Rebuilds indexes for the given base DN and installation directory.
     *
     * @param baseDir the base installation directory.
     * @param baseDN the base DN to rebuild indexes for.
     * @return the status code
     */
    public static int rebuildIndex(String baseDir, String baseDN) throws Exception {
        Map<String, String> rebuildIndexData = new HashMap<String, String>(2);
        rebuildIndexData.put(SetupConstants.CONFIG_VAR_BASE_DIR, baseDir);
        rebuildIndexData.put(SetupConstants.CONFIG_VAR_ROOT_SUFFIX, baseDN);
        return rebuildIndex(rebuildIndexData);
    }

    // Programmatic way of rebuilding indexes in OpenDJ.
    // This method simulates the OpenDJ cli command rebuild-index.
    // eg., rebuild-index -b dc=example,dc=com -i uid -i mail

    public static int rebuildIndex(Map map) throws Exception {
        int ret = 0;
        shutdownServer("Rebuild index");
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);

        String[] args = {
                "--configFile",
                getOpenDJConfigFile(map),
                "--baseDN",
                (String) map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX),
                "--rebuildAll",
                "--noPropertiesFile",
                "--offline"};
        OutputStream bos = new ByteArrayOutputStream();
        OutputStream boe = new ByteArrayOutputStream();
        TimeThread.start();
        ret = RebuildIndex.mainRebuildIndex(args, true, bos, boe);
        TimeThread.stop();
        String outStr = bos.toString();
        String errStr = boe.toString();
        if (errStr.length() != 0) {
            debug.error("EmbeddedOpenDS:rebuildIndex:stderr=" +
                    errStr);
        }
        if (debug.messageEnabled()) {
            String msg = "msg=Rebuild complete.";
            int idx = outStr.indexOf(msg);
            if (idx >= 0) {
                debug.message("EmbeddedOpenDS:rebuildIndex: " +
                        "Rebuild Status: " + outStr.substring(idx));
            }
            debug.message("EmbeddedOpenDS:rebuildIndex:Result:" +
                    outStr);
        }
        startServer(getOpenDJBaseDir(map));
        return ret;
    }

    /**
     * @return true if installed OpenDS is version 1.0.2
     */
    public static boolean isOpenDSVer1Installed() {
        boolean openDSVer1x = false;

        if (getOpenDSVersion().equals(OPENDS_1x_VER)) {
            openDSVer1x = true;
        }

        return openDSVer1x;
    }

    /**
     * @return true if installed OpenDS is version 2.3.0BACKPORT2
     */
    public static boolean isOpenDSVer230Installed() {
        boolean openDSVer230b2 = false;

        if (getOpenDSVersion().equals(OPENDS_230B2_VER)) {
            openDSVer230b2 = true;
        }

        return openDSVer230b2;
    }

    public static String getOpenDSVersion() {
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        String odsRoot = AMSetupServlet.getBaseDir() + "/" + SetupConstants.SMS_OPENDS_DATASTORE;
        String version = "unknown";

        File configLdif = new File(odsRoot + OPENDS_UPGRADE_DIR);

        File buildInfo = new File(odsRoot + "/" + "config" + "/" + SetupConstants.OPENDJ_BUILDINFO);

        if (configLdif.exists() && configLdif.isDirectory()) {
            String[] configFile = configLdif.list(new FilenameFilter() {
                //@Override -- Not Allowed Here.
                public boolean accept(File dir, String name) {
                    return name.startsWith(OPENDS_CONFIG_LDIF);
                }
            });

            if (configFile.length != 0) {
                version = configFile[0].substring(configFile[0].lastIndexOf('.') + 1);
            } else {
                debug.error("Unable to determine OpenDJ version");
            }
        } else if (buildInfo.exists() && buildInfo.canRead() && buildInfo.isFile()) {
              String buildInfoVersionText = getOpenDJBuildInfo(buildInfo);
              if ( (buildInfoVersionText != null) && (!buildInfoVersionText.isEmpty()) ) {
                  version = buildInfoVersionText.trim();
              } else {
                  debug.error("Unable to determine OpenDJ version");
              }
        } else {
            if (debug.warningEnabled()) {
                debug.warning("Unable to determine OpenDJ version; could be pre-config");
            }
        }

        if (debug.messageEnabled()) {
            debug.message("Found OpenDJ version: " + version);
        }

        return version;
    }

    /**
     * Initialises OpenDJ for client use, used by the CLI tools
     */
    public static void initializeForClientUse() {
        EmbeddedUtils.initializeForClientUse();
    }

    // Returns the installation directory for the embedded OpenDJ.
    private static String getOpenDJBaseDir(Map configProperties) {
        String basedir = (String) configProperties
                .get(SetupConstants.CONFIG_VAR_BASE_DIR);
        return basedir + "/" + SetupConstants.SMS_OPENDS_DATASTORE;
    }

    // Returns the configuration file name for the embedded OpenDJ.
    private static String getOpenDJConfigFile(Map configProperties) {
        return getOpenDJBaseDir(configProperties) + "/config/config.ldif";
    }


    // Returns the host name for the embedded OpenDJ.
    private static String getOpenDJHostName(Map configProperties) {
        String dirHost = (String) configProperties
                .get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST);
        if (dirHost.equals("localhost")) {
            dirHost = (String) configProperties
                    .get(SetupConstants.CONFIG_VAR_SERVER_HOST);
        }
        return dirHost;
    }

    // Helper Method to return the first line of the OpenDJ buildInfo file.
    private static String getOpenDJBuildInfo(File buildInfo) {
        try {
            Scanner scan = new Scanner(buildInfo);
            while (scan.hasNextLine()) {
                return scan.nextLine();
            }
        } catch (FileNotFoundException e) {
            // Simply Ignore this Exception and Allow UpStream to Handle Null.
        }
        return null;
    }
}
