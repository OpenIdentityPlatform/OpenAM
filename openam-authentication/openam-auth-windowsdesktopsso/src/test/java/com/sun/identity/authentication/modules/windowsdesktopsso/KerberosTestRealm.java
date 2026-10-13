/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.authentication.modules.windowsdesktopsso;

import org.apache.commons.io.FileUtils;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import sun.security.krb5.Config;

import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.io.File;
import java.nio.file.Files;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

/**
 * A Kerberos realm for tests: an Apache Kerby KDC on a random local port and a user logged in to it with the JDK
 * Kerberos client.
 * <p>
 * Not part of the PowerMock test class: PowerMock routes that class's calls to JDK classes through reflection,
 * which fails for JDK packages that the surefire argLine does not open, such as java.nio.file.
 * <p>
 * Changes JVM-wide state while running: {@code java.security.krb5.conf} points at the KDC's krb5.conf, the JDK caches
 * that krb5.conf, and the global JAAS configuration has the client entry. {@link #stop()} restores the properties and
 * the JAAS configuration and reloads the JDK's krb5.conf.
 */
final class KerberosTestRealm {

    private static final String CLIENT_LOGIN = "kerberos-test-client";
    private static final Oid KERBEROS_PRINCIPAL_NAME = oid("1.2.840.113554.1.2.2.1");
    // Set by Kerby and by the module's service login.
    private static final String[] SYSTEM_PROPERTIES = {"java.security.krb5.conf", "java.security.krb5.realm",
            "java.security.krb5.kdc", "java.security.auth.login.config"};

    private final Map<String, String> savedProperties = new HashMap<>();
    private Configuration savedConfiguration;
    private File workDir;
    private SimpleKdcServer kdc;
    private LoginContext clientLogin;
    private Subject client;

    /**
     * Starts the KDC with the given principals, all with random keys, and logs the user in.
     *
     * @param realm the Kerberos realm.
     * @param user the principal of the client user.
     * @param services service principals to create.
     * @throws Exception if the KDC does not start or the user cannot log in.
     */
    void start(String realm, String user, String... services) throws Exception {
        for (String name : SYSTEM_PROPERTIES) {
            savedProperties.put(name, System.getProperty(name));
        }
        // Creates the default configuration if none is installed yet, so the one restored by stop() is that default.
        savedConfiguration = Configuration.getConfiguration();
        workDir = Files.createTempDirectory("kerberos-test-realm").toFile();
        kdc = new SimpleKdcServer();
        kdc.setWorkDir(workDir);
        kdc.setKdcRealm(realm);
        kdc.setAllowUdp(false);
        // Writes workDir/krb5.conf with "kdc = localhost:<port>" and sets java.security.krb5.conf to it.
        kdc.init();
        kdc.start();
        kdc.createPrincipal(user);
        for (String service : services) {
            kdc.createPrincipal(service);
        }

        // The default JAAS configuration cannot be created once java.security.auth.login.config names a missing
        // file, as the "/dev/null" set by the module does on Windows. A configuration set here is used instead.
        Configuration.setConfiguration(new ClientConfiguration(user, exportKeytab(user)));
        clientLogin = new LoginContext(CLIENT_LOGIN);
        clientLogin.login();
        client = clientLogin.getSubject();
    }

    /**
     * Exports the keys of a principal.
     *
     * @param principal the principal.
     * @return the keytab file.
     * @throws Exception if the keytab cannot be written.
     */
    File exportKeytab(String principal) throws Exception {
        File keytab = new File(workDir, principal.replaceAll("\\W", "_") + ".keytab");
        kdc.exportPrincipal(principal, keytab);
        return keytab;
    }

    /**
     * Returns the first token the user sends to a service, as a browser does after "WWW-Authenticate: Negotiate".
     *
     * @param servicePrincipal the service principal, with the realm.
     * @param mech the GSS mechanism OID: SPNEGO or Kerberos V5.
     * @return the token.
     * @throws Exception if the KDC does not issue a ticket for the service.
     */
    byte[] initialToken(String servicePrincipal, String mech) throws Exception {
        return Subject.doAs(client, (PrivilegedExceptionAction<byte[]>) () -> {
            GSSManager manager = GSSManager.getInstance();
            GSSName service = manager.createName(servicePrincipal, KERBEROS_PRINCIPAL_NAME);
            GSSContext context = manager.createContext(service, oid(mech), null, GSSContext.DEFAULT_LIFETIME);
            try {
                return context.initSecContext(new byte[0], 0, 0);
            } finally {
                context.dispose();
            }
        });
    }

    /**
     * Stops the KDC, logs the user out, restores the JAAS configuration and the system properties and reloads the
     * JDK's krb5.conf.
     *
     * @throws Exception if the KDC does not stop, the user cannot be logged out or krb5.conf cannot be reloaded.
     */
    void stop() throws Exception {
        try {
            if (kdc != null) {
                kdc.stop();
            }
        } finally {
            Configuration.setConfiguration(savedConfiguration);
            for (Map.Entry<String, String> property : savedProperties.entrySet()) {
                if (property.getValue() == null) {
                    System.clearProperty(property.getKey());
                } else {
                    System.setProperty(property.getKey(), property.getValue());
                }
            }
            FileUtils.deleteQuietly(workDir);
            // Drops Kerby's realm and KDC port, which later Kerberos code in this JVM would otherwise still use.
            Config.refresh();
            if (clientLogin != null) {
                clientLogin.logout();
            }
        }
    }

    private static Oid oid(String value) {
        try {
            return new Oid(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(value, e);
        }
    }

    /** JAAS entry of the client: the user's TGT, obtained with the user's keytab. */
    private static final class ClientConfiguration extends Configuration {
        private final String user;
        private final File keytab;

        ClientConfiguration(String user, File keytab) {
            this.user = user;
            this.keytab = keytab;
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            if (!CLIENT_LOGIN.equals(name)) {
                return null;
            }
            Map<String, String> options = new HashMap<>();
            options.put("principal", user);
            options.put("useKeyTab", "true");
            options.put("keyTab", keytab.getAbsolutePath());
            options.put("doNotPrompt", "true");
            // The JDK reads krb5.conf once per JVM; this one appeared after the JVM started.
            options.put("refreshKrb5Config", "true");
            return new AppConfigurationEntry[] {new AppConfigurationEntry(
                    "com.sun.security.auth.module.Krb5LoginModule",
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options)};
        }
    }
}
