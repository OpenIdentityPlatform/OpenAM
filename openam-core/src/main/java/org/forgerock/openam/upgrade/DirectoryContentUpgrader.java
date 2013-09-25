/*
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.upgrade;

import com.sun.identity.setup.AMSetupServlet;
import com.sun.identity.setup.EmbeddedOpenDS;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.fields.CoreTokenField;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.EntryNotFoundException;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldif.ChangeRecordReader;
import org.forgerock.opendj.ldif.ChangeRecordWriter;
import org.forgerock.opendj.ldif.ConnectionChangeRecordWriter;
import org.forgerock.opendj.ldif.LDIFChangeRecordReader;

/**
 * This class is aiming to upgrade the content of the configuration store. The possible changes may involve directory
 * schema, modifying entries and in case of using embedded: creating and rebuilding indexes.
 * The upgrade of an embedded instance is handled by {@link OpenDJUpgrader} separately from this upgrader.
 * At the moment the following changes are made to the configuration store:
 * <ul>
 *  <li>Adding the schema for CTSv2 if frCoreToken objectclass isn't already defined.</li>
 *  <li>Creating the CTS containers in the directory if they don't already exist.</li>
 *  <li>Creating the indexes for CTSv2 if embedded configstore is used.</li>
 *  <li>Adding the schema for the dashboard service to the embedded user store.</li>
 *  <li>Adding the schema for deviceprint module to the embedded user store.</li>
 * </ul>
 *
 * @author Peter Major
 */
public class DirectoryContentUpgrader {

    private static final Debug DEBUG = Debug.getInstance("amUpgrade");
    private static final String DASHBOARD_OC = "forgerock-am-dashboard-service";
    private static final String DEVICE_PRINT_OC = "devicePrintProfilesContainer";
    private final List<Upgrader> upgraders = new ArrayList<Upgrader>();
    private final DataLayerConnectionFactory connFactory;
    private final String baseDir;
    private final String baseDN;

    /**
     * This constructor will initialize the different directory content upgraders and ensures that each of them are
     * actually applicable. At the end this upgrader will have a list of {@link Upgrader}s that needs to be executed.
     *
     * @param baseDir The base directory of OpenAM (where the configuration can be found).
     * @param baseDN The base DN of the configuration store.
     * @throws UpgradeException If there was a problem while checking if a given Upgrader is applicable.
     */
    public DirectoryContentUpgrader(String baseDir, String baseDN) throws UpgradeException {
        this.baseDir = baseDir;
        this.baseDN = baseDN;
        connFactory = InjectorHolder.getInstance(DataLayerConnectionFactory.class);
        upgraders.add(new AddCTSSchema());
        upgraders.add(new CreateCTSContainer());
        if (EmbeddedOpenDS.isStarted()) {
            upgraders.add(new CreateCTSIndexes());
            upgraders.add(new AddDashboardSchema());
            upgraders.add(new AddDevicePrintSchema());
        }
        Connection conn = null;
        try {
            conn = connFactory.getConnection();
            Schema schema = null;
            try {
                schema = Schema.readSchemaForEntry(conn, DN.valueOf(baseDN)).asStrictSchema();
            } catch (ErrorResultException ere) {
                DEBUG.error("Unable to read directory schema, the schema won't be upgraded", ere);
            }
            Iterator<Upgrader> it = upgraders.iterator();
            while (it.hasNext()) {
                if (!it.next().isUpgradeNecessary(conn, schema)) {
                    it.remove();
                }
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to get a connection", ere);
            throw new UpgradeException(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
    }

    /**
     * As its name says, this method checks if a given entry exists in the directory or not.
     *
     * @param conn The connection to use when searching for the entry.
     * @param dn The DN of the entry that we are looking for.
     * @return <code>false</code> if the entry does not exist yet, <code>true</code> otherwise.
     * @throws UpgradeException If there was a problem while checking the existance of the entry.
     */
    private boolean entryExists(Connection conn, DN dn) throws UpgradeException {
        try {
            conn.readEntry(dn, "dn");
        } catch (EntryNotFoundException enfe) {
            return false;
        } catch (ErrorResultException ere) {
            DEBUG.error("Unable to read entry with dn: " + dn, ere);
            throw new UpgradeException(ere);
        }
        return true;
    }

    /**
     * Returns the list of LDIF files that needs to be processed as part of the upgrade. Used by
     * {@link org.forgerock.openam.upgrade.steps.UpgradeDirectoryContentStep} when generating the upgrade reports.
     *
     * @return The path to the LDIF files that needs to be processed.
     */
    public List<String> getLDIFPaths() {
        List<String> ret = new ArrayList<String>(upgraders.size());
        for (Upgrader upgrader : upgraders) {
            ret.add(upgrader.getLDIFPath());
        }
        return ret;
    }

    /**
     * Reads in the LDIF file, and writes the changerecords on the LDAP connection.
     *
     * @param conn The connection to use to write the changes to.
     * @param path The path to the LDIF file that needs to be processed.
     * @throws UpgradeException If there was an error while processing either the LDIF file, or the changerecord
     * itself.
     */
    private void processLDIF(Connection conn, String path) throws UpgradeException {
        ChangeRecordReader reader = null;
        try {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Processing LDIF file under " + path);
            }
            Map<String, String> tags = new HashMap<String, String>(2);
            tags.put("@SM_CONFIG_ROOT_SUFFIX@", baseDN);
            //external configstore won't get the indexes during upgrade, and this is safe for embedded
            tags.put("@DB_NAME@", "userRoot");
            StringBuffer content = AMSetupServlet.readFile(path);
            String tagSwapped = UpgradeServices.tagSwap(tags, content.toString());
            reader = new LDIFChangeRecordReader(
                    new ByteArrayInputStream(tagSwapped.getBytes(Charset.forName("UTF-8"))));
            ChangeRecordWriter writer = new ConnectionChangeRecordWriter(conn);
            while (reader.hasNext()) {
                writer.writeChangeRecord(reader.readChangeRecord());
            }
        } catch (IOException ioe) {
            DEBUG.error("An error occurred while processing " + path, ioe);
            //since this change should be applicable let's wrap this exception and interrupt upgrade.
            throw new UpgradeException(ioe);
        } finally {
            //No need to close writer, #upgrade() should close the connection after the upgrade is complete
            IOUtils.closeIfNotNull(reader);
        }
    }

    /**
     * Tells whether there is any LDIF file to process as part of the upgrade.
     * @return <code>true</code> if there is at least one LDIF file to process as part of the upgrade.
     */
    public boolean isApplicable() {
        return !upgraders.isEmpty();
    }

    /**
     * Performs the upgrade by traversing through the candidate LDIF files and tries to process them. If embedded
     * configuration store is used the indexes are also rebuilt as part of the upgrade. That will make sure that the
     * newly created indexes are all operational.
     *
     * @throws UpgradeException If there was an error while processing the LDIF files.
     */
    public void upgrade() throws UpgradeException {
        Connection conn = null;
        try {
            conn = connFactory.getConnection();
            for (Upgrader upgrader : upgraders) {
                processLDIF(conn, upgrader.getLDIFPath());
            }
        } catch (ErrorResultException ere) {
            DEBUG.error("An error occurred while trying to get a connection", ere);
            throw new UpgradeException(ere);
        } finally {
            IOUtils.closeIfNotNull(conn);
        }
        if (EmbeddedOpenDS.isStarted()) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Rebuilding indexes in embedded directory");
            }
            Map<String, String> rebuildIndexData = new HashMap<String, String>(2);
            rebuildIndexData.put(SetupConstants.CONFIG_VAR_BASE_DIR, baseDir);
            rebuildIndexData.put(SetupConstants.CONFIG_VAR_ROOT_SUFFIX, baseDN);
            try {
                EmbeddedOpenDS.rebuildIndex(rebuildIndexData);
            } catch (Exception ex) {
                throw new UpgradeException(ex);
            }
        }
    }

    /**
     * Defines a given change to the directory schema/content. As such it can provide the path to the LDIF file that
     * needs to be processed, and as well detect if performing the given change is necessary at all.
     */
    private interface Upgrader {

        /**
         * Returns the path to LDIF file this Upgrader represents.
         * @return The path to the LDIF file.
         */
        public String getLDIFPath();

        /**
         * Tells whether a given change to the schema/content is already applied, or upgrade is necessary.
         *
         * @param conn The connection to check given conditions against.
         * @param schema The directory schema.
         * @return <code>true</code> if processing the LDIF file is necessary for the directory to be up-to-date.
         * @throws UpgradeException If there was an error while performing the upgrade check.
         */
        public boolean isUpgradeNecessary(Connection conn, Schema schema) throws UpgradeException;
    }

    private class AddCTSSchema implements Upgrader {

        @Override
        public String getLDIFPath() {
            return "/WEB-INF/template/ldif/sfha/cts-add-schema.ldif";
        }

        @Override
        public boolean isUpgradeNecessary(Connection conn, Schema schema) {
            return schema != null && !schema.hasObjectClass(CoreTokenConstants.FR_CORE_TOKEN);
        }
    }

    private class CreateCTSContainer implements Upgrader {

        @Override
        public String getLDIFPath() {
            return "/WEB-INF/template/ldif/sfha/cts-container.ldif";
        }

        @Override
        public boolean isUpgradeNecessary(Connection conn, Schema schema) throws UpgradeException {
            return !entryExists(conn, new LDAPConfig(baseDN).getTokenStoreRootSuffix());
        }
    }

    private class CreateCTSIndexes implements Upgrader {

        @Override
        public String getLDIFPath() {
            return "/WEB-INF/template/ldif/sfha/cts-indices.ldif";
        }

        @Override
        public boolean isUpgradeNecessary(Connection conn, Schema schema) throws UpgradeException {
            DN indexDN = DN.valueOf("ds-cfg-attribute=" + CoreTokenField.EXPIRY_DATE.toString()
                    + ",cn=Index,ds-cfg-backend-id=userRoot,cn=Backends,cn=config");
            return !entryExists(conn, indexDN);
        }
    }

    private class AddDashboardSchema implements Upgrader {

        @Override
        public String getLDIFPath() {
            return "/WEB-INF/template/ldif/opendj/opendj_dashboard.ldif";
        }

        @Override
        public boolean isUpgradeNecessary(Connection conn, Schema schema) throws UpgradeException {
            return !schema.hasObjectClass(DASHBOARD_OC);
        }
    }

    private class AddDevicePrintSchema implements Upgrader {

        @Override
        public String getLDIFPath() {
            return "/WEB-INF/template/ldif/opendj/opendj_deviceprint.ldif";
        }

        @Override
        public boolean isUpgradeNecessary(Connection conn, Schema schema) throws UpgradeException {
            return !schema.hasObjectClass(DEVICE_PRINT_OC);
        }
    }
}
