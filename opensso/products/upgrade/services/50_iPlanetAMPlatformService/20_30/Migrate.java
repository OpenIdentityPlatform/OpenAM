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
 * $Id: Migrate.java,v 1.5 2009/09/30 17:34:29 goodearth Exp $
 *
 */

import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.io.File;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Migrates <code>iPlanetAMPlatformService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMPlatformService";
    final static String SERVICE_DIR = "50_iPlanetAMPlatformService/20_30";
    final static String SCHEMA_FILE = "amPlatform_addSubSchema.xml";
    final static String SCHEMA_FILE1 = "amPlatform_addAttrs.xml";
    final static String SCHEMA_FILE2 = "amPlatform_addConfig.xml";
    final static String schemaType = "Global";
    final static String SVCCONFIG_NAME = "server-default";
    final static String SITE_ATTR = "com-sun-identity-sites";
    final static String SERVER_ATTR = "com-sun-identity-servers";
    final static String PLATFORM_ATTR = "iplanet-am-platform-server-list";
    final static String ATTR_SITE_LIST = "iplanet-am-platform-site-list";
    final static String ATTR_LOGIN_URL = "iplanet-am-platform-login-url";
    final static String ATTR_LOGOUT_URL = "iplanet-am-platform-logout-url";
    final static String SERVER_HOST = "com.iplanet.am.server.host";
    final static String SERVER_PORT = "com.iplanet.am.server.port";
    final static String SERVER_PROTO = "com.iplanet.am.server.protocol";
    final static String ATTR_SERVER_VALIDATOR = "ServerIDValidator";
    final static String ATTR_SITE_VALIDATOR = "SiteIDValidator";
    final static String ATTR_LOCALES = "iplanet-am-platform-available-locales";
    final static String INSTANCE_ID = "00";

    /**
     * Updates the <code>iPlanetAMPlatformService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        String classMethod = "iPlanetAMPlatformService/20_30:migrateService: ";
        boolean isSuccess = false;
        try {
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE1);
            UpgradeUtils.addAttributeToSchema(
                    SERVICE_NAME, schemaType, fileName);
            // add subschema 
            fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
            UpgradeUtils.addSubSchema(SERVICE_NAME, null, schemaType, fileName);
            fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE2);
            UpgradeUtils.createService(fileName);

            UpgradeUtils.removeAttributeSchema(SERVICE_NAME, schemaType,
                    ATTR_LOGIN_URL, null);
            UpgradeUtils.removeAttributeSchema(SERVICE_NAME, schemaType,
                    ATTR_LOGOUT_URL, null);
            UpgradeUtils.removeAttributeSchema(SERVICE_NAME, schemaType,
                    ATTR_SERVER_VALIDATOR, null);
            UpgradeUtils.removeAttributeSchema(SERVICE_NAME, schemaType,
                    ATTR_SITE_VALIDATOR, null);
            UpgradeUtils.removeAttributeSchema(SERVICE_NAME, schemaType,
                    ATTR_LOCALES, null);

            // read serverdefaults.properties 
            Properties properties = new Properties();
            try {
                properties.load(
                        new FileInputStream(
                        UpgradeUtils.getServerDefaultsPath()));
            } catch (IOException ioe) {
                UpgradeUtils.debug.error("Error loading properties", ioe);
            }
            Set vSet = new HashSet();
            Enumeration propertiesNames = properties.propertyNames();
            while (propertiesNames.hasMoreElements()) {
                String propertyName = (String) propertiesNames.nextElement();
                String value = (String) properties.get(propertyName);
                vSet.add(propertyName + "=" + value);
            }
            // add to server-default subconfig
            UpgradeUtils.addServerDefaults(SERVICE_NAME, SERVER_ATTR,
                    null, INSTANCE_ID, vSet, null);
            if (UpgradeUtils.debug.messageEnabled()) {
                UpgradeUtils.debug.message(classMethod +
                    "serverdefaults.properties " + "values are :" + vSet);
            }

            // get the values of site list.
            Set attrValueSet = UpgradeUtils.getAttributeValue(SERVICE_NAME,
                    ATTR_SITE_LIST, schemaType);
            Iterator i = attrValueSet.iterator();
            while (i.hasNext()) {
                String attrVal = (String) i.next();
                String siteURL = null;
                String remStr = null;
                int index = attrVal.indexOf("|");
                if (index != -1) {
                    siteURL = attrVal.substring(0, index);
                    remStr = attrVal.substring(index + 1);
                }
                index = remStr.indexOf("|");
                if (index != -1) {
                    String siteId = remStr.substring(0, index);
                    String accessPointStr = remStr.substring(index + 1);
                    StringTokenizer st =
                            new StringTokenizer(accessPointStr, "|");
                    Set accessPoints = new HashSet();
                    while (st.hasMoreTokens()) {
                        accessPoints.add((String) st.nextToken());
                    }
                    UpgradeUtils.createSite(siteURL, accessPoints);
                }
            }

            /* Get existing/pre-migrated AMConfig.properties, 
             * get the latest AMConfig.properties from the deployed 
             * OpenSSO pointing to old DIT. Read the
             * "com.iplanet.am.version" from the latest OpenSSO bits
             * and replace only property in the existing/pre-migrated 
             * AMConfig.properties.
             */
   
            String serverStr = UpgradeUtils.getServerName() + "/" 
                + UpgradeUtils.getDeployURI();
            Properties amconfigProp = UpgradeUtils.getServerProperties();
            Properties p = UpgradeUtils.getProperties(
                UpgradeUtils.getConfigDir() + File.separator + 
                "AMConfig.properties");
            String prodVersion = (String) p.get("com.iplanet.am.version");
            if (prodVersion != null) {
                amconfigProp.put("com.iplanet.am.version",prodVersion);
                UpgradeUtils.storeProperties(amconfigProp);
            }
            // get value of iplanet-am-platform-server-list attribute
            attrValueSet = UpgradeUtils.getAttributeValue(SERVICE_NAME,
                    PLATFORM_ATTR, schemaType);

            Iterator attrValIterator = attrValueSet.iterator();
            while (attrValIterator.hasNext()) {
                String value = (String) attrValIterator.next();
                StringTokenizer st = new StringTokenizer(value, "|");
                String serverName = st.nextToken();
                String serverId = st.nextToken();
                String siteId = null;
                if (st.countTokens() == 3) {
                    siteId = st.nextToken();
                }
                // get complete instance name with deployURI
                String serverInstance =
                        UpgradeUtils.getServerInstance(serverName);
                if (serverName != null &&
                        serverName.equalsIgnoreCase(serverStr)) {
                    Set values = getValues(amconfigProp);
                    // read serverconfig XML
                    String serverconfigXML = UpgradeUtils.getServerConfigXML();
                    if (UpgradeUtils.debug.messageEnabled()) {
                        UpgradeUtils.debug.message(classMethod +
                                "AMConfig.properties " +
                                "values are :" + values);
                        UpgradeUtils.debug.message(classMethod +
                                "serverconfigXMl is :" + serverconfigXML);
                    }
                    UpgradeUtils.addServerDefaults(SERVICE_NAME, SERVER_ATTR,
                            serverInstance, serverId, values, serverconfigXML);
                } else {
                    UpgradeUtils.createServiceInstance(
                            serverInstance, serverId);
                }
                if (siteId != null) {
                    UpgradeUtils.addToSite(serverInstance, siteId);
                }
            }
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME, e);
        }
        return isSuccess;
    }

    /**
     * Post Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
        return true;
    }

    /**
     * Pre Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean preMigrateTask() {
        return true;
    }

    /**
     * Returns a set of values from the properties.
     * 
     * @param properties the properties object.
     * @return a set of values with the value in the format propertyName=value
     */
    private Set getValues(Properties properties) {
        Set vSet = new HashSet();
        Enumeration propertiesNames = properties.propertyNames();
        while (propertiesNames.hasMoreElements()) {
            String propertyName = (String) propertiesNames.nextElement();
            String value = (String) properties.get(propertyName);
            vSet.add(propertyName + "=" + value);
        }
        return vSet;
    }
}
